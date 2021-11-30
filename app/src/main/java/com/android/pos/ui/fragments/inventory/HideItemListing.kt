package com.android.pos.ui.fragments.inventory

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.TbItem
import com.android.pos.databinding.FragmentItemsBinding
import com.android.pos.ui.adapter.ItemListAdapter
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.SwipeHelper
import com.android.pos.utils.extensions.alert
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HideItemListing : Fragment() {

    private var isreOrder: Boolean = false
    private var deleteAndHide: Boolean = false

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
                Log.e(
                    "reorder after", "" + ":::" + ":::" +
                            viewHolder.bindingAdapterPosition.toString() + " :::  " + target.bindingAdapterPosition.toString()
                )
                if (dragFrom == -1) {
                    dragFrom = oldPos
                }
                dragTo = newPos

                val a = adapter.getItem(dragFrom).sort
                val b = adapter.getItem(dragTo).sort
                Log.e("onItemMove", "$a:: $b")



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

                    Log.e("clearView", "$dragFrom :: $dragTo")
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

        object : SwipeHelper(activity, binding.rvAllItemList) {
            override fun instantiateUnderlayButton(
                viewHolder: RecyclerView.ViewHolder?,
                underlayButtons: MutableList<UnderlayButton?>
            ) {

                underlayButtons.add(UnderlayButton(
                    "UnHide",
                    0,
                    Color.parseColor("#2997cc")
                ) { pos ->

                    alert(
                        getString(R.string.app_name),
                        getString(R.string.unhide_item_message)
                    ) {
                        positiveButton(getString(R.string.activate)) {
                            deleteAndHide = true
                            deleteObj = adapter.getItem(pos)
                            viewModel.deleteAndHide(deleteObj!!.itemId, deleteAndHide, true)
                        }
                        negativeButton(R.string.tv_cancel) {
                            // Do negative stuff here
                        }
                    }


                })

                /*underlayButtons.add(UnderlayButton(
                    "Delete",
                    0,
                    Color.parseColor("#FF3C30")
                ) { pos ->

                    activity?.let {
                        AlertUtils.showCustomAlertWithListener(
                            it, getString(R.string.delete_item_message)
                        ) { _, _ ->

                            deleteAndHide = false
                            deletePos = pos
                            deleteObj = adapter.getItem(pos)
                            //delete API call
                            viewModel.deleteAndHide(deleteObj!!.itemId, deleteAndHide)
                            //Delete item in database
//                            viewModel.dbDeleteAndHide(deleteObj!!.itemId, deleteAndHide)
                        }
                    }

                })*/

                /* underlayButtons.add(UnderlayButton(
                     "Edit",
                     0,
                     Color.parseColor("#2997cc")
                 ) { pos ->

                     val itemObject = adapter.getItem(pos)
                     val bundle = Bundle()
                     bundle.putBoolean("isEdit", true)
                     bundle.putParcelable("itemObject", itemObject)

                     findNavController().navigate(R.id.action_inventory_to_createItem, bundle)


                 })*/
            }
        }

    }

    private fun onClick() {
        binding.txtCreateItem.setOnClickListener {
            findNavController().navigate(R.id.action_inventory_to_createItem)
        }

    }

    private fun itemsObserver() {

        viewModel.showItemsList.observe(viewLifecycleOwner, {

            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        binding.rvAllItemList.visibility = View.VISIBLE
                        binding.progressCircular.visibility = View.GONE
                        it.data?.let { it1 ->
                            adapter.add(it1)
                            binding.edtSearch.hint = "Search (" + it1.size + ") Items"
                        }
                    }
                    Status.ERROR -> {
                        binding.rvAllItemList.visibility = View.GONE
                        binding.progressCircular.visibility = View.GONE
                    }
                    Status.LOADING -> {
                        binding.rvAllItemList.visibility = View.GONE
                        binding.progressCircular.visibility = View.VISIBLE
                    }
                }
            }


        })
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
        adapter = ItemListAdapter(false)
        binding.rvAllItemList.adapter = adapter
    }

    private fun reallyMoved(oldPos: Int, newPos: Int, categoryId: Int?, inventoryId: Int?) {
        if (categoryId != null) {
            isreOrder = true
            Log.e("reallyMoved", "$oldPos :: $newPos")
            viewModel.reOrderItem(inventoryId!!, newPos, oldPos)
        }

    }
}