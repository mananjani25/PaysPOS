package com.pays.pos.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.data.remote.Constants
import com.pays.pos.databinding.DialogPrinterTypeSelectionBinding

class PrinterTypeSelection : DialogFragment() {

    private lateinit var binding: DialogPrinterTypeSelectionBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogPrinterTypeSelectionBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onClick()
    }

    private fun onClick() {
        binding.imgBack.setOnClickListener {
            dismiss()
        }

        binding.radioGrp.setOnCheckedChangeListener { radioGroup, i ->
            when (radioGroup.checkedRadioButtonId) {
                R.id.rbKitchenPrinter -> {
                    val result = Bundle()
                    result.putString("type", Constants.KITCHEN)
                    setFragmentResult("request_printer_type", result)
                    findNavController().navigateUp()
                }
                R.id.rbCustomerPrinter -> {
                    val result = Bundle()
                    result.putString("type", Constants.CUSTOMER)
                    setFragmentResult("request_printer_type", result)
                    findNavController().navigateUp()

                }
                R.id.rbBothPrinter -> {
                    val result = Bundle()
                    result.putString("type", Constants.KITCHENANDCUSTOMER)
                    setFragmentResult("request_printer_type", result)
                    findNavController().navigateUp()

                }
            }

        }

    }


}