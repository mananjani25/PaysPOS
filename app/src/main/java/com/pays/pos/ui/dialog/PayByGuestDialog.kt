package com.pays.pos.ui.dialog

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.pays.pos.MainApplication
import com.pays.pos.R
import com.pays.pos.data.entities.CartModel
import com.pays.pos.data.model.DineInModel
import com.pays.pos.data.model.requestModel.*
import com.pays.pos.data.model.responseModel.GetFloorPlanResponse
import com.pays.pos.data.model.responseModel.GetOrderDetailsResponse
import com.pays.pos.data.model.responseModel.GuestPaymentAttributes
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.CASH_DISCOUNT_SURCHARGE_DINEIN
import com.pays.pos.data.remote.Constants.DINE_IN_DISCOUNT
import com.pays.pos.data.remote.Constants.DINE_IN_SERVICECHARGE
import com.pays.pos.data.remote.Constants.DINE_IN_SUBTOTAL
import com.pays.pos.data.remote.Constants.DINE_IN_TAX
import com.pays.pos.data.remote.Constants.EMPLOYEE_ID
import com.pays.pos.data.remote.Constants.LOCATION_ID
import com.pays.pos.data.remote.Constants.ORDER_TYPE_ID
import com.pays.pos.data.remote.Constants.SERVICE_CHARGE_DINEIN
import com.pays.pos.data.remote.Constants.SUB_TOTAL_DINEIN
import com.pays.pos.data.remote.Constants.TAX_CHARGE_DINEIN
import com.pays.pos.data.remote.Constants.TERMINAL_ID
import com.pays.pos.data.remote.Constants.TIPS_AMOUNT_DINEIN
import com.pays.pos.data.remote.Constants.TOTAL_DISCOUNT_DINEIN
import com.pays.pos.data.remote.Constants.TOTAL_PRICE_DINEIN
import com.pays.pos.databinding.DialogPayByGuestBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.logger.MessageEvent
import com.pays.pos.ui.fragments.dinein.DineInOrderTableViewModel
import com.pays.pos.ui.fragments.payment.PaymentViewModel
import com.pays.pos.utils.*
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.floor

@AndroidEntryPoint
open class PayByGuestDialog : Fragment(), View.OnClickListener {

    private var tipID: Int? = null
    private var dineInAdapterList: ArrayList<DineInModel>? = null
    private var isGuestPay: Boolean = false
    private lateinit var binding: DialogPayByGuestBinding
    private var remainingAmount: Double = 0.0
    private val viewModel by viewModels<DineInOrderTableViewModel>()
    private var orderOfflineId: String = ""
    private var paymentOfflineId: String = ""
    private var orderId: Int? = null
    private var orderIDNew: Int? = null
    private var paymentId: Int? = null
    private var tipAmount: Double = 0.0
    private var getOrderDetailsResponse: GetOrderDetailsResponse.Data? = null
    private var future_delivery_date: String = ""
    private var future_delivery_time: String = ""
    private var fourthValue: Double = 0.0
    private var thirdValue: Double = 0.0
    private var secondValue: Int = 0
    private var cartList: CartModel? = null
    private var splitValue: Int = -1
    private var noCashAdj: Double = 0.0
    private var totalPrice: Double = 0.0
    var cashSurcharge: Double = 0.0
    private var totaldiscount: Double = 0.0
    private val paymentViewModel by viewModels<PaymentViewModel>()
    private var subTotalPrice: Double = 0.0
    private var divideCashDiscount: Double = 0.0
    var paymentType = "Cash"
    private var totalTax: Double = 0.0
    var cardPaymentAmount = 0.0
    private var isUpdate: Boolean = false
    private var totalServiceCharge: Double = 0.0
    private var isTotalPayment: Boolean = false
    private var paymentAmount: Double = 0.0
    private var guestSelectedPos: Int = 0

    private var guestId: Int? = null
    private var guestRequestModel: GuestPaymentRequest? = null
    private var splitModel: DineInOrderPayment? = null
    private var floorPlanModel: GetFloorPlanResponse.Data.FloorPlanTable? = null
    private var isLastPayment: Boolean = false
    private var totalGuestCount: Int = 0
    private var paidGuestCount: Int = 0

    private var isSplitByNo: Boolean = false
    private var isCustomCash: Boolean = false
    private var isSplitByAmount: Boolean = false

    private var splitAfterAmount: Double = 0.0
    private var wholeTotalFromDinein: Double = 0.0
    var optionType = ""
    var isGuestPaymentTotal = true
    var amountType = ""
    var rateorAmount = ""
    var cashDiscountType = ""
    private val TAG = "PayByGuestDialog"
    var splitPaidAmount = 0.0

    @Inject
    lateinit var prefProvider: PrefProvider
    var isNextPayment = false

    companion object {
        fun newInstance() = PayByGuestDialog()

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogPayByGuestBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.model = viewModel



        splitValue = -1
        isSplitByNo = false
        isSplitByAmount = false
        observeShowProgress()
        navigateOnPaymentSuccess()
        wholePaymentObservor()
        isTotalPayment = requireArguments().getBoolean("isTotalPayment")
        isLastPayment = requireArguments().getBoolean("isLastPayment")
        LogUtil.logE(TAG, "isLastPayment:  ${isLastPayment}")
        totalPrice = requireArguments().getDouble("totalPrice")
        LogUtil.logE(TAG, "totalPrice:  ${totalPrice}")
        subTotalPrice = requireArguments().getDouble("subTotalPrice")
        totalServiceCharge = requireArguments().getDouble("totalServiceCharge")
        divideCashDiscount = requireArguments().getDouble("divideCashDiscount")
        totaldiscount = requireArguments().getDouble("totalDiscount")
        totalTax = requireArguments().getDouble("totalTax")
        getOrderDetailsResponse = requireArguments()?.getParcelable(Constants.PRINT_DATA_DINE_IN)
        dineInAdapterList =
            requireArguments().getParcelableArrayList<DineInModel>(Constants.DINE_IN_ADAPTER_LIST)
        guestSelectedPos = requireArguments().getInt(Constants.GUEST_POSITION)
        floorPlanModel = requireArguments().getParcelable("floorPlan")
        guestRequestModel = requireArguments().getParcelable("model")
        totalGuestCount = requireArguments().getInt("totalGuestCount")
        cartList = requireArguments().getParcelable("cartList")
        guestId = requireArguments().getInt("id")
        paidGuestCount = requireArguments().getInt("paidGuestCount")
        isGuestPay = requireArguments().getBoolean("isGuestPay")
        optionType = prefProvider.getValue(Constants.OPTION_TYPE, "")
        splitModel = requireArguments().getParcelable("orderPayment")
        orderOfflineId = requireArguments().getString("orderOfflineId").toString()
        paymentOfflineId = requireArguments().getString("paymentOfflineId").toString()
        cashDiscountType = optionType

        var navControll = findNavController()
        navControll.currentBackStackEntry?.savedStateHandle?.getLiveData<Bundle>("data")
            ?.observe(viewLifecycleOwner) {
                isNextPayment = it.getBoolean("isNextPayment")
                remainingAmount =
                    String.format("%.2f", it.getDouble("remainingAmount", 0.0)).toDouble()
                splitValue = it.getInt("splitvalue", -1)
                isSplitByAmount = it.getBoolean("isSplitByAmount", false)
                isSplitByNo = it.getBoolean("isSplitByNo", false)
//                subTotalPrice = requireArguments().getDouble("subTotalPrice")
//                totalServiceCharge = requireArguments().getDouble("totalServiceCharge")
//                divideCashDiscount = requireArguments().getDouble("divideCashDiscount")
//                totaldiscount = requireArguments().getDouble("totalDiscount")
//                totalTax = requireArguments().getDouble("totalTax")
                getOrderDetailsResponse =
                    requireArguments()?.getParcelable(Constants.PRINT_DATA_DINE_IN)
                dineInAdapterList =
                    requireArguments().getParcelableArrayList<DineInModel>(Constants.DINE_IN_ADAPTER_LIST)


                setSplitData()

            }


        var cardActualAmount = 0.0
        if (cashDiscountType == "CashDiscount") {
            cardActualAmount = totalPrice
        } else if (cashDiscountType == "SurCharge") {
            cardActualAmount = totalPrice + MethodUtils.calculateCashDiscount(
                totalPrice,
                prefProvider,
                requireContext()
            )
        } else {
            cardActualAmount = totalPrice
        }

        paymentViewModel.saveActualValue(
            totalPrice,
            subTotalPrice,
            totalTax,
            totalServiceCharge,
            tipAmount,
            totaldiscount,
            MethodUtils.calculateCashDiscount(totalPrice, prefProvider, requireContext()),
            cardActualAmount
        )

        if (isTotalPayment) {
            isTotalPayment = true
            isGuestPaymentTotal = false
            isLastPayment = true
            setTotalPaymentData()
        } else {
            setupGuestWiseData()
            var remainingGuest = totalGuestCount - paidGuestCount
            if (remainingGuest == 1 && isLastPayment) {
                isGuestPaymentTotal = true
            }
        }
        binding.txtCustom.setOnClickListener(this)
        binding.imgBack.setOnClickListener(this)
        binding.llCash.setOnClickListener(this)
        binding.llCredit.setOnClickListener(this)
        binding.txtOriginalAmount.setOnClickListener(this)
        binding.txtSecondAmount.setOnClickListener(this)
        binding.txtThirdAmount.setOnClickListener(this)
        binding.txtFourthAmount.setOnClickListener(this)
        binding.txtAddTips.setOnClickListener(this)

        totalPrice = allPaymentSummarySavedPref(TOTAL_PRICE_DINEIN, totalPrice)
        subTotalPrice = allPaymentSummarySavedPref(SUB_TOTAL_DINEIN, subTotalPrice)
        totalServiceCharge = allPaymentSummarySavedPref(SERVICE_CHARGE_DINEIN, totalServiceCharge)
        totalTax = allPaymentSummarySavedPref(TAX_CHARGE_DINEIN, totalTax)
        divideCashDiscount =
            allPaymentSummarySavedPref(CASH_DISCOUNT_SURCHARGE_DINEIN, divideCashDiscount)
        tipAmount = allPaymentSummarySavedPref(TIPS_AMOUNT_DINEIN, tipAmount)
        totaldiscount = allPaymentSummarySavedPref(TOTAL_DISCOUNT_DINEIN, totaldiscount)

        isUpdate = requireArguments().getBoolean("update")
        if (isUpdate) {

            orderId = requireArguments().getInt("orderId")
            paymentId = requireArguments().getInt("paymentId")
            paymentOfflineId = requireArguments().getString("paymentOfflineId").toString()
            orderOfflineId = requireArguments().getString("orderOfflineId").toString()
        }

        orderIDNew = requireArguments().getInt("orderId")

        callbackSetup()
        binding.txtSplitAmount.setOnClickListener(this)

        return binding.root
    }

    private fun callbackSetup() {
        setFragmentResultListener("request_key_split") { requestKey: String, bundle: Bundle ->
            splitValue = bundle.getInt("split")

            if (splitValue != -1) {
                isSplitByNo = true
                isSplitByAmount = false
                if (isNextPayment) {
                    splitAfterAmount =
                        String.format("%.2f", ((remainingAmount + tipAmount)) / splitValue)
                            .toDouble()


                    isNextPayment = false
                } else {
                    splitAfterAmount =
                        String.format("%.2f", ((totalPrice + tipAmount)) / splitValue)
                            .toDouble()
                }
                setSplitData()
            } else {
                splitAfterAmount = String.format("%.2f", bundle.getDouble("splitByAmount"))
                    .toDouble()
                val totalAmountFormat = MethodUtils.roundOffAmount(totalPrice)
                binding.txtSplitValue.text =
                    "Out of $totalAmountFormat Total, Payment 1 of $splitAfterAmount"
                isSplitByNo = false
                isSplitByAmount = true
                setSplitData()
            }
        }
        setFragmentResultListener("request_for_customAmount") { requestKey: String, bundle: Bundle ->
            val amounnt = bundle.getDouble("amount")
            isCustomCash = true
            paymentAmount = amounnt
            if (isSplitByNo) {
                EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} PayByGuestDialog.kt_setFragmentResultListener(\"request_for_customAmount\")_isSplitByNo= ${isSplitByNo} _8"))

                var remaining_payment =
                    String.format(
                        "%.2f",
                        prefProvider.getValue(TOTAL_PRICE_DINEIN, "")
                            .toDouble() - splitAfterAmount
                    ).toDouble()
                if (remaining_payment <= 0.0) {
                    prefProvider.setValueboolean("isLastPayment", true)
                } else {
                    prefProvider.setValueboolean("isLastPayment", false)
                }

                var remainningCashDiscount =
                    divideCashDiscount - (divideCashDiscount / splitValue)
                prefProvider.setValue(
                    Constants.CASH_DISCOUNT_SURCHARGE_DINEIN,
                    String.format("%.2f", remainningCashDiscount)
                )
            }

            paymentType = "Cash"
            if (isTotalPayment) {
                EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} PayByGuestDialog.kt_ isTotalPayment= ${isTotalPayment} _8"))
                makePayment()
            } else {
                viewModel.totalPayAmount(paymentAmount)
                guestPaySpit()
                var finallLastPayment = isLastPayment && isGuestPaymentTotal
                guestRequestModel?.paymentAttributes?.let { logPrintGuest(it) }
                guestRequestModel?.let {
                    guestId?.let { it1 ->
                        finallLastPayment?.let { it2 ->
                            splitModel?.let { it3 ->
                                viewModel.payByGuest(
                                    it1, it,
                                    it2,
                                    it3
                                )
                            }
                        }
                    }
                }
            }
        }

        setFragmentResultListener("request_key_tips") { requestKey: String, bundle: Bundle ->
            tipAmount = bundle.getDouble("tipAmount")
            tipID = bundle.getInt("tipId")

            tipAmountCalculation()
        }
    }


    private fun tipAmountCalculation() {

        val _totalPrice =
            if (splitValue == -1) (totalPrice) else splitAfterAmount

        if (tipAmount == 0.00) {
            binding.txtTotalAmount.text = MethodUtils.roundOffAmount(_totalPrice)
        } else {
            binding.txtTotalAmount.text =
                MethodUtils.roundOffAmount(_totalPrice + tipAmount) + " (" + MethodUtils.roundOffAmount(
                    tipAmount
                ) + " Tip Added)"
        }


        getCashPaymentOptionList(_totalPrice + tipAmount)
        MethodUtils.setPriceTextView(binding.txtTipAmt, tipAmount)
        LogUtil.logE(TAG, "_totalPrice:  ${_totalPrice}")
        MethodUtils.setPriceTextView(binding.txtTotal, cardPaymentAmount + tipAmount)
        cardPaymentAmount += tipAmount
        binding.txtCardAmount.text =
            "$ " + String.format("%.2f", cardPaymentAmount)
    }

    @SuppressLint("SetTextI18n")
    private fun setSplitData() {
        if (isNextPayment) {
            if (isSplitByNo) {
                isCustomCash = false
                if (MethodUtils.isEnableCashDiscount(requireContext())) {
                    var last_cash_discount_surcharge =
                        prefProvider.getValue(Constants.CASH_DISCOUNT_SURCHARGE_DINEIN, "")//3.33
                    if (last_cash_discount_surcharge.isEmpty() || last_cash_discount_surcharge == "0.0") {
                        last_cash_discount_surcharge = "0.0"
                    }
                    cardPaymentAmount =
                        (remainingAmount + last_cash_discount_surcharge.toDouble())
                    binding.txtCardAmount.text = "$ " + String.format("%.2f", cardPaymentAmount)
//                    divideCashDiscount =
//                        prefProvider.getValue(Constants.CASH_DISCOUNT_SURCHARGE_DINEIN, "")
//                            .toDouble()
                } else {
                    cardPaymentAmount = remainingAmount
                    binding.txtCardAmount.text =
                        "$" + String.format("%.2f", remainingAmount)
                }
                MethodUtils.setPriceTextView(binding.txtTotalAmount, remainingAmount)
                getCashPaymentOptionList(remainingAmount)
            } else if (isSplitByAmount) {
                isCustomCash = false
                splitAfterAmount = String.format("%.2f", remainingAmount).toDouble()
                val tipAmount1 =
                    (splitAfterAmount * prefProvider.getValue(TIPS_AMOUNT_DINEIN, "")
                        .toDouble()) / prefProvider.getValue(TOTAL_PRICE_DINEIN, "")
                        .toDouble()
                val total = (prefProvider.getValue(TOTAL_PRICE_DINEIN, "").toDouble() + tipAmount1)
                val subTotalPrice1 = (splitAfterAmount * prefProvider.getValue(SUB_TOTAL_DINEIN, "")
                    .toDouble()) / total
                val totalServiceCharge1 =
                    (splitAfterAmount * prefProvider.getValue(SERVICE_CHARGE_DINEIN, "")
                        .toDouble()) / total
                val totalTax1 = (splitAfterAmount * prefProvider.getValue(TAX_CHARGE_DINEIN, "")
                    .toDouble()) / total
                val totalDiscount1 =
                    (splitAfterAmount * prefProvider.getValue(TOTAL_DISCOUNT_DINEIN, "")
                        .toDouble()) / total
                val cashDiscount1 = (splitAfterAmount * prefProvider.getValue(
                    CASH_DISCOUNT_SURCHARGE_DINEIN, ""
                ).toDouble()) / total

                subTotalPrice = subTotalPrice1
                totalTax = totalTax1
                totalServiceCharge = totalServiceCharge1
                totaldiscount = totalDiscount1
                divideCashDiscount = cashDiscount1

                if (MethodUtils.isEnableCashDiscount(requireContext())) {
                    if (cashDiscountType == "CashDiscount") {
                        cardPaymentAmount = splitAfterAmount + divideCashDiscount
                        binding.txtCardAmount.text = "$ " + String.format("%.2f", cardPaymentAmount)
                    } else if (cashDiscountType == "SurCharge") {
                        cardPaymentAmount = splitAfterAmount - divideCashDiscount
                        binding.txtCardAmount.text =
                            "$ " + String.format("%.2f", cardPaymentAmount)
                    }
                } else {
                    cardPaymentAmount = remainingAmount
                    binding.txtCardAmount.text =
                        "$" + String.format("%.2f", remainingAmount)
                }
                MethodUtils.setPriceTextView(binding.txtTotalAmount, remainingAmount)
                getCashPaymentOptionList(remainingAmount)
            }
            isSplitByNo = false
            isSplitByAmount = false
            isCustomCash = false
        } else {
            if (isSplitByNo) {
                if (MethodUtils.isEnableCashDiscount(requireContext())) {
                    cardPaymentAmount = (splitAfterAmount + (divideCashDiscount / splitValue))
                    binding.txtCardAmount.text =
                        "$ " + String.format(
                            "%.2f",
                            (splitAfterAmount + (divideCashDiscount / splitValue))
                        )
                } else {
                    cardPaymentAmount = splitAfterAmount
                    binding.txtCardAmount.text =
                        "$" + String.format("%.2f", splitAfterAmount)
                }
                MethodUtils.setPriceTextView(binding.txtTotalAmount, splitAfterAmount)
                getCashPaymentOptionList(splitAfterAmount)
            } else if (isSplitByAmount) {
                val tipAmount1 =
                    (splitAfterAmount * tipAmount) / (totalPrice + tipAmount)
                val total = ((totalPrice) + tipAmount1)
                val subTotalPrice1 = (splitAfterAmount * subTotalPrice) / total
                val totalServiceCharge1 =
                    (splitAfterAmount * totalServiceCharge) / total
                val totalTax1 = (splitAfterAmount * totalTax) / total
                val totalDiscount1 = (splitAfterAmount * totaldiscount) / total
                val cashDiscount1 = (splitAfterAmount * divideCashDiscount) / total



                prefProvider.setValue(
                    SUB_TOTAL_DINEIN,
                    String.format("%.2f", subTotalPrice - subTotalPrice1)
                )
                prefProvider.setValue(
                    SERVICE_CHARGE_DINEIN,
                    String.format("%.2f", totalServiceCharge - totalServiceCharge1)
                )
                prefProvider.setValue(
                    TAX_CHARGE_DINEIN,
                    String.format("%.2f", totalTax - totalTax1)
                )
                prefProvider.setValue(
                    TIPS_AMOUNT_DINEIN,
                    String.format("%.2f", tipAmount - tipAmount1)
                )
                prefProvider.setValue(
                    TOTAL_DISCOUNT_DINEIN,
                    String.format("%.2f", totaldiscount - totalDiscount1)
                )
                prefProvider.setValue(
                    CASH_DISCOUNT_SURCHARGE_DINEIN,
                    String.format("%.2f", divideCashDiscount - cashDiscount1)
                )

                totalPrice = splitAfterAmount
                subTotalPrice = subTotalPrice1
                totalTax = totalTax1
                totalServiceCharge = totalServiceCharge1
                totaldiscount = totalDiscount1
                divideCashDiscount = cashDiscount1


                if (MethodUtils.isEnableCashDiscount(requireContext())) {
                    if (cashDiscountType == "CashDiscount") {
                        cardPaymentAmount = splitAfterAmount + (divideCashDiscount)
                        binding.txtCardAmount.text = "$ " + String.format("%.2f", cardPaymentAmount)
                    } else if (cashDiscountType == "SurCharge") {
                        cardPaymentAmount = splitAfterAmount - divideCashDiscount
                        binding.txtCardAmount.text =
                            "$ " + String.format("%.2f", cardPaymentAmount)
                    }
                } else {
                    cardPaymentAmount = splitAfterAmount
                    binding.txtCardAmount.text =
                        "$" + String.format("%.2f", cardPaymentAmount)
                }

                MethodUtils.setPriceTextView(binding.txtTotalAmount, splitAfterAmount)
                getCashPaymentOptionList(splitAfterAmount)
            }
        }
    }

    fun allPaymentSummarySavedPref(type: String, value: Double): Double {
        if (prefProvider.getValue(type, "").isEmpty()) {
            prefProvider.setValue(type, String.format("%.2f", value))
            return prefProvider.getValue(type, "").toDouble()
        } else {
            return prefProvider.getValue(type, "").toDouble()
        }
    }

    private fun wholePaymentObservor() {
        paymentViewModel.msgText.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                /*    AlertUtils.showCustomAlertWithListenerWithOK(requireContext(), it) { _, _ ->*/
                gotoPay()


                /*}*/

            }
        }

    }

    private fun gotoPay() {
        LogUtil.logE(TAG, "HEREGOTOPAY")


        orderId?.let {
            EventBus.getDefault()
                .post(MessageEvent("${Constants.LINE_BREAK_TAB} PayByGuestDialog.kt _ gotoPay() _ prefProvider.setValueInt(ORDER_ID) _ orderId -> ${Gson().toJson(it)}"))

            prefProvider.setValueInt("ORDER_ID", it) }

        when {
            paymentType == "Card" -> {
                if (isSplitByNo) {
                    val bundle = Bundle()
                    bundle.putDouble("PaidAmount", cardPaymentAmount)
                    var wholetotalPriceTemp = String.format(
                        "%.2f",
                        prefProvider.getValue(TOTAL_PRICE_DINEIN, "").toDouble()
                    )
                    bundle.putDouble("WholetotalPrice", wholetotalPriceTemp.toDouble())
                    var remainingAmount = 0.0

                    var tempCashDiscount = 0.0
                    if (cardPaymentAmount > prefProvider.getValue(TOTAL_PRICE_DINEIN, "")
                            .toDouble()
                    ) {
                        tempCashDiscount =
                            cardPaymentAmount - prefProvider.getValue(TOTAL_PRICE_DINEIN, "")
                                .toDouble()
                        remainingAmount =
                            cardPaymentAmount - prefProvider.getValue(TOTAL_PRICE_DINEIN, "")
                                .toDouble() - tempCashDiscount
                    } else {

                        val dis_charge_value =
                            if (splitValue == -1) divideCashDiscount else divideCashDiscount / splitValue

                        bundle.putDouble("dis_charge_value", dis_charge_value)
                        remainingAmount =
                            wholetotalPriceTemp.toDouble() + dis_charge_value - cardPaymentAmount
                    }

                    prefProvider.setValue(
                        TOTAL_PRICE_DINEIN,
                        String.format("%.2f", remainingAmount)
                    )
                    bundle.putDouble(
                        "remainingAmount",
                        remainingAmount
                    )
                    bundle.putDouble("TipAmount", tipAmount)
                    orderId?.let { bundle.putInt("orderID", it) }
                    bundle.putInt("splitValue", splitValue)
                    if (splitValue != -1) {
                        if (remainingAmount <= 0.0) {
                            bundle.putBoolean("isSpilt", false)
                            prefProvider.setValue(SUB_TOTAL_DINEIN, "")
                            prefProvider.setValue(TOTAL_DISCOUNT_DINEIN, "")
                            prefProvider.setValue(TIPS_AMOUNT_DINEIN, "")
                            prefProvider.setValue(TAX_CHARGE_DINEIN, "")
                            prefProvider.setValue(SERVICE_CHARGE_DINEIN, "")
                            prefProvider.setValueInt("ORDER_ID", -1)

                            EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} PayByGuestDialog.kt -> goToPay()_ ORDER_ID -> ${Gson().toJson(prefProvider.getValueInt("ORDER_ID",-2))}"))

                        } else {
                            bundle.putBoolean("isSpilt", true)
                            setPaymentAttriButes(SUB_TOTAL_DINEIN, splitValue)
                            setPaymentAttriButes(SERVICE_CHARGE_DINEIN, splitValue)
                            setPaymentAttriButes(TAX_CHARGE_DINEIN, splitValue)
                            setPaymentAttriButes(TIPS_AMOUNT_DINEIN, splitValue)
                            setPaymentAttriButes(TOTAL_DISCOUNT_DINEIN, splitValue)
                        }
                    } else {
                        bundle.putBoolean("isSpilt", false)
                        prefProvider.setValue(SUB_TOTAL_DINEIN, "")
                        prefProvider.setValue(TOTAL_DISCOUNT_DINEIN, "")
                        prefProvider.setValue(TIPS_AMOUNT_DINEIN, "")
                        prefProvider.setValue(TAX_CHARGE_DINEIN, "")
                        prefProvider.setValue(SERVICE_CHARGE_DINEIN, "")
                    }
                    bundle.putBoolean("isDineIn", true)
                    bundle.putBoolean("isGuest", isGuestPay)
                    bundle.putBoolean("isSplitByNo", isSplitByNo)
                    bundle.putBoolean("isSplitByAmount", isSplitByAmount)
                    bundle.putBoolean("isTotalPayment", isTotalPayment)
                    bundle.putBoolean("isLastPayment", isLastPayment)
                    bundle.putString("paymentType", "Cash")
                    bundle.putInt("orderID", orderIDNew ?: 0)
                    bundle.putParcelable(Constants.PRINT_DATA_DINE_IN, getOrderDetailsResponse)
                    isGuestPaymentTotal = false
                    bundle.putBoolean("isGuestPaymentTotal", isGuestPaymentTotal)
                    bundle.putParcelableArrayList(Constants.DINE_IN_ADAPTER_LIST, dineInAdapterList)
                    bundle.putDouble(DINE_IN_SUBTOTAL, subTotalPrice)
                    bundle.putDouble(DINE_IN_TAX, totalTax)
                    bundle.putDouble(DINE_IN_DISCOUNT, totaldiscount)
                    bundle.putDouble(DINE_IN_SERVICECHARGE, totalServiceCharge)
                    bundle.putInt(Constants.GUEST_POSITION, guestSelectedPos)
                    bundle.putDouble("noCashAdj", noCashAdj)
                    findNavController().navigate(
                        R.id.action_payByGuestDialog_to_orderCompleteFragment,
                        bundle
                    )
                } else if (isSplitByAmount) {
                    val bundle = Bundle()
                    bundle.putDouble("PaidAmount", cardPaymentAmount)
                    var wholetotalPriceTemp = String.format(
                        "%.2f",
                        prefProvider.getValue(TOTAL_PRICE_DINEIN, "").toDouble()
                    )
                    bundle.putDouble("WholetotalPrice", wholetotalPriceTemp.toDouble())
                    var remainingAmount = 0.0

                    var tempCashDiscount = 0.0
                    if (cardPaymentAmount > prefProvider.getValue(TOTAL_PRICE_DINEIN, "")
                            .toDouble()
                    ) {
                        tempCashDiscount =
                            cardPaymentAmount - prefProvider.getValue(TOTAL_PRICE_DINEIN, "")
                                .toDouble()
                        remainingAmount =
                            cardPaymentAmount - prefProvider.getValue(TOTAL_PRICE_DINEIN, "")
                                .toDouble() - tempCashDiscount
                    } else {

                        val dis_charge_value =
                            if (splitValue == -1) divideCashDiscount else divideCashDiscount / splitValue

                        bundle.putDouble("dis_charge_value", dis_charge_value)
                        remainingAmount =
                            wholetotalPriceTemp.toDouble() + dis_charge_value - cardPaymentAmount
                    }

                    prefProvider.setValue(
                        TOTAL_PRICE_DINEIN,
                        String.format("%.2f", remainingAmount)
                    )
                    bundle.putDouble(
                        "remainingAmount",
                        remainingAmount
                    )
                    bundle.putDouble("TipAmount", tipAmount)
                    orderId?.let { bundle.putInt("orderID", it) }
                    bundle.putInt("splitValue", splitValue)
                    if (splitValue != -1) {
                        if (prefProvider.getValue(TOTAL_PRICE_DINEIN, "")
                                .toDouble() <= cardPaymentAmount
                        ) {
                            bundle.putBoolean("isSpilt", false)
                            prefProvider.setValueInt("ORDER_ID", -1)
                            EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} PayByGuestDialog.kt -> goToPay()_ ORDER_ID -> ${Gson().toJson(prefProvider.getValueInt("ORDER_ID",-2))} _2"))

                        } else {
                            bundle.putBoolean("isSpilt", true)
                        }
                    } else {
                        bundle.putBoolean("isSpilt", false)
                    }
                    bundle.putBoolean("isSplitByNo", isSplitByNo)
                    bundle.putBoolean("isSplitByAmount", isSplitByAmount)
                    bundle.putString("paymentType", "Card")
                    bundle.putBoolean("isGuest", isGuestPay)
                    bundle.putInt("orderID", orderIDNew ?: 0)
                    bundle.putBoolean("isDineIn", true)
                    isGuestPaymentTotal = true
                    bundle.putBoolean("isGuestPaymentTotal", true)
                    bundle.putBoolean("isTotalPayment", isTotalPayment)
                    bundle.putParcelable(Constants.PRINT_DATA_DINE_IN, getOrderDetailsResponse)
                    bundle.putParcelableArrayList(Constants.DINE_IN_ADAPTER_LIST, dineInAdapterList)
                    bundle.putDouble(DINE_IN_SUBTOTAL, subTotalPrice)
                    bundle.putDouble(DINE_IN_TAX, totalTax)
                    bundle.putDouble(DINE_IN_DISCOUNT, totaldiscount)
                    bundle.putDouble(DINE_IN_SERVICECHARGE, totalServiceCharge)
                    bundle.putInt(Constants.GUEST_POSITION, guestSelectedPos)
                    bundle.putDouble("noCashAdj", noCashAdj)
                    findNavController().navigate(
                        R.id.action_payByGuestDialog_to_orderCompleteFragment,
                        bundle
                    )
                } else {
                    val bundle = Bundle()
                    bundle.putDouble("PaidAmount", cardPaymentAmount)
                    bundle.putDouble("WholetotalPrice", cardPaymentAmount)
                    bundle.putDouble("dis_charge_value", 0.0)
                    bundle.putDouble(
                        "remainingAmount",
                        0.0
                    )
                    bundle.putDouble("TipAmount", tipAmount)
                    bundle.putDouble("paymentAmount", cardPaymentAmount)
                    bundle.putInt("orderID", prefProvider.getValueInt("ORDER_ID", -1))
                    //bundle.putParcelable("receiptData", it.data)
                    bundle.putBoolean("isSpilt", false)
                    bundle.putBoolean("isDineIn", true)
                    bundle.putBoolean("isTotalPayment", isTotalPayment)
                    bundle.putBoolean("isGuest", isGuestPay)
                    bundle.putInt("splitValue", -1)
                    bundle.putBoolean("isLastPayment", isLastPayment)
                    bundle.putString("paymentType", "Card")
                    bundle.putBoolean("isSplitByNo", isSplitByNo)
                    bundle.putBoolean("isSplitByAmount", isSplitByAmount)
                    bundle.putInt("orderID", orderIDNew ?: 0)
                    if (isGuestPaymentTotal && isLastPayment) {
                        prefProvider.setValueInt("ORDER_ID", -1)
                        EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} PayByGuestDialog.kt -> goToPay()_ ORDER_ID -> ${Gson().toJson(prefProvider.getValueInt("ORDER_ID",-2))} _3"))

                    } else if (isLastPayment && isTotalPayment) {
                        prefProvider.setValueInt("ORDER_ID", -1)
                        EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} PayByGuestDialog.kt -> goToPay()_ ORDER_ID -> ${Gson().toJson(prefProvider.getValueInt("ORDER_ID",-2))} _4"))

                    }
                    isGuestPaymentTotal = true
                    bundle.putBoolean("isGuestPaymentTotal", true)
                    bundle.putParcelable(Constants.PRINT_DATA_DINE_IN, getOrderDetailsResponse)
                    bundle.putParcelableArrayList(Constants.DINE_IN_ADAPTER_LIST, dineInAdapterList)
                    bundle.putDouble(DINE_IN_SUBTOTAL, subTotalPrice)
                    bundle.putDouble(DINE_IN_TAX, totalTax)
                    bundle.putDouble(DINE_IN_DISCOUNT, totaldiscount)
                    bundle.putDouble(DINE_IN_SERVICECHARGE, totalServiceCharge)
                    bundle.putInt(Constants.GUEST_POSITION, guestSelectedPos)
                    bundle.putDouble("noCashAdj", noCashAdj)
                    if (isLastPayment!!)
                        bundle.putBoolean("isGuest", false)
                    findNavController().navigate(
                        R.id.action_payByGuestDialog_to_orderCompleteFragment,
                        bundle
                    )
                    prefProvider.setValueInt("ORDER_ID", -1)
                    EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} PayByGuestDialog.kt -> goToPay()_ ORDER_ID -> ${Gson().toJson(prefProvider.getValueInt("ORDER_ID",-2))} _5"))

                }
            }
            paymentType == "Cash" -> {
                when {

                    isSplitByNo && isCustomCash -> {
                        val bundle = Bundle()
                        bundle.putDouble("PaidAmount", paymentAmount)
                        val totalPriceTemp = String.format(
                            "%.2f",
                            prefProvider.getValue(TOTAL_PRICE_DINEIN, "").toDouble()
                        )
                        //  val totalPriceAfterTip = totalPriceTemp.toDouble() + tipAmount


                        bundle.putDouble("WholetotalPrice", totalPriceTemp.toDouble())

                        var remainingAmount = 0.0
                        remainingAmount = String.format(
                            "%.2f",
                            totalPriceTemp.toDouble() - splitAfterAmount
                        ).toDouble()
                        prefProvider.setValue(
                            TOTAL_PRICE_DINEIN,
                            String.format("%.2f", remainingAmount)
                        )
                        bundle.putDouble(
                            "remainingAmount",
                            remainingAmount
                        )
                        var splitChange = paymentAmount - splitAfterAmount
                        bundle.putDouble(
                            "splitChange", String.format("%.2f", splitChange).toDouble()
                        )
                        bundle.putDouble("TipAmount", tipAmount)

                        isGuestPaymentTotal =
                            (paymentAmount - splitChange) == totalPriceTemp.toDouble()

                        bundle.putBoolean("isGuestPaymentTotal", isGuestPaymentTotal)
                        orderId?.let { bundle.putInt("orderID", it) }
                        bundle.putBoolean("isSpilt", true)
                        bundle.putInt("splitValue", splitValue)
                        if (splitValue != -1) {
                            if (remainingAmount <= 0.0) {
                                bundle.putBoolean("isSpilt", false)
                                prefProvider.setValue(SUB_TOTAL_DINEIN, "")
                                prefProvider.setValue(TOTAL_DISCOUNT_DINEIN, "")
                                prefProvider.setValue(TIPS_AMOUNT_DINEIN, "")
                                prefProvider.setValue(TAX_CHARGE_DINEIN, "")
                                prefProvider.setValue(SERVICE_CHARGE_DINEIN, "")
                                prefProvider.setValueInt("ORDER_ID", -1)
                                EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} PayByGuestDialog.kt -> goToPay()_ ORDER_ID -> ${Gson().toJson(prefProvider.getValueInt("ORDER_ID",-2))} _6"))

                            } else {
                                bundle.putBoolean("isSpilt", true)
                                setPaymentAttriButes(SUB_TOTAL_DINEIN, splitValue)
                                setPaymentAttriButes(SERVICE_CHARGE_DINEIN, splitValue)
                                setPaymentAttriButes(TAX_CHARGE_DINEIN, splitValue)
                                setPaymentAttriButes(TIPS_AMOUNT_DINEIN, splitValue)
                                setPaymentAttriButes(TOTAL_DISCOUNT_DINEIN, splitValue)
                            }
                        } else {
                            bundle.putBoolean("isSpilt", false)
                            prefProvider.setValue(SUB_TOTAL_DINEIN, "")
                            prefProvider.setValue(TOTAL_DISCOUNT_DINEIN, "")
                            prefProvider.setValue(TIPS_AMOUNT_DINEIN, "")
                            prefProvider.setValue(TAX_CHARGE_DINEIN, "")
                            prefProvider.setValue(SERVICE_CHARGE_DINEIN, "")
                        }
                        bundle.putBoolean("isDineIn", true)
                        bundle.putBoolean("isGuest", isGuestPay)
                        bundle.putBoolean("isSplitByNo", isSplitByNo)
                        bundle.putBoolean("isSplitByAmount", isSplitByAmount)
                        bundle.putBoolean("isTotalPayment", isTotalPayment)
                        bundle.putBoolean("isCustomCash", isCustomCash)
                        bundle.putInt("orderID", orderIDNew ?: 0)
                        bundle.putBoolean("isLastPayment", isLastPayment)
                        bundle.putString("paymentType", "Cash")

                        bundle.putParcelable(Constants.PRINT_DATA_DINE_IN, getOrderDetailsResponse)
                        bundle.putParcelableArrayList(
                            Constants.DINE_IN_ADAPTER_LIST,
                            dineInAdapterList
                        )
                        bundle.putDouble(DINE_IN_SUBTOTAL, subTotalPrice)
                        bundle.putDouble(DINE_IN_TAX, totalTax)
                        bundle.putDouble(DINE_IN_DISCOUNT, totaldiscount)
                        bundle.putDouble(DINE_IN_SERVICECHARGE, totalServiceCharge)
                        bundle.putInt(Constants.GUEST_POSITION, guestSelectedPos)
                        bundle.putDouble("noCashAdj", noCashAdj)

                        findNavController().navigate(
                            R.id.action_payByGuestDialog_to_orderCompleteFragment,
                            bundle
                        )

                    }
                    isSplitByNo -> {
                        val bundle = Bundle()
                        bundle.putDouble("PaidAmount", splitAfterAmount)
                        val totalPriceTemp = String.format(
                            "%.2f",
                            prefProvider.getValue(TOTAL_PRICE_DINEIN, "").toDouble()
                        )
                        //  val totalPriceAfterTip = totalPriceTemp.toDouble() + tipAmount


                        bundle.putDouble("WholetotalPrice", totalPriceTemp.toDouble())

                        var remainingAmount = 0.0
                        remainingAmount = String.format(
                            "%.2f",
                            totalPriceTemp.toDouble() - splitAfterAmount
                        ).toDouble()
                        prefProvider.setValue(
                            TOTAL_PRICE_DINEIN,
                            String.format("%.2f", remainingAmount)
                        )
                        bundle.putDouble(
                            "remainingAmount",
                            remainingAmount
                        )

                        bundle.putDouble("TipAmount", tipAmount)


                        orderId?.let { bundle.putInt("orderID", it) }
                        bundle.putBoolean("isSpilt", true)
                        bundle.putInt("splitValue", splitValue)
                        if (splitValue != -1) {
                            if (remainingAmount <= 0.0) {
                                bundle.putBoolean("isSpilt", false)
                                prefProvider.setValue(SUB_TOTAL_DINEIN, "")
                                prefProvider.setValue(TOTAL_DISCOUNT_DINEIN, "")
                                prefProvider.setValue(TIPS_AMOUNT_DINEIN, "")
                                prefProvider.setValue(TAX_CHARGE_DINEIN, "")
                                prefProvider.setValue(SERVICE_CHARGE_DINEIN, "")
                                prefProvider.setValueInt("ORDER_ID", -1)
                                EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} PayByGuestDialog.kt -> goToPay()_ ORDER_ID -> ${Gson().toJson(prefProvider.getValueInt("ORDER_ID",-2))} _7"))

                            } else {
                                bundle.putBoolean("isSpilt", true)
                                setPaymentAttriButes(SUB_TOTAL_DINEIN, splitValue)
                                setPaymentAttriButes(SERVICE_CHARGE_DINEIN, splitValue)
                                setPaymentAttriButes(TAX_CHARGE_DINEIN, splitValue)
                                setPaymentAttriButes(TIPS_AMOUNT_DINEIN, splitValue)
                                setPaymentAttriButes(TOTAL_DISCOUNT_DINEIN, splitValue)
                            }
                        } else {
                            bundle.putBoolean("isSpilt", false)
                            prefProvider.setValue(SUB_TOTAL_DINEIN, "")
                            prefProvider.setValue(TOTAL_DISCOUNT_DINEIN, "")
                            prefProvider.setValue(TIPS_AMOUNT_DINEIN, "")
                            prefProvider.setValue(TAX_CHARGE_DINEIN, "")
                            prefProvider.setValue(SERVICE_CHARGE_DINEIN, "")
                        }
                        bundle.putBoolean("isDineIn", true)
                        bundle.putBoolean("isGuest", isGuestPay)
                        bundle.putBoolean("isSplitByNo", isSplitByNo)
                        bundle.putBoolean("isSplitByAmount", isSplitByAmount)
                        bundle.putBoolean("isTotalPayment", isTotalPayment)
                        bundle.putInt("orderID", orderIDNew ?: 0)
                        bundle.putBoolean("isLastPayment", isLastPayment)
                        bundle.putString("paymentType", "Cash")
                        isGuestPaymentTotal = remainingAmount <= 0.0
                        bundle.putBoolean("isGuestPaymentTotal", isGuestPaymentTotal)
                        bundle.putParcelable(Constants.PRINT_DATA_DINE_IN, getOrderDetailsResponse)
                        bundle.putParcelableArrayList(
                            Constants.DINE_IN_ADAPTER_LIST,
                            dineInAdapterList
                        )
                        bundle.putDouble(DINE_IN_SUBTOTAL, subTotalPrice)
                        bundle.putDouble(DINE_IN_TAX, totalTax)
                        bundle.putDouble(DINE_IN_DISCOUNT, totaldiscount)
                        bundle.putDouble(DINE_IN_SERVICECHARGE, totalServiceCharge)
                        bundle.putInt(Constants.GUEST_POSITION, guestSelectedPos)
                        bundle.putDouble("noCashAdj", noCashAdj)

                        findNavController().navigate(
                            R.id.action_payByGuestDialog_to_orderCompleteFragment,
                            bundle
                        )

                    }
                    isSplitByAmount -> {
                        val bundle = Bundle()
                        bundle.putDouble("PaidAmount", splitAfterAmount)
                        var wholetotalPriceTemp = String.format(
                            "%.2f",
                            prefProvider.getValue(TOTAL_PRICE_DINEIN, "").toDouble()
                        )

                        bundle.putDouble("WholetotalPrice", wholetotalPriceTemp.toDouble())
                        var remainingAmount = 0.0
                        remainingAmount = String.format(
                            "%.2f",
                            wholetotalPriceTemp.toDouble() - splitAfterAmount
                        ).toDouble()
                        prefProvider.setValue(
                            TOTAL_PRICE_DINEIN,
                            String.format("%.2f", remainingAmount)
                        )
                        bundle.putDouble(
                            "remainingAmount",
                            remainingAmount
                        )
                        orderId?.let { bundle.putInt("orderID", it) }
                        bundle.putInt("splitValue", splitValue)
                        if (remainingAmount == 0.0) {
                            bundle.putBoolean("isSpilt", false)
                            prefProvider.setValueInt("ORDER_ID", -1)
                            EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} PayByGuestDialog.kt -> goToPay()_ ORDER_ID -> ${Gson().toJson(prefProvider.getValueInt("ORDER_ID",-2))} _8"))

                        } else {
                            bundle.putBoolean("isSpilt", true)
                        }
                        bundle.putDouble("TipAmount", tipAmount)
                        bundle.putBoolean("isSplitByNo", isSplitByNo)
                        bundle.putBoolean("isSplitByAmount", isSplitByAmount)
                        bundle.putString("paymentType", "Cash")
                        bundle.putBoolean("isDineIn", true)
                        isGuestPaymentTotal = splitAfterAmount == remainingAmount
                        bundle.putBoolean("isGuestPaymentTotal", isGuestPaymentTotal)
                        bundle.putBoolean("isTotalPayment", isTotalPayment)
                        bundle.putBoolean("isLastPayment", isLastPayment)
                        bundle.putInt("orderID", orderIDNew ?: 0)
                        bundle.putParcelable(Constants.PRINT_DATA_DINE_IN, getOrderDetailsResponse)
                        bundle.putParcelableArrayList(
                            Constants.DINE_IN_ADAPTER_LIST,
                            dineInAdapterList
                        )

                        bundle.putDouble(DINE_IN_SUBTOTAL, subTotalPrice)
                        bundle.putDouble(DINE_IN_TAX, totalTax)
                        bundle.putDouble(DINE_IN_DISCOUNT, totaldiscount)
                        bundle.putDouble(DINE_IN_SERVICECHARGE, totalServiceCharge)
                        bundle.putInt(Constants.GUEST_POSITION, guestSelectedPos)
                        bundle.putDouble("noCashAdj", noCashAdj)
                        findNavController().navigate(
                            R.id.action_payByGuestDialog_to_orderCompleteFragment,
                            bundle
                        )
                    }
                    isCustomCash -> {
                        val bundle = Bundle()
                        bundle.putBoolean("isDineIn", true)
                        if (remainingAmount == 0.0) {
                            bundle.putDouble("PaidAmount", paymentAmount)
                        } else {
                            bundle.putDouble("PaidAmount", remainingAmount)
                        }
                        bundle.putDouble(
                            "WholetotalPrice",
                            prefProvider.getValue(TOTAL_PRICE_DINEIN, "").toDouble()
                        )
                        var remaining_custom: Double
                        if (isSplitByNo || isSplitByAmount) {
                            remaining_custom = paymentAmount - splitAfterAmount
                            bundle.putBoolean("isSpilt", true)
                        } else {
                            remaining_custom =
                                paymentAmount - prefProvider.getValue(TOTAL_PRICE_DINEIN, "")
                                    .toDouble()
                            bundle.putBoolean("isSpilt", false)
                        }
                        bundle.putDouble("TipAmount", tipAmount)
                        bundle.putDouble(
                            "remainingAmount",
                            remaining_custom
                        )
                        orderId?.let { bundle.putInt("orderID", it) }
                        bundle.putInt("splitValue", -1)

                        bundle.putBoolean("isSplitByNo", isSplitByNo)
                        bundle.putBoolean("isSplitByAmount", isSplitByAmount)
                        bundle.putBoolean("isCustomCash", isCustomCash)
                        bundle.putString("paymentType", "Cash")
                        bundle.putBoolean("isGuest", isGuestPay)
                        bundle.putBoolean("isSplitByAmount", isSplitByAmount)
                        bundle.putBoolean("isTotalPayment", isTotalPayment)
                        bundle.putBoolean("isLastPayment", isLastPayment)
                        bundle.putString("paymentType", "Cash")
                        bundle.putInt("orderID", orderIDNew ?: 0)
                        if (remainingAmount == 0.0) {
                            isGuestPaymentTotal =
                                (paymentAmount - remaining_custom) == prefProvider.getValue(
                                    TOTAL_PRICE_DINEIN,
                                    ""
                                ).toDouble()
                        } else {
                            isGuestPaymentTotal =
                                (paymentAmount - remaining_custom) == remainingAmount
                        }
                        bundle.putBoolean("isGuestPaymentTotal", isGuestPaymentTotal)

                        bundle.putParcelableArrayList(
                            Constants.DINE_IN_ADAPTER_LIST,
                            dineInAdapterList
                        )
                        bundle.putDouble(DINE_IN_SUBTOTAL, subTotalPrice)
                        bundle.putDouble(DINE_IN_TAX, totalTax)
                        bundle.putDouble(DINE_IN_DISCOUNT, totaldiscount)
                        bundle.putDouble(DINE_IN_SERVICECHARGE, totalServiceCharge)
                        bundle.putParcelable(Constants.PRINT_DATA_DINE_IN, getOrderDetailsResponse)
                        bundle.putInt(Constants.GUEST_POSITION, guestSelectedPos)
                        bundle.putDouble("noCashAdj", noCashAdj)
                        findNavController().navigate(
                            R.id.action_payByGuestDialog_to_orderCompleteFragment,
                            bundle
                        )

                        prefProvider.setValueInt("ORDER_ID", -1)
                        EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} PayByGuestDialog.kt -> goToPay()_ ORDER_ID -> ${Gson().toJson(prefProvider.getValueInt("ORDER_ID",-2))} _9"))

                    }
                    else -> {
                        val bundle = Bundle()
                        if (remainingAmount == 0.0) {
                            bundle.putDouble("PaidAmount", totalPrice + tipAmount)
                        } else {
                            bundle.putDouble("PaidAmount", remainingAmount)
                        }

                        bundle.putDouble(
                            "WholetotalPrice",
                            prefProvider.getValue(TOTAL_PRICE_DINEIN, "").toDouble()
                        )
                        bundle.putDouble(
                            "remainingAmount",
                            0.0
                        )
                        bundle.putInt("orderID", prefProvider.getValueInt("ORDER_ID", -1))
                        //bundle.putParcelable("receiptData", it.data)
                        bundle.putBoolean("isSpilt", false)
                        bundle.putBoolean("isDineIn", true)
                        bundle.putInt("splitValue", -1)
                        bundle.putBoolean("isGuest", isGuestPay)
                        bundle.putString("paymentType", "Cash")
                        bundle.putBoolean("isSplitByNo", isSplitByNo)
                        bundle.putBoolean("isSplitByAmount", isSplitByAmount)
                        bundle.putBoolean("isLastPayment", isLastPayment)
                        bundle.putBoolean("isTotalPayment", isTotalPayment)
                        bundle.putInt("orderID", orderIDNew ?: 0)

                        bundle.putDouble("TipAmount", tipAmount)
                        if (isGuestPaymentTotal && isLastPayment) {
                            prefProvider.setValueInt("ORDER_ID", -1)
                            EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} PayByGuestDialog.kt -> goToPay()_ ORDER_ID -> ${Gson().toJson(prefProvider.getValueInt("ORDER_ID",-2))} _10"))
                        } else if (isLastPayment && isTotalPayment) {
                            prefProvider.setValueInt("ORDER_ID", -1)
                            EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} PayByGuestDialog.kt -> goToPay()_ ORDER_ID -> ${Gson().toJson(prefProvider.getValueInt("ORDER_ID",-2))} _11"))
                        }
                        isGuestPaymentTotal = true
                        bundle.putBoolean("isGuestPaymentTotal", true)
                        bundle.putParcelable(Constants.PRINT_DATA_DINE_IN, getOrderDetailsResponse)
                        bundle.putParcelableArrayList(
                            Constants.DINE_IN_ADAPTER_LIST,
                            dineInAdapterList
                        )
                        bundle.putDouble(DINE_IN_SUBTOTAL, subTotalPrice)
                        bundle.putDouble(DINE_IN_TAX, totalTax)
                        bundle.putDouble(DINE_IN_DISCOUNT, totaldiscount)
                        bundle.putDouble(DINE_IN_SERVICECHARGE, totalServiceCharge)
                        bundle.putInt(Constants.GUEST_POSITION, guestSelectedPos)
                        bundle.putDouble("noCashAdj", noCashAdj)
                        if (isLastPayment!!)
                            bundle.putBoolean("isGuest", false)
                        findNavController().navigate(
                            R.id.action_payByGuestDialog_to_orderCompleteFragment,
                            bundle
                        )

                    }
                }
            }
        }
    }


    fun setPaymentAttriButes(type: String, split: Int) {
        var temp_value =
            prefProvider.getValue(type, "").toDouble()
        var remaining_Value =
            temp_value - (temp_value / split)
        prefProvider.setValue(
            type,
            remaining_Value.toString()
        )
    }

    private fun setTotalPaymentData() {
        MethodUtils.setPriceTextView(binding.txtSubTotal, subTotalPrice)
        MethodUtils.setPriceTextView(binding.txtTax, totalTax)
        MethodUtils.setPriceTextView(binding.txtTipAmt, tipAmount)
        MethodUtils.setPriceTextView(binding.txtServiceCharge, totalServiceCharge)

        if (MethodUtils.isEnableCashDiscount(requireContext())) {
            if (cashDiscountType == "CashDiscount") {
                binding.txtCardAmount.text = "$ " + String.format("%.2f", totalPrice)
                cardPaymentAmount = totalPrice
                binding.linearNonCashAdjamounnt.visibility = View.VISIBLE

                binding.txtNoncashAdj.text = "-$ " + String.format("%.2f", divideCashDiscount)
                noCashAdj = divideCashDiscount
                totalPrice -= divideCashDiscount
            } else if (cashDiscountType == "SurCharge") {
                binding.linearNonCashAdjamounnt.visibility = View.GONE
                binding.txtCardAmount.text =
                    "$ " + String.format("%.2f", totalPrice + divideCashDiscount)
                cardPaymentAmount = totalPrice + divideCashDiscount
            }
        } else {
            binding.txtCardAmount.text =
                "$" + String.format("%.2f", totalPrice)
            cardPaymentAmount = totalPrice
        }
        MethodUtils.setPriceTextView(
            binding.txtTotal,
            (totalPrice + divideCashDiscount + tipAmount)
        )
        MethodUtils.setPriceTextView(
            binding.txtTotalAmount,
            (totalPrice + tipAmount)
        )
        binding.linearDiscount.visibility = View.VISIBLE
        binding.txtDiscount.text = "- " +
                MainApplication.getInstance()!!.getText(R.string.symbole)
                    .toString() + String.format(
            "%.2f", totaldiscount
        )
        getCashPaymentOptionList(totalPrice + tipAmount)


    }

    private fun setUpPaymentTypeWiseDiscount() {
        if (MethodUtils.isEnableCashDiscount(requireContext())) {
            if (optionType == "CashDiscount") {
                if (!isSplitByAmount && !isSplitByNo && !isCustomCash && splitValue == -1) {
                    binding.linearNonCashAdjamounnt.visibility = View.GONE
                    MethodUtils.setPriceTextView(binding.txtTotal, cardPaymentAmount)
                } else {
                    binding.linearNonCashAdjamounnt.visibility = View.GONE
                }
            } else if (optionType == "SurCharge") {
                binding.linearNonCashAdjamounnt.visibility = View.VISIBLE
                if (splitValue != -1) {
                    noCashAdj = MethodUtils.roundOffAmountDouble(divideCashDiscount / splitValue)
                    binding.txtNoncashAdj.text =
                        "-$ " + String.format("%.2f", divideCashDiscount / splitValue)
                    totalPrice += (divideCashDiscount / splitValue)
                } else {
                    noCashAdj = divideCashDiscount
                    binding.txtNoncashAdj.text =
                        "-$ " + String.format("%.2f", divideCashDiscount)
                    totalPrice += (divideCashDiscount)
                }
            }
        } else {
            binding.linearNonCashAdjamounnt.visibility = View.GONE
        }
        if (tipAmount == 0.00) {
            MethodUtils.setPriceTextView(binding.txtTotalAmount, cardPaymentAmount)
        } else {
            binding.txtTotalAmount.text =
                MethodUtils.roundOffAmount(cardPaymentAmount) + " (" + MethodUtils.roundOffAmount(
                    tipAmount
                ) + " Tip Added)"
        }
    }

    private fun setupGuestWiseData() {
        MethodUtils.setPriceTextView(binding.txtSubTotal, subTotalPrice)
        MethodUtils.setPriceTextView(binding.txtTax, totalTax)
        //MethodUtils.setPriceTextView(binding.txtDiscount, totalDiscount)
        MethodUtils.setPriceTextView(binding.txtTipAmt, tipAmount)
        MethodUtils.setPriceTextView(binding.txtServiceCharge, totalServiceCharge)
        binding.linearDiscount.visibility = View.VISIBLE
        binding.txtDiscount.text = "- " +
                MainApplication.getInstance()!!.getText(R.string.symbole)
                    .toString() + String.format(
            "%.2f", totaldiscount
        )


        if (MethodUtils.isEnableCashDiscount(requireContext())) {
            if (cashDiscountType == "CashDiscount") {
                binding.txtCardAmount.text = "$ " + String.format("%.2f", totalPrice)
                cardPaymentAmount = totalPrice
                binding.linearNonCashAdjamounnt.visibility = View.VISIBLE
                noCashAdj = divideCashDiscount
                binding.txtNoncashAdj.text = "-$ " + String.format("%.2f", divideCashDiscount)
                totalPrice -= divideCashDiscount
            } else if (cashDiscountType == "SurCharge") {
                binding.linearNonCashAdjamounnt.visibility = View.GONE
                binding.txtCardAmount.text =
                    "$ " + String.format("%.2f", totalPrice + divideCashDiscount)
                cardPaymentAmount = totalPrice + divideCashDiscount
            }
        } else {
            binding.txtCardAmount.text =
                "$" + String.format("%.2f", totalPrice)
            cardPaymentAmount = totalPrice
        }
        MethodUtils.setPriceTextView(
            binding.txtTotal,
            (totalPrice + divideCashDiscount + tipAmount)
        )
        MethodUtils.setPriceTextView(
            binding.txtTotalAmount,
            (totalPrice + tipAmount)
        )
        getCashPaymentOptionList(totalPrice + tipAmount)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.imgBack -> {
                // dialog?.dismiss()
                findNavController().popBackStack()
            }
            R.id.txtCustom -> {
                paymentType = "Cash"
                val bundle = Bundle()
                if (splitAfterAmount != 0.0) {
                    bundle.putDouble("totalprice", (splitAfterAmount + tipAmount))
                } else {
                    bundle.putDouble("totalprice", ((totalPrice + tipAmount)))
                }
                findNavController().navigate(
                    R.id.action_payByGuestDialog_to_customAmountFragment,
                    bundle
                )
            }
            R.id.txtSplitAmount -> {
                if (totalPrice == 0.0) {

                    AlertUtils.showCustomAlert(
                        requireActivity(),
                        "You can't split amount less then $ 1.00"
                    )

                } else {

                    if (tipAmount == 0.0) {
                        val bundle = Bundle()
                        if (isSplitByNo || isSplitByAmount) {
                            bundle.putDouble("totalPrice", (splitAfterAmount))
                        } else {
                            if (remainingAmount == 0.0) {
                                bundle.putDouble("totalPrice", totalPrice + tipAmount)
                            } else {
                                bundle.putDouble("totalPrice", remainingAmount)
                            }
                        }
                        bundle.putInt("splitValue", splitValue)
                        findNavController().navigate(
                            R.id.action_payByGuestDialog_to_splitAmountFragment,
                            bundle
                        )
                    } else {
                        AlertUtils.showCustomAlertWithYesNoListener(
                            requireActivity(),
                            getString(R.string.tip_after_split_alert)
                        ) { _, _ ->

                            tipAmount = 0.0
                            tipAmountCalculation()
                        }
                    }
                }
            }
            R.id.llCredit -> {
                paymentType = "Card"
                setUpPaymentTypeWiseDiscount()
                if (isSplitByNo) {
                    var remaining_payment =
                        String.format(
                            "%.2f",
                            prefProvider.getValue(TOTAL_PRICE_DINEIN, "")
                                .toDouble() - splitAfterAmount
                        ).toDouble()
                    if (remaining_payment <= 0.0) {
                        prefProvider.setValueboolean("isLastPayment", true)
                    } else {
                        prefProvider.setValueboolean("isLastPayment", false)
                    }

                    var remainningCashDiscount =
                        divideCashDiscount - (divideCashDiscount / splitValue)
                    prefProvider.setValue(
                        Constants.CASH_DISCOUNT_SURCHARGE_DINEIN,
                        String.format("%.2f", remainningCashDiscount)
                    )
                } else if (isSplitByAmount) {
                    cardPaymentAmount
                }

                if (isTotalPayment) {
                    makePaymentCreditCard()
                } else {
                    viewModel.totalPayAmount(cardPaymentAmount)
                    guestPaySpit()
                    var finallLastPayment = isLastPayment && isGuestPaymentTotal
                    guestRequestModel?.paymentAttributes?.let { logPrintGuest(it) }
                    guestRequestModel?.let {
                        guestId?.let { it1 ->
                            finallLastPayment?.let { it2 ->
                                splitModel?.let { it3 ->
                                    viewModel.payByGuest(
                                        it1, it,
                                        it2,
                                        it3
                                    )
                                }
                            }
                        }
                    }
                }
            }

            R.id.llCash -> {
                paymentType = "Cash"
                EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} PayByGuestDialog.kt_R.id.llCash"))

                paymentAmount = when {

                    isSplitByNo -> {
                        var remaining_payment =
                            String.format(
                                "%.2f",
                                prefProvider.getValue(TOTAL_PRICE_DINEIN, "")
                                    .toDouble() - splitAfterAmount
                            ).toDouble()
                        if (remaining_payment <= 0.0) {
                            prefProvider.setValueboolean("isLastPayment", true)
                        } else {
                            prefProvider.setValueboolean("isLastPayment", false)
                        }

                        var remainningCashDiscount =
                            divideCashDiscount - (divideCashDiscount / splitValue)
                        prefProvider.setValue(
                            Constants.CASH_DISCOUNT_SURCHARGE_DINEIN,
                            String.format("%.2f", remainningCashDiscount)
                        )
                        splitAfterAmount
                    }
                    isSplitByAmount -> {
                        splitAfterAmount
                    }
                    isCustomCash -> {
                        paymentAmount
                    }
                    else -> {
                        if (remainingAmount == 0.0) {
                            totalPrice
                        } else {
                            remainingAmount
                        }
                    }
                }


                if (isTotalPayment) {
                    EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} PayByGuestDialog.kt_ isTotalPayment= ${isTotalPayment} _1"))
                    makePayment()
                } else {

                    viewModel.totalPayAmount(paymentAmount)

                    guestPaySpit()
                    var finallLastPayment = false
                    if (!isSplitByNo && !isSplitByAmount) {
                        finallLastPayment = isLastPayment && isGuestPaymentTotal
                    }
                    guestRequestModel?.paymentAttributes?.let { logPrintGuest(it) }
                    guestRequestModel?.let {
                        guestId?.let { it1 ->
                            finallLastPayment?.let { it2 ->
                                splitModel?.let { it3 ->
                                    viewModel.payByGuest(
                                        it1, it,
                                        it2,
                                        it3
                                    )
                                }
                            }
                        }
                    }
                }
            }

            R.id.txtOriginalAmount -> {
                paymentType = "Cash"
                paymentAmount = when {
                    isSplitByNo -> {
                        var remaining_payment =
                            String.format(
                                "%.2f",
                                prefProvider.getValue(TOTAL_PRICE_DINEIN, "")
                                    .toDouble() - splitAfterAmount
                            ).toDouble()
                        if (remaining_payment <= 0.0) {
                            prefProvider.setValueboolean("isLastPayment", true)
                        } else {
                            prefProvider.setValueboolean("isLastPayment", false)
                        }

                        var remainningCashDiscount =
                            divideCashDiscount - (divideCashDiscount / splitValue)
                        prefProvider.setValue(
                            Constants.CASH_DISCOUNT_SURCHARGE_DINEIN,
                            String.format("%.2f", remainningCashDiscount)
                        )
                        splitAfterAmount
                    }
                    isSplitByAmount -> {
                        splitAfterAmount
                    }
                    isCustomCash -> {
                        paymentAmount
                    }
                    else -> {
                        if (remainingAmount == 0.0) {
                            totalPrice + tipAmount
                        } else {
                            remainingAmount
                        }
                    }
                }
                if (isTotalPayment) {
                    EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} PayByGuestDialog.kt_ isTotalPayment= ${isTotalPayment} _2"))
                    makePayment()

                } else {
                    viewModel.totalPayAmount(paymentAmount)
                    guestPaySpit()
                    var finallLastPayment = isLastPayment && isGuestPaymentTotal
                    guestRequestModel?.paymentAttributes?.let { logPrintGuest(it) }
                    guestRequestModel?.let {
                        guestId?.let { it1 ->
                            finallLastPayment?.let { it2 ->
                                splitModel?.let { it3 ->
                                    viewModel.payByGuest(
                                        it1, it,
                                        it2,
                                        it3
                                    )
                                }
                            }
                        }
                    }
                }

            }
            R.id.txtSecondAmount -> {
                paymentType = "Cash"
                paymentAmount = secondValue.toDouble()
                isCustomCash = true
                if (isSplitByNo) {
                    var remaining_payment =
                        String.format(
                            "%.2f",
                            prefProvider.getValue(TOTAL_PRICE_DINEIN, "")
                                .toDouble() - splitAfterAmount
                        ).toDouble()
                    if (remaining_payment <= 0.0) {
                        prefProvider.setValueboolean("isLastPayment", true)
                    } else {
                        prefProvider.setValueboolean("isLastPayment", false)
                    }

                    var remainningCashDiscount =
                        divideCashDiscount - (divideCashDiscount / splitValue)
                    prefProvider.setValue(
                        Constants.CASH_DISCOUNT_SURCHARGE_DINEIN,
                        String.format("%.2f", remainningCashDiscount)
                    )
                }
                if (isTotalPayment) {
                    EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} PayByGuestDialog.kt_ isTotalPayment= ${isTotalPayment} _3"))
                    makePayment()
                } else {
                    viewModel.totalPayAmount(paymentAmount)
                    guestPaySpit()
                    var finallLastPayment = isLastPayment && isGuestPaymentTotal
                    guestRequestModel?.paymentAttributes?.let { logPrintGuest(it) }
                    guestRequestModel?.let {
                        guestId?.let { it1 ->
                            finallLastPayment?.let { it2 ->
                                splitModel?.let { it3 ->
                                    viewModel.payByGuest(
                                        it1, it,
                                        it2,
                                        it3
                                    )
                                }
                            }
                        }
                    }
                }
            }

            R.id.txtThirdAmount -> {
                paymentType = "Cash"
                paymentAmount = thirdValue
                isCustomCash = true
                if (isSplitByNo) {
                    var remaining_payment =
                        String.format(
                            "%.2f",
                            prefProvider.getValue(TOTAL_PRICE_DINEIN, "")
                                .toDouble() - splitAfterAmount
                        ).toDouble()
                    if (remaining_payment <= 0.0) {
                        prefProvider.setValueboolean("isLastPayment", true)
                    } else {
                        prefProvider.setValueboolean("isLastPayment", false)
                    }

                    var remainningCashDiscount =
                        divideCashDiscount - (divideCashDiscount / splitValue)
                    prefProvider.setValue(
                        Constants.CASH_DISCOUNT_SURCHARGE_DINEIN,
                        String.format("%.2f", remainningCashDiscount)
                    )
                }
                if (isTotalPayment) {
                    EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} PayByGuestDialog.kt_ isTotalPayment= ${isTotalPayment} _4"))
                    makePayment()
                } else {
                    viewModel.totalPayAmount(paymentAmount)
                    guestPaySpit()
                    var finallLastPayment = isLastPayment && isGuestPaymentTotal
                    guestRequestModel?.paymentAttributes?.let { logPrintGuest(it) }
                    guestRequestModel?.let {
                        guestId?.let { it1 ->
                            finallLastPayment?.let { it2 ->
                                splitModel?.let { it3 ->
                                    viewModel.payByGuest(
                                        it1, it,
                                        it2,
                                        it3
                                    )
                                }
                            }
                        }
                    }
                }
            }
            R.id.txtFourthAmount -> {
                paymentType = "Cash"
                paymentAmount = fourthValue
                isCustomCash = true
                if (isSplitByNo) {
                    var remaining_payment =
                        String.format(
                            "%.2f",
                            prefProvider.getValue(TOTAL_PRICE_DINEIN, "")
                                .toDouble() - splitAfterAmount
                        ).toDouble()
                    if (remaining_payment <= 0.0) {
                        prefProvider.setValueboolean("isLastPayment", true)
                    } else {
                        prefProvider.setValueboolean("isLastPayment", false)
                    }

                    var remainningCashDiscount =
                        divideCashDiscount - (divideCashDiscount / splitValue)
                    prefProvider.setValue(
                        Constants.CASH_DISCOUNT_SURCHARGE_DINEIN,
                        String.format("%.2f", remainningCashDiscount)
                    )
                }
                if (isTotalPayment) {
                    EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} PayByGuestDialog.kt_ isTotalPayment= ${isTotalPayment} _5"))
                    makePayment()

                } else {
                    viewModel.totalPayAmount(paymentAmount)
                    guestPaySpit()
                    var finallLastPayment = isLastPayment && isGuestPaymentTotal
                    guestRequestModel?.paymentAttributes?.let { logPrintGuest(it) }
                    guestRequestModel?.let {
                        guestId?.let { it1 ->
                            finallLastPayment?.let { it2 ->
                                splitModel?.let { it3 ->
                                    viewModel.payByGuest(
                                        it1, it,
                                        it2,
                                        it3
                                    )
                                }
                            }
                        }
                    }
                }
            }


            R.id.txtAddTips -> {
                val bundle = Bundle()
                bundle.putDouble("totalPrice", totalPrice)
                bundle.putDouble("totalTip", tipAmount)
                findNavController().navigate(
                    R.id.action_payByGuestDialog_to_addTipsDialog,
                    bundle
                )
            }

        }

    }

    private fun createRequestForCreditCardTotalAmount(): OrderRequestModel {
        val orderModel = OrderAttributeRequestModel()
        orderModel.apply {
            date = TimeFormatUtils.getCurrentDate()
            employeeId = prefProvider.getValueInt(EMPLOYEE_ID, 0)
            locationId = prefProvider.getValueInt(LOCATION_ID, 1)
            terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)
            note = ""
            openOrderType = "DineIn"
            orderTypeId = 2
            paymentStatus = 1
            subTotal = subTotalPrice
            totalAmount = cardPaymentAmount
            totalDiscount = totaldiscount
            totalServiceCharges = totalServiceCharge
            totalTaxAmount = totalTax
            totalTips = tipAmount
            cash_discount_or_surcharge = divideCashDiscount
            cash_discount_type = cashDiscountType
            offlineId = orderOfflineId

        }
        EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} PayByGuestDialog.kt, createRequestForCreditCardTotalAmount() orderModel=${Gson().toJson(orderModel)}"))

        return OrderRequestModel(
            completed_all_payments = false,
            order = orderModel
        )
    }

    private fun makePaymentCreditCard() {
        EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} makePaymentCreditCard()_4"))

        if (isUpdate)
            paymentViewModel.updateOrder(
                true,
                orderId,
                paymentId,
                paymentOfflineId,
                orderOfflineId
            )
        val requestModel = createRequestForCreditCardTotalAmount()
        if (requestModel != null) {
            EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} makePaymentCreditCard()_if (requestModel != null)_4"))

            paymentViewModel.totalPayAmount(cardPaymentAmount)
            var reemainvalue = 0.0
            if (!isCustomCash) {
                if (cashDiscountType == "CashDiscount") {
                    reemainvalue = paymentViewModel.actual_Total - cardPaymentAmount
                } else {
                    reemainvalue = paymentViewModel.actual_Total - divideCashDiscount - totalPrice
                }
            }

            if (reemainvalue == 0.0) {
                requestModel.order.paymentAttributes = paymentAttributes()
                logPrint(requestModel.order.paymentAttributes!!)
                paymentViewModel.dineInWholePayment(requestModel, orderId!!, splitValue)
            } else {
                EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} makePaymentCreditCard()_else_4"))
                val paymentReq = paymentAttributes()
                logPrint(paymentReq)
                var completePayment = false
                if (isSplitByNo) {
                    completePayment = false
                } else completePayment = !isSplitByAmount

                var splitOrderRequest = SpitByOrderRequestModel(
                    orderId,
                    completePayment,
                    SpitByOrderPaymentModel(listOf(paymentReq))
                )

                EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} makePaymentCreditCard()_paymentViewModel.splitByOrder(splitOrderRequest, true)_Before_4"))
                EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} makePaymentCreditCard()_paymentViewModel.splitByOrder(splitOrderRequest, true), splitOrderRequest -> ${Gson().toJson(splitOrderRequest)} _4"))
                paymentViewModel.splitByOrder(splitOrderRequest, true)
                EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} makePaymentCreditCard()_paymentViewModel.splitByOrder(splitOrderRequest, true)_After_4"))

            }

        }
    }

    private fun makePayment() {
        EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} makePayment()_5"))
        if (isUpdate)
            paymentViewModel.updateOrder(
                true,
                orderId,
                paymentId,
                paymentOfflineId,
                orderOfflineId
            )
        val requestModel = createRequestForTotalAmount()
        if (requestModel != null) {
            EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} makePayment()_if (requestModel != null)_5"))
            paymentViewModel.totalPayAmount(paymentAmount)
            var reemainvalue = 0.0
            if (!isCustomCash || isSplitByNo) {
                if (cashDiscountType == "CashDiscount") {
                    reemainvalue =
                        paymentViewModel.actual_Total - divideCashDiscount - paymentAmount
                } else {
                    reemainvalue = paymentViewModel.actual_Total - totalPrice
                }
            }
            if (reemainvalue == 0.0) {
                requestModel.order.paymentAttributes = paymentAttributes()
                logPrint(requestModel.order.paymentAttributes!!)
                orderId?.let { paymentViewModel.dineInWholePayment(requestModel, it, splitValue) }
            } else {
                EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} makePayment()_else_5"))

                val paymentReq = paymentAttributes()
                logPrint(paymentReq)
                var completePayment = false
                if (isSplitByNo) {
                    completePayment = false
                } else completePayment = !isSplitByAmount

                var splitOrderRequest = SpitByOrderRequestModel(
                    orderId,
                    completePayment,
                    SpitByOrderPaymentModel(listOf(paymentReq))
                )

                EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} makePayment()_paymentViewModel.splitByOrder(splitOrderRequest, true)_Before_5"))
                EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} makePayment()_paymentViewModel.splitByOrder(splitOrderRequest, true), splitOrderRequest -> ${Gson().toJson(splitOrderRequest)} _5"))
                paymentViewModel.splitByOrder(splitOrderRequest, true)
                EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} makePayment()_paymentViewModel.splitByOrder(splitOrderRequest, true)_After_5"))

            }
        }
    }

    fun logPrint(data: PaymentAttributes) {
        Log.d(TAG, "  makePayment: total : " + data!!.amount)
        Log.d(TAG, "  makePayment: subtotal : " + data!!.subTotal)
        Log.d(TAG, "  makePayment: cashdiscount : " + data!!.cash_discount_or_surcharge)
        Log.d(TAG, "  makePayment: tax :  " + data!!.taxAmount)
        Log.d(TAG, "  makePayment: servicecharge :  " + data!!.serviceChargeAmount)
        Log.d(TAG, "  makePayment: tip : " + data!!.tips)
        Log.d(TAG, "  makePayment: totaldiscount : " + data!!.totalDiscount)
    }

    fun logPrintGuest(data: GuestPaymentAttributes) {
        Log.d(TAG, "  makePayment: total : " + data!!.amount)
        Log.d(TAG, "  makePayment: subtotal : " + data!!.subTotal)
        Log.d(TAG, "  makePayment: cashdiscount : " + data!!.cash_discount_or_surcharge)
        Log.d(TAG, "  makePayment: tax :  " + data!!.taxAmount)
        Log.d(TAG, "  makePayment: servicecharge :  " + data!!.serviceChargeAmount)
        Log.d(TAG, "  makePayment: tip : " + data!!.tips)
        Log.d(TAG, "  makePayment: totaldiscount : " + data!!.totalDiscount)
    }

    private fun guestPaySpit() {
        if (paymentType == "Card") {
            var isLastPayment = prefProvider.getValueboolean("isLastPayment", false)
            when {
                (isSplitByNo && !isLastPayment) || (isSplitByNo && isCustomCash) || isNextPayment -> {
                    guestRequestModel?.paymentAttributes!!.amount =
                        cardPaymentAmount
                    guestRequestModel?.paymentAttributes!!.serviceChargeAmount =
                        totalServiceCharge / splitValue
                    guestRequestModel?.paymentAttributes!!.subTotal =
                        subTotalPrice / splitValue
                    guestRequestModel?.paymentAttributes!!.taxAmount =
                        totalTax / splitValue
                    guestRequestModel?.paymentAttributes!!.tips =
                        tipAmount
                    guestRequestModel?.paymentAttributes!!.totalDiscount =
                        totaldiscount / splitValue
                    guestRequestModel?.paymentAttributes!!.paymentType = paymentType
                    guestRequestModel?.paymentAttributes!!.cash_discount_or_surcharge =
                        divideCashDiscount / splitValue
                    guestRequestModel?.paymentAttributes!!.cash_discount_type = cashDiscountType

                    val guestPaymentAttributes = GuestPaymentAttributes()
                    guestPaymentAttributes.amount = guestRequestModel?.paymentAttributes!!.amount
                    guestPaymentAttributes.serviceChargeAmount =
                        guestRequestModel?.paymentAttributes!!.serviceChargeAmount
                    guestPaymentAttributes.subTotal =
                        guestRequestModel?.paymentAttributes!!.subTotal
                    guestPaymentAttributes.taxAmount =
                        guestRequestModel?.paymentAttributes!!.taxAmount
                    guestPaymentAttributes.tips = guestRequestModel?.paymentAttributes!!.tips
                    guestPaymentAttributes.totalDiscount =
                        guestRequestModel?.paymentAttributes!!.totalDiscount
                    guestPaymentAttributes.payableType =
                        guestRequestModel?.paymentAttributes!!.payableType
                    guestPaymentAttributes.paymentType =
                        guestRequestModel?.paymentAttributes!!.paymentType
                    guestPaymentAttributes.offlineId =
                        guestRequestModel?.paymentAttributes!!.offlineId
                    guestPaymentAttributes.order_id =
                        guestRequestModel?.paymentAttributes!!.order_id
                    guestPaymentAttributes.cash_discount_or_surcharge =
                        guestRequestModel?.paymentAttributes!!.cash_discount_or_surcharge
                    guestPaymentAttributes.cash_discount_type =
                        guestRequestModel?.paymentAttributes!!.cash_discount_type
                    guestRequestModel?.paymentAttributes!!.paymentAttributes =
                        listOf(guestPaymentAttributes)
                }
                else -> {
                    guestRequestModel?.paymentAttributes!!.amount =
                        cardPaymentAmount
                    guestRequestModel?.paymentAttributes!!.serviceChargeAmount =
                        totalServiceCharge
                    guestRequestModel?.paymentAttributes!!.subTotal =
                        subTotalPrice
                    guestRequestModel?.paymentAttributes!!.taxAmount =
                        totalTax
                    guestRequestModel?.paymentAttributes!!.tips =
                        tipAmount
                    guestRequestModel?.paymentAttributes!!.totalDiscount =
                        totaldiscount
                    guestRequestModel?.paymentAttributes!!.paymentType = paymentType
                    guestRequestModel?.paymentAttributes!!.cash_discount_or_surcharge =
                        divideCashDiscount
                    guestRequestModel?.paymentAttributes!!.cash_discount_type = cashDiscountType

                    val guestPaymentAttributes = GuestPaymentAttributes()
                    guestPaymentAttributes.amount = guestRequestModel?.paymentAttributes!!.amount
                    guestPaymentAttributes.serviceChargeAmount =
                        guestRequestModel?.paymentAttributes!!.serviceChargeAmount
                    guestPaymentAttributes.subTotal =
                        guestRequestModel?.paymentAttributes!!.subTotal
                    guestPaymentAttributes.taxAmount =
                        guestRequestModel?.paymentAttributes!!.taxAmount
                    guestPaymentAttributes.tips = guestRequestModel?.paymentAttributes!!.tips
                    guestPaymentAttributes.totalDiscount =
                        guestRequestModel?.paymentAttributes!!.totalDiscount
                    guestPaymentAttributes.payableType =
                        guestRequestModel?.paymentAttributes!!.payableType
                    guestPaymentAttributes.paymentType =
                        guestRequestModel?.paymentAttributes!!.paymentType
                    guestPaymentAttributes.offlineId =
                        guestRequestModel?.paymentAttributes!!.offlineId
                    guestPaymentAttributes.order_id =
                        guestRequestModel?.paymentAttributes!!.order_id
                    guestPaymentAttributes.cash_discount_or_surcharge =
                        guestRequestModel?.paymentAttributes!!.cash_discount_or_surcharge
                    guestPaymentAttributes.cash_discount_type =
                        guestRequestModel?.paymentAttributes!!.cash_discount_type
                    guestRequestModel?.paymentAttributes!!.paymentAttributes =
                        listOf(guestPaymentAttributes)
                }
            }
        } else if (paymentType == "Cash") {
            var isLastPayment = prefProvider.getValueboolean("isLastPayment", false)

            when {
                (isSplitByNo && !isLastPayment) || (isSplitByNo && isCustomCash) || isNextPayment -> {
                    guestRequestModel?.paymentAttributes!!.amount =
                        splitAfterAmount
                    guestRequestModel?.paymentAttributes!!.serviceChargeAmount =
                        totalServiceCharge / splitValue
                    guestRequestModel?.paymentAttributes!!.subTotal =
                        subTotalPrice / splitValue
                    guestRequestModel?.paymentAttributes!!.taxAmount =
                        totalTax / splitValue
                    guestRequestModel?.paymentAttributes!!.tips =
                        tipAmount
                    guestRequestModel?.paymentAttributes!!.totalDiscount =
                        totaldiscount / splitValue
                    guestRequestModel?.paymentAttributes!!.paymentType = paymentType
                    guestRequestModel?.paymentAttributes!!.cash_discount_or_surcharge =
                        divideCashDiscount / splitValue
                    guestRequestModel?.paymentAttributes!!.cash_discount_type = cashDiscountType

                    val guestPaymentAttributes = GuestPaymentAttributes()
                    guestPaymentAttributes.amount = guestRequestModel?.paymentAttributes!!.amount
                    guestPaymentAttributes.serviceChargeAmount =
                        guestRequestModel?.paymentAttributes!!.serviceChargeAmount
                    guestPaymentAttributes.subTotal =
                        guestRequestModel?.paymentAttributes!!.subTotal
                    guestPaymentAttributes.taxAmount =
                        guestRequestModel?.paymentAttributes!!.taxAmount
                    guestPaymentAttributes.tips = guestRequestModel?.paymentAttributes!!.tips
                    guestPaymentAttributes.totalDiscount =
                        guestRequestModel?.paymentAttributes!!.totalDiscount
                    guestPaymentAttributes.payableType =
                        guestRequestModel?.paymentAttributes!!.payableType
                    guestPaymentAttributes.paymentType =
                        guestRequestModel?.paymentAttributes!!.paymentType
                    guestPaymentAttributes.offlineId =
                        guestRequestModel?.paymentAttributes!!.offlineId
                    guestPaymentAttributes.order_id =
                        guestRequestModel?.paymentAttributes!!.order_id
                    guestPaymentAttributes.cash_discount_or_surcharge =
                        guestRequestModel?.paymentAttributes!!.cash_discount_or_surcharge
                    guestPaymentAttributes.cash_discount_type =
                        guestRequestModel?.paymentAttributes!!.cash_discount_type
                    guestRequestModel?.paymentAttributes!!.paymentAttributes =
                        listOf(guestPaymentAttributes)
                }
                isCustomCash -> {
                    guestRequestModel?.paymentAttributes!!.amount =
                        totalPrice
                    guestRequestModel?.paymentAttributes!!.serviceChargeAmount =
                        totalServiceCharge
                    guestRequestModel?.paymentAttributes!!.subTotal =
                        subTotalPrice
                    guestRequestModel?.paymentAttributes!!.taxAmount =
                        totalTax
                    guestRequestModel?.paymentAttributes!!.tips =
                        tipAmount
                    guestRequestModel?.paymentAttributes!!.totalDiscount =
                        totaldiscount
                    guestRequestModel?.paymentAttributes!!.paymentType = paymentType
                    guestRequestModel?.paymentAttributes!!.cash_discount_or_surcharge =
                        divideCashDiscount
                    guestRequestModel?.paymentAttributes!!.cash_discount_type = cashDiscountType

                    val guestPaymentAttributes = GuestPaymentAttributes()
                    guestPaymentAttributes.amount = guestRequestModel?.paymentAttributes!!.amount
                    guestPaymentAttributes.serviceChargeAmount =
                        guestRequestModel?.paymentAttributes!!.serviceChargeAmount
                    guestPaymentAttributes.subTotal =
                        guestRequestModel?.paymentAttributes!!.subTotal
                    guestPaymentAttributes.taxAmount =
                        guestRequestModel?.paymentAttributes!!.taxAmount
                    guestPaymentAttributes.tips = guestRequestModel?.paymentAttributes!!.tips
                    guestPaymentAttributes.totalDiscount =
                        guestRequestModel?.paymentAttributes!!.totalDiscount
                    guestPaymentAttributes.payableType =
                        guestRequestModel?.paymentAttributes!!.payableType
                    guestPaymentAttributes.paymentType =
                        guestRequestModel?.paymentAttributes!!.paymentType
                    guestPaymentAttributes.offlineId =
                        guestRequestModel?.paymentAttributes!!.offlineId
                    guestPaymentAttributes.order_id =
                        guestRequestModel?.paymentAttributes!!.order_id
                    guestPaymentAttributes.cash_discount_or_surcharge =
                        guestRequestModel?.paymentAttributes!!.cash_discount_or_surcharge
                    guestPaymentAttributes.cash_discount_type =
                        guestRequestModel?.paymentAttributes!!.cash_discount_type
                    guestRequestModel?.paymentAttributes!!.paymentAttributes =
                        listOf(guestPaymentAttributes)
                }
                else -> {
                    guestRequestModel?.paymentAttributes!!.amount =
                        paymentAmount
                    guestRequestModel?.paymentAttributes!!.serviceChargeAmount =
                        totalServiceCharge
                    guestRequestModel?.paymentAttributes!!.subTotal =
                        subTotalPrice
                    guestRequestModel?.paymentAttributes!!.taxAmount =
                        totalTax
                    guestRequestModel?.paymentAttributes!!.tips =
                        tipAmount
                    guestRequestModel?.paymentAttributes!!.totalDiscount =
                        totaldiscount
                    guestRequestModel?.paymentAttributes!!.paymentType = paymentType
                    guestRequestModel?.paymentAttributes!!.cash_discount_or_surcharge =
                        divideCashDiscount
                    guestRequestModel?.paymentAttributes!!.cash_discount_type = cashDiscountType

                    val guestPaymentAttributes = GuestPaymentAttributes()
                    guestPaymentAttributes.amount = guestRequestModel?.paymentAttributes!!.amount
                    guestPaymentAttributes.serviceChargeAmount =
                        guestRequestModel?.paymentAttributes!!.serviceChargeAmount
                    guestPaymentAttributes.subTotal =
                        guestRequestModel?.paymentAttributes!!.subTotal
                    guestPaymentAttributes.taxAmount =
                        guestRequestModel?.paymentAttributes!!.taxAmount
                    guestPaymentAttributes.tips = guestRequestModel?.paymentAttributes!!.tips
                    guestPaymentAttributes.totalDiscount =
                        guestRequestModel?.paymentAttributes!!.totalDiscount
                    guestPaymentAttributes.payableType =
                        guestRequestModel?.paymentAttributes!!.payableType
                    guestPaymentAttributes.paymentType =
                        guestRequestModel?.paymentAttributes!!.paymentType
                    guestPaymentAttributes.offlineId =
                        guestRequestModel?.paymentAttributes!!.offlineId
                    guestPaymentAttributes.order_id =
                        guestRequestModel?.paymentAttributes!!.order_id
                    guestPaymentAttributes.cash_discount_or_surcharge =
                        guestRequestModel?.paymentAttributes!!.cash_discount_or_surcharge
                    guestPaymentAttributes.cash_discount_type =
                        guestRequestModel?.paymentAttributes!!.cash_discount_type
                    guestRequestModel?.paymentAttributes!!.paymentAttributes =
                        listOf(guestPaymentAttributes)
                }
            }
        }

    }


    private fun paymentAttributes(): PaymentAttributes {
        if (paymentType == "Card") {
            var isLastPayment = prefProvider.getValueboolean("isLastPayment", false)
            if (isSplitByNo && !isLastPayment && isNextPayment) {
                val paymentReq = PaymentAttributes().apply {
                    amount = cardPaymentAmount
                    employeeId = prefProvider.getValueInt(EMPLOYEE_ID, 0)
                    payableType = "Order"
                    paymentType = "Card"
                    serviceChargeAmount = totalServiceCharge / splitValue
                    subTotal = subTotalPrice / splitValue
                    taxAmount = totalTax / splitValue
                    terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)
                    tips = tipAmount
                    tipsAdjusted = false
                    totalDiscount = totaldiscount / splitValue
                    order_id = orderId
                    cash_discount_or_surcharge = divideCashDiscount / splitValue
                    total_cash_discount = divideCashDiscount / splitValue
                    cash_discount_type = cashDiscountType
                }
                return paymentReq
            } else {
                val paymentReq = PaymentAttributes().apply {
                    amount = cardPaymentAmount
                    employeeId = prefProvider.getValueInt(EMPLOYEE_ID, 0)
                    payableType = "Order"
                    paymentType = "Card"
                    serviceChargeAmount = totalServiceCharge
                    subTotal = subTotalPrice
                    taxAmount = totalTax
                    terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)
                    tips = tipAmount
                    tipsAdjusted = false
                    totalDiscount = totaldiscount
                    offlineId = paymentOfflineId
                    order_id = orderId
                    cash_discount_or_surcharge = divideCashDiscount
                    total_cash_discount = divideCashDiscount
                    cash_discount_type = cashDiscountType

                }
                return paymentReq
            }
        } else {
            var isLastPayment = prefProvider.getValueboolean("isLastPayment", false)
            if (isSplitByNo && !isLastPayment) {
                val paymentReq = PaymentAttributes().apply {
                    amount = splitAfterAmount
                    employeeId = prefProvider.getValueInt(EMPLOYEE_ID, 0)
                    payableType = "Order"
                    paymentType = "Cash"
                    serviceChargeAmount = totalServiceCharge / splitValue
                    subTotal = subTotalPrice / splitValue
                    taxAmount = totalTax / splitValue
                    terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)
                    tips = tipAmount
                    tipsAdjusted = false
                    totalDiscount = totaldiscount / splitValue
                    order_id = orderId
                    cash_discount_type = cashDiscountType
                    offlineId = paymentOfflineId
                    cash_discount_or_surcharge = divideCashDiscount / splitValue
                    total_cash_discount = divideCashDiscount / splitValue
                }
                return paymentReq
            } else if (isCustomCash) {
                val paymentReq = PaymentAttributes().apply {
                    amount = totalPrice
                    employeeId = prefProvider.getValueInt(EMPLOYEE_ID, 0)
                    payableType = "Order"
                    paymentType = "Cash"
                    serviceChargeAmount = totalServiceCharge
                    subTotal = subTotalPrice
                    taxAmount = totalTax
                    terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)
                    tips = tipAmount
                    tipsAdjusted = false
                    totalDiscount = totaldiscount
                    order_id = orderId
                    offlineId = paymentOfflineId
                    cash_discount_type = cashDiscountType
                    cash_discount_or_surcharge = divideCashDiscount
                    total_cash_discount = divideCashDiscount
                }
                return paymentReq
            } else {
                val paymentReq = PaymentAttributes().apply {
                    amount = paymentAmount
                    employeeId = prefProvider.getValueInt(EMPLOYEE_ID, 0)
                    payableType = "Order"
                    paymentType = "Cash"
                    serviceChargeAmount = totalServiceCharge
                    subTotal = subTotalPrice
                    taxAmount = totalTax
                    terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)
                    tips = tipAmount
                    tipsAdjusted = false
                    totalDiscount = totaldiscount
                    order_id = orderId
                    cash_discount_type = cashDiscountType
                    cash_discount_or_surcharge = divideCashDiscount
                    total_cash_discount = divideCashDiscount
                    offlineId = paymentOfflineId
                }
                return paymentReq
            }

        }


    }

    @SuppressLint("SetTextI18n")
    private fun getCashPaymentOptionList(totalPrice: Double) {
        LogUtil.logE(TAG, "totalPrice  $totalPrice")
        secondValue = floor(totalPrice + 1).toInt()
        LogUtil.logE(TAG, "secondValue  $secondValue")
        val newVal = totalPrice + 1
        thirdValue = calculateCashOption(newVal)
        LogUtil.logE(TAG, "thirdValuatedValue:   $thirdValue")
        if (secondValue.toDouble() == thirdValue) {
            if (secondValue > 1000) {
                thirdValue += 100
            } else {
                thirdValue += 50
            }

        }
        fourthValue = calculateCashOption(thirdValue)
        if (thirdValue == fourthValue) {
            fourthValue += 100
        } else {
            fourthValue += 50
        }

        MethodUtils.setPriceTextView(binding.txtOriginalAmount, totalPrice)
        MethodUtils.setPriceTextView(binding.txtSecondAmount, secondValue.toDouble())
        MethodUtils.setPriceTextView(binding.txtThirdAmount, thirdValue)
        MethodUtils.setPriceTextView(binding.txtFourthAmount, fourthValue)


    }

    private fun calculateCashOption(value: Double): Double {
        if (value > 1000) {
            return ceil(value / 100) * 100

        } else if (value > 500) {
            return ceil(value / 50) * 50
        } else {
            val arrAmount = arrayOf(
                5,
                10,
                20,
                50,
                100,
                110,
                120,
                150,
                200,
                210,
                220,
                250,
                300,
                310,
                320,
                350,
                400,
                410,
                420,
                450,
                500
            )
            val myValue = value.toInt()
            LogUtil.logE(TAG, "myValue:  ${myValue}")
            var searchIndex: Int = -1
            val filterValue = arrAmount.filter {
                it >= value
            }.first()
            searchIndex = arrAmount.indexOf(filterValue)
            LogUtil.logE(TAG, "filterValue:  ${filterValue}")
            LogUtil.logE(TAG, "searchIndex:  ${searchIndex}")

            /* arrAmount.forEachIndexed { index, i ->
                 if (i >= value) {
                      = i
                     return@forEachIndexed
                 }
             }*/

            if (arrAmount.contains(myValue)) {
                searchIndex += 1
            }

            if (searchIndex >= arrAmount.size) {
                return 550.0
            } else {
                val lastAmount = arrAmount[searchIndex]
                return lastAmount.toDouble()
            }


//            if (searchIndex > 0)
        }
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

        viewModel.msgText.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                AlertUtils.showCustomAlert(requireContext(), it)
            }
        })


    }

    private fun navigateOnPaymentSuccess() {
        viewModel.onPayment.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { str ->
                LogUtil.logE(TAG, "getstr:   $str")
                /*AlertUtils.showCustomAlertWithListenerWithOK(requireContext(), str) { _, _ ->*/


                gotoPay()


                /*}*/

            }
        })
    }


    private fun createRequestForTotalAmount(): OrderRequestModel {
        val orderModel = OrderAttributeRequestModel()
        orderModel.apply {
            date = TimeFormatUtils.getCurrentDate()
            employeeId = prefProvider.getValueInt(EMPLOYEE_ID, 0)
            locationId = prefProvider.getValueInt(LOCATION_ID, 1)
            terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)
            note = ""
            openOrderType = "DineIn"
            orderTypeId = prefProvider.getValueInt(ORDER_TYPE_ID, 2)
            paymentStatus = 1
            subTotal = paymentViewModel.actual_SubTotal
            totalAmount =
                paymentViewModel.actual_Total - paymentViewModel.actual_CashDiscountSurCharge
            totalDiscount = paymentViewModel.actual_TotalDiscount
            totalServiceCharges = paymentViewModel.actual_TotalServiceCharge
            totalTaxAmount = paymentViewModel.actual_TotalTax
            totalTips = paymentViewModel.actual_TotalTips
            cash_discount_type = cashDiscountType
            cash_discount_or_surcharge = paymentViewModel.actual_CashDiscountSurCharge
            offlineId = orderOfflineId
        }

        EventBus.getDefault().post(MessageEvent("${Constants.LINE_BREAK_TAB} PayByGuestDialog.kt, createRequestForTotalAmount() orderModel=${Gson().toJson(orderModel)}"))

        return OrderRequestModel(
            completed_all_payments = true,
            order = orderModel
        )
    }
}
