package com.pays.pos.ui.fragments.checkout

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.DialogInterface
import android.os.Bundle
import android.os.Message
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.data.entities.CartModel
import com.pays.pos.data.entities.RedeemLoyaltyInfo
import com.pays.pos.data.entities.TbItem
import com.pays.pos.data.model.CheckOutDineInDataModel
import com.pays.pos.data.model.requestModel.*
import com.pays.pos.data.model.responseModel.CreateOrderResponse
import com.pays.pos.data.model.responseModel.GuestPaymentAttributes
import com.pays.pos.data.remote.ApiService
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.DINE_IN_ADAPTER_LIST
import com.pays.pos.data.remote.Constants.DINE_IN_GUEST_PAYMENT_DATA
import com.pays.pos.data.remote.Constants.OPTION_TYPE
import com.pays.pos.data.remote.Constants.PRINT_DATA_DINE_IN
import com.pays.pos.databinding.FragmentCheckoutDetailsNewBinding
import com.pays.pos.di.ApiModule1
import com.pays.pos.di.MagtekModule
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.pays.pos.ui.fragments.dashboard.bolddashboard.CustomDisplay
import com.pays.pos.ui.fragments.dinein.DineInOrderTableViewModel
import com.pays.pos.ui.fragments.loginscreen.PasscodeViewModel
import com.pays.pos.ui.fragments.magtek.MagtekRequestUtils
import com.pays.pos.ui.fragments.magtek.MagtekViewModel
import com.pays.pos.ui.fragments.magtek.PaymentResponse
import com.pays.pos.ui.fragments.magtekPro.MTParser
import com.pays.pos.ui.fragments.magtekPro.SessionManager
import com.pays.pos.ui.fragments.payment.PaymentBoldPosFragment
import com.pays.pos.ui.fragments.payment.PaymentViewModel
import com.pays.pos.ui.fragments.settings.tip.TipListViewModel
import com.pays.pos.utils.*
import com.pays.pos.utils.callback.DeleteOptionCallback
import com.pays.pos.utils.callback.magtekCallback
import com.pays.pos.utils.extensions.*
import com.pays.pos.utils.paxUtils.AppThreadPool
import com.pays.pos.utils.paxUtils.POSLinkCreatorWrapper
import com.pays.pos.utils.paxUtils.SettingINI
import com.pays.pos.utils.statusUtils.Status
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.magtek.mobile.android.mtlib.IMTCardData
import com.magtek.mobile.android.mtlib.MTConnectionState
import com.magtek.mobile.android.mtusdk.*
import com.pax.poslink.PaymentRequest
import com.pax.poslink.PosLink
import com.pax.poslink.ProcessTransResult
import com.pays.pos.ui.fragments.dashboard.bolddashboard.CustomDisplayDineIn
import com.pays.pos.ui.fragments.dineInNew.DineInOrderTableViewModelPays
import com.pays.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class CheckoutDineInFragmentNew(val dineInDataModel: CheckOutDineInDataModel) : Fragment(),
    magtekCallback,
    DeleteOptionCallback, IDeviceListCallback {

    private lateinit var presentation: CustomDisplayDineIn
    private val dashboardViewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val passcodeViewModel by activityViewModels<PasscodeViewModel>()
    private val dineInViewModel by viewModels<DineInOrderTableViewModelPays>()
    private val magtekProViewModel by viewModels<MagtekViewModel>()

    private var cardCVV: String = ""
    private var cardExpDate: String = ""
    private var cardNumber: String = ""
    private var isLastPayment: Boolean = false
    private var isManualCard: Boolean = false
    private lateinit var binding: FragmentCheckoutDetailsNewBinding
    private val TAG = "DashboardCategoryBold"

    var serviceChargeAppliedList: ArrayList<OrderServiceChargesAttribute> = arrayListOf()

    private var requestCancel: Boolean = false
    private var orderId: Int? = null
    private var orderOfflineId: String = ""
    private var paymentOfflineId: String = ""
    var isSelectedCount = 1
    private val paymentviewModel by activityViewModels<PaymentViewModel>()
    private val dineinOrderVieweModel by activityViewModels<DineInOrderTableViewModel>()
    private val dineInPaymentViewModel by viewModels<CheckoutDineInPaymentViewModel>()
    private val viewModel by activityViewModels<DashBoardCategoryViewModel>()
    var listtextview: ArrayList<AppCompatTextView> = arrayListOf()
    var paymentType = "Cash"
    private var guestRequestModel: GuestPaymentRequest? = null
    private var guestId: Int? = null
    var cashDiscountSurcharge = 0.0
    var cardActualAmount = 0.0
    private var paymentId: Int = -1
    private var isPaymentScreen = true
    private var isSplitScreen = false
    private var isGuestPay = false
    private var orderIdNew: Int? = 0

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
    // PAX variables
    private lateinit var mPaymentRequest: PaymentRequest
    private var posLink: PosLink = PosLink()
    var CARDBIN = ""
    var cardLastDigits = ""
    var CardName = ""
    var EDCType = ""
    var GlobalUID = ""
    var RefNumber = ""
    var ECRRefNumber = ""
    var PAXtoken = ""
    var ExtData = ""

    private var cartList: CartModel? = null
    private var splitModel: DineInOrderPayment? = null

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


    companion object {
        fun newInstacne(
            modelDineIn: CheckOutDineInDataModel
        ): CheckoutDineInFragmentNew {
            val frag = CheckoutDineInFragmentNew(modelDineIn)
            val bundle = Bundle()
            bundle.putParcelable("dineInModel", modelDineIn)

            frag.arguments = bundle
            LogUtil.logE(TAG, "modelDineInmodelDineIn:  ${Gson().toJson(modelDineIn)}")
            return frag
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentCheckoutDetailsNewBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        navigateOnPaymentSuccess()

        val device = prefProvider.getValueInt(Constants.MAGTEK_HARDWARE, 0)

        if (device == 0) {
            magtekModule.setupInit()
            magtekModule.setCallback(this)
        } else {
            mSessionManager.setDineInFragment(this)

        }

        getCustomerDisplay(requireContext())?.let { display ->
            presentation = CustomDisplayDineIn(
                display,
                requireContext(),
                viewLifecycleOwner,
                dashboardViewModel,
                passcodeViewModel,
                dineInViewModel
            )
        }
        if (prefProvider.getValueboolean(Constants.IS_PAX_CONNECTED, false)) {
            binding.llManualCardEntry.visibility = View.GONE
        }
        initPOSLink()
        getMerchantDataObserver()

        return binding.root
    }

    // to init poslink for pax payment
    private fun initPOSLink() {
        POSLinkCreatorWrapper.createSync(
            requireContext(),
            object : AppThreadPool.FinishInMainThreadCallback<PosLink?> {
                override fun onFinish(result: PosLink?) {
                    posLink = result!!
                    Log.d("initPOSLink: ","onFinish")
                }
            })
    }

    // To get merchant data of pax device
    private fun getMerchantDataObserver() {
        magtekProViewModel.merchantData.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { response ->
                Log.d("merchantData: ","merchantData observe")
                val resultCode = response.resultCode
                val status = response.resultTxt
                val mID = response.VarValue
                prefProvider.setValueboolean(Constants.IS_PAX_CONNECTED, true)
                prefProvider.setValue(
                    Constants.MERCHANT_ID,
                    mID
                )
                CoroutineScope(Dispatchers.Main).launch {
                    ProgressUtils.dismissProgressDialog()
                    makePaxPaymentRequest()
//                    AlertUtils.showCustomAlert(requireContext(), "Merchant $mID is connected successfully")
                }
                Log.d("Merchant Details: ", mID + " " + resultCode + "  " + status)
            }
        }
    }

    private val tipListViewModel by activityViewModels<TipListViewModel>()

    override fun onResume() {
        super.onResume()
        if (this::presentation.isInitialized) {
            presentation.show()
            presentation.onDisplayChanged()
            //presentation.showSurcharge(true)
            //presentation.showWouldYouLikeToAddTipScreen(tipListViewModel,WholetotalPrice)
        }
    }

    @Inject
    lateinit var apiService: ApiService

    override fun onPause() {
        super.onPause()
        if (this::presentation.isInitialized) {
            presentation.show()
            presentation.onLogOutOrClockOutWithApiService(apiService)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        LogUtil.logE(TAG, "dineInDataModel:  ${Gson().toJson(dineInDataModel)}")
        orderId = dineInDataModel?.orderId
        isGuestPay = dineInDataModel?.isFromGuest ?: false
        isLastPayment = dineInDataModel?.isLastPayment ?: false
        guestRequestModel = dineInDataModel?.guestPaymentReq
        splitModel = dineInDataModel?.splitModel
        serviceChargeAppliedList = dineInDataModel?.servicChargeAppliedlist!!
        LogUtil.logE("orderId :: ", orderId.toString())
        if (orderId != null) {
            paymentId = arguments?.getInt("paymentId")!!
            paymentOfflineId = arguments?.getString("paymentOfflineId").toString()
            orderOfflineId = arguments?.getString("orderOfflineId").toString()
        }

        getDataFromPref()
        setupTabDesign()
        paymentClick()
        splitClick()
        observeShowProgress()
        observeData()
        callback()
        setUpManualCardFocusChanged()
        observeQueueCreate()
    }

    private fun setUpManualCardFocusChanged() {
        binding.edtCardNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                try {
                    if (s?.length == 22) {
                        binding.edtMMYY.requestFocus()
                    }
                } catch (e: Exception) {
                }
            }
        })
        binding.edtMMYY.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                try {
                    if (s?.length == 5) {
                        binding.edtCVV.requestFocus()
                    } else if (s?.length == 0) {
                        binding.edtCardNumber.requestFocus()
                    }
                } catch (e: Exception) {
                }
            }
        })
        binding.edtCVV.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                try {
                    if (s?.length == 0) {
                        binding.edtMMYY.requestFocus()
                    }
                } catch (e: Exception) {
                }
            }
        })
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
            SunmiPrintHelper.getInstance().openCashBox()
            cashPaymentWithVariation()
        }


    }

    fun setupColorChanges(txtview: AppCompatTextView, listtextview: ArrayList<AppCompatTextView>) {
        txtview.setBackgroundDrawable(resources.getDrawable(R.drawable.button_action_hover))
        txtview.setTextColor(resources.getColor(R.color.white))
        for (i in listtextview.indices) {
            listtextview[i].setBackgroundDrawable(resources.getDrawable(R.drawable.background_square_border_grey))
            listtextview[i].setTextColor(resources.getColor(R.color.txtColor))
        }
    }

    private fun splitClick() {

        binding.linearNextSplit.setOnSingleClickListener {
            PaymentBoldPosFragment.newInstance().addTipHideShow(false)
            viewModel.setSplitCount(isSelectedCount)
            loadPaymentLayout()
            tipAmountCalculation()
        }

        binding.tvFullAmount.setOnSingleClickListener {
            listtextview = arrayListOf()
            listtextview.add(binding.tv2ways)
            listtextview.add(binding.tv3ways)
            listtextview.add(binding.tv4ways)
            listtextview.add(binding.tv5ways)
            listtextview.add(binding.tv6ways)
            listtextview.add(binding.tvCustom)
            setupColorChanges(binding.tvFullAmount, listtextview)
            binding.tvCustom.text = "Custom"
            isSelectedCount = 1
            tipsetupGlobal(tipAmount, isSelectedCount)
            binding.tvwaysplit?.visibility = View.INVISIBLE
            binding.tvFullAMounttxt.visibility = View.VISIBLE

        }

        binding.tv2ways.setOnSingleClickListener {
            listtextview = arrayListOf()
            listtextview.add(binding.tvFullAmount)
            listtextview.add(binding.tv3ways)
            listtextview.add(binding.tv4ways)
            listtextview.add(binding.tv5ways)
            listtextview.add(binding.tv6ways)
            listtextview.add(binding.tvCustom)
            setupColorChanges(binding.tv2ways, listtextview)
            binding.tvCustom.text = "Custom"
            isSelectedCount = 2
            tipsetupGlobal(tipAmount, isSelectedCount)
            binding.tvFullAMounttxt.visibility = View.INVISIBLE
            binding.tvwaysplit?.visible()
            binding.tvwaysplit?.text = "$isSelectedCount Way Split Amount"

        }
        binding.tv3ways.setOnSingleClickListener {
            listtextview = arrayListOf()
            listtextview.add(binding.tv2ways)
            listtextview.add(binding.tvFullAmount)
            listtextview.add(binding.tv4ways)
            listtextview.add(binding.tv5ways)
            listtextview.add(binding.tv6ways)
            listtextview.add(binding.tvCustom)
            setupColorChanges(binding.tv3ways, listtextview)
            binding.tvCustom.text = "Custom"
            isSelectedCount = 3
            tipsetupGlobal(tipAmount, isSelectedCount)
            binding.tvFullAMounttxt.visibility = View.INVISIBLE
            binding.tvwaysplit?.visible()
            binding.tvwaysplit?.text = "$isSelectedCount Way Split Amount"
        }
        binding.tv4ways.setOnSingleClickListener {
            listtextview = arrayListOf()
            listtextview.add(binding.tv2ways)
            listtextview.add(binding.tv3ways)
            listtextview.add(binding.tvFullAmount)
            listtextview.add(binding.tv5ways)
            listtextview.add(binding.tv6ways)
            listtextview.add(binding.tvCustom)
            setupColorChanges(binding.tv4ways, listtextview)
            binding.tvCustom.text = "Custom"
            isSelectedCount = 4
            tipsetupGlobal(tipAmount, isSelectedCount)
            binding.tvFullAMounttxt.visibility = View.INVISIBLE
            binding.tvwaysplit?.visible()
            binding.tvwaysplit?.text = "$isSelectedCount Way Split Amount"
        }
        binding.tv5ways.setOnSingleClickListener {
            listtextview = arrayListOf()
            listtextview.add(binding.tv2ways)
            listtextview.add(binding.tv3ways)
            listtextview.add(binding.tv4ways)
            listtextview.add(binding.tvFullAmount)
            listtextview.add(binding.tv6ways)
            listtextview.add(binding.tvCustom)
            setupColorChanges(binding.tv5ways, listtextview)
            binding.tvCustom.text = "Custom"
            isSelectedCount = 5
            tipsetupGlobal(tipAmount, isSelectedCount)
            binding.tvFullAMounttxt.visibility = View.INVISIBLE
            binding.tvwaysplit?.visible()
            binding.tvwaysplit?.text = "$isSelectedCount Way Split Amount"
        }
        binding.tv6ways.setOnSingleClickListener {
            listtextview = arrayListOf()
            listtextview.add(binding.tv2ways)
            listtextview.add(binding.tv3ways)
            listtextview.add(binding.tv4ways)
            listtextview.add(binding.tv5ways)
            listtextview.add(binding.tvFullAmount)
            listtextview.add(binding.tvCustom)
            setupColorChanges(binding.tv6ways, listtextview)
            binding.tvCustom.text = "Custom"
            isSelectedCount = 6
            tipsetupGlobal(tipAmount, isSelectedCount)
            binding.tvFullAMounttxt.visibility = View.INVISIBLE
            binding.tvwaysplit?.visible()
            binding.tvwaysplit?.text = "$isSelectedCount Way Split Amount"
        }
        binding.tvCustom.setOnSingleClickListener {
            listtextview = arrayListOf()
            listtextview.add(binding.tv2ways)
            listtextview.add(binding.tv3ways)
            listtextview.add(binding.tv4ways)
            listtextview.add(binding.tv5ways)
            listtextview.add(binding.tv6ways)
            listtextview.add(binding.tvFullAmount)
            setupColorChanges(binding.tvCustom, listtextview)
            val bundle = Bundle()

            bundle.putDouble("totalPrice", WholetotalPrice)
            bundle.putInt("splitValue", isSelectedCount)

            findNavController().navigate(R.id.action_splitFragment_to_splitdialog)
        }
    }

    private fun observeData() {
        paymentviewModel.data.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                viewModel.redeemLoyaltyInfo = RedeemLoyaltyInfo()
                prefProvider.setValueInt("ORDER_ID", it.data.order.id)
                when {
                    paymentType == "Cash" -> {
                        LogUtil.logE("TipAmount 4:: ", tipAmount.toString())

                        val bundle = Bundle()
                        bundle.putBoolean("isDineIn", true)
                        bundle.putBoolean("isTotalPayment", true)
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
                                wholePrice - String.format(
                                    "%.2f",
                                    paymentAmount + cashDiscountSurcharge
                                ).toDouble()
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
//                                if (cashDiscountType == "CashDiscount") {
                                splitAllAmounts(
                                    Constants.CASH_DISCOUNT_SURCHARGE,
                                    cashDiscountSurcharge
                                )
//                                }

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
//                                if (cashDiscountType == "CashDiscount") {
                                splitAllAmounts(
                                    Constants.CASH_DISCOUNT_SURCHARGE,
                                    cashDiscountSurcharge
                                )
//                                }

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
//                                if (cashDiscountType == "CashDiscount") {
                                splitAllAmounts(
                                    Constants.CASH_DISCOUNT_SURCHARGE,
                                    cashDiscountSurcharge
                                )
//                                }

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
                        bundle.putParcelableArrayList(
                            DINE_IN_ADAPTER_LIST, dineInDataModel.dineInAdapterList?.toCollection(
                                arrayListOf()
                            )
                        )
                        bundle.putParcelable(PRINT_DATA_DINE_IN, dineInDataModel.dineInOrderDetails)
                        bundle.putParcelable(
                            DINE_IN_GUEST_PAYMENT_DATA,
                            dineInDataModel.guestPaymentModel
                        )
                        dineInDataModel.guestPosition?.let { it1 ->
                            bundle.putInt(
                                Constants.GUEST_POSITION,
                                it1
                            )
                        }

                        if (findNavController().currentDestination?.id == R.id.paymentBoldPosFragment) {

                            findNavController().navigate(
                                R.id.action_paymentBoldPosFragment_to_orderComplete,
                                bundle
                            )
                        }

                    }
                    paymentType == "Card" -> {
                        LogUtil.logE("TipAmount 4:: ", tipAmount.toString())

                        val bundle = Bundle()
                        bundle.putBoolean("isDineIn", true)
                        bundle.putBoolean("isTotalPayment", true)
                        if (remainingAmount == 0.0) {
                            bundle.putDouble("PaidAmount", paymentAmount)
                        } else {
                            bundle.putDouble("PaidAmount", remainingAmount)
                        }

                        var wholePrice =
                            prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble()
                        bundle.putDouble("WholetotalPrice", wholePrice)
                        var remainingValue = 0.0
                        remainingValue = if (cashDiscountType == "SurCharge") {
                            String.format("%.2f", wholePrice + cashDiscountSurcharge)
                                .toDouble() - paymentAmount
                        } else {
                            wholePrice - paymentAmount
                        }
                        if (remainingValue <= 0.0) {
                            remainingValue = 0.0
                        }


                        bundle.putDouble(
                            "remainingAmount",
                            remainingValue
                        )
                        prefProvider.setValue(Constants.WHOLE_AMOUNT, remainingValue.toString())

                        if (remainingValue == 0.0) {
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
                        bundle.putBoolean("isFromActiveOrder", false)
                        bundle.putParcelableArrayList(
                            DINE_IN_ADAPTER_LIST, dineInDataModel.dineInAdapterList?.toCollection(
                                arrayListOf()
                            )
                        )
                        bundle.putParcelable(PRINT_DATA_DINE_IN, dineInDataModel.dineInOrderDetails)
                        bundle.putParcelable(
                            DINE_IN_GUEST_PAYMENT_DATA,
                            dineInDataModel.guestPaymentModel
                        )
                        dineInDataModel.guestPosition?.let { it1 ->
                            bundle.putInt(
                                Constants.GUEST_POSITION,
                                it1
                            )
                        }

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
                LogUtil.logE("observeShowProgress1", it.toString())
                if (it) {
                    ProgressUtils.showProgressDialog(
                        "Please wait payment under process",
                        requireActivity()
                    )
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }

        dineinOrderVieweModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                LogUtil.logE("observeShowProgress2", it.toString())
                if (it) {
                    ProgressUtils.showProgressDialog(
                        "Please wait payment under process",
                        requireActivity()
                    )
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }

        dineinOrderVieweModel.showProgressCash.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                LogUtil.logE("observeShowProgress2", it.toString())
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
        Log.d(TAG, "paymentClick: click 2")

        LogUtil.logE(TAG, "isGuestPay:  ${isGuestPay}")
        if (isGuestPay) {
            if (custom_paymentAmount != 0.0) {
                dineinOrderVieweModel.totalPayAmount(custom_paymentAmount)
            }
            paymentType = "Cash"
            guestAttributeCalculation(-1, "")
            guestRequestModel?.paymentAttributes?.let { logPrintGuest(it) }
            if (dineInDataModel.isLastPayment) {
                dineinOrderVieweModel.payByGuest(
                    dineInDataModel.guestId ?: 0,
                    dineInDataModel.guestPaymentReq!!,
                    dineInDataModel.isLastPayment == isSelectedCount <= 1,
                    dineInDataModel.splitModel!!
                )
            } else {
                dineinOrderVieweModel.payByGuest(
                    dineInDataModel.guestId ?: 0, dineInDataModel.guestPaymentReq!!,
                    false, dineInDataModel.splitModel!!
                )
            }

        } else {
            Log.d(TAG, "paymentClick: click 3")
            makeCashPayment()
        }
    }


    private fun guestAttributeCalculation(i: Int, toJson: String) {
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
            totalDiscount
        guestRequestModel?.paymentAttributes!!.paymentType = paymentType
        if (paymentType == "Cash") {
            if (cashDiscountType == "CashDiscount") {
                guestRequestModel?.paymentAttributes!!.cash_discount_or_surcharge =
                    cashDiscountSurcharge
            } else {
                guestRequestModel?.paymentAttributes!!.cash_discount_or_surcharge =
                    0.0
            }
        } else {
            if (cashDiscountType == "SurCharge") {
                guestRequestModel?.paymentAttributes!!.cash_discount_or_surcharge =
                    cashDiscountSurcharge
            } else {
                guestRequestModel?.paymentAttributes!!.cash_discount_or_surcharge =
                    0.0
            }
        }
        guestRequestModel?.paymentAttributes!!.cash_discount_type = cashDiscountType
        guestRequestModel?.paymentAttributes!!.terminalId =
            prefProvider.getValueInt(Constants.TERMINAL_ID, 0)
        guestRequestModel?.paymentAttributes!!.employeeId =
            prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)


        if (i == 3 && paymentType == "Card") {
            guestRequestModel?.paymentAttributes!!.cardName =
                CardValidator.getCardType(cardNumber.trim())?.name.toString().uppercase()
            guestRequestModel?.paymentAttributes!!.cardNumber =
                if (cardNumber.isNotEmpty()) cardNumber.takeLast(4) else ""
            guestRequestModel?.paymentAttributes!!.cardType = "Credit"

            guestRequestModel?.paymentAttributes!!.magensaResponse = toJson


            LogUtil.logE(TAG, "getOptionType:  ${prefProvider.getValue(OPTION_TYPE, "")}")
            if (prefProvider.getValue(OPTION_TYPE, "").equals("SurCharge", true)) {
                guestRequestModel?.paymentAttributes?.cash_discount_or_surcharge =
                    cashDiscountSurcharge

            } else {
                guestRequestModel?.paymentAttributes?.cash_discount_or_surcharge = 0.0
            }
        } else if (paymentType == "Card" && toJson.isNotEmpty()) {

            if (toJson.isNotEmpty()) {
                val model = Gson().fromJson(
                    toJson,
                    PaymentResponse.PaymentResponseItem::class.java
                )
                LogUtil.logE("magensaResponse", Gson().toJson(model))


                guestRequestModel?.paymentAttributes!!.magensaResponse = toJson

                if (model.dataOutput != null) {
                    LogUtil.logE("dataOutput", Gson().toJson(model))
                    val cardNumber = model.dataOutput.PANLast4
                    var cardN = ""
                    model.dataOutput.additionalOutputData?.forEach {
                        LogUtil.logE("additionalOutputData", it.key)
                        if (it.key == "CardType") {
                            cardN = it.value
                        }
                    }
                    guestRequestModel?.paymentAttributes!!.cardName = cardN
                    guestRequestModel?.paymentAttributes!!.cardNumber = cardNumber

                    guestRequestModel?.paymentAttributes!!.cardName =
                        CardValidator.getCardType(cardNumber.trim())?.name.toString().uppercase()
                    guestRequestModel?.paymentAttributes!!.cardNumber =
                        if (cardNumber.isNotEmpty()) cardNumber.takeLast(4) else ""
                    guestRequestModel?.paymentAttributes!!.cardType = "Credit"


                }

                if (model.cardSwipeOutput != null) {
                    LogUtil.logE("cardSwipeOutput", Gson().toJson(model))
                    val cardNumber = model.cardSwipeOutput.pANLast4
                    var cardN = ""
                    model.cardSwipeOutput.additionalOutputData?.forEach {
                        if (it.key == "CardType") {
                            cardN = it.value
                        }
                    }

                    guestRequestModel?.paymentAttributes!!.cardName = cardN
                    guestRequestModel?.paymentAttributes!!.cardNumber = cardNumber
                }




                guestRequestModel?.paymentAttributes!!.cardType = "Credit"
                guestRequestModel?.paymentAttributes!!.transactionId =
                    model.transactionOutput?.transactionID.toString()
            }
        }
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
        guestPaymentAttributes.terminalId =
            guestRequestModel?.paymentAttributes!!.terminalId
        guestPaymentAttributes.employeeId =
            guestRequestModel?.paymentAttributes!!.employeeId
        guestPaymentAttributes.cash_discount_or_surcharge =
            guestRequestModel?.paymentAttributes!!.cash_discount_or_surcharge
        guestPaymentAttributes.cash_discount_type =
            guestRequestModel?.paymentAttributes!!.cash_discount_type

        if (paymentType == "Card") {
            guestPaymentAttributes.cardName = guestRequestModel?.paymentAttributes!!.cardName
            guestPaymentAttributes.cardNumber = guestRequestModel?.paymentAttributes!!.cardNumber
            guestPaymentAttributes.cardType = guestRequestModel?.paymentAttributes!!.cardType
            guestPaymentAttributes.magensaResponse =
                guestRequestModel?.paymentAttributes!!.magensaResponse
            guestPaymentAttributes.transactionId =
                guestRequestModel?.paymentAttributes!!.transactionId
            if (prefProvider.getValue(OPTION_TYPE, "").equals("SurCharge", true)) {
                guestPaymentAttributes?.cash_discount_or_surcharge =
                    cashDiscountSurcharge

            } else {
                guestPaymentAttributes?.cash_discount_or_surcharge = 0.0
            }
        } else if (paymentType == "Cash") {
            if (prefProvider.getValue(OPTION_TYPE, "").equals("CashDiscount", true)) {
                guestPaymentAttributes?.cash_discount_or_surcharge = cashDiscountSurcharge
                guestRequestModel?.paymentAttributes?.cash_discount_or_surcharge =
                    cashDiscountSurcharge

            } else {
                guestPaymentAttributes?.cash_discount_or_surcharge = 0.0
                guestRequestModel?.paymentAttributes?.cash_discount_or_surcharge = 0.0
            }
        }

        guestRequestModel?.paymentAttributes!!.paymentAttributes =
            listOf(guestPaymentAttributes)
    }

    private fun guestPaySpit() {
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
            totalDiscount
        guestRequestModel?.paymentAttributes!!.paymentType = paymentType
        guestRequestModel?.paymentAttributes!!.cash_discount_or_surcharge =
            cashDiscountSurcharge
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

    private fun paymentClick() {
        binding.llCreditCard.setOnSingleClickListener {

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
            //  makePaymentCreditCard()
            paymentAmount += tipAmount

            if (paymentAmount != 0.0) {
                if (mSessionManager.isConnected) {
                    magtekModule.stopListner(false)
                    if (device == 0) {
                        magtekPaymentCall()
                    } else {
                        magtekProPaymentCall()
                    }
                    prefProvider.setValueboolean(Constants.IS_PAX_CONNECTED, false)
                } else if (prefProvider.getValueboolean(Constants.IS_PAX_CONNECTED, false) && !mSessionManager.isConnected) {
                    makePaxPaymentRequest()
                } else {
                    errorDisplay("Please connect a payment device.")
                }
            } else {
                errorDisplay("Payment Amount is zero.")
            }

            dashboardViewModel.paymentType = "card"

        }
        binding.llManualCardEntry.setOnSingleClickListener {
            binding.frameLayoutId.visible()
            binding.relativeMain.gone()
            binding.llManualCard.visible()
            isManualCard = true

        }

        binding.tvCash0.setOnSingleClickListener {

            custom_paymentAmount = 0.0
            prefProvider.setValue(Constants.OPEN_ORDER_ITEMS_BASE, "")

            if (isGuestPay) {
                dineinOrderVieweModel.totalPayAmount(
                    binding.tvCash0.text.toString().replace("$", "").trim().toDouble()
                )
            } else {
                paymentviewModel.totalPayAmount(
                    binding.tvCash0.text.toString().replace("$", "").trim().toDouble()
                )
            }
            SunmiPrintHelper.getInstance().openCashBox()
            paymentAmount = binding.tvCash0.text.toString().replace("$", "").trim().toDouble()
            Log.d(TAG, "paymentClick: click 1")
            cashPaymentWithVariation()
        }
        binding.tvCash1.setOnSingleClickListener {

            custom_paymentAmount =
                binding.tvCash1.text.toString().replace("$", "").trim().toDouble()
            SunmiPrintHelper.getInstance().openCashBox()
            cashPaymentWithVariation()
        }
        binding.tvCash2.setOnSingleClickListener {
            custom_paymentAmount =
                binding.tvCash2.text.toString().replace("$", "").trim().toDouble()
            SunmiPrintHelper.getInstance().openCashBox()
            cashPaymentWithVariation()
        }
        binding.tvCash3.setOnSingleClickListener {

            custom_paymentAmount =
                binding.tvCash3.text.toString().replace("$", "").trim().toDouble()
            SunmiPrintHelper.getInstance().openCashBox()
            cashPaymentWithVariation()
        }
        binding.tvCustomAmount.setOnSingleClickListener {


            val finalCashAmount =  binding.tvCash0.text.toString().replace("$", "").trim().toDouble()
            val bundleVal = Bundle().apply {
                putDouble("totalprice", ((WholetotalPrice + tipAmount)))
                putDouble("amountToDisplay", finalCashAmount)
            }





            findNavController().navigate(
                R.id.action_paymentBoldPosFragment_to_customAmountFragment,
                bundleVal
            )

        }
        binding.tvPaymentLink.setOnSingleClickListener {

        }


        binding.imgBackManualCard.setOnSingleClickListener {
            isManualCard = false
            binding.relativeMain.visible()
            binding.llManualCard.gone()
        }

        binding.txtCharge.setOnSingleClickListener {

            paymentType = "Card"


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
            cardExpDate = binding.edtMMYY.rawText.toString().trim()
            cardCVV = binding.edtCVV.text.toString().trim()

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

    // To make card payment via pax device
    private fun makePaxPaymentRequest() {
        GlobalScope.launch {
            Log.d("getCommSettingFromFile ","getCommSettingFromFile: "+Gson().toJson(SettingINI.getCommSettingFromFile(context!!,"/storage/emulated/0/Download/"+ SettingINI.FILENAME)))
            posLink.SetCommSetting(SettingINI.getCommSettingFromFile(context!!,"/storage/emulated/0/Download/"+ SettingINI.FILENAME))
            val amt = ((paymentAmount-tipAmount) * 100).roundToInt()
            val tip_amt = (tipAmount * 100).roundToInt()
            ECRRefNumber = System.currentTimeMillis().toString()
            Log.d("Amt: ","amt $amt tip $tip_amt")
            var broadPOS_version = prefProvider.getValue(
                Constants.BROADPOS_VERSION,
                ""
            )

            CoroutineScope(Dispatchers.Main).launch {
                ProgressUtils.showProgressDialog(requireActivity())
            }
            mPaymentRequest = PaymentRequest()
            mPaymentRequest.TransType = mPaymentRequest.ParseTransType("SALE")
            mPaymentRequest.TenderType = mPaymentRequest.ParseTenderType("CREDIT")
            mPaymentRequest.Amount = amt.toString()
            mPaymentRequest.TipAmt = tip_amt.toString()
            mPaymentRequest.ECRRefNum = ECRRefNumber
            if (broadPOS_version.contains("TSYS")) {
                mPaymentRequest.ExtData = "<Force>T</Force>"
            } else if (broadPOS_version.contains("Rapid")) {
                mPaymentRequest.ExtData = "<Force>T</Force><TokenRequest>1</TokenRequest>"
            }

            Log.d("ECRRefNum", "ECRRefNum: $ECRRefNumber")

            posLink.PaymentRequest = mPaymentRequest
            val result = posLink.ProcessTrans()
            Log.d("result: ", result.Code.toString() + " " + result.Msg)
            if (result.Code === ProcessTransResult.ProcessTransResultCode.OK) {
                val msg = Message()
                msg.what = Constants.TRANSACTION_SUCCESSED
                msg.obj = posLink.PaymentResponse

                val response = msg.obj as com.pax.poslink.PaymentResponse
                val resultCode = response.ResultCode
                val resultTxt = response.ResultTxt
                val approvedAmount = response.ApprovedAmount
                ExtData = response.ExtData
                RefNumber = response.RefNum

                cardLastDigits = response.BogusAccountNum
                EDCType = response.CardType
                CARDBIN = response.CardInfo.CardBin
                var tipAmount = response.ApprovedTipAmount
                GlobalUID = response.PaymentTransInfo.GlobalUid
                paymentviewModel.setPAXData(RefNumber, GlobalUID)
//                prefProvider.setValue(Constants.GLOBAL_ID, globalUID!!)

//                dineInDataModel.guestPaymentReq?.paymentAttributes?.let { it ->
//                    it.cardName = response.CardType
//                    it.cardNumber = cardLastDigits
//                    it.cardType = 0.toString()
//
//                }

                //implementation("org.dom4j:dom4j:2.1.3")
                PAXtoken = response.PaymentTransInfo.Token
                Log.d("token:", "token $PAXtoken")
                Log.d(
                    "Payment Details: ",
                    "$ExtData $resultCode $resultTxt $GlobalUID $RefNumber"
                )
                Log.d("Payment Details: ", "$cardLastDigits $approvedAmount $CARDBIN $EDCType $tipAmount ${Gson().toJson(response)}")

                if (resultCode == "000000") {
                    CoroutineScope(Dispatchers.Main).launch {
                        ProgressUtils.dismissProgressDialog()
                        coroutineScope {
                            makePaymentCreditCard()
                        }
                    }
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        ProgressUtils.dismissProgressDialog()
                        AlertUtils.showCustomAlertWithListenerWithOK(requireContext(),resultTxt,object:
                            DialogInterface.OnClickListener{
                            override fun onClick(p0: DialogInterface?, p1: Int) {
                                try {
                                    p0?.dismiss()
                                } catch (e: Exception) {
                                }
                            }
                        })
//                        requireActivity().toast("$resultCode $resultTxt", Toast.LENGTH_LONG)
//                        connectBP()
                    }
                }
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    ProgressUtils.dismissProgressDialog()
                    AlertUtils.showCustomAlertWithListenerWithOKCancel(
                        requireContext(),
                        getString(R.string.pax_connect_error), getString(R.string.reconnect),
                    )
                    { _, _ ->
                        // Add connect to PAX logic
                        magtekProViewModel.initPOSLink(requireContext())
                    }

                    /*if (result.Msg.toString() == "CONNECT ERROR" || result.Msg.toString() == "TIME OUT"){
                        AlertUtils.showCustomAlertWithListenerWithOKCancel(
                            requireContext(),
                            getString(R.string.pax_connect_error), getString(R.string.reconnect),
                        )
                        { _, _ ->
                            // Add connect to PAX logic
                            magtekProViewModel.initPOSLink(requireContext())
                        }
//                        Toast.makeText(requireContext(), R.string.pax_connect_error, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(requireContext(), "getMerchantDetails Failed ${result.Code} ${result.Msg}", Toast.LENGTH_LONG).show()
                    }*/
                }
            }

        }
    }

    // To make card payment by adding card details manually
    private fun manualCardPaymentCall(
        cardNumber: String,
        expDate: String,
        cardCVV: String
    ) {

        val jsonArray1 = magtekRequestUtils.processManualEntry(
            (paymentAmount * 100),
            cardNumber,
            expDate,
            cardCVV
        )



        networkCall(jsonArray1, 3)

    }

    private fun errorDisplay(msg: String) {

        AlertUtils.showCustomAlert(requireContext(), msg)
    }

    fun getDataFromPref() {
        redeemLoyaltyInfo = viewModel.redeemLoyaltyInfo
        prefProvider.setValue(Constants.ORDER_TYPE, Constants.DINE_IN)
        if (prefProvider.getValue(Constants.WHOLE_AMOUNT, "").isEmpty() || prefProvider.getValue(
                Constants.WHOLE_AMOUNT,
                ""
            ) == "0.0" || prefProvider.getValue(
                Constants.WHOLE_AMOUNT,
                ""
            ) == "0.00"
        ) {
            LogUtil.logE(TAG, "totalPrice  ${viewModel.totalPrice}")

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
        paymentviewModel.setServiceChargeListApplied(serviceChargeAppliedList)
        viewModel.ordertypelist.forEach {
            //For resolving issue BIS-303
            //Added one more OR condition to check if order_type_name from preference is "DineIn" or "Dine In"
            //By Dharmesh Basapati
            if (prefProvider.getValue(Constants.ORDER_TYPE_NAME, Constants.DINE_IN) == it.name ||
                prefProvider.getValue(Constants.ORDER_TYPE_NAME, Constants.DINE_IN) == it.orderType) {
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

    // To set different cash payment options and total amount values
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

    // To calculate tip added by user
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
//            if(this::presentation.isInitialized){
//                presentation.show()
//                presentation.showTipsAdded(tipAmount,WholetotalPrice)
//            }
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

    // Split total amount as per user's split choice
    private fun splitAllAmounts(TAG: String, amount: Double) {
        var remainingValue = prefProvider.getValue(TAG, "").toDouble() - amount
        prefProvider.setValue(TAG, String.format("%.2f", remainingValue))
        Log.d(TAG, "splitAllAmounts: " + prefProvider.getValue(TAG, "").toDouble())
    }

    // Calculate service charge / cash discount on amount
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
        binding.linearTab1.setOnSingleClickListener {
            PaymentBoldPosFragment.newInstance().addTipHideShow(false)
            isSelectedCount = 1
            tipsetupGlobal(tipAmount, isSelectedCount)
            loadPaymentLayout()
            tipAmountCalculation()
        }

        if(isGuestPay)
            binding.linearTab2.gone()
        else binding.linearTab2.setOnSingleClickListener {
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
                    binding.tvwaysplit?.visibility = View.INVISIBLE
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
                binding.tvwaysplit?.visibility = View.INVISIBLE

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

        paymentAmount -= tipAmount
        paymentAmount = MethodUtils.roundOffAmountDouble(paymentAmount)
        paymentType = "Card"
        paymentviewModel.saveOrder(false)


        if (isGuestPay) {

            ////

            val myRequest = cartList?.let {
                paymentviewModel.createOrderRequestForCard(
                    it,
                    subTotalPrice,
                    paymentAmount,
                    totalServiceCharge,
                    totalTax,
                    Constants.DINE_IN,
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
                    cardNumber,
                    cashDiscountType,
                    tipID,
                    GlobalUID,
                    RefNumber,
                    ExtData,
                    ECRRefNumber,
                    PAXtoken,
                    cardLastDigits,
                    cardTypeOfTransaction = EDCType
                )
            }

            ///

            if (custom_paymentAmount != 0.0) {
                dineinOrderVieweModel.totalPayAmount(custom_paymentAmount)
            }
            paymentType = "Card"
            guestAttributeCalculation(myRequest?.order?.paymentAttributes?.cardType ?: -1,"")


            if(myRequest?.order?.paymentAttributes!=null){

                val paymentAttributes = myRequest.order.paymentAttributes

                dineInDataModel.guestPaymentReq?.paymentAttributes.let { it ->


                }

                dineInDataModel.guestPaymentReq?.paymentAttributes?.paymentAttributes?.forEach {
                    it.cardName = paymentAttributes?.cardName?:""
                    it.cardNumber = paymentAttributes?.cardNumber?:""
                    it.cardType = "Credit"/*paymentAttributes?.cardType.toString()*/

                    it.ext_data = paymentAttributes?.ext_data?:""
                    it.global_uniq_id = paymentAttributes?.global_uniq_id?:""
                    it.pax_transaction_token = paymentAttributes?.pax_transaction_token?:""
                    it.ecr_ref_num = paymentAttributes?.ecr_ref_num?:""
                    it.ref_num = paymentAttributes?.ref_num?:""
                }
            }


            guestRequestModel?.paymentAttributes?.let { logPrintGuest(it) }
            if (dineInDataModel.isLastPayment) {
                dineinOrderVieweModel.payByGuest(
                    dineInDataModel.guestId ?: 0,
                    dineInDataModel.guestPaymentReq!!,
                    dineInDataModel.isLastPayment == isSelectedCount <= 1,
                    dineInDataModel.splitModel!!
                )
            } else {
                dineinOrderVieweModel.payByGuest(
                    dineInDataModel.guestId ?: 0, dineInDataModel.guestPaymentReq!!,
                    false, dineInDataModel.splitModel!!
                )
            }

        } else {
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

            val myRequest = cartList?.let {
                paymentviewModel.createOrderRequestForCard(
                    it,
                    subTotalPrice,
                    paymentAmount,
                    totalServiceCharge,
                    totalTax,
                    Constants.DINE_IN,
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
                    cardNumber,
                    cashDiscountType,
                    tipID,
                    GlobalUID,
                    RefNumber,
                    ExtData,
                    ECRRefNumber,
                    PAXtoken,
                    cardLastDigits,
                    cardTypeOfTransaction = EDCType
                )
            }
            if (myRequest != null) {
                if (custom_paymentAmount.toDouble() != 0.0) {
                    paymentviewModel.totalPayAmount(custom_paymentAmount)
                }
                paymentAttributesRequest(myRequest)
            }
        }
    }

    // make order request with payment attributes on cash payment to reflect on server
    private fun makeCashPayment() {

        paymentType = "Cash"

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
        Log.d(TAG, "paymentClick: click 4")
        val myRequest = cartList?.let {
            paymentviewModel.createOrderRequest(
                it,
                subTotalPrice,
                paymentAmount,
                totalServiceCharge,
                totalTax,
                Constants.DINE_IN,
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
                tipID, offlineId = orderOfflineId
            )
        }

        if (myRequest != null) {
            if (custom_paymentAmount != 0.0) {
                paymentviewModel.totalPayAmount(custom_paymentAmount)
            }
            paymentAttributesRequest(myRequest)
        }
    }

    fun removeCustomer() {
        prefProvider.setValue(Constants.CUSTOMER_NAME, "")
        prefProvider.setValue(Constants.RECEIPT_CUSTOMER_NAME, "")
        prefProvider.setValueInt(Constants.CUSTOMER_ID, -1)
        prefProvider.setValue("PaidAmount", "")
        prefProvider.setValue(Constants.WHOLE_AMOUNT, "")
        prefProvider.setValue(Constants.PREF_CUSTOMER, "")
        prefProvider.setValue(Constants.SUB_TOTAL, "")
        prefProvider.setValue(Constants.CASH_DISCOUNT_SURCHARGE, "")
        prefProvider.setValue(Constants.TOTAL_DISCOUNT, "")
        prefProvider.setValue(Constants.TIP, "")
        prefProvider.setValue(Constants.TAX_CHARGE, "")
        prefProvider.setValue(Constants.SERVICE_CHARGE, "")
        prefProvider.setValueInt("ORDER_ID", -1)
        prefProvider.setValueInt(Constants.PAYMENT_ID, 0)
        prefProvider.setValue(Constants.TOTAL_PRICE_ACTUAL, "0.0")
        prefProvider.setValue(Constants.SUB_TOTAL_ACTUAL, "0.0")
        prefProvider.setValue(Constants.TOTAL_DISCOUNT_ACTUAL, "0.0")
        prefProvider.setValue(
            Constants.TOTAL_SERVICE_CHARGE_ACTUAL,
            "0.0"
        )
        prefProvider.setValue(Constants.TAX_CHARGE_ACTUAL, "0.0")
        prefProvider.setValue(Constants.TIPS_AMOUNT_ACTUAL, "0.0")
    }

    // generate payment attributes request
    private fun paymentAttributesRequest(myRequest: OrderRequestModel) {
        val orderId = prefProvider.getValueInt("ORDER_ID", -1)
        if (orderId == -1) {
            if (myRequest.order.totalAmount!=0.0){
                myRequest.completed_all_payments = isSelectedCount <= 1
            }else{
                myRequest.completed_all_payments = true
            }

            paymentviewModel.submit(myRequest)
        } else {
            val paymentReq = myRequest.order.paymentAttributes
            if (paymentReq != null) {
                paymentReq.order_id = orderId
            }

            if (prefProvider.getValueboolean(
                    Constants.SPLIT_ENABLE,
                    false
                ) && prefProvider.getValueInt("ORDER_ID", -1) != -1
            ) {


                // total amount - (hal pay amoutn + alredy pay )
                val aa = SpitByOrderRequestModel(
                    orderId, isSelectedCount <= 1,
                    SpitByOrderPaymentModel(listOf(paymentReq) as List<PaymentAttributes>)
                )

                paymentviewModel.splitByOrder(aa, true)
            } else {
                if (myRequest.order.totalAmount!=0.0){
                    myRequest.completed_all_payments = isSelectedCount <= 1
                }else{
                    myRequest.completed_all_payments = true
                }
                paymentviewModel.submit(myRequest)
            }

        }
    }

    // To make card pqayment using magtek device
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
        ProgressUtils.dismissProgressDialog()

        val jsonArray1 = magtekModule.m_scra?.let {
            magtekRequestUtils.processCardSwipe(
                (paymentAmount * 100),
                magtekModule.m_scra!!.ksn,
                magtekModule.m_scra!!.magnePrint,
                magtekModule.m_scra!!.magnePrintStatus,
                it.track2
            )
        }

        networkCall(jsonArray1, 1)

    }

    private fun networkCall(jsonArray1: JsonArray?, i: Int) {

        ProgressUtils.showProgressDialog("Please wait payment under process", requireActivity())
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
                    if (response.body() != null && response.body()!![0].transactionOutput != null) {
                        if (response.body()!![0].transactionOutput?.isTransactionApproved == true) {
                            if (isDynamo())
                                magtekModule.closeDevice()
                            paymentviewModel.setMagensaResponse(
                                Gson().toJson(response.body()!![0]),
                                (if (i == 3){ cardNumber=cardNumber.takeLast(4)
                                }else if(i== 1){
                                    cardNumber = (response.body()!![0].dataOutput?.PANLast4).toString()
                               }else if(i==2){
                                    cardNumber = (response.body()!![0].dataOutput?.PANLast4).toString()
                                } else {
                                    cardNumber = ""
                                }).toString()
                            )
                            if (isGuestPay) {
                                paymentAmount -= tipAmount
                                paymentType = "Card"
                                dineinOrderVieweModel.totalPayAmount(paymentAmount)

                                guestAttributeCalculation(i, Gson().toJson(response.body()!![0]))
                                guestRequestModel?.paymentAttributes?.let { logPrintGuest(it) }
                                if (dineInDataModel.isLastPayment) {
                                    dineinOrderVieweModel.payByGuest(
                                        dineInDataModel.guestId ?: 0,
                                        dineInDataModel.guestPaymentReq!!,
                                        dineInDataModel.isLastPayment == isSelectedCount <= 1,
                                        dineInDataModel.splitModel
                                    )
                                } else {
                                    dineinOrderVieweModel.payByGuest(
                                        dineInDataModel.guestId ?: 0,
                                        dineInDataModel.guestPaymentReq!!,
                                        false,
                                        dineInDataModel.splitModel!!
                                    )
                                }
                            } else {
                                makePaymentCreditCard()
                            }
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
            (paymentAmount * 100),
            TLVParser.getHexString(data),
            Constants.AUTHORIZE
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
                            LogUtil.logE("", "[CONNECTED]")

                            startTransaction()
                        }
                        ConnectionState.Disconnected -> {
                            LogUtil.logE("", "[DISCONNECTED]")
                            ProgressUtils.dismissProgressDialog()
                            AlertUtils.showCustomAlert(requireContext(), "DISCONNECTED")
                        }
                        ConnectionState.Disconnecting -> {
                            LogUtil.logE("", "[DISCONNECTING]")
                        }
                        ConnectionState.Connecting -> {
                            LogUtil.logE("", "[CONNECTING]")

                        }
                        else -> ""
                    }
                }
                EventType.TransactionResult -> {

//                ProgressUtils.dismissProgressDialog()

                    LogUtil.logE("TransactionResult", "TransactionResult called")

                    dismissDialog()

                    val jsonArray1 = magtekRequestUtils.processData(
                        (paymentAmount * 100),
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

        ProgressUtils.showProgressDialog("Please tap, insert or swipe card", requireActivity())
        ProgressUtils.setCallback(this)


        LogUtil.logE("mSessionManager", mSessionManager.isConnected.toString())
        if (mSessionManager.isConnected) {
            startTransaction()
        } else {
            val deviceList: List<IDevice> = CoreAPI.getDeviceList(context, DeviceType.MMS, this)
            setupList(deviceList)

        }
    }

    private fun setupList(deviceList: List<IDevice>) {

        if (deviceList.isNotEmpty()) {

            val device = deviceList[0]
            mSessionManager.device = device
            mSessionManager.connectDevice()
        }

    }

    override fun OnDeviceList(mlist: MutableList<IDevice>?) {
        if (mlist != null) {
            setupList(mlist)
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
        val currencyCode = byteArrayOf(0x08, 0x40)
        transaction.setCurrencyCode(currencyCode)
        mSessionManager.startTransaction(transaction, getSignature = false, fallback = false)


    }

    private fun isDynamo(): Boolean {
        val device = prefProvider.getValueInt(Constants.MAGTEK_HARDWARE, 0)
        return device == 0
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

    private fun navigateOnPaymentSuccess() {
        dineinOrderVieweModel.onPayment.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { str ->
                LogUtil.logE(TAG, "getstr:   $str")
                /*AlertUtils.showCustomAlertWithListenerWithOK(requireContext(), str) { _, _ ->*/


                gotoPay()


                /*}*/

            }
        }
    }

    private fun gotoPay() {
        when {

            paymentType == "Cash" -> {
                LogUtil.logE("TipAmount 4:: ", tipAmount.toString())

                val bundle = Bundle()
                bundle.putBoolean("isDineIn", true)
                bundle.putBoolean("isTotalPayment", true)
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
                    if (prefProvider.getValue(Constants.WHOLE_AMOUNT, "").isEmpty()) {
                        0.0
                    } else {
                        String.format(
                            "%.2f",
                            prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble()
                        ).toDouble()
                    }

                LogUtil.logE(TAG, "wholePricewholePrice:  ${wholePrice}")

                bundle.putDouble("WholetotalPrice", wholePrice)
                var remainingValue = 0.0
                if (custom_paymentAmount != 0.0) {
                    if (cashDiscountType == "CashDiscount") {
                        wholePrice -= cashDiscountSurcharge
                    }
                    if (custom_paymentAmount != 0.0 && isSelectedCount != 1) {
                        var splitChange = 0.0
                        if (cashDiscountType == "CashDiscount") {
                            splitChange =
                                custom_paymentAmount - paymentAmount + cashDiscountSurcharge
                        } else {
                            splitChange =
                                custom_paymentAmount - paymentAmount
                        }

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
                    if (cashDiscountType == "CashDiscount") {
                        remainingValue =
                            wholePrice - String.format(
                                "%.2f",
                                paymentAmount + cashDiscountSurcharge
                            ).toDouble()
                    } else {
                        remainingValue = wholePrice - paymentAmount
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
                        splitAllAmounts(
                            Constants.CASH_DISCOUNT_SURCHARGE,
                            cashDiscountSurcharge
                        )
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
                        splitAllAmounts(
                            Constants.CASH_DISCOUNT_SURCHARGE,
                            cashDiscountSurcharge
                        )
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

                }


                orderId?.let { bundle.putInt("orderID", it) }
                //bundle.putParcelable("receiptData", it.data)
                bundle.putInt("splitValue", isSelectedCount)
                bundle.putBoolean("isSplitByAmount", false)
                bundle.putString("paymentType", "Cash")
                bundle.putParcelable("cartList", cartList)
                bundle.putParcelable("redeemLoyalty", redeemLoyaltyInfo)
                bundle.putDouble("TipAmount", tipAmount)

                bundle.putDouble("noCashAdj", cashDiscountSurcharge)
                bundle.putBoolean("isFromActiveOrder", false)
                bundle.putBoolean("isGuestPaymentTotal", isLastPayment)
                bundle.putBoolean("isGuest", isGuestPay)
                bundle.putBoolean("isLastPayment", isLastPayment)
                bundle.putParcelableArrayList(
                    DINE_IN_ADAPTER_LIST, dineInDataModel.dineInAdapterList?.toCollection(
                        arrayListOf()
                    )
                )


                bundle.putParcelable(PRINT_DATA_DINE_IN, dineInDataModel.dineInOrderDetails)
                bundle.putParcelable(DINE_IN_GUEST_PAYMENT_DATA, dineInDataModel.guestPaymentModel)
                dineInDataModel.guestPosition?.let { it1 ->
                    bundle.putInt(
                        Constants.GUEST_POSITION,
                        it1
                    )
                }
                if (findNavController().currentDestination?.id == R.id.paymentBoldPosFragment) {
                    findNavController().navigate(
                        R.id.action_paymentBoldPosFragment_to_orderComplete,
                        bundle
                    )
                }

            }

            paymentType == "Card" -> {
                LogUtil.logE("TipAmount 4:: ", tipAmount.toString())

                val bundle = Bundle()
                bundle.putBoolean("isDineIn", true)

                if (remainingAmount == 0.0) {
                    bundle.putDouble("PaidAmount", paymentAmount)
                } else {
                    bundle.putDouble("PaidAmount", remainingAmount)
                }

                val wholePrice =
                    prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble()
                bundle.putDouble("WholetotalPrice", wholePrice)
                var remainingValue = 0.0
                remainingValue = if (cashDiscountType == "SurCharge") {
                    String.format("%.2f", wholePrice + cashDiscountSurcharge)
                        .toDouble() - paymentAmount
                } else {
                    wholePrice - paymentAmount
                }
                if (remainingValue <= 0.0) {
                    remainingValue = 0.0
                }

                bundle.putDouble(
                    "remainingAmount",
                    remainingValue
                )
                prefProvider.setValue(Constants.WHOLE_AMOUNT, remainingValue.toString())

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


                orderId?.let { bundle.putInt("orderID", it) }
                // bundle.putParcelable("receiptData", it.data)
                bundle.putInt("splitValue", isSelectedCount)
                bundle.putBoolean("isSplitByAmount", false)
                bundle.putString("paymentType", "Card")
                bundle.putParcelable("cartList", cartList)
                bundle.putParcelable("redeemLoyalty", redeemLoyaltyInfo)
                bundle.putDouble("TipAmount", tipAmount)

                bundle.putDouble("noCashAdj", cashDiscountSurcharge)
                bundle.putBoolean("isFromActiveOrder", false)
                bundle.putBoolean("isGuestPaymentTotal", isLastPayment)
                bundle.putBoolean("isGuest", isGuestPay)
                bundle.putBoolean("isLastPayment", isLastPayment)
                bundle.putParcelableArrayList(
                    DINE_IN_ADAPTER_LIST, dineInDataModel.dineInAdapterList?.toCollection(
                        arrayListOf()
                    )
                )
                bundle.putParcelable(PRINT_DATA_DINE_IN, dineInDataModel.dineInOrderDetails)
                bundle.putParcelable(DINE_IN_GUEST_PAYMENT_DATA, dineInDataModel.guestPaymentModel)
                dineInDataModel.guestPosition?.let { it1 ->
                    bundle.putInt(
                        Constants.GUEST_POSITION,
                        it1
                    )
                }

                if (findNavController().currentDestination?.id == R.id.paymentBoldPosFragment) {
                    findNavController().navigate(
                        R.id.action_paymentBoldPosFragment_to_orderComplete,
                        bundle
                    )
                }

            }
        }

    }

    private fun observeQueueCreate() {
        paymentviewModel.queueStartSaveOrder.observe(requireActivity()) { event ->
            event.getContentIfNotHandled()?.let {
                createQueuePrinter(it)
            }
        }
    }

    private fun createQueuePrinter(createOrder: CreateOrderResponse) {
        val listPrinter: List<Int> = listOf()
        val orderRequest = cartList?.let {

            paymentviewModel.createOrderRequest(
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
            location_id = prefProvider.getValueInt(Constants.LOCATION_ID, 0),
            order_type = prefProvider.getValue(Constants.ORDER_TYPE, ""),
            printer_id = listPrinter,
            order_item_attributes = orderRequest?.order?.orderItemsAttributes ?: listOf(),
            order_data = orderRequest?.order ?: OrderAttributeRequestModel(),
            terminal_id = prefProvider.getValueInt(Constants.TERMINAL_ID, 0)

        )
        paymentviewModel.createQueuePrinter(createRequest, createOrder)
    }

}