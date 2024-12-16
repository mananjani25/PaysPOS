package com.pays.pos.ui.fragments.eGiftCard

import android.os.Bundle
import android.os.CountDownTimer
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.pax.poslink.ManageRequest
import com.pax.poslink.PosLink
import com.pax.poslink.ProcessTransResult
import com.pays.pos.R
import com.pays.pos.data.model.requestModel.giftCard.request.GiftCardCheckBalanceRequest
import com.pays.pos.data.remote.Constants
import com.pays.pos.databinding.FragmentBalanceInquiryBinding
import com.pays.pos.ui.fragments.payment.PaymentViewModel
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils.Companion.toPrecision
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.runOnUiThread
import com.pays.pos.utils.extensions.setOnSingleClickListener
import com.pays.pos.utils.paxUtils.SettingINI
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BalanceInquiryFragment : Fragment() {

    private lateinit var binding: FragmentBalanceInquiryBinding
    private val giftCardViewModel by activityViewModels<GiftCardViewModel>()

    private var posLink: PosLink = PosLink()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBalanceInquiryBinding.inflate(layoutInflater)
        hideDefaultKeypads()
        onClick()
        showProgressObserver()
//        startPAXTestWithGiftCard()
        return binding.root
    }

    private fun closePaxRequest(){
        countDownTimer?.cancel()
        countDownTimer=null
//        try{
//            posLink.CancelTrans()
//        }catch (e:Exception){}
    }

    override fun onStop() {
        closePaxRequest()
        super.onStop()
    }
    private fun startPAXTestWithGiftCard() {
        GlobalScope.launch {
            posLink.SetCommSetting(
                SettingINI.getCommSettingFromFile(
                    requireContext(),
                    "/storage/emulated/0/Download/" + SettingINI.FILENAME
                )
            )

            val manageRequest = ManageRequest()
            manageRequest.TransType = manageRequest.ParseTransType("INPUTACCOUNT")
            manageRequest.EDCType=manageRequest.ParseEDCType("GIFT")
            manageRequest.MagneticSwipeEntryFlag = "1";
            manageRequest.ManualEntryFlag = "1";
            manageRequest.ContactlessEntryFlag = "0";
            manageRequest.TimeOut = "1000";
            manageRequest.ContinuousScreen = "0";
            manageRequest.ECRRefNum = System.currentTimeMillis().toString(); // Enable swipe entry (adjust based on your use case)
            posLink.ManageRequest = manageRequest
            val result = posLink.ProcessTrans()

            if (result.Code === ProcessTransResult.ProcessTransResultCode.OK) {
                val msg = Message()
                msg.what = Constants.TRANSACTION_SUCCESSED
                msg.obj = posLink.ManageResponse
                val response = msg.obj as com.pax.poslink.ManageResponse
                val resultCode = response.ResultCode

                if (resultCode == "000000") {
                    runOnUiThread(kotlinx.coroutines.Runnable {
                        with(binding) {
                            edtGiftCardNumber.text?.clear()
                            edtGiftCardNumber.setText(response.PAN.toString())
                            checkBalanceEnquiryForGiftcard()
                        }
                    })
                }else{

                }

            }

        }
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

        giftCardViewModel.showGiftCardProgress.observe(viewLifecycleOwner){event ->
            event.getContentIfNotHandled()?.let {
                Log.e("ObserverdGiftCardProgress",it.toString())
                if (it){
                    ProgressUtils.showProgressDialog(requireActivity())

                }
                else{
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


    private var countDownTimer: CountDownTimer? = null

    private fun onClick() {

        binding.btnReadCard?.let {
            it.setOnSingleClickListener(object:View.OnClickListener{
                override fun onClick(p0: View?) {

                    countDownTimer?.cancel()
                    binding.btnReadCard?.isClickable=false

                    countDownTimer = object : CountDownTimer(5000, 1000) {
                        override fun onTick(millisUntilFinished: Long) {
                        }
                        override fun onFinish() {
                            binding.btnReadCard?.isClickable=true
                        }
                    }.start()
                    startPAXTestWithGiftCard()
                }
            })
        }
        binding.imgBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // to check balance of existing gift card
        binding.txtCheckBalance.setOnClickListener {
            checkBalanceEnquiryForGiftcard()
        }
    }

    private fun checkBalanceEnquiryForGiftcard() {
        val inputGiftCardNumber = binding.edtGiftCardNumber.text.toString().replace(" ","")

        if (inputGiftCardNumber.isNotEmpty() && inputGiftCardNumber.length == 8) {
            giftCardViewModel.giftCardCheckBalance(GiftCardCheckBalanceRequest(name = inputGiftCardNumber))
        }
        else if(inputGiftCardNumber.isNotEmpty() && (inputGiftCardNumber.length == 13 || inputGiftCardNumber.length == 17)){
//                closePaxRequest()
            giftCardViewModel.physcialGiftCardCheckBalance(GiftCardCheckBalanceRequest(name = inputGiftCardNumber))

        }
        else {
            AlertUtils.showCustomAlert(requireContext(), "Please enter 8-digit gift card number.")
            return
        }
    }

}