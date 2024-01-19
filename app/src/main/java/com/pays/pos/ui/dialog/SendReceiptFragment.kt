package com.pays.pos.ui.dialog

import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.style.UnderlineSpan
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.data.remote.Constants.END_DATE
import com.pays.pos.data.remote.Constants.START_DATE
import com.pays.pos.databinding.DailogSendReceiptBinding
import com.pays.pos.ui.fragments.payment.OrderCompleteViewModel
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.extensions.liveSnackBar
import com.pays.pos.utils.extensions.showAlert
import com.pays.pos.utils.extensions.visible
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class SendReceiptFragment : DialogFragment() {

    private var ETS: Boolean = false
    private var emailAddress: String? = null
    private var isEod: Boolean = false
    private var isFromTimeSheet: Boolean = false
    private lateinit var binding: DailogSendReceiptBinding
    var orderId: Int = 0
    var type: Int = 0

    private val viewModelOrder by viewModels<OrderCompleteViewModel>()

    companion object {
        fun newInstance() = SendReceiptFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DailogSendReceiptBinding.inflate(inflater, container, false)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        val back = ColorDrawable(ContextCompat.getColor(binding.root.context,R.color.bg_color))
        val inset = InsetDrawable(back, 150, 100, 150, 100)
        dialog?.window?.setBackgroundDrawable(inset);
        setupSnackbar()
        observe()



        isEod = requireArguments().getBoolean("EOD", false)
        isFromTimeSheet = requireArguments().getBoolean("isFromTimeSheet", false)
        ETS = requireArguments().getBoolean("ETS", false)

        if (isEod) {

            if (ETS){
                binding.txtAmount.text = "Email employee tip summary"

            }else {
                binding.txtAmount.text = "Email end of the day report"

            }

            emailAddress = requireArguments().getString("email")
            if (emailAddress != null)
                binding.edtEmail.setText(emailAddress)
        }

        if (!isEod)
            orderId = requireArguments().getInt("orderId")
        type = requireArguments().getInt("type", 0)


        if (isFromTimeSheet) {
            emailAddress = requireArguments().getString("email")
            if (emailAddress != null)
                binding.edtEmail.setText(emailAddress)
        }

        if (type == 1) {
            binding.edtPhoneNo.visible()
        } else if (type == 2) {
            binding.linearEditEmail?.visible()
        }

        binding.edtEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                for (span in s!!.getSpans(0, s.length, UnderlineSpan::class.java)) {
                    s.removeSpan(span)
                }
            }

        })
        binding.imgBack.setOnClickListener {
            dismiss()
        }
        binding.txtSend.setOnClickListener {

            MethodUtils.hideKeyboard(requireActivity())

            if (isFromTimeSheet) {
                if (binding.edtEmail.text.toString().trim().isEmpty()) {
                    it.showAlert(getString(R.string.email_validate))
                } else {

                    val result = Bundle().apply {
                        putString("email", binding.edtEmail.text.toString().trim())
                    }
                    setFragmentResult("request_key_timesheet", result)
                    findNavController().navigateUp()
                    dismiss()
                }
            } else

                if (ETS){ // Employee Tip Summary


                    val startDate = requireArguments().getString(START_DATE)
                    val endDate = requireArguments().getString(END_DATE)

                   //findNavController().navigateUp()
                   //dismiss()

                    viewModelOrder.sendMailForETS(
                        binding.edtEmail.text.toString().trim(),
                        startDate!!,
                        endDate!!

                    )



                }else if (isEod) {

                    if (binding.edtEmail.text.toString().trim().isEmpty()) {
                        it.showAlert(getString(R.string.email_validate))
                    } else {

                        val result = Bundle().apply {
                            putString("email", binding.edtEmail.text.toString().trim())
                        }
                        setFragmentResult("request_key_eod", result)
                        findNavController().navigateUp()
                        dismiss()

                        viewModelOrder.submit(
                            if (type == 1) "Message" else "Email",
                            binding.edtEmail.text.toString().trim(),
                            binding.edtPhoneNo.text.toString().trim(),
                            orderId
                        )
                    }



                } else {

                    viewModelOrder.submit(
                        if (type == 1) "Message" else "Email",
                        binding.edtEmail.text.toString().trim(),
                        binding.edtPhoneNo.text.toString().trim(),
                        orderId
                    )
                }
        }
        return binding.root
    }

    private fun observe() {

        viewModelOrder.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {

                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }

            }
        }

        viewModelOrder.data.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { baseResponse ->
                activity?.let {

                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, baseResponse.message
                    ) { _, _ ->
                        dismiss()
                    }

                    if (viewModelOrder.itsFromETP){
                        viewModelOrder.itsFromETP = false
                        findNavController().navigateUp()
                        dismiss()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val window: Window? = dialog!!.window
        val size = Point()
        val display: Display = window?.windowManager?.defaultDisplay!!
        display.getSize(size)
        val width: Int = size.x
        window.setLayout((width * 0.50).toInt(), WindowManager.LayoutParams.MATCH_PARENT)
        window.setGravity(Gravity.CENTER)
    }

    private fun setupSnackbar() {
        binding.root.liveSnackBar(this, viewModelOrder.snackbarText, Snackbar.LENGTH_SHORT)

    }
}