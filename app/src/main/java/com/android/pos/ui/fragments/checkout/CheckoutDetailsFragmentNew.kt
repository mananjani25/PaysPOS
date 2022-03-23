package com.android.pos.ui.fragments.checkout

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
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
import com.android.pos.ui.fragments.payment.PaymentViewModel
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.TLVParser
import com.android.pos.utils.callback.DeleteOptionCallback
import com.android.pos.utils.callback.magtekCallback
import com.android.pos.utils.extensions.runOnUiThread
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
class CheckoutDetailsFragmentNew : Fragment(), magtekCallback,
    DeleteOptionCallback {
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
        paymentonClick()
        splitonClick()
        observeShowProgress()
        observeData()
        callback()
    }

    @SuppressLint("SetTextI18n")
    private fun callback() {
        requireActivity().supportFragmentManager.setFragmentResultListener(
            "request_key_tips",
            viewLifecycleOwner
        ) { requestKey: String, bundle: Bundle ->
            tipAmount = bundle.getDouble("tipAmount")
            tipID = bundle.getInt("tipId")
            tipAmountCalculation()
        }
        requireActivity().supportFragmentManager.setFragmentResultListener(
            "request_key_split",
            viewLifecycleOwner
        ) { requestKey: String, bundle: Bundle ->

            isSelectedCount = bundle.getInt("split")
            binding.tvCustom.text = "Custom ($isSelectedCount Ways)"
            tipsetupGlobal(tipAmount, isSelectedCount)
        }
        requireActivity().supportFragmentManager.setFragmentResultListener(
            "request_for_customAmount",
            viewLifecycleOwner
        ) { requestKey: String, bundle: Bundle ->
            val amounnt = bundle.getDouble("amount")
            val totalPrice = bundle.getDouble("totalAmount")
            MethodUtils.setPriceTextView(binding.tvCustomAmount, amounnt)
            custom_paymentAmount = amounnt
            binding.tvCustomAmount.text = "Custom (" + binding.tvCustomAmount.text.toString() + ")"
        }


    }

    private fun splitonClick() {

        binding.linearNextSplit.setOnClickListener {
            loadPaymentLayout()
            setupPaymentScreen(isSelectedCount)
        }
        binding.tvFullAmount.setOnClickListener {
            binding.tvFullAmount.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
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
        }

        binding.tv2ways.setOnClickListener {
            binding.tv2ways.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
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

        }
        binding.tv3ways.setOnClickListener {
            binding.tv3ways.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
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
        }
        binding.tv4ways.setOnClickListener {
            binding.tv4ways.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
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
        }
        binding.tv5ways.setOnClickListener {
            binding.tv5ways.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
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
        }
        binding.tv6ways.setOnClickListener {
            binding.tv6ways.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
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
        }
        binding.tvCustom.setOnClickListener {
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
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
            var bundle = Bundle()
            bundle.putDouble("totalPrice", WholetotalPrice)
            bundle.putInt("splitValue", isSelectedCount)

            findNavController().navigate(R.id.action_splitFragment_to_splitdialog)
        }
    }

    private fun observeData() {
        paymentviewModel.data.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                prefProvider.setValueInt("ORDER_ID", it.data.order.id)
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
                            remainingValue = custom_paymentAmount - paymentAmount
                            bundle.putDouble(
                                "remainingAmount",
                                remainingValue
                            )
                            prefProvider.setValue(
                                Constants.WHOLE_AMOUNT,
                                String.format("%.2f", (wholePrice - paymentAmount)).toString()
                            )
                        } else {
                            remainingValue = wholePrice - paymentAmount
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
                            prefProvider.setValueboolean(Constants.SPLIT_ENABLE,false)
                            bundle.putBoolean("isCustomCash", false)
                            splitAllAmounts(Constants.SUB_TOTAL, 0.0)
                            splitAllAmounts(Constants.TOTAL_DISCOUNT, 0.0)
                            splitAllAmounts(Constants.TAX_CHARGE, 0.0)
                            splitAllAmounts(Constants.SERVICE_CHARGE, 0.0)
                            splitAllAmounts(Constants.CASH_DISCOUNT_SURCHARGE, 0.0)
                            splitAllAmounts(Constants.TIP, 0.0)
                        } else {
                            if (custom_paymentAmount != 0.0 && isSelectedCount != 1) {
                                prefProvider.setValueboolean(Constants.SPLIT_ENABLE,true)
                                bundle.putBoolean("isSpilt", true)
                                bundle.putBoolean("isSplitByNo", true)
                                bundle.putBoolean("isCustomCash", true)
                                splitAllAmounts(Constants.SUB_TOTAL, subTotalPrice)
                                splitAllAmounts(Constants.TOTAL_DISCOUNT, totalDiscount)
                                splitAllAmounts(Constants.TAX_CHARGE, totalTax)
                                splitAllAmounts(Constants.SERVICE_CHARGE, totalServiceCharge)
                                splitAllAmounts(Constants.CASH_DISCOUNT_SURCHARGE, 0.0)
                                splitAllAmounts(Constants.TIP, 0.0)
                            } else if (custom_paymentAmount != 0.0) {
                                bundle.putBoolean("isSpilt", false)
                                prefProvider.setValueboolean(Constants.SPLIT_ENABLE,false)
                                bundle.putBoolean("isSplitByNo", false)
                                bundle.putBoolean("isCustomCash", true)
                                splitAllAmounts(Constants.SUB_TOTAL, subTotalPrice)
                                splitAllAmounts(Constants.TOTAL_DISCOUNT, totalDiscount)
                                splitAllAmounts(Constants.TAX_CHARGE, totalTax)
                                splitAllAmounts(Constants.SERVICE_CHARGE, totalServiceCharge)
                                splitAllAmounts(Constants.CASH_DISCOUNT_SURCHARGE, 0.0)
                                splitAllAmounts(Constants.TIP, 0.0)
                            } else {
                                bundle.putBoolean("isSpilt", true)
                                bundle.putBoolean("isSplitByNo", true)
                                bundle.putBoolean("isCustomCash", false)
                                prefProvider.setValueboolean(Constants.SPLIT_ENABLE,true)
                                splitAllAmounts(Constants.SUB_TOTAL, subTotalPrice)
                                splitAllAmounts(Constants.TOTAL_DISCOUNT, totalDiscount)
                                splitAllAmounts(Constants.TAX_CHARGE, totalTax)
                                splitAllAmounts(Constants.SERVICE_CHARGE, totalServiceCharge)
                                splitAllAmounts(Constants.CASH_DISCOUNT_SURCHARGE, 0.0)
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
                        bundle.putBoolean("isFromActiveOrder", false)


                        findNavController().navigate(
                            R.id.action_paymentBoldPosFragment_to_orderComplete,
                            bundle
                        )

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

                        var wholePrice =
                            prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble()
                        bundle.putDouble("WholetotalPrice", wholePrice)
                        var remainingValue = 0.0
                        remainingValue = wholePrice - paymentAmount
                        bundle.putDouble(
                            "remainingAmount",
                            remainingValue
                        )
                        prefProvider.setValue(Constants.WHOLE_AMOUNT, remainingValue.toString())

                        if (remainingValue == 0.0) {
                            bundle.putBoolean("isSpilt", false)
                            bundle.putBoolean("isSplitByNo", false)
                            prefProvider.setValueboolean(Constants.SPLIT_ENABLE,false)
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
                            prefProvider.setValueboolean(Constants.SPLIT_ENABLE,true)
                            splitAllAmounts(Constants.SUB_TOTAL, subTotalPrice)
                            splitAllAmounts(Constants.TOTAL_DISCOUNT, totalDiscount)
                            splitAllAmounts(Constants.TAX_CHARGE, totalTax)
                            splitAllAmounts(Constants.SERVICE_CHARGE, totalServiceCharge)
                            splitAllAmounts(Constants.CASH_DISCOUNT_SURCHARGE, 0.0)
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
                        bundle.putBoolean("isFromActiveOrder", false)


                        findNavController().navigate(
                            R.id.action_paymentBoldPosFragment_to_orderComplete,
                            bundle
                        )

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

    private fun paymentonClick() {

        binding.llPaycash.setOnClickListener {
            paymentAmount = String.format("%.2f", WholetotalPrice / isSelectedCount).toDouble()
            subTotalPrice = String.format("%.2f", subTotalPrice / isSelectedCount).toDouble()
            totalServiceCharge =
                String.format("%.2f", totalServiceCharge / isSelectedCount).toDouble()
            totalTax = String.format("%.2f", totalTax / isSelectedCount).toDouble()
            totalDiscount = String.format("%.2f", totalDiscount / isSelectedCount).toDouble()
            cashDiscountSurcharge =
                String.format("%.2f", cashDiscountSurcharge / isSelectedCount).toDouble()

            makeCashPayment()

        }
        binding.llCreditCard.setOnClickListener {
            binding.llCreditCard.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
            binding.llManualCardEntry.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash1.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash2.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash3.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCustomAmount.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvPaymentLink.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tvCreditCard.setTextColor(resources.getColor(R.color.white))
            binding.tvManualCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash1.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash2.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash3.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustomAmount.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvPaymentLink.setTextColor(resources.getColor(R.color.txtColor))

            val device = prefProvider.getValueInt(Constants.MAGTEK_HARDWARE, 0)
            paymentAmount = String.format("%.2f", WholetotalPrice / isSelectedCount).toDouble()
            subTotalPrice = String.format("%.2f", subTotalPrice / isSelectedCount).toDouble()
            totalServiceCharge =
                String.format("%.2f", totalServiceCharge / isSelectedCount).toDouble()
            totalTax = String.format("%.2f", totalTax / isSelectedCount).toDouble()
            totalDiscount = String.format("%.2f", totalDiscount / isSelectedCount).toDouble()
            cashDiscountSurcharge =
                String.format("%.2f", cashDiscountSurcharge / isSelectedCount).toDouble()
            if (device == 0) {
                magtekPaymentCall()
            } else {
                magtekProPaymentCall()
            }
        }
        binding.llManualCardEntry.setOnClickListener {
            binding.llManualCardEntry.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
            binding.llCreditCard.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash1.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash2.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash3.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCustomAmount.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvPaymentLink.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tvManualCard.setTextColor(resources.getColor(R.color.white))
            binding.tvCreditCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash1.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash2.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash3.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustomAmount.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvPaymentLink.setTextColor(resources.getColor(R.color.txtColor))
            binding.frameLayoutId.visibility = View.VISIBLE
            binding.relativeMain.visibility = View.GONE
            loadManualCardEntryFragment(ManualCardEntryFragment())
        }
        binding.tvCash1.setOnClickListener {
            binding.tvCash1.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
            binding.llCreditCard.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.llManualCardEntry.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash2.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash3.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCustomAmount.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvPaymentLink.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tvCash1.setTextColor(resources.getColor(R.color.white))
            binding.tvCreditCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvManualCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash2.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash3.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustomAmount.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvPaymentLink.setTextColor(resources.getColor(R.color.txtColor))
            custom_paymentAmount =
                binding.tvCash1.text.toString().replace("$", "").trim().toDouble()
        }
        binding.tvCash2.setOnClickListener {
            binding.tvCash2.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
            binding.llCreditCard.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.llManualCardEntry.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash1.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash3.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCustomAmount.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvPaymentLink.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tvCash2.setTextColor(resources.getColor(R.color.white))
            binding.tvCreditCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvManualCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash1.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash3.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustomAmount.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvPaymentLink.setTextColor(resources.getColor(R.color.txtColor))
            custom_paymentAmount =
                binding.tvCash2.text.toString().replace("$", "").trim().toDouble()
        }
        binding.tvCash3.setOnClickListener {
            binding.tvCash3.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
            binding.llCreditCard.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.llManualCardEntry.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash1.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash2.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCustomAmount.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvPaymentLink.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tvCash3.setTextColor(resources.getColor(R.color.white))
            binding.tvCreditCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvManualCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash1.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash2.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustomAmount.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvPaymentLink.setTextColor(resources.getColor(R.color.txtColor))
            custom_paymentAmount =
                binding.tvCash3.text.toString().replace("$", "").trim().toDouble()
        }
        binding.tvCustomAmount.setOnClickListener {

            binding.tvCustomAmount.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
            binding.llCreditCard.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.llManualCardEntry.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash1.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash2.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash3.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvPaymentLink.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tvCustomAmount.setTextColor(resources.getColor(R.color.white))
            binding.tvCreditCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvManualCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash1.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash2.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash3.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvPaymentLink.setTextColor(resources.getColor(R.color.txtColor))

            val bundleVal = Bundle().apply {
                putDouble("totalprice", ((WholetotalPrice + tipAmount)))
            }
            findNavController().navigate(
                R.id.action_paymentBoldPosFragment_to_customAmountFragment,
                bundleVal
            )

        }
        binding.tvPaymentLink.setOnClickListener {
            binding.tvPaymentLink.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
            binding.llCreditCard.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.llManualCardEntry.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash1.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash2.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash3.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCustomAmount.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tvPaymentLink.setTextColor(resources.getColor(R.color.white))
            binding.tvCreditCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvManualCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash1.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash2.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash3.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustomAmount.setTextColor(resources.getColor(R.color.txtColor))
        }
    }

    private fun loadManualCardEntryFragment(fragment: Fragment) {
        val fm: FragmentManager = requireActivity().supportFragmentManager
        fm.beginTransaction().replace(R.id.frameLayoutId, fragment).commit()
    }

    fun getDataFromPref() {
        if (prefProvider.getValue(Constants.WHOLE_AMOUNT, "").isEmpty()) {
            WholetotalPrice = viewModel.totalPrice
            prefProvider.setValue(
                Constants.WHOLE_AMOUNT,
                String.format("%.2f", viewModel.totalPrice)
            )
        } else {
            WholetotalPrice = prefProvider.getValue(Constants.WHOLE_AMOUNT, "").toDouble()
        }

        if (prefProvider.getValue(Constants.SUB_TOTAL, "").isEmpty()) {
            subTotalPrice = viewModel.subTotalPrice
            prefProvider.setValue(
                Constants.SUB_TOTAL,
                String.format("%.2f", viewModel.subTotalPrice)
            )
        } else {
            subTotalPrice = prefProvider.getValue(Constants.SUB_TOTAL, "").toDouble()
        }

        if (prefProvider.getValue(Constants.TAX_CHARGE, "").isEmpty()) {
            totalTax = viewModel.totalTax
            prefProvider.setValue(Constants.TAX_CHARGE, String.format("%.2f", viewModel.totalTax))
        } else {
            totalTax = prefProvider.getValue(Constants.TAX_CHARGE, "").toDouble()
        }


        if (prefProvider.getValue(Constants.SERVICE_CHARGE, "").isEmpty()) {
            totalServiceCharge = viewModel.totalServiceCharge
            prefProvider.setValue(
                Constants.SERVICE_CHARGE,
                String.format("%.2f", viewModel.totalServiceCharge)
            )
        } else {
            totalServiceCharge = prefProvider.getValue(Constants.SERVICE_CHARGE, "").toDouble()
        }


        if (prefProvider.getValue(Constants.TOTAL_DISCOUNT, "").isEmpty()) {
            totalDiscount = viewModel.totalDiscount
            prefProvider.setValue(
                Constants.TOTAL_DISCOUNT,
                String.format("%.2f", viewModel.totalDiscount)
            )
        } else {
            totalDiscount = prefProvider.getValue(Constants.TOTAL_DISCOUNT, "").toDouble()
        }


        if (prefProvider.getValue(Constants.TIP, "").isEmpty()) {
            tipAmount = viewModel.tip
            prefProvider.setValue(Constants.TIP, String.format("%.2f", viewModel.tip))
        } else {
            tipAmount = prefProvider.getValue(Constants.TIP, "").toDouble()
        }


        if (MethodUtils.isEnableCashDiscount(requireContext())) {
            if (prefProvider.getValue(Constants.CASH_DISCOUNT_SURCHARGE, "").isEmpty()) {
                cashDiscountSurcharge = MethodUtils.calculateCashDiscount(
                    WholetotalPrice,
                    prefProvider,
                    requireContext()
                )
                prefProvider.setValue(
                    Constants.CASH_DISCOUNT_SURCHARGE,
                    String.format("%.2f", cashDiscountSurcharge)
                )
            } else {
                cashDiscountSurcharge =
                    prefProvider.getValue(Constants.CASH_DISCOUNT_SURCHARGE, "0.0").toDouble()
            }
        } else {
            cashDiscountSurcharge = 0.0
            prefProvider.setValue(
                Constants.CASH_DISCOUNT_SURCHARGE,
                String.format("%.2f", cashDiscountSurcharge)
            )
        }

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
            MethodUtils.calculateCashDiscount(viewModel.totalPrice, prefProvider, requireContext()),
            viewModel.totalPrice
        )

        setupPaymentScreen(isSelectedCount)
        MethodUtils.setPriceTextView(
            binding.tvAmount,
            prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble()
        )
        val formatterdate = SimpleDateFormat("yyyy-MM-dd")
        val formattertime = SimpleDateFormat("hh:mm a")
        val date = Date()
        future_delivery_date = formatterdate.format(date)
        future_delivery_time = formattertime.format(date)

    }

    fun setupPaymentScreen(isSelectCount: Int) {
        MethodUtils.getCashPaymentOptionList(
            WholetotalPrice / isSelectCount,
            binding.tvCash1,
            binding.tvCash2,
            binding.tvCash3
        )
        MethodUtils.setPriceTextView(binding.tvCash, WholetotalPrice / isSelectCount)
        binding.tvCash.text = "Cash (" + binding.tvCash.text + ")"
        MethodUtils.setPriceTextView(binding.tvCard, WholetotalPrice / isSelectCount)
        binding.tvCard.text = "Card (" + binding.tvCard.text + ")"
    }

    private fun tipAmountCalculation() {
        if (tipAmount == 0.00) {
            MethodUtils.setPriceTextView(binding.tvCash, WholetotalPrice / isSelectedCount)
            binding.tvCash.text = "Cash (" + binding.tvCash.text + ")"
            MethodUtils.setPriceTextView(binding.tvCard, WholetotalPrice / isSelectedCount)
            binding.tvCard.text = "Card (" + binding.tvCard.text + ")"
            MethodUtils.setPriceTextView(
                binding.tvAmount,
                prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble()
            )
        } else {
            MethodUtils.setPriceTextView(
                binding.tvCash,
                (WholetotalPrice / isSelectedCount) + tipAmount
            )
            binding.tvCash.text =
                "Cash (" + binding.tvCash.text + ") (" + MethodUtils.roundOffAmount(tipAmount) + " Tip Added)"
            MethodUtils.setPriceTextView(
                binding.tvCard,
                (WholetotalPrice / isSelectedCount) + tipAmount
            )
            binding.tvCard.text =
                "Card (" + binding.tvCard.text + ") (" + MethodUtils.roundOffAmount(tipAmount) + " Tip Added)"
            MethodUtils.getCashPaymentOptionList(
                (WholetotalPrice / isSelectedCount) + tipAmount,
                binding.tvCash1,
                binding.tvCash2,
                binding.tvCash3
            )
            MethodUtils.setPriceTextView(
                binding.tvAmount,
                (prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0")
                    .toDouble() / isSelectedCount) + tipAmount
            )
            binding.tvAmount.text =
                binding.tvAmount.text.toString() + " (" + tipAmount + " Tip Added)"
        }
    }

    public fun splitAllAmounts(TAG: String, amount: Double) {
        var remainingValue = prefProvider.getValue(TAG, "").toDouble() - amount
        prefProvider.setValue(TAG, String.format("%.2f", remainingValue))
    }

    fun tipsetupGlobal(tipAmount: Double, isSelectCount: Int) {
        if (tipAmount == 0.0) {
            MethodUtils.setPriceTextView(
                binding.tvAmount,
                prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble() / isSelectCount
            )
        } else {
            MethodUtils.setPriceTextView(
                binding.tvAmount,
                (prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0")
                    .toDouble() / isSelectCount) + tipAmount
            )
            binding.tvAmount.text =
                binding.tvAmount.text.toString() + " (" + MethodUtils.roundOffAmount(tipAmount) + " Tip Added)"
        }
    }

    private fun setupTabDesign() {
        binding.linearTab1.setOnClickListener {
            loadPaymentLayout()
        }
        binding.linearTab2.setOnClickListener {
            loadSplitLayout()
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
                paymentType, cashDiscountType,
                tipID
            )
        }
        Log.e(TAG, "myRequestOriginal ${Gson().toJson(myRequest)}")
        if (myRequest != null) {
            paymentviewModel.totalPayAmount(viewModel.totalPrice)
            paymentAttributesRequest(myRequest)
        }
    }

    private fun makeCashPayment() {
        paymentType = "Cash"

        if (orderId != -1 && orderId != 0)
            paymentviewModel.updateOrder(
                true,
                orderId,
                paymentId,
                paymentOfflineId,
                orderOfflineId
            )


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
            } else {
                paymentviewModel.totalPayAmount(viewModel.totalPrice)
            }
            paymentAttributesRequest(myRequest)
        }
    }

    fun paymentAttributesRequest(myRequest: OrderRequestModel) {
        val orderId = prefProvider.getValueInt("ORDER_ID", -1)
        Log.e(TAG, "orderIdmyRequestOriginal ${orderId}")
        if (orderId == -1) {
            paymentviewModel.submit(myRequest)
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

            paymentviewModel.splitByOrder(aa, false)

        }
    }

    private fun magtekPaymentCall() {

        paymentviewModel.cardReaderList().observe(viewLifecycleOwner) {

            if (it.status == Status.SUCCESS) {

                if (it.data == null) {

//                    if (magtekModule.m_scra?.isDeviceConnected == true) {
//                        magtekModule.startTransactionWithLED()
//                    } else
//                        showdialog()

                    AlertUtils.showCustomAlert(requireContext(), "Please connect device")

                } else {

                    if (magtekModule.m_scra?.isDeviceConnected == true) {

                        magtekModule.startTransactionWithLED()
                    } else {
                        ProgressUtils.showProgressDialog(requireActivity())
                        magtekModule.setupInit()
                        magtekModule.openDevice(it.data.mcAddress)
                    }


                }

            }
        }

    }

    private fun showdialog() {
        val builder: android.app.AlertDialog.Builder =
            android.app.AlertDialog.Builder(requireContext())
        builder.setTitle(" Card Reader Not Found")
        builder.setMessage("Please enter mac address")

        val input = EditText(requireContext())
        input.hint = "14:42:FC:0B:FB:FF"
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)

        builder.setPositiveButton("OK") { dialog, which ->
            val m_Text = input.text.toString().trim()

            if (m_Text.isEmpty())
                return@setPositiveButton

            testDevice(m_Text)
        }
        builder.setNegativeButton(
            "Cancel"
        ) { dialog, which -> dialog.cancel() }

        builder.show()
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

        ProgressUtils.setCallback(this)
        if (isDismiss) {

            ProgressUtils.dismissProgressDialog()

            if (requestCancel) {
                AlertUtils.showCustomAlert(
                    requireContext(), message
                )
            } else {
                AlertUtils.showCustomAlert(
                    requireContext(), message
                )
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

                    AlertUtils.showCustomAlert(requireContext(), "Connection error")
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

        networkCall(jsonArray1, 1)

    }

    private fun networkCall(jsonArray1: JsonArray?, i: Int) {

        ProgressUtils.showProgressDialog(requireActivity())

        var call: Call<PaymentResponse>? = null
        if (i == 1) {
            call = jsonArray1?.let { apiModule1.getRetrofit1().processCardSwipe(it) }
        } else if (i == 2) {
            call = jsonArray1?.let { apiModule1.getRetrofit1().processData(it) }
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

                        if (response.body()!![0].transactionOutput?.isTransactionApproved == true) {
                            if (isDynamo())
                                magtekModule.stopListner()
                            paymentviewModel.setMagensaResponse(Gson().toJson(response.body()!![0]))
                            makePaymentCreditCard()
                        } else {
                            AlertUtils.showCustomAlert(
                                requireContext(),
                                response.body()!![0].transactionOutput?.transactionMessage
                            )
                        }

                        if (isDynamo())
                            magtekModule.setLED(false)

                    } else {
                        if (response.body()!![0].mPPGv4WSFault != null)
                            AlertUtils.showCustomAlert(
                                requireContext(),
                                response.body()!![0].mPPGv4WSFault?.faultCode + "\n" +
                                        response.body()!![0].mPPGv4WSFault?.faultReason
                            )
                    }
                }
            }

            override fun onFailure(call: Call<PaymentResponse>, t: Throwable) {

                ProgressUtils.dismissProgressDialog()
            }
        })
    }


    override fun OnARQCReceived(data: ByteArray) {

        ProgressUtils.dismissProgressDialog()

        val jsonArray1 = magtekRequestUtils.processData(
            (paymentAmount * 100).toInt(),
            TLVParser.getHexString(data),
            Constants.SALE
        )

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
                        Constants.SALE
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
            if (mSessionManager.device != null) {
                mSessionManager.connectDevice()
            } else {
                ProgressUtils.dismissProgressDialog()
                dismissDialog()
                AlertUtils.showCustomAlert(requireActivity(), "Please connect device")
            }
        }
    }

    private fun startTransaction() {


        val paymentMethods = TransactionBuilder.GetPaymentMethods(true, true, true, false)

        val transaction = Transaction(
            60,
            paymentMethods,
            "1.0",
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
}