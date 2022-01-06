package com.android.pos.ui.fragments.payment

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.android.pos.data.model.requestModel.CreateQueuePrinterRequestModel
import com.android.pos.data.model.requestModel.OrderAttributeRequestModel
import com.android.pos.data.model.requestModel.SpitByOrderPaymentModel
import com.android.pos.data.model.requestModel.SpitByOrderRequestModel
import com.android.pos.data.model.responseModel.CreateOrderResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.CASH_DISCOUNT_SURCHARGE
import com.android.pos.data.remote.Constants.LOCATION_ID
import com.android.pos.data.remote.Constants.ORDER_TYPE
import com.android.pos.data.remote.Constants.SERVICE_CHARGE
import com.android.pos.data.remote.Constants.SPLIT_NO
import com.android.pos.data.remote.Constants.SPLIT_PAY_AMOUNT
import com.android.pos.data.remote.Constants.SPLIT_PAY_TYPE
import com.android.pos.data.remote.Constants.SUB_TOTAL
import com.android.pos.data.remote.Constants.TAX_CHARGE
import com.android.pos.data.remote.Constants.TERMINAL_ID
import com.android.pos.data.remote.Constants.TIP
import com.android.pos.data.remote.Constants.TOTAL_DISCOUNT
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
    private var remainingAmount: Double = 0.0
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
                remainingAmount =
                    String.format("%.2f", it.getDouble("remainingAmount", 0.0)).toDouble()
                splitValue = it.getInt("splitvalue", -1)
                isSplitByAmount = it.getBoolean("isSplitByAmount", false)
                isSplitByNo = it.getBoolean("isSplitByNo", false)
                setSplitData()
            }

        cartList = requireArguments().getParcelable("cartList")
        totalPrice = requireArguments().getDouble("totalPrice")
        subTotalPrice = requireArguments().getDouble("subTotalPrice")
        totalTax = requireArguments().getDouble("totalTax")
        totalServiceCharge = requireArguments().getDouble("totalServiceCharge")
        totalDiscount = requireArguments().getDouble("totalDiscount")
        cashDiscountType = prefProvider.getValue(Constants.OPTION_TYPE, "")



        if (MethodUtils.isEnableCashDiscount(requireContext())) {
            if (prefProvider.getValue(CASH_DISCOUNT_SURCHARGE, "").isEmpty()) {
                cashDiscountSurcharge = MethodUtils.calculateCashDiscount(
                    totalPrice,
                    prefProvider,
                    requireContext()
                )
                prefProvider.setValue(
                    CASH_DISCOUNT_SURCHARGE,
                    String.format("%.2f", cashDiscountSurcharge)
                )
            } else {
                cashDiscountSurcharge =
                    prefProvider.getValue(CASH_DISCOUNT_SURCHARGE, "").toDouble()
            }
        } else {
            cashDiscountSurcharge = 0.0
        }

        setUpPaymentSummary()

        if (prefProvider.getValue("WholeTotal", "").isEmpty()) {
            prefProvider.setValue("WholeTotal", String.format("%.2f", totalPrice))
        } else {
            WholetotalPrice = prefProvider.getValue("WholeTotal", "").toDouble()
        }

        if (prefProvider.getValue(SUB_TOTAL, "").isEmpty()) {
            prefProvider.setValue(SUB_TOTAL, String.format("%.2f", subTotalPrice))
        } else {
            subTotalPrice = prefProvider.getValue(SUB_TOTAL, "").toDouble()
        }

        if (prefProvider.getValue(TAX_CHARGE, "").isEmpty()) {
            prefProvider.setValue(TAX_CHARGE, String.format("%.2f", totalTax))
        } else {
            totalTax = prefProvider.getValue(TAX_CHARGE, "").toDouble()
        }


        if (prefProvider.getValue(SERVICE_CHARGE, "").isEmpty()) {
            prefProvider.setValue(SERVICE_CHARGE, String.format("%.2f", totalServiceCharge))
        } else {
            totalServiceCharge = prefProvider.getValue(SERVICE_CHARGE, "").toDouble()
        }


        if (prefProvider.getValue(TOTAL_DISCOUNT, "").isEmpty()) {
            prefProvider.setValue(TOTAL_DISCOUNT, String.format("%.2f", totalDiscount))
        } else {
            totalDiscount = prefProvider.getValue(TOTAL_DISCOUNT, "").toDouble()
        }


        if (prefProvider.getValue(TIP, "").isEmpty()) {
            prefProvider.setValue(TIP, String.format("%.2f", tipAmount))
        } else {
            tipAmount = prefProvider.getValue(TIP, "").toDouble()
        }






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

    @SuppressLint("SetTextI18n")
    private fun setSplitData() {
        if (isNextPayment) {
            if (isSplitByNo) {
                if (MethodUtils.isEnableCashDiscount(requireContext())) {
                    var last_cash_discount_surcharge =
                        prefProvider.getValue(CASH_DISCOUNT_SURCHARGE, "")//3.33
                    if (last_cash_discount_surcharge.isEmpty() || last_cash_discount_surcharge == "0.0") {
                        last_cash_discount_surcharge = "0.0"
                    }
                    cardPaymentAmount =
                        (remainingAmount + last_cash_discount_surcharge.toDouble())
                    binding.txtCardAmount.text = "$ " + String.format("%.2f", cardPaymentAmount)
                    cashDiscountSurcharge =
                        prefProvider.getValue(CASH_DISCOUNT_SURCHARGE, "").toDouble()
                } else {
                    cardPaymentAmount = remainingAmount
                    binding.txtCardAmount.text =
                        "$" + String.format("%.2f", remainingAmount)
                }
                MethodUtils.setPriceTextView(binding.txtTotalAmount, remainingAmount)
                getCashPaymentOptionList(remainingAmount)

                isSplitByNo = false

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
                val totalDiscount1 = (splitAfterAmount * totalDiscount) / total
                val cashDiscount1 = (splitAfterAmount * cashDiscountSurcharge) / total

                subTotalPrice = subTotalPrice1
                totalTax = totalTax1
                totalServiceCharge = totalServiceCharge1
                totalDiscount = totalDiscount1
                cashDiscountSurcharge = cashDiscount1

                if (MethodUtils.isEnableCashDiscount(requireContext())) {
                    if (cashDiscountType == "CashDiscount") {
                        cardPaymentAmount = splitAfterAmount + cashDiscountSurcharge
                        binding.txtCardAmount.text = "$ " + String.format("%.2f", cardPaymentAmount)
                    } else if (cashDiscountType == "SurCharge") {
                        cardPaymentAmount = splitAfterAmount - cashDiscountSurcharge
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
                    cardPaymentAmount = (splitAfterAmount + (cashDiscountSurcharge / splitValue))
                    binding.txtCardAmount.text =
                        "$ " + String.format(
                            "%.2f",
                            (splitAfterAmount + (cashDiscountSurcharge / splitValue))
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
                val totalDiscount1 = (splitAfterAmount * totalDiscount) / total
                val cashDiscount1 = (splitAfterAmount * cashDiscountSurcharge) / total


                totalPrice = splitAfterAmount
                subTotalPrice = subTotalPrice1
                totalTax = totalTax1
                totalServiceCharge = totalServiceCharge1
                totalDiscount = totalDiscount1
                cashDiscountSurcharge = cashDiscount1

                if (MethodUtils.isEnableCashDiscount(requireContext())) {
                    if (cashDiscountType == "CashDiscount") {
                        cardPaymentAmount = splitAfterAmount + (cashDiscountSurcharge)
                        binding.txtCardAmount.text = "$ " + String.format("%.2f", cardPaymentAmount)
                    } else if (cashDiscountType == "SurCharge") {
                        cardPaymentAmount = splitAfterAmount - cashDiscountSurcharge
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

    private fun setUpPaymentSummary() {
        MethodUtils.setPriceTextView(binding.txtTotal, totalPrice)
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
        if (MethodUtils.isEnableCashDiscount(requireContext())) {
            if (cashDiscountType == "CashDiscount") {
                binding.txtCardAmount.text = "$ " + String.format("%.2f", totalPrice)
                cardPaymentAmount = totalPrice
                binding.linearnoncashAdj.visibility = View.VISIBLE
                binding.txtNoncashAdj.text = "$ " + String.format(
                    "%.2f",
                    MethodUtils.calculateCashDiscount(totalPrice, prefProvider, requireContext())
                )
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
        MethodUtils.setPriceTextView(binding.txtTotalAmount, totalPrice)
        getCashPaymentOptionList((totalPrice + tipAmount))
    }

    private fun setUpPaymentTypeWiseData(payType: String) {
        if (payType == "Cash") {
            if (MethodUtils.isEnableCashDiscount(requireContext())) {
                if (cashDiscountType == "CashDiscount") {
                    binding.linearnoncashAdj.visibility = View.VISIBLE
                    binding.txtNoncashAdj.text = "$ " + String.format("%.2f", cashDiscountSurcharge)
                    if (splitValue != -1) {
                        totalPrice -= (cashDiscountSurcharge / splitValue)
                    } else {
                        totalPrice -= cashDiscountSurcharge
                    }
                } else if (cashDiscountType == "SurCharge") {
                    if (!isSplitByAmount && !isSplitByNo && !isCustomCash && splitValue == -1) {
                        binding.linearnoncashAdj.visibility = View.GONE
                        MethodUtils.setPriceTextView(binding.txtTotal, WholetotalPrice)
                    } else {
                        binding.linearnoncashAdj.visibility = View.GONE
                    }
                }
            } else {
                binding.linearnoncashAdj.visibility = View.GONE
            }

            MethodUtils.setPriceTextView(
                binding.txtTotalAmount,
                totalPrice / splitValue
            )


        } else if (payType == "Card") {
            if (MethodUtils.isEnableCashDiscount(requireContext())) {
                if (cashDiscountType == "CashDiscount") {
                    if (!isSplitByAmount && !isSplitByNo && !isCustomCash && splitValue == -1) {
                        binding.linearnoncashAdj.visibility = View.GONE
                        MethodUtils.setPriceTextView(binding.txtTotal, cardPaymentAmount)
                    } else {
                        binding.linearnoncashAdj.visibility = View.GONE
                    }
                } else if (cashDiscountType == "SurCharge") {
                    binding.linearnoncashAdj.visibility = View.VISIBLE
                    if (splitValue != -1) {
                        binding.txtNoncashAdj.text =
                            "$ " + String.format("%.2f", cashDiscountSurcharge / splitValue)
                        totalPrice += (cashDiscountSurcharge / splitValue)
                    } else {
                        binding.txtNoncashAdj.text =
                            "$ " + String.format("%.2f", cashDiscountSurcharge)
                        totalPrice += (cashDiscountSurcharge)
                    }
                }
            } else {
                binding.linearnoncashAdj.visibility = View.GONE
            }
            MethodUtils.setPriceTextView(binding.txtTotalAmount, cardPaymentAmount)

        }
    }


    @SuppressLint("SetTextI18n")
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
        setFragmentResultListener("request_for_customAmount") { requestKey: String, bundle: Bundle ->
            val amounnt = bundle.getDouble("amount")
            isCustomCash = true
            isSplitByNo = false
            isNextPayment = false
            isSplitByAmount = false
            splitValue = -1
            paymentAmount = amounnt
            cardPaymentAmount = paymentAmount + cashDiscountSurcharge
            binding.txtCardAmount.text = "$ " + String.format("%.2f", cardPaymentAmount)
            MethodUtils.setPriceTextView(binding.txtTotalAmount, paymentAmount)
            getCashPaymentOptionList(paymentAmount)
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
                paymentType = "Cash"
                paymentAmount = when {
                    isSplitByNo -> {
                        var remainningCashDiscount =
                            cashDiscountSurcharge - (cashDiscountSurcharge / splitValue)
                        prefProvider.setValue(
                            CASH_DISCOUNT_SURCHARGE,
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
                makePayment()

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
                paymentType = "Cash"
                paymentAmount = when {
                    isSplitByNo -> {
                        var remainningCashDiscount =
                            cashDiscountSurcharge - (cashDiscountSurcharge / splitValue)
                        prefProvider.setValue(
                            CASH_DISCOUNT_SURCHARGE,
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
                makePayment()

            }
            R.id.llCredit -> {
                paymentType = "Card"
                setUpPaymentTypeWiseData("Card")
                if (isSplitByNo) {
                    var remainningCashDiscount =
                        cashDiscountSurcharge - (cashDiscountSurcharge / splitValue)
                    prefProvider.setValue(
                        CASH_DISCOUNT_SURCHARGE,
                        String.format("%.2f", remainningCashDiscount)
                    )
                } else if (isSplitByAmount) {
                    prefProvider.setValue(
                        CASH_DISCOUNT_SURCHARGE,
                        String.format("%.2f", cashDiscountSurcharge)
                    )
                }
                makePaymentCreditCard()
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
                viewModel.createOrderRequestForCard(
                    it,
                    subTotalPrice / splitValue,
                    cardPaymentAmount,
                    totalServiceCharge / splitValue,
                    totalTax / splitValue,
                    prefProvider.getValue(Constants.ORDER_TYPE, "").toString(),
                    future_delivery_date,
                    future_delivery_date,
                    true,
                    totalDiscount / splitValue,
                    tipAmount,
                    splitValue,
                    redeemLoyaltyInfo,
                    cashDiscountSurcharge / splitValue,
                    true,
                    paymentType, cashDiscountType
                )
            }
            if (myRequest != null) {
                viewModel.totalPayAmount(cardPaymentAmount)
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
        } else if (isSplitByAmount) {
            try {

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
                    viewModel.totalPayAmount(cardPaymentAmount)
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
            } catch (e: Exception) {
                Log.d(TAG, "makePayment: " + e.toString())
            }
        } else {


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
                    subTotalPrice / splitValue,
                    paymentAmount,
                    totalServiceCharge / splitValue,
                    totalTax / splitValue,
                    prefProvider.getValue(Constants.ORDER_TYPE, "").toString(),
                    future_delivery_date,
                    future_delivery_date,
                    false,
                    totalDiscount / splitValue,
                    tipAmount / splitValue,
                    splitValue,
                    redeemLoyaltyInfo,
                    cashDiscountSurcharge / splitValue,
                    true,
                    paymentType,
                    cashDiscountType
                )
            }
            if (myRequest != null) {
                viewModel.totalPayAmount((paymentAmount))

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
            try {
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
                    viewModel.totalPayAmount(splitAfterAmount)
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
            } catch (e: Exception) {
                Log.d(TAG, "makePayment: " + e.toString())
            }
        } else if (isCustomCash) {

            val myRequest = cartList?.let {
                viewModel.createOrderRequest(
                    it,
                    subTotalPrice,
                    totalPrice,
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
                viewModel.totalPayAmount(totalPrice)
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
        } else {

            val myRequest = cartList?.let {

                viewModel.createOrderRequest(
                    it,
                    subTotalPrice,
                    paymentAmount,
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

    private fun observeData() {

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {

                Log.e("observe : splitValue", splitValue.toString())

                prefProvider.setValueInt("ORDER_ID", it.data.order.id)

                when {
                    paymentType == "Card" -> {
                        when {
                            isSplitByNo -> {
                                val bundle = Bundle()
                                bundle.putDouble("PaidAmount", cardPaymentAmount)
                                var wholetotalPriceTemp = String.format(
                                    "%.2f",
                                    prefProvider.getValue("WholeTotal", "").toDouble()
                                )
                                bundle.putDouble("WholetotalPrice", wholetotalPriceTemp.toDouble())
                                var remainingAmount = 0.0

                                var tempCashDiscount = 0.0
                                if (cardPaymentAmount > WholetotalPrice) {
                                    tempCashDiscount = cardPaymentAmount - WholetotalPrice
                                    remainingAmount =
                                        cardPaymentAmount - WholetotalPrice - tempCashDiscount
                                } else {

                                    val dis_charge_value =
                                        if (splitValue == -1) cashDiscountSurcharge else cashDiscountSurcharge / splitValue

                                    bundle.putDouble("dis_charge_value", dis_charge_value)
                                    remainingAmount =
                                        wholetotalPriceTemp.toDouble() + dis_charge_value - cardPaymentAmount
                                }

                                prefProvider.setValue(
                                    "WholeTotal",
                                    String.format("%.2f", remainingAmount)
                                )
                                bundle.putDouble(
                                    "remainingAmount",
                                    remainingAmount
                                )
                                bundle.putInt("orderID", it.data.order.id)
                                bundle.putParcelable("receiptData", it.data)
                                bundle.putInt("splitValue", splitValue)
                                if (splitValue != -1) {
                                    if (remainingAmount <= 0.0) {
                                        bundle.putBoolean("isSpilt", false)
                                        prefProvider.setValue(SUB_TOTAL, "")
                                        prefProvider.setValue(TOTAL_DISCOUNT, "")
                                        prefProvider.setValue(TIP, "")
                                        prefProvider.setValue(TAX_CHARGE, "")
                                        prefProvider.setValue(SERVICE_CHARGE, "")
                                    } else {
                                        bundle.putBoolean("isSpilt", true)
                                        setPaymentAttriButes(SUB_TOTAL, splitValue)
                                        setPaymentAttriButes(SERVICE_CHARGE, splitValue)
                                        setPaymentAttriButes(TAX_CHARGE, splitValue)
                                        setPaymentAttriButes(TIP, splitValue)
                                        setPaymentAttriButes(TOTAL_DISCOUNT, splitValue)
                                    }
                                } else {
                                    bundle.putBoolean("isSpilt", false)
                                    prefProvider.setValue(SUB_TOTAL, "")
                                    prefProvider.setValue(TOTAL_DISCOUNT, "")
                                    prefProvider.setValue(TIP, "")
                                    prefProvider.setValue(TAX_CHARGE, "")
                                    prefProvider.setValue(SERVICE_CHARGE, "")
                                }

                                bundle.putBoolean("isSplitByNo", isSplitByNo)
                                bundle.putBoolean("isSplitByAmount", isSplitByAmount)
                                bundle.putString("paymentType", "Card")
                                bundle.putBoolean("isDineIn", false)
                                findNavController().navigate(
                                    R.id.action_paymentFragment_to_orderCompleteFragment,
                                    bundle
                                )
                                prefProvider.setValue(SPLIT_PAY_TYPE, SPLIT_NO)
                                prefProvider.setValue(
                                    SPLIT_PAY_AMOUNT,
                                    (splitAfterAmount - tipAmount).toString()
                                )
                                prefProvider.setValueInt(SPLIT_NO, splitValue)

                            }
                            isSplitByAmount -> {
                                val bundle = Bundle()
                                bundle.putDouble("PaidAmount", cardPaymentAmount)
                                var wholetotalPriceTemp = String.format(
                                    "%.2f",
                                    prefProvider.getValue("WholeTotal", "").toDouble()
                                )
                                bundle.putDouble("WholetotalPrice", wholetotalPriceTemp.toDouble())
                                var remainingAmount = 0.0

                                var tempCashDiscount = 0.0
                                if (cardPaymentAmount > WholetotalPrice) {
                                    tempCashDiscount = cardPaymentAmount - WholetotalPrice
                                    remainingAmount =
                                        cardPaymentAmount - WholetotalPrice - tempCashDiscount
                                } else {

                                    val dis_charge_value =
                                        if (splitValue == -1) cashDiscountSurcharge else cashDiscountSurcharge / splitValue

                                    bundle.putDouble("dis_charge_value", dis_charge_value)
                                    remainingAmount =
                                        wholetotalPriceTemp.toDouble() + dis_charge_value - cardPaymentAmount
                                }

                                prefProvider.setValue(
                                    "WholeTotal",
                                    String.format("%.2f", remainingAmount)
                                )
                                bundle.putDouble(
                                    "remainingAmount",
                                    remainingAmount
                                )
                                bundle.putInt("orderID", it.data.order.id)
                                bundle.putParcelable("receiptData", it.data)
                                bundle.putInt("splitValue", splitValue)
                                if (splitValue != -1) {
                                    if (WholetotalPrice <= cardPaymentAmount) {
                                        bundle.putBoolean("isSpilt", false)
                                    } else {
                                        bundle.putBoolean("isSpilt", true)
                                    }
                                } else {
                                    bundle.putBoolean("isSpilt", false)
                                }
                                bundle.putBoolean("isSplitByNo", isSplitByNo)
                                bundle.putBoolean("isSplitByAmount", isSplitByAmount)
                                bundle.putString("paymentType", "Card")
                                bundle.putBoolean("isDineIn", false)
                                findNavController().navigate(
                                    R.id.action_paymentFragment_to_orderCompleteFragment,
                                    bundle
                                )
                                prefProvider.setValue(SPLIT_PAY_TYPE, SPLIT_NO)
                                prefProvider.setValue(
                                    SPLIT_PAY_AMOUNT,
                                    (splitAfterAmount - tipAmount).toString()
                                )
                                prefProvider.setValueInt(SPLIT_NO, splitValue)

                            }
                            else -> {
                                val bundle = Bundle()
                                bundle.putBoolean("isDineIn", false)
                                bundle.putDouble("PaidAmount", cardPaymentAmount)
                                bundle.putDouble("WholetotalPrice", WholetotalPrice)
                                bundle.putDouble(
                                    "remainingAmount",
                                    0.0
                                )
                                bundle.putDouble("dis_charge_value", 0.0)
                                bundle.putInt("orderID", it.data.order.id)
                                bundle.putParcelable("receiptData", it.data)
                                bundle.putInt("splitValue", -1)
                                bundle.putBoolean("isSpilt", false)
                                bundle.putBoolean("isSplitByNo", isSplitByNo)
                                bundle.putBoolean("isSplitByAmount", isSplitByAmount)
                                bundle.putString("paymentType", "Card")
                                findNavController().navigate(
                                    R.id.action_paymentFragment_to_orderCompleteFragment,
                                    bundle
                                )
                                prefProvider.setValueInt("ORDER_ID", -1)
                            }
                        }

                    }
                    paymentType == "Cash" -> {
                        when {
                            isSplitByNo -> {
                                val bundle = Bundle()
                                bundle.putDouble("PaidAmount", splitAfterAmount)
                                var wholetotalPriceTemp = String.format(
                                    "%.2f",
                                    prefProvider.getValue("WholeTotal", "").toDouble()
                                )
                                bundle.putDouble("WholetotalPrice", wholetotalPriceTemp.toDouble())
                                var remainingAmount = 0.0
                                remainingAmount = String.format(
                                    "%.2f",
                                    wholetotalPriceTemp.toDouble() - splitAfterAmount
                                ).toDouble()
                                prefProvider.setValue(
                                    "WholeTotal",
                                    String.format("%.2f", remainingAmount)
                                )
                                bundle.putDouble(
                                    "remainingAmount",
                                    remainingAmount
                                )
                                bundle.putInt("orderID", it.data.order.id)
                                bundle.putParcelable("receiptData", it.data)
                                bundle.putInt("splitValue", splitValue)


                                if (splitValue != -1) {
                                    if (remainingAmount <= 0.0) {
                                        bundle.putBoolean("isSpilt", false)
                                        prefProvider.setValue(SUB_TOTAL, "")
                                        prefProvider.setValue(TOTAL_DISCOUNT, "")
                                        prefProvider.setValue(TIP, "")
                                        prefProvider.setValue(TAX_CHARGE, "")
                                        prefProvider.setValue(SERVICE_CHARGE, "")
                                    } else {
                                        bundle.putBoolean("isSpilt", true)
                                        setPaymentAttriButes(SUB_TOTAL, splitValue)
                                        setPaymentAttriButes(SERVICE_CHARGE, splitValue)
                                        setPaymentAttriButes(TAX_CHARGE, splitValue)
                                        setPaymentAttriButes(TIP, splitValue)
                                        setPaymentAttriButes(TOTAL_DISCOUNT, splitValue)
                                    }
                                } else {
                                    bundle.putBoolean("isSpilt", false)
                                    prefProvider.setValue(SUB_TOTAL, "")
                                    prefProvider.setValue(TOTAL_DISCOUNT, "")
                                    prefProvider.setValue(TIP, "")
                                    prefProvider.setValue(TAX_CHARGE, "")
                                    prefProvider.setValue(SERVICE_CHARGE, "")
                                }
                                bundle.putBoolean("isSplitByNo", isSplitByNo)
                                bundle.putBoolean("isSplitByAmount", isSplitByAmount)
                                bundle.putString("paymentType", "Cash")
                                bundle.putBoolean("isDineIn", false)
                                findNavController().navigate(
                                    R.id.action_paymentFragment_to_orderCompleteFragment,
                                    bundle
                                )
                                prefProvider.setValue(SPLIT_PAY_TYPE, SPLIT_NO)
                                prefProvider.setValue(
                                    SPLIT_PAY_AMOUNT,
                                    (splitAfterAmount - tipAmount).toString()
                                )
                                prefProvider.setValueInt(SPLIT_NO, splitValue)

                            }
                            isSplitByAmount -> {
                                val bundle = Bundle()
                                bundle.putDouble("PaidAmount", splitAfterAmount)
                                var wholetotalPriceTemp = String.format(
                                    "%.2f",
                                    prefProvider.getValue("WholeTotal", "").toDouble()
                                )

                                bundle.putDouble("WholetotalPrice", wholetotalPriceTemp.toDouble())
                                var remainingAmount = 0.0
                                remainingAmount = String.format(
                                    "%.2f",
                                    wholetotalPriceTemp.toDouble() - splitAfterAmount
                                ).toDouble()
                                prefProvider.setValue(
                                    "WholeTotal",
                                    String.format("%.2f", remainingAmount)
                                )
                                bundle.putDouble(
                                    "remainingAmount",
                                    remainingAmount
                                )
                                bundle.putInt("orderID", it.data.order.id)
                                bundle.putParcelable("receiptData", it.data)
                                bundle.putInt("splitValue", splitValue)
                                if (remainingAmount == 0.0) {
                                    bundle.putBoolean("isSpilt", false)
                                } else {
                                    bundle.putBoolean("isSpilt", true)
                                }
                                bundle.putBoolean("isSplitByNo", isSplitByNo)
                                bundle.putBoolean("isSplitByAmount", isSplitByAmount)
                                bundle.putString("paymentType", "Cash")
                                bundle.putBoolean("isDineIn", false)
                                findNavController().navigate(
                                    R.id.action_paymentFragment_to_orderCompleteFragment,
                                    bundle
                                )
                                prefProvider.setValue(SPLIT_PAY_TYPE, SPLIT_NO)
                                prefProvider.setValue(
                                    SPLIT_PAY_AMOUNT,
                                    (splitAfterAmount - tipAmount).toString()
                                )
                                prefProvider.setValueInt(SPLIT_NO, splitValue)

                            }
                            isCustomCash -> {
                                val bundle = Bundle()
                                bundle.putBoolean("isDineIn", false)
                                if (remainingAmount == 0.0) {
                                    bundle.putDouble("PaidAmount", paymentAmount)
                                } else {
                                    bundle.putDouble("PaidAmount", remainingAmount)
                                }


                                bundle.putDouble("WholetotalPrice", totalPrice + tipAmount)
                                var remaining_custom = paymentAmount - totalPrice + tipAmount
                                bundle.putDouble(
                                    "remainingAmount",
                                    remaining_custom
                                )
                                bundle.putInt("orderID", it.data.order.id)
                                bundle.putParcelable("receiptData", it.data)
                                bundle.putInt("splitValue", -1)
                                bundle.putBoolean("isSpilt", false)
                                bundle.putBoolean("isSplitByNo", isSplitByNo)
                                bundle.putBoolean("isSplitByAmount", isSplitByAmount)
                                bundle.putBoolean("isCustomCash", isCustomCash)
                                bundle.putString("paymentType", "Cash")
                                findNavController().navigate(
                                    R.id.action_paymentFragment_to_orderCompleteFragment,
                                    bundle
                                )

                                prefProvider.setValueInt("ORDER_ID", -1)
                            }
                            else -> {
                                val bundle = Bundle()
                                bundle.putBoolean("isDineIn", false)

                                if (remainingAmount == 0.0) {
                                    bundle.putDouble("PaidAmount", totalPrice + tipAmount)
                                } else {
                                    bundle.putDouble("PaidAmount", remainingAmount)
                                }


                                bundle.putDouble("WholetotalPrice", WholetotalPrice)
                                bundle.putDouble(
                                    "remainingAmount",
                                    0.0
                                )
                                bundle.putInt("orderID", it.data.order.id)
                                bundle.putParcelable("receiptData", it.data)
                                bundle.putInt("splitValue", -1)
                                bundle.putBoolean("isSpilt", false)
                                bundle.putBoolean("isSplitByNo", isSplitByNo)
                                bundle.putBoolean("isSplitByAmount", isSplitByAmount)
                                bundle.putString("paymentType", "Cash")
                                findNavController().navigate(
                                    R.id.action_paymentFragment_to_orderCompleteFragment,
                                    bundle
                                )

                                prefProvider.setValueInt("ORDER_ID", -1)
                            }
                        }
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