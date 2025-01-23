package com.pays.pos.ui.fragments.settings.hardware.printer

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.pays.pos.data.model.PrinterListModel
import com.pays.pos.databinding.DialogPrinterSnumberBinding
import com.pays.pos.utils.AlertUtils

class DialogSNumber : DialogFragment() {
    private lateinit var binding:DialogPrinterSnumberBinding
    private var printerListModel: PrinterListModel?=null
    private var layoutPosition = 0
    private var TAG = "DialogSNumber"


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogPrinterSnumberBinding.inflate(inflater,container,false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        printerListModel = arguments?.getParcelable<PrinterListModel>("printerListModel")
        layoutPosition = arguments?.getInt("layoutPosition")?:0
        Log.e(TAG,"parcelableList:  ${Gson().toJson(printerListModel)}")
        Log.e(TAG,"layoutPosition:  ${layoutPosition}")

        binding.txtContinue.setOnClickListener {
            if (binding.edtSplitNo.text?.trim()?.isEmpty() == true) {
                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireContext(),
                    "Please Enter Valid Serial Number.",
                )
                { _, _ ->

                }
            }
            else if (binding.edtSplitNo.text?.trim()?.length!! < 13){
                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireContext(),
                    "Please Enter Valid Serial Number.",
                )
                { _, _ ->

                }

            }
            else {

                var result = Bundle()
                result.putString("serial_number", binding.edtSplitNo.text.toString())
                result.putParcelable("printerListModel", printerListModel)
                result.putInt("layoutPosition", layoutPosition)
                setFragmentResult("request_cloud_serial_number", result)
                findNavController().navigateUp()
            }
        }


        binding.imgBack.setOnClickListener {
            dismiss()
        }

    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }
}