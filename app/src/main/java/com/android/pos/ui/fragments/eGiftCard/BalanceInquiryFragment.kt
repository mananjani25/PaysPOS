package com.android.pos.ui.fragments.eGiftCard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.requestModel.giftCard.request.GiftCardCheckBalanceRequest
import com.android.pos.databinding.FragmentBalanceInquiryBinding
import com.android.pos.ui.fragments.payment.PaymentViewModel
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.LogUtil
import com.android.pos.utils.MethodUtils.Companion.toPrecision
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.gone
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BalanceInquiryFragment : Fragment() {

    private lateinit var binding: FragmentBalanceInquiryBinding
    private val giftCardViewModel by activityViewModels<GiftCardViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBalanceInquiryBinding.inflate(layoutInflater)
        hideDefaultKeypads()
        onClick()
        showProgressObserver()
        return binding.root
    }

    private fun showProgressObserver() {
        giftCardViewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                LogUtil.logE("observeShowProgress", it.toString())
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }

        giftCardViewModel.giftCardCheckBalanceData.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if(it.data!=null){
                    AlertUtils.showCustomAlertWithTitleListenerWithOK(
                        requireContext(),
                        title = getString(R.string.msg_remaining_balance),
                        message = "$${it.data.amount.toPrecision(2)}"
                    ) { _, _ ->
                    }
                } else {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        requireContext(),
                        message = it.message
                    ) { _, _ ->
                    }
                }

            }
        }
    }

    private fun hideDefaultKeypads() {
        binding.apply {
            llKeypad.apply {
                txt10.gone()
                txt20.gone()
                txt30.gone()
                tvClear.gone()
            }
        }
    }

    private fun onClick() {

        binding.imgBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // to check balance of existing gift card
        binding.txtCheckBalance.setOnClickListener {

            val inputGiftCardNumber = binding.edtGiftCardNumber.text.toString().replace(" ","")

            if (inputGiftCardNumber.isNotEmpty() && inputGiftCardNumber.length == 8) {
                giftCardViewModel.giftCardCheckBalance(GiftCardCheckBalanceRequest(name = inputGiftCardNumber))
            } else {
                AlertUtils.showCustomAlert(requireContext(), "Please enter 8-digit gift card number")
                return@setOnClickListener
            }

        }
    }

}