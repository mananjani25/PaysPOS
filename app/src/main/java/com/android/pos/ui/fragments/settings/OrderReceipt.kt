package com.android.pos.ui.fragments.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.databinding.FragmentOrderReceiptSettingsBinding


class OrderReceipt : Fragment() {

    private lateinit var binding: FragmentOrderReceiptSettingsBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_order_receipt_settings,
                container,
                false
            )
        binding.lifecycleOwner = this

        binding.llCustomerReceiptSettings.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_customerReceiptSettings)
        }

        binding.llKitchenReceiptSettings.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_kitchenReceiptSettings)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}