package com.android.pos.ui.dialog

import android.graphics.Point
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.requestModel.RefundRequestModel
import com.android.pos.data.model.responseModel.GetOrderDetailsResponse
import com.android.pos.databinding.DialogRefundReasonBinding
import com.android.pos.ui.fragments.transactions.TransactionDetailsViewModel
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.liveSnackBar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class ReasonForRefundDialog : DialogFragment() {

    private var refundAmount: Double = 0.0
    private lateinit var binding: DialogRefundReasonBinding
    private lateinit var refundData: RefundRequestModel
    private val viewModel by viewModels<TransactionDetailsViewModel>()

    companion object {
        fun newInstance() = ReasonForRefundDialog()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_refund_reason, container, false)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        refundData = arguments?.getParcelable("refundData")!!


        refundAmount = arguments?.getDouble("refundAmount")!!


        binding.tvTagRefundAmount.text = requireActivity()?.getString(R.string.tv_refund) + " " +
                requireActivity()?.getString(R.string.symbole) + "" + String.format(
            requireActivity().getString(R.string.format), refundAmount
        )


        binding.tvRefundAmount.text =
            requireActivity()?.getString(R.string.symbole) + " " + String.format(
                requireActivity().getString(R.string.format), refundAmount
            )


        binding.txtDone.setOnClickListener {
            viewModel.refundPaymentApiCall(
                refundAmount,
                refundData,
                binding.edtReasonForRefund.text.toString()
            )
        }

        setupSnackbar()
        observeShowProgress()
        navigate()

        return binding.root
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
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    }

    private fun observeShowProgress() {

        viewModel.showProgress.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        })

    }

    private fun navigate() {

        viewModel.dataRefundDone.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { createTaxResponse ->
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, createTaxResponse.message
                    ) { _, _ ->
                        findNavController().popBackStack(R.id.issueRefundFragment, true)
                        //  findNavController().navigateUp()
                    }
                }
            }
        })

    }


}