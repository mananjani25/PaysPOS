package com.android.pos.ui.fragments.checkout

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.CartModel
import com.android.pos.data.entities.RedeemLoyaltyInfo
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.requestModel.PaymentAttributes
import com.android.pos.data.model.requestModel.SpitByOrderPaymentModel
import com.android.pos.data.model.requestModel.SpitByOrderRequestModel
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.ORDER_TYPE
import com.android.pos.data.remote.Constants.TAKEOUT
import com.android.pos.data.remote.Constants.WHOLE_AMOUNT
import com.android.pos.databinding.FragmentPayFullAmountBinding
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
import com.android.pos.utils.callback.ItemListner
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
import javax.inject.Inject

@AndroidEntryPoint
class PayFullAmountFragment(val bundle: Bundle?) : Fragment(), ItemListner, magtekCallback,
    DeleteOptionCallback {
    private var orderOfflineId: String = ""
    private var paymentOfflineId: String = ""
    private var paymentId: Int = -1
    private var orderId: Int = -1
    private var requestCancel: Boolean = false
    private lateinit var binding: FragmentPayFullAmountBinding
    private val paymentviewModel by activityViewModels<PaymentViewModel>()
    private val viewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val TAG = "DashboardCategoryBold"
    var frameLayoutId = 0
    var paymentType = "Cash"
    var cashDiscountSurcharge = 0.0
    var cardActualAmount = 0.0
    var llRoot = 0
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
    var tipID = null
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (prefProvider.getValue(WHOLE_AMOUNT, "").isEmpty()) {
            WholetotalPrice = viewModel.totalPrice
            prefProvider.setValue(WHOLE_AMOUNT, String.format("%.2f", viewModel.totalPrice))
        } else {
            WholetotalPrice = prefProvider.getValue(WHOLE_AMOUNT, "").toDouble()
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
                    prefProvider.getValue(Constants.CASH_DISCOUNT_SURCHARGE, "").toDouble()
            }
        } else {
            cashDiscountSurcharge = 0.0
            prefProvider.getValue(
                cashDiscountSurcharge.toString(),
                String.format("%.2f", cashDiscountSurcharge)
            )
        }

        getCartData()
        observeShowProgress()

//
//        if (isNextPayment) {
//            splitAllAMounts(splitValue)
//        }
    }

    private fun splitAllAMounts(splitValue: Int) {
        subTotalPrice /= splitValue
        totalDiscount /= splitValue
        totalTax /= splitValue
        totalServiceCharge /= splitValue
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentPayFullAmountBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        magtekModule.setCallback(this)
        onClick()
        frameLayoutId = bundle?.getInt("frameLayoutId")!!
        llRoot = bundle.getInt("llRoot")
        orderId = bundle.getInt("orderId")

        Log.e("orderId :: ", orderId.toString())

        if (orderId != -1 && orderId != 0) {
            paymentId = bundle.getInt("paymentId")
            paymentOfflineId = bundle.getString("paymentOfflineId").toString()
            orderOfflineId = bundle.getString("orderOfflineId").toString()
        }
//
//        var navController = findNavController()
//        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<Bundle>("data")
//            ?.observe(viewLifecycleOwner) {
//                isNextPayment = it.getBoolean("isNextPayment")
//                remainingAmount =
//                    String.format("%.2f", it.getDouble("remainingAmount", 0.0)).toDouble()
//                splitValue = it.getInt("splitvalue", -1)
//                splitAllAMounts(splitValue)
//            }

        return binding.root
    }


    private fun getCartData() {


        cartList = viewModel.cartModel
        Log.e("ORDER_TYPE", prefProvider.getValue(ORDER_TYPE, TAKEOUT))

        viewModel.ordertypelist.forEach {
            if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == it.orderType) {
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

        MethodUtils.getCashPaymentOptionList(
            WholetotalPrice,
            binding.tvCash1,
            binding.tvCash2,
            binding.tvCash3
        )
        MethodUtils.setPriceTextView(binding.tvCash, WholetotalPrice)
        binding.tvCash.text = "Cash (" + binding.tvCash.text + ")"
        MethodUtils.setPriceTextView(binding.tvCard, WholetotalPrice)
        binding.tvCard.text = "Card (" + binding.tvCard.text + ")"

    }


    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: ")
    }

    private fun onClick() {
        binding.llManualCardEntry.setOnClickListener {
            val viewPager: FrameLayout = activity?.findViewById(frameLayoutId) as FrameLayout
            viewPager.visibility = View.VISIBLE

            val llRoot: LinearLayout = activity?.findViewById(llRoot) as LinearLayout
            llRoot.visibility = View.GONE

            loadManualCardEntryFragment(ManualCardEntryFragment())
        }


        binding.llPaycash.setOnClickListener {
            paymentAmount = WholetotalPrice
            observeData()
            makeCashPayment()

        }

        binding.tvCash1.setOnClickListener {

        }
        binding.llCreditCard.setOnClickListener {
            binding.llCreditCard.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
            binding.llManualCardEntry.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash1.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash2.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash3.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvPaymentLink.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tvCreditCard.setTextColor(resources.getColor(R.color.white))
            binding.tvManualCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash1.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash2.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash3.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvPaymentLink.setTextColor(resources.getColor(R.color.txtColor))

            val device = prefProvider.getValueInt(Constants.MAGTEK_HARDWARE, 0)
            observeData()
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
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvPaymentLink.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tvManualCard.setTextColor(resources.getColor(R.color.white))
            binding.tvCreditCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash1.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash2.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash3.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvPaymentLink.setTextColor(resources.getColor(R.color.txtColor))
        }
        binding.tvCash1.setOnClickListener {
            binding.tvCash1.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
            binding.llCreditCard.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.llManualCardEntry.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash2.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash3.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvPaymentLink.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tvCash1.setTextColor(resources.getColor(R.color.white))
            binding.tvCreditCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvManualCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash2.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash3.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvPaymentLink.setTextColor(resources.getColor(R.color.txtColor))
        }
        binding.tvCash2.setOnClickListener {
            binding.tvCash2.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
            binding.llCreditCard.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.llManualCardEntry.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash1.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash3.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvPaymentLink.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tvCash2.setTextColor(resources.getColor(R.color.white))
            binding.tvCreditCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvManualCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash1.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash3.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvPaymentLink.setTextColor(resources.getColor(R.color.txtColor))
        }
        binding.tvCash3.setOnClickListener {
            binding.tvCash3.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
            binding.llCreditCard.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.llManualCardEntry.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash1.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash2.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvPaymentLink.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tvCash3.setTextColor(resources.getColor(R.color.white))
            binding.tvCreditCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvManualCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash1.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash2.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvPaymentLink.setTextColor(resources.getColor(R.color.txtColor))
        }
        binding.tvCustom.setOnClickListener {

            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.button_selected))
            binding.llCreditCard.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.llManualCardEntry.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash1.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash2.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvCash3.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            binding.tvPaymentLink.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tvCustom.setTextColor(resources.getColor(R.color.white))
            binding.tvCreditCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvManualCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash1.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash2.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash3.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvPaymentLink.setTextColor(resources.getColor(R.color.txtColor))

            val bundleVal = Bundle().apply {

                if (splitAfterAmount != 0.0) {
                    putDouble("totalprice", (splitAfterAmount + tipAmount))
                } else {
                    putDouble("totalprice", ((WholetotalPrice + tipAmount)))
                }
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
            binding.tvCustom.setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))

            binding.tvPaymentLink.setTextColor(resources.getColor(R.color.white))
            binding.tvCreditCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvManualCard.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash1.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash2.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCash3.setTextColor(resources.getColor(R.color.txtColor))
            binding.tvCustom.setTextColor(resources.getColor(R.color.txtColor))
        }
    }

    private fun loadManualCardEntryFragment(fragment: Fragment) {
        val fm: FragmentManager = requireActivity().supportFragmentManager
        fm.beginTransaction().replace(frameLayoutId, fragment).commit()
    }


    override fun onItemSelected(item: TbItem) {

    }

    override fun onCancelItemSelected() {

    }

    override fun onCategorySelected(item: TbItem) {

    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop: ")
    }

    override fun onDetach() {
        super.onDetach()
        Log.d(TAG, "onDetach: ")
    }

    private fun observeData() {

        paymentviewModel.data.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                when {
                    paymentType == "Cash" -> {
                        Log.e("TipAmount 4:: ", tipAmount.toString())

                        val bundle = Bundle()
                        bundle.putBoolean("isDineIn", false)

                        if (remainingAmount == 0.0) {
                            bundle.putDouble("PaidAmount", WholetotalPrice)
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
                        bundle.putBoolean("isSplitByNo", false)
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

                        prefProvider.setValueInt("ORDER_ID", -1)


                    }
                    paymentType == "Card" -> {
                        Log.e("TipAmount 4:: ", tipAmount.toString())

                        val bundle = Bundle()
                        bundle.putBoolean("isDineIn", false)

                        if (remainingAmount == 0.0) {
                            bundle.putDouble("PaidAmount", WholetotalPrice)
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
                        bundle.putBoolean("isSplitByNo", false)
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

                        prefProvider.setValueInt("ORDER_ID", -1)


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

    private fun makePaymentCreditCard() {
        paymentType = "Card"
        paymentAmount = WholetotalPrice
        Log.e(TAG, "cartList:  ${Gson().toJson(cartList)}")
        Log.e(TAG, "cartListcartItems:  ${Gson().toJson(cartItems)}")
        val myRequest = cartList?.let {
            Log.d("yash", "makeCashPayment: total Price : $paymentAmount")
            Log.d("yash", "makeCashPayment: sub_total   : $subTotalPrice")
            Log.d("yash", "makeCashPayment: totaltax    : $totalTax")
            Log.d("yash", "makeCashPayment: total disc  : $totalDiscount")
            Log.d("yash", "makeCashPayment: total serv  : $totalServiceCharge")
            paymentviewModel.createOrderRequestForCard(
                it,
                subTotalPrice,
                paymentAmount,
                totalServiceCharge,
                totalTax,
                prefProvider.getValue(ORDER_TYPE, TAKEOUT),
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
            paymentviewModel.totalPayAmount(WholetotalPrice)
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

        Log.e("ORDER TYPE", prefProvider.getValue(ORDER_TYPE, TAKEOUT))
        val myRequest = cartList?.let {

            paymentviewModel.createOrderRequest(
                it,
                subTotalPrice,
                paymentAmount,
                totalServiceCharge,
                totalTax,
                prefProvider.getValue(ORDER_TYPE, TAKEOUT),
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

        Log.e("ORDER TYPE 1", prefProvider.getValue(ORDER_TYPE, TAKEOUT))
        if (myRequest != null) {
            paymentviewModel.totalPayAmount(WholetotalPrice)
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
                (WholetotalPrice * 100).toInt(),
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
            (WholetotalPrice * 100).toInt(),
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
            MethodUtils.roundOffAmountString(paymentAmount),
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