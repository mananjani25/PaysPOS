package com.pays.pos.ui.fragments.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.data.remote.Constants
import com.pays.pos.databinding.FragmentOrderReceiptSettingsBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.extensions.visible
import dagger.hilt.android.AndroidEntryPoint
import java.lang.Exception
import javax.inject.Inject


@AndroidEntryPoint
class OrderReceipt : Fragment() {


    @Inject
    lateinit var prefProvider: PrefProvider

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


        if (!prefProvider?.getValueboolean(Constants.IS_PRINTER_QUEUE_ENABLE,false)){
            binding.llKitchenReceiptSettings.visible()
        }

        binding.llCustomerReceiptSettings.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_settings_to_customerReceiptSettings)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        binding.llKitchenReceiptSettings.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_settings_to_kitchenReceiptSettings)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}