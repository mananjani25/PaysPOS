package com.android.pos.ui.fragments.payment

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
import com.android.pos.data.entities.CashDiscountModel
import com.android.pos.data.entities.RedeemLoyaltyInfo
import com.android.pos.data.model.requestModel.*
import com.android.pos.data.model.responseModel.CreateOrderResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.LOCATION_ID
import com.android.pos.data.remote.Constants.ORDER_TYPE
import com.android.pos.data.remote.Constants.SPLIT_NO
import com.android.pos.data.remote.Constants.SPLIT_PAY_AMOUNT
import com.android.pos.data.remote.Constants.SPLIT_PAY_TYPE
import com.android.pos.data.remote.Constants.TERMINAL_ID
import com.android.pos.databinding.PaymentFragmentBinding
import com.android.pos.di.PrefProvider
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.visible
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.floor

@AndroidEntryPoint
open class PaymentFragment : Fragment(), View.OnClickListener {
    private var isSplitByNo: Boolean = false
    private var isSplitByAmount: Boolean = false
    private var splitAfterAmount: Double = 0.0
    private var orderOfflineId: String = ""
    private var paymentOfflineId: String = ""
    private var orderId: Int? = null
    private var paymentId: Int? = null
    private var isUpdate: Boolean = false
    private var tipAmount: Double = 0.0
    private var future_delivery_date: String = ""
    private var future_delivery_time: String = ""
    private var redeemLoyaltyInfo: RedeemLoyaltyInfo? = null
    private var fourthValue: Double = 0.0
    private var thirdValue: Double = 0.0
    private var secondValue: Int = 0
    private var cartList: CartModel? = null
    private var splitValue: Int = -1
    private var totalPrice: Double = 0.0
    private var WholetotalPrice: Double = 0.0
    private var totalDiscount: Double = 0.0
    private var subTotalPrice: Double = 0.0
    private var totalTax: Double = 0.0
    private var totalServiceCharge: Double = 0.0
    private var paymentAmount: Double = 0.0
    private var isCustomCash = false
    var cashDiscountData: CashDiscountModel? = null
    private lateinit var binding: PaymentFragmentBinding
    private val TAG = "PaymentFragment"
    var final_discount = 0.0
    var paymentType = "Cash"
    var cashDiscountType = ""
    var cardPaymentAmount = 0.0

    var isNextPayment = false
    var cashDiscountSurcharge: Double = 0.0
    var finalPrice: Double = 0.0
    var splitOldValue: Int = 0

    @Inject
    lateinit var prefProvider: PrefProvider


    companion object {
        fun newInstance() = PaymentFragment()
    }

    private val viewModel by viewModels<PaymentViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = PaymentFragmentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.model = viewModel
        splitValue = -1
        isSplitByNo = false
        isSplitByAmount = false
        tipAmount = 0.0

        var navController = findNavController()
        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<Bundle>("data")
            ?.observe(viewLifecycleOwner) {
                isNextPayment = it.getBoolean("isNextPayment")
//                totalPrice = it.getDouble("remainingAmount", 0.0)
                splitValue = it.getInt("splitvalue", -1)
                splitOldValue = splitValue
                isSplitByAmount = it.getBoolean("isSplitByAmount", false)
                isSplitByNo = it.getBoolean("isSplitByNo", false)
                setSplitData()
            }

        cartList = requireArguments().getParcelable("cartList")
        totalPrice = requireArguments().getDouble("totalPrice")
        cashDiscountSurcharge = requireArguments().getDouble("cashDiscountSurcharge", 0.0)
        cashDiscountType = prefProvider.getValue(Constants.OPTION_TYPE, "")
        if (prefProvider.getValue("WholeTotalPrice", "").isEmpty()) {
            WholetotalPrice = totalPrice
            if (cashDiscountType == "CashDiscount") {
                WholetotalPrice -= cashDiscountSurcharge
            }
            prefProvider.setValue("WholeTotalPrice", String.format("%.2f", WholetotalPrice))
        }

        subTotalPrice = requireArguments().getDouble("subTotalPrice")
        totalTax = requireArguments().getDouble("totalTax")

        totalServiceCharge = requireArguments().getDouble("totalServiceCharge")
        totalDiscount = requireArguments().getDouble("totalDiscount")
        future_delivery_time = requireArguments().getString("future_delivery_time").toString()
        future_delivery_date = requireArguments().getString("future_delivery_date").toString()
        redeemLoyaltyInfo = Gson().fromJson(
            requireArguments().getString("redeemLoyalty").toString(),
            RedeemLoyaltyInfo::class.java
        )

        isUpdate = requireArguments().getBoolean("update")
        if (isUpdate) {

            orderId = requireArguments().getInt("orderId")
            paymentId = requireArguments().getInt("paymentId")
            paymentOfflineId = requireArguments().getString("paymentOfflineId").toString()
            orderOfflineId = requireArguments().getString("orderOfflineId").toString()
        }


        setUpPaymentSummary()

        binding.txtSplitAmount.setOnClickListener(this)
        binding.txtCustom.setOnClickListener(this)
        binding.imgBack.setOnClickListener(this)
        binding.llCash.setOnClickListener(this)
        binding.llCredit.setOnClickListener(this)
        binding.txtOriginalAmount.setOnClickListener(this)
        binding.txtSecondAmount.setOnClickListener(this)
        binding.txtThirdAmount.setOnClickListener(this)
        binding.txtFourthAmount.setOnClickListener(this)
        binding.txtAddTips.setOnClickListener(this)
        binding.linearMore.setOnClickListener(this)

        Log.d(TAG, "onClick: $cardPaymentAmount")
        callbackSetup()
        observeShowProgress()
        observeData()
        observeQueueStart()
        queuePrinterObserver()


        return binding.root
    }

    private fun setSplitData() {
        if (isSplitByNo) {
            if (MethodUtils.isEnableCashDiscount(requireContext())) {
                binding.txtCardAmount.text =
                    "$ " + String.format(
                        "%.2f",
                        (totalPrice + cashDiscountSurcharge) / splitValue
                    )
            } else {
                binding.txtCardAmount.text =
                    "$" + String.format("%.2f", totalPrice / splitValue)
            }
            MethodUtils.setPriceTextView(binding.txtTotalAmount, totalPrice / splitValue)
            getCashPaymentOptionList(totalPrice / splitValue)
        } else if (isSplitByAmount) {
            if (MethodUtils.isEnableCashDiscount(requireContext())) {
                if (cashDiscountType == "CashDiscount") {
                    binding.txtCardAmount.text = "$ " + String.format("%.2f", totalPrice)
                } else if (cashDiscountType == "SurCharge") {
                    binding.txtCardAmount.text =
                        "$ " + String.format("%.2f", totalPrice + cashDiscountSurcharge)
                }
            } else {
                binding.txtCardAmount.text =
                    "$" + String.format("%.2f", totalPrice)
            }

            MethodUtils.setPriceTextView(binding.txtTotalAmount, totalPrice)
        }


    }

    private fun setUpPaymentSummary() {
        if (isSplitByNo || isSplitByAmount) {
            splitDataWithAmount()
        }
        if (MethodUtils.isEnableCashDiscount(requireContext())) {
            if (cashDiscountType == "CashDiscount") {
                binding.txtCardAmount.text = "$ " + String.format("%.2f", totalPrice)
                cardPaymentAmount = totalPrice
                binding.linearnoncashAdj.visibility = View.VISIBLE
                binding.txtNoncashAdj.text = "$ " + String.format("%.2f", cashDiscountSurcharge)
                totalPrice -= cashDiscountSurcharge
            } else if (cashDiscountType == "SurCharge") {
                binding.linearnoncashAdj.visibility = View.GONE
                binding.txtCardAmount.text =
                    "$ " + String.format("%.2f", totalPrice + cashDiscountSurcharge)
                cardPaymentAmount = totalPrice + cashDiscountSurcharge
            }
        } else {
            binding.txtCardAmount.text =
                "$" + String.format("%.2f", totalPrice)
            cardPaymentAmount = totalPrice
            binding.linearnoncashAdj.visibility = View.GONE
        }
        MethodUtils.setPriceTextView(binding.txtTotal, totalPrice)
        MethodUtils.setPriceTextView(binding.txtTotalAmount, totalPrice)
        MethodUtils.setPriceTextView(binding.txtSubTotal, subTotalPrice)
        MethodUtils.setPriceTextView(binding.txtTax, totalTax)
        MethodUtils.setPriceTextView(binding.txtTipAmt, tipAmount)
        MethodUtils.setPriceTextView(binding.txtServiceCharge, totalServiceCharge)
        if (redeemLoyaltyInfo?.needToApplyLoyalty == true) {
            binding.llLoyalty.visible()
            binding.llLoyaltyPoint.visible()
            binding.txtLoyaltyAmount.text =
                "- $" + String.format("%.2f", redeemLoyaltyInfo?.usedLoyaltyAmount ?: 0.0)
            binding.txtUsedLoyaltyPoints.text = "${redeemLoyaltyInfo?.usedLoyaltyPoints ?: 0}"
        } else {
            binding.llLoyalty.gone()
            binding.llLoyaltyPoint.gone()
        }

        if (totalDiscount == 0.0) {
            binding.linearDiscount.visibility = View.GONE
        } else {
            binding.linearDiscount.visibility = View.VISIBLE
            binding.txtDiscount.text = "- " +
                    MainApplication.getInstance()!!.getText(R.string.symbole)
                        .toString() + String.format(
                "%.2f", totalDiscount
            )
        }
        getCashPaymentOptionList((totalPrice + tipAmount))
    }

    private fun splitDataWithAmount() {
        val splitPayType = prefProvider.getValue(SPLIT_PAY_TYPE, "")
        val splitPayAmount = prefProvider.getValue(SPLIT_PAY_AMOUNT, "")

        if (splitPayType == SPLIT_NO) {
            tipAmount = 0.0

            if (splitPayAmount.isNotEmpty()) {
                val splitNo = prefProvider.getValueInt(SPLIT_NO, -1)
                if (splitNo != -1) {
                    val split_totalPrice = totalPrice / splitNo
                    totalPrice -= split_totalPrice

                    val split_subTotalPrice = subTotalPrice / splitNo
                    subTotalPrice -= split_subTotalPrice

                    val split_totalTax = totalTax / splitNo
                    totalTax -= split_totalTax

                    val split_totalServiceCharge = totalServiceCharge / splitNo
                    totalServiceCharge -= split_totalServiceCharge

                    val split_totalDiscount = totalDiscount / splitNo
                    totalDiscount -= split_totalDiscount

                    val split_cashDiscount = cashDiscountSurcharge / splitNo
                    cashDiscountSurcharge -= split_cashDiscount
                }

            }
        } else if (splitPayType == SPLIT_PAY_AMOUNT) {


            if (splitPayAmount.isNotEmpty()) {


                val tipAmount1 =
                    (splitPayAmount.toDouble() * tipAmount) / (totalPrice + tipAmount)

                val total = ((totalPrice) + tipAmount1)

                val subTotalPrice1 = (splitPayAmount.toDouble() * subTotalPrice) / total
                val totalServiceCharge1 =
                    (splitPayAmount.toDouble() * totalServiceCharge) / total
                val totalTax1 = (splitPayAmount.toDouble() * totalTax) / total
                val totalDiscount1 = (splitPayAmount.toDouble() * totalDiscount) / total


                totalPrice -= splitPayAmount.toDouble()
                subTotalPrice -= subTotalPrice1
                totalTax -= totalTax1
                totalServiceCharge -= totalServiceCharge1
                totalDiscount -= totalDiscount1
//                tipAmount -= tipAmount1

            }
        }

    }

    private fun setUpPaymentTypeWiseData(payType: String) {
        if (payType == "Cash") {
            if (MethodUtils.isEnableCashDiscount(requireContext())) {
                if (cashDiscountType == "CashDiscount") {
                    binding.linearnoncashAdj.visibility = View.VISIBLE
                    binding.txtNoncashAdj.text = "$" + String.format("%.2f", cashDiscountSurcharge)
                    totalPrice -= cashDiscountSurcharge
                } else if (cashDiscountType == "SurCharge") {
                    binding.linearnoncashAdj.visibility = View.GONE
                }
            } else {
                binding.linearnoncashAdj.visibility = View.GONE
            }
            MethodUtils.setPriceTextView(binding.txtTotal, totalPrice)
            MethodUtils.setPriceTextView(binding.txtTotalAmount, totalPrice)
        } else if (payType == "Card") {
            if (MethodUtils.isEnableCashDiscount(requireContext())) {
                if (cashDiscountType == "CashDiscount") {
                    binding.linearnoncashAdj.visibility = View.GONE
                } else if (cashDiscountType == "SurCharge") {
                    binding.linearnoncashAdj.visibility = View.VISIBLE
                    binding.txtNoncashAdj.text = "$" + String.format("%.2f", cashDiscountSurcharge)
                    totalPrice += cashDiscountSurcharge
                }
            } else {
                binding.linearnoncashAdj.visibility = View.GONE
            }
            MethodUtils.setPriceTextView(binding.txtTotal, cardPaymentAmount)
            MethodUtils.setPriceTextView(binding.txtTotalAmount, cardPaymentAmount)
        }
    }


    @SuppressLint("SetTextI18n")
    private fun callbackSetup() {

        setFragmentResultListener("request_key_split") { requestKey: String, bundle: Bundle ->
            splitValue = bundle.getInt("split")

            if (splitValue != -1) {
                if (splitOldValue == 0) {
                    splitOldValue = splitValue
                } else {
                    splitValue += splitOldValue
                }
                isSplitByNo = true
                isSplitByAmount = false
                splitAfterAmount = ((totalPrice + tipAmount)) / splitValue
                val totalAmountFormat = MethodUtils.roundOffAmount(totalPrice)

                binding.txtSplitValue.text =
                    "Out of $totalAmountFormat Total, Payment 1 of $splitValue"
                setSplitData()
//                getCashPaymentOptionList(splitAfterAmount)
            } else {
//                splitDataWithAmount()
                isSplitByAmount = true
                isSplitByNo = false
                val splitValue = bundle.getDouble("splitByAmount")
                splitAfterAmount = splitValue
                val totalAmountFormat = MethodUtils.roundOffAmount(totalPrice)
                binding.txtSplitValue.text =
                    "Out of $totalAmountFormat Total, Payment 1 of $splitValue"
                setSplitData()
//                getCashPaymentOptionList(splitAfterAmount)
            }
        }
        setFragmentResultListener("request_for_customAmount") { requestKey: String, bundle: Bundle ->
            val amounnt = bundle.getDouble("amount")
            isCustomCash = true
            finalPrice = totalPrice
            totalPrice = amounnt
            MethodUtils.setPriceTextView(binding.txtTotalAmount, totalPrice)

        }

        setFragmentResultListener("request_key_tips") { requestKey: String, bundle: Bundle ->
            tipAmount = bundle.getDouble("tipAmount")

            tipAmountCalculation()
        }


    }

    override fun onDestroy() {
        super.onDestroy()
        ProgressUtils.dismissProgressDialog()
    }

    private fun tipAmountCalculation() {

        val _totalPrice =
            if (splitValue == -1) (totalPrice) else (totalPrice) / splitValue

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
        MethodUtils.setPriceTextView(binding.txtTotal, _totalPrice + tipAmount)
    }


    @SuppressLint("SetTextI18n")
    private fun getCashPaymentOptionList(totalPrice: Double) {
        Log.e(TAG, "totalPrice  $totalPrice")
        secondValue = floor(totalPrice + 1).toInt()
        Log.e(TAG, "secondValue  $secondValue")
        val newVal = totalPrice + 1
        thirdValue = calculateCashOption(newVal)
        Log.e(TAG, "thirdValuethirdValue:   ${thirdValue}")
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

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.linearMore -> {
                //createQueuePrinter()
            }

            R.id.imgBack -> {

                if (prefProvider.getValue(SPLIT_PAY_AMOUNT, "") == "") {
                    prefProvider.setValue(SPLIT_PAY_AMOUNT, "")
                    prefProvider.setValueInt(SPLIT_NO, -1)
                    findNavController().navigate(R.id.action_paymentFragment_to_dashboardCategoryNew)
                } else {
                    AlertUtils.showCustomAlert(requireContext(), "Please complete all payment.")
                }


            }

            R.id.txtOriginalAmount -> {

                if (paymentType == "Card") {
                    if (totalPrice > cashDiscountSurcharge) {
                        totalPrice -= cashDiscountSurcharge
                    }
                    setUpPaymentTypeWiseData("Cash")
                    paymentType = "Cash"
                } else {
                    paymentType = "Cash"
                    paymentAmount = when {

                        isSplitByNo -> {
                            ((totalPrice) / splitValue) + tipAmount
                        }
                        isSplitByAmount -> {
                            splitAfterAmount
                        }
                        else -> {
                            ((totalPrice + tipAmount))
                        }
                    }
                    makePayment()
                }
            }
            R.id.txtSecondAmount -> {
                finalPrice = totalPrice + tipAmount
                isCustomCash = true
                paymentAmount = secondValue.toDouble()
                totalPrice = paymentAmount

                makePayment()
            }
            R.id.txtThirdAmount -> {
                finalPrice = totalPrice + tipAmount
                isCustomCash = true
                paymentAmount = thirdValue
                totalPrice = paymentAmount
                makePayment()
            }
            R.id.txtFourthAmount -> {
                finalPrice = totalPrice + tipAmount
                isCustomCash = true
                paymentAmount = fourthValue
                totalPrice = paymentAmount
                makePayment()
            }


            R.id.llCash -> {
                if (paymentType == "Card") {
                    if (totalPrice > cashDiscountSurcharge) {
                        totalPrice -= cashDiscountSurcharge
                    }
                    setUpPaymentTypeWiseData("Cash")
                    paymentType = "Cash"
                } else {
                    paymentType = "Cash"
                    paymentAmount = when {

                        isSplitByNo -> {
                            ((totalPrice) / splitValue) + tipAmount
                        }
                        isSplitByAmount -> {
                            splitAfterAmount
                        }
                        else -> {
                            ((totalPrice + tipAmount))
                        }
                    }


                    makePayment()

                }


            }
            R.id.llCredit -> {
                paymentType = "Card"
                if (isSplitByAmount || isSplitByNo) {
                    totalPrice -= splitAfterAmount
                }
                setUpPaymentTypeWiseData("Card")

                val handler = Handler()
                handler.postDelayed({
                    makePaymentCreditCard()
                }, 2000)

            }


            R.id.txtCustom -> {
                val bundle = Bundle()
                bundle.putDouble("totalprice", ((totalPrice + tipAmount)))
                findNavController().navigate(
                    R.id.action_paymentFragment_to_customAmountFragment,
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
                            bundle.putDouble("totalPrice", totalPrice + tipAmount)
                        }
                        bundle.putInt("splitValue", splitValue)
                        findNavController().navigate(
                            R.id.action_paymentFragment_to_splitAmountFragment,
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

            R.id.txtAddTips -> {
                val bundle = Bundle()
                bundle.putDouble("totalPrice", totalPrice)
                bundle.putDouble("totalTip", tipAmount)
                findNavController().navigate(
                    R.id.action_paymentFragment_to_addTipsDialog,
                    bundle
                )
            }
        }


    }

    private fun createQueuePrinter(createOrder: CreateOrderResponse) {
        val listPrinter: List<Int> = listOf()
        val orderRequest = cartList?.let {

            viewModel.createOrderRequest(
                it,
                subTotalPrice,
                (totalPrice + tipAmount),
                totalServiceCharge,
                totalTax,
                prefProvider.getValue(Constants.ORDER_TYPE, "").toString(),
                future_delivery_date,
                future_delivery_date,
                true,
                totalDiscount,
                tipAmount,
                splitValue,
                redeemLoyaltyInfo,
                cashDiscountSurcharge,
                true,
                paymentType, cashDiscountType
            )
        }
        val createRequest = CreateQueuePrinterRequestModel(
            location_id = prefProvider.getValueInt(LOCATION_ID, 0),
            order_type = prefProvider.getValue(ORDER_TYPE, ""),
            printer_id = listPrinter,
            order_item_attributes = orderRequest?.order?.orderItemsAttributes ?: listOf(),
            order_data = orderRequest?.order ?: OrderAttributeRequestModel(),
            terminal_id = prefProvider.getValueInt(TERMINAL_ID, 0)

        )
        viewModel.createQueuePrinter(createRequest, createOrder)
    }

    private fun makePaymentCreditCard() {

        val myRequest = cartList?.let {

            viewModel.createOrderRequest(
                it,
                subTotalPrice,
                cardPaymentAmount,
                totalServiceCharge,
                totalTax,
                prefProvider.getValue(Constants.ORDER_TYPE, "").toString(),
                future_delivery_date,
                future_delivery_date,
                true,
                totalDiscount,
                tipAmount,
                splitValue,
                redeemLoyaltyInfo,
                cashDiscountSurcharge,
                true,
                paymentType, cashDiscountType
            )
        }
        if (myRequest != null) {
            viewModel.totalPayAmount(cardPaymentAmount)
            viewModel.submit(myRequest)
        }
    }


    private fun makePayment() {

        if (isUpdate)
            viewModel.updateOrder(
                true,
                orderId,
                paymentId,
                paymentOfflineId,
                orderOfflineId
            )

        if (isSplitByNo) {

            val myRequest = cartList?.let {

                viewModel.createOrderRequest(
                    it,
                    subTotalPrice,
                    (totalPrice + tipAmount),
                    totalServiceCharge,
                    totalTax,
                    prefProvider.getValue(Constants.ORDER_TYPE, "").toString(),
                    future_delivery_date,
                    future_delivery_date,
                    false,
                    totalDiscount,
                    tipAmount,
                    splitValue,
                    redeemLoyaltyInfo,
                    cashDiscountSurcharge,
                    true,
                    paymentType,
                    cashDiscountType
                )
            }
            if (myRequest != null) {
                viewModel.totalPayAmount(paymentAmount)

                val orderId = prefProvider.getValueInt("ORDER_ID", -1)

                if (orderId == -1) {
                    viewModel.submit(myRequest)
                } else {

                    val paymentReq = myRequest.order.paymentAttributes
                    if (paymentReq != null) {
                        paymentReq.order_id = orderId
                    }

                    val aa = SpitByOrderRequestModel(
                        orderId,
                        true,
                        paymentReq!!,
                        SpitByOrderPaymentModel(listOf(paymentReq))
                    )


                    viewModel.splitByOrder(aa!!, false)

                }


            }


        } else if (isSplitByAmount) {


            val tipAmount =
                (splitAfterAmount * tipAmount) / (totalPrice + tipAmount)

            val total = (totalPrice + tipAmount)

            val subTotalPrice = (splitAfterAmount * subTotalPrice) / total
            val totalServiceCharge =
                (splitAfterAmount * totalServiceCharge) / total
            val totalTax = (splitAfterAmount * totalTax) / total
            val totalDiscount = (splitAfterAmount * totalDiscount) / total


            val myRequest = cartList?.let {

                viewModel.createOrderRequest(
                    it,
                    subTotalPrice,
                    splitAfterAmount,
                    totalServiceCharge,
                    totalTax,
                    prefProvider.getValue(Constants.ORDER_TYPE, "").toString(),
                    future_delivery_date,
                    future_delivery_date,
                    false,
                    totalDiscount,
                    tipAmount,
                    splitValue,
                    redeemLoyaltyInfo,
                    cashDiscountSurcharge,
                    true,
                    paymentType,
                    cashDiscountType
                )
            }
            if (myRequest != null) {
                viewModel.totalPayAmount(paymentAmount)
                val orderId = prefProvider.getValueInt("ORDER_ID", -1)
                if (orderId == -1) {
                    viewModel.submit(myRequest)
                } else {

                    val paymentReq = myRequest.order.paymentAttributes
                    if (paymentReq != null) {
                        paymentReq.order_id = orderId
                    }

                    val aa = SpitByOrderRequestModel(
                        orderId,
                        false,
                        paymentReq!!,
                        SpitByOrderPaymentModel(listOf(paymentReq))
                    )


                    viewModel.splitByOrder(aa!!, false)

                }
            }


        } else {

            val myRequest = cartList?.let {

                viewModel.createOrderRequest(
                    it,
                    subTotalPrice,
                    (totalPrice + tipAmount),
                    totalServiceCharge,
                    totalTax,
                    prefProvider.getValue(Constants.ORDER_TYPE, "").toString(),
                    future_delivery_date,
                    future_delivery_date,
                    true,
                    totalDiscount,
                    tipAmount,
                    splitValue,
                    redeemLoyaltyInfo,
                    cashDiscountSurcharge,
                    true,
                    paymentType, cashDiscountType
                )
            }
            if (myRequest != null) {
                viewModel.totalPayAmount(paymentAmount)
                val orderId = prefProvider.getValueInt("ORDER_ID", -1)
                if (orderId == -1) {
                    viewModel.submit(myRequest)
                } else {

                    val paymentReq = myRequest.order.paymentAttributes
                    if (paymentReq != null) {
                        paymentReq.order_id = orderId
                    }

                    val aa = SpitByOrderRequestModel(
                        orderId,
                        true,
                        paymentReq!!,
                        SpitByOrderPaymentModel(listOf(paymentReq))
                    )

                    viewModel.splitByOrder(aa, false)

                }
            }
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

    }

    private fun observeQueueStart() {
        viewModel.QueueStart.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                createQueuePrinter(it)


            }
        })
    }

    private fun observeData() {

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {

                Log.e("observe : splitValue", splitValue.toString())

                prefProvider.setValueInt("ORDER_ID", it.data.order.id)

                when {
                    paymentType == "Card" -> {
                        val bundle = Bundle()
                        bundle.putDouble("totalPrice", cardPaymentAmount)
                        bundle.putDouble("paymentAmount", cardPaymentAmount)
                        orderId?.let { bundle.putInt("orderID", it) }
                        //bundle.putParcelable("receiptData", it.data)
                        bundle.putBoolean("isSpilt", false)
                        bundle.putBoolean("isDineIn", false)
                        bundle.putBoolean("isGuest", false)
                        bundle.putDouble("WholetotalPrice", WholetotalPrice)
                        bundle.putString("paymentType", "Card")
                        findNavController().navigate(
                            R.id.action_paymentFragment_to_orderCompleteFragment,
                            bundle
                        )
                        prefProvider.setValue(Constants.SPLIT_PAY_AMOUNT_DINE_IN, "")
                        prefProvider.setValueInt(Constants.SPLIT_NO_DINE_IN, -1)
                        prefProvider.setValue(Constants.SPLIT_PAY_TYPE_DINE_IN, "")
                        prefProvider.setValueInt("ORDER_ID", -1)
                    }
                    isCustomCash -> {
                        val bundle = Bundle()
                        bundle.putDouble("totalPrice", (totalPrice + tipAmount))
                        bundle.putDouble("paymentAmount", (totalPrice + tipAmount))
                        bundle.putDouble("finalPrice", finalPrice)
                        bundle.putInt("orderID", it.data.order.id)
                        bundle.putParcelable("receiptData", it.data)
                        bundle.putBoolean("isSpilt", false)
                        if ((totalPrice + tipAmount).equals(finalPrice)) {
                            bundle.putBoolean("isCustomCash", false)
                        } else {
                            bundle.putBoolean("isCustomCash", true)
                        }
                        bundle.putBoolean("isSplitByNo", false)
                        bundle.putBoolean("isSplitByAmount", false)
                        bundle.putString("paymentType", "Cash")
                        findNavController().navigate(
                            R.id.action_paymentFragment_to_orderCompleteFragment,
                            bundle
                        )

                        prefProvider.setValue(SPLIT_PAY_AMOUNT, "")
                        prefProvider.setValueInt(SPLIT_NO, -1)
                        prefProvider.setValue(SPLIT_PAY_TYPE, "")
                        prefProvider.setValueInt("ORDER_ID", -1)


                    }
                    isSplitByNo -> {

                        var payAmount = (((totalPrice) / splitValue) + tipAmount)
                        val bundle = Bundle()
                        bundle.putDouble("totalPrice", payAmount)
                        bundle.putDouble("WholetotalPrice", WholetotalPrice)
                        bundle.putDouble("paymentAmount", paymentAmount)
                        var paidAmountVal = payAmount
                        bundle.putInt("orderID", it.data.order.id)
                        bundle.putParcelable("receiptData", it.data)
                        if (prefProvider.getValue("PaidAmount", "").isNotEmpty()) {
                            paidAmountVal += String.format(
                                "%.2f", prefProvider.getValue("PaidAmount", "0.0")
                                    .toDouble()
                            ).toDouble()

                            prefProvider.setValue(
                                "PaidAmount",
                                String.format("%.2f", paidAmountVal)
                            )
                        } else {
                            prefProvider.setValue(
                                "PaidAmount",
                                String.format("%.2f", paidAmountVal)
                            )
                        }
                        bundle.putDouble(
                            "paidAmountValue",
                            String.format(
                                "%.2f", prefProvider.getValue("PaidAmount", "0.0")
                                    .toDouble()
                            ).toDouble()
                        )

                        if (WholetotalPrice <= String.format(
                                "%.2f", prefProvider.getValue("PaidAmount", "0.0")
                                    .toDouble()
                            ).toDouble()
                        ) {
                            bundle.putBoolean("isSpilt", false)
                            Log.d(
                                "yash",
                                "send: " + WholetotalPrice + " - " + prefProvider.getValue(
                                    "PaidAmount",
                                    "0.0"
                                ) + " = " + WholetotalPrice.minus(
                                    String.format(
                                        "%.2f", prefProvider.getValue("PaidAmount", "0.0")
                                            .toDouble()
                                    ).toDouble()
                                ) + " false"
                            )
                        } else {
                            Log.d(
                                "yash",
                                "send: " + WholetotalPrice + " - " + prefProvider.getValue(
                                    "PaidAmount",
                                    "0.0"
                                ) + " = " + WholetotalPrice.minus(
                                    String.format(
                                        "%.2f", prefProvider.getValue("PaidAmount", "0.0")
                                            .toDouble()
                                    ).toDouble()
                                ) + " true"
                            )
                            bundle.putBoolean("isSpilt", true)
                        }
                        bundle.putInt("splitValue", splitValue)
                        bundle.putBoolean("isSplitByNo", isSplitByNo)
                        bundle.putBoolean("isSplitByAmount", isSplitByAmount)
                        bundle.putString("paymentType", "Cash")
                        bundle.putDouble(
                            "remainingAmount",
                            (totalPrice) - (payAmount - tipAmount)
                        )
                        bundle.putDouble("payAmount", payAmount)
                        findNavController().navigate(
                            R.id.action_paymentFragment_to_orderCompleteFragment,
                            bundle
                        )

                        val splitPayAmount = prefProvider.getValue(SPLIT_PAY_AMOUNT, "")
                        if (splitPayAmount.isNotEmpty()) {
                            payAmount += splitPayAmount.toDouble()
                        }


                        prefProvider.setValue(SPLIT_PAY_TYPE, SPLIT_NO)
                        prefProvider.setValue(SPLIT_PAY_AMOUNT, (payAmount - tipAmount).toString())
                        prefProvider.setValueInt(SPLIT_NO, splitValue)

                    }
                    isSplitByAmount -> {

                        var payAmount = splitAfterAmount
                        val bundle = Bundle()
                        bundle.putDouble("totalPrice", payAmount)
                        bundle.putDouble("paymentAmount", paymentAmount)
                        bundle.putInt("orderID", it.data.order.id)
                        bundle.putParcelable("receiptData", it.data)
                        bundle.putString("paymentType", "Cash")

                        bundle.putBoolean("isSplitByNo", isSplitByNo)
                        bundle.putBoolean("isSplitByAmount", isSplitByAmount)

                        if (MethodUtils.roundOffAmountDouble(payAmount) != MethodUtils.roundOffAmountDouble(
                                totalPrice
                            )
                        ) {

                            bundle.putBoolean("isSpilt", true)
                            bundle.putDouble("remainingAmount", totalPrice - payAmount)
                            bundle.putDouble("payAmount", payAmount)


                            val splitPayAmount = prefProvider.getValue(SPLIT_PAY_AMOUNT, "")
                            if (splitPayAmount.isNotEmpty()) {
                                payAmount += splitPayAmount.toDouble()
                            }
                            prefProvider.setValue(SPLIT_PAY_AMOUNT, payAmount.toString())
                            prefProvider.setValueInt(SPLIT_NO, splitValue)
                            prefProvider.setValue(SPLIT_PAY_TYPE, SPLIT_PAY_AMOUNT)
                        } else {
                            bundle.putBoolean("isSpilt", false)
                            bundle.putDouble(
                                "remainingAmount",
                                ((totalPrice + tipAmount)) - payAmount
                            )

                            prefProvider.setValue(SPLIT_PAY_AMOUNT, "")
                            prefProvider.setValueInt(SPLIT_NO, -1)
                            prefProvider.setValue(SPLIT_PAY_TYPE, "")
                        }

                        findNavController().navigate(
                            R.id.action_paymentFragment_to_orderCompleteFragment,
                            bundle
                        )

                    }
                    else -> {
                        val bundle = Bundle()
                        bundle.putDouble("totalPrice", (totalPrice + tipAmount))
                        bundle.putDouble("paymentAmount", paymentAmount)
                        bundle.putInt("orderID", it.data.order.id)
                        bundle.putParcelable("receiptData", it.data)
                        bundle.putBoolean("isSpilt", false)
                        bundle.putBoolean("isSplitByNo", isSplitByNo)
                        bundle.putBoolean("isSplitByAmount", isSplitByAmount)
                        bundle.putString("paymentType", "Cash")
                        findNavController().navigate(
                            R.id.action_paymentFragment_to_orderCompleteFragment,
                            bundle
                        )

                        prefProvider.setValue(SPLIT_PAY_AMOUNT, "")
                        prefProvider.setValueInt(SPLIT_NO, -1)
                        prefProvider.setValue(SPLIT_PAY_TYPE, "")
                        prefProvider.setValueInt("ORDER_ID", -1)


                    }
                }
            }
        })

    }

    private fun queuePrinterObserver() {
        viewModel.queuePrinter.observe(requireActivity(), {
            it.getContentIfNotHandled()?.let { data ->
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, data.toString()
                    ) { _, _ ->
                        val navController = findNavController()
                        navController.popBackStack()
                    }
                }

            }
        })
    }
}