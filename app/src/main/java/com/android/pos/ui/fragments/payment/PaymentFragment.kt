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
import com.android.pos.data.model.requestModel.SpitByOrderPaymentModel
import com.android.pos.data.model.requestModel.SpitByOrderRequestModel
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.SPLIT_NO
import com.android.pos.data.remote.Constants.SPLIT_PAY_AMOUNT
import com.android.pos.data.remote.Constants.SPLIT_PAY_TYPE
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
    private var totalDiscount: Double = 0.0
    private var subTotalPrice: Double = 0.0
    private var totalTax: Double = 0.0
    private var totalServiceCharge: Double = 0.0
    private var paymentAmount: Double = 0.0
    var cashDiscountData: CashDiscountModel? = null
    private lateinit var binding: PaymentFragmentBinding
    private val TAG = "PaymentFragment"
    var final_discount = 0.0
    var paymentType = "Cash"
    var cashDiscountType = "CashDiscount"
    var cardPaymentAmount = 0.0

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
        cartList = requireArguments().getParcelable("cartList")
        totalPrice = requireArguments().getDouble("totalPrice")
        subTotalPrice = requireArguments().getDouble("subTotalPrice")
        totalTax = requireArguments().getDouble("totalTax")
        totalServiceCharge = requireArguments().getDouble("totalServiceCharge")
        totalDiscount = requireArguments().getDouble("totalDiscount")
        future_delivery_time = requireArguments().getString("future_delivery_time").toString()
        future_delivery_date = requireArguments().getString("future_delivery_date").toString()
        getSerchargeCashDisDetail()
        callbackSetup()
        observeShowProgress()
        observeData()


        return binding.root
    }


    @SuppressLint("SetTextI18n")
    private fun callbackSetup() {

        setFragmentResultListener("request_key_split") { requestKey: String, bundle: Bundle ->
            splitValue = bundle.getInt("split")

            if (splitValue != -1) {

                isSplitByNo = true
                isSplitByAmount = false
                splitAfterAmount = ((totalPrice + tipAmount) - final_discount) / splitValue

                MethodUtils.setPriceTextView(binding.txtTotalAmount, splitAfterAmount)
                val totalAmountFormat = MethodUtils.roundOffAmount(totalPrice - final_discount)

                binding.txtSplitValue.text =
                    "Out of $totalAmountFormat Total, Payment 1 of $splitValue"

                getCashPaymentOptionList(splitAfterAmount)
            } else {
                isSplitByAmount = true
                isSplitByNo = false
                val splitValue = bundle.getDouble("splitByAmount")

                splitAfterAmount = splitValue

                MethodUtils.setPriceTextView(binding.txtTotalAmount, splitAfterAmount)
                val totalAmountFormat = MethodUtils.roundOffAmount(totalPrice - final_discount)

                binding.txtSplitValue.text =
                    "Out of $totalAmountFormat Total, Payment 1 of $splitValue"

                getCashPaymentOptionList(splitAfterAmount)
            }
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
            if (splitValue == -1) (totalPrice - final_discount) else (totalPrice - final_discount) / splitValue

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
    private fun setupData(optionType: String) {

        totalPrice = requireArguments().getDouble("totalPrice")
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

        if (paymentType == "Card") {
            if (optionType == "SurCharge") {
                binding.txtCashdiscount.text = "- $" + String.format("%.2f", 0.0)
                MethodUtils.setPriceTextView(
                    binding.txtTotal,
                    (totalPrice + tipAmount) + final_discount
                )
                MethodUtils.setPriceTextView(
                    binding.txtTotalAmount,
                    (totalPrice + tipAmount) + final_discount
                )
                binding.linearCashdiiscount.visibility = View.GONE
                binding.linearnoncashAdj.visibility = View.VISIBLE
                cardPaymentAmount = (totalPrice + tipAmount) + final_discount
                binding.txtNoncashAdj.text = "+ $" + String.format("%.2f", final_discount)
            } else {
                binding.linearCashdiiscount.visibility = View.GONE
                binding.linearnoncashAdj.visibility = View.GONE
                binding.txtCashdiscount.text = "- $" + String.format("%.2f", final_discount)
                MethodUtils.setPriceTextView(
                    binding.txtTotal,
                    (totalPrice + tipAmount)
                )
                MethodUtils.setPriceTextView(
                    binding.txtTotalAmount,
                    (totalPrice + tipAmount)
                )
                cardPaymentAmount = (totalPrice + tipAmount)
                binding.txtNoncashAdj.text = "- $" + String.format("%.2f", 0.0)
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


        } else {
            if (optionType == "SurCharge") {
                binding.linearCashdiiscount.visibility = View.GONE
                binding.linearnoncashAdj.visibility = View.GONE
                binding.txtCashdiscount.text = "- $" + String.format("%.2f", 0.0)
                MethodUtils.setPriceTextView(binding.txtTotal, (totalPrice + tipAmount))
                binding.txtNoncashAdj.text = "- $" + String.format("%.2f", final_discount)
            } else {
                binding.linearCashdiiscount.visibility = View.VISIBLE
                binding.linearnoncashAdj.visibility = View.GONE
                binding.txtCashdiscount.text = "- $" + String.format("%.2f", final_discount)
                MethodUtils.setPriceTextView(
                    binding.txtTotal,
                    (totalPrice + tipAmount) - final_discount
                )
                binding.txtNoncashAdj.text = "- $" + String.format("%.2f", 0.0)
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

            isUpdate = requireArguments().getBoolean("update")
            if (isUpdate) {

                orderId = requireArguments().getInt("orderId")
                paymentId = requireArguments().getInt("paymentId")
                paymentOfflineId = requireArguments().getString("paymentOfflineId").toString()
                orderOfflineId = requireArguments().getString("orderOfflineId").toString()
            }

            val splitPayType = prefProvider.getValue(SPLIT_PAY_TYPE, "")


            val splitPayAmount = prefProvider.getValue(SPLIT_PAY_AMOUNT, "")

            if (splitPayType == SPLIT_NO) {
                tipAmount = 0.0

                if (splitPayAmount.isNotEmpty()) {
                    totalPrice -= splitPayAmount.toDouble()

                    val splitNo = prefProvider.getValueInt(SPLIT_NO, -1)
                    if (splitNo != -1) {

                        val split_subTotalPrice = subTotalPrice / splitNo
                        subTotalPrice -= split_subTotalPrice

                        val split_totalTax = totalTax / splitNo
                        totalTax -= split_totalTax

                        val split_totalServiceCharge = totalServiceCharge / splitNo
                        totalServiceCharge -= split_totalServiceCharge

                        val split_totalDiscount = totalDiscount / splitNo
                        totalDiscount -= split_totalDiscount

//                    val split_tip_amount = tipAmount / splitNo
//                    tipAmount -= split_tip_amount
//
//                    totalPrice += tipAmount
                    }

                }
            } else if (splitPayType == SPLIT_PAY_AMOUNT) {


                if (splitPayAmount.isNotEmpty()) {


                    val tipAmount1 =
                        (splitPayAmount.toDouble() * tipAmount) / (totalPrice + tipAmount) - final_discount

                    val total = ((totalPrice - final_discount) + tipAmount1)

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

            MethodUtils.setPriceTextView(
                binding.txtTotalAmount,
                (totalPrice + tipAmount) - final_discount
            )
            getCashPaymentOptionList((totalPrice + tipAmount) - final_discount)

        }




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

        Log.d(TAG, "onClick: $cardPaymentAmount")
    }

    private fun getSerchargeCashDisDetail() {
        viewModel.getCashDiscountDetails(active = 1)
            ?.observe(viewLifecycleOwner, { cashDiscountData ->
                if (cashDiscountData != null) {
                    if (paymentType == "Cash") {
                        if (cashDiscountData.option_type == "SurCharge") {
                            if (cashDiscountData.amount_type == "Dollar") {
                                final_discount = cashDiscountData.rate_or_amount
                            } else if (cashDiscountData.amount_type == "Percentage") {
                                final_discount =
                                    subTotalPrice * 100 / cashDiscountData.rate_or_amount
                            }
                        } else if (cashDiscountData.option_type == "CashDiscount") {
                            if (cashDiscountData.amount_type == "Dollar") {
                                final_discount = cashDiscountData.rate_or_amount
                            } else if (cashDiscountData.amount_type == "Percentage") {
                                final_discount =
                                    subTotalPrice * 100 / cashDiscountData.rate_or_amount
                            }
                        }
                    }

                }
                cashDiscountType = cashDiscountData.option_type
                setupData(cashDiscountData.option_type)
            })


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


                paymentAmount = when {

                    isSplitByNo -> {

                        ((totalPrice - final_discount) / splitValue) + tipAmount

                    }
                    isSplitByAmount -> {
                        splitAfterAmount
                    }
                    else -> {
                        ((totalPrice + tipAmount) - final_discount)
                    }
                }


                makePayment()
            }
            R.id.txtSecondAmount -> {
                paymentAmount = secondValue.toDouble()
                makePayment()
            }
            R.id.txtThirdAmount -> {
                paymentAmount = thirdValue
                makePayment()
            }
            R.id.txtFourthAmount -> {
                paymentAmount = fourthValue
                makePayment()
            }


            R.id.llCash -> {
                if (paymentType == "Card") {
                    getSerchargeCashDisDetail()
                    paymentType = "Cash"
                } else {
                    paymentType = "Cash"
                    paymentAmount = when {

                        isSplitByNo -> {
                            ((totalPrice - final_discount) / splitValue) + tipAmount
                        }
                        isSplitByAmount -> {
                            splitAfterAmount
                        }
                        else -> {
                            ((totalPrice + tipAmount) - final_discount)
                        }
                    }


                    makePayment()

                }


            }
            R.id.llCredit -> {
                paymentType = "Card"
                getSerchargeCashDisDetail()

                val handler = Handler()
                handler.postDelayed({
                    makePaymentCreditCard()
                }, 2000)

            }


            R.id.txtCustom -> {
                findNavController().navigate(R.id.action_paymentFragment_to_customAmountFragment)
            }
            R.id.txtSplitAmount -> {
                if (totalPrice == 0.0) {

                    AlertUtils.showCustomAlert(
                        requireActivity(),
                        "You can't split amount less then 1."
                    )

                } else {
                    if (tipAmount == 0.0) {
                        val bundle = Bundle()
                        bundle.putDouble("totalPrice", (totalPrice + tipAmount - final_discount))
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
                final_discount,
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

        if (MethodUtils.roundOffAmountDouble(splitAfterAmount) == MethodUtils.roundOffAmountDouble(
                totalPrice - final_discount
            )
        ) {
            isSplitByNo = false
            isSplitByAmount = false
        }

        if (isSplitByNo) {

            val myRequest = cartList?.let {

                viewModel.createOrderRequest(
                    it,
                    subTotalPrice,
                    (totalPrice + tipAmount) - final_discount,
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
                    final_discount,
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
                (splitAfterAmount * tipAmount) / (totalPrice + tipAmount) - final_discount

            val total = (totalPrice + tipAmount) - final_discount

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
                    final_discount,
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
                    (totalPrice + tipAmount) - final_discount,
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
                    final_discount,
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

                    isSplitByNo -> {

                        var payAmount = (((totalPrice - final_discount) / splitValue) + tipAmount)
                        val bundle = Bundle()
                        bundle.putDouble("totalPrice", payAmount)
                        bundle.putDouble("paymentAmount", paymentAmount)
                        bundle.putInt("orderID", it.data.order.id)
                        bundle.putParcelable("receiptData", it.data)
                        bundle.putBoolean("isSpilt", true)
                        bundle.putInt("splitValue", splitValue)
                        bundle.putString("paymentType", "Cash")
                        bundle.putDouble(
                            "remainingAmount",
                            (totalPrice - final_discount) - (payAmount - tipAmount)
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
                                ((totalPrice + tipAmount) - final_discount) - payAmount
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
                        bundle.putDouble("totalPrice", (totalPrice + tipAmount) - final_discount)
                        bundle.putDouble("paymentAmount", paymentAmount)
                        bundle.putInt("orderID", it.data.order.id)
                        bundle.putParcelable("receiptData", it.data)
                        bundle.putBoolean("isSpilt", false)
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
}