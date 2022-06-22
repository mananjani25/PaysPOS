package com.android.pos.ui.fragments.checkout

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.CartModel
import com.android.pos.data.entities.RedeemLoyaltyInfo
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.requestModel.OrderRequestModel
import com.android.pos.data.model.requestModel.PaymentAttributes
import com.android.pos.data.model.requestModel.SpitByOrderPaymentModel
import com.android.pos.data.model.requestModel.SpitByOrderRequestModel
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.FragmentCheckoutDetailsNewBinding
import com.android.pos.di.ApiModule1
import com.android.pos.di.MagtekModule
import com.android.pos.di.PrefProvider
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.ui.fragments.magtek.MagtekRequestUtils
import com.android.pos.ui.fragments.magtek.PaymentResponse
import com.android.pos.ui.fragments.magtekPro.MTParser
import com.android.pos.ui.fragments.magtekPro.SessionManager
import com.android.pos.ui.fragments.payment.PaymentBoldPosFragment
import com.android.pos.ui.fragments.payment.PaymentViewModel
import com.android.pos.utils.*
import com.android.pos.utils.callback.DeleteOptionCallback
import com.android.pos.utils.callback.magtekCallback
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.invisible
import com.android.pos.utils.extensions.runOnUiThread
import com.android.pos.utils.extensions.visible
import com.android.pos.utils.statusUtils.Status
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.magtek.mobile.android.mtlib.IMTCardData
import com.magtek.mobile.android.mtlib.MTConnectionState
import com.magtek.mobile.android.mtusdk.*
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class CheckoutDetailsFragmentNew(val isFromOpenOrder: Boolean = false) : Fragment(), magtekCallback,
    DeleteOptionCallback, IDeviceListCallback {
    private var cardNumber: String = ""
    private var isError: Boolean = false
    private var isCardRev: Boolean = false
    private var isInsert: Boolean = false
    private var isManualCard: Boolean = false
    private lateinit var binding: FragmentCheckoutDetailsNewBinding
    private val TAG = "DashboardCategoryBold"

    private var requestCancel: Boolean = false
    private var orderId: Int? = null
    private var orderOfflineId: String = ""
    private var paymentOfflineId: String = ""
    var isSelectedCount = 1
    private val paymentviewModel by activityViewModels<PaymentViewModel>()
    private val viewModel by activityViewModels<DashBoardCategoryViewModel>()

    var paymentType = "Cash"
    var cashDiscountSurcharge = 0.0
    var cardActualAmount = 0.0
    private var paymentId: Int = -1
    private var isPaymentScreen = true
    private var isSplitScreen = false

    private var remainingAmount: Double = 0.0
    var cashDiscountType = ""
    var paymentAmount = 0.0
    private var redeemLoyaltyInfo: RedeemLoyaltyInfo? = null
    var totalPrice = 0.0
    private var splitValue: Int = -1
    var tipAmount = 0.0
    private var cartItems: List<TbItem>? = null
    var subTotalPrice = 0.0
    var totalTax = 0.0
    private var WholetotalPrice: Double = 0.0
    var tipID = 0
    var totalServiceCharge = 0.0
    private var future_delivery_date: String = ""
    private var future_delivery_time: String = ""
    var totalDiscount = 0.0
    var cardPaymentAmount = 0.0

    private var cartList: CartModel? = null

    @Inject
    lateinit var magtekModule: MagtekModule

    @Inject
    lateinit var magtekRequestUtils: MagtekRequestUtils

    @Inject
    lateinit var apiModule1: ApiModule1

    @Inject
    lateinit var mSessionManager: SessionManager

    @Inject
    lateinit var prefProvider: PrefProvider

    private var splitAfterAmount: Double = 0.0
    private var custom_paymentAmount = 0.0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentCheckoutDetailsNewBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        val device = prefProvider.getValueInt(Constants.MAGTEK_HARDWARE, 0)

        if (device == 0) {
            magtekModule.setupInit()
            magtekModule.setCallback(this)
        } else {
            mSessionManager.setOutputFragment(this)

        }




        orderId = arguments?.getInt("orderId")

        Log.e("orderId :: ", orderId.toString())
        if (orderId != null) {
            paymentId = arguments?.getInt("paymentId")!!
            paymentOfflineId = arguments?.getString("paymentOfflineId").toString()
            orderOfflineId = arguments?.getString("orderOfflineId").toString()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getDataFromPref()
        setupTabDesign()
        paymentClick()
        splitClick()
        observeShowProgress()
        observeData()
        callback()
    }

    @SuppressLint("SetTextI18n")
    private fun callback() {
        requireActivity().supportFragmentManager.setFragmentResultListener(
            "request_key_tips",
            viewLifecycleOwner
        ) { _: String, bundle: Bundle ->
            tipAmount = bundle.getDouble("tipAmount")
            viewModel.setTipAmount(tipAmount)
            tipID = bundle.getInt("tipId")
//            isSelectedCount = 1
            tipAmountCalculation()
            loadPaymentLayout()
        }
        requireActivity().supportFragmentManager.setFragmentResultListener(
            "request_key_split",
            viewLifecycleOwner
        ) { _: String, bundle: Bundle ->

            isSelectedCount = bundle.getInt("split")
            if (isSelectedCount > 1) {
                binding.tvCustom.text = "Custom ($isSelectedCount Ways)"
            } else {
                binding.tvCustom.text = "Custom"
            }
            binding.tvwaysplit?.visible()
            binding.tvwaysplit?.text = "$isSelectedCount Way Split Amount"
            tipsetupGlobal(tipAmount, isSelectedCount)
        }
        requireActivity().supportFragmentManager.setFragmentResultListener(
            "request_for_customAmount",
            viewLifecycleOwner
        ) { _: String, bundle: Bundle ->
            val amount = bundle.getDouble("amount")
            val totalPrice = bundle.getDouble("totalAmount")
            MethodUtils.setPriceTextView(binding.tvCustomAmount, amount)
            custom_paymentAmount = amount
            binding.tvCustomAmount.text = "Custom (" + binding.tvCustomAmount.text.toString() + ")"
            cashPaymentWithVariation()
        }


    }

    private fun splitClick() {

        binding.linearNextSplit.setOnClickListener {
            PaymentBoldPosFragment.newInstance().addTipHideShow(false)
            viewModel.setSplitCount(isSelectedCount)
            loadPaymentLayout()
            tipAmountCalculation()
        }
        binding.tvFullAmount.setOnClickListener {
            binding.tvFullAmount.setBackgroundDrawable(resources.getDrawable(R.drawable.button_action_hover))
            binding.tv2ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv3ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv4ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv5ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv6ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tvFullAmount.setTextColor(resources.getColor(R.color.white))
            binding.tv2ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv3ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv4ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv5ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv6ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.text = "Custom"
            isSelectedCount = 1
            tipsetupGlobal(tipAmount, isSelectedCount)
            binding.tvFullAMounttxt.visibility = View.VISIBLE
            binding.tvwaysplit?.visibility = View.INVISIBLE

        }

        binding.tv2ways.setOnClickListener {
            binding.tv2ways.setBackgroundDrawable(resources.getDrawable(R.drawable.button_action_hover))
            binding.tvFullAmount.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv3ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv4ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv5ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv6ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tv2ways.setTextColor(resources.getColor(R.color.white))
            binding.tvFullAmount.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv3ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv4ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv5ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv6ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.text = "Custom"
            isSelectedCount = 2
            tipsetupGlobal(tipAmount, isSelectedCount)
            binding.tvFullAMounttxt.visibility = View.INVISIBLE
            binding.tvwaysplit?.visible()
            binding.tvwaysplit?.text = "$isSelectedCount Way Split Amount"

        }
        binding.tv3ways.setOnClickListener {
            binding.tv3ways.setBackgroundDrawable(resources.getDrawable(R.drawable.button_action_hover))
            binding.tvFullAmount.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv2ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv4ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv5ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv6ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tv3ways.setTextColor(resources.getColor(R.color.white))
            binding.tvFullAmount.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv2ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv4ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv5ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv6ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.text = "Custom"
            isSelectedCount = 3
            tipsetupGlobal(tipAmount, isSelectedCount)
            binding.tvFullAMounttxt.visibility = View.INVISIBLE
            binding.tvwaysplit?.visible()
            binding.tvwaysplit?.text = "$isSelectedCount Way Split Amount"
        }
        binding.tv4ways.setOnClickListener {
            binding.tv4ways.setBackgroundDrawable(resources.getDrawable(R.drawable.button_action_hover))
            binding.tvFullAmount.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv2ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv3ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv5ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv6ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tv4ways.setTextColor(resources.getColor(R.color.white))
            binding.tvFullAmount.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv2ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv3ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv5ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv6ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.text = "Custom"
            isSelectedCount = 4
            tipsetupGlobal(tipAmount, isSelectedCount)
            binding.tvFullAMounttxt.visibility = View.INVISIBLE
            binding.tvwaysplit?.visible()
            binding.tvwaysplit?.text = "$isSelectedCount Way Split Amount"
        }
        binding.tv5ways.setOnClickListener {
            binding.tv5ways.setBackgroundDrawable(resources.getDrawable(R.drawable.button_action_hover))
            binding.tvFullAmount.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv2ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv3ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv4ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv6ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tv5ways.setTextColor(resources.getColor(R.color.white))
            binding.tvFullAmount.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv2ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv3ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv4ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv6ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.text = "Custom"
            isSelectedCount = 5
            tipsetupGlobal(tipAmount, isSelectedCount)
            binding.tvFullAMounttxt.visibility = View.INVISIBLE
            binding.tvwaysplit?.visible()
            binding.tvwaysplit?.text = "$isSelectedCount Way Split Amount"
        }
        binding.tv6ways.setOnClickListener {
            binding.tv6ways.setBackgroundDrawable(resources.getDrawable(R.drawable.button_action_hover))
            binding.tvFullAmount.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv2ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv3ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv4ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv5ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tv6ways.setTextColor(resources.getColor(R.color.white))
            binding.tvFullAmount.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv2ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv3ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv4ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv5ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.text = "Custom"
            isSelectedCount = 6
            tipsetupGlobal(tipAmount, isSelectedCount)
            binding.tvFullAMounttxt.visibility = View.INVISIBLE
            binding.tvwaysplit?.visible()
            binding.tvwaysplit?.text = "$isSelectedCount Way Split Amount"
        }
        binding.tvCustom.setOnClickListener {
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.button_action_hover))
            binding.tvFullAmount.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv2ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv3ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv4ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv5ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tv6ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tvCustom.setTextColor(resources.getColor(R.color.white))
            binding.tvFullAmount.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv2ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv3ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv4ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv5ways.setTextColor(resources.getColor(R.color.txtColor))
            binding.tv6ways.setTextColor(resources.getColor(R.color.txtColor))
            val bundle = Bundle()
            bundle.putDouble("totalPrice", WholetotalPrice)
            bundle.putInt("splitValue", isSelectedCount)

            findNavController().navigate(R.id.action_splitFragment_to_splitdialog)
        }
    }

    private fun observeData() {
        paymentviewModel.data.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                Log.e(TAG, "receiptData: ${Gson().toJson(it.data)}")
                viewModel.redeemLoyaltyInfo = RedeemLoyaltyInfo()
                prefProvider.setValueInt("ORDER_ID", it.data.order.id)
                viewModel.updateActiveOrderFlagClear()


                isInsert = false
                isCardRev = false

                viewModel.setTipAmount(0.0)
                when {
                    paymentType == "Cash" -> {
                        Log.e("TipAmount 4:: ", tipAmount.toString())

                        val bundle = Bundle()
                        bundle.putBoolean("isDineIn", false)

                        if (remainingAmount == 0.0) {
                            if (custom_paymentAmount != 0.0) {
                                bundle.putDouble("PaidAmount", custom_paymentAmount)
                            } else {
                                bundle.putDouble("PaidAmount", paymentAmount)
                            }
                        } else {
                            bundle.putDouble("PaidAmount", remainingAmount)
                        }

                        var wholePrice =
                            String.format(
                                "%.2f",
                                prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble()
                            ).toDouble()

                        bundle.putDouble("WholetotalPrice", wholePrice)
                        var remainingValue = 0.0
                        if (custom_paymentAmount != 0.0) {
                            if (cashDiscountType == "CashDiscount") {
                                wholePrice -= cashDiscountSurcharge
                            }
                            if (custom_paymentAmount != 0.0 && isSelectedCount != 1) {
                                var splitChange = 0.0
                                splitChange = custom_paymentAmount - paymentAmount
                                bundle.putDouble(
                                    "splitChange", String.format("%.2f", splitChange).toDouble()
                                )
                                remainingValue = wholePrice - (custom_paymentAmount - splitChange)
                                bundle.putDouble(
                                    "remainingAmount",
                                    remainingValue
                                )
                            } else {
                                if (custom_paymentAmount >= wholePrice) {
                                    remainingValue =
                                        custom_paymentAmount - wholePrice
                                    bundle.putDouble(
                                        "remainingAmount",
                                        remainingValue
                                    )
                                } else {
                                    remainingValue =
                                        wholePrice - custom_paymentAmount
                                    bundle.putDouble(
                                        "remainingAmount",
                                        remainingValue
                                    )
                                }

                            }

                            prefProvider.setValue(
                                Constants.WHOLE_AMOUNT,
                                String.format("%.2f", remainingValue).toString()
                            )
                        } else {
                            remainingValue = if (cashDiscountType == "CashDiscount") {
                                wholePrice - (paymentAmount + cashDiscountSurcharge)
                            } else {
                                wholePrice - paymentAmount
                            }
                            bundle.putDouble(
                                "remainingAmount",
                                remainingValue
                            )
                            prefProvider.setValue(
                                Constants.WHOLE_AMOUNT,
                                String.format("%.2f", remainingValue)
                            )
                        }

                        if (remainingValue == 0.0 || remainingValue <= 0.0) {
                            bundle.putBoolean("isSpilt", false)
                            bundle.putBoolean("isSplitByNo", false)
                            prefProvider.setValueboolean(Constants.SPLIT_ENABLE, false)
                            bundle.putBoolean("isCustomCash", false)
                            splitAllAmounts(Constants.SUB_TOTAL, 0.0)
                            splitAllAmounts(Constants.TOTAL_DISCOUNT, 0.0)
                            splitAllAmounts(Constants.TAX_CHARGE, 0.0)
                            splitAllAmounts(Constants.SERVICE_CHARGE, 0.0)
                            splitAllAmounts(Constants.CASH_DISCOUNT_SURCHARGE, 0.0)
                            splitAllAmounts(Constants.TIP, 0.0)
                        } else {
                            if (custom_paymentAmount != 0.0 && isSelectedCount != 1) {
                                prefProvider.setValueboolean(Constants.SPLIT_ENABLE, true)
                                bundle.putBoolean("isSpilt", true)
                                bundle.putBoolean("isSplitByNo", true)
                                bundle.putBoolean("isCustomCash", true)
                                splitAllAmounts(Constants.SUB_TOTAL, subTotalPrice)
                                splitAllAmounts(Constants.TOTAL_DISCOUNT, totalDiscount)
                                splitAllAmounts(Constants.TAX_CHARGE, totalTax)
                                splitAllAmounts(Constants.SERVICE_CHARGE, totalServiceCharge)
                                if (cashDiscountType == "CashDiscount") {
                                    splitAllAmounts(
                                        Constants.CASH_DISCOUNT_SURCHARGE,
                                        cashDiscountSurcharge
                                    )
                                }

                                splitAllAmounts(Constants.TIP, 0.0)
                            } else if (custom_paymentAmount != 0.0) {
                                bundle.putBoolean("isSpilt", false)
                                prefProvider.setValueboolean(Constants.SPLIT_ENABLE, false)
                                bundle.putBoolean("isSplitByNo", false)
                                bundle.putBoolean("isCustomCash", true)
                                splitAllAmounts(Constants.SUB_TOTAL, subTotalPrice)
                                splitAllAmounts(Constants.TOTAL_DISCOUNT, totalDiscount)
                                splitAllAmounts(Constants.TAX_CHARGE, totalTax)
                                splitAllAmounts(Constants.SERVICE_CHARGE, totalServiceCharge)
                                if (cashDiscountType == "CashDiscount") {
                                    splitAllAmounts(
                                        Constants.CASH_DISCOUNT_SURCHARGE,
                                        cashDiscountSurcharge
                                    )
                                }

                                splitAllAmounts(Constants.TIP, 0.0)
                            } else {
                                bundle.putBoolean("isSpilt", true)
                                bundle.putBoolean("isSplitByNo", true)
                                bundle.putBoolean("isCustomCash", false)
                                prefProvider.setValueboolean(Constants.SPLIT_ENABLE, true)
                                splitAllAmounts(Constants.SUB_TOTAL, subTotalPrice)
                                splitAllAmounts(Constants.TOTAL_DISCOUNT, totalDiscount)
                                splitAllAmounts(Constants.TAX_CHARGE, totalTax)
                                splitAllAmounts(Constants.SERVICE_CHARGE, totalServiceCharge)
                                if (cashDiscountType == "CashDiscount") {
                                    splitAllAmounts(
                                        Constants.CASH_DISCOUNT_SURCHARGE,
                                        cashDiscountSurcharge
                                    )
                                }

                                splitAllAmounts(Constants.TIP, 0.0)
                            }

                        }


                        bundle.putInt("orderID", it.data.order.id ?: 0)
                        bundle.putParcelable("receiptData", it.data)
                        bundle.putInt("splitValue", isSelectedCount)
                        bundle.putBoolean("isSplitByAmount", false)
                        bundle.putString("paymentType", "Cash")
                        bundle.putParcelable("cartList", cartList)
                        bundle.putParcelable("redeemLoyalty", redeemLoyaltyInfo)
                        bundle.putDouble("TipAmount", tipAmount)

                        bundle.putDouble("noCashAdj", cashDiscountSurcharge)
                        bundle.putBoolean("isFromActiveOrder", isFromOpenOrder)


                        if (findNavController().currentDestination?.id == R.id.paymentBoldPosFragment) {
                            findNavController().navigate(
                                R.id.action_paymentBoldPosFragment_to_orderComplete,
                                bundle
                            )
                        }

                    }
                    paymentType == "Card" -> {
                        Log.e("TipAmount 4:: ", tipAmount.toString())

                        val bundle = Bundle()
                        bundle.putBoolean("isDineIn", false)

                        if (remainingAmount == 0.0) {
                            bundle.putDouble("PaidAmount", paymentAmount)
                        } else {
                            bundle.putDouble("PaidAmount", remainingAmount)
                        }
                        Log.d(TAG, "observeData: paidAMount value :  " + paymentAmount)

                        val wholePrice =
                            prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble()
                        Log.d(TAG, "observeData: wholePrice value :  " + wholePrice)
                        bundle.putDouble("WholetotalPrice", wholePrice)
                        Log.d(
                            TAG,
                            "observeData: cashDiscountSurcharge value :  " + cashDiscountSurcharge
                        )


                        var remainingValue = 0.0
                        remainingValue = if (cashDiscountType == "SurCharge") {
                            Log.d(
                                TAG,
                                "observeData: " + wholePrice + " " + String.format(
                                    "%.2f",
                                    paymentAmount - cashDiscountSurcharge
                                ).toDouble()
                            )
                            wholePrice - String.format(
                                "%.2f",
                                paymentAmount - cashDiscountSurcharge
                            ).toDouble()
                        } else {
                            wholePrice - paymentAmount
                        }

                        if (remainingValue <= 0.0) {
                            remainingValue = 0.0
                        }


                        prefProvider.setValue(
                            Constants.WHOLE_AMOUNT,
                            String.format("%.2f", remainingValue)
                        )


                        bundle.putDouble(
                            "remainingAmount",
                            String.format("%.2f", remainingValue).toDouble()
                        )
                        Log.d(
                            TAG,
                            "observeData: remaining value :  " + String.format(
                                "%.2f",
                                remainingValue
                            )
                        )
                        if (remainingValue == 0.0 || remainingValue <= 0.0) {
                            bundle.putBoolean("isSpilt", false)
                            bundle.putBoolean("isSplitByNo", false)
                            prefProvider.setValueboolean(Constants.SPLIT_ENABLE, false)
                            bundle.putBoolean("isCustomCash", false)
                            splitAllAmounts(Constants.SUB_TOTAL, 0.0)
                            splitAllAmounts(Constants.TOTAL_DISCOUNT, 0.0)
                            splitAllAmounts(Constants.TAX_CHARGE, 0.0)
                            splitAllAmounts(Constants.SERVICE_CHARGE, 0.0)
                            splitAllAmounts(Constants.CASH_DISCOUNT_SURCHARGE, 0.0)
                            splitAllAmounts(Constants.TIP, 0.0)
                        } else {
                            bundle.putBoolean("isSpilt", true)
                            bundle.putBoolean("isSplitByNo", true)
                            bundle.putBoolean("isCustomCash", false)
                            prefProvider.setValueboolean(Constants.SPLIT_ENABLE, true)
                            splitAllAmounts(Constants.SUB_TOTAL, subTotalPrice)
                            splitAllAmounts(Constants.TOTAL_DISCOUNT, totalDiscount)
                            splitAllAmounts(Constants.TAX_CHARGE, totalTax)
                            splitAllAmounts(Constants.SERVICE_CHARGE, totalServiceCharge)
                            splitAllAmounts(
                                Constants.CASH_DISCOUNT_SURCHARGE,
                                cashDiscountSurcharge
                            )
                            splitAllAmounts(Constants.TIP, 0.0)
                        }


                        bundle.putInt("orderID", it.data.order.id ?: 0)
                        bundle.putParcelable("receiptData", it.data)
                        bundle.putInt("splitValue", isSelectedCount)
                        bundle.putBoolean("isSplitByAmount", false)
                        bundle.putString("paymentType", "Card")
                        bundle.putParcelable("cartList", cartList)
                        bundle.putParcelable("redeemLoyalty", redeemLoyaltyInfo)
                        bundle.putDouble("TipAmount", tipAmount)

                        bundle.putDouble("noCashAdj", cashDiscountSurcharge)
                        bundle.putBoolean("isFromActiveOrder", isFromOpenOrder)

                        if (findNavController().currentDestination?.id == R.id.paymentBoldPosFragment) {
                            findNavController().navigate(
                                R.id.action_paymentBoldPosFragment_to_orderComplete,
                                bundle
                            )
                        }

                    }
                }

            }
        }
    }

    private fun observeShowProgress() {

        paymentviewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }

    }

    private fun cashPaymentWithVariation() {
        paymentAmount = String.format("%.2f", WholetotalPrice / isSelectedCount).toDouble()
        subTotalPrice = String.format("%.2f", subTotalPrice / isSelectedCount).toDouble()
        totalServiceCharge =
            String.format("%.2f", totalServiceCharge / isSelectedCount).toDouble()
        totalTax = String.format("%.2f", totalTax / isSelectedCount).toDouble()
        totalDiscount = String.format("%.2f", totalDiscount / isSelectedCount).toDouble()
        cashDiscountSurcharge =
            String.format("%.2f", cashDiscountSurcharge / isSelectedCount).toDouble()
        if (cashDiscountType == "CashDiscount") {
            paymentAmount -= cashDiscountSurcharge
        }
        makeCashPayment()
    }

    private fun paymentClick() {
        binding.llCreditCard.setOnClickListener {

            val device = prefProvider.getValueInt(Constants.MAGTEK_HARDWARE, 0)
            subTotalPrice = String.format("%.2f", subTotalPrice / isSelectedCount).toDouble()
            totalServiceCharge =
                String.format("%.2f", totalServiceCharge / isSelectedCount).toDouble()
            totalTax = String.format("%.2f", totalTax / isSelectedCount).toDouble()
            totalDiscount = String.format("%.2f", totalDiscount / isSelectedCount).toDouble()
            cashDiscountSurcharge =
                String.format("%.2f", cashDiscountSurcharge / isSelectedCount).toDouble()
            paymentAmount = String.format("%.2f", WholetotalPrice / isSelectedCount).toDouble()
            Log.d(TAG, "paymentClick: cashDiscountSurcharge " + cashDiscountSurcharge)
            Log.d(TAG, "paymentClick: paymentAmount  " + paymentAmount)
            if (cashDiscountType == "SurCharge") {
                paymentAmount =
                    String.format("%.2f", paymentAmount + cashDiscountSurcharge).toDouble()
            }
            paymentAmount += tipAmount

            if (paymentAmount != 0.0) {
                magtekModule.stopListner(false)
                if (device == 0) {
                    magtekPaymentCall()
                } else {
                    magtekProPaymentCall()
                }
            } else {
                errorDisplay("Payment Amount is zero.")
            }

            //  makePaymentCreditCard()
        }
        binding.llManualCardEntry.setOnClickListener {
            binding.frameLayoutId.visible()
            binding.relativeMain.gone()
            binding.llManualCard.visible()
            isManualCard = true

        }

        binding.tvCash0.setOnClickListener {

            custom_paymentAmount = 0.0

            paymentviewModel.totalPayAmount(
                binding.tvCash0.text.toString().replace("$", "").trim().toDouble()
            )
            paymentAmount = binding.tvCash0.text.toString().replace("$", "").trim().toDouble()
            cashPaymentWithVariation()
        }
        binding.tvCash1.setOnClickListener {

            custom_paymentAmount =
                binding.tvCash1.text.toString().replace("$", "").trim().toDouble()
            cashPaymentWithVariation()
        }
        binding.tvCash2.setOnClickListener {
            custom_paymentAmount =
                binding.tvCash2.text.toString().replace("$", "").trim().toDouble()
            cashPaymentWithVariation()
        }
        binding.tvCash3.setOnClickListener {

            custom_paymentAmount =
                binding.tvCash3.text.toString().replace("$", "").trim().toDouble()
            cashPaymentWithVariation()
        }
        binding.tvCustomAmount.setOnClickListener {

            paymentAmount = binding.tvCash0.text.toString().replace("$", "").trim().toDouble()
            val bundleVal = Bundle().apply {
                putDouble("totalprice", ((paymentAmount + tipAmount)))
            }
            findNavController().navigate(
                R.id.action_paymentBoldPosFragment_to_customAmountFragment,
                bundleVal
            )

        }
        binding.tvPaymentLink.setOnClickListener {

        }


        binding.imgBackManualCard.setOnClickListener {
            isManualCard = false
            binding.relativeMain.visible()
            binding.llManualCard.gone()
        }

        binding.txtCharge.setOnClickListener {

            MethodUtils.hideKeyboard(requireActivity())

            subTotalPrice = String.format("%.2f", subTotalPrice / isSelectedCount).toDouble()
            totalServiceCharge =
                String.format("%.2f", totalServiceCharge / isSelectedCount).toDouble()
            totalTax = String.format("%.2f", totalTax / isSelectedCount).toDouble()
            totalDiscount = String.format("%.2f", totalDiscount / isSelectedCount).toDouble()
            cashDiscountSurcharge =
                String.format("%.2f", cashDiscountSurcharge / isSelectedCount).toDouble()
            paymentAmount = String.format("%.2f", WholetotalPrice / isSelectedCount).toDouble()
            Log.d(TAG, "paymentClick: cashDiscountSurcharge " + cashDiscountSurcharge)
            Log.d(TAG, "paymentClick: paymentAmount  " + paymentAmount)
            if (cashDiscountType == "SurCharge") {
                paymentAmount =
                    String.format("%.2f", paymentAmount + cashDiscountSurcharge).toDouble()
            }
            paymentAmount += tipAmount


            cardNumber = binding.edtCardNumber.rawText.toString().trim()

            val cardExpDate = binding.edtMMYY.rawText.toString().trim()
            val cardCVV = binding.edtCVV.text.toString().trim()

            when {
                !CardValidator.validateCardNumber(cardNumber) -> {
                    errorDisplay("Please enter valid card number")
                }

                !CardValidator.validateExpiryDate(
                    cardExpDate.take(2),
                    cardExpDate.takeLast(2)
                ) -> {
                    errorDisplay("Please enter valid card expiration date")
                }
                !CardValidator.validateCVV(cardCVV, CardValidator.getCardType(cardNumber)) -> {
                    errorDisplay("Please enter valid CVV number")
                }
                else -> {
                    if (paymentAmount != 0.0) {
                        manualCardPaymentCall(
                            cardNumber,
                            cardExpDate.takeLast(2) + cardExpDate.take(2),
                            cardCVV
                        )
                    } else {
                        errorDisplay("Payment Amount is zero.")
                    }

                }
            }
        }
    }

    private fun manualCardPaymentCall(
        cardNumber: String,
        expDate: String,
        cardCVV: String
    ) {

        val jsonArray1 = magtekRequestUtils.processManualEntry(
            (paymentAmount * 100).toInt(),
            cardNumber,
            expDate,
            cardCVV
        )

        networkCall(jsonArray1, 3)

    }

    private fun errorDisplay(msg: String) {

        AlertUtils.showCustomAlert(requireContext(), msg)
    }

    private fun loadManualCardEntryFragment(fragment: Fragment) {
        val fm: FragmentManager = requireActivity().supportFragmentManager
        fm.beginTransaction().replace(R.id.frameLayoutId, fragment).commit()
    }

    fun getDataFromPref() {
        redeemLoyaltyInfo = viewModel.redeemLoyaltyInfo
        if (prefProvider.getValue(Constants.WHOLE_AMOUNT, "").isEmpty() || prefProvider.getValue(
                Constants.WHOLE_AMOUNT,
                ""
            ) == "0.0"
        ) {
            WholetotalPrice = viewModel.totalPrice
            prefProvider.setValue(
                Constants.WHOLE_AMOUNT,
                String.format("%.2f", viewModel.totalPrice)
            )
        } else {
            WholetotalPrice = prefProvider.getValue(Constants.WHOLE_AMOUNT, "").toDouble()
        }

        if (prefProvider.getValue(Constants.SUB_TOTAL, "").isEmpty() || prefProvider.getValue(
                Constants.SUB_TOTAL,
                ""
            ) == "0.0"
        ) {
            subTotalPrice = viewModel.subTotalPrice
            prefProvider.setValue(
                Constants.SUB_TOTAL,
                String.format("%.2f", viewModel.subTotalPrice)
            )
        } else {
            subTotalPrice = prefProvider.getValue(Constants.SUB_TOTAL, "").toDouble()
        }

        if (prefProvider.getValue(Constants.TAX_CHARGE, "").isEmpty() || prefProvider.getValue(
                Constants.TAX_CHARGE,
                ""
            ) == "0.0"
        ) {
            totalTax = viewModel.totalTax
            prefProvider.setValue(Constants.TAX_CHARGE, String.format("%.2f", viewModel.totalTax))
        } else {
            totalTax = prefProvider.getValue(Constants.TAX_CHARGE, "").toDouble()
        }


        if (prefProvider.getValue(Constants.SERVICE_CHARGE, "").isEmpty() || prefProvider.getValue(
                Constants.SERVICE_CHARGE,
                ""
            ) == "0.0"
        ) {
            totalServiceCharge = viewModel.totalServiceCharge
            prefProvider.setValue(
                Constants.SERVICE_CHARGE,
                String.format("%.2f", viewModel.totalServiceCharge)
            )
        } else {
            totalServiceCharge = prefProvider.getValue(Constants.SERVICE_CHARGE, "").toDouble()
        }


        if (prefProvider.getValue(Constants.TOTAL_DISCOUNT, "").isEmpty() || prefProvider.getValue(
                Constants.TOTAL_DISCOUNT,
                ""
            ) == "0.0"
        ) {
            totalDiscount = viewModel.totalDiscount
            prefProvider.setValue(
                Constants.TOTAL_DISCOUNT,
                String.format("%.2f", viewModel.totalDiscount)
            )
        } else {
            totalDiscount = prefProvider.getValue(Constants.TOTAL_DISCOUNT, "").toDouble()
        }


        if (prefProvider.getValue(Constants.TIP, "").isEmpty() || prefProvider.getValue(
                Constants.TIP,
                ""
            ) == "0.0"
        ) {
            tipAmount = viewModel.tip
            prefProvider.setValue(Constants.TIP, String.format("%.2f", viewModel.tip))
        } else {
            tipAmount = prefProvider.getValue(Constants.TIP, "").toDouble()
        }

        if (prefProvider.getValue(Constants.TIP, "").isEmpty() || prefProvider.getValue(
                Constants.TIP,
                ""
            ) == "0.0"
        ) {
            tipAmount = viewModel.tip
            prefProvider.setValue(Constants.TIP, String.format("%.2f", viewModel.tip))
        } else {
            tipAmount = prefProvider.getValue(Constants.TIP, "").toDouble()
        }
        if (prefProvider.getValue(Constants.CASH_DISCOUNT_SURCHARGE, "")
                .isEmpty() || prefProvider.getValue(
                Constants.CASH_DISCOUNT_SURCHARGE,
                ""
            ) == "0.0"
        ) {
            cashDiscountSurcharge = viewModel.cashdiscountAmount
            prefProvider.setValue(
                Constants.CASH_DISCOUNT_SURCHARGE,
                String.format("%.2f", viewModel.cashdiscountAmount)
            )
        } else {
            cashDiscountSurcharge =
                prefProvider.getValue(Constants.CASH_DISCOUNT_SURCHARGE, "").toDouble()
        }
        cashDiscountType = viewModel.cashDiscountType




        cartList = viewModel.cartModel
        Log.e("ORDER_TYPE", prefProvider.getValue(Constants.ORDER_TYPE, Constants.TAKEOUT))

        viewModel.ordertypelist.forEach {
            if (prefProvider.getValue(Constants.ORDER_TYPE, Constants.TAKEOUT) == it.orderType) {
                paymentviewModel.setOrderTypeId(it.id)
            }
        }
        paymentviewModel.saveActualValue(
            viewModel.totalPrice,
            viewModel.subTotalPrice,
            viewModel.totalTax,
            viewModel.totalServiceCharge,
            viewModel.tip,
            viewModel.totalDiscount,
            viewModel.cashdiscountAmount,
            viewModel.totalPrice
        )

        setupPaymentScreen(isSelectedCount)


        MethodUtils.setPriceTextView(
            binding.tvAmount,
            getCalCashDiscWithAmount(
                prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble(), true
            )
        )
        val formatterdate = SimpleDateFormat("yyyy-MM-dd")
        val formattertime = SimpleDateFormat("hh:mm a")
        val date = Date()
        future_delivery_date = formatterdate.format(date)
        future_delivery_time = formattertime.format(date)

    }

    private fun setupPaymentScreen(isSelectCount: Int) {
        MethodUtils.getCashPaymentOptionList(
            getCalCashDiscWithAmount(WholetotalPrice, true) / isSelectCount,
            binding.tvCash1,
            binding.tvCash2,
            binding.tvCash3
        )
        MethodUtils.setPriceTextView(
            binding.tvCash,
            getCalCashDiscWithAmount(WholetotalPrice, true) / isSelectCount
        )
        MethodUtils.setPriceTextView(
            binding.tvCash0,
            getCalCashDiscWithAmount(WholetotalPrice, true) / isSelectCount
        )
        binding.tvCash.text = "Cash (" + binding.tvCash.text + ")"
        MethodUtils.setPriceTextView(
            binding.tvCard,
            getCalCashDiscWithAmount(WholetotalPrice, false) / isSelectCount
        )
        binding.tvCard.text = "Card (" + binding.tvCard.text + ")"
    }

    private fun tipAmountCalculation() {
        if (tipAmount == 0.00) {
            binding.tvsplittip?.gone()
            binding.tvtipcard?.gone()
            binding.tvtipcash?.gone()
            MethodUtils.setPriceTextView(
                binding.tvCash,
                getCalCashDiscWithAmount(WholetotalPrice, true) / isSelectedCount
            )
            MethodUtils.setPriceTextView(
                binding.tvCash0,
                getCalCashDiscWithAmount(WholetotalPrice, true) / isSelectedCount
            )
            MethodUtils.setPriceTextView(
                binding.tvCard,
                getCalCashDiscWithAmount(WholetotalPrice, false) / isSelectedCount
            )
            binding.tvCash.text = "Cash (" + binding.tvCash.text + ")"
            binding.tvCard.text = "Card (" + binding.tvCard.text + ")"
            MethodUtils.setPriceTextView(
                binding.tvAmount,
                getCalCashDiscWithAmount(
                    prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble(), true
                )
            )
        } else {
            MethodUtils.setPriceTextView(
                binding.tvCash,
                (getCalCashDiscWithAmount(WholetotalPrice, true) / isSelectedCount) + tipAmount
            )
            MethodUtils.setPriceTextView(
                binding.tvCash0,
                (getCalCashDiscWithAmount(WholetotalPrice, true) / isSelectedCount) + tipAmount
            )
            MethodUtils.setPriceTextView(
                binding.tvCard,
                (getCalCashDiscWithAmount(WholetotalPrice, false) / isSelectedCount) + tipAmount
            )
            binding.tvCash.text =
                "Cash (" + binding.tvCash.text + ")"
            binding.tvtipcash?.visible()
            binding.tvtipcash?.text = "(" + MethodUtils.roundOffAmount(tipAmount) + " Tip Added)"
            binding.tvCard.text =
                "Card (" + binding.tvCard.text + ")"
            binding.tvtipcard?.visible()
            binding.tvtipcard?.text = "(" + MethodUtils.roundOffAmount(tipAmount) + " Tip Added)"
            MethodUtils.getCashPaymentOptionList(
                (getCalCashDiscWithAmount(WholetotalPrice, true) / isSelectedCount) + tipAmount,
                binding.tvCash1,
                binding.tvCash2,
                binding.tvCash3
            )
            MethodUtils.setPriceTextView(
                binding.tvAmount,
                (getCalCashDiscWithAmount(
                    prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble(), true
                ) / isSelectedCount) + tipAmount
            )
            binding.tvAmount.text =
                binding.tvAmount.text.toString()
            binding.tvsplittip?.visible()
            binding.tvsplittip?.text = "(" + MethodUtils.roundOffAmount(tipAmount) + " Tip Added)"
        }
    }

    private fun splitAllAmounts(TAG: String, amount: Double) {
        val remainingValue = prefProvider.getValue(TAG, "").toDouble() - amount
        prefProvider.setValue(TAG, String.format("%.2f", remainingValue))
        Log.d(TAG, "splitAllAmounts: " + prefProvider.getValue(TAG, "").toDouble())
    }

    private fun getCalCashDiscWithAmount(totalprice: Double, isCash: Boolean): Double {
        return if (isCash) {
            if (cashDiscountType == "CashDiscount") {
                totalprice - cashDiscountSurcharge
            } else {
                totalprice
            }
        } else {
            if (cashDiscountType == "SurCharge") {
                totalprice + cashDiscountSurcharge
            } else {
                totalprice
            }
        }
        return totalprice
    }

    private fun tipsetupGlobal(tipAmount: Double, isSelectCount: Int) {
        if (tipAmount == 0.0) {
            binding.tvsplittip?.gone()
            MethodUtils.setPriceTextView(
                binding.tvAmount,
                getCalCashDiscWithAmount(
                    prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble(), true
                ) / isSelectCount
            )
        } else {
            MethodUtils.setPriceTextView(
                binding.tvAmount,
                (getCalCashDiscWithAmount(
                    prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble(), true
                ) / isSelectCount) + tipAmount
            )
            binding.tvAmount.text =
                binding.tvAmount.text.toString()
            binding.tvsplittip?.visible()
            binding.tvsplittip?.text = "(" + MethodUtils.roundOffAmount(tipAmount) + " Tip Added)"
        }
    }

    private fun setupTabDesign() {
        binding.linearTab1.setOnClickListener {
            PaymentBoldPosFragment.newInstance().addTipHideShow(false)
            isSelectedCount = 1
            tipsetupGlobal(tipAmount, isSelectedCount)
            loadPaymentLayout()
            tipAmountCalculation()
        }
        binding.linearTab2.setOnClickListener {

            if (tipAmount != 0.0 && viewModel.tipTransactionAmount != 0.0) {
                AlertUtils.showCustomAlertWithListenerWithOKCancel(
                    requireContext(),
                    "If you are going to do split payment then existing tip will be removed."
                ) { _, _ ->
                    PaymentBoldPosFragment.newInstance().addTipHideShow(true)
                    tipAmount = 0.0
                    viewModel.setTipAmount(0.0)
                    loadSplitLayout()
                    binding.tvFullAmount.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
                    binding.tv2ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
                    binding.tv3ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
                    binding.tv4ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
                    binding.tv5ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
                    binding.tv6ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
                    binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
                    binding.tvFullAmount.setTextColor(resources.getColor(R.color.txtColor))
                    binding.tv2ways.setTextColor(resources.getColor(R.color.txtColor))
                    binding.tv3ways.setTextColor(resources.getColor(R.color.txtColor))
                    binding.tv4ways.setTextColor(resources.getColor(R.color.txtColor))
                    binding.tv5ways.setTextColor(resources.getColor(R.color.txtColor))
                    binding.tv6ways.setTextColor(resources.getColor(R.color.txtColor))
                    binding.tvCustom.setTextColor(resources.getColor(R.color.txtColor))
                    binding.tvCustom.text = "Custom"
                    isSelectedCount = 1
                    tipsetupGlobal(tipAmount, isSelectedCount)
                    binding.tvFullAMounttxt.visibility = View.VISIBLE
                    binding.tvwaysplit?.invisible()
                }
            } else {
                loadSplitLayout()
                binding.tvFullAmount.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
                binding.tv2ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
                binding.tv3ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
                binding.tv4ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
                binding.tv5ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
                binding.tv6ways.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
                binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
                binding.tvFullAmount.setTextColor(resources.getColor(R.color.txtColor))
                binding.tv2ways.setTextColor(resources.getColor(R.color.txtColor))
                binding.tv3ways.setTextColor(resources.getColor(R.color.txtColor))
                binding.tv4ways.setTextColor(resources.getColor(R.color.txtColor))
                binding.tv5ways.setTextColor(resources.getColor(R.color.txtColor))
                binding.tv6ways.setTextColor(resources.getColor(R.color.txtColor))
                binding.tvCustom.setTextColor(resources.getColor(R.color.txtColor))
                binding.tvCustom.text = "Custom"
                isSelectedCount = 1
                tipsetupGlobal(tipAmount, isSelectedCount)
                binding.tvFullAMounttxt.visibility = View.VISIBLE
                binding.tvwaysplit?.invisible()
            }

        }
    }

    private fun loadSplitLayout() {
        binding.tab2.setTextColor(resources.getColor(R.color.txt_color_blue))
        binding.view2.setBackgroundColor(resources.getColor(R.color.txt_color_blue))
        binding.tab1.setTextColor(resources.getColor(R.color.white))
        binding.view1.setBackgroundColor(resources.getColor(R.color.backgroundColor))
        isSplitScreen = true
        isPaymentScreen = false
        binding.paymentLinearLayout.visibility = View.GONE
        binding.splitLinearLayout.visibility = View.VISIBLE
    }

    private fun loadPaymentLayout() {
        binding.tab1.setTextColor(resources.getColor(R.color.txt_color_blue))
        binding.view1.setBackgroundColor(resources.getColor(R.color.txt_color_blue))
        binding.tab2.setTextColor(resources.getColor(R.color.white))
        binding.view2.setBackgroundColor(resources.getColor(R.color.backgroundColor))
        isPaymentScreen = true
        isSplitScreen = false
        binding.paymentLinearLayout.visibility = View.VISIBLE
        binding.splitLinearLayout.visibility = View.GONE
    }

    private fun makePaymentCreditCard() {
        paymentType = "Card"
        Log.e(TAG, "cartList:  ${Gson().toJson(cartList)}")
        Log.e(TAG, "cartListcartItems:  ${Gson().toJson(cartItems)}")
        if (orderId != -1 && orderId != 0) {
            paymentviewModel.updateOrder(
                true,
                orderId,
                paymentId,
                paymentOfflineId,
                orderOfflineId
            )
        } else {
            paymentviewModel.updateOrder(false, null, null, "", "")
        }
        paymentviewModel.saveOrder(false)
        val myRequest = cartList?.let {
            paymentviewModel.createOrderRequestForCard(
                it,
                subTotalPrice,
                paymentAmount,
                totalServiceCharge,
                totalTax,
                prefProvider.getValue(Constants.ORDER_TYPE, Constants.TAKEOUT),
                future_delivery_date,
                future_delivery_time,
                true,
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
        Log.e(TAG, "myRequestOriginal ${Gson().toJson(myRequest)}")
        if (myRequest != null) {
            paymentviewModel.totalPayAmount(paymentAmount)
            paymentAttributesRequest(myRequest)
        }
    }

    private fun makeCashPayment() {
        paymentType = "Cash"
        Log.e(TAG, "makeCashPayorderId  ${orderId}")
        Log.e(TAG, "makeCashPrefOrderId  ${prefProvider.getValueInt("ORDER_ID", -1)}")

        if (orderId != -1 && orderId != 0) {
            paymentviewModel.updateOrder(
                true,
                orderId,
                paymentId,
                paymentOfflineId,
                orderOfflineId
            )
        } else {
            paymentviewModel.updateOrder(false, null, null, "", "")
        }


        paymentviewModel.saveOrder(false)
        Log.d("yash", "makeCashPayment: total Price : " + paymentAmount)
        Log.d("yash", "makeCashPayment: sub_total   : " + subTotalPrice)
        Log.d("yash", "makeCashPayment: totaltax    : " + totalTax)
        Log.d("yash", "makeCashPayment: total disc  : " + totalDiscount)
        Log.d("yash", "makeCashPayment: total serv  : " + totalServiceCharge)
        val myRequest = cartList?.let {
            paymentviewModel.createOrderRequest(
                it,
                subTotalPrice,
                paymentAmount,
                totalServiceCharge,
                totalTax,
                prefProvider.getValue(Constants.ORDER_TYPE, Constants.TAKEOUT),
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
        Log.e(TAG, "myRequestOriginal ${Gson().toJson(myRequest)}")
        Log.e("ORDER TYPE 1", prefProvider.getValue(Constants.ORDER_TYPE, Constants.TAKEOUT))
        if (myRequest != null) {
            if (custom_paymentAmount != 0.0) {
                paymentviewModel.totalPayAmount(custom_paymentAmount)
            }
            paymentAttributesRequest(myRequest)
        }
    }

    private fun paymentAttributesRequest(myRequest: OrderRequestModel) {
        val orderId = prefProvider.getValueInt("ORDER_ID", -1)
        Log.e(TAG, "orderIdmyRequestOriginal ${orderId}")
        if (orderId == -1) {
            myRequest.completed_all_payments = isSelectedCount <= 1
            paymentviewModel.submit(myRequest)
        } else {
            val paymentReq = myRequest.order.paymentAttributes
            if (paymentReq != null) {
                paymentReq.order_id = orderId
            }

            // total amount - (hal pay amoutn + alredy pay )
            val aa = SpitByOrderRequestModel(
                orderId, isSelectedCount <= 1,
                SpitByOrderPaymentModel(listOf(paymentReq) as List<PaymentAttributes>)
            )

            paymentviewModel.splitByOrder(aa, false)

        }
    }

    private fun magtekPaymentCall() {

        paymentviewModel.cardReaderList().observe(viewLifecycleOwner) {

            if (it.status == Status.SUCCESS) {

                if (it.data == null) {
                    AlertUtils.showCustomAlert(requireContext(), "Please connect device")
                } else {

                    if (magtekModule.m_scra?.isDeviceConnected == true) {

                        magtekModule.startTransactionWithLED()
                    } else {
                        ProgressUtils.showProgressDialog(requireActivity())

                        magtekModule.openDevice(it.data.mcAddress)
                    }


                }

            }
        }

    }


    private fun testDevice(m_Text: String) {
        if (magtekModule.m_scra?.isDeviceConnected == true) {

            magtekModule.startTransactionWithLED()
        } else {
            ProgressUtils.showProgressDialog(requireActivity())
            magtekModule.setupInit()
            magtekModule.openDeviceTest(m_Text)
        }
    }

    override fun startScanning() {
    }

    override fun processStart(message: String, isDismiss: Boolean) {


        isInsert = false
        isCardRev = false

        ProgressUtils.setCallback(this)
        if (isDismiss) {

            ProgressUtils.dismissProgressDialog()


            if (requestCancel) {

                if (message.equals("timeout", true)) {
                    AlertUtils.showCustomAlert(
                        requireContext(), "Timeout"
                    )
                } else {
                    AlertUtils.showCustomAlert(
                        requireContext(), message
                    )
                }
            } else {
                if (message.equals("timeout", true)) {
                    AlertUtils.showCustomAlert(
                        requireContext(), "Timeout"
                    )
                } else {
                    AlertUtils.showCustomAlert(
                        requireContext(), message
                    )
                }
            }


        } else {
            ProgressUtils.showProgressDialog(message, requireActivity())
        }

    }

    override fun stopScanning() {
    }

    override fun onConnect(deviceState: MTConnectionState) {

        runOnUiThread {
            when (deviceState) {
                MTConnectionState.Connected -> {
                    ProgressUtils.dismissProgressDialog()
                    magtekModule.startTransactionWithLED()

                }
                MTConnectionState.Disconnected -> {
                    magtekModule.setLED(false)
                    magtekModule.closeDevice()

                    ProgressUtils.dismissProgressDialog()

                    //  AlertUtils.showCustomAlert(requireContext(), "Connection error")
                }
                else -> {
                }
            }
        }
    }

    override fun onDeviceResponse(response: String) {

    }

    override fun onDeviceList(bluetoothDevice: BluetoothDevice) {

    }

    override fun OnCardDataReceived(imtCardData: IMTCardData) {

        magtekModule.stopListner(true)
        isCardRev = true



        if (magtekModule.m_scra?.track2?.isEmpty() == true) {
            magtekModule.stopListner(false)
            isCardRev = false

            AlertUtils.showCustomAlert(requireContext(), "Please swipe the card properly.")

        } else {

            ProgressUtils.dismissProgressDialog()

            val jsonArray1 = magtekModule.m_scra?.let {
                magtekRequestUtils.processCardSwipe(
                    (paymentAmount * 100).toInt(),
                    magtekModule.m_scra!!.ksn,
                    magtekModule.m_scra!!.magnePrint,
                    magtekModule.m_scra!!.magnePrintStatus,
                    it.track2
                )
            }

            if (!isInsert)
                networkCall(jsonArray1, 1)
        }


    }

    private fun networkCall(jsonArray1: JsonArray?, i: Int) {

        ProgressUtils.showProgressDialog(requireActivity())

        var call: Call<PaymentResponse>? = null
        when (i) {
            1 -> {
                call = jsonArray1?.let { apiModule1.getRetrofit1().processCardSwipe(it) }
            }
            2 -> {
                call = jsonArray1?.let { apiModule1.getRetrofit1().processData(it) }
            }
            3 -> {
                call = jsonArray1?.let { apiModule1.getRetrofit1().processManualEntry(it) }
            }
        }

        call!!.enqueue(object : Callback<PaymentResponse> {

            override fun onResponse(
                call: Call<PaymentResponse>,
                response: Response<PaymentResponse>
            ) {
                ProgressUtils.dismissProgressDialog()

                if (response.isSuccessful) {
                    Log.e("onResponse", Gson().toJson(response.body()))
                    if (response.body() != null && response.body()!![0].transactionOutput != null) {

                        if (isDynamo())
                            magtekModule.setLED(false)


                        if (response.body()!![0].transactionOutput?.isTransactionApproved == true) {
                            if (isDynamo())
                                magtekModule.closeDevice()
                            paymentviewModel.setMagensaResponse(
                                Gson().toJson(response.body()!![0]),
                                if (i == 3) cardNumber else ""
                            )

                            paymentAmount -= tipAmount
                            makePaymentCreditCard()

                            isInsert = true
                            isCardRev = true
                            isError = false
                        } else {
                            AlertUtils.showCustomAlert(
                                requireContext(),
                                response.body()!![0].transactionOutput?.transactionMessage
                            )
                            isInsert = false
                            isCardRev = false
                            isError = true
                            magtekModule.stopListner(false)
                        }


                    } else {
                        if (response.body()!![0].mPPGv4WSFault != null)
                            AlertUtils.showCustomAlert(
                                requireContext(),
                                response.body()!![0].mPPGv4WSFault?.faultCode + "\n" +
                                        response.body()!![0].mPPGv4WSFault?.faultReason
                            )
                        isInsert = false
                        isCardRev = false
                        isError = true
                        magtekModule.stopListner(false)
                    }
                }
            }

            override fun onFailure(call: Call<PaymentResponse>, t: Throwable) {

                ProgressUtils.dismissProgressDialog()

                AlertUtils.showCustomAlert(requireContext(), t.message)

                isInsert = false
                isCardRev = false
                isError = true
                magtekModule.stopListner(false)
            }
        })
    }


    override fun OnARQCReceived(data: ByteArray) {


        magtekModule.stopListner(true)

        isInsert = true

        ProgressUtils.dismissProgressDialog()

        val jsonArray1 = magtekRequestUtils.processData(
            (paymentAmount * 100).toInt(),
            TLVParser.getHexString(data),
            Constants.AUTHORIZE
        )

        if (!isCardRev)
            networkCall(jsonArray1, 2)

    }

    override fun onItemClickListener(position: Int) {

        if (!isDynamo()) {
            mSessionManager.cancelTransaction()
        } else {
            requestCancel = true
            magtekModule.cancelTransaction()
        }
    }

    fun processEvent(eventType: EventType, data: IData) {
        Log.d(
            "processEvent", ": eventType=$eventType"
        )

        runOnUiThread {
            when (eventType) {
                EventType.ConnectionState -> {
                    when (ConnectionStateBuilder.GetValue(data.StringValue())) {
                        ConnectionState.Connected -> {
                            Log.e("", "[CONNECTED]")

                            // ProgressUtils.dismissProgressDialog()

                            startTransaction()
                        }
                        ConnectionState.Disconnected -> {
                            Log.e("", "[DISCONNECTED]")
                            ProgressUtils.dismissProgressDialog()
                            AlertUtils.showCustomAlert(requireContext(), "DISCONNECTED")
                        }
                        ConnectionState.Disconnecting -> {
                            Log.e("", "[DISCONNECTING]")
                        }
                        ConnectionState.Connecting -> {
                            Log.e("", "[CONNECTING]")

                        }
                        else -> ""
                    }
                }
                EventType.TransactionResult -> {

//                ProgressUtils.dismissProgressDialog()

                    Log.e("TransactionResult", "TransactionResult called")

                    dismissDialog()

                    val jsonArray1 = magtekRequestUtils.processData(
                        (paymentAmount * 100).toInt(),
                        MTParser.getHexString(data.ByteArray()),
                        Constants.AUTHORIZE
                    )

                    networkCall(jsonArray1, 2)


                }

                EventType.TransactionStatus -> {
                    when (TransactionStatusBuilder.GetStatusCode(data.StringValue())) {

                        TransactionStatus.TimedOut -> {
                            AlertUtils.showCustomAlert(requireContext(), "TRANSACTION TIMED OUT")
                            //  ProgressUtils.dismissProgressDialog()
                            dismissDialog()
                        }
                        TransactionStatus.HostCancelled -> {
                            AlertUtils.showCustomAlert(requireContext(), "HOST CANCELLED")
                            // ProgressUtils.dismissProgressDialog()
                            dismissDialog()
                        }
                        TransactionStatus.TransactionCancelled -> {
                            AlertUtils.showCustomAlert(requireContext(), "TRANSACTION CANCELLED")
                            // ProgressUtils.dismissProgressDialog()
                            dismissDialog()
                        }
                        TransactionStatus.TransactionError -> {
                            AlertUtils.showCustomAlert(requireContext(), "TRANSACTION ERROR")
                            //  ProgressUtils.dismissProgressDialog()
                            dismissDialog()
                        }
                        else -> ""
                    }
                }
            }
        }

    }

    private fun dismissDialog() {
        ProgressUtils.dismissProgressDialog()
    }

    private fun magtekProPaymentCall() {

        ProgressUtils.showProgressDialog(requireActivity())
        ProgressUtils.setCallback(this)


        Log.e("mSessionManager", mSessionManager.isConnected.toString())
        if (mSessionManager.isConnected) {
            startTransaction()
        } else {

            val deviceList: List<IDevice> = CoreAPI.getDeviceList(context, DeviceType.MMS, this)
            setupList(deviceList)


//            if (mSessionManager.device != null) {
//                mSessionManager.connectDevice()
//            } else {
//                ProgressUtils.dismissProgressDialog()
//                dismissDialog()
//                AlertUtils.showCustomAlert(requireActivity(), "Please connect device")
//            }
        }
    }

    private fun setupList(deviceList: List<IDevice>) {

        if (deviceList.isNotEmpty()) {

            val device = deviceList[0]
            mSessionManager.device = device
            mSessionManager.connectDevice()
        }

    }


    private fun startTransaction() {


        val paymentMethods = TransactionBuilder.GetPaymentMethods(true, true, true, false)

        val transaction = Transaction(
            60,
            paymentMethods,
            paymentAmount.toString(),
            "",
            true,
            true,
            0
        )

        mSessionManager.startTransaction(transaction, getSignature = false, fallback = false)


    }

    private fun isDynamo(): Boolean {
        val device = prefProvider.getValueInt(Constants.MAGTEK_HARDWARE, 0)
        return device == 0
    }

    override fun OnDeviceList(mlist: MutableList<IDevice>?) {
        if (mlist != null) {
            setupList(mlist)
        }
    }
}