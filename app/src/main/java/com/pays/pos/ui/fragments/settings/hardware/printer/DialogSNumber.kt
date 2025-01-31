package com.pays.pos.ui.fragments.settings.hardware.printer

import android.app.Dialog
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.pays.pos.data.model.PrinterListModel
import com.pays.pos.databinding.DialogPrinterSnumberBinding
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.MethodUtils.Companion.showKeyboard

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

        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        printerListModel = arguments?.getParcelable<PrinterListModel>("printerListModel")
        layoutPosition = arguments?.getInt("layoutPosition")?:0
        Log.e(TAG,"parcelableList:  ${Gson().toJson(printerListModel)}")
        Log.e(TAG,"layoutPosition:  ${layoutPosition}")

        setupKeyboard()
        setupTouchHandling()

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

            isCancelable = true
        }


        binding.imgBack.setOnClickListener {
            dismiss()
        }

        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

    }

    private fun setupKeyboard() {
        binding.edtSplitNo.requestFocus()
        binding.edtSplitNo.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
                imm?.showSoftInput(binding.edtSplitNo, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        // Automatically show the keyboard
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchHandling() {
        // Handle touch anywhere in the dialog to focus on the EditText
        binding.root.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                binding.edtSplitNo.requestFocus()
                val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
                imm?.showSoftInput(binding.edtSplitNo, InputMethodManager.SHOW_IMPLICIT)
            }
            false
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }
}