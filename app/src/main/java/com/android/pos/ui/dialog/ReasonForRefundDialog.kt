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
import com.android.pos.data.remote.Constants.DIALOG_KEY_VARIATION_DETAILS
import com.android.pos.data.remote.Constants.IS_CLOCKOUT
import com.android.pos.data.remote.Constants.IS_REFUND
import com.android.pos.databinding.DialogRefundReasonBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.fragments.transactions.TransactionDetailsViewModel
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.extensions.setNavigationResult
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class ReasonForRefundDialog : DialogFragment() {

    private var refundAmount: Double = 0.0
    private lateinit var binding: DialogRefundReasonBinding
    private lateinit var refundData: RefundRequestModel
    private val viewModel by viewModels<TransactionDetailsViewModel>()

    @Inject
    lateinit var prefProvider: PrefProvider

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

        binding.imgBack.setOnClickListener {
            findNavController().navigateUp()
        }

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
                        //  prefProvider.setValueboolean(IS_REFUND, true)

                        val bundle = Bundle().apply {
                            putInt("orderId", refundData.paymentRefund?.orderId!!)
                        }

                        findNavController().navigate(
                            R.id.action_reasonForRefundDialog_to_transactionDetailsFragment, bundle
                        )
                        //  findNavController().navigateUp()
                    }
                }
            }
        })

    }


}