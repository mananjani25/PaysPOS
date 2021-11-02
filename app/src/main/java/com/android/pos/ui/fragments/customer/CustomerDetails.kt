package com.android.pos.ui.fragments.customer

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.TbCustomer
import com.android.pos.databinding.FragmentCustomerDetailsBinding
import com.android.pos.ui.adapter.SalesReportAdapter
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.EventObserver
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.extensions.visible
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson

class CustomerDetails : Fragment() {

    private lateinit var binding: FragmentCustomerDetailsBinding
    lateinit var customerModel: TbCustomer
    val TAG = "CustomerDetails"
    private val viewModel by viewModels<CustomerListViewModel>()
    private val salesReportAdapter by lazy { SalesReportAdapter() }

    companion object {
        private val CUSTOMER_MODEL = "customer_model"
        fun newInstance(model: TbCustomer): CustomerDetails {
            val args = Bundle()
            args.putParcelable(CUSTOMER_MODEL, model)
            val fragment = CustomerDetails()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCustomerDetailsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initControls()
        initObservers()

        //call initial api
        viewModel.getReportSummary()
    }

    private fun initControls() {
        customerModel =
            requireArguments().getParcelable<TbCustomer>(
                CUSTOMER_MODEL
            )!!
        binding.model = customerModel
        binding.executePendingBindings()

        Log.e(TAG, "CustomerDetails:  ${Gson().toJson(customerModel)}")
        binding.txtEdit.setOnClickListener {
            val bundle: Bundle = bundleOf("isEdit" to true, "dataModel" to customerModel)
            findNavController().navigate(R.id.action_customer_to_addEditCustomer, bundle)
        }

        if (customerModel.phones.isNotEmpty()) {
            binding.txtPhoneNo.text =
                "${AlertUtils.usNumberFormat(customerModel.phones[0].phone_number)}"
        }
        if (customerModel.addresses.isNotEmpty()) {
            ("" + customerModel.addresses[0].address1 + "," + customerModel.addresses[0].address2 + "," + customerModel.addresses[0].city).also {
                binding.txtAddress.text = it
            }
        }

        binding.rvOrderHistory.adapter = salesReportAdapter
    }

    private fun initObservers() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)
        viewModel.showProgress.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        })
        viewModel.orderHistory.observe(viewLifecycleOwner, EventObserver { data ->
            data?.let {
                if (it.salesSummary?.isNotEmpty() == true) {
                    binding.llOrderHistory.visible()
                    salesReportAdapter.add(it.salesSummary)
                } else {
                    binding.llOrderHistory.gone()
                }
            }
        })
    }
}