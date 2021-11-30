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
import com.android.pos.data.remote.Constants.EMPLOYEE_ID
import com.android.pos.data.remote.Constants.LOCATION_ID
import com.android.pos.data.remote.Constants.TERMINAL_ID
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

    private val viewModel by viewModels<DineInOrderTableViewModel>()
    private var orderOfflineId: String = ""
    private var paymentOfflineId: String = ""
    private var orderId: Int? = null
    private var paymentId: Int? = null
    private var isUpdate: Boolean = false
    private var tipAmount: Double = 0.0
    private var future_delivery_date: String = ""
    private var future_delivery_time: String = ""
    private var fourthValue: Double = 0.0
    private var thirdValue: Double = 0.0
    private var secondValue: Int = 0
    private var cartList: CartModel? = null
    private var splitValue: Int = -1
    private var totalPrice: Double = 0.0
    private var totaldiscount: Double = 0.0
    private val paymentViewModel by viewModels<PaymentViewModel>()
    private var subTotalPrice: Double = 0.0
    var paymentType = "Cash"
    private var totalTax: Double = 0.0
    var cashDiscount: Double = 0.0
    var cardPaymentAmount = 0.0
    private var totalServiceCharge: Double = 0.0
    private var isTotalPayment: Boolean = false
    private var paymentAmount: Double = 0.0

    private var guestId: Int? = null
    private var guestRequestModel: GuestPaymentRequest? = null
    private var splitModel: DineInOrderPayment? = null
    private var floorPlanModel: GetFloorPlanResponse.Data.FloorPlanTable? = null
    private var isLastPayment: Boolean? = false

    private var isSplitByNo: Boolean = false
    private var isSplitByAmount: Boolean = false

    private var splitAfterAmount: Double = 0.0
    var optionType = ""
    var amountType = ""
    var rateorAmount = ""
    var cashDiscountType = "CashDiscount"
    private val TAG = "PayByGuestDialog"

    @Inject
    lateinit var prefProvider: PrefProvider


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
        optionType = prefProvider.getValue(Constants.OPTION_TYPE, "CashDiscount")
        cashDiscountType = optionType
        amountType = prefProvider.getValue(Constants.AMOUNT_TYPE, "Dollar")
        rateorAmount = prefProvider.getValue(Constants.RATE_OR_AMOUNT, "0")
        if (isTotalPayment) {
            isTotalPayment = true
            orderId = requireArguments().getInt("orderId")
            setTotalPaymentData()
            binding.txtCustom.setOnClickListener(this)
            binding.imgBack.setOnClickListener(this)
            binding.llCash.setOnClickListener(this)
            binding.llCredit.setOnClickListener(this)
            binding.txtOriginalAmount.setOnClickListener(this)
            binding.txtSecondAmount.setOnClickListener(this)
            binding.txtThirdAmount.setOnClickListener(this)
            binding.txtFourthAmount.setOnClickListener(this)
            binding.txtAddTips.setOnClickListener(this)
        } else {
            floorPlanModel = requireArguments().getParcelable("floorPlan")
            orderId = requireArguments().getInt("orderId")

            cartList = requireArguments().getParcelable("cartList")
            guestId = requireArguments().getInt("id")
            guestRequestModel = requireArguments().getParcelable("model")
            isLastPayment = requireArguments().getBoolean("isLastPayment")
            isGuestPay = requireArguments().getBoolean("isGuestPay")
            Log.e(TAG, "isLastPayment  ${isLastPayment}")
            splitModel = requireArguments().getParcelable("orderPayment")
            setupData()
        }

        setFragmentResultListener("request_key_split") { requestKey: String, bundle: Bundle ->
            splitValue = bundle.getInt("split")

            if (splitValue != -1) {

                isSplitByNo = true
                isSplitByAmount = false
                splitAfterAmount = (totalPrice + tipAmount) / splitValue

                MethodUtils.setPriceTextView(binding.txtTotalAmount, splitAfterAmount)
                val totalAmountFormat = MethodUtils.roundOffAmount(totalPrice)

                binding.txtSplitValue.text =
                    "Out of $totalAmountFormat Total, Payment 1 of $splitValue"

                getCashPaymentOptionList(splitAfterAmount)
            } else {
                isSplitByAmount = true
                isSplitByNo = false
                val splitValue = bundle.getDouble("splitByAmount")

                splitAfterAmount = splitValue

                MethodUtils.setPriceTextView(binding.txtTotalAmount, splitAfterAmount)
                val totalAmountFormat = MethodUtils.roundOffAmount(totalPrice)

                binding.txtSplitValue.text =
                    "Out of $totalAmountFormat Total, Payment 1 of $splitValue"

                getCashPaymentOptionList(splitAfterAmount)
            }
        }

        binding.txtSplitAmount.setOnClickListener(this)

        return binding.root
    }

    fun getDiscountCashData(): Double {
        if (amountType == "Dollar") {
            return rateorAmount.toDouble().also { cashDiscount = it }
        } else if (amountType == "Percentage") {
            return (viewModel.subTotalAmount * 100 / rateorAmount.toDouble()).also {
                cashDiscount = it
            }
        }
        return 0.0
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
                val bundle = Bundle()
                bundle.putDouble("totalPrice", cardPaymentAmount)
                bundle.putDouble("paymentAmount", cardPaymentAmount)
                orderId?.let { bundle.putInt("orderID", it) }
                //bundle.putParcelable("receiptData", it.data)
                bundle.putBoolean("isSpilt", false)
                bundle.putBoolean("isDineIn", true)
                bundle.putBoolean("isGuest", isGuestPay)
                bundle.putString("paymentType", "Card")
                findNavController().navigate(
                    R.id.action_payByGuestDialog_to_orderCompleteFragment,
                    bundle
                )
                prefProvider.setValue(Constants.SPLIT_PAY_AMOUNT_DINE_IN, "")
                prefProvider.setValueInt(Constants.SPLIT_NO_DINE_IN, -1)
                prefProvider.setValue(Constants.SPLIT_PAY_TYPE_DINE_IN, "")
                prefProvider.setValueInt("ORDER_ID", -1)
            }
            isSplitByNo -> {

                var payAmount = (totalPrice + tipAmount) / splitValue
                val bundle = Bundle()
                bundle.putDouble("totalPrice", payAmount)
                bundle.putDouble("paymentAmount", paymentAmount)
                orderId?.let { bundle.putInt("orderID", it) }
                // bundle.putParcelable("receiptData", it.data)
                bundle.putBoolean("isSpilt", true)
                bundle.putInt("splitValue", splitValue)
                bundle.putDouble("remainingAmount", totalPrice - payAmount)
                bundle.putBoolean("isDineIn", true)
                bundle.putBoolean("isGuest", isGuestPay)
                bundle.putString("paymentType", "Cash")
                if (isLastPayment!!)
                    bundle.putBoolean("isGuest", false)
                findNavController().navigate(
                    R.id.action_payByGuestDialog_to_orderCompleteFragment,
                    bundle
                )

                val splitPayAmount =
                    prefProvider.getValue(Constants.SPLIT_PAY_AMOUNT_DINE_IN, "")
                if (splitPayAmount.isNotEmpty()) {
                    payAmount += splitPayAmount.toDouble()
                }


                prefProvider.setValue(
                    Constants.SPLIT_PAY_TYPE_DINE_IN,
                    Constants.SPLIT_NO_DINE_IN
                )
                prefProvider.setValue(
                    Constants.SPLIT_PAY_AMOUNT_DINE_IN,
                    payAmount.toString()
                )
                prefProvider.setValueInt(Constants.SPLIT_NO_DINE_IN, splitValue)

            }
            isSplitByAmount -> {

                var payAmount = splitAfterAmount
                val bundle = Bundle()
                bundle.putDouble("totalPrice", payAmount)
                bundle.putDouble("paymentAmount", paymentAmount)
                bundle.putString("paymentType", "Cash")
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
                    bundle.putDouble("remainingAmount", totalPrice - payAmount)

                    prefProvider.setValue(Constants.SPLIT_PAY_AMOUNT_DINE_IN, "")
                    prefProvider.setValueInt(Constants.SPLIT_NO_DINE_IN, -1)
                    prefProvider.setValue(Constants.SPLIT_PAY_TYPE_DINE_IN, "")
                }

                bundle.putBoolean("isDineIn", true)
                findNavController().navigate(
                    R.id.action_payByGuestDialog_to_orderCompleteFragment,
                    bundle
                )

            }
            else -> {
                val bundle = Bundle()
                bundle.putDouble("totalPrice", totalPrice + tipAmount)
                bundle.putDouble("paymentAmount", paymentAmount)
                orderId?.let { bundle.putInt("orderID", it) }
                //bundle.putParcelable("receiptData", it.data)
                bundle.putBoolean("isSpilt", false)
                bundle.putBoolean("isDineIn", true)
                bundle.putBoolean("isGuest", isGuestPay)
                bundle.putString("paymentType", "Cash")
                if (isLastPayment!!)
                    bundle.putBoolean("isGuest", false)


                findNavController().navigate(
                    R.id.action_payByGuestDialog_to_orderCompleteFragment,
                    bundle
                )

                prefProvider.setValue(Constants.SPLIT_PAY_AMOUNT_DINE_IN, "")
                prefProvider.setValueInt(Constants.SPLIT_NO_DINE_IN, -1)
                prefProvider.setValue(Constants.SPLIT_PAY_TYPE_DINE_IN, "")
                prefProvider.setValueInt("ORDER_ID", -1)


            }
        }

//        } else {
//            val bundle = bundleOf("orderId" to orderId, "isGuestPaid" to true)
//
//            findNavController().navigate(
//                R.id.action_payByGuestDialog_to_dineInOrderTable,
//                bundle
//            )
//        }
    }

    private fun setTotalPaymentData() {

        totalPrice = requireArguments().getDouble("totalPrice")
        subTotalPrice = requireArguments().getDouble("subTotalPrice")
        totalTax = requireArguments().getDouble("totalTax")
        totalServiceCharge = requireArguments().getDouble("totalServiceCharge")
        totaldiscount = requireArguments().getDouble("totalDiscount")
        future_delivery_time = requireArguments().getString("future_delivery_time").toString()
        future_delivery_date = requireArguments().getString("future_delivery_date").toString()
        getCashPaymentOptionList(totalPrice)
        MethodUtils.setPriceTextView(binding.txtSubTotal, subTotalPrice)
        MethodUtils.setPriceTextView(binding.txtTax, totalTax)
        MethodUtils.setPriceTextView(binding.txtTotal, totalPrice)
        MethodUtils.setPriceTextView(binding.txtTipAmt, tipAmount)
        //MethodUtils.setPriceTextView(binding.txtDiscount, totalDiscount)
        MethodUtils.setPriceTextView(binding.txtServiceCharge, totalServiceCharge)
        setUpPaymentTypeWiseDiscount()

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

        val splitPayType = prefProvider.getValue(Constants.SPLIT_PAY_TYPE_DINE_IN, "")
        val splitPayAmount = prefProvider.getValue(Constants.SPLIT_PAY_AMOUNT_DINE_IN, "")

        if (splitPayType == Constants.SPLIT_NO_DINE_IN) {

            if (splitPayAmount.isNotEmpty()) {
                totalPrice -= splitPayAmount.toDouble()

                val splitNo = prefProvider.getValueInt(Constants.SPLIT_NO_DINE_IN, -1)
                if (splitNo != -1) {

                    val split_subTotalPrice = subTotalPrice / splitNo
                    subTotalPrice -= split_subTotalPrice

                    val split_totalTax = totalTax / splitNo
                    totalTax -= split_totalTax

                    val split_totalServiceCharge = totalServiceCharge / splitNo
                    totalServiceCharge -= split_totalServiceCharge

                    val split_totalDiscount = totaldiscount / splitNo
                    totaldiscount -= split_totalDiscount

                    val split_tip_amount = tipAmount / splitNo
                    tipAmount -= split_tip_amount

                    totalPrice += tipAmount
                }

            }
        } else if (splitPayType == Constants.SPLIT_PAY_AMOUNT_DINE_IN) {


            if (splitPayAmount.isNotEmpty()) {


                val tipAmount1 = (splitPayAmount.toDouble() * tipAmount) / (totalPrice + tipAmount)

                val total = (totalPrice + tipAmount1)

                val subTotalPrice1 = (splitPayAmount.toDouble() * subTotalPrice) / total
                val totalServiceCharge1 =
                    (splitPayAmount.toDouble() * totalServiceCharge) / total
                val totalTax1 = (splitPayAmount.toDouble() * totalTax) / total
                val totalDiscount1 = (splitPayAmount.toDouble() * totaldiscount) / total


                totalPrice -= splitPayAmount.toDouble()
                subTotalPrice -= subTotalPrice1
                totalTax -= totalTax1
                totalServiceCharge -= totalServiceCharge1
                totaldiscount -= totalDiscount1
                tipAmount -= tipAmount1


            }
        }

        getCashPaymentOptionList(totalPrice + tipAmount)


    }

    private fun setUpPaymentTypeWiseDiscount() {
        if (paymentType == "Card") {
            if (optionType == "SurCharge") {

                binding.linnearCashDiscounnt.visibility = View.GONE
                binding.linearNonCashAdjamounnt.visibility = View.VISIBLE
                binding.txtCashdiscount.text = "- $" + String.format("%.2f", 0.0)
                //100 -4 = 96 == totalprice
                MethodUtils.setPriceTextView(
                    binding.txtTotal,
                    (totalPrice + tipAmount + getDiscountCashData()) + getDiscountCashData()
                )
                MethodUtils.setPriceTextView(
                    binding.txtTotalAmount,
                    (totalPrice + tipAmount + getDiscountCashData()) + getDiscountCashData()
                )
                cardPaymentAmount =
                    (totalPrice + tipAmount + getDiscountCashData()) + getDiscountCashData()
                binding.txtNoncashAdj.text = "+ $" + String.format("%.2f", getDiscountCashData())
            } else {
                binding.linnearCashDiscounnt.visibility = View.GONE
                binding.linearNonCashAdjamounnt.visibility = View.GONE

                binding.txtCashdiscount.text = "- $" + String.format("%.2f", getDiscountCashData())
                MethodUtils.setPriceTextView(
                    binding.txtTotal,
                    (totalPrice + tipAmount) + getDiscountCashData()
                )
                MethodUtils.setPriceTextView(
                    binding.txtTotalAmount,
                    (totalPrice + tipAmount) + getDiscountCashData()
                )
                cardPaymentAmount = (totalPrice + tipAmount) + getDiscountCashData()
                binding.txtNoncashAdj.text = "- $" + String.format("%.2f", 0.0)
            }
        } else if (paymentType == "Cash") {
            if (optionType == "SurCharge") {
                binding.linnearCashDiscounnt.visibility = View.GONE
                binding.linearNonCashAdjamounnt.visibility = View.GONE
                binding.txtCashdiscount.text = "- $" + String.format("%.2f", 0.0)
                MethodUtils.setPriceTextView(
                    binding.txtTotal,
                    (totalPrice + tipAmount) + getDiscountCashData()
                )
                MethodUtils.setPriceTextView(
                    binding.txtTotalAmount,
                    (totalPrice + tipAmount) + getDiscountCashData()
                )
                binding.txtNoncashAdj.text = "- $" + String.format("%.2f", getDiscountCashData())
            } else {
                binding.linnearCashDiscounnt.visibility = View.VISIBLE
                binding.linearNonCashAdjamounnt.visibility = View.GONE
                binding.txtCashdiscount.text = "- $" + String.format("%.2f", getDiscountCashData())
                MethodUtils.setPriceTextView(
                    binding.txtTotal,
                    (totalPrice + tipAmount)
                )
                MethodUtils.setPriceTextView(
                    binding.txtTotalAmount,
                    (totalPrice + tipAmount)
                )
                binding.txtNoncashAdj.text = "- $" + String.format("%.2f", 0.0)
            }
        }
    }

    private fun setupData() {

        requireArguments().getDouble("totalPrice")?.let {
            totalPrice = it
        }
        requireArguments().getDouble("subTotalPrice")?.let {
            subTotalPrice = it
        }
        requireArguments().getDouble("totalTax")?.let {
            totalTax = it
        }
        totalServiceCharge = requireArguments().getDouble("totalServiceCharge")
        totaldiscount = requireArguments().getDouble("totalDiscount")
        future_delivery_time = requireArguments().getString("future_delivery_time").toString()
        future_delivery_date = requireArguments().getString("future_delivery_date").toString()

        MethodUtils.setPriceTextView(binding.txtSubTotal, subTotalPrice)
        MethodUtils.setPriceTextView(binding.txtTax, totalTax)
        //MethodUtils.setPriceTextView(binding.txtDiscount, totalDiscount)
        MethodUtils.setPriceTextView(binding.txtTipAmt, tipAmount)
        MethodUtils.setPriceTextView(binding.txtTotal, totalPrice + tipAmount)

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


        isUpdate = requireArguments().getBoolean("update")
        if (isUpdate) {

            orderId = requireArguments().getInt("orderId")
            paymentId = requireArguments().getInt("paymentId")
            paymentOfflineId = requireArguments().getString("paymentOfflineId").toString()
            orderOfflineId = requireArguments().getString("orderOfflineId").toString()
        }

        val splitPayType = prefProvider.getValue(Constants.SPLIT_PAY_TYPE_DINE_IN, "")
        val splitPayAmount = prefProvider.getValue(Constants.SPLIT_PAY_AMOUNT_DINE_IN, "")

        if (splitPayType == Constants.SPLIT_NO_DINE_IN) {

            if (splitPayAmount.isNotEmpty()) {
                totalPrice -= splitPayAmount.toDouble()

                val splitNo = prefProvider.getValueInt(Constants.SPLIT_NO_DINE_IN, -1)
                if (splitNo != -1) {

                    val split_subTotalPrice = subTotalPrice / splitNo
                    subTotalPrice -= split_subTotalPrice

                    val split_totalTax = totalTax / splitNo
                    totalTax -= split_totalTax

                    val split_totalServiceCharge = totalServiceCharge / splitNo
                    totalServiceCharge -= split_totalServiceCharge

                    val split_totalDiscount = totaldiscount / splitNo
                    totaldiscount -= split_totalDiscount

                    val split_tip_amount = tipAmount / splitNo
                    tipAmount -= split_tip_amount

                    totalPrice += tipAmount

                }

            }
        } else if (splitPayType == Constants.SPLIT_PAY_AMOUNT_DINE_IN) {


            if (splitPayAmount.isNotEmpty()) {


                val tipAmount1 = (splitPayAmount.toDouble() * tipAmount) / (totalPrice + tipAmount)

                val total = (totalPrice + tipAmount1)

                val subTotalPrice1 = (splitPayAmount.toDouble() * subTotalPrice) / total
                val totalServiceCharge1 =
                    (splitPayAmount.toDouble() * totalServiceCharge) / total
                val totalTax1 = (splitPayAmount.toDouble() * totalTax) / total
                val totalDiscount1 = (splitPayAmount.toDouble() * totaldiscount) / total


                totalPrice -= splitPayAmount.toDouble()
                subTotalPrice -= subTotalPrice1
                totalTax -= totalTax1
                totalServiceCharge -= totalServiceCharge1
                totaldiscount -= totalDiscount1
                tipAmount -= tipAmount1


            }
        }

        getCashPaymentOptionList(totalPrice + tipAmount)


        // binding.txtSplitAmount.setOnClickListener(this)
        binding.txtCustom.setOnClickListener(this)
        binding.imgBack.setOnClickListener(this)
        binding.llCash.setOnClickListener(this)
        binding.llCredit.setOnClickListener(this)

        binding.txtOriginalAmount.setOnClickListener(this)
        binding.txtSecondAmount.setOnClickListener(this)
        binding.txtThirdAmount.setOnClickListener(this)
        binding.txtFourthAmount.setOnClickListener(this)
        binding.txtAddTips.setOnClickListener(this)

        if (cartList?.orderType == Constants.DINE_IN) {

        }


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
                val handler = Handler()
                handler.postDelayed({
                    makePaymentCreditCard()
                }, 2000)


            }

            R.id.llCash -> {
                if (paymentType == "Card") {
                    paymentType = "Cash"
                    setUpPaymentTypeWiseDiscount()
                } else {
                    paymentType = "Cash"

                    paymentAmount = when {

                        isSplitByNo -> {
                            (totalPrice + tipAmount) / splitValue
                        }
                        isSplitByAmount -> {
                            splitAfterAmount
                        }
                        else -> {
                            (totalPrice + tipAmount)
                        }
                    }


                    if (isTotalPayment) {
                        makePayment(0.0)

                    } else {

                        guestPaySpit()

                        guestRequestModel?.let {
                            guestId?.let { it1 ->
                                isLastPayment?.let { it2 ->
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

            R.id.txtOriginalAmount -> {
                paymentAmount = when {

                    isSplitByNo -> {
                        (totalPrice + tipAmount) / splitValue
                    }
                    isSplitByAmount -> {
                        splitAfterAmount
                    }
                    else -> {
                        (totalPrice + tipAmount)
                    }
                }
                if (isTotalPayment) {

                    makePayment(0.0)

                } else {
                    guestPaySpit()

                    guestRequestModel?.let {
                        guestId?.let { it1 ->
                            isLastPayment?.let { it2 ->
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

                    makePayment(0.0)

                } else {
                    guestPaySpit()
                    guestRequestModel?.let {
                        guestId?.let { it1 ->
                            isLastPayment?.let { it2 ->
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

                    makePayment(0.0)

                } else {
                    guestPaySpit()
                    guestRequestModel?.let {
                        guestId?.let { it1 ->
                            isLastPayment?.let { it2 ->
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

                    makePayment(0.0)

                } else {
                    guestPaySpit()
                    guestRequestModel?.let {
                        guestId?.let { it1 ->
                            isLastPayment?.let { it2 ->
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
            subTotal = MethodUtils.roundOffAmountDouble(subTotalPrice / splitValue)
            totalAmount = cardPaymentAmount
            totalDiscount = totaldiscount / splitValue
            totalServiceCharges = totalServiceCharge / splitValue
            totalTaxAmount = totalTax / splitValue
            totalTips = tipAmount / splitValue
            cash_discount_or_surcharge = getDiscountCashData()
            cash_discount_type = cashDiscountType

        }

        return OrderRequestModel(
            completed_all_payments = false,
            order = orderModel
        )
    }

    private fun makePaymentCreditCard() {
        val requestModel = createRequestForCreditCardTotalAmount()
        requestModel.order.paymentAttributes = paymentAttributes()
        paymentViewModel.totalPayAmount(cardPaymentAmount)
        orderId?.let { paymentViewModel.dineInWholePayment(requestModel, it, splitValue) }

    }

    private fun guestPaySpit() {
        if (splitValue != -1) {
            guestRequestModel?.paymentAttributes!!.amount =
                totalPrice / splitValue
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

            val guestPaymentAttributes = GuestPaymentAttributes()
            guestPaymentAttributes.amount =
                totalPrice / splitValue
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
            guestPaymentAttributes.payableType = guestRequestModel?.paymentAttributes!!.payableType
            guestPaymentAttributes.paymentType = guestRequestModel?.paymentAttributes!!.paymentType
            guestPaymentAttributes.offlineId = guestRequestModel?.paymentAttributes!!.offlineId
            guestPaymentAttributes.order_id = guestRequestModel?.paymentAttributes!!.order_id

            guestRequestModel?.paymentAttributes!!.paymentAttributes =
                listOf(guestPaymentAttributes)
        }
    }

    private fun makePayment(amounta: Double) {
        Log.e(TAG, "amountForPayment   ${amounta}")

        val orderId1 = prefProvider.getValueInt("ORDER_ID", -1)
        if (orderId1 == -1) {

            val requestModel = createRequestForTotalAmount()
            requestModel.order.paymentAttributes = paymentAttributes()

            orderId?.let { paymentViewModel.dineInWholePayment(requestModel, it, splitValue) }
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

    private fun paymentAttributes(): PaymentAttributes {
        if (paymentType == "Card") {
            val paymentReq = PaymentAttributes().apply {
                amount = cardPaymentAmount
                employeeId = prefProvider.getValueInt(EMPLOYEE_ID, 0)
                payableType = "Order"
                paymentType = "Card"
                serviceChargeAmount =
                    if (splitValue == -1) MethodUtils.roundOffAmountDouble(totalServiceCharge) else MethodUtils.roundOffAmountDouble(
                        totalServiceCharge / splitValue
                    )
                subTotal =
                    if (splitValue == -1) MethodUtils.roundOffAmountDouble(subTotalPrice) else MethodUtils.roundOffAmountDouble(
                        subTotalPrice / splitValue
                    )
                taxAmount =
                    if (splitValue == -1) MethodUtils.roundOffAmountDouble(totalTax) else MethodUtils.roundOffAmountDouble(
                        totalTax / splitValue
                    )
                terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)
                tips =
                    if (splitValue == -1) MethodUtils.roundOffAmountDouble(tipAmount) else MethodUtils.roundOffAmountDouble(
                        tipAmount / splitValue
                    )
                tipsAdjusted = false
                totalDiscount =
                    if (splitValue == -1) MethodUtils.roundOffAmountDouble(totalDiscount) else MethodUtils.roundOffAmountDouble(
                        totalDiscount / splitValue
                    )
                order_id = orderId
                cash_discount_or_surcharge = getDiscountCashData()
                cash_discount_type = cashDiscountType
                cashDiscount = getDiscountCashData()
            }
            return paymentReq
        } else {
            val paymentReq = PaymentAttributes().apply {
                amount = if (splitValue == -1) totalPrice else totalPrice / splitValue
                employeeId = prefProvider.getValueInt(EMPLOYEE_ID, 0)
                payableType = "Order"
                paymentType = "Cash"
                serviceChargeAmount =
                    if (splitValue == -1) MethodUtils.roundOffAmountDouble(totalServiceCharge) else MethodUtils.roundOffAmountDouble(
                        totalServiceCharge / splitValue
                    )
                subTotal =
                    if (splitValue == -1) MethodUtils.roundOffAmountDouble(subTotalPrice) else MethodUtils.roundOffAmountDouble(
                        subTotalPrice / splitValue
                    )
                taxAmount =
                    if (splitValue == -1) MethodUtils.roundOffAmountDouble(totalTax) else MethodUtils.roundOffAmountDouble(
                        totalTax / splitValue
                    )
                terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)
                tips =
                    if (splitValue == -1) MethodUtils.roundOffAmountDouble(tipAmount) else MethodUtils.roundOffAmountDouble(
                        tipAmount / splitValue
                    )
                tipsAdjusted = false
                totalDiscount =
                    if (splitValue == -1) MethodUtils.roundOffAmountDouble(totalDiscount) else MethodUtils.roundOffAmountDouble(
                        totalDiscount / splitValue
                    )
                order_id = orderId
                cash_discount_type = cashDiscountType
                cash_discount_or_surcharge = getDiscountCashData()
                cashDiscount = getDiscountCashData()
            }
            return paymentReq
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

        if (isSplitByNo) {


            orderModel.apply {
                date = TimeFormatUtils.getCurrentDate()
                employeeId = prefProvider.getValueInt(EMPLOYEE_ID, 0)
                locationId = prefProvider.getValueInt(LOCATION_ID, 1)
                terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)
                note = ""
                openOrderType = "DineIn"
                orderTypeId = 2
                paymentStatus = 1
                subTotal = MethodUtils.roundOffAmountDouble(subTotalPrice / splitValue)
                totalAmount =
                    MethodUtils.roundOffAmountDouble(totalPrice / splitValue) - MethodUtils.roundOffAmountDouble(
                        tipAmount / splitValue
                    )
                totalDiscount = totaldiscount / splitValue
                totalServiceCharges = totalServiceCharge / splitValue
                totalTaxAmount = totalTax / splitValue
                totalTips = tipAmount / splitValue
                cash_discount_type = cashDiscountType
                cash_discount_or_surcharge = getDiscountCashData()

            }

            return OrderRequestModel(
                completed_all_payments = false,
                order = orderModel
            )
        } else {

            orderModel.apply {
                date = TimeFormatUtils.getCurrentDate()

                employeeId = prefProvider.getValueInt(EMPLOYEE_ID, 0)
                locationId = prefProvider.getValueInt(LOCATION_ID, 1)
                terminalId = prefProvider.getValueInt(TERMINAL_ID, 0)
                note = ""
                openOrderType = "DineIn"
                orderTypeId = 2
                paymentStatus = 1
                subTotal = MethodUtils.roundOffAmountDouble(subTotalPrice)
                totalAmount =
                    MethodUtils.roundOffAmountDouble(totalPrice) - MethodUtils.roundOffAmountDouble(
                        tipAmount
                    )
                totalDiscount = totaldiscount
                totalServiceCharges = totalServiceCharge
                totalTaxAmount = totalTax
                totalTips = tipAmount
                cash_discount_type = cashDiscountType
                cash_discount_or_surcharge = getDiscountCashData()


            }

            return OrderRequestModel(
                completed_all_payments = true,
                order = orderModel
            )
        }


    }
}
