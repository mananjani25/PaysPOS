package com.android.pos.ui.dialog

import android.graphics.Point
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.databinding.DailogSendReceiptBinding
import com.android.pos.ui.fragments.payment.OrderCompleteViewModel
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.extensions.visible
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class SendReceiptFragment : DialogFragment() {

    private var isEod: Boolean = false
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
        binding = DataBindingUtil.inflate(inflater, R.layout.dailog_send_receipt, container, false)
        binding.lifecycleOwner = this
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        setupSnackbar()
        observe()



        isEod = requireArguments().getBoolean("EOD", false)
        if (!isEod)
            orderId = requireArguments().getInt("orderID")
        type = requireArguments().getInt("type")

        if (type == 1) {
            binding.edtPhoneNo.visible()
        } else if (type == 2) {
            binding.edtEmail.visible()
        }

        binding.imgBack.setOnClickListener {
            dismiss()
        }
        binding.txtSend.setOnClickListener {

            if (isEod) {

                if (binding.edtEmail.text.toString().trim().isEmpty()) {
                    it.showAlert(getString(R.string.email_validate))
                } else {

                    val result = Bundle().apply {
                        putString("email", binding.edtEmail.text.toString().trim())
                    }
                    setFragmentResult("request_key_eod", result)
                    findNavController().navigateUp()
                    dismiss()
                }

            } else {

                viewModelOrder.submit(
                    if (type == 1) "Message" else "Email", binding.edtEmail.text.toString().trim(),
                    binding.edtPhoneNo.text.toString().trim(), orderId
                )
            }
        }
        return binding.root
    }

    private fun observe() {

        viewModelOrder.showProgress.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        })

        viewModelOrder.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { baseResponse ->
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, baseResponse.message
                    ) { _, _ ->
                        dismiss()
                    }
                }
            }
        })
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