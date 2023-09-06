package com.android.pos.ui.fragments.payment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.MainApplication
import com.android.pos.R
import com.android.pos.data.entities.CartModel
import com.android.pos.data.entities.CashDiscountModel
import com.android.pos.data.entities.RedeemLoyaltyInfo
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.SplitBundleModel
import com.android.pos.data.model.requestModel.*
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
import com.android.pos.data.remote.Constants.SUB_TOTAL_ACTUAL
import com.android.pos.data.remote.Constants.TAX_CHARGE
import com.android.pos.data.remote.Constants.TAX_CHARGE_ACTUAL
import com.android.pos.data.remote.Constants.TERMINAL_ID
import com.android.pos.data.remote.Constants.TIP
import com.android.pos.data.remote.Constants.TIPS_AMOUNT_ACTUAL
import com.android.pos.data.remote.Constants.TOTAL_DISCOUNT
import com.android.pos.data.remote.Constants.TOTAL_DISCOUNT_ACTUAL
import com.android.pos.data.remote.Constants.TOTAL_PRICE_ACTUAL
import com.android.pos.data.remote.Constants.TOTAL_SERVICE_CHARGE_ACTUAL
import com.android.pos.databinding.PaymentFragmentBinding
import com.android.pos.di.PrefProvider
import com.android.pos.utils.*
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.visible
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.floor

@AndroidEntryPoint
open class PaymentFragment : Fragment(), View.OnClickListener {
    private var cartItems: List<TbItem>? = null
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
    private var tipID: Int? = null
    private var noCashAdj: Double = 0.0
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
    var isFromDashboard: Boolean = false
    var isFromActiveOrder: Boolean = false
    var manualSaleCart: CartModel? = null

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

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    LogUtil.logE(
                        TAG,
                        "splitAmountValue  ${prefProvider.getValue(SPLIT_PAY_AMOUNT, "")}"
                    )
                    if (prefProvider.getValue(SPLIT_PAY_AMOUNT, "") == "") {
                        prefProvider.setValue(SPLIT_PAY_AMOUNT, "")
                        prefProvider.setValueInt(SPLIT_NO, -1)
                        findNavController().navigate(R.id.action_paymentFragment_to_dashboardCategoryNew)
                    } else {
                        AlertUtils.showCustomAlert(requireContext(), "Please complete all payment.")
                    }


                }

            }

        isFromActiveOrder = arguments?.getBoolean("isFromActiveOrder") ?: false
        isFromDashboard = arguments?.getBoolean(Constants.IS_NEXT_AMOUNT) ?: false

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        binding = PaymentFragmentBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.model = viewModel
        splitValue = -1
        isSplitByNo = false
        isSplitByAmount = false
        tipAmount = 0.0
        tipID = null

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
        cartItems = cartList?.items
        manualSaleCart = requireArguments().getParcelable("cartList")

        totalPrice = requireArguments().getDouble("totalPrice")
        subTotalPrice = requireArguments().getDouble("subTotalPrice")
        totalTax = requireArguments().getDouble("totalTax")
        totalServiceCharge = requireArguments().getDouble("totalServiceCharge")

        totalDiscount = requireArguments().getDouble("totalDiscount")
        cashDiscountType = prefProvider.getValue(Constants.OPTION_TYPE, "")

        redeemLoyaltyInfo = requireArguments().getParcelable("redeemLoyalty")

        isUpdate = requireArguments().getBoolean("update")

        Log.e(TAG, "isSplitByAmount:  ${isSplitByAmount}")

        viewModel.setSer(requireArguments().getDouble("totalServiceCharge"))
        viewModel.setDis(requireArguments().getDouble("totalDiscount"))


        if (isUpdate) {

            orderId = requireArguments().getInt("orderId")
            paymentId = requireArguments().getInt("paymentId")
            paymentOfflineId = requireArguments().getString("paymentOfflineId").toString()
            orderOfflineId = requireArguments().getString("orderOfflineId").toString()
        }

        future_delivery_time = requireArguments().getString("future_delivery_time").toString()
        future_delivery_date = requireArguments().getString("future_delivery_date").toString()



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
        LogUtil.logE(TAG, "gottotalPrice:  ${totalPrice}")
        viewModel.saveActualValue(
            totalPrice,
            subTotalPrice,
            totalTax,
            totalServiceCharge,
            tipAmount,
            totalDiscount,
            MethodUtils.calculateCashDiscount(totalPrice, prefProvider, requireContext()),
            cardActualAmount
        )
        setUpPaymentSummary()

        if (prefProvider.getValue("WholeTotal", "").isEmpty()) {
            WholetotalPrice = totalPrice
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
            viewModel.setSer(totalServiceCharge)
        } else {
            totalServiceCharge = prefProvider.getValue(SERVICE_CHARGE, "").toDouble()
            viewModel.setSer(prefProvider.getValue(SERVICE_CHARGE, "").toDouble())
        }


        if (prefProvider.getValue(TOTAL_DISCOUNT, "").isEmpty()) {
            prefProvider.setValue(TOTAL_DISCOUNT, String.format("%.2f", totalDiscount))
            viewModel.setDis(totalDiscount)
        } else {
            totalDiscount = prefProvider.getValue(TOTAL_DISCOUNT, "").toDouble()
            viewModel.setDis(prefProvider.getValue(TOTAL_DISCOUNT, "").toDouble())
        }


        if (prefProvider.getValue(TIP, "").isEmpty()) {
            prefProvider.setValue(TIP, String.format("%.2f", tipAmount))
        } else {
            tipAmount = prefProvider.getValue(TIP, "").toDouble()
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


        if (isFromDashboard) {
            var data = prefProvider.getValue(Constants.SAVE_SPLIT_BUNDLE, "")

            if (data.isNotEmpty()) {

                var splitModel =
                    Gson().fromJson<SplitBundleModel>(data, SplitBundleModel::class.java)
                LogUtil.logE(TAG, "splitModel:  ${Gson().toJson(splitModel)}")
                isNextPayment = splitModel.isNextPayment
                remainingAmount = splitModel.remainingAmt
                splitValue = splitModel.splitValue
                isSplitByAmount = splitModel.isSplitByAmount
                isSplitByNo = splitModel.isSplitByNo
                WholetotalPrice = splitModel.remainingAmt
                subTotalPrice = splitModel.subTotalWT
                totalTax = splitModel.totalTaxAmount
                totalDiscount = splitModel.totalDiscount
                totalServiceCharge = splitModel.serviceCharge
                cashDiscountSurcharge = splitModel.cashDiscountSurcharge
                tipAmount = splitModel.tip
                totalPrice = splitModel.remainingAmt
                cartList = splitModel.cartlist
                setUpPaymentSummaryActual(
                    prefProvider.getValue(TOTAL_PRICE_ACTUAL, "").toDouble(),
                    prefProvider.getValue(SUB_TOTAL_ACTUAL, "").toDouble(),
                    prefProvider.getValue(TOTAL_SERVICE_CHARGE_ACTUAL, "").toDouble(),
                    prefProvider.getValue(TAX_CHARGE_ACTUAL, "").toDouble(),
                    prefProvider.getValue(TIPS_AMOUNT_ACTUAL, "").toDouble(),
                    prefProvider.getValue(TOTAL_DISCOUNT_ACTUAL, "").toDouble(),
                    splitModel.redeemLoyaltyInfo
                )
                setSplitData()
            }


        }


        return binding.root
    }

    private fun setUpPaymentSummaryActual(
        actualAmount: Double,
        actualSubTotal: Double,
        actualServiceCharge: Double,
        actualTax: Double,
        actualTip: Double,
        actualDiscount: Double,
        redeemLoyaltyInfo: RedeemLoyaltyInfo?
    ) {
        LogUtil.logE(TAG, "actualAmount  ${actualAmount}")
        MethodUtils.setPriceTextView(binding.txtTotal, actualAmount)
        MethodUtils.setPriceTextView(binding.txtSubTotal, actualSubTotal)
        MethodUtils.setPriceTextView(binding.txtTax, actualTax)
        MethodUtils.setPriceTextView(binding.txtTipAmt, actualTip)
        MethodUtils.setPriceTextView(binding.txtServiceCharge, actualServiceCharge)

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
        binding.linearDiscount.visibility = View.VISIBLE
        binding.txtDiscount.text = "- " +
                MainApplication.getInstance()!!.getText(R.string.symbole)
                    .toString() + String.format(
            "%.2f", actualDiscount
        )

        if (MethodUtils.isEnableCashDiscount(requireContext())) {
            if (cashDiscountType == "CashDiscount") {
                binding.linearnoncashAdj.visibility = View.VISIBLE
                noCashAdj =
                    MethodUtils.calculateCashDiscount(actualAmount, prefProvider, requireContext())
                binding.txtNoncashAdj.text = "$ " + String.format(
                    "%.2f",
                    MethodUtils.calculateCashDiscount(actualAmount, prefProvider, requireContext())
                )
                totalPrice -= cashDiscountSurcharge
            } else if (cashDiscountType == "SurCharge") {
                binding.linearnoncashAdj.visibility = View.GONE
            }
        } else {
            binding.linearnoncashAdj.visibility = View.GONE
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setSplitData() {
        if (isNextPayment) {
            if (isSplitByNo) {
                isCustomCash = false
                if (MethodUtils.isEnableCashDiscount(requireContext())) {
                    var last_cash_discount_surcharge =
                        prefProvider.getValue(CASH_DISCOUNT_SURCHARGE, "")//3.33
                    if (last_cash_discount_surcharge.isEmpty() || last_cash_discount_surcharge == "0.0") {
                        last_cash_discount_surcharge = "0.0"
                    }
                    cardPaymentAmount =
                        (remainingAmount + last_cash_discount_surcharge.toDouble())
                    LogUtil.logE(
                        TAG,
                        "paylast_cash_discount_surcharge ${last_cash_discount_surcharge.toDouble()}"
                    )
                    LogUtil.logE(TAG, "paycardPaymentAmount: ${cardPaymentAmount}")
                    MethodUtils.setPriceTextView(binding.txtCardAmount, cardPaymentAmount)
                    //  binding.txtCardAmount.text = "$ " + String.format("%.2f", cardPaymentAmount)
                    if (prefProvider.getValue(CASH_DISCOUNT_SURCHARGE, "").isNotEmpty()) {
                        cashDiscountSurcharge =
                            prefProvider.getValue(CASH_DISCOUNT_SURCHARGE, "").toDouble()
                    }
                } else {
                    cardPaymentAmount = remainingAmount
                    MethodUtils.setPriceTextView(binding.txtCardAmount, remainingAmount)
//                    binding.txtCardAmount.text =
//                        "$" + String.format("%.2f", remainingAmount)
                }
                MethodUtils.setPriceTextView(binding.txtTotalAmount, remainingAmount)
                getCashPaymentOptionList(remainingAmount)
            } else if (isSplitByAmount) {
                isCustomCash = false
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
                        MethodUtils.setPriceTextView(binding.txtCardAmount, cardPaymentAmount)
                        //  binding.txtCardAmount.text = "$ " + String.format("%.2f", cardPaymentAmount)
                    } else if (cashDiscountType == "SurCharge") {
                        cardPaymentAmount = splitAfterAmount - cashDiscountSurcharge
                        MethodUtils.setPriceTextView(binding.txtCardAmount, cardPaymentAmount)
                        /*binding.txtCardAmount.text =
                            "$ " + String.format("%.2f", cardPaymentAmount)*/
                    }
                } else {
                    cardPaymentAmount = remainingAmount
                    MethodUtils.setPriceTextView(binding.txtCardAmount, remainingAmount)
//                    binding.txtCardAmount.text =
//                        "$" + String.format("%.2f", remainingAmount)
                }
                LogUtil.logE(TAG, "remainingAmount  ${remainingAmount}")
                MethodUtils.setPriceTextView(binding.txtTotalAmount, remainingAmount)
                getCashPaymentOptionList(remainingAmount)

            }
            isSplitByNo = false
        } else {
            if (isSplitByNo) {
                if (MethodUtils.isEnableCashDiscount(requireContext())) {
                    cardPaymentAmount = (splitAfterAmount + (cashDiscountSurcharge / splitValue))

                    LogUtil.logE(TAG, "paycardPaymentAmount:  ${cardPaymentAmount}")
                    MethodUtils.setPriceTextView(
                        binding.txtCardAmount,
                        (splitAfterAmount + (cashDiscountSurcharge / splitValue))
                    )
//                    binding.txtCardAmount.text =
//                        "$ " + String.format(
//                            "%.2f",
//                            (splitAfterAmount + (cashDiscountSurcharge / splitValue))
//                        )
                } else {
                    cardPaymentAmount = splitAfterAmount
                    MethodUtils.setPriceTextView(binding.txtCardAmount, splitAfterAmount)
//                    binding.txtCardAmount.text =
//                        "$" + String.format("%.2f", splitAfterAmount)
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
                        // binding.txtCardAmount.text = "$ " + String.format("%.2f", cardPaymentAmount)
                        MethodUtils.setPriceTextView(binding.txtCardAmount, cardPaymentAmount)
                    } else if (cashDiscountType == "SurCharge") {
                        cardPaymentAmount = splitAfterAmount - cashDiscountSurcharge
                        MethodUtils.setPriceTextView(binding.txtCardAmount, cardPaymentAmount)
                        /*binding.txtCardAmount.text =
                            "$ " + String.format("%.2f", cardPaymentAmount)*/
                    }
                } else {
                    cardPaymentAmount = splitAfterAmount
                    MethodUtils.setPriceTextView(binding.txtCardAmount, cardPaymentAmount)
                    /*binding.txtCardAmount.text =
                        "$" + String.format("%.2f", cardPaymentAmount)*/
                }

                MethodUtils.setPriceTextView(binding.txtTotalAmount, splitAfterAmount)
                getCashPaymentOptionList(splitAfterAmount)
            }
        }
    }

    private fun setUpPaymentSummary() {
        LogUtil.logE(TAG, "SetupSummaryTotal  ${totalPrice}")
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
        binding.linearDiscount.visibility = View.VISIBLE
        binding.txtDiscount.text = "- " +
                MainApplication.getInstance()!!.getText(R.string.symbole)
                    .toString() + String.format(
            "%.2f", totalDiscount
        )

        if (MethodUtils.isEnableCashDiscount(requireContext())) {
            if (cashDiscountType == "CashDiscount") {
                MethodUtils.setPriceTextView(binding.txtCardAmount, totalPrice)
                // binding.txtCardAmount.text = "$ " + String.format("%.2f", totalPrice)
                cardPaymentAmount = totalPrice
                binding.linearnoncashAdj.visibility = View.VISIBLE
                noCashAdj =
                    MethodUtils.calculateCashDiscount(totalPrice, prefProvider, requireContext())
                binding.txtNoncashAdj.text = "$ " + String.format(
                    "%.2f",
                    MethodUtils.calculateCashDiscount(totalPrice, prefProvider, requireContext())
                )
                LogUtil.logE(TAG, "cashDiscountSurcharge:  ${cashDiscountSurcharge}")
                totalPrice -= cashDiscountSurcharge
                if (totalPrice < 0.0) {
                    totalPrice = 0.0
                }
            } else if (cashDiscountType == "SurCharge") {
                binding.linearnoncashAdj.visibility = View.GONE
                MethodUtils.setPriceTextView(
                    binding.txtCardAmount,
                    totalPrice + cashDiscountSurcharge
                )
                /* binding.txtCardAmount.text =
                     "$ " + String.format("%.2f", totalPrice + cashDiscountSurcharge)*/
                cardPaymentAmount = totalPrice + cashDiscountSurcharge
            }
        } else {
            MethodUtils.setPriceTextView(binding.txtCardAmount, totalPrice)
            /* binding.txtCardAmount.text =
                 "$" + String.format("%.2f", totalPrice)*/
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
                    noCashAdj = MethodUtils.roundOffAmountDouble(cashDiscountSurcharge)
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
                cardPaymentAmount
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
                        noCashAdj =
                            MethodUtils.roundOffAmountDouble(cashDiscountSurcharge / splitValue)
                        binding.txtNoncashAdj.text =
                            "$ " + String.format("%.2f", cashDiscountSurcharge / splitValue)
                        totalPrice += (cashDiscountSurcharge / splitValue)
                    } else {
                        noCashAdj = MethodUtils.roundOffAmountDouble(cashDiscountSurcharge)
                        binding.txtNoncashAdj.text =
                            "$ " + String.format("%.2f", cashDiscountSurcharge)
                        totalPrice += (cashDiscountSurcharge)
                    }
                }
            } else {
                binding.linearnoncashAdj.visibility = View.GONE
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
            val totalPrice = bundle.getDouble("totalAmount")
            finalPrice = amounnt + tipAmount
            isCustomCash = true
            paymentAmount = amounnt
            if (isSplitByNo) {
                var remaining_payment =
                    String.format(
                        "%.2f",
                        prefProvider.getValue("WholeTotal", "")
                            .toDouble() - splitAfterAmount
                    ).toDouble()
                if (remaining_payment <= 0.0) {
                    prefProvider.setValueboolean("isLastPayment", true)
                } else {
                    prefProvider.setValueboolean("isLastPayment", false)
                }
                var remainningCashDiscount =
                    cashDiscountSurcharge - (cashDiscountSurcharge / splitValue)
                prefProvider.setValue(
                    CASH_DISCOUNT_SURCHARGE,
                    String.format("%.2f", remainningCashDiscount)
                )
            }
            makePayment()
        }

        setFragmentResultListener("request_key_tips") { requestKey: String, bundle: Bundle ->
            tipAmount = bundle.getDouble("tipAmount")
            tipID = bundle.getInt("tipId")

            tipAmountCalculation()
        }


    }

    override fun onDestroy() {
        super.onDestroy()
        ProgressUtils.dismissProgressDialog()
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
        binding.txtCardAmount.text =
            "$ " + String.format("%.2f", cardPaymentAmount + tipAmount)
    }


    @SuppressLint("SetTextI18n")
    private fun getCashPaymentOptionList(totalPrice: Double) {
        LogUtil.logE(TAG, "totalPrice  $totalPrice")
        secondValue = floor(totalPrice + 1).toInt()
        LogUtil.logE(TAG, "secondValue  $secondValue")
        val newVal = totalPrice + 1
        thirdValue = calculateCashOption(newVal)
        LogUtil.logE(TAG, "thirdValuethirdValue:   ${thirdValue}")
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

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.linearMore -> {
                //createQueuePrinter()
            }

            R.id.imgBack -> {

                LogUtil.logE(
                    TAG,
                    "splitAmountValue  ${prefProvider.getValue(SPLIT_PAY_AMOUNT, "")}"
                )
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
                        LogUtil.logE(TAG, "SplitNo")
                        var remaining_payment =
                            String.format(
                                "%.2f",
                                prefProvider.getValue("WholeTotal", "")
                                    .toDouble() - splitAfterAmount
                            ).toDouble()
                        if (remaining_payment <= 0.0) {
                            prefProvider.setValueboolean("isLastPayment", true)
                        } else {
                            prefProvider.setValueboolean("isLastPayment", false)
                        }
                        var remainningCashDiscount =
                            cashDiscountSurcharge - (cashDiscountSurcharge / splitValue)
                        prefProvider.setValue(
                            CASH_DISCOUNT_SURCHARGE,
                            String.format("%.2f", remainningCashDiscount)
                        )
                        splitAfterAmount
                    }
                    isSplitByAmount -> {
                        LogUtil.logE(TAG, "SplitByAmount")
                        splitAfterAmount
                    }
                    isCustomCash -> {
                        LogUtil.logE(TAG, "CustomCash")
                        paymentAmount
                    }
                    else -> {
                        if (remainingAmount == 0.0) {
                            LogUtil.logE(TAG, "RemainingAmtZero")
                            totalPrice
                        } else {
                            LogUtil.logE(TAG, "RemainingNotZero")
                            remainingAmount
                        }
                    }
                }
                makePayment()

            }
            R.id.txtSecondAmount -> {
                finalPrice = totalPrice + tipAmount
                isCustomCash = true
                if (isSplitByNo) {
                    var remaining_payment =
                        String.format(
                            "%.2f",
                            prefProvider.getValue("WholeTotal", "")
                                .toDouble() - splitAfterAmount
                        ).toDouble()
                    if (remaining_payment <= 0.0) {
                        prefProvider.setValueboolean("isLastPayment", true)
                    } else {
                        prefProvider.setValueboolean("isLastPayment", false)
                    }
                    var remainningCashDiscount =
                        cashDiscountSurcharge - (cashDiscountSurcharge / splitValue)
                    prefProvider.setValue(
                        CASH_DISCOUNT_SURCHARGE,
                        String.format("%.2f", remainningCashDiscount)
                    )
                }
                paymentAmount = secondValue.toDouble()
                makePayment()
            }
            R.id.txtThirdAmount -> {
                finalPrice = totalPrice + tipAmount
                isCustomCash = true
                if (isSplitByNo) {
                    var remaining_payment =
                        String.format(
                            "%.2f",
                            prefProvider.getValue("WholeTotal", "")
                                .toDouble() - splitAfterAmount
                        ).toDouble()
                    if (remaining_payment <= 0.0) {
                        prefProvider.setValueboolean("isLastPayment", true)
                    } else {
                        prefProvider.setValueboolean("isLastPayment", false)
                    }
                    var remainningCashDiscount =
                        cashDiscountSurcharge - (cashDiscountSurcharge / splitValue)
                    prefProvider.setValue(
                        CASH_DISCOUNT_SURCHARGE,
                        String.format("%.2f", remainningCashDiscount)
                    )
                }

                paymentAmount = thirdValue
                makePayment()
            }
            R.id.txtFourthAmount -> {
                finalPrice = totalPrice + tipAmount
                isCustomCash = true
                if (isSplitByNo) {
                    var remaining_payment =
                        String.format(
                            "%.2f",
                            prefProvider.getValue("WholeTotal", "")
                                .toDouble() - splitAfterAmount
                        ).toDouble()
                    if (remaining_payment <= 0.0) {
                        prefProvider.setValueboolean("isLastPayment", true)
                    } else {
                        prefProvider.setValueboolean("isLastPayment", false)
                    }
                    var remainningCashDiscount =
                        cashDiscountSurcharge - (cashDiscountSurcharge / splitValue)
                    prefProvider.setValue(
                        CASH_DISCOUNT_SURCHARGE,
                        String.format("%.2f", remainningCashDiscount)
                    )
                }

                paymentAmount = fourthValue
                makePayment()
            }


            R.id.llCash -> {
                paymentType = "Cash"
                paymentAmount = when {
                    isSplitByNo -> {
                        var remaining_payment =
                            String.format(
                                "%.2f",
                                prefProvider.getValue("WholeTotal", "")
                                    .toDouble() - splitAfterAmount
                            ).toDouble()
                        if (remaining_payment <= 0.0) {
                            prefProvider.setValueboolean("isLastPayment", true)
                        } else {
                            prefProvider.setValueboolean("isLastPayment", false)
                        }
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
                            totalPrice
                        } else {
                            remainingAmount
                        }
                    }
                }
                makePayment()

            }
            9 +
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

                if (splitAfterAmount != 0.0) {
                    bundle.putDouble("totalprice", (splitAfterAmount + tipAmount))
                } else {
                    bundle.putDouble("totalprice", ((totalPrice + tipAmount)))
                }
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
                            if (remainingAmount == 0.0) {
                                bundle.putDouble("totalPrice", totalPrice + tipAmount)
                            } else {
                                bundle.putDouble("totalPrice", remainingAmount)
                            }
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
                future_delivery_time,
                true,
                totalDiscount,
                tipAmount,
                splitValue,
                redeemLoyaltyInfo,
                cashDiscountSurcharge,
                true,
                paymentType, cashDiscountType,
                tipID,
                true, offlineId = createOrder.data.order.offlineId
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

        if (cartItems?.isNotEmpty() == true && cartList?.items?.isEmpty() == true) {
            cartList?.items = cartItems
        }
        if (isSplitByNo) {
            LogUtil.logE(TAG, "isSplitByNo:  ${isSplitByNo}")

            val myRequest = cartList?.let {
                viewModel.createOrderRequestForCard(
                    it,
                    subTotalPrice / splitValue,
                    cardPaymentAmount,
                    totalServiceCharge / splitValue,
                    totalTax / splitValue,
                    prefProvider.getValue(Constants.ORDER_TYPE, "").toString(),
                    future_delivery_date,
                    future_delivery_time,
                    true,
                    totalDiscount / splitValue,
                    tipAmount,
                    splitValue,
                    redeemLoyaltyInfo,
                    cashDiscountSurcharge / splitValue,
                    true,
                    paymentType, cashDiscountType,
                    "tipID",
                    totalServiceChargeM = totalServiceCharge,
                    totalDiscountM =  totalDiscount

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
                        SpitByOrderPaymentModel(listOf(paymentReq) as List<PaymentAttributes>)
                    )

                    viewModel.splitByOrder(aa, false)

                }
            }
        } else if (isSplitByAmount) {
            try {

                val myRequest = cartList?.let {

                    viewModel.createOrderRequestForCard(
                        it,
                        subTotalPrice,
                        cardPaymentAmount,
                        totalServiceCharge,
                        totalTax,
                        prefProvider.getValue(Constants.ORDER_TYPE, "").toString(),
                        future_delivery_date,
                        future_delivery_time,
                        false,
                        totalDiscount,
                        tipAmount,
                        splitValue,
                        redeemLoyaltyInfo,
                        cashDiscountSurcharge,
                        true,
                        paymentType,
                        cashDiscountType,
                        "tipID"
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
                            SpitByOrderPaymentModel(listOf(paymentReq) as List<PaymentAttributes>)
                        )


                        viewModel.splitByOrder(aa!!, false)

                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "makePayment: " + e.toString())
            }
        } else {


            val myRequest = cartList?.let {

                viewModel.createOrderRequestForCard(
                    it,
                    subTotalPrice,
                    cardPaymentAmount,
                    totalServiceCharge,
                    totalTax,
                    prefProvider.getValue(Constants.ORDER_TYPE, "").toString(),
                    future_delivery_date,
                    future_delivery_time,
                    true,
                    totalDiscount,
                    tipAmount,
                    splitValue,
                    redeemLoyaltyInfo,
                    cashDiscountSurcharge,
                    true,
                    paymentType, cashDiscountType,
                    "tipID"
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
                        SpitByOrderPaymentModel(listOf(paymentReq) as List<PaymentAttributes>)
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

        val isLastPayment = prefProvider.getValueboolean("isLastPayment", false)
        if (isSplitByNo && !isLastPayment && isCustomCash) {

            val myRequest = cartList?.let {

                viewModel.createOrderRequest(
                    it,
                    subTotalPrice / splitValue,
                    splitAfterAmount,
                    totalServiceCharge / splitValue,
                    totalTax / splitValue,
                    prefProvider.getValue(Constants.ORDER_TYPE, "").toString(),
                    future_delivery_date,
                    future_delivery_time,
                    false,
                    totalDiscount / splitValue,
                    tipAmount,
                    splitValue,
                    redeemLoyaltyInfo,
                    cashDiscountSurcharge / splitValue,
                    true,
                    paymentType,
                    cashDiscountType,
                    tipID
                )
            }
            LogUtil.logE(TAG, "myRequest  ${Gson().toJson(myRequest)}")
            if (myRequest != null) {
                viewModel.totalPayAmount((paymentAmount))

                val orderId = prefProvider.getValueInt("ORDER_ID", -1)
                LogUtil.logE(TAG, "orderIdInside  ${orderId}")

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

                        SpitByOrderPaymentModel(listOf(paymentReq) as List<PaymentAttributes>)
                    )


                    viewModel.splitByOrder(aa!!, false)

                }


            }
        } else if (isSplitByNo && !isLastPayment) {
            val myRequest = cartList?.let {

                viewModel.createOrderRequest(
                    it,
                    subTotalPrice / splitValue,
                    splitAfterAmount,
                    totalServiceCharge / splitValue,
                    totalTax / splitValue,
                    prefProvider.getValue(Constants.ORDER_TYPE, "").toString(),
                    future_delivery_date,
                    future_delivery_time,
                    false,
                    totalDiscount / splitValue,
                    tipAmount,
                    splitValue,
                    redeemLoyaltyInfo,
                    cashDiscountSurcharge / splitValue,
                    true,
                    paymentType,
                    cashDiscountType,
                    tipID,
                    totalServiceChargeM = totalServiceCharge,
                    totalDiscountM =  totalDiscount
                )
            }
            LogUtil.logE(TAG, "myRequestSplitNo  ${Gson().toJson(myRequest)}")
            if (myRequest != null) {
                viewModel.totalPayAmount((paymentAmount))

                val orderId = prefProvider.getValueInt("ORDER_ID", -1)
                LogUtil.logE(TAG, "orderidinsplit  ${orderId}")

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

                        SpitByOrderPaymentModel(listOf(paymentReq) as List<PaymentAttributes>)
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
                        future_delivery_time,
                        false,
                        totalDiscount,
                        tipAmount,
                        splitValue,
                        redeemLoyaltyInfo,
                        cashDiscountSurcharge,
                        true,
                        paymentType,
                        cashDiscountType,
                        tipID
                    )
                }
                LogUtil.logE(TAG, "myRequest  ${Gson().toJson(myRequest)}")
                if (myRequest != null) {
                    viewModel.totalPayAmount(splitAfterAmount)
                    val orderId = prefProvider.getValueInt("ORDER_ID", -1)
                    LogUtil.logE(TAG, "orderIDIsLast  ${orderId})}")
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

                            SpitByOrderPaymentModel(listOf(paymentReq) as List<PaymentAttributes>)
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
                    WholetotalPrice,
                    totalServiceCharge,
                    totalTax,
                    prefProvider.getValue(Constants.ORDER_TYPE, "").toString(),
                    future_delivery_date,
                    future_delivery_time,
                    true,
                    totalDiscount,
                    tipAmount,
                    splitValue,
                    redeemLoyaltyInfo,
                    cashDiscountSurcharge,
                    true,
                    paymentType, cashDiscountType,
                    tipID
                )
            }
            LogUtil.logE(TAG, "myRequest  ${Gson().toJson(myRequest)}")
            if (myRequest != null) {
                viewModel.totalPayAmount(paymentAmount)
                val orderId = prefProvider.getValueInt("ORDER_ID", -1)
                LogUtil.logE(TAG, "orderIdCustom  ${orderId}")
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

                        SpitByOrderPaymentModel(listOf(paymentReq) as List<PaymentAttributes>)
                    )

                    viewModel.splitByOrder(aa, false)

                }
            }
        } else {

            LogUtil.logE(TAG, "cartList:  ${Gson().toJson(cartList)}")
            LogUtil.logE(TAG, "cartListcartItems:  ${Gson().toJson(cartItems)}")
            if (cartItems?.isNotEmpty() == true && cartList?.items?.isEmpty() == true) {
                cartList?.items = cartItems
            }
            val myRequest = cartList?.let {

                viewModel.createOrderRequest(
                    it,
                    subTotalPrice,
                    paymentAmount,
                    totalServiceCharge,
                    totalTax,
                    prefProvider.getValue(Constants.ORDER_TYPE, "").toString(),
                    future_delivery_date,
                    future_delivery_time,
                    true,
                    totalDiscount,
                    tipAmount,
                    splitValue,
                    redeemLoyaltyInfo,
                    cashDiscountSurcharge,
                    true,
                    paymentType, cashDiscountType,
                    tipID
                )
            }
            LogUtil.logE(TAG, "myRequestOriginal ${Gson().toJson(myRequest)}")
            if (myRequest != null) {
                viewModel.totalPayAmount(paymentAmount)
                val orderId = prefProvider.getValueInt("ORDER_ID", -1)
                LogUtil.logE(TAG, "orderIdmyRequestOriginal ${orderId}")
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

                        SpitByOrderPaymentModel(listOf(paymentReq) as List<PaymentAttributes>)
                    )

                    viewModel.splitByOrder(aa, false)

                }
            }
        }
    }


    private fun observeShowProgress() {

        viewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                LogUtil.logE("observeShowProgress4", it.toString())
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }

    }

    private fun observeQueueStart() {
        viewModel.QueueStartTakeOut.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                createQueuePrinter(it)


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

    private fun observeData() {

        viewModel.data.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {

                LogUtil.logE("observe : splitValue", splitValue.toString())

                prefProvider.setValueInt("ORDER_ID", it.data.order.id)

                viewModel.updateActiveOrderFlagClear()

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
                                        prefProvider.setValueInt("ORDER_ID", -1)
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
                                    prefProvider.setValueInt("ORDER_ID", -1)
                                    prefProvider.setValue(TOTAL_DISCOUNT, "")
                                    prefProvider.setValue(TIP, "")
                                    prefProvider.setValue(TAX_CHARGE, "")
                                    prefProvider.setValue(SERVICE_CHARGE, "")
                                }
                                bundle.putDouble("TipAmount", tipAmount)
                                bundle.putBoolean("isSplitByNo", isSplitByNo)
                                bundle.putBoolean("isSplitByAmount", isSplitByAmount)
                                bundle.putString("paymentType", "Card")
                                bundle.putBoolean("isDineIn", false)
                                bundle.putParcelable("cartList", cartList)
                                bundle.putParcelable("redeemLoyalty", redeemLoyaltyInfo)
                                bundle.putDouble("noCashAdj", noCashAdj)
                                bundle.putBoolean("isFromActiveOrder", isFromActiveOrder)
                                findNavController().navigate(
                                    R.id.action_paymentFragment_to_orderCompleteFragment,
                                    bundle
                                )

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
                                bundle.putInt("orderID", it.data.order.id ?: 0)
                                bundle.putParcelable("receiptData", it.data)
                                bundle.putInt("splitValue", splitValue)
                                if (splitValue != -1) {
                                    if (WholetotalPrice <= cardPaymentAmount) {
                                        bundle.putBoolean("isSpilt", false)
                                        prefProvider.setValueInt("ORDER_ID", -1)
                                    } else {
                                        bundle.putBoolean("isSpilt", true)
                                    }
                                } else {
                                    bundle.putBoolean("isSpilt", false)
                                }
                                bundle.putDouble("TipAmount", tipAmount)
                                bundle.putBoolean("isSplitByNo", isSplitByNo)
                                bundle.putBoolean("isSplitByAmount", isSplitByAmount)
                                bundle.putString("paymentType", "Card")
                                bundle.putBoolean("isDineIn", false)
                                bundle.putParcelable("cartList", cartList)
                                bundle.putParcelable("redeemLoyalty", redeemLoyaltyInfo)
                                bundle.putDouble("noCashAdj", noCashAdj)
                                bundle.putBoolean("isFromActiveOrder", isFromActiveOrder)
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
                                bundle.putDouble("TipAmount", tipAmount)
                                bundle.putDouble("dis_charge_value", 0.0)
                                bundle.putInt("orderID", it.data.order.id)
                                bundle.putParcelable("receiptData", it.data)
                                bundle.putInt("splitValue", -1)
                                bundle.putBoolean("isSpilt", false)
                                bundle.putBoolean("isSplitByNo", isSplitByNo)
                                bundle.putBoolean("isSplitByAmount", isSplitByAmount)
                                bundle.putString("paymentType", "Card")
                                bundle.putParcelable("cartList", cartList)
                                bundle.putParcelable("redeemLoyalty", redeemLoyaltyInfo)
                                bundle.putDouble("noCashAdj", noCashAdj)
                                bundle.putBoolean("isFromActiveOrder", isFromActiveOrder)
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
                            isSplitByNo && isCustomCash -> {
                                val bundle = Bundle()
                                bundle.putDouble("PaidAmount", paymentAmount)
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
                                    "remainingAmount", remainingAmount
                                )
                                var splitChange = paymentAmount - splitAfterAmount
                                bundle.putDouble(
                                    "splitChange", String.format("%.2f", splitChange).toDouble()
                                )
                                bundle.putInt("orderID", it.data.order.id ?: 0)
                                bundle.putParcelable("receiptData", it.data)
                                bundle.putInt("splitValue", splitValue)
                                bundle.putDouble("TipAmount", tipAmount)

                                LogUtil.logE("TipAmount 1:: ", tipAmount.toString())

                                if (splitValue != -1) {
                                    if (remainingAmount <= 0.0) {
                                        bundle.putBoolean("isSpilt", false)
                                        prefProvider.setValue(SUB_TOTAL, "")
                                        prefProvider.setValue(TOTAL_DISCOUNT, "")
                                        prefProvider.setValue(TIP, "")
                                        prefProvider.setValue(TAX_CHARGE, "")
                                        prefProvider.setValue(SERVICE_CHARGE, "")
                                        prefProvider.setValueInt("ORDER_ID", -1)
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
                                    prefProvider.setValueInt("ORDER_ID", -1)
                                    prefProvider.setValue(TOTAL_DISCOUNT, "")
                                    prefProvider.setValue(TIP, "")
                                    prefProvider.setValue(TAX_CHARGE, "")
                                    prefProvider.setValue(SERVICE_CHARGE, "")
                                }
                                bundle.putBoolean("isSplitByNo", isSplitByNo)
                                bundle.putBoolean("isSplitByAmount", isSplitByAmount)
                                bundle.putBoolean("isCustomCash", isCustomCash)
                                bundle.putString("paymentType", "Cash")
                                bundle.putBoolean("isDineIn", false)
                                bundle.putParcelable("cartList", cartList)
                                bundle.putParcelable("redeemLoyalty", redeemLoyaltyInfo)
                                bundle.putDouble("noCashAdj", noCashAdj)
                                bundle.putBoolean("isFromActiveOrder", isFromActiveOrder)
                                findNavController().navigate(
                                    R.id.action_paymentFragment_to_orderCompleteFragment,
                                    bundle
                                )
                            }
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
                                bundle.putInt("orderID", it.data.order.id ?: 0)
                                bundle.putParcelable("receiptData", it.data)
                                bundle.putInt("splitValue", splitValue)
                                bundle.putDouble("TipAmount", tipAmount)
                                LogUtil.logE("TipAmount 2:: ", tipAmount.toString())
                                if (splitValue != -1) {
                                    if (remainingAmount <= 0.0) {
                                        bundle.putBoolean("isSpilt", false)
                                        prefProvider.setValue(SUB_TOTAL, "")
                                        prefProvider.setValue(TOTAL_DISCOUNT, "")
                                        prefProvider.setValue(TIP, "")
                                        prefProvider.setValue(TAX_CHARGE, "")
                                        prefProvider.setValue(SERVICE_CHARGE, "")
                                        prefProvider.setValueInt("ORDER_ID", -1)
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
                                    prefProvider.setValueInt("ORDER_ID", -1)
                                    prefProvider.setValue(TOTAL_DISCOUNT, "")
                                    prefProvider.setValue(TIP, "")
                                    prefProvider.setValue(TAX_CHARGE, "")
                                    prefProvider.setValue(SERVICE_CHARGE, "")
                                }
                                bundle.putBoolean("isSplitByNo", isSplitByNo)
                                bundle.putBoolean("isSplitByAmount", isSplitByAmount)
                                bundle.putString("paymentType", "Cash")
                                bundle.putParcelable("cartList", cartList)
                                bundle.putParcelable("redeemLoyalty", redeemLoyaltyInfo)
                                bundle.putBoolean("isDineIn", false)
                                bundle.putDouble("noCashAdj", noCashAdj)
                                bundle.putBoolean("isFromActiveOrder", isFromActiveOrder)
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
                                bundle.putInt("orderID", it.data.order.id ?: 0)
                                bundle.putParcelable("receiptData", it.data)
                                bundle.putParcelable("cartList", cartList)
                                bundle.putInt("splitValue", splitValue)
                                if (remainingAmount == 0.0) {
                                    bundle.putBoolean("isSpilt", false)
                                    prefProvider.setValueInt("ORDER_ID", -1)
                                } else {
                                    bundle.putBoolean("isSpilt", true)
                                }
                                bundle.putDouble("TipAmount", tipAmount)
                                bundle.putBoolean("isSplitByNo", isSplitByNo)
                                bundle.putBoolean("isSplitByAmount", isSplitByAmount)
                                bundle.putString("paymentType", "Cash")
                                bundle.putBoolean("isDineIn", false)
                                bundle.putDouble("noCashAdj", noCashAdj)
                                bundle.putBoolean("isFromActiveOrder", isFromActiveOrder)
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
                                var remaining_custom = paymentAmount - WholetotalPrice
                                bundle.putDouble(
                                    "remainingAmount",
                                    remaining_custom
                                )
                                bundle.putInt("orderID", it.data.order.id ?: 0)
                                bundle.putParcelable("receiptData", it.data)
                                bundle.putInt("splitValue", -1)
                                bundle.putBoolean("isSpilt", false)
                                bundle.putBoolean("isSplitByNo", isSplitByNo)
                                bundle.putBoolean("isSplitByAmount", isSplitByAmount)
                                bundle.putBoolean("isCustomCash", isCustomCash)
                                bundle.putString("paymentType", "Cash")
                                bundle.putParcelable("cartList", cartList)
                                bundle.putParcelable("redeemLoyalty", redeemLoyaltyInfo)
                                bundle.putDouble("TipAmount", tipAmount)
                                LogUtil.logE("TipAmount 3:: ", tipAmount.toString())
                                bundle.putDouble("noCashAdj", noCashAdj)
                                bundle.putBoolean("isFromActiveOrder", isFromActiveOrder)
                                findNavController().navigate(
                                    R.id.action_paymentFragment_to_orderCompleteFragment,
                                    bundle
                                )

                                prefProvider.setValueInt("ORDER_ID", -1)
                            }
                            else -> {

                                LogUtil.logE("TipAmount 4:: ", tipAmount.toString())

                                val bundle = Bundle()
                                bundle.putBoolean("isDineIn", false)

                                if (remainingAmount == 0.0) {
                                    bundle.putDouble("PaidAmount", totalPrice)
                                } else {
                                    bundle.putDouble("PaidAmount", remainingAmount)
                                }


                                bundle.putDouble("WholetotalPrice", WholetotalPrice)
                                bundle.putDouble(
                                    "remainingAmount",
                                    0.0
                                )
                                bundle.putInt("orderID", it.data.order.id ?: 0)
                                bundle.putParcelable("receiptData", it.data)
                                bundle.putInt("splitValue", -1)
                                bundle.putBoolean("isSpilt", false)
                                bundle.putBoolean("isSplitByNo", isSplitByNo)
                                bundle.putBoolean("isSplitByAmount", isSplitByAmount)
                                bundle.putString("paymentType", "Cash")
                                bundle.putParcelable("cartList", cartList)
                                bundle.putParcelable("redeemLoyalty", redeemLoyaltyInfo)
                                bundle.putDouble("TipAmount", tipAmount)

                                bundle.putDouble("noCashAdj", noCashAdj)
                                bundle.putBoolean("isFromActiveOrder", isFromActiveOrder)
                                if (findNavController().currentDestination?.id == R.id.paymentFragment) {
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
            }
        }

    }

    private fun queuePrinterObserver() {
        viewModel.queuePrinter.observe(requireActivity()) {
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
        }
    }
}