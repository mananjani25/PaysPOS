package com.android.pos.ui.dialog

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.MainApplication
import com.android.pos.R
import com.android.pos.data.entities.CartModel
import com.android.pos.data.model.requestModel.*
import com.android.pos.data.model.responseModel.GetFloorPlanResponse
import com.android.pos.data.model.responseModel.GuestPaymentAttributes
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.CASH_DISCOUNT_SURCHARGE_DINEIN
import com.android.pos.data.remote.Constants.EMPLOYEE_ID
import com.android.pos.data.remote.Constants.LOCATION_ID
import com.android.pos.data.remote.Constants.SERVICE_CHARGE_DINEIN
import com.android.pos.data.remote.Constants.SUB_TOTAL_DINEIN
import com.android.pos.data.remote.Constants.TAX_CHARGE_DINEIN
import com.android.pos.data.remote.Constants.TERMINAL_ID
import com.android.pos.data.remote.Constants.TIPS_AMOUNT_DINEIN
import com.android.pos.data.remote.Constants.TOTAL_DISCOUNT_DINEIN
import com.android.pos.data.remote.Constants.TOTAL_PRICE_DINEIN
import com.android.pos.databinding.DialogPayByGuestBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.fragments.dinein.DineInOrderTableViewModel
import com.android.pos.ui.fragments.payment.PaymentViewModel
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.TimeFormatUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.floor

@AndroidEntryPoint
open class PayByGuestDialog : Fragment(), View.OnClickListener {

    private var isGuestPay: Boolean = false
    private lateinit var binding: DialogPayByGuestBinding
    private var remainingAmount: Double = 0.0
    private val viewModel by viewModels<DineInOrderTableViewModel>()
    private var orderOfflineId: String = ""
    private var paymentOfflineId: String = ""
    private var orderId: Int? = null
    private var paymentId: Int? = null
    private var tipAmount: Double = 0.0
    private var future_delivery_date: String = ""
    private var future_delivery_time: String = ""
    private var fourthValue: Double = 0.0
    private var thirdValue: Double = 0.0
    private var secondValue: Int = 0
    private var cartList: CartModel? = null
    private var splitValue: Int = -1
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
        totalPrice = requireArguments().getDouble("totalPrice")
        floorPlanModel = requireArguments().getParcelable("floorPlan")
        subTotalPrice = requireArguments().getDouble("subTotalPrice")
        totalTax = requireArguments().getDouble("totalTax")
        guestRequestModel = requireArguments().getParcelable("model")
        divideCashDiscount = requireArguments().getDouble("divideCashDiscount")
        totalServiceCharge = requireArguments().getDouble("totalServiceCharge")
        totaldiscount = requireArguments().getDouble("totalDiscount")
        totalGuestCount = requireArguments().getInt("totalGuestCount")
        cartList = requireArguments().getParcelable("cartList")
        guestId = requireArguments().getInt("id")
        paidGuestCount = requireArguments().getInt("paidGuestCount")
        isGuestPay = requireArguments().getBoolean("isGuestPay")
        optionType = prefProvider.getValue(Constants.OPTION_TYPE, "")
        splitModel = requireArguments().getParcelable("orderPayment")
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
                    splitAfterAmount = ((remainingAmount + tipAmount)) / splitValue
                    isNextPayment = false
                } else {
                    splitAfterAmount = ((totalPrice + tipAmount)) / splitValue
                }
                setSplitData()
            } else {
                splitAfterAmount = bundle.getDouble("splitByAmount")
                val totalAmountFormat = MethodUtils.roundOffAmount(totalPrice)
                binding.txtSplitValue.text =
                    "Out of $totalAmountFormat Total, Payment 1 of $splitAfterAmount"
                isSplitByNo = false
                isSplitByAmount = true
                setSplitData()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setSplitData() {
        if (isNextPayment) {
            if (isSplitByNo) {
                if (MethodUtils.isEnableCashDiscount(requireContext())) {
                    var last_cash_discount_surcharge =
                        prefProvider.getValue(Constants.CASH_DISCOUNT_SURCHARGE_DINEIN, "")//3.33
                    if (last_cash_discount_surcharge.isEmpty() || last_cash_discount_surcharge == "0.0") {
                        last_cash_discount_surcharge = "0.0"
                    }
                    cardPaymentAmount =
                        (remainingAmount + last_cash_discount_surcharge.toDouble())
                    binding.txtCardAmount.text = "$ " + String.format("%.2f", cardPaymentAmount)
                    divideCashDiscount =
                        prefProvider.getValue(Constants.CASH_DISCOUNT_SURCHARGE_DINEIN, "").toDouble()
                } else {
                    cardPaymentAmount = remainingAmount
                    binding.txtCardAmount.text =
                        "$" + String.format("%.2f", remainingAmount)
                }
                MethodUtils.setPriceTextView(binding.txtTotalAmount, remainingAmount)
                getCashPaymentOptionList(remainingAmount)
            } else if (isSplitByAmount) {
                splitAfterAmount = remainingAmount
                val tipAmount1 =
                    (splitAfterAmount * tipAmount) / (totalPrice + tipAmount)
                val total = ((totalPrice) + tipAmount1)
                totalPrice -= splitAfterAmount
                val subTotalPrice1 = (splitAfterAmount * subTotalPrice) / total
                val totalServiceCharge1 =
                    (splitAfterAmount * totalServiceCharge) / total
                val totalTax1 = (splitAfterAmount * totalTax) / total
                val totalDiscount1 = (splitAfterAmount * totaldiscount) / total
                val cashDiscount1 = (splitAfterAmount * divideCashDiscount) / total

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
                AlertUtils.showCustomAlertWithListenerWithOK(requireContext(), it) { _, _ ->
                    gotoPay()

                }

            }
        }

    }

    private fun gotoPay() {


        orderId?.let { prefProvider.setValueInt("ORDER_ID", it) }
        when {
            paymentType == "Card" -> {
                if (isSplitByNo) {
                    val bundle = Bundle()
                    bundle.putDouble("totalPrice", cardPaymentAmount)
                    bundle.putDouble("paymentAmount", cardPaymentAmount)
                    orderId?.let { bundle.putInt("orderID", it) }
                    bundle.putInt("splitValue", splitValue)
                    bundle.putDouble("remainingAmount", wholeTotalFromDinein - totalPrice)
                    bundle.putBoolean("isSpilt", true)
                    bundle.putBoolean("isDineIn", true)
                    bundle.putBoolean("isTotalPayment", isTotalPayment)
                    bundle.putBoolean("isGuest", isGuestPay)
                    bundle.putBoolean("isLastPayment", isLastPayment)
                    bundle.putString("paymentType", "Card")
                    findNavController().navigate(
                        R.id.action_payByGuestDialog_to_orderCompleteFragment,
                        bundle
                    )
                    prefProvider.setValue(Constants.SPLIT_PAY_AMOUNT_DINE_IN, "")
                    prefProvider.setValueInt(Constants.SPLIT_NO_DINE_IN, -1)
                    prefProvider.setValue(Constants.SPLIT_PAY_TYPE_DINE_IN, "")
                    prefProvider.setValueInt("ORDER_ID", -1)
                } else if (isSplitByAmount) {
                    val bundle = Bundle()
                    bundle.putDouble("totalPrice", cardPaymentAmount)
                    bundle.putDouble("paymentAmount", cardPaymentAmount)
                    orderId?.let { bundle.putInt("orderID", it) }
                    bundle.putBoolean("isTotalPayment", isTotalPayment)
                    bundle.putBoolean("isGuest", isGuestPay)
                    bundle.putBoolean("isLastPayment", isLastPayment)
                    bundle.putString("paymentType", "Card")

                    if (MethodUtils.roundOffAmountDouble(cardPaymentAmount) != MethodUtils.roundOffAmountDouble(
                            wholeTotalFromDinein
                        )
                    ) {

                        bundle.putBoolean("isSpilt", true)
                        bundle.putDouble(
                            "remainingAmount",
                            wholeTotalFromDinein - cardPaymentAmount
                        )


                        val splitPayAmount = prefProvider.getValue(
                            Constants.SPLIT_PAY_AMOUNT_DINE_IN,
                            ""
                        )
                        if (splitPayAmount.isNotEmpty()) {
                            cardPaymentAmount += splitPayAmount.toDouble()
                        }
                        prefProvider.setValue(
                            Constants.SPLIT_PAY_AMOUNT_DINE_IN,
                            cardPaymentAmount.toString()
                        )
                        prefProvider.setValueInt(Constants.SPLIT_NO_DINE_IN, splitValue)
                        prefProvider.setValue(
                            Constants.SPLIT_PAY_TYPE_DINE_IN,
                            Constants.SPLIT_PAY_AMOUNT_DINE_IN
                        )
                    } else {
                        bundle.putBoolean("isSpilt", false)
                        bundle.putBoolean("isLastPayment", isLastPayment)
                        bundle.putDouble(
                            "remainingAmount",
                            wholeTotalFromDinein - cardPaymentAmount
                        )

                        prefProvider.setValue(Constants.SPLIT_PAY_AMOUNT_DINE_IN, "")
                        prefProvider.setValueInt(Constants.SPLIT_NO_DINE_IN, -1)
                        prefProvider.setValue(Constants.SPLIT_PAY_TYPE_DINE_IN, "")
                    }

                    bundle.putBoolean("isDineIn", true)
                    findNavController().navigate(
                        R.id.action_payByGuestDialog_to_orderCompleteFragment,
                        bundle
                    )
                    prefProvider.setValueInt("ORDER_ID", -1)

                } else {
                    val bundle = Bundle()
                    bundle.putDouble("PaidAmount", cardPaymentAmount)
                    bundle.putDouble("WholetotalPrice", cardPaymentAmount)
                    bundle.putDouble("dis_charge_value", 0.0)
                    bundle.putDouble(
                        "remainingAmount",
                        0.0
                    )
                    bundle.putDouble("paymentAmount", cardPaymentAmount)
                    orderId?.let { bundle.putInt("orderID", it) }
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
                    isGuestPaymentTotal = true
                    bundle.putBoolean("isGuestPaymentTotal", true)
                    if (isLastPayment!!)
                        bundle.putBoolean("isGuest", false)
                    findNavController().navigate(
                        R.id.action_payByGuestDialog_to_orderCompleteFragment,
                        bundle
                    )
                    prefProvider.setValueInt("ORDER_ID", -1)
                }
            }
            paymentType == "Cash" -> {
                when {
                    isSplitByNo -> {
                        val bundle = Bundle()
                        bundle.putDouble("PaidAmount", splitAfterAmount)
                        var totalPriceTemp = String.format(
                            "%.2f",
                            prefProvider.getValue(TOTAL_PRICE_DINEIN, "").toDouble()
                        )
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
                        isGuestPaymentTotal = false
                        bundle.putBoolean("isGuestPaymentTotal", isGuestPaymentTotal)

                        findNavController().navigate(
                            R.id.action_payByGuestDialog_to_orderCompleteFragment,
                            bundle
                        )

                    }
                    isSplitByAmount -> {
                        var payAmount = splitAfterAmount
                        val bundle = Bundle()
                        bundle.putDouble("totalPrice", payAmount)
                        bundle.putDouble("paymentAmount", paymentAmount)
                        bundle.putString("paymentType", "Cash")
                        bundle.putBoolean("isTotalPayment", isTotalPayment)
                        bundle.putBoolean("isLastPayment", isLastPayment)
                        isGuestPaymentTotal = false
                        bundle.putBoolean("isGuestPaymentTotal", isGuestPaymentTotal)
                        orderId?.let { bundle.putInt("orderID", it) }
                        //                                bundle.putParcelable("receiptData", it.data)


                        if (MethodUtils.roundOffAmountDouble(payAmount) != MethodUtils.roundOffAmountDouble(
                                totalPrice
                            )
                        ) {

                            bundle.putBoolean("isSpilt", true)
                            bundle.putDouble("remainingAmount", totalPrice - payAmount)


                            val splitPayAmount = prefProvider.getValue(
                                Constants.SPLIT_PAY_AMOUNT_DINE_IN,
                                ""
                            )
                            if (splitPayAmount.isNotEmpty()) {
                                payAmount += splitPayAmount.toDouble()
                            }
                            prefProvider.setValue(
                                Constants.SPLIT_PAY_AMOUNT_DINE_IN,
                                payAmount.toString()
                            )
                            prefProvider.setValueInt(Constants.SPLIT_NO_DINE_IN, splitValue)
                            prefProvider.setValue(
                                Constants.SPLIT_PAY_TYPE_DINE_IN,
                                Constants.SPLIT_PAY_AMOUNT_DINE_IN
                            )
                        } else {
                            bundle.putBoolean("isSpilt", false)
                            bundle.putBoolean("isLastPayment", isLastPayment)
                            bundle.putDouble("remainingAmount", totalPrice - payAmount)

                        }

                        prefProvider.setValueInt("ORDER_ID", -1)
                        bundle.putBoolean("isDineIn", true)
                        findNavController().navigate(
                            R.id.action_payByGuestDialog_to_orderCompleteFragment,
                            bundle
                        )

                    }
                    else -> {
                        val bundle = Bundle()
                        if (remainingAmount == 0.0) {
                            bundle.putDouble("PaidAmount", totalPrice + tipAmount)
                        } else {
                            bundle.putDouble("PaidAmount", remainingAmount)
                        }

                        bundle.putDouble("WholetotalPrice", prefProvider.getValue(TOTAL_PRICE_DINEIN,"").toDouble())
                        bundle.putDouble(
                            "remainingAmount",
                            0.0
                        )
                        orderId?.let { bundle.putInt("orderID", it) }
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
                        isGuestPaymentTotal = true
                        bundle.putBoolean("isGuestPaymentTotal", true)
                        if (isLastPayment!!)
                            bundle.putBoolean("isGuest", false)
                        findNavController().navigate(
                            R.id.action_payByGuestDialog_to_orderCompleteFragment,
                            bundle
                        )
                        prefProvider.setValueInt("ORDER_ID", -1) }
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
                binding.txtNoncashAdj.text = "$ " + String.format("%.2f", divideCashDiscount)
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
            (totalPrice + tipAmount)
        )
        MethodUtils.setPriceTextView(
            binding.txtTotalAmount,
            (totalPrice + tipAmount)
        )

        if (totaldiscount == 0.0) {
            binding.linearDiscount.visibility = View.GONE
        } else {
            binding.linearDiscount.visibility = View.VISIBLE
            binding.txtDiscount.text = "- " +
                    MainApplication.getInstance()!!.getText(R.string.symbole)
                        .toString() + String.format(
                "%.2f", totaldiscount
            )
        }
        getCashPaymentOptionList(totalPrice + tipAmount)


    }

    private fun setUpPaymentTypeWiseDiscount() {
        if (MethodUtils.isEnableCashDiscount(requireContext())) {
            if (optionType == "CashDiscount") {
                binding.linearNonCashAdjamounnt.visibility = View.GONE
            } else {
                binding.linearNonCashAdjamounnt.visibility = View.VISIBLE
                binding.txtNoncashAdj.text = "$ " + String.format(
                    "%.2f",
                    MethodUtils.calculateCashDiscount(totalPrice, prefProvider, requireContext())
                )
            }
        } else {
            binding.linearNonCashAdjamounnt.visibility = View.GONE
        }

        MethodUtils.setPriceTextView(binding.txtTotalAmount, cardPaymentAmount)
        MethodUtils.setPriceTextView(binding.txtTotal, cardPaymentAmount)
    }

    private fun setupGuestWiseData() {
        MethodUtils.setPriceTextView(binding.txtSubTotal, subTotalPrice)
        MethodUtils.setPriceTextView(binding.txtTax, totalTax)
        //MethodUtils.setPriceTextView(binding.txtDiscount, totalDiscount)
        MethodUtils.setPriceTextView(binding.txtTipAmt, tipAmount)
        MethodUtils.setPriceTextView(binding.txtServiceCharge, totalServiceCharge)
        if (totaldiscount == 0.0) {
            binding.linearDiscount.visibility = View.GONE
        } else {
            binding.linearDiscount.visibility = View.VISIBLE
            binding.txtDiscount.text = "- " +
                    MainApplication.getInstance()!!.getText(R.string.symbole)
                        .toString() + String.format(
                "%.2f", totaldiscount
            )
        }


        if (MethodUtils.isEnableCashDiscount(requireContext())) {
            if (cashDiscountType == "CashDiscount") {
                binding.txtCardAmount.text = "$ " + String.format("%.2f", totalPrice)
                cardPaymentAmount = totalPrice
                binding.linearNonCashAdjamounnt.visibility = View.VISIBLE
                binding.txtNoncashAdj.text = "$ " + String.format("%.2f", divideCashDiscount)
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
            (totalPrice + tipAmount)
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
            R.id.txtSplitAmount -> {

                val bundle = Bundle()
                bundle.putDouble("totalPrice", (totalPrice + tipAmount))
                bundle.putInt("splitValue", splitValue)
                findNavController().navigate(
                    R.id.action_payByGuestDialog_to_splitAmountFragment,
                    bundle
                )
            }
            R.id.llCredit -> {
                paymentType = "Card"
                setUpPaymentTypeWiseDiscount()
                if (isSplitByNo) {
                    var remaining_payment =
                        String.format("%.2f",prefProvider.getValue(TOTAL_PRICE_DINEIN, "").toDouble() - splitAfterAmount).toDouble()
                    if (remaining_payment <= 0.0) {
                        prefProvider.setValueboolean("isLastPayment", true)
                    } else {
                        prefProvider.setValueboolean("isLastPayment", false)
                    }

                    var remainningCashDiscount =
                        divideCashDiscount - (divideCashDiscount / splitValue)
                    prefProvider.setValue(
                        Constants.CASH_DISCOUNT_SURCHARGE,
                        String.format("%.2f", remainningCashDiscount)
                    )
                } else if (isSplitByAmount) {
                    prefProvider.setValue(
                        Constants.CASH_DISCOUNT_SURCHARGE,
                        String.format("%.2f", divideCashDiscount)
                    )
                }

                if (isTotalPayment) {
                    makePaymentCreditCard()
                } else {
                    guestPaySpit()
                    var finallLastPayment = isLastPayment && isGuestPaymentTotal
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

                paymentAmount = when {

                    isSplitByNo -> {
                        var remaining_payment =
                            String.format("%.2f",prefProvider.getValue(TOTAL_PRICE_DINEIN, "").toDouble() - splitAfterAmount).toDouble()
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
                    else -> {
                        if (remainingAmount == 0.0) {
                            totalPrice + tipAmount
                        } else {
                            remainingAmount
                        }
                    }
                }


                if (isTotalPayment) {
                    makePayment()
                } else {
                    guestPaySpit()
                    var finallLastPayment = isLastPayment && isGuestPaymentTotal
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
                paymentAmount = when {
                    isSplitByNo -> {
                        var remaining_payment =
                            String.format("%.2f",prefProvider.getValue(TOTAL_PRICE_DINEIN, "").toDouble() - splitAfterAmount).toDouble()
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
                    else -> {
                        if (remainingAmount == 0.0) {
                            totalPrice + tipAmount
                        } else {
                            remainingAmount
                        }
                    }
                }
                if (isTotalPayment) {

                    makePayment()

                } else {
                    guestPaySpit()
                    var finallLastPayment = isLastPayment && isGuestPaymentTotal
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
                paymentAmount = secondValue.toDouble()
                if (isTotalPayment) {

                    makePayment()

                } else {
                    guestPaySpit()
                    var finallLastPayment = isLastPayment && isGuestPaymentTotal
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
                paymentAmount = thirdValue
                if (isTotalPayment) {
                    makePayment()
                } else {
                    guestPaySpit()
                    var finallLastPayment = isLastPayment && isGuestPaymentTotal
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
                paymentAmount = fourthValue
                if (isTotalPayment) {

                    makePayment()

                } else {
                    guestPaySpit()
                    var finallLastPayment = isLastPayment && isGuestPaymentTotal
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

        }

        return OrderRequestModel(
            completed_all_payments = false,
            order = orderModel
        )
    }

    private fun makePaymentCreditCard() {
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
            paymentViewModel.totalPayAmount(cardPaymentAmount)
            val orderId = prefProvider.getValueInt("ORDER_ID", -1)
            if (!isSplitByNo && !isSplitByAmount) {
                requestModel.order.paymentAttributes = paymentAttributes()
                paymentViewModel.dineInWholePayment(requestModel, orderId, splitValue)
            } else {
                val paymentReq = paymentAttributes()
                val aa = SpitByOrderRequestModel(
                    orderId,
                    true,
                    paymentReq!!,
                    SpitByOrderPaymentModel(listOf(paymentReq))
                )
                paymentViewModel.splitByOrder(aa!!, true)
            }
        }
    }

    private fun guestPaySpit() {
        if (paymentType == "Cash") {
            if (isSplitByNo) {
                guestRequestModel?.paymentAttributes!!.amount =
                    paymentAmount
                guestRequestModel?.paymentAttributes!!.serviceChargeAmount =
                    totalServiceCharge / splitValue
                guestRequestModel?.paymentAttributes!!.subTotal =
                    subTotalPrice / splitValue
                guestRequestModel?.paymentAttributes!!.taxAmount =
                    totalTax / splitValue
                guestRequestModel?.paymentAttributes!!.tips =
                    tipAmount / splitValue
                guestRequestModel?.paymentAttributes!!.totalDiscount =
                    totaldiscount / splitValue
                guestRequestModel?.paymentAttributes!!.paymentType = paymentType
                guestRequestModel?.paymentAttributes!!.cash_discount_or_surcharge =
                    divideCashDiscount / splitValue
                guestRequestModel?.paymentAttributes!!.cash_discount_type = cashDiscountType

                val guestPaymentAttributes = GuestPaymentAttributes()
                guestPaymentAttributes.amount =
                    paymentAmount
                guestPaymentAttributes.serviceChargeAmount =
                    totalServiceCharge / splitValue
                guestPaymentAttributes.subTotal =
                    subTotalPrice / splitValue
                guestPaymentAttributes.taxAmount =
                    totalTax / splitValue
                guestPaymentAttributes.tips =
                    tipAmount / splitValue
                guestPaymentAttributes.totalDiscount =
                    totaldiscount / splitValue
                guestPaymentAttributes.payableType =
                    guestRequestModel?.paymentAttributes!!.payableType
                guestPaymentAttributes.paymentType =
                    guestRequestModel?.paymentAttributes!!.paymentType
                guestPaymentAttributes.offlineId = guestRequestModel?.paymentAttributes!!.offlineId
                guestPaymentAttributes.order_id = guestRequestModel?.paymentAttributes!!.order_id
                guestPaymentAttributes.cash_discount_or_surcharge =
                    guestRequestModel?.paymentAttributes!!.cash_discount_or_surcharge
                guestPaymentAttributes.cash_discount_type =
                    guestRequestModel?.paymentAttributes!!.cash_discount_type

                guestRequestModel?.paymentAttributes!!.paymentAttributes =
                    listOf(guestPaymentAttributes)
            } else {
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
                guestPaymentAttributes.amount =
                    paymentAmount
                guestPaymentAttributes.serviceChargeAmount =
                    totalServiceCharge
                guestPaymentAttributes.subTotal =
                    subTotalPrice
                guestPaymentAttributes.taxAmount =
                    totalTax
                guestPaymentAttributes.tips =
                    tipAmount
                guestPaymentAttributes.totalDiscount =
                    totaldiscount
                guestPaymentAttributes.payableType =
                    guestRequestModel?.paymentAttributes!!.payableType
                guestPaymentAttributes.paymentType =
                    guestRequestModel?.paymentAttributes!!.paymentType
                guestPaymentAttributes.offlineId = guestRequestModel?.paymentAttributes!!.offlineId
                guestPaymentAttributes.order_id = guestRequestModel?.paymentAttributes!!.order_id
                guestPaymentAttributes.cash_discount_or_surcharge =
                    guestRequestModel?.paymentAttributes!!.cash_discount_or_surcharge
                guestPaymentAttributes.cash_discount_type =
                    guestRequestModel?.paymentAttributes!!.cash_discount_type

                guestRequestModel?.paymentAttributes!!.paymentAttributes =
                    listOf(guestPaymentAttributes)
            }

        } else if (paymentType == "Card") {
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
            guestRequestModel?.paymentAttributes!!.cash_discount_or_surcharge = cashSurcharge
            guestRequestModel?.paymentAttributes!!.paymentType = paymentType
            guestRequestModel?.paymentAttributes!!.cash_discount_type = cashDiscountType

            val guestPaymentAttributes = GuestPaymentAttributes()
            guestPaymentAttributes.amount =
                cardPaymentAmount
            guestPaymentAttributes.serviceChargeAmount =
                totalServiceCharge
            guestPaymentAttributes.subTotal =
                subTotalPrice
            guestPaymentAttributes.taxAmount =
                totalTax
            guestPaymentAttributes.tips =
                tipAmount
            guestPaymentAttributes.totalDiscount =
                totaldiscount
            guestPaymentAttributes.payableType =
                guestRequestModel?.paymentAttributes!!.payableType
            guestPaymentAttributes.paymentType =
                guestRequestModel?.paymentAttributes!!.paymentType
            guestPaymentAttributes.cash_discount_or_surcharge =
                guestRequestModel?.paymentAttributes!!.cash_discount_or_surcharge
            guestPaymentAttributes.offlineId = guestRequestModel?.paymentAttributes!!.offlineId
            guestPaymentAttributes.order_id = guestRequestModel?.paymentAttributes!!.order_id
            guestPaymentAttributes.cash_discount_type =
                guestRequestModel?.paymentAttributes!!.cash_discount_type


            guestRequestModel?.paymentAttributes!!.paymentAttributes =
                listOf(guestPaymentAttributes)
        }
    }

    private fun makePayment() {
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
            paymentViewModel.totalPayAmount(paymentAmount)
            val orderId = prefProvider.getValueInt("ORDER_ID", -1)
            if (!isSplitByNo && !isSplitByAmount) {
                requestModel.order.paymentAttributes = paymentAttributes()
                paymentViewModel.dineInWholePayment(requestModel, orderId, splitValue)
            } else {
                val paymentReq = paymentAttributes()
                val aa = SpitByOrderRequestModel(
                    orderId,
                    true,
                    paymentReq!!,
                    SpitByOrderPaymentModel(listOf(paymentReq))
                )
                paymentViewModel.splitByOrder(aa!!, true)
            }
        }
    }

    private fun paymentAttributes(): PaymentAttributes {
        if (paymentType == "Card") {
            if (isSplitByNo) {
                val paymentReq = PaymentAttributes().apply {
                    amount = cardPaymentAmount
                    employeeId = prefProvider.getValueInt(EMPLOYEE_ID, 0)
                    payableType = "Order"
                    paymentType = "Card"
                    serviceChargeAmount = totalServiceCharge/splitValue
                    subTotal = subTotalPrice/splitValue
                    taxAmount = totalTax/splitValue
                    terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)
                    tips = tipAmount/splitValue
                    tipsAdjusted = false
                    totalDiscount = totalDiscount/splitValue
                    order_id = orderId
                    cash_discount_or_surcharge = divideCashDiscount/splitValue
                    total_cash_discount = divideCashDiscount/splitValue
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
                    totalDiscount = totalDiscount
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
                    amount = paymentAmount
                    employeeId = prefProvider.getValueInt(EMPLOYEE_ID, 0)
                    payableType = "Order"
                    paymentType = "Cash"
                    serviceChargeAmount = totalServiceCharge / splitValue
                    subTotal = subTotalPrice / splitValue
                    taxAmount = totalTax / splitValue
                    terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)
                    tips = tipAmount / splitValue
                    tipsAdjusted = false
                    totalDiscount = totalDiscount / splitValue
                    order_id = orderId
                    cash_discount_type = cashDiscountType
                    cash_discount_or_surcharge = divideCashDiscount / splitValue
                    total_cash_discount = divideCashDiscount / splitValue
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
                    totalDiscount = totalDiscount
                    order_id = orderId
                    cash_discount_type = cashDiscountType
                    cash_discount_or_surcharge = divideCashDiscount
                    total_cash_discount = divideCashDiscount
                }
                return paymentReq
            }

        }


    }

//    override fun getTheme(): Int {
//        return R.style.DialogTheme
//    }

    @SuppressLint("SetTextI18n")
    private fun getCashPaymentOptionList(totalPrice: Double) {
        Log.e(TAG, "totalPrice  $totalPrice")
        secondValue = floor(totalPrice + 1).toInt()
        Log.e(TAG, "secondValue  $secondValue")
        val newVal = totalPrice + 1
        thirdValue = calculateCashOption(newVal)
        Log.e(TAG, "thirdValuatedValue:   $thirdValue")
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
            Log.e(TAG, "myValue:  ${myValue}")
            var searchIndex: Int = -1
            val filterValue = arrAmount.filter {
                it >= value
            }.first()
            searchIndex = arrAmount.indexOf(filterValue)
            Log.e(TAG, "filterValue:  ${filterValue}")
            Log.e(TAG, "searchIndex:  ${searchIndex}")

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
                Log.e(TAG, "getstr:   $str")

                AlertUtils.showCustomAlertWithListenerWithOK(requireContext(), str) { _, _ ->


                    gotoPay()


                }

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
            orderTypeId = 2
            paymentStatus = 1
            subTotal = paymentViewModel.actual_SubTotal
            totalAmount = paymentViewModel.actual_Total
            totalDiscount = paymentViewModel.actual_TotalDiscount
            totalServiceCharges = paymentViewModel.actual_TotalServiceCharge
            totalTaxAmount = paymentViewModel.actual_TotalTax
            totalTips = paymentViewModel.actual_TotalTips
            cash_discount_type = cashDiscountType
            cash_discount_or_surcharge = paymentViewModel.actual_CashDiscountSurCharge
        }

        return OrderRequestModel(
            completed_all_payments = false,
            order = orderModel
        )
    }
}
