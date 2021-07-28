package com.android.pos.ui.fragments.customer

import android.os.Bundle
import android.text.Layout
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

class CustomerDetails : Fragment() {

    private lateinit var binding: FragmentCustomerDetailsBinding
    lateinit var customerModel: com.android.pos.data.model.CustomerListResponse.Data

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

        binding.txtEdit.setOnClickListener {
            val bundle: Bundle = bundleOf("isEdit" to true, "dataModel" to customerModel)
            findNavController().navigate(R.id.action_customer_to_addEditCustomer, bundle)
        }

    }
}