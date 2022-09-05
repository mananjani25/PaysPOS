package com.android.pos.ui.fragments.inventory

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.TbItem
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.FragmentItemsBinding
import com.android.pos.ui.adapter.boldpos.ItemListPageAdapter
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.LogUtil
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.callback.ItemCallback
import com.android.pos.utils.extensions.alert
import com.android.pos.utils.extensions.runOnUiThread
import com.android.pos.utils.statusUtils.Status
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AllItems(val clickedPosition: Int, val totalItems: Int) : Fragment(), ItemCallback {

    private var totalItemCount: Int = 0
    private var pageCount: Int = 49
    private var isreOrder: Boolean = false
    private var deleteAndHide: Boolean = false

    private var deletePos: Int = -1
    private var deleteObj: TbItem? = null

    private lateinit var adapterPage: ItemListPageAdapter
    private lateinit var binding: FragmentItemsBinding
    private val viewModel by viewModels<ItemsViewModel>()
    var listSize: Int? = 0

    var dragFrom = -1
    var dragTo = -1


    private var syncReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            getInventoryCountsObserver()
        }

    }

    private val TAG = this.javaClass.name
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentItemsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        requireActivity().registerReceiver(
            syncReceiver,
            IntentFilter(Constants.SYNC_NOTIFICATION)
        )

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getInventoryCountsObserver()
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

            }

            override fun beforeTextChanged(
                s: CharSequence,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun afterTextChanged(s: Editable) {

                if (s.isNotEmpty() && s.length > 2) {
                    getSearchItemsFromDB(s.toString().trim())
                } else {
                    itemsObserver()
                }


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

                val a = adapterPage.peek(dragFrom)?.sort
                val b = adapterPage.peek(dragTo)?.sort
                Log.e("onItemMove", "$a:: $b")



                adapterPage.onItemMove(
                    viewHolder.bindingAdapterPosition,
                    target.bindingAdapterPosition
                )

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
                        adapterPage.peek(dragFrom)?.sort ?: 0,
                        adapterPage.peek(dragTo)?.sort ?: 0,
                        adapterPage.peek(viewHolder.bindingAdapterPosition)?.categoryId,
                        adapterPage.peek(viewHolder.bindingAdapterPosition)?.itemId
                    )
                }

                dragFrom = -1
                dragTo = -1
            }
        })

        touchHelper.attachToRecyclerView(binding.rvAllItemList)


    }

    private fun onClick() {
        binding.txtCreateItem.setOnClickListener {
            findNavController().navigate(R.id.action_inventory_to_createItem)
        }

    }

    private fun itemsObserver() {
        if (view != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                viewModel.allItems.collectLatest {
                    Log.e("collectLatest", it.toString())
                    adapterPage.submitData(it)

                }
            }
        }
    }

    private fun getSearchItemsFromDB(query: String) {
        var searchText = query
        searchText = "%$searchText%"
        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.allItemsQuery(desc = searchText).collectLatest {
                adapterPage.submitData(it)
            }
        }
    }

    private fun deleteObserver() {

        viewModel.data.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
//                if (!isreOrder)
                AlertUtils.showCustomAlert(requireActivity(), it.message)
                val intent = Intent()
                intent.action = "inventory"
                intent.putExtra("position", clickedPosition)
                requireContext().sendBroadcast(intent)
                if (isreOrder) {
                    isreOrder = false
                    viewModel.reOrder(adapterPage.snapshot().items.toCollection(arrayListOf()))
                }

//                val intent = Intent()
//                intent.action = "inventory"
//                intent.putExtra("position", clickedPosition)
//                requireContext().sendBroadcast(intent)


                // viewModel.dbDeleteAndHide(deleteObj!!.itemId, deleteAndHide)
            }
        }

    }

    private fun observeShowProgress() {

        viewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }

    }


    private fun setAdapter() {
        binding.rvAllItemList.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayoutManager.VERTICAL
            )
        )

        binding.rvAllItemList.setHasFixedSize(true)


        adapterPage = ItemListPageAdapter()
        binding.rvAllItemList.adapter = adapterPage
        adapterPage.setCallback(this)


    }


    private fun getInventoryCountsObserver() {
        try {
            if (view != null) {
                viewModel.inventoryCounts().observe(viewLifecycleOwner) {
                    it?.let { resource ->
                        when (resource.status) {
                            Status.SUCCESS -> {
                                totalItemCount = it.data?.data?.activeItems ?: 0
                                binding.edtSearch.hint = "Search (" + totalItemCount + ") Items"
                            }
                            Status.ERROR -> {

                            }
                            Status.LOADING -> {

                            }
                        }
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun reallyMoved(oldPos: Int, newPos: Int, categoryId: Int?, inventoryId: Int?) {
        if (categoryId != null) {
            isreOrder = true
            LogUtil.logE("reallyMoved", "$oldPos :: $newPos")
            viewModel.reOrderItem(inventoryId!!, oldPos, newPos)
        }

    }


    override fun onItemClickListener(view: View?, pos: Int) {
        val popupMenu = view?.let { PopupMenu(requireContext(), it) }
        popupMenu?.menuInflater?.inflate(R.menu.edit_delete__hide_menu, popupMenu.menu)
        popupMenu?.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_edit -> {
                    val itemObject = adapterPage.peek(pos)
                    val bundle = Bundle()
                    bundle.putBoolean("isEdit", true)
                    bundle.putParcelable("itemObject", itemObject)

                    findNavController().navigate(R.id.action_inventory_to_createItem, bundle)
                }
                R.id.menu_delete -> {
                    activity?.let {
                        AlertUtils.showCustomAlertWithListener(
                            it, getString(R.string.delete_item_message)
                        ) { _, _ ->

                            deleteAndHide = false
                            deletePos = pos
                            deleteObj = adapterPage.peek(pos)
                            //delete API call
                            viewModel.deleteAndHide(deleteObj!!.itemId, deleteAndHide, false)
                            //Delete item in database
//                            viewModel.dbDeleteAndHide(deleteObj!!.itemId, deleteAndHide)
                        }
                    }
                }
                R.id.menu_hide -> {
                    alert(
                        getString(R.string.app_name),
                        getString(R.string.hide_item_message)
                    ) {
                        positiveButton(getString(R.string.deactivate)) {
                            deleteAndHide = true
                            deleteObj = adapterPage.peek(pos)
                            viewModel.deleteAndHide(deleteObj!!.itemId, deleteAndHide, false)
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