package com.pays.pos.ui.fragments.settings.discount

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.entities.TbDiscount
import com.pays.pos.databinding.DiscountFragmentBinding
import com.pays.pos.ui.adapter.DiscountListAdapter
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.SwipeHelper
import com.pays.pos.utils.callback.ItemCallback
import com.pays.pos.utils.extensions.alert
import com.pays.pos.utils.extensions.liveSnackBar
import com.pays.pos.utils.extensions.showAlert
import com.pays.pos.utils.statusUtils.Status
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class DiscountList : Fragment() , ItemCallback {

    private lateinit var binding: DiscountFragmentBinding

    private var position: Int = -1
    private lateinit var discountListUpdateDelete: ArrayList<TbDiscount>
    private val viewModel by activityViewModels<DiscountListViewModel>()
    private lateinit var discountListadapter: DiscountListAdapter
    private lateinit var discountObject: TbDiscount
    private val TAG = "DiscountList"


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DiscountFragmentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        setUpRecyclerView()
        getTaxListObserver()
        setupSnackbar()
        observeShowProgress()
        deleteDiscount()
        notifyAdapter()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //setAdapter()
        onClick()

    }

    private fun onClick() {
        binding.txtCreateDiscount.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_createDiscount)
        }
    }

    private fun setUpRecyclerView() {
        discountListadapter = DiscountListAdapter(viewModel)
        binding.rvDiscountList.adapter = discountListadapter
        discountListadapter.setCallback(this)
    }

    private fun getTaxListObserver() {
        viewModel.getDiscountList.observe(viewLifecycleOwner) {


            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.rvDiscountList.visibility = View.VISIBLE
                        resource.data?.let { taxList ->
                            LogUtil.logE(TAG, "taxList:  ${Gson().toJson(taxList)}")
                            // Collections.reverse(taxList)
                            //LogUtil.logE(TAG,"taxListReversed:  ${Gson().toJson(taxList)}")
                            setTaxData(taxList)
                        }
                    }
                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.rvDiscountList.visibility = View.VISIBLE
                        binding.root.showAlert(resource.message)
                    }
                    Status.LOADING -> {
                        ProgressUtils.showProgressDialog(requireActivity())
                        binding.rvDiscountList.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun notifyAdapter() {
        viewModel.notifydata.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {

                discountListadapter.notifyDataSetChanged()
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

    private fun setTaxData(taxList: List<TbDiscount>) {
        discountListUpdateDelete = taxList as ArrayList<TbDiscount>
        discountListadapter.apply {
            addDiscount(taxList)
            notifyDataSetChanged()
        }
    }

    private fun deleteDiscount() {

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                /* AlertUtils.showAlert(requireActivity(), it.message)
                 var adapter = binding.rvTaxList.adapter as TaxListAdapter
                 var list = adapter.taxList
                 list.remove(taxObject)
                 adapter.taxList = list
                 adapter.notifyDataSetChanged()*/

                AlertUtils.showCustomAlert(requireActivity(), it.message)
                discountListUpdateDelete.remove(discountObject)
                discountListadapter.addDiscount(discountListUpdateDelete)
                discountListadapter.notifyItemRemoved(position)
                discountListadapter.notifyItemRangeChanged(position, discountListUpdateDelete.size)

            }
        })

    }

    private fun setupSnackbar() =
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    override fun onItemClickListener(view: View?, pos: Int) {
        val popupMenu = view?.let { PopupMenu(requireContext(), it) }
        popupMenu?.menuInflater?.inflate(R.menu.edit_delete_menu, popupMenu.menu)
        popupMenu?.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_edit -> {
                    discountObject = discountListadapter.getItem(pos)
                    val bundle = Bundle()
                    bundle.putBoolean("isEdit", true)
                    bundle.putParcelable("discountObject", discountObject)

                    //     var bundle= bundleOf()
                    findNavController().navigate(R.id.action_settings_to_createDiscount, bundle)
                }
                R.id.menu_delete -> {
                    position = pos

                    alert(
                        getString(R.string.app_name),
                        if (discountListadapter.getItem(pos).isActive) {
                            getString(R.string.delete_active_discount_message)
                        }else{
                            getString(R.string.delete_discount_message)
                        }
                    ) {
                        positiveButton(getString(R.string.tv_delete)) {
                            // Do positive stuff here
                            discountObject = discountListadapter.getItem(pos)
                            viewModel.delete(discountListadapter.getItem(pos).id)
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


    /*private fun setAdapter() {
        val list: ArrayList<DiscountListModel> = arrayListOf()
        list.add(DiscountListModel(0, "Staff Meal", "20%", false))
        list.add(DiscountListModel(0, "Military", "15%", false))
        list.add(DiscountListModel(0, "Senior Citizen", "25%", false))
        binding.rvDiscountList.adapter = DiscountListAdapter(requireContext(), list)

    }*/

}