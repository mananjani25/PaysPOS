package com.android.pos.ui.fragments.settings.kitchenreceipt

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.remote.Constants.KEY
import com.android.pos.data.remote.Constants.ORDER_RECEIPTS
import com.android.pos.data.remote.Constants.SETTING_KEY
import com.android.pos.databinding.FragmentKitchenReceiptSettingsBinding

class KitchenReceiptSettings : Fragment(),CompoundButton.OnCheckedChangeListener {

    private lateinit var binding: FragmentKitchenReceiptSettingsBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_kitchen_receipt_settings, container, false)
        binding.lifecycleOwner = this

        binding.ivBack.setOnClickListener {
            val navController = findNavController()
            navController.previousBackStackEntry?.savedStateHandle?.set(KEY,ORDER_RECEIPTS)
            navController.popBackStack()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
       // if(buttonView?.id==)
    }
}