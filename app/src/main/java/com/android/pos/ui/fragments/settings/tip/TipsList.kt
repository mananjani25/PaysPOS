package com.android.pos.ui.fragments.settings.tip

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.model.responseModel.GetTipReponse
import com.android.pos.databinding.FragmentTipsBinding
import com.android.pos.ui.adapter.TipsListAdapter
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.LogUtil
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.SwipeHelper
import com.android.pos.utils.callback.ItemCallback
import com.android.pos.utils.extensions.alert
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.statusUtils.Status
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class TipsList : Fragment(), ItemCallback {

    private var position: Int = -1
    private lateinit var binding: FragmentTipsBinding
    private val viewModel by viewModels<TipListViewModel>()
    private lateinit var tipListadapter: TipsListAdapter
    private lateinit var tipObject: GetTipReponse.Data
    private lateinit var tipListUpdateDelete: ArrayList<GetTipReponse.Data>
    var dragFrom = -1
    var dragTo = -1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTipsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        setUpRecyclerView()
        getTipListObserver()
        setupSnackbar()
        observeShowProgress()
        deleteTip()
        notifyAdapter()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //   setAdapter()
        binding.txtAddNewTip.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_addTip)
        }
    }

    private fun setUpRecyclerView() {
        tipListadapter = TipsListAdapter(viewModel)
        binding.rvTipList.adapter = tipListadapter
        tipListadapter.setCallback(this)


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

                val a = tipListadapter.getItem(dragFrom).sort
                val b = tipListadapter.getItem(dragTo).sort
                LogUtil.logE("onItemMove", "$a:: $b")



                tipListadapter.onItemMove(
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
                        tipListadapter.getItem(dragFrom).sort,
                        tipListadapter.getItem(dragTo).sort,
                        tipListadapter.getItem(viewHolder.bindingAdapterPosition).id
                    )
                }

                dragFrom = -1
                dragTo = -1
            }
        })

        touchHelper.attachToRecyclerView(binding.rvTipList)

    }

    private fun reallyMoved(oldPos: Int, newPos: Int, categoryIdOld: Int?) {
        if (categoryIdOld != null) {
            LogUtil.logE("reallyMoved", "$oldPos :: $newPos")
            viewModel.reOrderItem(categoryIdOld, oldPos, newPos)
        }

    }

    private fun getTipListObserver() {
        viewModel.getTipList.observe(viewLifecycleOwner) {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.rvTipList.visibility = View.VISIBLE
                        resource.data?.let { tipList ->
                            setTipData(tipList)
                        }
                    }
                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.rvTipList.visibility = View.VISIBLE
                        binding.root.showAlert(resource.message)
                    }
                    Status.LOADING -> {
                        ProgressUtils.showProgressDialog(requireActivity())
                        binding.rvTipList.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun notifyAdapter() {
        viewModel.notifydata.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                tipListadapter.notifyDataSetChanged()
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

        viewModel.data1.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                AlertUtils.showCustomAlert(requireActivity(), it.message)


                viewModel.reOrder(tipListadapter.getAll())


            }
        }

    }

    private fun setTipData(tipList: List<GetTipReponse.Data>) {
        tipListUpdateDelete = tipList as ArrayList<GetTipReponse.Data>
        tipListadapter.apply {
            addTips(tipList)
            notifyDataSetChanged()
        }
    }

    private fun deleteTip() {

        viewModel.data.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                /* AlertUtils.showAlert(requireActivity(), it.message)
                 var adapter = binding.rvTaxList.adapter as TaxListAdapter
                 var list = adapter.taxList
                 list.remove(taxObject)
                 adapter.taxList = list
                 adapter.notifyDataSetChanged()*/

                AlertUtils.showCustomAlert(requireActivity(), it.message)
                /*tipListUpdateDelete.remove(tipObject)
                tipListadapter.addTips(tipListUpdateDelete)
                tipListadapter.notifyItemRemoved(position)
                tipListadapter.notifyItemRangeChanged(position, tipListUpdateDelete.size)*/

            }
        }

    }

    private fun setupSnackbar() =
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    override fun onItemClickListener(view: View?, pos: Int) {
        val popupMenu = view?.let { PopupMenu(requireContext(), it) }
        popupMenu?.menuInflater?.inflate(R.menu.edit_delete_menu, popupMenu.menu)
        popupMenu?.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_edit -> {
                    tipObject = tipListadapter.getItem(pos)
                    val bundle = Bundle()
                    bundle.putBoolean("isEdit", true)
                    bundle.putParcelable("tipObject", tipObject)

                    //     var bundle= bundleOf()
                    findNavController().navigate(R.id.action_settings_to_addTip, bundle)
                }
                R.id.menu_delete -> {
                    position = pos

                    alert(
                        getString(R.string.app_name),
                        if (tipListadapter.getItem(pos).isActive) {
                            getString(R.string.delete_active_tip_message)
                        }else{
                            getString(R.string.delete_tip_message)
                        }
                    ) {
                        positiveButton(getString(R.string.tv_delete)) {
                            // Do positive stuff here
                            tipObject = tipListadapter.getItem(pos)
                            viewModel.delete(tipListadapter.getItem(pos).id)
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