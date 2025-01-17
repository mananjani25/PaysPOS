package com.pays.pos.ui.fragments.eGiftCard

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Message
import android.text.Editable
import android.text.TextWatcher
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
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils.Companion.toPrecision
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.TimeFormatUtils.prefProvider
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.runOnUiThread
import com.pays.pos.utils.extensions.setOnSingleClickListener
import com.pays.pos.utils.paxUtils.SettingINI
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import javax.inject.Inject

@AndroidEntryPoint
class BalanceInquiryFragment : Fragment() {

    private lateinit var binding: FragmentBalanceInquiryBinding
    private val giftCardViewModel by activityViewModels<GiftCardViewModel>()

    @Inject
    lateinit var prefProvider:PrefProvider

    var fromPAXSwipe:Boolean=false
    private var posLink: PosLink = PosLink()
    lateinit var weakContext: WeakReference<Context>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBalanceInquiryBinding.inflate(layoutInflater)
        weakContext= WeakReference(requireActivity())
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
    private fun startPAXWithGiftCard() {
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
            manageRequest.TimeOut = "200";
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
                    withContext(Dispatchers.Main){
                        binding.apply {
                            btnReadCard?.isClickable = true
                            fromPAXSwipe=true
                            if (response.PAN.isNullOrEmpty()){
                                edtGiftCardNumber.setText(response.Track2Data.toString())
                                Log.d("VALID: ", "Here__Track: ${response.Track2Data.toString()}")
                            }else{
                                edtGiftCardNumber.setText(response.PAN.toString())
                                Log.d("VALID: ", "Here__Pan: ${response.PAN.toString()}")
                            }
                            checkBalanceEnquiryForGiftcard()
                        }
                    }
                }else{
                    runOnUiThread(object : Runnable {
                        override fun run() {
                            binding.btnReadCard?.isClickable = true
                        }
                    })
                }

            }else{
                runOnUiThread(object : Runnable {
                    override fun run() {
                        binding.btnReadCard?.isClickable = true
                    }
                })
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
                        giftCardViewModel.clearGiftCardObserver()
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
                    when(prefProvider.getValue(Constants.PAYMENT_GATEWAY_TYPE,"")){
                        Constants.PAX->{
                            if (prefProvider.getValueboolean(
                                    Constants.IS_PAX_CONNECTED,
                                    false
                                )) {
                                countDownTimer?.cancel()
                                binding.btnReadCard?.isClickable = false

                                countDownTimer = object : CountDownTimer(5000, 1000) {
                                    override fun onTick(millisUntilFinished: Long) {
                                    }

                                    override fun onFinish() {
                                        binding.btnReadCard?.isClickable = true
                                    }
                                }.start()
                                startPAXWithGiftCard()
                            }else{
                                showAlertDialog(getString(R.string.please_connect_pax))
                            }
                        }
                        Constants.DEJAVOO->{
                            showAlertDialog(getString(R.string._not_supported,Constants.DEJAVOO))
                        }
                        Constants.VALOR->{
                            showAlertDialog(getString(R.string._not_supported,Constants.VALOR))
                        }
                        else->{
                            showAlertDialog(getString(R.string.please_connect_payment_device))
                        }
                    }

                }
            })
        }
        binding.imgBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // to check balance of existing gift card
        binding.txtCheckBalance.setOnClickListener {
            Log.d("VALID: ", "txtCheckBalance Called")
            if (fromPAXSwipe){
                checkBalanceEnquiryForGiftcard()
            }else{
                weakContext.get()?.let {
//                    AlertUtils.showCustomAlertWithListenerWithOK(it,getString(R.string.are_you_sure_proceed),object:DialogInterface.OnClickListener{
//                        override fun onClick(p0: DialogInterface?, p1: Int) {
//                            checkBalanceEnquiryForGiftcard()
//                        }
//                    })

                    val inputGiftCardNumber = binding.edtGiftCardNumber.text.toString().replace(" ","")
                    if (inputGiftCardNumber.isNotEmpty() && inputGiftCardNumber.length == 8) {
                        Log.d("VALID: ", "Here_1")
                        AlertUtils.showCustomAlertWithListenerWithOKCancelUpdated(
                            requireContext(),
                            getString(R.string.are_you_sure_proceed),
                            "Ok"
                        ) { dialogInterface, clickedButton ->
                            if (clickedButton == 0) {
                                // Perform the OK action: call `checkBalanceEnquiryForGiftcard()`
                                checkBalanceEnquiryForGiftcard()
                            } else {
                                // Perform the Cancel action: dismiss the dialog
                                dialogInterface?.dismiss()
                            }
                        }
                    }
                    else if(inputGiftCardNumber.isNotEmpty() && (inputGiftCardNumber.length == 13 || inputGiftCardNumber.length == 17)){
//                closePaxRequest()
                        Log.d("VALID: ", "Here_2")
                        AlertUtils.showCustomAlertWithListenerWithOKCancelUpdated(
                            requireContext(),
                            getString(R.string.are_you_sure_proceed),
                            "Ok"
                        ) { dialogInterface, clickedButton ->
                            if (clickedButton == 0) {
                                // Perform the OK action: call `checkBalanceEnquiryForGiftcard()`
                                checkBalanceEnquiryForGiftcard()
                            } else {
                                // Perform the Cancel action: dismiss the dialog
                                dialogInterface?.dismiss()
                            }
                        }

                    }
                    else {
                        Log.d("VALID: ", "Here_3")
                        AlertUtils.showCustomAlert(requireContext(), "Please enter 8-digit gift card number.")
                    }



                }
            }
        }
    }

    private fun showAlertDialog(message:String){
        runOnUiThread(kotlinx.coroutines.Runnable {
            AlertUtils.showCustomAlert(requireContext(), message)
        })
    }

    private fun checkBalanceEnquiryForGiftcard() {
        val inputGiftCardNumber = binding.edtGiftCardNumber.text.toString().replace(" ","")
        Log.d("VALID: ", "${inputGiftCardNumber.toString()}")
        if (inputGiftCardNumber.isNotEmpty() && inputGiftCardNumber.length == 8) {
            Log.d("VALID: ", "Here_1")
            giftCardViewModel.giftCardCheckBalance(GiftCardCheckBalanceRequest(name = inputGiftCardNumber))
        }
        else if(inputGiftCardNumber.isNotEmpty() && (inputGiftCardNumber.length == 13 || inputGiftCardNumber.length == 17)){
//                closePaxRequest()
            Log.d("VALID: ", "Here_2")
            giftCardViewModel.physcialGiftCardCheckBalance(GiftCardCheckBalanceRequest(name = inputGiftCardNumber))

        }
        else {
            Log.d("VALID: ", "Here_3")
            AlertUtils.showCustomAlert(requireContext(), "Please enter 8-digit gift card number.")
            return
        }
    }

}