package com.pays.pos.ui.fragments.eGiftCard

import android.content.Context
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
import com.google.gson.Gson
import com.pax.poslink.ManageRequest
import com.pax.poslink.PosLink
import com.pax.poslink.ProcessTransResult
import com.pays.pos.R
import com.pays.pos.data.entities.CartModel
import com.pays.pos.data.entities.TbCartItem
import com.pays.pos.data.entities.TbCustomer
import com.pays.pos.data.remote.Constants
import com.pays.pos.databinding.FragmentPlasticCardNumberBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.extensions.invisible
import com.pays.pos.utils.extensions.runOnUiThread
import com.pays.pos.utils.extensions.setOnSingleClickListener
import com.pays.pos.utils.paxUtils.SettingINI
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import javax.inject.Inject

@AndroidEntryPoint
class PlasticCardNumber : Fragment() {

    @Inject
    lateinit var prefProvider: PrefProvider
    private lateinit var binding: FragmentPlasticCardNumberBinding
    private val dashboardViewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val TAG = "PlasticCardNumber"
    private var customer: TbCustomer? = null
    private var posLink: PosLink = PosLink()
    var fromPAXSwipe:Boolean=false
    lateinit var weakContext:WeakReference<Context>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPlasticCardNumberBinding.inflate(inflater, container, false)
        binding.llKeypad?.tvDot?.invisible()
        weakContext= WeakReference(requireActivity())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getData()
        onClick()
        onClickKeypad()
        obserVer()
//        startPAXTestWithGiftCard()

    }
    private var countDownTimer: CountDownTimer? = null

    private fun closePaxRequest(){
        countDownTimer?.cancel()
        countDownTimer=null
//        try{
//            posLink.CancelTrans()
//        }catch (e:Exception){}
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
            manageRequest.EDCType = manageRequest.ParseEDCType("GIFT")
            manageRequest.MagneticSwipeEntryFlag = "1";
            manageRequest.ManualEntryFlag = "1";
            manageRequest.ContactlessEntryFlag = "0";
            manageRequest.TimeOut = "200";
            manageRequest.ContinuousScreen = "0";
            manageRequest.ECRRefNum = System.currentTimeMillis()
                .toString(); // Enable swipe entry (adjust based on your use case)
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
                            var cardValue=""
                            if (response.PAN.isNullOrEmpty()){
                                Log.d("VALID: ", "Here__Track: ${response.Track2Data.toString()}")
                                cardValue=response.Track2Data.toString()
                            }else{
                                cardValue=response.PAN.toString()
                                Log.d("VALID: ", "Here__Pan: ${response.PAN.toString()}")
                            }

                            if (!cardValue.contains('*')){
                                fromPAXSwipe=true
                                edtAmount?.setText(cardValue)
                                startProcessingWithGiftcard()
                            }else{
                                AlertUtils.showCustomAlert(requireContext(), getString(R.string.invalid_card))
                            }
                        }
                    }

                    /*runOnUiThread(Runnable {
                        with(binding) {
                            edtAmount?.text?.clear()
                            edtAmount?.setText(response.PAN.toString())

                        }
                    })*/
                } else {
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

    private fun obserVer() {

        dashboardViewModel.physicalcardexistsornot.observe(viewLifecycleOwner, {
            it.getContentIfNotHandled()?.let {

                if (it != 200) {

                    AlertUtils.showCustomAlert(
                        requireContext(),
                        "Gift Card number has already been taken."
                    )
                } else {
                    prefProvider.setValue(
                        Constants.CUSTOMER_NAME,
                        customer?.first_name + " " + customer?.last_name
                    )

                    prefProvider.setValue(
                        Constants.PHYSICAL_GIFT_CARD_NUMBER,
                        binding.edtAmount?.text.toString().trim()
                    )

                    prefProvider.setValue(
                        Constants.RECEIPT_CUSTOMER_NAME,
                        customer?.first_name + " " + customer?.last_name
                    )
                    var orderTypeIdFromDb: Int = 0
                    synchronized(this) {
                        CoroutineScope(Dispatchers.IO).launch {
                            orderTypeIdFromDb =
                                dashboardViewModel.orderTypeByName(Constants.TAKEOUT)
                        }
                    }
                    customer?.id?.let { prefProvider.setValueInt(Constants.CUSTOMER_ID, it) }

                    prefProvider.saveCustomerData(customer)

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
                    prefProvider.setValueboolean(Constants.IS_ADD_VALUE_IN_GIFT_CARD, false)

                    val cm = CartModel()
                    val tbItem = TbCartItem()
                    tbItem.name = "Physical Gift Card"
                    tbItem.quantity = 1
                    tbItem.itemQuantity = 1
                    val totalPrice =
                        prefProvider.getValue(Constants.GIFT_CARD_PURCHASE_AMOUNT, "0.0").toDouble()
                    tbItem.price = totalPrice
                    tbItem.employeeID = prefProvider.getValueInt(Constants.EMPLOYEE_ID, -1)
                    tbItem.orderTypeId = orderTypeIdFromDb
                    tbItem.orderType = Constants.GIFT_CARD
                    tbItem.orderTypeName = Constants.GIFT_CARD

                    cm.apply {
                        employeeID = prefProvider.getValueInt(Constants.EMPLOYEE_ID, -1)
                        terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, -1)
                        isOpenOrder = false
                        orderTypeId = orderTypeIdFromDb
                        orderType = Constants.GIFT_CARD
                        orderTypeName = Constants.GIFT_CARD
                        locationId = prefProvider.getLocationId()
                    }
                    Log.e(TAG, "checkItems: ${Gson().toJson(tbItem)}")

                    dashboardViewModel.addCart(cm)
                    dashboardViewModel.addItemToCartItems(tbItem)


                    CoroutineScope(Dispatchers.IO).launch {
                        delay(100)

                        runOnUiThread(kotlinx.coroutines.Runnable {
                            val bundle = Bundle()
                            bundle.putBoolean("update", true)
                            bundle.putDouble("totalPrice", totalPrice)
                            bundle.putDouble("finalprice", totalPrice)
                            bundle.putDouble("cashDiscountSurcharge", 0.0)
                            bundle.putDouble("subTotalPrice", totalPrice)
                            bundle.putDouble("totalTax", 0.0)
                            bundle.putDouble("totalDiscount", 0.0)
                            bundle.putDouble("totalServiceCharge", 0.0)
                            bundle.putParcelable("cartList", cm)

                            findNavController().navigate(
                                R.id.action_plasticCardNumber_to_paymentBoldPosFragment,
                                bundle
                            )
                        })
                    }

                }
            }

        })
    }

    private fun getData() {
        customer = requireArguments().getParcelable<TbCustomer>("customer")
        Log.e(TAG, "customerData: ${Gson().toJson(customer)}")

    }

    private fun onClickKeypad() {
        binding.llKeypad?.tvOne?.setOnClickListener {
            setCardNumber("1", false)

        }

        binding.llKeypad?.tvTwo?.setOnClickListener {
            setCardNumber("2", false)

        }

        binding.llKeypad?.tvThree?.setOnClickListener {
            setCardNumber("3", false)

        }

        binding.llKeypad?.tvFour?.setOnClickListener {
            setCardNumber("4", false)

        }

        binding.llKeypad?.tvFive?.setOnClickListener {
            setCardNumber("5", false)

        }

        binding.llKeypad?.tvSix?.setOnClickListener {
            setCardNumber("6", false)

        }

        binding.llKeypad?.tvSeven?.setOnClickListener {

            setCardNumber("7", false)
        }

        binding.llKeypad?.tvEight?.setOnClickListener {
            setCardNumber("8", false)

        }

        binding.llKeypad?.tvNine?.setOnClickListener {
            setCardNumber("9", false)

        }

        binding.llKeypad?.tvZero?.setOnClickListener {
            setCardNumber("0", false)

        }


    }

    private fun setCardNumber(value: String, isDelete: Boolean) {
        if (isDelete && binding.edtAmount?.text?.length!! >= 1) {

            binding.edtAmount?.setText(removeLastCharacter(binding.edtAmount?.text.toString()))
        } else if (binding.edtAmount?.text?.trim()!!
                .equals("") || binding.edtAmount?.text.toString()?.isEmpty()
        ) {
            binding.edtAmount?.setText("")
            binding.edtAmount?.append(value)
        } else {
            binding.edtAmount?.append(value)
        }


    }

    private fun showAlertDialog(message:String){
        runOnUiThread(kotlinx.coroutines.Runnable {
            AlertUtils.showCustomAlert(requireContext(), message)
        })
    }

    private fun onClick() {

        binding.btnReadCard?.let {
            it.setOnSingleClickListener(object:View.OnClickListener{
                override fun onClick(p0: View?) {
                    when(prefProvider.getValue(Constants.PAYMENT_GATEWAY_TYPE,"")){
                        Constants.PAX->{
                            if (prefProvider.getValueboolean(Constants.IS_PAX_CONNECTED,false)){
                                countDownTimer?.cancel()
                                binding.btnReadCard?.isClickable=false

                                countDownTimer = object : CountDownTimer(5000, 1000) {
                                    override fun onTick(millisUntilFinished: Long) {
                                    }
                                    override fun onFinish() {
                                        binding.btnReadCard?.isClickable=true
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

        binding.edtAmount?.setOnClickListener(object:View.OnClickListener{
            override fun onClick(p0: View?) {
                try{
                    posLink.CancelTrans()
                }catch (e:Exception){

                }
            }
        })

        binding.imgBack?.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.llKeypad?.tvBack?.setOnClickListener {
            setCardNumber("", true)

        }


        binding.txtNext?.setOnClickListener {
            if (fromPAXSwipe) {
                startProcessingWithGiftcard()
            }else{
                weakContext.get()?.let {
//                    AlertUtils.showCustomAlertWithListenerWithOK(it,getString(R.string.are_you_sure_proceed),object: DialogInterface.OnClickListener{
//                        override fun onClick(p0: DialogInterface?, p1: Int) {
//                            startProcessingWithGiftcard()
//                        }
//                    })

                    startProcessingWithGiftcard()
//                    AlertUtils.showCustomAlertWithListenerWithOKCancelUpdated(
//                        requireContext(),
//                        getString(R.string.are_you_sure_proceed),
//                        "Ok"
//                    ) { dialogInterface, clickedButton ->
//                        if (clickedButton == 0) {
//
//                        } else {
//                            dialogInterface?.dismiss()
//                        }
//                    }
                }
            }
        }


    }

    private fun startProcessingWithGiftcard() {
        if (binding.edtAmount?.text.toString().trim().length < 13) {
            AlertUtils.showCustomAlert(requireContext(), "Please enter Valid Gift Card number")
        } else {
            closePaxRequest()
            AlertUtils.showCustomAlertWithListenerWithOKCancelUpdated(
                requireContext(),
                getString(R.string.are_you_sure_proceed),
                "Ok"
            ) { dialogInterface, clickedButton ->
                if (clickedButton == 0) {
                    dashboardViewModel.checkCardExistOrNot(binding.edtAmount?.text.toString().trim())
                } else {
                    dialogInterface?.dismiss()
                }
            }

        }
    }

    private fun removeLastCharacter(str: String): String {
        Log.e("checkLength", "strLength:  ${str.length}")
        if (str.length == 1) {
            return ""
        } else {
            return str.substring(0, str.length - 1)
        }
    }

    override fun onStop() {
closePaxRequest()
        super.onStop()
    }
}