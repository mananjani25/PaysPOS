package com.android.pos.ui.fragments.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.databinding.PaymentFragmentBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PaymentFragment : Fragment() {

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

        return binding.root
    }

    private fun setupData() {

        binding.txtSplitAmount.setOnClickListener {
            findNavController().navigate(R.id.action_paymentFragment_to_splitAmountFragment)
        }
        binding.txtCustom.setOnClickListener {
            findNavController().navigate(R.id.action_paymentFragment_to_customAmountFragment)
        }

        val totalPrice = requireArguments().getDouble("totalPrice")
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

    }


}