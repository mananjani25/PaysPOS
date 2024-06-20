package com.pays.pos.ui.fragments.inventory

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.pays.pos.R
import com.pays.pos.data.entities.TbItem
import com.pays.pos.databinding.FragmentItemsBinding
import com.pays.pos.ui.adapter.ItemListAdapter
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.callback.ItemCallback
import com.pays.pos.utils.extensions.alert
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.setOnSingleClickListener
import com.pays.pos.utils.extensions.visible
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HideItemListing(val clickedPosition: Int) : Fragment(), ItemCallback {

    private var isreOrder: Boolean = false
    private var deleteAndHide: Boolean = false
    var listSize: Int? = 0

    private var deletePos: Int = -1
    private var deleteObj: TbItem? = null
    private lateinit var adapter: ItemListAdapter
    private lateinit var binding: FragmentItemsBinding
    private val viewModel by viewModels<ItemsViewModel>()

    var dragFrom = -1
    var dragTo = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentItemsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.txtCreateItem.visibility = View.GONE
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setAdapter()
        onClick()
        itemsObserver()
        deleteObserver()
        setupHelper()
        searchFilter()
        observeShowProgress()
    }

    private fun searchFilter() {

        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.toString() == " ") {
                    binding.edtSearch.setText("")
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun afterTextChanged(s: Editable) {

                adapter.filter.filter(s.toString().trim())

            }
        })
    }


    private fun setupHelper() {


        val touchHelper = ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP + ItemTouchHelper.DOWN, 0) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {

                val oldPos = viewHolder.bindingAdapterPosition
                val newPos = target.bindingAdapterPosition
                LogUtil.logE(
                    "reorder after", "" + ":::" + ":::" +
                            viewHolder.bindingAdapterPosition.toString() + " :::  " + target.bindingAdapterPosition.toString()
                )
                if (dragFrom == -1) {
                    dragFrom = oldPos
                }
                dragTo = newPos

                val a = adapter.getItem(dragFrom).sort
                val b = adapter.getItem(dragTo).sort
                LogUtil.logE("onItemMove", "$a:: $b")



                adapter.onItemMove(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)

                return true
            }

            override fun isLongPressDragEnabled(): Boolean {
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {

                if (dragFrom != -1 && dragTo != -1 && dragFrom != dragTo) {

                    LogUtil.logE("clearView", "$dragFrom :: $dragTo")
                    reallyMoved(
                        adapter.getItem(dragFrom).sort,
                        adapter.getItem(dragTo).sort,
                        adapter.getItem(viewHolder.bindingAdapterPosition).categoryId,
                        adapter.getItem(viewHolder.bindingAdapterPosition).itemId
                    )
                }

                dragFrom = -1
                dragTo = -1
            }
        })

        touchHelper.attachToRecyclerView(binding.rvAllItemList)
    }

    private fun onClick() {
        binding.txtCreateItem.setOnSingleClickListener {
            findNavController().navigate(R.id.action_inventory_to_createItem)
        }

    }

    private fun itemsObserver() {

        viewModel.hideItemsListPos.observe(viewLifecycleOwner) {

            Log.e("MENU ITEM","HIDE ITEM OBSERVE ${Gson().toJson(it)}")

            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {

                        binding.progressCircular.visibility = View.GONE
                        listSize = it.data?.size
                        if (listSize == 0) {
                            binding.txtNodata.visible()
                            binding.rvAllItemList.visibility = View.GONE
                            if (it.message != null && it.message.isNotEmpty()) {
                                binding.txtNodata.text = it.message
                            } else {
                                binding.txtNodata.text = "No data available"
                                binding.llNoData?.visible()
                            }
                        } else {

                            setAdapter()
                            binding.rvAllItemList.visibility = View.VISIBLE
                            binding.llNoData?.gone()
                            binding.txtNodata.gone()
                        }
                        it.data?.let { it1 ->
                            adapter.add(it1)
                            binding.edtSearch.hint = "Search (" + it1.size + ") Items"
                            adapter.notifyDataSetChanged()
                        }


                        Log.e("MENU ITEM","HIDE ITEM SUCCESS")

                    }
                    Status.ERROR -> {
                        binding.rvAllItemList.visibility = View.GONE
                        binding.progressCircular.visibility = View.GONE
                        Log.e("MENU ITEM","HIDE ITEM ERROR")
                    }
                    Status.LOADING -> {
                        binding.rvAllItemList.visibility = View.GONE
                        binding.progressCircular.visibility = View.VISIBLE
                        Log.e("MENU ITEM","HIDE LOADING")
                    }
                }
            }


        }
    }

    private fun deleteObserver() {

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                if (!isreOrder)
                    AlertUtils.showCustomAlert(requireActivity(), it.message)

                if (isreOrder) {
                    isreOrder = false
                    viewModel.reOrder(adapter.getAll())
                }
                // viewModel.dbDeleteAndHide(deleteObj!!.itemId, deleteAndHide)

                val intent = Intent()
                intent.action = "inventory"
                intent.putExtra("position", clickedPosition)
                requireContext().sendBroadcast(intent)
            }
        })

    }

    private fun observeShowProgress() {

        viewModel.showProgress.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        })

    }


    private fun setAdapter() {
        binding.rvAllItemList.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayoutManager.VERTICAL
            )
        )
        adapter = ItemListAdapter(false, "")
        binding.rvAllItemList.adapter = adapter
        adapter.setCallback(this)
    }

    private fun reallyMoved(oldPos: Int, newPos: Int, categoryId: Int?, inventoryId: Int?) {
        if (categoryId != null) {
            isreOrder = true
            LogUtil.logE("reallyMoved", "$oldPos :: $newPos")
            viewModel.reOrderItem(inventoryId!!, newPos, oldPos)
        }

    }

    override fun onItemClickListener(view: View?, pos: Int) {
        val popupMenu = view?.let { PopupMenu(requireContext(), it) }
        popupMenu?.menuInflater?.inflate(R.menu.edit_delete__hide_menu, popupMenu.menu)
        popupMenu?.menu?.findItem(R.id.menu_edit)?.isVisible = false
        popupMenu?.menu?.findItem(R.id.menu_delete)?.isVisible = false
        popupMenu?.menu?.findItem(R.id.menu_hide)?.title = "Unhide For POS"
        popupMenu?.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_hide -> {
                    alert(
                        getString(R.string.app_name),
                        getString(R.string.unhide_item_message)
                    ) {
                        positiveButton(getString(R.string.activate)) {
                            viewModel.unHideItems(adapter.getItem(pos).itemId,"pos")
                        }
                        negativeButton(R.string.tv_cancel) {
                            // Do negative stuff here
                        }
                    }

                }
            }
            true
        }
        popupMenu?.show()
    }
}