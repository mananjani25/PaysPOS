package com.android.pos.ui.fragments.inventory

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.RadioButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
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
import com.android.pos.utils.statusUtils.Status
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
       // setupHelper()
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

            override fun beforeTextChanged(
                s: CharSequence,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun afterTextChanged(s: Editable) {

                if (s.isNotEmpty() && s.length > 2 && !s.toString().endsWith(" ")) {
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

        try {
            adapterPage.addLoadStateListener { loadState ->
                if (loadState.refresh is LoadState.NotLoading && adapterPage.itemCount == 0) {
                    // Data is not available; show the "Data not available" message
                    binding.rvAllItemList.visibility = View.GONE
                    binding.llNoData!!.visibility = View.VISIBLE
                } else {
                    // Data is available; hide the message
                    binding.rvAllItemList.visibility = View.VISIBLE
                    binding.llNoData!!.visibility = View.GONE
                    binding.txtNodata.setText(resources.getString(R.string.no_data_available))
                    binding.txtItemWillAppear!!.setText(resources.getString(R.string.items_will_be_appear_here))
                }
            }
        } catch (e: Exception) {

        }

        binding.rvAllItemList.adapter = adapterPage
        binding.rvAllItemList.itemAnimator = null
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
        popupMenu?.menuInflater?.inflate(R.menu.item_option_menu_delete_hide, popupMenu.menu)
        var menu_pos = popupMenu?.menu?.findItem(R.id.menu_hide_pos)
        var menu_website = popupMenu?.menu?.findItem(R.id.menu_hide_website)
        if (adapterPage.peek(pos)?.hide_status == "UnHide") {
            menu_pos?.title = "Hide For POS"
        } else {
            menu_pos?.title = "UnHide For POS"
        }
        if (adapterPage.peek(pos)?.website_hide_status == "UnHideOnWebsite") {
            menu_website?.title = "Hide For Website"
        } else {
            menu_website?.title = "UnHide For Website"
        }
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
                            viewModel.deleteItems(deleteObj!!.itemId)
                        }
                    }
                }
                R.id.menu_hide_pos -> {
                    if (adapterPage.peek(pos)?.hide_status == "HideForToday" || adapterPage.peek(pos)?.hide_status == "HideForIndefinitely") {
                        viewModel.unHideItems(adapterPage.peek(pos)?.itemId!!, "pos")
                    } else {
                        dialogShowForHide(adapterPage.peek(pos), "pos")
                    }
                }
                R.id.menu_hide_website -> {
                    if (adapterPage.peek(pos)?.website_hide_status == "HideForTodayOnWebsite" || adapterPage.peek(
                            pos
                        )?.website_hide_status == "HideForIndefinitelyOnWebsite"
                    ) {
                        viewModel.unHideItems(adapterPage.peek(pos)?.itemId!!, "website")
                    } else {
                        dialogShowForHide(adapterPage.peek(pos), "website")
                    }
                }
            }
            true
        }
        popupMenu?.show()
    }

    fun dialogShowForHide(tbdata: TbItem?, type: String) {
        var dialogView = LayoutInflater.from(context).inflate(R.layout.hide_item_dialog, null)
        val customDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .show()
        customDialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        val back = ColorDrawable(ContextCompat.getColor(binding.root.context, R.color.bg_color))
        val inset = InsetDrawable(back, 150, 200, 150, 200)
        customDialog?.window?.setBackgroundDrawable(inset);
        var txttitle = customDialog.findViewById<AppCompatTextView>(R.id.txtTitle)
        var imgback = customDialog.findViewById<AppCompatImageView>(R.id.imgBack)
        var rdone = customDialog.findViewById<RadioButton>(R.id.hidetoday)
        var rdtwo = customDialog.findViewById<RadioButton>(R.id.hideindefinitely)
        var txtSave = customDialog.findViewById<AppCompatTextView>(R.id.txtSave)
        var txtCancel = customDialog.findViewById<AppCompatTextView>(R.id.txtcancel)
        txttitle.text = "Select hide type (" + tbdata?.name + ")"
        var status = if (type == "website") {
            "HideForIndefinitelyOnWebsite"
        } else {
            "HideForIndefinitely"
        }

        txtCancel.setOnClickListener {
            customDialog.dismiss()
        }
        imgback.setOnClickListener {
            customDialog.dismiss()
        }
        rdone.setOnClickListener {
            if (type == "website") {
                status = "HideForTodayOnWebsite"
            } else {
                status = "HideForToday"
            }
        }
        rdtwo.setOnClickListener {
            if (type == "website") {
                status = "HideForIndefinitelyOnWebsite"
            } else {
                status = "HideForIndefinitely"
            }

        }
        txtSave.setOnClickListener {
            Log.d(TAG, "dialogShowForHide: " + status)
            tbdata?.itemId?.let { it1 -> viewModel.hideItems(it1, status, type) }
            customDialog.dismiss()
        }


    }

}