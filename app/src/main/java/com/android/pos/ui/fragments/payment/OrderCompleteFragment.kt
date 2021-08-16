package com.android.pos.ui.fragments.payment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.CustomerListResponse
import com.android.pos.databinding.FragmentOrderCompletBinding
import com.android.pos.utils.MethodUtils

class OrderCompleteFragment : Fragment(), View.OnClickListener {
    private var totalPrice: Double = 0.0
    private var paymentAmount: Double = 0.0

    private lateinit var binding: FragmentOrderCompletBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOrderCompletBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        totalPrice = requireArguments().getDouble("totalPrice")
        paymentAmount = requireArguments().getDouble("paymentAmount")

        binding.txtTitle.text =
            MethodUtils.roundOffAmount(paymentAmount) + " cash"

        if (totalPrice != paymentAmount) {
            binding.txtChangeAmount.text =
                MethodUtils.roundOffAmount(paymentAmount - totalPrice) + " Change"
            binding.txtPaymentAmount.text = "Out of " + MethodUtils.roundOffAmount(paymentAmount)

        }

        binding.txtHome.setOnClickListener(this)
        binding.txtAddCustomer.setOnClickListener(this)
        binding.llMessage.setOnClickListener(this)
        binding.llEmail.setOnClickListener(this)
        binding.llNoReceipt.setOnClickListener(this)
        binding.llPrint.setOnClickListener(this)
        binding.txtSend.setOnClickListener(this)
        binding.imgBack.setOnClickListener(this)

        setFragmentResultListener("request_key_customer") { requestKey: String, bundle: Bundle ->
            val result = bundle.getParcelable<CustomerListResponse.Data>("data")
            if (result != null) {
                Log.e("request_key_customer", result.first_name)

            }
        }
    }

    override fun onClick(v: View?) {

        when (v?.id) {
            R.id.txtHome -> {
                findNavController().navigate(R.id.action_orderCompleteFragment_to_dashboardCategoryNew)
            }
            R.id.txtAddCustomer -> {
                findNavController().navigate(R.id.action_orderCompleteFragment_to_assignCustomerOrderFragment)
            }
            R.id.llMessage -> {
                binding.llSendReceipt.visibility = View.VISIBLE
                binding.edtEmail.visibility = View.GONE
                binding.imgBack.visibility = View.VISIBLE
                binding.edtPhoneNo.visibility = View.VISIBLE
                binding.llOptions.visibility = View.GONE
                binding.txtHome.visibility = View.GONE
                binding.txtAddCustomer.visibility = View.GONE
                binding.llOptions.visibility = View.GONE
                MethodUtils.hideKeyboard(requireActivity())
            }
            R.id.llEmail -> {

                binding.llSendReceipt.visibility = View.VISIBLE
                binding.edtEmail.visibility = View.VISIBLE
                binding.imgBack.visibility = View.VISIBLE
                binding.edtPhoneNo.visibility = View.GONE
                binding.llOptions.visibility = View.GONE
                binding.txtHome.visibility = View.GONE
                binding.txtAddCustomer.visibility = View.GONE
                binding.llOptions.visibility = View.GONE
                MethodUtils.hideKeyboard(requireActivity())
            }
            R.id.llNoReceipt -> {
                findNavController().navigate(R.id.action_orderCompleteFragment_to_dashboardCategoryNew)
            }
            R.id.llPrint -> {
                findNavController().navigate(R.id.action_orderCompleteFragment_to_dashboardCategoryNew)
            }
            R.id.txtSend -> {

            }
            R.id.imgBack -> {
                MethodUtils.hideKeyboard(requireActivity())
                binding.llSendReceipt.visibility = View.GONE
                binding.imgBack.visibility = View.GONE
                binding.txtHome.visibility = View.VISIBLE
                binding.txtAddCustomer.visibility = View.VISIBLE
                binding.llOptions.visibility = View.VISIBLE
            }
        }
    }


}