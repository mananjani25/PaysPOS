package com.android.pos.ui.fragments.customer

import android.os.Bundle
import android.text.Layout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.pos.data.model.CustomerDetailModel
import com.android.pos.databinding.FragmentCustomerBinding
import com.android.pos.databinding.FragmentCustomerDetailsBinding

class CustomerDetails : Fragment() {

    private lateinit var binding: FragmentCustomerDetailsBinding
    lateinit var customerModel: CustomerDetailModel

    companion object {
        private val CUSTOMER_MODEL = "customer_model"
        fun newInstance(model: CustomerDetailModel): CustomerDetails {
            val args = Bundle()
            args.putSerializable(CUSTOMER_MODEL, model)
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

        customerModel = requireArguments().getSerializable(CUSTOMER_MODEL) as CustomerDetailModel
        binding.model = customerModel
        binding.executePendingBindings()

    }
}