package com.android.pos.ui.fragments.customer

import android.os.Bundle
import android.text.Layout
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.CustomerDetailModel
import com.android.pos.databinding.FragmentCustomerBinding
import com.android.pos.databinding.FragmentCustomerDetailsBinding
import com.android.pos.utils.AlertUtils
import com.google.gson.Gson

class CustomerDetails : Fragment() {

    private lateinit var binding: FragmentCustomerDetailsBinding
    lateinit var customerModel: com.android.pos.data.model.CustomerListResponse.Data
    val TAG = "CustomerDetails"

    companion object {
        private val CUSTOMER_MODEL = "customer_model"
        fun newInstance(model: com.android.pos.data.model.CustomerListResponse.Data): CustomerDetails {
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

        customerModel =
            requireArguments().getParcelable<com.android.pos.data.model.CustomerListResponse.Data>(
                CUSTOMER_MODEL
            )!!
        binding.model = customerModel
        binding.executePendingBindings()

        Log.e(TAG, "CustomerDetails:  ${Gson().toJson(customerModel)}")
        binding.txtEdit.setOnClickListener {
            val bundle: Bundle = bundleOf("isEdit" to true, "dataModel" to customerModel)
            findNavController().navigate(R.id.action_customer_to_addEditCustomer, bundle)
        }

        if (customerModel.phones.size > 0) {
            binding.txtPhoneNo.setText("${AlertUtils.usNumberFormat(customerModel.phones.get(0).phone_number)}")
        }
        if (customerModel.addresses.size > 0) {
            binding.txtAddress.setText(""+customerModel.addresses.get(0).address1+","+customerModel.addresses.get(0).address2+","+customerModel.addresses.get(0).city)
        }
    }
}