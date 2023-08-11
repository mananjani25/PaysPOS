package com.android.pos.ui.fragments.checkout

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.os.Message
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.CartModel
import com.android.pos.data.entities.RedeemLoyaltyInfo
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.requestModel.*
import com.android.pos.data.model.requestModel.giftCard.request.GiftCardCheckBalanceRequest
import com.android.pos.data.model.responseModel.CreateOrderResponse
import com.android.pos.data.remote.ApiService
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.DEFAULT_ORDER
import com.android.pos.data.remote.Constants.GIFT_CARD
import com.android.pos.data.remote.Constants.GIFT_CARD_NUMBER
import com.android.pos.data.remote.Constants.GIFT_CARD_PIN
import com.android.pos.data.remote.Constants.IS_GIFT_CARD_REDEEM
import com.android.pos.data.remote.Constants.IS_ORDER_REDEEMABLE_WITH_GIFT_CARD
import com.android.pos.data.remote.Constants.ORDER_TYPE
import com.android.pos.data.remote.Constants.TAKEOUT
import com.android.pos.data.remote.Constants.TIP_ADDED
import com.android.pos.data.remote.Constants.TIP_ADDED_AMOUNT
import com.android.pos.databinding.FragmentCheckoutDetailsNewBinding
import com.android.pos.di.ApiModule1
import com.android.pos.di.MagtekModule
import com.android.pos.di.PrefProvider
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.ui.fragments.dashboard.bolddashboard.CustomDisplay
import com.android.pos.ui.fragments.dinein.DineInOrderTableViewModel
import com.android.pos.ui.fragments.eGiftCard.GiftCardViewModel
import com.android.pos.ui.fragments.loginscreen.PasscodeViewModel
import com.android.pos.ui.fragments.magtek.MagtekRequestUtils
import com.android.pos.ui.fragments.magtek.PaymentResponse
import com.android.pos.ui.fragments.magtekPro.MTParser
import com.android.pos.ui.fragments.magtekPro.SessionManager
import com.android.pos.ui.fragments.payment.PaymentBoldPosFragment
import com.android.pos.ui.fragments.payment.PaymentViewModel
import com.android.pos.ui.fragments.settings.tip.TipListViewModel
import com.android.pos.utils.*
import com.android.pos.utils.MethodUtils.Companion.toPrecision
import com.android.pos.utils.callback.DeleteOptionCallback
import com.android.pos.utils.callback.magtekCallback
import com.android.pos.utils.extensions.*
import com.android.pos.utils.paxUtils.AppThreadPool
import com.android.pos.utils.paxUtils.POSLinkCreatorWrapper
import com.android.pos.utils.paxUtils.SettingINI
import com.android.pos.utils.statusUtils.Status
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.magtek.mobile.android.mtlib.IMTCardData
import com.magtek.mobile.android.mtlib.MTConnectionState
import com.magtek.mobile.android.mtusdk.*
import com.pax.poslink.PaymentRequest
import com.pax.poslink.PosLink
import com.pax.poslink.ProcessTransResult
import com.pax.poslink.aidl.BasePOSLinkCallback
import com.pax.poslink.broadpos.BroadPOSCommunicator
import com.pax.poslink.broadpos.BroadPOSCommunicator.StartListenerCallBack
import com.pax.poslink.fullIntegration.InputAccount
import com.pax.poslink.fullIntegration.InputAccount.InputAccountCallback
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject


@AndroidEntryPoint
class CheckoutDetailsFragmentNew(val isFromOpenOrder: Boolean = false) : Fragment(), magtekCallback,
    DeleteOptionCallback, IDeviceListCallback, InputAccountCallback, BasePOSLinkCallback<InputAccount.InputAccountResponse> {
    private var textToPay: Boolean = false
    private var isShow: Boolean = false
    private lateinit var presentation: CustomDisplay
    private val dashboardViewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val passcodeViewModel by activityViewModels<PasscodeViewModel>()
    private var cardNumber: String = ""
    private var isError: Boolean = false
    private var isCardRev: Boolean = false
    private var isInsert: Boolean = false
    private var isManualCard: Boolean = false
    private lateinit var binding: FragmentCheckoutDetailsNewBinding
    private val dineInViewModel by viewModels<DineInOrderTableViewModel>()
    private val TAG = "DashboardCategoryBold"

    private var requestCancel: Boolean = false
    private var orderId: Int? = null
    private var orderOfflineId: String = ""
    private var paymentOfflineId: String = ""
    var isSelectedCount = 1
    private val paymentviewModel by activityViewModels<PaymentViewModel>()
    private val giftCardViewModel by activityViewModels<GiftCardViewModel>()
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
    var totalServiceChargeM = 0.0
    var totalDiscountM = 0.0
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
    var ExtData = ""

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

        getCustomerDisplay(requireContext())?.let { display ->
            presentation = CustomDisplay(
                display,
                requireContext(),
                viewLifecycleOwner,
                dashboardViewModel,
                passcodeViewModel,
                dineInViewModel
            )
//            {
//                tipAmount = it
//                tipAmountCalculation()
//            }
        }

        val device = prefProvider.getValueInt(Constants.MAGTEK_HARDWARE, 0)

        if (device == 0) {
            magtekModule.setupInit()
            magtekModule.setCallback(this)
        } else {
            mSessionManager.setOutputFragment(this)

        }


        if (prefProvider.getValueboolean(Constants.IS_PAX_CONNECTED, false)) {
            binding.llManualCardEntry.visibility = View.GONE
        }

        orderId = arguments?.getInt("orderId")

        LogUtil.logE("orderId :: ", orderId.toString())
        if (orderId != null) {
            paymentId = arguments?.getInt("paymentId")!!
            paymentOfflineId = arguments?.getString("paymentOfflineId").toString()
            orderOfflineId = arguments?.getString("orderOfflineId").toString()
        }

        if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) != GIFT_CARD) {
            binding.tvOther.visible()
            binding.lnrGiftCard.visible()
        } else {
            binding.tvOther.gone()
            binding.lnrGiftCard.gone()
        }

//        BroadPOSCommunicator.startListeningService()

        initPOSLink()
//        setCommSetting()

        return binding.root
    }

    private fun initPOSLink() {
        POSLinkCreatorWrapper.createSync(
            context!!,
            object : AppThreadPool.FinishInMainThreadCallback<PosLink?> {
                override fun onFinish(result: PosLink?) {
                    posLink = result!!
                    Log.d("initPOSLink: ","onFinish")
                }
            })
    }

    private val tipListViewModel by activityViewModels<TipListViewModel>()

    override fun onResume() {
        super.onResume()
        if (this::presentation.isInitialized) {
            presentation.show()
            presentation.onDisplayChanged()
            val showCashCreditPrice = prefProvider.getValueboolean(
                Constants.SHOW_CASH_CREDIT_PRICE_ON_CUSTOMER_DISPLAY,
                false
            )
            if (!showCashCreditPrice) {
                presentation.showSurcharge(true)
            }
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
        getDataFromPref()
        setupTabDesign()
        paymentClick()
        splitClick()
        observeShowProgress()
        observeQueueCreate()
        observeData()
        callback()
        setUpManualCardFocusChanged()
        showProgressObserver()
    }

    private fun setUpManualCardFocusChanged() {
        binding.edtCardNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                try {
                    Log.e(TAG, "checkMSfsLOnT ${s?.length}")
                    if (s?.length == 22) {
                        binding.edtMMYY.requestFocus()
                    }

                } catch (e: Exception) {
                }

            }

            override fun afterTextChanged(s: Editable?) {
                try {
                    Log.e(TAG, "checkMSfsL ${s?.length}")
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
                    Log.e(TAG, "CheckYYLength ${s?.length}")
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

        if (prefProvider.getValueboolean(TIP_ADDED, false) && prefProvider.getValue(
                ORDER_TYPE,
                TAKEOUT
            ) != GIFT_CARD
        ) {

            val tip = prefProvider.getValue(TIP_ADDED_AMOUNT, "")
            if (tip.isNotEmpty()) {
                tipAmount = tip.toDouble()
                viewModel.setTipAmount(tipAmount)
                tipID = prefProvider.getValueInt(Constants.TIP_ADDED_ID, tipID)
            }
            tipAmountCalculation()
        }

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

            prefProvider.setValueboolean(Constants.TIP_ADDED, true)
            prefProvider.setValue(Constants.TIP_ADDED_AMOUNT, tipAmount.toString())
            prefProvider.setValueInt(Constants.TIP_ADDED_ID, tipID)

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

        binding.linearNextSplit.setOnSingleClickListener {
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
                LogUtil.logE(TAG, "receiptData: ${Gson().toJson(it.data)}")
                viewModel.redeemLoyaltyInfo = RedeemLoyaltyInfo()
                prefProvider.setValueInt("ORDER_ID", it.data.order.id)
                viewModel.updateActiveOrderFlagClear()

                prefProvider.setValueboolean(IS_GIFT_CARD_REDEEM, false)
                prefProvider.setValueboolean(IS_ORDER_REDEEMABLE_WITH_GIFT_CARD, false)
                prefProvider.setValue(GIFT_CARD_NUMBER, "")
                prefProvider.setValue(GIFT_CARD_PIN, "")

                isInsert = false
                isCardRev = false

                viewModel.setTipAmount(0.0)
                when {
                    paymentType == "Cash" -> {
                        LogUtil.logE("TipAmount 4:: ", tipAmount.toString())

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
                                wholePrice - String.format(
                                    "%.2f",
                                    paymentAmount + cashDiscountSurcharge
                                ).toDouble()
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
                        bundle.putBoolean("isFromActiveOrder", isFromOpenOrder)


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
                        bundle.putBoolean("isDineIn", false)

                        if (remainingAmount == 0.0) {
                            bundle.putDouble("PaidAmount", paymentAmount)
                        } else {
                            bundle.putDouble("PaidAmount", remainingAmount)
                        }
                        Log.d(TAG, "observeData: paidAMount value :  " + paymentAmount)

                        var wholePrice = 0.0
                        if (prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").isNotEmpty()) {
                            wholePrice =
                                prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble()
                        }
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
                                    paymentAmount - (cashDiscountSurcharge/isSelectedCount)
                                ).toDouble()
                            )
                            wholePrice - String.format(
                                "%.2f",
                                paymentAmount - (cashDiscountSurcharge/isSelectedCount)
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

                    paymentType == "External" -> {

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
                                wholePrice - String.format(
                                    "%.2f",
                                    paymentAmount + cashDiscountSurcharge
                                ).toDouble()
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

        giftCardViewModel.giftCardData.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {

                if (it.data != null) {
                    Log.d(TAG, "observeData: SellGiftCardResponse = $it")
                    LogUtil.logE(TAG, "receiptData: ${Gson().toJson(it.data)}")
                    prefProvider.setValueInt("ORDER_ID", it.data.gift_card.id)

                    isInsert = false
                    isCardRev = false

                    viewModel.setTipAmount(0.0)
                    when (paymentType) {
                        "Cash" -> {
                            LogUtil.logE("TipAmount 4:: ", tipAmount.toString())

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
                                    remainingValue =
                                        wholePrice - (custom_paymentAmount - splitChange)
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

                                if (remainingValue <= 0.0) {
                                    remainingValue = 0.0
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


                            bundle.putInt("orderID", it.data.gift_card.id ?: 0)
                            bundle.putParcelable("giftCardReceiptData", it.data)
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

                        "Card" -> {
                            LogUtil.logE("TipAmount 4:: ", tipAmount.toString())

                            val bundle = Bundle()
                            bundle.putBoolean("isDineIn", false)

                            if (remainingAmount == 0.0) {
                                bundle.putDouble("PaidAmount", paymentAmount)
                            } else {
                                bundle.putDouble("PaidAmount", remainingAmount)
                            }
                            Log.d(TAG, "observeData: paidAMount value :  " + paymentAmount)

                            var wholePrice = 0.0
                            if (prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").isNotEmpty()) {
                                wholePrice =
                                    prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble()
                            }
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


                            bundle.putInt("orderID", it.data.gift_card.id ?: 0)
                            bundle.putParcelable("giftCardReceiptData", it.data)
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
                } else {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        requireContext(),
                        message = it.message
                    ) { _, _ ->
                    }
                }


            }
        }

        giftCardViewModel.addValueInGiftCardData.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it.data != null) {
                    Log.d(TAG, "observeData: SellGiftCardResponse = $it")
                    LogUtil.logE(TAG, "receiptData: ${Gson().toJson(it.data)}")
                    prefProvider.setValueInt("ORDER_ID", it.data.gift_card.id)

                    isInsert = false
                    isCardRev = false

                    viewModel.setTipAmount(0.0)
                    when (paymentType) {
                        "Cash" -> {
                            LogUtil.logE("TipAmount 4:: ", tipAmount.toString())

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
                                    remainingValue =
                                        wholePrice - (custom_paymentAmount - splitChange)
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

                                if (remainingValue <= 0.0) {
                                    remainingValue = 0.0
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


                            bundle.putInt("orderID", it.data.gift_card.id ?: 0)
                            bundle.putParcelable("giftCardReceiptData", it.data)
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

                        "Card" -> {
                            LogUtil.logE("TipAmount 4:: ", tipAmount.toString())

                            val bundle = Bundle()
                            bundle.putBoolean("isDineIn", false)

                            if (remainingAmount == 0.0) {
                                bundle.putDouble("PaidAmount", paymentAmount)
                            } else {
                                bundle.putDouble("PaidAmount", remainingAmount)
                            }
                            Log.d(TAG, "observeData: paidAMount value :  " + paymentAmount)

                            var wholePrice = 0.0
                            if (prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").isNotEmpty()) {
                                wholePrice =
                                    prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble()
                            }
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


                            bundle.putInt("orderID", it.data.gift_card.id ?: 0)
                            bundle.putParcelable("giftCardReceiptData", it.data)
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

                } else {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        requireContext(),
                        message = it.message
                    ) { _, _ ->
                    }
                }
            }

        }
    }

    private fun observeShowProgress() {

        paymentviewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {

                LogUtil.logE("observeShowProgress", it.toString())
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

        paymentviewModel.showProgressCash.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {

                LogUtil.logE("observeShowProgress", it.toString())
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }

        giftCardViewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {

                LogUtil.logE("observeShowProgress", it.toString())
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

        giftCardViewModel.showProgressCash.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {

                LogUtil.logE("observeShowProgress", it.toString())
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
        Log.e(
            "checkPaymentAmount",
            "checkPrice   ${paymentAmount}"
        )
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
        if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == GIFT_CARD) {
            if (prefProvider.getValueboolean(Constants.IS_ADD_VALUE_IN_GIFT_CARD, false)) {
                addValueInGiftCardUsingCash()
            } else {
                sellGiftCardUsingCash()
            }
        } else {
            makeCashPayment()
        }
    }

    private fun redeemGiftCard() {
        subTotalPrice = String.format("%.2f", subTotalPrice / isSelectedCount).toDouble()
        totalServiceCharge =
            String.format("%.2f", totalServiceCharge / isSelectedCount).toDouble()
        totalTax = String.format("%.2f", totalTax / isSelectedCount).toDouble()
        totalDiscount = String.format("%.2f", totalDiscount / isSelectedCount).toDouble()
        makeCashPayment()
    }

    private fun getTwoDecimal(value: Double): Double {
        Log.e("csafa", "oewenvalue     ${value}")

        try {
            var tmp = value.toString()
            var tmpIndex = tmp.indexOf(".", 0, true)

            if (tmp.length > tmpIndex + 3) {
                Log.e("getDecimal", "tmpGetDecimal  ${tmp.get(tmpIndex + 3)}")
                if (tmp.get(tmpIndex + 3).toString().toInt() >= 5) {
                    return String.format("%.2f", value).toDouble()
                } else {
                    var data = tmp.substring(0, tmpIndex + 3)
                    return String.format("%.2f", data.toDouble()).toDouble()
                }
            } else {
                return String.format("%.2f", value).toDouble()
            }
        } catch (e: Exception) {
            return value
        }


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
                }
            } else {
                errorDisplay("Payment Amount is zero.")
            }

            //  makePaymentCreditCard()
        }

        binding.llManualCardEntry.setOnSingleClickListener {
            binding.frameLayoutId.visible()
            binding.relativeMain.gone()
            binding.llManualCard.visible()
            isManualCard = true

        }

        binding.lnrGiftCard.setOnSingleClickListener {
            binding.frameLayoutId.visible()
            binding.relativeMain.gone()
            binding.llManualCard.gone()
            binding.llGiftCard.visible()
            isManualCard = false
        }

        binding.tvCash0.setOnSingleClickListener {
            custom_paymentAmount = 0.0

            paymentviewModel.totalPayAmount(
                binding.tvCash0.text.toString().replace("$", "").trim().toDouble()
            )
            paymentAmount = binding.tvCash0.text.toString().replace("$", "").trim().toDouble()
            cashPaymentWithVariation()
        }
        binding.tvCash1.setOnSingleClickListener {

            custom_paymentAmount =
                binding.tvCash1.text.toString().replace("$", "").trim().toDouble()
            cashPaymentWithVariation()
        }
        binding.tvCash2.setOnSingleClickListener {
            custom_paymentAmount =
                binding.tvCash2.text.toString().replace("$", "").trim().toDouble()
            cashPaymentWithVariation()
        }
        binding.tvCash3.setOnSingleClickListener {

            custom_paymentAmount =
                binding.tvCash3.text.toString().replace("$", "").trim().toDouble()
            cashPaymentWithVariation()
        }
        binding.tvCustomAmount.setOnSingleClickListener {

            paymentAmount = binding.tvCash0.text.toString().replace("$", "").trim().toDouble()
            val bundleVal = Bundle().apply {
                putDouble("totalprice", ((paymentAmount + tipAmount)))
            }
            findNavController().navigate(
                R.id.action_paymentBoldPosFragment_to_customAmountFragment,
                bundleVal
            )

        }
        binding.tvPaymentLink.setOnSingleClickListener {
            textToPay = true

            custom_paymentAmount = 0.0

            paymentviewModel.totalPayAmount(
                binding.tvCash0.text.toString().replace("$", "").trim().toDouble()
            )
            paymentAmount = binding.tvCash0.text.toString().replace("$", "").trim().toDouble()
            cashPaymentWithVariation()


        }


        binding.imgBackManualCard.setOnSingleClickListener {
            MethodUtils.hideKeyboard(requireActivity())
            isManualCard = false
            binding.relativeMain.visible()
            binding.llManualCard.gone()

        }

        binding.imgBackGiftCard.setOnSingleClickListener {
            MethodUtils.hideKeyboard(requireActivity())
            isManualCard = false
            binding.relativeMain.visible()
            binding.llGiftCard.gone()

        }

        binding.txtCharge.setOnSingleClickListener {

            MethodUtils.hideKeyboard(requireActivity())

            subTotalPrice = String.format("%.2f", subTotalPrice / isSelectedCount).toDouble()
            totalServiceCharge =
                String.format("%.2f", totalServiceCharge / isSelectedCount).toDouble()
            totalTax = String.format("%.2f", totalTax / isSelectedCount).toDouble()
            totalDiscount = String.format("%.2f", totalDiscount / isSelectedCount).toDouble()
            cashDiscountSurcharge = String.format("%.2f", cashDiscountSurcharge).toDouble()
            paymentAmount = String.format("%.2f", WholetotalPrice / isSelectedCount).toDouble()
            Log.d(TAG, "paymentClick: cashDiscountSurcharge " + cashDiscountSurcharge)
            Log.d(TAG, "paymentClick: paymentAmount  " + paymentAmount)
            if (cashDiscountType == "SurCharge") {
                paymentAmount =
                    String.format("%.2f", paymentAmount + (cashDiscountSurcharge / isSelectedCount))
                        .toDouble()
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

        binding.txtChargeGC.setOnSingleClickListener {
            val giftCardNumber = binding.edtGiftCardNumber.rawText.toString().trim()

            if (giftCardNumber.isEmpty() || giftCardNumber.length != 8) {
                AlertUtils.showCustomAlert(
                    requireContext(),
                    "Please enter 8-digit gift card number"
                )
                return@setOnSingleClickListener
            } else {
                giftCardViewModel.giftCardCheckBalance(GiftCardCheckBalanceRequest(name = giftCardNumber))
            }
        }
    }

    private fun connectBP(){
        BroadPOSCommunicator.getInstance(activity)
            .startListeningService(object : StartListenerCallBack {
                override fun onSuccess() {
                    Toast.makeText(context, "Successful StartListenerCallBack", Toast.LENGTH_SHORT).show()
                }

                override fun onFail(msg: String) {
                    Toast.makeText(context, "Failed StartListenerCallBack", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showProgressObserver() {
        giftCardViewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                LogUtil.logE("observeShowProgress", it.toString())
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }

        giftCardViewModel.giftCardCheckBalanceData.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it.data != null) {

                    if (it.data.amount == 0.0) {
                        binding.edtGiftCardNumber.setText("")
                        prefProvider.setValueboolean(IS_GIFT_CARD_REDEEM, false)
                        AlertUtils.showCustomAlertWithListenerWithOK(
                            requireContext(),
                            message = getString(R.string.msg_insufficient_gift_card_balance)
                        ) { _, _ ->
                        }
                    } else {
                        custom_paymentAmount = 0.0

                        val actualTotalAmountWithTip = (WholetotalPrice / isSelectedCount) +  tipAmount

                        val giftCardBalanceAmount = it.data.amount

                        if (actualTotalAmountWithTip < giftCardBalanceAmount) {
                            val giftCardNumber =
                                binding.edtGiftCardNumber.rawText.toString().trim()
                            prefProvider.setValueboolean(IS_GIFT_CARD_REDEEM, true)
                            prefProvider.setValue(GIFT_CARD_NUMBER, giftCardNumber)
                            prefProvider.setValue(GIFT_CARD_PIN, "")
                            prefProvider.setValueboolean(
                                IS_ORDER_REDEEMABLE_WITH_GIFT_CARD,
                                true
                            )
                            val actualTotalAmount = (WholetotalPrice / isSelectedCount)
                            paymentAmount = actualTotalAmount
                            paymentviewModel.totalPayAmount(paymentAmount)
                            redeemGiftCard()
                        } else {
                            prefProvider.setValueboolean(
                                IS_ORDER_REDEEMABLE_WITH_GIFT_CARD,
                                false
                            )
                            AlertUtils.showCustomAlertWithListenerWithOK(
                                requireContext(),
                                message = "Your GiftCard Balance is $${giftCardBalanceAmount.toPrecision(2)}. Please use split payment."
                            ) { _, _ ->
                            }
                        }
                        binding.edtGiftCardNumber.setText("")
                    }
                } else {
                    binding.edtGiftCardNumber.setText("")
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        requireContext(),
                        message = it.message
                    ) { _, _ ->
                    }
                }

            }
        }
    }

        /*private fun setCommSetting() {
        //create commsetting object

        var file = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val iniFile = "/storage/emulated/0/Download/"+ SettingINI.FILENAME
        *//*val iniFile =
            activity!!.applicationContext.filesDir.absolutePath + "/" + SettingINI.FILENAME*//*
        val commset: CommSetting = SettingINI.getCommSettingFromFile(iniFile)
        Log.d("iniFile: ","iniFile $iniFile ${file.absolutePath}")

        //initialization value  for comsetting's attribute
        commset.type = CommSetting.TCP
        commset.timeOut = "-1"
        commset.baudRate = "9600"
//        commset.serialPort = "COM1"
        commset.isEnableProxy = false
        commset.destPort = prefProvider.getValue(
            Constants.PAX_PORT,
            ""
        )
        commset.destIP = prefProvider.getValue(
            Constants.PAX_IP,
            ""
        )
        *//*val selectedHost = "UNKNOWN"
        Convenience.setHost(context, commset, selectedHost)*//*
        Log.i(
            "TAG", "coms.CommType = " + commset.type + "; coms.TimeOut=" + commset.timeOut
                    + "; SerialPort=" + commset.serialPort + "; coms.BaudRate=" + commset.baudRate
                    + "; coms.DestIP=" + commset.destIP + "; coms.DestPort=" + commset.destPort + "; coms.MacAddr=" + commset.macAddr + "; coms.EnableProxy=" + commset.isEnableProxy
        )
        POSLinkAndroid.initPOSListener(context, commset)
        SettingINI.saveCommSettingToFile(iniFile, commset)
        // set the folder to save the "comsetting.ini" file
        posLink.appDataFolder = file.absolutePath
        posLink.SetCommSetting(commset)
        Log.d("SetCommSetting: ", "saved successfully")
    }*/

    private fun makePaxPaymentRequest() {
        GlobalScope.launch {
            Log.d("getCommSettingFromFile ","getCommSettingFromFile: "+Gson().toJson(SettingINI.getCommSettingFromFile("/storage/emulated/0/Download/"+ SettingINI.FILENAME)))
            posLink.SetCommSetting(SettingINI.getCommSettingFromFile("/storage/emulated/0/Download/"+ SettingINI.FILENAME))
            val amt = ((paymentAmount-tipAmount)*100).toInt()
            val tip_amt = (tipAmount*100).toInt()
            Log.d("Amt: ","amt $amt tip $tip_amt")

            CoroutineScope(Dispatchers.Main).launch {
                ProgressUtils.showProgressDialog(requireActivity())
            }
            mPaymentRequest = PaymentRequest()
            mPaymentRequest.TransType = mPaymentRequest.ParseTransType("SALE")
            mPaymentRequest.TenderType = mPaymentRequest.ParseTenderType("CREDIT")
            mPaymentRequest.Amount = amt.toString()
            mPaymentRequest.TipAmt = tip_amt.toString()
            mPaymentRequest.ECRRefNum = System.currentTimeMillis().toString()
            mPaymentRequest.ExtData = "<Force>T</Force>"
            Log.d("ECRRefNum", "ECRRefNum: ${System.currentTimeMillis().toString()}")

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

                //implementation("org.dom4j:dom4j:2.1.3")
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
                        requireActivity().toast("$resultCode $resultTxt", Toast.LENGTH_LONG)
                    }
                }
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    ProgressUtils.dismissProgressDialog()
                    if (result.Msg.toString() == "CONNECT ERROR" || result.Msg.toString() == "TIME OUT"){
                        Toast.makeText(requireContext(), "Please check your internet connection", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(requireContext(), "getMerchantDetails Failed ${result.Code} ${result.Msg}", Toast.LENGTH_LONG).show()
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

    private fun loadManualCardEntryFragment(fragment: Fragment) {
        val fm: FragmentManager = requireActivity().supportFragmentManager
        fm.beginTransaction().replace(R.id.frameLayoutId, fragment).commit()
    }

    fun getDataFromPref() {
        redeemLoyaltyInfo = viewModel.redeemLoyaltyInfo
        if (prefProvider.getValue(Constants.WHOLE_AMOUNT, "")
                .isEmpty() || prefProvider.getValue(
                Constants.WHOLE_AMOUNT,
                ""
            ) == "0.0"
        ) {
            Log.e("AmtviewModeltotalPrice", "totalPrice  ${viewModel.totalPrice}")
            viewModel.totalServiceCharge =
                String.format("%.2f", viewModel.totalServiceCharge).toDouble()
            WholetotalPrice = viewModel.subTotalPrice + viewModel.totalTax + String.format(
                "%.2f",
                viewModel.totalServiceCharge
            ).toDouble()
            if (redeemLoyaltyInfo?.needToApplyLoyalty == true ) {
                WholetotalPrice -= redeemLoyaltyInfo?.usedLoyaltyAmount!!
                viewModel.totalPrice = WholetotalPrice
            } else {
                viewModel.totalPrice = WholetotalPrice
            }
            Log.e("checkWhole", "WholetotalPrice:  ${WholetotalPrice}")
            Log.e("checkWhole", "subTotalPrice:  ${viewModel.subTotalPrice}")
            Log.e("checkWhole", "totalServiceCharge:  ${viewModel.totalServiceCharge}")
            Log.e("checkWhole", "totalTax:  ${viewModel.totalTax}")
            Log.e("checkWhole", "totalDiscount:  ${viewModel.totalDiscount}")
            WholetotalPrice = String.format("%.2f", WholetotalPrice).toDouble()
            Log.e("checkWholePrice", "WholetotalPrice:  ${WholetotalPrice}")

            prefProvider.setValue(
                Constants.WHOLE_AMOUNT,
                String.format("%.2f", getTwoDecimal(viewModel.totalPrice))
            )
        } else {
            WholetotalPrice = prefProvider.getValue(Constants.WHOLE_AMOUNT, "").toDouble()
            viewModel.totalPrice = WholetotalPrice
        }

        WholetotalPrice = getTwoDecimal(WholetotalPrice)
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
            prefProvider.setValue(
                Constants.TAX_CHARGE,
                String.format("%.2f", viewModel.totalTax)
            )
        } else {
            totalTax = prefProvider.getValue(Constants.TAX_CHARGE, "").toDouble()
        }


        if (prefProvider.getValue(Constants.SERVICE_CHARGE, "")
                .isEmpty() || prefProvider.getValue(
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


        if (prefProvider.getValue(Constants.TOTAL_DISCOUNT, "")
                .isEmpty() || prefProvider.getValue(
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
            ) == "0.00"
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

        //Added (&& condition to check name) by Dharmesh to resolve issue BIS-352
        viewModel.ordertypelist.forEach {
            if (prefProvider.getOrderTypeName(
                    Constants.ORDER_TYPE, DEFAULT_ORDER
                ) == it.orderType && prefProvider.getOrderTypeName(
                    Constants.ORDER_TYPE_NAME, DEFAULT_ORDER
                ) == it.name
            ) {
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


        MethodUtils.setPriceTextViewDown(
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
        Log.e(TAG, "WholetotalPrice:   ${WholetotalPrice}")
        MethodUtils.setPriceTextView(
            binding.tvCard,
            getCalCashDiscWithAmount(WholetotalPrice, false) / isSelectCount
        )
        if (this::presentation.isInitialized) {
            presentation.show()
            presentation.updateTotals(
                binding.tvCash.text.toString(),
                binding.tvCard.text.toString()
            )
        }
        binding.tvCash.text = "Cash (" + binding.tvCash.text + ")"
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
            if (this::presentation.isInitialized) {
                presentation.show()
                presentation.updateTotals(
                    binding.tvCash.text.toString(),
                    binding.tvCard.text.toString()
                )
            }
            binding.tvCash.text = "Cash (" + binding.tvCash.text + ")"
            binding.tvCard.text = "Card (" + binding.tvCard.text + ")"
            MethodUtils.setPriceTextViewDown(
                binding.tvAmount,
                getCalCashDiscWithAmount(
                    prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble(), true
                )
            )
        } else {
            if (this::presentation.isInitialized) {
                presentation.show()
                presentation.showTipsAdded(tipAmount, WholetotalPrice)
            }

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
            if (this::presentation.isInitialized) {
                presentation.show()
                presentation.updateTotals(
                    binding.tvCash.text.toString(),
                    binding.tvCard.text.toString()
                )
            }
            binding.tvCash.text =
                "Cash (" + binding.tvCash.text + ")"
            binding.tvtipcash?.visible()
            binding.tvtipcash?.text =
                "(" + MethodUtils.roundOffAmount(tipAmount) + " Tip Added)"
            binding.tvCard.text =
                "Card (" + binding.tvCard.text + ")"
            binding.tvtipcard?.visible()
            binding.tvtipcard?.text =
                "(" + MethodUtils.roundOffAmount(tipAmount) + " Tip Added)"
            MethodUtils.getCashPaymentOptionList(
                (getCalCashDiscWithAmount(WholetotalPrice, true) / isSelectedCount) + tipAmount,
                binding.tvCash1,
                binding.tvCash2,
                binding.tvCash3
            )
            MethodUtils.setPriceTextViewDown(
                binding.tvAmount,
                (getCalCashDiscWithAmount(
                    prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble(), true
                ) / isSelectedCount).toDouble() + tipAmount
            )
            binding.tvAmount.text =
                binding.tvAmount.text.toString()
            binding.tvsplittip?.visible()
            binding.tvsplittip?.text =
                "(" + MethodUtils.roundOffAmount(tipAmount) + " Tip Added)"
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
                if (totalprice - cashDiscountSurcharge < 0.0) {
                    0.0
                } else {
                    totalprice - cashDiscountSurcharge
                }
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
            MethodUtils.setPriceTextViewDown(
                binding.tvAmount,
                getCalCashDiscWithAmount(
                    prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble(), true
                ) / isSelectCount
            )
        } else {
            MethodUtils.setPriceTextViewDown(
                binding.tvAmount,
                (getCalCashDiscWithAmount(
                    prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble(), true
                ) / isSelectCount) + tipAmount
            )
            binding.tvAmount.text =
                binding.tvAmount.text.toString()
            binding.tvsplittip?.visible()
            binding.tvsplittip?.text =
                "(" + MethodUtils.roundOffAmount(tipAmount) + " Tip Added)"
        }
    }

    private fun setupTabDesign() {

        if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == GIFT_CARD) {
            PaymentBoldPosFragment.newInstance().addTipHideShow(true)
            binding.linearTab2.gone()
        } else {
            PaymentBoldPosFragment.newInstance().addTipHideShow(false)
            binding.linearTab2.visible()
        }

        binding.linearTab1.setOnSingleClickListener {
            if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) != GIFT_CARD) {
                PaymentBoldPosFragment.newInstance().addTipHideShow(false)
            }
            isSelectedCount = 1
            tipsetupGlobal(tipAmount, isSelectedCount)
            loadPaymentLayout()
            tipAmountCalculation()
        }
        binding.linearTab2.setOnSingleClickListener {

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
        paymentAmount -= tipAmount
        paymentAmount = MethodUtils.roundOffAmountDouble(paymentAmount)
        paymentType = "Card"
        LogUtil.logE(TAG, "cartList:  ${Gson().toJson(cartList)}")
        LogUtil.logE(TAG, "cartListcartItems:  ${Gson().toJson(cartItems)}")
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
                prefProvider.getValue(Constants.ORDER_TYPE, ""),
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
                tipID,
                GlobalUID,
                RefNumber,
                ExtData
            )
        }
        LogUtil.logE(TAG, "myRequestOriginal ${Gson().toJson(myRequest)}")
        if (myRequest != null) {
            paymentviewModel.totalPayAmount(paymentAmount)
            paymentAttributesRequest(myRequest)
        }
    }

    private fun makeCashPayment() {
        paymentType = if (prefProvider.getValueboolean(IS_GIFT_CARD_REDEEM, false)) {
            "External"
        } else {
            "Cash"
        }

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
        paymentviewModel.textPay(textToPay)
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
                prefProvider.getValue(Constants.ORDER_TYPE, ""),
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
        LogUtil.logE("ORDER TYPE 1", prefProvider.getValue(Constants.ORDER_TYPE, ""))
        if (myRequest != null) {
            if (custom_paymentAmount != 0.0) {
                paymentviewModel.totalPayAmount(custom_paymentAmount)
            }
            paymentAttributesRequest(myRequest)
        }
    }

    private fun paymentAttributesRequest(myRequest: OrderRequestModel) {
        val orderId = prefProvider.getValueInt("ORDER_ID", -1)
        LogUtil.logE(TAG, "orderIdmyRequestOriginal ${orderId}")
        Log.e("textToPay", textToPay.toString())
        if (orderId == -1) {
            if (textToPay) {
                myRequest.completed_all_payments = false
            } else if (prefProvider.getValueboolean(IS_GIFT_CARD_REDEEM, false)) {
                myRequest.completed_all_payments = prefProvider.getValueboolean(
                    Constants.IS_ORDER_REDEEMABLE_WITH_GIFT_CARD,
                    false
                ) && isSelectedCount <= 1
            } else {
                if (myRequest.order.totalAmount != 0.0) {
                    myRequest.completed_all_payments = isSelectedCount <= 1
                } else {
                    myRequest.completed_all_payments = true
                }
            }
            paymentviewModel.submit(myRequest)
        } else {

            if (textToPay) {

                paymentviewModel.textPaySplit(orderId)

            } else {

                val paymentReq = myRequest.order.paymentAttributes
                if (paymentReq != null) {
                    paymentReq.order_id = orderId
                }

                var giftCardRedeem: SpitByOrderRequestModel.GiftCardRedeem? = null

                if (prefProvider.getValueboolean(IS_GIFT_CARD_REDEEM, false)) {
                    giftCardRedeem = SpitByOrderRequestModel.GiftCardRedeem(
                        prefProvider.getValue(
                            GIFT_CARD_NUMBER,
                            ""
                        ), prefProvider.getValue(GIFT_CARD_PIN, "")
                    )
                }

                // total amount - (hal pay amoutn + alredy pay )
                val aa = SpitByOrderRequestModel(
                    orderId, isSelectedCount <= 1,
                    SpitByOrderPaymentModel(
                        listOf(paymentReq) as List<PaymentAttributes>,
                    ),
                    gift_card_redeem = prefProvider.getValueboolean(IS_GIFT_CARD_REDEEM, false),
                    gift_card = giftCardRedeem
                )

                paymentviewModel.splitByOrder(aa, false)
            }
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
                    (paymentAmount * 100),
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
                    LogUtil.logE("onResponse", Gson().toJson(response.body()))
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
                            giftCardViewModel.setMagensaResponse(
                                Gson().toJson(response.body()!![0]),
                                if (i == 3) cardNumber else ""
                            )

                            if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == GIFT_CARD) {
                                if (prefProvider.getValueboolean(
                                        Constants.IS_ADD_VALUE_IN_GIFT_CARD,
                                        false
                                    )
                                ) {
                                    addValueInGiftCardUsingCard()
                                } else {
                                    sellGiftCardUsingCard()
                                }
                            } else {
                                makePaymentCreditCard()
                            }

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

    private fun sellGiftCardUsingCash() {
        paymentType = "Cash"
        val myRequest = cartList?.let {
            giftCardViewModel.createSellGiftCardRequestUsingCash()
        }
        if (myRequest != null) {
            giftCardViewModel.sellGiftCard(myRequest)
        }
    }

    private fun sellGiftCardUsingCard() {
        paymentType = "Card"
        val myRequest = cartList?.let {
            giftCardViewModel.createSellGiftCardRequestUsingCard()
        }
        if (myRequest != null) {
            giftCardViewModel.sellGiftCard(myRequest)
        }
    }

    private fun addValueInGiftCardUsingCash() {
        paymentType = "Cash"
        val myRequest = cartList?.let {
            giftCardViewModel.createAddValueInGiftCardRequestUsingCash()
        }
        if (myRequest != null) {
            giftCardViewModel.addValueInGiftCard(true, myRequest)
        }
    }

    private fun addValueInGiftCardUsingCard() {
        paymentType = "Card"
        val myRequest = cartList?.let {
            giftCardViewModel.createAddValueInGiftCardRequestUsingCard()
        }
        if (myRequest != null) {
            giftCardViewModel.addValueInGiftCard(false, myRequest)
        }
    }

    override fun OnARQCReceived(data: ByteArray) {


        magtekModule.stopListner(true)

        isInsert = true

        ProgressUtils.dismissProgressDialog()

        val jsonArray1 = magtekRequestUtils.processData(
            (paymentAmount * 100),
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
                            LogUtil.logE("", "[CONNECTED]")

//                            ProgressUtils.dismissProgressDialog()
                            prefProvider.setValueboolean(Constants.DYNANA_FLAX, true)

                            ProgressUtils.showProgressDialog(
                                "Please tap, insert or swipe card",
                                requireActivity()
                            )
                            ProgressUtils.setCallback(this)
                            startTransaction()
                        }

                        ConnectionState.Disconnected -> {
                            LogUtil.logE("", "[DISCONNECTED]")
                            ProgressUtils.dismissProgressDialog()

                            mSessionManager.isConnected = false
                            prefProvider.setValueboolean(Constants.DYNANA_FLAX, false)
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

                    println("TransactionResult : " + MTParser.getHexString(data.ByteArray()))
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
                            AlertUtils.showCustomAlert(
                                requireContext(),
                                "TRANSACTION TIMED OUT"
                            )
                            //  ProgressUtils.dismissProgressDialog()
                            dismissDialog()
                        }

                        TransactionStatus.HostCancelled -> {
                            AlertUtils.showCustomAlert(requireContext(), "HOST CANCELLED")
                            // ProgressUtils.dismissProgressDialog()
                            dismissDialog()
                        }

                        TransactionStatus.TransactionCancelled -> {
                            AlertUtils.showCustomAlert(
                                requireContext(),
                                "TRANSACTION CANCELLED"
                            )
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


        LogUtil.logE("mSessionManager", mSessionManager.isConnected.toString())
        if (mSessionManager.isConnected) {

            ProgressUtils.showProgressDialog(
                "Please tap, insert or swipe card",
                requireActivity()
            )
            ProgressUtils.setCallback(this)

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
        } else {
            if (!isShow) {
                isShow = true
                AlertUtils.showCustomAlert(requireContext(), "Please connect payment device.")
            } else {
                isShow = false
            }
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
            0,
        )

        val currencyCode = byteArrayOf(0x08, 0x40)
        transaction.setCurrencyCode(currencyCode)
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

    private fun observeQueueCreate() {
        paymentviewModel.queueStartSaveOrder.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                createQueuePrinter(it)
            }
        }
    }

    private fun createQueuePrinter(createOrder: CreateOrderResponse) {
        val listPrinter: List<Int> = listOf()
        LogUtil.logE(TAG, "cartListcartList  ${Gson().toJson(cartList)}")
        if (cartList != null) {
            val orderRequest = cartList?.let {

                paymentviewModel.createOrderRequest(
                    it,
                    viewModel.subTotalPrice,
                    viewModel.totalPrice,
                    viewModel.totalServiceCharge,
                    viewModel.totalTax,
                    prefProvider.getValue(Constants.ORDER_TYPE, "").toString(),
                    "",
                    "",
                    false,
                    viewModel.totalDiscount,
                    0.0,
                    0,
                    null,
                    0.0,
                    false,
                    "Cash",
                    cashDiscountType,
                    isPrinterQueue = true,
                    offlineId = createOrder.data.order.offlineId
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

    override fun onInputAccountStart() {
        Log.d("onInputAccountStart","onInputAccountStart")
    }

    override fun onEnterExpiryDate() {
        Log.d("onEnterExpiryDate","onEnterExpiryDate")
    }

    override fun onEnterZip() {
        Log.d("onEnterZip","onEnterZip")
    }

    override fun onEnterCVV() {
        Log.d("onEnterCVV","onEnterCVV")
    }

    override fun onSelectEMVApp(p0: MutableList<String>?) {
        Log.d("onSelectEMVApp","onSelectEMVApp ${p0.toString()}")
    }

    override fun onProcessing(p0: String?, p1: String?) {
        Log.d("onProcessing","onProcessing $p0 $p1")
    }

    override fun onWarnRemoveCard() {
        Log.d("onWarnRemoveCard","onWarnRemoveCard")
    }

    override fun onFinish(p0: InputAccount.InputAccountResponse?) {
        Log.d("InputAccount onFinish","onFinish ${p0.toString()}")
    }
}