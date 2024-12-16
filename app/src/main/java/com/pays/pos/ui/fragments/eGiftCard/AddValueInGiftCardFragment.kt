package com.pays.pos.ui.fragments.eGiftCard

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.pax.poslink.ManageRequest
import com.pax.poslink.PosLink
import com.pax.poslink.ProcessTransResult
import com.pays.pos.R
import com.pays.pos.data.entities.CartModel
import com.pays.pos.data.entities.TbCartItem
import com.pays.pos.data.model.requestModel.giftCard.request.GiftCardCheckBalanceRequest
import com.pays.pos.data.model.requestModel.giftCard.response.GiftCardCheckBalanceResponse
import com.pays.pos.data.remote.Constants
import com.pays.pos.databinding.FragmentAddValueInGiftCardBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.pays.pos.utils.*
import com.pays.pos.utils.extensions.runOnUiThread
import com.pays.pos.utils.extensions.setOnSingleClickListener
import com.pays.pos.utils.paxUtils.SettingINI
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class AddValueInGiftCardFragment : Fragment() {

    private lateinit var binding: FragmentAddValueInGiftCardBinding

    @Inject
    lateinit var prefProvider: PrefProvider
    private val dashboardViewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val giftCardViewModel: GiftCardViewModel by viewModels()
    private val TAG = "AddValueInGiftCardFragment"
    private var giftCardNumberGlb =""
    private var posLink: PosLink = PosLink()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddValueInGiftCardBinding.inflate(layoutInflater)
        setDefaultAmountsInKeypad()
        setKeyPad()
        onClick()
        setObservables()
        setupSnackbar()
        setupProgress()
        startPAXTestWithGiftCard()
        return binding.root
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
                    runOnUiThread(Runnable {
                        with(binding){
                            edtGiftCardNumber.text?.clear()
                            edtGiftCardNumber.setText(response.PAN.toString())
                        }
                    })
                }else{

                }

            }

        }
    }

    private fun setupProgress() {
        giftCardViewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event?.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }
    }

    private fun setupSnackbar() {
        giftCardViewModel.snackbarText.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                AlertUtils.showCustomAlert(
                    requireActivity(),
                    it.toString()
                )
            }

        }
    }

    private fun setObservables() {
        dashboardViewModel.physicalcardexistsornot.observe(viewLifecycleOwner,{
            it.getContentIfNotHandled()?.let {

                Log.e(TAG,"checkIT:  ${it}")
                if (it == 200){

                    AlertUtils.showCustomAlert(requireContext(),"No gift card found with this number.")
                }else{
                    giftCardViewModel.giftCardCheckBalance(
                        GiftCardCheckBalanceRequest(
                            giftCardNumberGlb
                        )
                    )

                }
            }

        })


        giftCardViewModel.giftCardCheckBalanceData.observe(
            viewLifecycleOwner,
            object : Observer<Event<GiftCardCheckBalanceResponse?>> {
                override fun onChanged(t: Event<GiftCardCheckBalanceResponse?>?) {
                    t?.getContentIfNotHandled()?.let {
                        if (it.data == null) {
                            context?.let { ctx ->
                                AlertUtils.showCustomAlertWithListenerWithOK(ctx, it.message, object: DialogInterface.OnClickListener{
                                    override fun onClick(p0: DialogInterface?, p1: Int) {
                                        focusAndOpenKeyboard()
                                    }

                                    private fun focusAndOpenKeyboard() {
                                        runOnUiThread(kotlinx.coroutines.Runnable {
                                            binding.run {
                                                edtGiftCardNumber.requestFocus()
                                                val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                                imm.showSoftInput(edtGiftCardNumber, InputMethodManager.SHOW_IMPLICIT)
                                            }
                                        })
                                    }
                                })
                            }

                        } else {
                            moveToCheckout()
                        }
                    }
                }
            })

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.edtAmount.addTextChangedListener(
            AmountTextWatcher(
                binding.edtAmount,
                true,
                isFromGiftCard = true
            )
        )


    }

    private fun setDefaultAmountsInKeypad() {
        binding.apply {
            llKeypad.apply {
                txt10.text = "$25"
                txt20.text = "$30"
                txt30.text = "$35"
            }
        }
    }

    private fun setKeyPad() {
        binding.llKeypad.tvOne.setOnClickListener {
            calculateValue("1", false)
        }

        binding.llKeypad.tvTwo.setOnClickListener {
            calculateValue("2", false)
        }

        binding.llKeypad.tvThree.setOnClickListener {
            calculateValue("3", false)
        }

        binding.llKeypad.tvFour.setOnClickListener {
            calculateValue("4", false)
        }

        binding.llKeypad.tvFive.setOnClickListener {
            calculateValue("5", false)
        }

        binding.llKeypad.tvSix.setOnClickListener {
            calculateValue("6", false)
        }

        binding.llKeypad.tvSeven.setOnClickListener {
            calculateValue("7", false)
        }

        binding.llKeypad.tvEight.setOnClickListener {
            calculateValue("8", false)
        }

        binding.llKeypad.tvNine.setOnClickListener {
            calculateValue("9", false)
        }

        binding.llKeypad.tvZero.setOnClickListener {
            calculateValue("0", false)
        }

        binding.llKeypad.tvClear.setOnClickListener {
            calculateValue("", true)
        }

        binding.llKeypad.tvDZero.setOnClickListener {
            calculateValue("00", false)
        }

        binding.llKeypad.txtClear.setOnClickListener {
            binding.edtAmount.setText("0.00")
        }

    }

    private fun calculateValue(number: String, delete: Boolean) {
        if (binding.edtAmount.text?.length!! > 1 && delete) {
            binding.edtAmount.setText(removeLastCharacter(binding.edtAmount.text.toString()))
        } else {
            binding.edtAmount.append(number)
        }
    }

    private fun removeLastCharacter(str: String): String {
        return str.substring(0, str.length - 1)
    }

    private fun closePaxRequest(){
        countDownTimer?.cancel()
        countDownTimer=null
        try{
            posLink.CancelTrans()
        }catch (e:Exception){}
    }

    override fun onStop() {
        closePaxRequest()
        super.onStop()
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
                            binding.btnReadCard?.isClickable=false
                        }
                    }.start()

                    startPAXTestWithGiftCard()
                }
            })
        }


        binding.imgBack.setOnClickListener {
            clearCartOnBackPress()
            findNavController().popBackStack()
        }

        binding.txtNext.setOnClickListener {
            closePaxRequest()
            MethodUtils.hideSoftKeyboard(requireActivity())
            val amount = binding.edtAmount.text.toString().replace("$", "").trim().toDouble()
            val giftCardNumber = binding.edtGiftCardNumber.text.toString().replace(" ", "")
            giftCardNumberGlb = giftCardNumber

            if(giftCardNumber.length < 8){
                AlertUtils.showCustomAlert(requireContext(), "Please enter 8-digit gift card number.")
                return@setOnClickListener
            }else if (giftCardNumber.length>8 && giftCardNumber.length<13){
                AlertUtils.showCustomAlert(requireContext(), "Invalid Gift Card Number.")
                return@setOnClickListener
            }

            else if (amount <= 0.0) {
                AlertUtils.showCustomAlert(requireContext(), "Please enter amount")
                return@setOnClickListener
            }

            else {
                if (giftCardNumber.length > 8){
                    prefProvider.setValue(Constants.PHYSICAL_GIFT_CARD_NUMBER,giftCardNumber)
                    prefProvider.setValue(Constants.GIFT_CARD_TYPE,"Physical")
                }
                else{
                    prefProvider.setValue(Constants.GIFT_CARD_TYPE,"Digital")

                }


                prefProvider.setValue(Constants.GIFT_CARD_PURCHASE_AMOUNT, amount.toString())
                prefProvider.setValue(Constants.GIFT_CARD_NUMBER, giftCardNumber)
                prefProvider.setValueboolean(Constants.IS_ADD_VALUE_IN_GIFT_CARD, true)

                activity?.let {
                    if (InternetUtils.isInternetAvailable(it.applicationContext)) {
                        dashboardViewModel.checkCardExistOrNot(giftCardNumber)

                       /* giftCardViewModel.giftCardCheckBalance(
                            GiftCardCheckBalanceRequest(
                                giftCardNumber
                            )
                        )
                        */
                    }
                }

//                moveToCheckout()
            }
        }

        binding.llKeypad.txt10.setOnClickListener {
            binding.edtAmount.setText(MethodUtils.roundOffAmount(25.0))
        }

        binding.llKeypad.txt20.setOnClickListener {
            binding.edtAmount.setText(MethodUtils.roundOffAmount(30.0))
        }

        binding.llKeypad.txt30.setOnClickListener {
            binding.edtAmount.setText(MethodUtils.roundOffAmount(35.0))
        }
    }

    //delete cart and clear local data on back press
    private fun clearCartOnBackPress() {
        prefProvider.setValue("PaidAmount", "")
        prefProvider.setValue(Constants.WHOLE_AMOUNT, "")
        prefProvider.setValueInt("cardCount", 0)
        prefProvider.setValue(Constants.SUB_TOTAL, "")
        prefProvider.setValue(Constants.CASH_DISCOUNT_SURCHARGE, "")
        prefProvider.setValue(Constants.TOTAL_DISCOUNT, "")
        prefProvider.setValue(Constants.TIP, "")
        prefProvider.setValue(Constants.TAX_CHARGE, "")
        prefProvider.setValue(Constants.SERVICE_CHARGE, "")
        prefProvider.setValue(Constants.ORDER_TYPE, "")
        prefProvider.setValue(Constants.ORDER_TYPE_NAME, "")
        prefProvider.setValueboolean(Constants.IS_ADD_VALUE_IN_GIFT_CARD, false)

        dashboardViewModel.deleteCart()
    }

    // Move to checkout screen with added gift card details to make payment
    private fun moveToCheckout() {

        prefProvider.setValue(Constants.CUSTOMER_NAME, "")
        prefProvider.setValue(Constants.RECEIPT_CUSTOMER_NAME, "")

        prefProvider.setValueInt(Constants.CUSTOMER_ID, -1)

        prefProvider.saveCustomerData(null)

        prefProvider.setValue("PaidAmount", "")
        prefProvider.setValue(Constants.WHOLE_AMOUNT, "")
        prefProvider.setValueInt("cardCount", 0)
        prefProvider.setValue(Constants.SUB_TOTAL, "")
        prefProvider.setValue(Constants.CASH_DISCOUNT_SURCHARGE, "")
        prefProvider.setValue(Constants.TOTAL_DISCOUNT, "")
        prefProvider.setValue(Constants.TIP, "")
        prefProvider.setValue(Constants.TAX_CHARGE, "")
        prefProvider.setValue(Constants.SERVICE_CHARGE, "")

        dashboardViewModel.deleteCart()

        prefProvider.setValue(Constants.ORDER_TYPE, Constants.GIFT_CARD)
        prefProvider.setValue(Constants.ORDER_TYPE_NAME, Constants.GIFT_CARD_NAME)

        val cm = CartModel()
        val tbItem = TbCartItem()
        tbItem.name = "${prefProvider.getValue(Constants.GIFT_CARD_TYPE,"Digital")} Gift Card"
        tbItem.quantity = 1
        tbItem.itemQuantity = 1
        val totalPrice = binding.edtAmount.text.toString().replace("$", "").trim().toDouble()
        tbItem.price = totalPrice
        tbItem.employeeID = prefProvider.getValueInt(Constants.EMPLOYEE_ID, -1)
        tbItem.orderTypeId = 0
        tbItem.orderType = Constants.GIFT_CARD
        tbItem.orderTypeName = Constants.GIFT_CARD

        cm.apply {
            employeeID = prefProvider.getValueInt(Constants.EMPLOYEE_ID, -1)
            terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, -1)
            isOpenOrder = false
            orderTypeId = 0
            orderType = Constants.GIFT_CARD
            orderTypeName = Constants.GIFT_CARD
        }

        Log.e(TAG, "tbItem:  ${Gson().toJson(tbItem)}")
        dashboardViewModel.addCart(cm)

        dashboardViewModel.addItemToCartItems(tbItem)

        val bundle = Bundle()
        bundle.putBoolean("update", true)
        bundle.putDouble("totalPrice", totalPrice)
        bundle.putDouble("finalprice", totalPrice)
        bundle.putDouble(
            "cashDiscountSurcharge",
            MethodUtils.calculateCashDiscount(
                totalPrice,
                prefProvider,
                requireContext()
            )
        )
        bundle.putDouble("subTotalPrice", totalPrice)
        bundle.putDouble("totalTax", 0.0)
        bundle.putDouble("totalDiscount", 0.0)
        bundle.putDouble("totalServiceCharge", 0.0)
        bundle.putParcelable("cartList", cm)
        bundle.putBoolean("isFromPayment", true)

        findNavController().navigate(
            R.id.action_addValueInGiftCard_to_paymentBoldPosFragment,
            bundle
        )
    }

}