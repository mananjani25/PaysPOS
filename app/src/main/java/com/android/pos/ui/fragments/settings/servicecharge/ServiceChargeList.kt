package com.android.pos.ui.fragments.settings.servicecharge

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.model.responseModel.GetServiceChargeResponse
import com.android.pos.databinding.ServiceChargeFragmentBinding
import com.android.pos.ui.adapter.ServiceChargeListAdapter
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.SwipeHelper
import com.android.pos.utils.extensions.alert
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.statusUtils.Status
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ServiceChargeList : Fragment() {

    private lateinit var binding: ServiceChargeFragmentBinding

    private var position: Int = -1
    private lateinit var discountListUpdateDelete: ArrayList<GetServiceChargeResponse.Data>
    private val viewModel by viewModels<ServiceChargeListViewModel>()
    private var serviceChargeListadapter = ServiceChargeListAdapter()
    private lateinit var serviceChargeObject: GetServiceChargeResponse.Data

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = ServiceChargeFragmentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        setUpRecyclerView()
        getTaxListObserver()
        setupSnackbar()
        observeShowProgress()
        deleteServiceCharge()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<AppCompatTextView>(R.id.txtAddServiceCharge).setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_addServiceCharge)
        }
    }

    private fun setUpRecyclerView() {
        binding.rvServiceCharge.adapter = serviceChargeListadapter

        object : SwipeHelper(activity, binding.rvServiceCharge) {
            override fun instantiateUnderlayButton(
                viewHolder: RecyclerView.ViewHolder?,
                underlayButtons: MutableList<UnderlayButton?>
            ) {

                underlayButtons.add(UnderlayButton(
                    "Edit",
                    0,
                    Color.parseColor("#2997cc")
                ) { pos ->

                    serviceChargeObject = serviceChargeListadapter.getItem(pos)
                    val bundle = Bundle()
                    bundle.putBoolean("isEdit", true)
                    bundle.putParcelable("serviceChargeObject", serviceChargeObject)

                    //     var bundle= bundleOf()
                    findNavController().navigate(
                        R.id.action_settings_to_addServiceCharge,
                        bundle
                    )

                })

                underlayButtons.add(UnderlayButton(
                    "Delete",
                    0,
                    Color.parseColor("#FF3C30")
                ) { pos ->

                    position = pos

                    alert(
                        getString(R.string.app_name),
                        getString(R.string.delete_tax_message)
                    ) {
                        positiveButton(getString(R.string.tv_delete)) {
                            // Do positive stuff here
                            serviceChargeObject = serviceChargeListadapter.getItem(pos)
                            viewModel.delete(serviceChargeListadapter.getItem(pos).id)
                        }
                        negativeButton(R.string.tv_cancel) {
                            // Do negative stuff here
                        }
                    }
                })
            }
        }
    }

    private fun getTaxListObserver() {
        viewModel.getDiscountList.observe(viewLifecycleOwner, {


            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.rvServiceCharge.visibility = View.VISIBLE
                        resource.data?.let { taxList -> setTaxData(taxList.data) }
                    }
                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.rvServiceCharge.visibility = View.VISIBLE
                        binding.root.showAlert(resource.message)
                    }
                    Status.LOADING -> {
                        ProgressUtils.showProgressDialog(requireActivity())
                        binding.rvServiceCharge.visibility = View.GONE
                    }
                }
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

    private fun setTaxData(taxList: List<GetServiceChargeResponse.Data>) {
        discountListUpdateDelete = taxList as ArrayList<GetServiceChargeResponse.Data>
        serviceChargeListadapter.apply {
            addServiceCharge(taxList)
            notifyDataSetChanged()
        }
    }

    private fun deleteServiceCharge() {

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                /* AlertUtils.showAlert(requireActivity(), it.message)
                 var adapter = binding.rvTaxList.adapter as TaxListAdapter
                 var list = adapter.taxList
                 list.remove(taxObject)
                 adapter.taxList = list
                 adapter.notifyDataSetChanged()*/

                AlertUtils.showCustomAlert(requireActivity(), it.message)
                discountListUpdateDelete.remove(serviceChargeObject)
                serviceChargeListadapter.addServiceCharge(discountListUpdateDelete)
                serviceChargeListadapter.notifyItemRemoved(position)
                serviceChargeListadapter.notifyItemRangeChanged(
                    position,
                    discountListUpdateDelete.size
                )

            }
        })

    }

    private fun setupSnackbar() =
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

}