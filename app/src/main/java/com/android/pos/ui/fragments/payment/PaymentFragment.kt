package com.android.pos.ui.fragments.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.databinding.PaymentFragmentBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PaymentFragment : Fragment() {

    private var splitValue: Int = -1
    private var totalPrice: Double = 0.0
    private lateinit var binding: PaymentFragmentBinding

    companion object {
        fun newInstance() = PaymentFragment()
    }

    private lateinit var viewModel: PaymentViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = PaymentFragmentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        setupData()
        callbackSetup()

        return binding.root
    }

    private fun callbackSetup() {

        setFragmentResultListener("request_key_split") { requestKey: String, bundle: Bundle ->
            splitValue = bundle.getInt("split")

            val splitAfterAmount = totalPrice / splitValue

            (getString(R.string.symbole) + String.format(
                "%.2f",
                splitAfterAmount
            )).also { binding.txtTotalAmount.text = it }

            val totalAmountFormat = getString(R.string.symbole) + String.format("%.2f", totalPrice)

            binding.txtSplitAmount.text = "Edit Split Amount"

            binding.txtSplitValue.text =
                "Out of $totalAmountFormat Total, Payment 1 of $splitValue"

        }


    }

    private fun setupData() {


        totalPrice = requireArguments().getDouble("totalPrice")
        val subTotalPrice = requireArguments().getDouble("subTotalPrice")
        val totalTax = requireArguments().getDouble("totalTax")
        val totalServiceCharge = requireArguments().getDouble("totalServiceCharge")


        (getString(R.string.symbole) + String.format(
            "%.2f",
            totalPrice
        )).also { binding.txtTotalAmount.text = it }

        (getString(R.string.symbole) + String.format(
            "%.2f",
            subTotalPrice
        )).also { binding.txtSubTotal.text = it }

        (getString(R.string.symbole) + String.format(
            "%.2f",
            totalTax
        )).also { binding.txtTax.text = it }

        (getString(R.string.symbole) + String.format(
            "%.2f",
            totalPrice
        )).also { binding.txtTotal.text = it }

        binding.txtSplitAmount.setOnClickListener {
            val bundle = Bundle()
            bundle.putDouble("totalPrice", totalPrice)
            bundle.putInt("splitValue", splitValue)
            findNavController().navigate(R.id.action_paymentFragment_to_splitAmountFragment, bundle)
        }
        binding.txtCustom.setOnClickListener {
            findNavController().navigate(R.id.action_paymentFragment_to_customAmountFragment)
        }

        binding.imgBack.setOnClickListener {
            findNavController().popBackStack()
        }

    }


}