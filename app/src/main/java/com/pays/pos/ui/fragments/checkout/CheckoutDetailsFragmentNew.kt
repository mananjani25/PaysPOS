package com.pays.pos.ui.fragments.checkout

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.reflect.TypeToken
import com.magtek.mobile.android.mtlib.IMTCardData
import com.magtek.mobile.android.mtlib.MTConnectionState
import com.magtek.mobile.android.mtusdk.ConnectionState
import com.magtek.mobile.android.mtusdk.ConnectionStateBuilder
import com.magtek.mobile.android.mtusdk.CoreAPI
import com.magtek.mobile.android.mtusdk.DeviceType
import com.magtek.mobile.android.mtusdk.EventType
import com.magtek.mobile.android.mtusdk.IData
import com.magtek.mobile.android.mtusdk.IDevice
import com.magtek.mobile.android.mtusdk.IDeviceListCallback
import com.magtek.mobile.android.mtusdk.Transaction
import com.magtek.mobile.android.mtusdk.TransactionBuilder
import com.magtek.mobile.android.mtusdk.TransactionStatus
import com.magtek.mobile.android.mtusdk.TransactionStatusBuilder
import com.pax.poslink.ManageRequest
import com.pax.poslink.PaymentRequest
import com.pax.poslink.PosLink
import com.pax.poslink.ProcessTransResult
import com.pax.poslink.aidl.BasePOSLinkCallback
import com.pax.poslink.broadpos.BroadPOSCommunicator
import com.pax.poslink.fullIntegration.InputAccount
import com.pax.poslink.fullIntegration.InputAccount.InputAccountCallback
import com.pays.payments.design.Dejavoo
import com.pays.payments.design.PaymentGatewayFactory
import com.pays.payments.design.PaymentGatewayType
import com.pays.payments.design.TransactionType
import com.pays.payments.design.Valor
import com.pays.pos.R
import com.pays.pos.data.entities.CartModel
import com.pays.pos.data.entities.PAXData
import com.pays.pos.data.entities.RedeemLoyaltyInfo
import com.pays.pos.data.entities.TaxData
import com.pays.pos.data.entities.TbCartItem
import com.pays.pos.data.entities.TbCustomer
import com.pays.pos.data.entities.TbDynamicPaymentRecords
import com.pays.pos.data.entities.TbItem
import com.pays.pos.data.model.requestModel.*
import com.pays.pos.data.model.requestModel.giftCard.request.GiftCardCheckBalanceRequest
import com.pays.pos.data.model.responseModel.CreateOrderResponse
import com.pays.pos.data.model.valor.ValorSuccessResponse
import com.pays.pos.data.remote.ApiService
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.DEFAULT_ORDER
import com.pays.pos.data.remote.Constants.DO_PRINT
import com.pays.pos.data.remote.Constants.GIFT_CARD
import com.pays.pos.data.remote.Constants.GIFT_CARD_NUMBER
import com.pays.pos.data.remote.Constants.GIFT_CARD_PIN
import com.pays.pos.data.remote.Constants.IS_GIFT_CARD_REDEEM
import com.pays.pos.data.remote.Constants.IS_ORDER_REDEEMABLE_WITH_GIFT_CARD
import com.pays.pos.data.remote.Constants.IS_PAX_PAYMENT_FAILED
import com.pays.pos.data.remote.Constants.ORDER_TYPE
import com.pays.pos.data.remote.Constants.PRE_AUTH_DETAILS
import com.pays.pos.data.remote.Constants.TAKEOUT
import com.pays.pos.data.remote.Constants.TIP_ADDED
import com.pays.pos.data.remote.Constants.TIP_ADDED_AMOUNT
import com.pays.pos.databinding.FragmentCheckoutDetailsNewBinding
import com.pays.pos.di.ApiModule1
import com.pays.pos.di.MagtekModule
import com.pays.pos.di.PrefProvider
import com.pays.pos.logger.CashBoxEvent
import com.pays.pos.logger.MessageEvent
import com.pays.pos.ui.activities.MainActivity
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.pays.pos.ui.fragments.dashboard.bolddashboard.CustomDisplay
import com.pays.pos.ui.fragments.dinein.DineInOrderTableViewModel
import com.pays.pos.ui.fragments.eGiftCard.GiftCardViewModel
import com.pays.pos.ui.fragments.loginscreen.PasscodeViewModel
import com.pays.pos.ui.fragments.magtek.MagtekRequestUtils
import com.pays.pos.ui.fragments.magtek.MagtekViewModel
import com.pays.pos.ui.fragments.magtek.PaymentResponse
import com.pays.pos.ui.fragments.magtekPro.MTParser
import com.pays.pos.ui.fragments.magtekPro.SessionManager
import com.pays.pos.ui.fragments.payment.PaymentBoldPosFragment
import com.pays.pos.ui.fragments.payment.PaymentViewModel
import com.pays.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper
import com.pays.pos.ui.fragments.settings.tip.TipListViewModel
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.CardValidator
import com.pays.pos.utils.Event
import com.pays.pos.utils.InternetUtils
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.MethodUtils.Companion.toPrecision
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.TLVParser
import com.pays.pos.utils.callback.DeleteOptionCallback
import com.pays.pos.utils.callback.magtekCallback
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.invisible
import com.pays.pos.utils.extensions.runOnUiThread
import com.pays.pos.utils.extensions.setOnSingleClickListener
import com.pays.pos.utils.extensions.visible
import com.pays.pos.utils.getCustomerDisplay
import com.pays.pos.utils.paxUtils.AppThreadPool
import com.pays.pos.utils.paxUtils.POSLinkCreatorWrapper
import com.pays.pos.utils.paxUtils.SettingINI
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.StringReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.roundToInt


@AndroidEntryPoint
class CheckoutDetailsFragmentNew(val isFromOpenOrder: Boolean = false) : Fragment(), magtekCallback,
    DeleteOptionCallback, IDeviceListCallback, InputAccountCallback,
    BasePOSLinkCallback<InputAccount.InputAccountResponse> {
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

    //    private val viewModel by activityViewModels<DashBoardCategoryViewModel>()
    private val magtekProViewModel by viewModels<MagtekViewModel>()

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
    var tipAmountToPaymentDevice = 0.0
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
    var retryCount = 1

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

    var oldItems = ""

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

    private var orderTypeToCheckKioskOrder: String = ""


    //Tip Before
    private var surchargeOnTip = 0.0

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
        }

        dashboardViewModel.paymentInProgress.value = false
        dashboardViewModel.tipBeforeEnabled = true

        val device = prefProvider.getValueInt(Constants.MAGTEK_HARDWARE, 0)

        dashboardViewModel.cashDiscountType = prefProvider.getValue(Constants.OPTION_TYPE, "")
        cashDiscountType = prefProvider.getValue(Constants.OPTION_TYPE, "")

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
        oldItems = arguments?.getString(Constants.OLD_ITEM) + ""

        /*if user will move to the old screen, then this will value will be found in the preferences, otherwise it was getting cleared */
        prefProvider.setValue(Constants.OLD_ITEM, oldItems)

        LogUtil.logE("orderId :: ", orderId.toString())
        if (orderId != null) {
            paymentId = arguments?.getInt("paymentId")!!
            paymentOfflineId = arguments?.getString("paymentOfflineId").toString()
            orderOfflineId = arguments?.getString("orderOfflineId").toString()
        }

        orderTypeToCheckKioskOrder = arguments?.getString("orderType_to_check_kiosk") ?: ""

        if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) != GIFT_CARD) {
            /* For now the gift card button is hidden, when required the below code will be uncommented*/
            binding.tvOther.visible()
//            binding.lnrGiftCard.visible()
        } else {
            binding.tvOther.gone()
            binding.lnrGiftCard.gone()
        }

//        BroadPOSCommunicator.startListeningService()

        initPOSLink()
        getMerchantDataObserver()
        setLoyaltyEarnedObserver()
//        setCommSetting()

        setPaymentErrorHandlerForCustomerIssue()
        initProgressObserver()
        initClickListener()
        return binding.root
    }

    private fun setPaymentErrorHandlerForCustomerIssue() {
        paymentviewModel.orderFailedDueToCustomerObservable.observe(requireActivity(),object :Observer<Event<OrderRequestModel>>{
            override fun onChanged(event: Event<OrderRequestModel>?) {
                event?.getContentIfNotHandled().let {
                    syncCustomerWithServer(it)
                }
            }

            private fun syncCustomerWithServer(it: OrderRequestModel?) {
                it?.order?.customer_id?.let {customerId->

                    /*--------------Set observer--------------*/
                    dashboardViewModel.createCustomerObservable.observe(viewLifecycleOwner,object : Observer<Pair<Boolean,OrderRequestModel?>>{
                        override fun onChanged(t: Pair<Boolean, OrderRequestModel?>?) {
                            t?.let {
                                if (it.first && it.second!=null){
                                    paymentviewModel.submit(it.second!!)
                                }else{
                                    CoroutineScope(Dispatchers.Main).launch {
                                        dismissProgressDialog()
                                        ProgressUtils.dismissProgressDialog()
                                    }
                                }
                            }
                        }
                    })
                    /*--------------Set observer--------------*/

                    var customer:TbCustomer?=null
                    runBlocking {
                        async {
                            dashboardViewModel.getCustomerDetailsFromId(customerId).value?.let {
                                customer = it
                            }
                        }.await()
                    }

                    customer?.let {nonNullCustomer->
                        var customerToCreate = CreateCustomerRequestModel()
                        customerToCreate.data?.apply {
                            first_name = nonNullCustomer.first_name?:""
                            last_name = nonNullCustomer.last_name?:""
                            company = nonNullCustomer.company?:""
                            email=nonNullCustomer.email
                            var phonesList= arrayListOf<CreateCustomerRequestModel.Customer.Phone>()
                            nonNullCustomer.phones.forEach {
                                phonesList.add(CreateCustomerRequestModel.Customer.Phone(it.id,it.phone_number))
                            }
                            phones_attributes=phonesList

                            var addresses = arrayListOf<CreateCustomerRequestModel.Customer.Addresses>()
                            nonNullCustomer.addresses.forEach {
                                addresses.add(CreateCustomerRequestModel.Customer.Addresses(it.id,it.address1,it.address2,it.city,it.state,it.country,it.postcode,it.type_of_address,it.latitude.toDouble(),it.longitude.toDouble()))
                            }
                            addresses_attributes=addresses
                        }
                        dashboardViewModel.createCustomer(customerToCreate, orderRequestModel = it)
                    }

                }

            }
        })

    }

    private fun initProgressObserver() {
        giftCardViewModel.showGiftCardProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                Log.e("ObserverdGiftCardProgress", it.toString())
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())

                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }
    }
    private fun showAlertDialog(message:String){
        runOnUiThread(kotlinx.coroutines.Runnable {
            AlertUtils.showCustomAlert(requireContext(), message)
        })
    }

    private var countDownTimer: CountDownTimer? = null
    private fun initClickListener() {
        binding.btnReadCard.setOnSingleClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {

                when(prefProvider.getValue(Constants.PAYMENT_GATEWAY_TYPE,"")){
                    Constants.PAX->{
                        if (prefProvider.getValueboolean(Constants.IS_PAX_CONNECTED,false)) {
                            countDownTimer?.cancel()
                            binding.btnReadCard?.isClickable = false

                            countDownTimer = object : CountDownTimer(5000, 1000) {
                                override fun onTick(millisUntilFinished: Long) {
                                }

                                override fun onFinish() {
                                    binding.btnReadCard?.isClickable = true
                                }
                            }.start()

                            transactionInProgress()
                            startPAXWithGiftCard()
                        }else{
                            showAlertDialog(getString(R.string.please_connect_pax))
                        }
                    }
                    Constants.DEJAVOO->{
                        showAlertDialog(getString(R.string._not_supported,Constants.DEJAVOO))
                    }
                    Constants.VALOR->{
                        showAlertDialog(getString(R.string._not_supported,Constants.VALOR))
                    }
                    else->{
                        showAlertDialog(getString(R.string.please_connect_payment_device))
                    }
                }

            }
        })
    }

    private fun setLoyaltyEarnedObserver() {
        paymentviewModel.earnedLoyaltyPoints.observe(viewLifecycleOwner,
            object : androidx.lifecycle.Observer<Event<Int>> {
                override fun onChanged(t: Event<Int>?) {
                    t?.getContentIfNotHandled()?.let {
                        if (it != 0) {
                            dashboardViewModel.setCustomerLoyaltyOnCustomerThankyouScreen(it)
                        }
                    }
                }
            })
    }

    // To init PosLink for pax payment
    private fun initPOSLink() {
        POSLinkCreatorWrapper.createSync(
            requireContext(),
            object : AppThreadPool.FinishInMainThreadCallback<PosLink?> {
                override fun onFinish(result: PosLink?) {
                    posLink = result!!
                    Log.d("initPOSLink: ", "onFinish")
                    magtekProViewModel.paxNetworkCall(requireContext(), false)
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

            val tipListViewModel by activityViewModels<TipListViewModel>()

            presentation.checkForTipBeforeTransaction(tipListViewModel)
        }


    }

    @Inject
    lateinit var apiService: ApiService

    override fun onPause() {
        super.onPause()
        /* if (this::presentation.isInitialized) {
             presentation.show()
             presentation.onLogOutOrClockOutWithApiService(apiService)
         }*/
    }

    override fun onStop() {
        super.onStop()
        closePaxRequest()
        if (this::presentation.isInitialized) {
            presentation.show()
            // presentation.onLogOutOrClockOutWithApiService(apiService)
        }

        if (this@CheckoutDetailsFragmentNew::paymentCoroutineScope.isInitialized) {
            paymentCoroutineScope.cancel()
        }
    }

    private fun closePaxRequest() {
        countDownTimer?.cancel()
        countDownTimer = null
        /* try{
             posLink.CancelTrans()
         }catch (e:Exception){}*/
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
        initDyanamicPayment()
        lifecycleScope.launch {
            delay(100)
            if (cartList == null) {
                cartList = dashboardViewModel.cartModel
                Log.e(TAG, "checkCartDealy:  ${Gson().toJson(cartList)}")
            }

        }

        observeForTipBeforeTransaction()

        Log.e("totalPriceChcek","totalPrice ${totalPrice}")
    }

    private fun observeForTipBeforeTransaction() {
        dashboardViewModel.customerGivenTipBefore.value = false

        dashboardViewModel.customerGivenTipBefore.observe(viewLifecycleOwner){
            if(it) {

                tipAmount = dashboardViewModel.totalTipAmount

                Log.d("TIP GIVEN: ", "TIP OBSERVER")

//                dashboardViewModel.customerGivenTipBefore.value = false

                var cashTip = tipAmount

                surchargeOnTip = MethodUtils.calculateCashDiscount(
                    tipAmount ,
                    prefProvider,
                    requireContext()
                )

                var cardTip = tipAmount + surchargeOnTip

                dashboardViewModel.apply {
                    employeeGivenTip = true
                    tipAmount = totalTipAmount
                    customerGivenTip.value = true
                }

                dashboardViewModel.setTipAmount(tipAmount)

                prefProvider.setValueboolean(Constants.TIP_ADDED, true)
                prefProvider.setValue(Constants.TIP_ADDED_AMOUNT, tipAmount.toString())
                prefProvider.setValueInt(Constants.TIP_ADDED_ID, tipID)

                tipAmountCalculation(cashTip,cardTip)
                loadPaymentLayout(cashTip,cardTip)
            }
        }
    }

    private fun initDyanamicPayment() {
        dashboardViewModel.getDynamicPaymentRecords(true, prefProvider.getLocationId()).asLiveData()
            .observe(viewLifecycleOwner,
                object : androidx.lifecycle.Observer<List<TbDynamicPaymentRecords>> {
                    override fun onChanged(list: List<TbDynamicPaymentRecords>?) {
                        Log.d("DynamicLiveData: ", "Called")
                        list?.let { dynamicList ->
                            if (dynamicList.isNotEmpty()) {
                                binding.tvOther.visible()
                                var layoutInflater = requireContext().getSystemService(
                                    Context.LAYOUT_INFLATER_SERVICE
                                ) as LayoutInflater
                                layoutInflater = LayoutInflater.from(requireContext())
                                binding.llDynamicLink.removeAllViews()
                                /* Render the dynamic button here with the help of loop */
                                dynamicList?.forEach {
                                    var itemDynamicButton =
                                        layoutInflater.inflate(R.layout.item_button, null, false)
                                    itemDynamicButton.findViewById<LinearLayout>(R.id.llDynamicPayment)
                                        .setPadding(
                                            getResources().getDimensionPixelSize(R.dimen._20sdp),
                                            getResources().getDimensionPixelSize(R.dimen._10sdp),
                                            getResources().getDimensionPixelSize(R.dimen._20sdp),
                                            getResources().getDimensionPixelSize(R.dimen._10sdp)
                                        )
                                    itemDynamicButton.id = it.id
                                    itemDynamicButton.findViewById<AppCompatTextView>(R.id.tvDynamicPaymentName).text =
                                        it.name

                                    itemDynamicButton.setOnSingleClickListener { view ->
                                        view.isEnabled = false
                                        Handler(Looper.getMainLooper()).postDelayed(object :
                                            java.lang.Runnable {
                                            override fun run() {
                                                view.isEnabled = true
                                            }
                                        }, 5000)
                                        startDynamicPayment(it.name, it.id)
                                    }

                                    binding.llDynamicLink.addView(itemDynamicButton)


                                    /*----------- Linear Layout --------------*/
                                    /*   var linearLayout = LinearLayout(requireContext())
                                   var layoutParams = LinearLayout.LayoutParams(
                                       LinearLayout.LayoutParams.WRAP_CONTENT,
                                       LinearLayout.LayoutParams.WRAP_CONTENT
                                   )
                                   linearLayout.layoutParams = layoutParams
                                   if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
                                       linearLayout.setBackgroundDrawable(getResources().getDrawable(R.drawable.background_txt_selector));
                                   else if(android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1)
                                       linearLayout.setBackground(getResources().getDrawable(R.drawable.background_txt_selector));
                                   else
                                       linearLayout.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.background_txt_selector));

                                   *//*----------- Linear Layout --------------*//*


                            *//*----------- Appcompat TextView --------------*//*
                            var appCompatTextView = AppCompatTextView(requireContext())
                            var textViewLayoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            appCompatTextView.layoutParams = textViewLayoutParams

                            appCompatTextView.text = it.name
                            appCompatTextView.gravity=Gravity.CENTER_VERTICAL
                            *//*----------- Appcompat TextView --------------*//*

                            linearLayout.addView(appCompatTextView)
                            binding.llPaymentLink.addView(linearLayout)*/
                                }
                            } else {
                                binding.llDynamicLink.children.forEach {
                                    it.isActivated = false
                                    it.isClickable = false
                                    it.isEnabled = false
                                    it.alpha = 0f
                                }

//                                binding.tvOther.invisible()
                            }
                        }

                    }
                })
    }

    private fun startDynamicPayment(name: String?, id: Int) {
        val cardAmount = binding.tvCard.text.toString().replace("$", "").replace("Card (", "")
            .replace(")", "").trim().toDouble()
        val cashAmount = binding.tvCash0.text.toString().replace("$", "").trim().toDouble()
        if (cardAmount != 0.00 && cashAmount != 0.00) {
            if (InternetUtils.isInternetAvailable(requireActivity().applicationContext)) {

                restrictTvCashClicks()

                custom_paymentAmount = 0.0

                if (cashDiscountType.equals("CashDiscount", ignoreCase = true)) {
                    paymentviewModel.totalPayAmount(
                        binding.tvCard.text.toString().replace("$", "").replace("Card (", "")
                            .replace(")", "").trim().toDouble()
                    )
                    paymentAmount =
                        binding.tvCard.text.toString().replace("$", "").replace("Card (", "")
                            .replace(")", "").trim().toDouble()
                } else {
                    paymentviewModel.totalPayAmount(
                        binding.tvCash0.text.toString().replace("$", "").trim().toDouble()
                    )
                    paymentAmount =
                        binding.tvCash0.text.toString().replace("$", "").trim().toDouble()
                }

                prefProvider.setValueboolean(Constants.IS_SELL_OR_ADD_VALUE_GIFT_CARD, false)
                dynamicCashPaymentWithVariation(
                    dynamicPaymentName = name ?: "", dynamicPaymentId = id
                )
            } else {
                errorDisplay("Please check your Network Connectivity.")
            }
        } else {
            errorDisplay(getString(R.string.payment_amount_is_zero))

        }

    }

    private fun setUpManualCardFocusChanged() {
        binding.edtCardNumber.transformationMethod = null
        binding.edtMMYY.transformationMethod = null
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
                dashboardViewModel.setTipAmount(tipAmount)
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


            /**
             * Used to show Given TIPS on OrderCompleted Fragment
             */

            if (tipAmount > 0.0) {

                var cashTip = tipAmount

                surchargeOnTip = MethodUtils.calculateCashDiscount(
                    tipAmount ,
                    prefProvider,
                    requireContext()
                )
                var cardTip = tipAmount + surchargeOnTip

//            var cardTip = tipAmount + MethodUtils.calculateCashDiscount(
//                tipAmount ,
//                prefProvider,
//                requireContext()
//            )

                dashboardViewModel.apply {
                    totalTipAmount = tipAmount
                    employeeGivenTip = true
                    customerGivenTipBefore.value = true
                   // customerGivenTip.value = true
                }

                dashboardViewModel.setTipAmount(tipAmount)
                tipID = bundle.getInt("tipId")

                prefProvider.setValueboolean(Constants.TIP_ADDED, true)
                prefProvider.setValue(Constants.TIP_ADDED_AMOUNT, tipAmount.toString())
                prefProvider.setValueInt(Constants.TIP_ADDED_ID, tipID)

                tipAmountCalculation(cashTip, cardTip)
                loadPaymentLayout(cashTip, cardTip)
            } else {

                var cashTip = tipAmount
                var cardTip = tipAmount + MethodUtils.calculateCashDiscount(
                    tipAmount,
                    prefProvider,
                    requireContext()
                )

                dashboardViewModel.apply {
                    totalTipAmount = tipAmount
                    employeeGivenTip = false
                    customerGivenTip.value = false

                    if(tipAmount == 0.0) {
                        tipRemovedObserver.value = true
                    }
                }

                dashboardViewModel.setTipAmount(tipAmount)
                tipID = 0

                prefProvider.setValueboolean(Constants.TIP_ADDED, false)
                prefProvider.setValue(Constants.TIP_ADDED_AMOUNT, tipAmount.toString())
                prefProvider.setValueInt(Constants.TIP_ADDED_ID, tipID)

                tipAmountCalculation(cashTip, cardTip)
                loadPaymentLayout(cashTip, cardTip)
            }
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

            EventBus.getDefault()
                .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ request_for_customAmount_1"))

            disconnectSyncChannel()
            val amount = bundle.getDouble("amount")
            val totalPrice = bundle.getDouble("totalAmount")
            MethodUtils.setPriceTextView(binding.tvCustomAmount, amount)
            custom_paymentAmount = amount
            binding.tvCustomAmount.text = "Custom (" + binding.tvCustomAmount.text.toString() + ")"
            cashPaymentWithVariation()

//            SunmiPrintHelper.getInstance().openCashBox()

            if (android.os.Build.BRAND.contains("Landi", ignoreCase = true)) {
//                EventBus.getDefault().post(MessageEvent(Constants.CASHBOX, true))
                EventBus.getDefault().post(CashBoxEvent(Constants.CASHBOX))
            } else {
                SunmiPrintHelper.getInstance().openCashBox()
            }
        }


    }

    private fun splitClick() {

        binding.linearNextSplit.setOnSingleClickListener {
            PaymentBoldPosFragment.newInstance().addTipHideShow(false)
            dashboardViewModel.paymentInProgress.value = false
            dashboardViewModel.tipBeforeEnabled = true
            dashboardViewModel.removeMainCart.value = true
            dashboardViewModel.splitChanged.value = isSelectedCount
            dashboardViewModel.setSplitCount(isSelectedCount)
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
            dashboardViewModel.wholetotalPrice = WholetotalPrice
            findNavController().navigate(R.id.action_splitFragment_to_splitdialog)
        }
    }

    private fun observeData() {
        paymentviewModel.orderCreate.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    runOnUiThread(Runnable {
                        dismissProgressDialog()
                    })
                }
                dashboardViewModel.activeOrderTypeName = ""
                dashboardViewModel.activeOrderTypeId = 0
                dashboardViewModel.activeOrderTypeText = ""
                dashboardViewModel.deleteOrderTypeBackupByName(
                    prefProvider.employeeId()
                )
            }


        }


        paymentviewModel.data.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                runOnUiThread(Runnable {
                    dismissProgressDialog()
                })
                LogUtil.logE(TAG, "receiptData: ${Gson().toJson(it.data)}")
                dashboardViewModel.redeemLoyaltyInfo = RedeemLoyaltyInfo()
                prefProvider.setValueInt("ORDER_ID", it.data.order.id)
                EventBus.getDefault()
                    .post(
                        MessageEvent(
                            "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt _ paymentviewModel.data..observe(.. _ prefProvider.setValueInt(ORDER_ID) _ id.data.order.id -> ${
                                Gson().toJson(
                                    it
                                )
                            }"
                        )
                    )

                dashboardViewModel.updateActiveOrderFlagClear()
                prefProvider.setValueboolean(IS_GIFT_CARD_REDEEM, false)
                prefProvider.setValueboolean(IS_ORDER_REDEEMABLE_WITH_GIFT_CARD, false)
                prefProvider.setValue(GIFT_CARD_NUMBER, "")
                prefProvider.setValue(GIFT_CARD_PIN, "")

                isInsert = false
                isCardRev = false

                dashboardViewModel.setTipAmount(0.0)
                when {
                    paymentType == "Cash" -> {
                        if (it.data.order.payments.last().paymentType.equals(
                                getString(R.string.external),
                                ignoreCase = true
                            )
                        ) {
                            performCashOperation(it, true)
                        } else {
                            performCashOperation(it)
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
                            wholePrice - String.format(
                                "%.2f",
                                paymentAmount - (cashDiscountSurcharge)
                            ).toDouble()
                        } else {
                            wholePrice - paymentAmount
                        }

                        if (remainingValue <= 0.0) {
                            remainingValue = 0.0
                        }


                         //to resolve tip before transaction issue

                        if(isSelectedCount > 1)
                        remainingValue += surchargeOnTip


                        prefProvider.setValue(
                            Constants.WHOLE_AMOUNT,
                            String.format("%.2f", remainingValue)
                        )

                        EventBus.getDefault().post(
                            MessageEvent(
                                "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_WHOLE_AMOUNT _ remainingValue -> ${
                                    Gson().toJson(remainingValue)
                                }", true
                            )
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
                            EventBus.getDefault()
                                .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ prefProvider.setValueboolean(Constants.SPLIT_ENABLE, false) _1"))

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
                            EventBus.getDefault()
                                .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ prefProvider.setValueboolean(Constants.SPLIT_ENABLE, true) remainingValue -> ${remainingValue} _2"))

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
                        bundle.putString("orderType_to_check_kiosk", orderTypeToCheckKioskOrder)

                        if (findNavController().currentDestination?.id == R.id.paymentBoldPosFragment) {
                            clearObserver()
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
                            bundle.putDouble("PaidAmount", paymentAmount)
                        } else {
                            bundle.putDouble("PaidAmount", remainingAmount)
                        }

                        val wholePrice =
                            String.format(
                                "%.2f",
                                prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble()
                            ).toDouble()

                        bundle.putDouble("WholetotalPrice", wholePrice)
                        var remainingValue = 0.0

                        remainingValue = wholePrice - paymentAmount

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
                            EventBus.getDefault()
                                .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ prefProvider.setValueboolean(Constants.SPLIT_ENABLE, false) _3"))

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
                            EventBus.getDefault()
                                .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ prefProvider.setValueboolean(Constants.SPLIT_ENABLE, true) _3"))
                            EventBus.getDefault()
                                .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ remainingValue -> ${remainingValue} _3"))

                        }


                        bundle.putInt("orderID", it.data.order.id ?: 0)
                        bundle.putParcelable("receiptData", it.data)
                        bundle.putInt("splitValue", isSelectedCount)
                        bundle.putBoolean("isSplitByAmount", false)
                        bundle.putString("paymentType", paymentType)
                        bundle.putParcelable("cartList", cartList)
                        bundle.putParcelable("redeemLoyalty", redeemLoyaltyInfo)
                        bundle.putDouble("TipAmount", tipAmount)

                        bundle.putDouble("noCashAdj", cashDiscountSurcharge)
                        bundle.putBoolean("isFromActiveOrder", isFromOpenOrder)
                        bundle.putString("orderType_to_check_kiosk", orderTypeToCheckKioskOrder)

                        if (findNavController().currentDestination?.id == R.id.paymentBoldPosFragment) {
                            clearObserver()
                            findNavController().navigate(
                                R.id.action_paymentBoldPosFragment_to_orderComplete,
                                bundle
                            )
                        }

                    }
                }

            }
        }

        paymentviewModel.transactionErrorText.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                runOnUiThread(Runnable {
                    dismissProgressDialog()
                })
                prefProvider.setValueboolean(IS_PAX_PAYMENT_FAILED, true)
                AlertUtils.showCustomAlert(
                    requireContext(),
                    getString(R.string.pax_transaction_error_message)
                )
            }
        }

        giftCardViewModel.giftCardData.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {

                runOnUiThread(object : java.lang.Runnable {
                    override fun run() {
                        dismissProgressDialog()
                    }
                })
                if (it.data != null) {
                    Log.d(TAG, "observeData: SellGiftCardResponse = $it")
                    LogUtil.logE(TAG, "receiptData: ${Gson().toJson(it.data)}")
                    prefProvider.setValueInt("ORDER_ID", it.data.gift_card.id)
                    EventBus.getDefault()
                        .post(
                            MessageEvent(
                                "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt _ giftcardViewModel.giftCard.observe(.. _ prefProvider.setValueInt(ORDER_ID) _ id.data.gift_card.id -> ${
                                    Gson().toJson(
                                        it.data.gift_card.id
                                    )
                                }"
                            )
                        )

                    isInsert = false
                    isCardRev = false

                    dashboardViewModel.setTipAmount(0.0)
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
                                EventBus.getDefault()
                                    .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ prefProvider.setValueboolean(Constants.SPLIT_ENABLE, false) _4"))

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

                                    EventBus.getDefault()
                                        .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ prefProvider.setValueboolean(Constants.SPLIT_ENABLE, true) _4"))
                                    EventBus.getDefault()
                                        .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ remainingValue -> ${remainingValue} _4"))

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
                                    EventBus.getDefault()
                                        .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ prefProvider.setValueboolean(Constants.SPLIT_ENABLE, false) _5"))

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

                                    EventBus.getDefault()
                                        .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ prefProvider.setValueboolean(Constants.SPLIT_ENABLE, true) _5"))
                                    EventBus.getDefault()
                                        .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ custom_paymentAmount -> ${custom_paymentAmount} _5"))

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
                            bundle.putString("orderType_to_check_kiosk", orderTypeToCheckKioskOrder)


                            if (findNavController().currentDestination?.id == R.id.paymentBoldPosFragment) {
                                clearObserver()
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

                                EventBus.getDefault()
                                    .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ prefProvider.setValueboolean(Constants.SPLIT_ENABLE, false) _6"))
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
                                EventBus.getDefault()
                                    .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ prefProvider.setValueboolean(Constants.SPLIT_ENABLE, true) _6"))
                                EventBus.getDefault()
                                    .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ remainingValue -> ${remainingValue} _6"))
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
                            bundle.putString("orderType_to_check_kiosk", orderTypeToCheckKioskOrder)

                            if (findNavController().currentDestination?.id == R.id.paymentBoldPosFragment) {
                                clearObserver()
                                findNavController().navigate(
                                    R.id.action_paymentBoldPosFragment_to_orderComplete,
                                    bundle
                                )
                            } else {
                                runOnUiThread(Runnable {
                                    dismissProgressDialog()
                                })
                            }

                        }

                        "External" -> {
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
                                EventBus.getDefault()
                                    .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ prefProvider.setValueboolean(Constants.SPLIT_ENABLE, false) _4"))

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

                                    EventBus.getDefault()
                                        .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ prefProvider.setValueboolean(Constants.SPLIT_ENABLE, true) _4"))
                                    EventBus.getDefault()
                                        .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ remainingValue -> ${remainingValue} _4"))

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
                                    EventBus.getDefault()
                                        .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ prefProvider.setValueboolean(Constants.SPLIT_ENABLE, false) _5"))

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

                                    EventBus.getDefault()
                                        .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ prefProvider.setValueboolean(Constants.SPLIT_ENABLE, true) custom_paymentAmount -> ${custom_paymentAmount} _5"))
                                }

                            }


                            bundle.putInt("orderID", it.data.gift_card.id ?: 0)
                            bundle.putParcelable("giftCardReceiptData", it.data)
                            bundle.putInt("splitValue", isSelectedCount)
                            bundle.putBoolean("isSplitByAmount", false)
                            bundle.putString("paymentType", "External")
                            bundle.putParcelable("cartList", cartList)
                            bundle.putParcelable("redeemLoyalty", redeemLoyaltyInfo)
                            bundle.putDouble("TipAmount", tipAmount)

                            bundle.putDouble("noCashAdj", cashDiscountSurcharge)
                            bundle.putBoolean("isFromActiveOrder", isFromOpenOrder)
                            bundle.putString("orderType_to_check_kiosk", orderTypeToCheckKioskOrder)


                            if (findNavController().currentDestination?.id == R.id.paymentBoldPosFragment) {
                                clearObserver()
                                findNavController().navigate(
                                    R.id.action_paymentBoldPosFragment_to_orderComplete,
                                    bundle
                                )
                            }

                        }
                    }
                } else {
                    runOnUiThread(Runnable {
                        dismissProgressDialog()
                    })

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

                runOnUiThread(object : java.lang.Runnable {
                    override fun run() {
                        dismissProgressDialog()
                    }
                })
                if (it.data != null) {
                    Log.d(TAG, "observeData: SellGiftCardResponse = $it")
                    LogUtil.logE(TAG, "receiptData: ${Gson().toJson(it.data)}")
                    prefProvider.setValueInt("ORDER_ID", it.data.gift_card.id)

                    EventBus.getDefault()
                        .post(
                            MessageEvent(
                                "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt _ giftCardViewModel.addValueInGiftCardData.observe(.. _ prefProvider.setValueInt(ORDER_ID) _ it.data.gift_card.id -> ${
                                    Gson().toJson(
                                        it.data.gift_card.id
                                    )
                                }"
                            )
                        )

                    isInsert = false
                    isCardRev = false

                    dashboardViewModel.setTipAmount(0.0)
                    if (it.data.gift_card.payments[it.data.gift_card.payments.size - 1].payment_type.equals(
                            Constants.EXTERNAL_PAYMENT
                        )
                    ) {
                        paymentType = Constants.EXTERNAL_PAYMENT
                    }

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

                                EventBus.getDefault()
                                    .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ prefProvider.setValueboolean(Constants.SPLIT_ENABLE, false) _7"))

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

                                    EventBus.getDefault()
                                        .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ prefProvider.setValueboolean(Constants.SPLIT_ENABLE, true) _7"))
                                    EventBus.getDefault()
                                        .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ remainingValue = ${remainingValue} _7"))

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

                                    EventBus.getDefault()
                                        .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ prefProvider.setValueboolean(Constants.SPLIT_ENABLE, false) _8"))

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

                                    EventBus.getDefault()
                                        .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ prefProvider.setValueboolean(Constants.SPLIT_ENABLE, true) _8"))
                                    EventBus.getDefault()
                                        .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ custom_paymentAmount = ${custom_paymentAmount} _8"))

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
                            bundle.putString("orderType_to_check_kiosk", orderTypeToCheckKioskOrder)


                            if (findNavController().currentDestination?.id == R.id.paymentBoldPosFragment) {
                                clearObserver()
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

                                EventBus.getDefault()
                                    .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ prefProvider.setValueboolean(Constants.SPLIT_ENABLE, false) _9"))

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

                                EventBus.getDefault()
                                    .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ prefProvider.setValueboolean(Constants.SPLIT_ENABLE, true) _9"))
                                EventBus.getDefault()
                                    .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ remainingValue -. ${remainingValue} _9"))

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
                            bundle.putString("orderType_to_check_kiosk", orderTypeToCheckKioskOrder)

                            if (findNavController().currentDestination?.id == R.id.paymentBoldPosFragment) {
                                clearObserver()
                                findNavController().navigate(
                                    R.id.action_paymentBoldPosFragment_to_orderComplete,
                                    bundle
                                )
                            }

                        }

                        Constants.EXTERNAL_PAYMENT -> {
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

                                EventBus.getDefault()
                                    .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ prefProvider.setValueboolean(Constants.SPLIT_ENABLE, false) _7"))

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

                                    EventBus.getDefault()
                                        .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ prefProvider.setValueboolean(Constants.SPLIT_ENABLE, true) _7"))
                                    EventBus.getDefault()
                                        .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ remainingValue = ${remainingValue} _7"))

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

                                    EventBus.getDefault()
                                        .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ prefProvider.setValueboolean(Constants.SPLIT_ENABLE, true) _8"))
                                    EventBus.getDefault()
                                        .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ custom_paymentAmount = ${custom_paymentAmount} _8"))

                                }

                            }


                            bundle.putInt("orderID", it.data.gift_card.id ?: 0)
                            bundle.putParcelable("giftCardReceiptData", it.data)
                            bundle.putInt("splitValue", isSelectedCount)
                            bundle.putBoolean("isSplitByAmount", false)
                            bundle.putString("paymentType", Constants.EXTERNAL_PAYMENT)
                            bundle.putParcelable("cartList", cartList)
                            bundle.putParcelable("redeemLoyalty", redeemLoyaltyInfo)
                            bundle.putDouble("TipAmount", tipAmount)

                            bundle.putDouble("noCashAdj", cashDiscountSurcharge)
                            bundle.putBoolean("isFromActiveOrder", isFromOpenOrder)
                            bundle.putString("orderType_to_check_kiosk", orderTypeToCheckKioskOrder)


                            if (findNavController().currentDestination?.id == R.id.paymentBoldPosFragment) {
                                clearObserver()
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

    private fun performCashOperation(it: CreateOrderResponse, dynamicPayment: Boolean = false) {

        LogUtil.logE("TipAmount 4:: ", tipAmount.toString())

        val bundle = Bundle()
        bundle.putBoolean("isDineIn", false)
        EventBus.getDefault()
            .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ performCashOperation_1"))

        if (remainingAmount == 0.0) {
            if (custom_paymentAmount != 0.0) {
                bundle.putDouble("PaidAmount", custom_paymentAmount)
            } else {
                bundle.putDouble("PaidAmount", paymentAmount)
            }
        } else {
            bundle.putDouble("PaidAmount", remainingAmount)
        }

        EventBus.getDefault()
            .post(
                MessageEvent(
                    "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ performCashOperation_prefProvider.getValue(Constants.WHOLE_AMOUNT....).toDouble()_1: ${
                        prefProvider.getValue(
                            Constants.WHOLE_AMOUNT,
                            "0.00"
                        )
                    }"
                )
            )

        EventBus.getDefault()
            .post(
                MessageEvent(
                    "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ performCashOperation_prefProvider.getValue(Constants.WHOLE_AMOUNT....).toDouble()_%.1f_2: ${
                        prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.00")
                    }"
                )
            )

        /*if Below is not executed then then maybe %.2f, is raising the error*/
        EventBus.getDefault()
            .post(
                MessageEvent(
                    "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ performCashOperation_prefProvider.getValue(Constants.WHOLE_AMOUNT....).toDouble()_%.2f_3: ${
                        prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.00")
                    }"
                )
            )
        var wholePrice = 0.0
        if (prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.00").isNotEmpty()){
            wholePrice =
                String.format(
                    "%.2f",
                    prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.00").toDouble()
                ).toDouble()
        }else{
            wholePrice = paymentAmount
        }


        bundle.putDouble("WholetotalPrice", wholePrice)
        var remainingValue = 0.0
        if (custom_paymentAmount != 0.0) {
            if (cashDiscountType == "CashDiscount") {
                wholePrice -= cashDiscountSurcharge
            }
            if (custom_paymentAmount != 0.0 && isSelectedCount != 1) {
                var splitChange = 0.0
                splitChange = custom_paymentAmount - (paymentAmount + tipAmount)
                bundle.putDouble(
                    "splitChange", String.format("%.2f", splitChange).toDouble()
                )
                splitChange = custom_paymentAmount - (paymentAmount)
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
            /*Here is the issue*/
            if (dynamicPayment) {
                remainingValue = wholePrice - paymentAmount
            } else {
                remainingValue = if (cashDiscountType == "CashDiscount") {
                    wholePrice - String.format(
                        "%.2f",
                        paymentAmount + cashDiscountSurcharge
                    ).toDouble()
                } else {
                    wholePrice - paymentAmount
                }
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

            EventBus.getDefault()
                .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ prefProvider.setValueboolean(Constants.SPLIT_ENABLE, false) _10"))

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

                EventBus.getDefault()
                    .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ prefProvider.setValueboolean(Constants.SPLIT_ENABLE, true) _10"))
                EventBus.getDefault()
                    .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ custom_paymentAmount -> ${custom_paymentAmount} _10"))

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

                EventBus.getDefault()
                    .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ prefProvider.setValueboolean(Constants.SPLIT_ENABLE, false) _11"))

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

                EventBus.getDefault()
                    .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ prefProvider.setValueboolean(Constants.SPLIT_ENABLE, true) _11"))
                EventBus.getDefault()
                    .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ custom_paymentAmount -> ${custom_paymentAmount} _11"))

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

        try {
            bundle.putString(
                "orderType_to_check_kiosk",
                arguments?.getString("orderType_to_check_kiosk")
            )
        } catch (e: Exception) {

        }

        if (findNavController().currentDestination?.id == R.id.paymentBoldPosFragment) {
            clearObserver()
            findNavController().navigate(
                R.id.action_paymentBoldPosFragment_to_orderComplete,
                bundle
            )
        }

    }

    private fun observeShowProgress() {

        paymentviewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {

                LogUtil.logE("observeShowProgress", it.toString())
                if (it) {
                    ProgressUtils.showProgressDialog(
                        if (prefProvider.getValueboolean(IS_PAX_PAYMENT_FAILED, false)) {
                            getString(R.string.reattempting_the_payment)
                        } else {
                            "Please wait payment under process"
                        },
                        requireActivity()
                    )
                } else {
                    runOnUiThread(Runnable {
                        dismissProgressDialog()
                    })
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
                        if (prefProvider.getValueboolean(IS_PAX_PAYMENT_FAILED, false)) {
                            getString(R.string.reattempting_the_payment)
                        } else {
                            "Please wait payment under process"
                        },
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

    // To make cash payment for placing order
    private fun cashPaymentWithVariation(
    ) {
        paymentAmount = String.format("%.2f", WholetotalPrice / isSelectedCount).toDouble()
        EventBus.getDefault().post(
            MessageEvent(
                "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_cashPaymentWithVariation() paymentAmount-> ${
                    Gson().toJson(paymentAmount)
                }, WholetotalPrice -> ${Gson().toJson(WholetotalPrice)}, isSelectedCount -> ${
                    Gson().toJson(
                        isSelectedCount
                    )
                }", true
            )
        )

//        paymentviewModel.tipOnAmount = paymentAmount
        //        Above code is commented, because the split amount was not changing, below code is the solution
        try {
            paymentviewModel.tipOnAmount = dashboardViewModel.totalPrice.toString()
                .substring(0, dashboardViewModel.totalPrice.toString().indexOf(".") + 3).toDouble()
        } catch (e: Exception) {
            try {
                paymentviewModel.tipOnAmount = dashboardViewModel.totalPrice.toString()
                    .substring(0, dashboardViewModel.totalPrice.toString().indexOf(".") + 2)
                    .toDouble()
            } catch (e: Exception) {
                try {
                    paymentviewModel.tipOnAmount = dashboardViewModel.totalPrice.toString()
                        .substring(0, dashboardViewModel.totalPrice.toString().indexOf(".") + 1)
                        .toDouble()
                } catch (e: Exception) {
                    try {
                        paymentviewModel.tipOnAmount = dashboardViewModel.totalPrice.toString()
                            .substring(0, dashboardViewModel.totalPrice.toString().indexOf("."))
                            .toDouble()
                    } catch (e: Exception) {
                    }
                }
            }
        }
        subTotalPrice = String.format("%.2f", subTotalPrice / isSelectedCount).toDouble()

        EventBus.getDefault().post(
            MessageEvent(
                "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_cashPaymentWithVariation() dashboardViewModel.subTotalPrice-> ${
                    Gson().toJson(dashboardViewModel.subTotalPrice)
                } , isSelectedCount-> ${isSelectedCount}", true
            )
        )

        totalServiceCharge =
            String.format("%.2f", totalServiceCharge / isSelectedCount).toDouble()
        totalTax = String.format("%.2f", totalTax / isSelectedCount).toDouble()
        totalDiscount = String.format("%.2f", totalDiscount / isSelectedCount).toDouble()
        cashDiscountSurcharge = if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == GIFT_CARD) {
            0.0
        } else {
            MethodUtils.getLatestCashDiscountOrSurCharge(
                WholetotalPrice,
                prefProvider,
                requireContext()
            ) / isSelectedCount
        }
        if (cashDiscountType.equals("CashDiscount")) {
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

    private fun dynamicCashPaymentWithVariation(
        dynamicPaymentName: String = "",
        dynamicPaymentId: Int = -1
    ) {
        paymentAmount = String.format("%.2f", WholetotalPrice / isSelectedCount).toDouble()
        EventBus.getDefault().post(
            MessageEvent(
                "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_cashPaymentWithVariation() paymentAmount-> ${
                    Gson().toJson(paymentAmount)
                }, WholetotalPrice -> ${Gson().toJson(WholetotalPrice)}, isSelectedCount -> ${
                    Gson().toJson(
                        isSelectedCount
                    )
                }", true
            )
        )

//        paymentviewModel.tipOnAmount = paymentAmount
        //        Above code is commented, because the split amount was not changing, below code is the solution
        try {
            paymentviewModel.tipOnAmount = dashboardViewModel.totalPrice.toString()
                .substring(0, dashboardViewModel.totalPrice.toString().indexOf(".") + 3).toDouble()
        } catch (e: Exception) {
            try {
                paymentviewModel.tipOnAmount = dashboardViewModel.totalPrice.toString()
                    .substring(0, dashboardViewModel.totalPrice.toString().indexOf(".") + 2)
                    .toDouble()
            } catch (e: Exception) {
                try {
                    paymentviewModel.tipOnAmount = dashboardViewModel.totalPrice.toString()
                        .substring(0, dashboardViewModel.totalPrice.toString().indexOf(".") + 1)
                        .toDouble()
                } catch (e: Exception) {
                    try {
                        paymentviewModel.tipOnAmount = dashboardViewModel.totalPrice.toString()
                            .substring(0, dashboardViewModel.totalPrice.toString().indexOf("."))
                            .toDouble()
                    } catch (e: Exception) {
                    }
                }
            }
        }
        subTotalPrice = String.format("%.2f", subTotalPrice / isSelectedCount).toDouble()

        EventBus.getDefault().post(
            MessageEvent(
                "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_cashPaymentWithVariation() dashboardViewModel.subTotalPrice-> ${
                    Gson().toJson(dashboardViewModel.subTotalPrice)
                } , isSelectedCount-> ${isSelectedCount}", true
            )
        )

        totalServiceCharge =
            String.format("%.2f", totalServiceCharge / isSelectedCount).toDouble()
        totalTax = String.format("%.2f", totalTax / isSelectedCount).toDouble()
        totalDiscount = String.format("%.2f", totalDiscount / isSelectedCount).toDouble()
        cashDiscountSurcharge = if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == GIFT_CARD) {
            0.0
        } else {
            MethodUtils.getLatestCashDiscountOrSurCharge(
                WholetotalPrice,
                prefProvider,
                requireContext()
            ) / isSelectedCount
        }
        if (cashDiscountType.equals("CashDiscount") && dynamicPaymentName.isEmpty()) {
            paymentAmount -= cashDiscountSurcharge
        }
        if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == GIFT_CARD) {
            if (prefProvider.getValueboolean(Constants.IS_ADD_VALUE_IN_GIFT_CARD, false)) {
                if (dynamicPaymentName.isNotEmpty()) {
                    addValueInGiftCardUsingCash(dynamicPaymentName)
                } else {
                    addValueInGiftCardUsingCash()
                }
            } else {
                if (prefProvider.getValue(Constants.GIFT_CARD_TYPE, "")
                        .equals("Physical") || (prefProvider.getValue(Constants.GIFT_CARD_TYPE, "")
                        .equals("Digital"))
                ) {
                    sellGiftCardUsingCash(
                        dynamicPaymentType = dynamicPaymentName,
                        dynamicPaymentId = dynamicPaymentId
                    )
                } else {
                    sellGiftCardUsingCash()
                }
            }
        } else {
            transactionInProgress()
            makeDynamicCashPayment(
                dynamicPaymentType = dynamicPaymentName,
                dynamicPaymentId = dynamicPaymentId
            )
        }
    }

    // To purchase gift card with cash payment
    private fun redeemGiftCard() {
        subTotalPrice = String.format("%.2f", subTotalPrice / isSelectedCount).toDouble()

        EventBus.getDefault().post(
            MessageEvent(
                "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_redeemGiftCard() dashboardViewModel.subTotalPrice-> ${
                    Gson().toJson(dashboardViewModel.subTotalPrice)
                } , isSelectedCount-> ${isSelectedCount}", true
            )
        )

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

    private fun disconnectSyncChannel() {
        MainActivity.consumer2?.let {
            it.disconnect()
            Log.e("onActionConnected", "onActionConnected: Disconnected")
        }
    }

    // manage click of different types of payment methods visible on screen
    private fun paymentClick() {

        binding.llCreditCard.setOnSingleClickListener {
            disconnectSyncChannel()

            if (InternetUtils.isInternetAvailable(applicationContext = requireActivity().applicationContext)) {


                restrictTvCashClicks()

                val device = prefProvider.getValueInt(Constants.MAGTEK_HARDWARE, 0)
                subTotalPrice = String.format("%.2f", subTotalPrice / isSelectedCount).toDouble()

                EventBus.getDefault().post(
                    MessageEvent(
                        "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_binding.llCreditCard.setOnSingleClickListener dashboardViewModel.subTotalPrice-> ${
                            Gson().toJson(dashboardViewModel.subTotalPrice)
                        } , isSelectedCount-> ${isSelectedCount}", true
                    )
                )

                Log.d(
                    "LOADER::",
                    "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
                )
                totalServiceCharge =
                    String.format("%.2f", totalServiceCharge / isSelectedCount).toDouble()
                totalTax = String.format("%.2f", totalTax / isSelectedCount).toDouble()
                totalDiscount = String.format("%.2f", totalDiscount / isSelectedCount).toDouble()
                cashDiscountSurcharge =
                    MethodUtils.getLatestCashDiscountOrSurCharge(
                        WholetotalPrice,
                        prefProvider,
                        requireContext()
                    ) / isSelectedCount

//                    if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == GIFT_CARD) {
//                        0.0
//                    } else {
//                        MethodUtils.getLatestCashDiscountOrSurCharge(
//                            WholetotalPrice,
//                            prefProvider,
//                            requireContext()
//                        ) / isSelectedCount
//                    }
                paymentAmount = String.format("%.2f", WholetotalPrice / isSelectedCount).toDouble()


                lifecycleScope.launch(Dispatchers.IO) {
                    EventBus.getDefault().post(
                        MessageEvent(
                            "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_paymentClick() paymentAmount-> ${
                                Gson().toJson(paymentAmount)
                            }, WholetotalPrice -> ${Gson().toJson(WholetotalPrice)}, isSelectedCount -> ${
                                Gson().toJson(
                                    isSelectedCount
                                )
                            }", true
                        )
                    )
                }

                Log.d(
                    "LOADER::",
                    "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
                )

                if (cashDiscountType == "SurCharge") {
                    paymentAmount =
                        String.format("%.2f", paymentAmount + cashDiscountSurcharge).toDouble()
                }

//        paymentviewModel.tipOnAmount = paymentAmount
                //        Above code is commented, because the split amount was not changing, below code is the solution
                try {
                    paymentviewModel.tipOnAmount = dashboardViewModel.totalPrice.toString()
                        .substring(0, dashboardViewModel.totalPrice.toString().indexOf(".") + 3)
                        .toDouble()
                } catch (e: Exception) {
                    try {
                        paymentviewModel.tipOnAmount = dashboardViewModel.totalPrice.toString()
                            .substring(0, dashboardViewModel.totalPrice.toString().indexOf(".") + 2)
                            .toDouble()
                    } catch (e: Exception) {
                        try {
                            paymentviewModel.tipOnAmount = dashboardViewModel.totalPrice.toString()
                                .substring(
                                    0,
                                    dashboardViewModel.totalPrice.toString().indexOf(".") + 1
                                )
                                .toDouble()
                        } catch (e: Exception) {
                            try {
                                paymentviewModel.tipOnAmount =
                                    dashboardViewModel.totalPrice.toString()
                                        .substring(
                                            0,
                                            dashboardViewModel.totalPrice.toString().indexOf(".")
                                        )
                                        .toDouble()
                            } catch (e: Exception) {
                            }
                        }
                    }
                }
//                paymentviewModel.tipOnAmount = dashboardViewModel.totalPrice.toString()
//                    .substring(0, dashboardViewModel.totalPrice.toString().indexOf(".") + 3).toDouble()

                /**
                 * Added to check tip details
                 * **/

                dashboardViewModel.apply {
                    totalAmount = paymentAmount
                    paymentTypeForTip = "card"
                }




                tipAmountToPaymentDevice = tipAmount + MethodUtils.calculateCashDiscount(
                    tipAmount ,
                    prefProvider,
                    requireContext()
                )



                paymentAmount += tipAmountToPaymentDevice
                Log.d(
                    "LOADER::",
                    "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
                )

                if (paymentAmount != 0.0) {
                    prefProvider.setValueboolean(Constants.IS_SELL_OR_ADD_VALUE_GIFT_CARD, false)
                    if (prefProvider.getValue(Constants.PAYMENT_GATEWAY_TYPE,"").equals(Constants.PAX, ignoreCase = true)){
                        if (prefProvider.getValueboolean(
                                Constants.IS_PAX_CONNECTED,
                                false
                            ) && !mSessionManager.isConnected
                        ) {
                            runOnUiThread(object : java.lang.Runnable {
                                override fun run() {
                                    showProgressDialog()
                                }
                            })
                            CoroutineScope(Dispatchers.Main).launch {
                                var paxData: PAXData? = paymentviewModel.getPaxPaymentData()
                                if (paxData != null) {
                                    prefProvider.setValueboolean(IS_PAX_PAYMENT_FAILED, true)
                                    // Retry api call if we have unsuccessful pending payment stored
                                    GlobalUID = paxData.globalUid
                                    ExtData = paxData.extData
                                    RefNumber = paxData.refNumber
                                    ECRRefNumber = paxData.eCRRefNumber
                                    PAXtoken = paxData.paxToken
                                    EDCType = paxData.EDCType
                                    cardLastDigits = paxData.cardLastDigits
                                    if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == GIFT_CARD) {
                                        if (prefProvider.getValueboolean(
                                                Constants.IS_ADD_VALUE_IN_GIFT_CARD,
                                                false
                                            )
                                        ) {
                                            giftCardViewModel.paxResponse = ExtData
                                            giftCardViewModel.cardNumberLast4 = cardLastDigits
                                            giftCardViewModel.cardNamePax = EDCType
                                            giftCardViewModel.transactionID = PAXtoken
                                            addValueInGiftCardUsingCard(paymentAmount)
                                        } else {
                                            giftCardViewModel.paxResponse = ExtData
                                            giftCardViewModel.cardNumberLast4 = cardLastDigits
                                            giftCardViewModel.cardNamePax = EDCType
                                            giftCardViewModel.transactionID = PAXtoken
                                            sellGiftCardUsingCard(paymentAmount)
                                        }
                                    } else {
                                        makePaymentCreditCard()
                                    }
//                                makePaymentCreditCard()
                                } else {
                                    transactionInProgress()
                                    makePaxPaymentRequest()
                                }
                            }
                        }else{
                            showPaymentNotConnectedMessage()
                        }
                    }else if (prefProvider.getValue(Constants.PAYMENT_GATEWAY_TYPE,"").equals(Constants.VELOR,ignoreCase = true) || prefProvider.getValue(Constants.PAYMENT_GATEWAY_TYPE,"").equals(Constants.VALOR,ignoreCase = true)){
                        makeValorPaymentRequest()
                    }else if(prefProvider.getValue(Constants.PAYMENT_GATEWAY_TYPE,"").equals(Constants.DEJAVOO,ignoreCase = true)){
                        makeDejavooPaymentRequest()
                    }else if(mSessionManager.isConnected){
//                        Magtek
                            runOnUiThread(object : java.lang.Runnable {
                                override fun run() {
                                    showProgressDialog()
                                }
                            })
                            magtekModule.stopListner(false)
                            if (device == 0) {
                                magtekPaymentCall()
                            } else {
                                magtekProPaymentCall()
                            }
                            prefProvider.setValueboolean(Constants.IS_PAX_CONNECTED, false)

                    }else{
                        showPaymentNotConnectedMessage()
                    }

                  /*  if (mSessionManager.isConnected) {
                        runOnUiThread(object : java.lang.Runnable {
                            override fun run() {
                                showProgressDialog()
                            }
                        })
                        magtekModule.stopListner(false)
                        if (device == 0) {
                            magtekPaymentCall()
                        } else {
                            magtekProPaymentCall()
                        }
                        prefProvider.setValueboolean(Constants.IS_PAX_CONNECTED, false)
                    } else if (prefProvider.getValueboolean(
                            Constants.IS_PAX_CONNECTED,
                            false
                        ) && !mSessionManager.isConnected
                    ) {
                        runOnUiThread(object : java.lang.Runnable {
                            override fun run() {
                                showProgressDialog()
                            }
                        })
                        CoroutineScope(Dispatchers.Main).launch {
                            var paxData: PAXData? = paymentviewModel.getPaxPaymentData()
                            if (paxData != null) {
                                prefProvider.setValueboolean(IS_PAX_PAYMENT_FAILED, true)
                                // Retry api call if we have unsuccessful pending payment stored
                                GlobalUID = paxData.globalUid
                                ExtData = paxData.extData
                                RefNumber = paxData.refNumber
                                ECRRefNumber = paxData.eCRRefNumber
                                PAXtoken = paxData.paxToken
                                EDCType = paxData.EDCType
                                cardLastDigits = paxData.cardLastDigits
                                if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == GIFT_CARD) {
                                    if (prefProvider.getValueboolean(
                                            Constants.IS_ADD_VALUE_IN_GIFT_CARD,
                                            false
                                        )
                                    ) {
                                        giftCardViewModel.paxResponse = ExtData
                                        giftCardViewModel.cardNumberLast4 = cardLastDigits
                                        giftCardViewModel.cardNamePax = EDCType
                                        giftCardViewModel.transactionID = PAXtoken
                                        addValueInGiftCardUsingCard()
                                    } else {
                                        giftCardViewModel.paxResponse = ExtData
                                        giftCardViewModel.cardNumberLast4 = cardLastDigits
                                        giftCardViewModel.cardNamePax = EDCType
                                        giftCardViewModel.transactionID = PAXtoken
                                        sellGiftCardUsingCard()
                                    }
                                } else {
                                    makePaymentCreditCard()
                                }
//                                makePaymentCreditCard()
                            } else {
                                makePaxPaymentRequest()
                            }
                        }
                    } else if (prefProvider.getValue(
                            Constants.VALOR_APP_ID, ""
                        ).isNotEmpty()
                    ) {
                        makeValorPaymentRequest()
                    } else if (prefProvider.getValue(
                            Constants.VALOR_APP_ID, ""
                        ).isNullOrEmpty()
                    ) {
                        makeDejavooPaymentRequest()
                    } else {
                        runOnUiThread(object : java.lang.Runnable {
                            override fun run() {
                                binding.llCreditCard.isEnabled = true
                                dismissProgressDialog()
                            }
                        })
                        errorDisplay("Please connect a payment device.")
                    }*/
                } else {
                    runOnUiThread(object : java.lang.Runnable {
                        override fun run() {
                            binding.llCreditCard.isEnabled = true
                            dismissProgressDialog()
                        }
                    })
                    errorDisplay(getString(R.string.payment_amount_is_zero))
                }
            } else {
                runOnUiThread(object : java.lang.Runnable {
                    override fun run() {
                        binding.llCreditCard.isEnabled = true
                        dismissProgressDialog()
                    }
                })
                errorDisplay("Please check your Network Connectivity.")
            }
            //  makePaymentCreditCard()
        }

        binding.llSavedCard.setOnSingleClickListener {
            disconnectSyncChannel()

            if (InternetUtils.isInternetAvailable(applicationContext = requireActivity().applicationContext)) {
                //Checking Internet connection
                runOnUiThread(object : java.lang.Runnable {
                    override fun run() {
                        showProgressDialog()
                    }
                })
                restrictTvCashClicks()

                val device = prefProvider.getValueInt(Constants.MAGTEK_HARDWARE, 0)
                subTotalPrice = String.format("%.2f", subTotalPrice / isSelectedCount).toDouble()

                EventBus.getDefault().post(
                    MessageEvent(
                        "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_binding.llCreditCard.setOnSingleClickListener dashboardViewModel.subTotalPrice-> ${
                            Gson().toJson(dashboardViewModel.subTotalPrice)
                        } , isSelectedCount-> ${isSelectedCount}", true
                    )
                )

                Log.d(
                    "LOADER::",
                    "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
                )
                totalServiceCharge =
                    String.format("%.2f", totalServiceCharge / isSelectedCount).toDouble()
                totalTax = String.format("%.2f", totalTax / isSelectedCount).toDouble()
                totalDiscount = String.format("%.2f", totalDiscount / isSelectedCount).toDouble()
                cashDiscountSurcharge =
                    MethodUtils.getLatestCashDiscountOrSurCharge(
                        WholetotalPrice,
                        prefProvider,
                        requireContext()
                    ) / isSelectedCount
//                    if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == GIFT_CARD) {
//                        0.0
//                    } else {
//                        MethodUtils.getLatestCashDiscountOrSurCharge(
//                            WholetotalPrice,
//                            prefProvider,
//                            requireContext()
//                        ) / isSelectedCount
//                    }
                paymentAmount = String.format("%.2f", WholetotalPrice / isSelectedCount).toDouble()

                lifecycleScope.launch(Dispatchers.IO) {
                    EventBus.getDefault().post(
                        MessageEvent(
                            "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_paymentClick() paymentAmount-> ${
                                Gson().toJson(paymentAmount)
                            }, WholetotalPrice -> ${Gson().toJson(WholetotalPrice)}, isSelectedCount -> ${
                                Gson().toJson(
                                    isSelectedCount
                                )
                            }", true
                        )
                    )
                }

                Log.d(
                    "LOADER::",
                    "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
                )

                if (cashDiscountType == "SurCharge") {
                    paymentAmount =
                        String.format("%.2f", paymentAmount + cashDiscountSurcharge).toDouble()
                }

//        paymentviewModel.tipOnAmount = paymentAmount
                //        Above code is commented, because the split amount was not changing, below code is the solution
                try {
                    paymentviewModel.tipOnAmount = dashboardViewModel.totalPrice.toString()
                        .substring(0, dashboardViewModel.totalPrice.toString().indexOf(".") + 3)
                        .toDouble()
                } catch (e: Exception) {
                    try {
                        paymentviewModel.tipOnAmount = dashboardViewModel.totalPrice.toString()
                            .substring(0, dashboardViewModel.totalPrice.toString().indexOf(".") + 2)
                            .toDouble()
                    } catch (e: Exception) {
                        try {
                            paymentviewModel.tipOnAmount = dashboardViewModel.totalPrice.toString()
                                .substring(
                                    0,
                                    dashboardViewModel.totalPrice.toString().indexOf(".") + 1
                                )
                                .toDouble()
                        } catch (e: Exception) {
                            try {
                                paymentviewModel.tipOnAmount =
                                    dashboardViewModel.totalPrice.toString()
                                        .substring(
                                            0,
                                            dashboardViewModel.totalPrice.toString().indexOf(".")
                                        )
                                        .toDouble()
                            } catch (e: Exception) {
                            }
                        }
                    }
                }
//                paymentviewModel.tipOnAmount = dashboardViewModel.totalPrice.toString()
//                    .substring(0, dashboardViewModel.totalPrice.toString().indexOf(".") + 3).toDouble()

                /**
                 * Added to check tip details
                 * **/

                dashboardViewModel.apply {
                    totalAmount = paymentAmount
                    paymentTypeForTip = "card"
                }


                tipAmountToPaymentDevice = tipAmount + MethodUtils.calculateCashDiscount(
                    tipAmount ,
                    prefProvider,
                    requireContext()
                )

                paymentAmount += tipAmountToPaymentDevice
                Log.d(
                    "LOADER::",
                    "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
                )

                if (paymentAmount != 0.0) {
                    if (mSessionManager.isConnected) {
//                        Magtek call
                        magtekModule.stopListner(false)
                        if (device == 0) {
                            magtekPaymentCall()
                        } else {
                            magtekProPaymentCall()
                        }
                        prefProvider.setValueboolean(Constants.IS_PAX_CONNECTED, false)
                    } else if (prefProvider.getValueboolean(
                            Constants.IS_PAX_CONNECTED,
                            false
                        ) && !mSessionManager.isConnected
                    ) {
//                        Pax is active and Magtek is inactive

                        CoroutineScope(Dispatchers.Main).launch {
                            var paxData: PAXData? = paymentviewModel.getPaxPaymentData()
                            if (paxData != null) {
                                prefProvider.setValueboolean(IS_PAX_PAYMENT_FAILED, true)
                                // Retry api call if we have unsuccessful pending payment stored
                                GlobalUID = paxData.globalUid
                                ExtData = paxData.extData
                                RefNumber = paxData.refNumber
                                ECRRefNumber = paxData.eCRRefNumber
                                PAXtoken = paxData.paxToken
                                EDCType = paxData.EDCType
                                cardLastDigits = paxData.cardLastDigits
                                if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == GIFT_CARD) {
                                    if (prefProvider.getValueboolean(
                                            Constants.IS_ADD_VALUE_IN_GIFT_CARD,
                                            false
                                        )
                                    ) {
                                        giftCardViewModel.paxResponse = ExtData
                                        giftCardViewModel.cardNumberLast4 = cardLastDigits
                                        giftCardViewModel.cardNamePax = EDCType
                                        giftCardViewModel.transactionID = PAXtoken
                                        addValueInGiftCardUsingCard()
                                    } else {
                                        giftCardViewModel.paxResponse = ExtData
                                        giftCardViewModel.cardNumberLast4 = cardLastDigits
                                        giftCardViewModel.cardNamePax = EDCType
                                        giftCardViewModel.transactionID = PAXtoken
                                        sellGiftCardUsingCard()
                                    }
                                } else {
                                    makePaymentCreditCard()
                                }
//                                makePaymentCreditCard()
                            } else {
                                //makePaxPaymentRequest()
                                transactionInProgress()
                                adjustPreAuthPaymentPax()
                            }
                        }
                    } else if (prefProvider.getValue(
                            Constants.VALOR_APP_ID, ""
                        ).isNotEmpty()
                    ) {
                        makeValorPaymentRequest()
                    } else {
                        runOnUiThread(object : java.lang.Runnable {
                            override fun run() {
                                binding.llCreditCard.isEnabled = true
                                dismissProgressDialog()
                            }
                        })
                        errorDisplay("Please connect a payment device.")
                    }
                } else {
                    runOnUiThread(object : java.lang.Runnable {
                        override fun run() {
                            binding.llCreditCard.isEnabled = true
                            dismissProgressDialog()
                        }
                    })
                    errorDisplay("Payment Amount is zero.")
                }
            } else {
                runOnUiThread(object : java.lang.Runnable {
                    override fun run() {
                        binding.llCreditCard.isEnabled = true
                        dismissProgressDialog()
                    }
                })
                errorDisplay("Please check your Network Connectivity.")
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
            val cardAmount = binding.tvCard.text.toString().replace("$", "").replace("Card (", "")
                .replace(")", "").trim().toDouble()
            val cashAmount = binding.tvCash0.text.toString().replace("$", "").trim().toDouble()

//            startPAXTestWithGiftCard()

//            Uncomment below code
            if (cardAmount != 0.00 && cashAmount != 0.00) {
                if (prefProvider.getValueboolean(IS_PAX_PAYMENT_FAILED, false)) {
                    AlertUtils.showCustomAlert(
                        requireContext(),
                        getString(R.string.pax_transaction_error_message)
                    )
                } else {
                    binding.frameLayoutId.visible()
                    binding.relativeMain.gone()
                    binding.llManualCard.gone()
                    binding.llGiftCard.visible()
                    isManualCard = false
                }
            } else {
                errorDisplay(getString(R.string.payment_amount_is_zero))
            }
        }

        /*binding.llDynamicPayment.setOnSingleClickListener {
            if (InternetUtils.isInternetAvailable(requireActivity().applicationContext)) {

                restrictTvCashClicks()

                custom_paymentAmount = 0.0

                if (cashDiscountType.equals("CashDiscount", ignoreCase = true)) {
                    paymentviewModel.totalPayAmount(
                        *//*binding.tvCard.text.toString().replace("$", "").trim().toDouble()*//*
                        binding.tvCard.text.toString().replace("$", "").replace("Card (", "")
                            .replace(")", "").trim().toDouble()
                    )
                    paymentAmount =
                            *//*binding.tvCash0.text.toString().replace("$", "").trim().toDouble()*//*
                        binding.tvCard.text.toString().replace("$", "").replace("Card (", "")
                            .replace(")", "").trim().toDouble()
                } else {
                    paymentviewModel.totalPayAmount(
                        binding.tvCash0.text.toString().replace("$", "").trim().toDouble()
                    )
                    paymentAmount =
                        binding.tvCash0.text.toString().repxlace("$", "").trim().toDouble()
                }

                cashPaymentWithVariation(dynamicPaymentType = getString(R.string.synergy) ?: "")
            } else
                errorDisplay("Please check your Network Connectivity.")

        }*/

        binding.tvCash0.setOnSingleClickListener {
            if (InternetUtils.isInternetAvailable(requireActivity().applicationContext)) {
                disconnectSyncChannel()

                restrictTvCashClicks()
                transactionInProgress()

                if (android.os.Build.BRAND.contains("Landi", ignoreCase = true)) {
//                    EventBus.getDefault().post(MessageEvent(Constants.CASHBOX, true))
                    EventBus.getDefault().post(CashBoxEvent(Constants.CASHBOX))
                } else {
                    SunmiPrintHelper.getInstance().openCashBox()
                }


                custom_paymentAmount = 0.0

                paymentviewModel.totalPayAmount(
                    binding.tvCash0.text.toString().replace("$", "").trim().toDouble()
                )
                paymentAmount = binding.tvCash0.text.toString().replace("$", "").trim().toDouble()
                prefProvider.setValueboolean(Constants.IS_SELL_OR_ADD_VALUE_GIFT_CARD, false)
                cashPaymentWithVariation()
            } else
                errorDisplay("Please check your Network Connectivity.")

        }
        binding.tvCash1.setOnSingleClickListener {

            if (InternetUtils.isInternetAvailable(requireActivity().applicationContext)) {
                disconnectSyncChannel()
                restrictTvCashClicks()
                transactionInProgress()
//                SunmiPrintHelper.getInstance().openCashBox()
                if (android.os.Build.BRAND.contains("Landi", ignoreCase = true)) {
//                    EventBus.getDefault().post(MessageEvent(Constants.CASHBOX, true))
                    EventBus.getDefault().post(CashBoxEvent(Constants.CASHBOX))
                } else {
                    SunmiPrintHelper.getInstance().openCashBox()
                }

                custom_paymentAmount =
                    binding.tvCash1.text.toString().replace("$", "").trim().toDouble()
                prefProvider.setValueboolean(Constants.IS_SELL_OR_ADD_VALUE_GIFT_CARD, false)
                cashPaymentWithVariation()
            } else
                errorDisplay("Please check your Network Connectivity.")
        }
        binding.tvCash2.setOnSingleClickListener {
            if (InternetUtils.isInternetAvailable(requireActivity().applicationContext)) {
                disconnectSyncChannel()
                restrictTvCashClicks()
                transactionInProgress()
//                SunmiPrintHelper.getInstance().openCashBox()
                if (android.os.Build.BRAND.contains("Landi", ignoreCase = true)) {
//                    EventBus.getDefault().post(MessageEvent(Constants.CASHBOX, true))
                    EventBus.getDefault().post(CashBoxEvent(Constants.CASHBOX))
                } else {
                    SunmiPrintHelper.getInstance().openCashBox()
                }
                custom_paymentAmount =
                    binding.tvCash2.text.toString().replace("$", "").trim().toDouble()
                prefProvider.setValueboolean(Constants.IS_SELL_OR_ADD_VALUE_GIFT_CARD, false)
                cashPaymentWithVariation()
            } else
                errorDisplay("Please check your Network Connectivity.")
        }
        binding.tvCash3.setOnSingleClickListener {

            if (InternetUtils.isInternetAvailable(requireActivity().applicationContext)) {
                disconnectSyncChannel()
                restrictTvCashClicks()
                transactionInProgress()
//                SunmiPrintHelper.getInstance().openCashBox()

                if (android.os.Build.BRAND.contains("Landi", ignoreCase = true)) {
//                    EventBus.getDefault().post(MessageEvent(Constants.CASHBOX, true))
                    EventBus.getDefault().post(CashBoxEvent(Constants.CASHBOX))
                } else {
                    SunmiPrintHelper.getInstance().openCashBox()
                }

                custom_paymentAmount =
                    binding.tvCash3.text.toString().replace("$", "").trim().toDouble()
                prefProvider.setValueboolean(Constants.IS_SELL_OR_ADD_VALUE_GIFT_CARD, false)
                cashPaymentWithVariation()
            } else
                errorDisplay("Please check your Network Connectivity.")
        }
        binding.tvCustomAmount.setOnSingleClickListener {
            if (InternetUtils.isInternetAvailable(requireActivity().applicationContext)) {
                transactionInProgress()
                paymentAmount = binding.tvCash0.text.toString().replace("$", "").trim().toDouble()
                val bundleVal = Bundle().apply {
                    putDouble("totalprice", ((paymentAmount)))
                    putDouble("amountToDisplay", ((paymentAmount)))
                }
                findNavController().navigate(
                    R.id.action_paymentBoldPosFragment_to_customAmountFragment,
                    bundleVal
                )
                prefProvider.setValueboolean(Constants.IS_SELL_OR_ADD_VALUE_GIFT_CARD, false)
            } else
                errorDisplay("Please check your Network Connectivity.")
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
            with(binding) {
                relativeMain.visible()
                llGiftCard.gone()
                edtGiftCardNumber.text?.clear()
            }
            /*try {
                posLink.CancelTrans()
            }catch (e:Exception){

            }*/
        }

        binding.txtCharge.setOnSingleClickListener {

            MethodUtils.hideKeyboard(requireActivity())

            subTotalPrice = String.format("%.2f", subTotalPrice / isSelectedCount).toDouble()

            EventBus.getDefault().post(
                MessageEvent(
                    "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_binding.txtCharge.setOnSingleClickListener dashboardViewModel.subTotalPrice-> ${
                        Gson().toJson(dashboardViewModel.subTotalPrice)
                    } , isSelectedCount-> ${isSelectedCount}", true
                )
            )

            totalServiceCharge =
                String.format("%.2f", totalServiceCharge / isSelectedCount).toDouble()
            totalTax = String.format("%.2f", totalTax / isSelectedCount).toDouble()
            totalDiscount = String.format("%.2f", totalDiscount / isSelectedCount).toDouble()
            cashDiscountSurcharge = if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == GIFT_CARD) {
                0.0
            } else {
                MethodUtils.getLatestCashDiscountOrSurCharge(
                    WholetotalPrice / isSelectedCount,
                    prefProvider,
                    requireContext()
                )
            }
            paymentAmount = String.format("%.2f", WholetotalPrice / isSelectedCount).toDouble()

            EventBus.getDefault().post(
                MessageEvent(
                    "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.txtCharge.setOnSingleClickListener() paymentAmount-> ${
                        Gson().toJson(paymentAmount)
                    }, WholetotalPrice -> ${Gson().toJson(WholetotalPrice)}, isSelectedCount -> ${
                        Gson().toJson(
                            isSelectedCount
                        )
                    }", true
                )
            )

            Log.d(TAG, "paymentClick: cashDiscountSurcharge " + cashDiscountSurcharge)
            Log.d(TAG, "paymentClick: paymentAmount  " + paymentAmount)
            if (cashDiscountType == "SurCharge") {
                paymentAmount =
                    String.format("%.2f", paymentAmount + (cashDiscountSurcharge))
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

            transactionInProgress()
            startTransactionWithGiftCardPayment()
//            AlertUtils.showCustomAlertWithListenerWithOKCancelUpdated(
//                requireContext(),
//                getString(R.string.are_you_sure_proceed),
//                "Ok"
//            ) { dialogInterface, clickedButton ->
//                if (clickedButton == 0) {
//
//                } else {
//                    dialogInterface?.dismiss()
//                }
//            }
        }
    }

    private fun showPaymentNotConnectedMessage() {
        runOnUiThread(object : java.lang.Runnable {
            override fun run() {
                binding.llCreditCard.isEnabled = true
                dismissProgressDialog()
            }
        })
        errorDisplay("Please connect a payment device.")
    }

    private fun makeDejavooCaptureRequestForPreAuth() {
        paymentCoroutineScope = CoroutineScope(Dispatchers.IO + paymentCoroutineExceptionHandler)
        paymentCoroutineScope.launch {
            val gatewayType = PaymentGatewayType.DEJAVOO
            val paymentGateway = paymentGatewayFactory.create(gatewayType)

            val finalAmount = (paymentAmount * 100).toInt()

            val paymentDetailsResponse = paymentviewModel.preAuthData

            /* Process Payment */
            var dejavoo = Dejavoo(
                registerId = "4986101",
                authKey = "kwg2GRbykg",
                tpn = "659324491704",
                paymentType = "Credit",
                transType = "Capture",
                amount = finalAmount.toString(),
                tip = "",
                refId = paymentDetailsResponse?.ecrRefNum.toString(),
                printReceipt = false,
                performedBy = prefProvider.employeeName(),
                isProd = false,
                txnType = TransactionType.CREDIT_SALE
            )

            context?.let {
                paymentGateway.performPreAuth(
                    it.applicationContext,
                    dejavoo,
                    onSuccess = { tResponse ->
                        var transactionJsonResponse = Gson().fromJson<String>(
                            tResponse,
                            String::class.java
                        )
                        val factory: XmlPullParserFactory = XmlPullParserFactory.newInstance()
                        factory.setNamespaceAware(true)
                        val xpp: XmlPullParser = factory.newPullParser()
                        xpp.setInput(StringReader(transactionJsonResponse))
                        var eventType = xpp.eventType

                        val parsedXml =
                            parseXml(transactionJsonResponse)/*.getElementsByTagName("xmp").item(0)?.textContent.toString()*/
                        var Message = ""
                        var RefId = ""
                        var RegisterId = ""
                        var TPN = ""
                        var AuthCode = ""
                        var PNRef = ""
                        var TransNum = ""
                        var ResultCode = ""
                        var RespMSG = ""
                        var PaymentType = ""
                        var Voided = ""
                        var TransType = ""
                        var SN = ""
                        var ExtData = ""
                        with(parseXml(transactionJsonResponse).childNodes.item(0).childNodes.item(0).childNodes) {
                            for (i in 0 until this.length) {

                                when ((this.item(i) as Element).tagName.toString()) {
                                    "Message" -> Message =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "RefId" -> RefId =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "RegisterId" -> RegisterId =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "TPN" -> TPN =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "AuthCode" -> AuthCode =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "PNRef" -> PNRef =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "TransNum" -> TransNum =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "ResultCode" -> ResultCode =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "RespMSG" -> RespMSG =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "PaymentType" -> PaymentType =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "Voided" -> Voided =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "TransType" -> TransType =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "SN" -> SN =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "ExtData" -> ExtData =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    else -> {

                                    }
                                }
                            }
                        }
//                    parseXml(transactionJsonResponse).childNodes.item(0).childNodes.item(0).childNodes
                        if (Message.equals("Canceled") || Message.equals("Error")) {
                            dismissProgressDialogWithAlert(RespMSG.replace("%20", " "))
                        } else if (Message.contains("Approved")) {
                            makePaymentCreditCardDejavoo(RefId, ExtData)
                        }
                    },
                    onFailure = { errorMessage ->
                        Log.e("Dejavoo: ",errorMessage)
                        EventBus.getDefault()
                            .post(
                                MessageEvent(
                                    "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew makeDejavooPaymentRequest()-> ${
                                        Gson().toJson(
                                            errorMessage
                                        )
                                    } "
                                )
                            )
//                        dismissProgressDialogWithAlert()
                    }
                )
            }
        }
    }

    private fun startTransactionWithGiftCardPayment() {
        binding.txtChargeGC.isEnabled = false
        val giftCardNumber = binding.edtGiftCardNumber.rawText.toString().trim()
        Log.e(TAG,"checkGiftCardNumber:  ${giftCardNumber}")

        if (giftCardNumber.isEmpty() || giftCardNumber.length < 8) {
            AlertUtils.showCustomAlert(
                requireContext(),
                "Please enter physical or digital gift card number."
            )
            binding.txtChargeGC.isEnabled = true
            return
        } else if (giftCardNumber.isNotEmpty() && giftCardNumber.length >= 8) {
            dashboardViewModel.checkCardExistOrNotOnSell(giftCardNumber)
            dashboardViewModel.isGiftCardSold.observe(viewLifecycleOwner) { event ->
                event.getContentIfNotHandled()?.let {
                    Log.e("ObserverdGiftCardProgress", it.toString())
                    if (it) {
                        closePaxRequest()
                        Log.e("checkGiftCardNumber", "giftCardNumber:  ${giftCardNumber}")
                        giftCardViewModel.physicalGiftCardCheckBalanceBeforePay(
                            GiftCardCheckBalanceRequest(name = giftCardNumber)
                        )
                    } else {
                        AlertUtils.showCustomAlertWithListenerWithOK(
                            requireContext(),
                            "This gift card has not been activated.",
                            object : DialogInterface.OnClickListener {
                                override fun onClick(p0: DialogInterface?, p1: Int) {
                                    binding.edtGiftCardNumber.text?.clear()
                                    /*binding.frameLayoutId.gone()
                                    binding.relativeMain.visible()
                                    binding.llManualCard.visible()
                                    binding.llGiftCard.gone()*/
                                }
                            })
                    }


                }
            }

        }
        /**
         * Added to prevent multiple api calls on multiple clicks.
         */
        Handler(Looper.getMainLooper()).postDelayed({
            binding.txtChargeGC.isEnabled = true  // Re-enable the button after delay
        }, 3000)  // 1000ms = 1 second (adjust the delay based on your use case)
    }

    /* This function will call the pax for gift card reading */
    private fun startPAXWithGiftCard() {
        runBlocking {
            initPOSLink()
            GlobalScope.launch {
                posLink.SetCommSetting(
                    SettingINI.getCommSettingFromFile(
                        requireContext(),
                        "/storage/emulated/0/Download/" + SettingINI.FILENAME
                    )
                )

                val manageRequest = ManageRequest()
                manageRequest.TransType = manageRequest.ParseTransType("INPUTACCOUNT")
                manageRequest.EDCType = manageRequest.ParseEDCType("GIFT")
                manageRequest.MagneticSwipeEntryFlag = "1";
                manageRequest.ManualEntryFlag = "1";
                manageRequest.ContactlessEntryFlag = "0";
                manageRequest.TimeOut = "200";
                manageRequest.ContinuousScreen = "0";
                manageRequest.ECRRefNum = System.currentTimeMillis()
                    .toString(); // Enable swipe entry (adjust based on your use case)
                posLink.ManageRequest = manageRequest
                val result = posLink.ProcessTrans()

                if (result.Code === ProcessTransResult.ProcessTransResultCode.OK) {
                    val msg = Message()
                    msg.what = Constants.TRANSACTION_SUCCESSED
                    msg.obj = posLink.ManageResponse
                    if (msg.obj != null) {
                        val response = msg.obj as com.pax.poslink.ManageResponse

                        val resultCode = response.ResultCode
                        if (resultCode == "000000") {
                            withContext(Dispatchers.Main) {
                                binding.apply {
                                    btnReadCard?.isClickable = true
                                    if (response.PAN.isNullOrEmpty()) {
                                        edtGiftCardNumber.setText(response.Track2Data.toString())
                                        Log.d(
                                            "VALID: ",
                                            "Here__Track: ${response.Track2Data.toString()}"
                                        )
                                    } else {
                                        edtGiftCardNumber.setText(response.PAN.toString())
                                        Log.d("VALID: ", "Here__Pan: ${response.PAN.toString()}")
                                    }
                                    startTransactionWithGiftCardPayment()
                                }
                            }
                        } else {
                            runOnUiThread(object : Runnable {
                                override fun run() {
                                    binding.btnReadCard?.isClickable = true
                                }
                            })
                        }
                    }
                } else {
                    runOnUiThread(object : Runnable {
                        override fun run() {
                            binding.btnReadCard?.isClickable = true
                        }
                    })
                }
            }
        }
    }

    fun parseXml(xmlContent: String): Document {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        return builder.parse(xmlContent.byteInputStream())
    }

    private fun makeDejavooPaymentRequest() {
        /*CoroutineScope(Dispatchers.Main).launch {
            ProgressUtils.showProgressDialog("Please wait...", requireActivity(), 0)
        }*/
        runOnUiThread(object : java.lang.Runnable {
            override fun run() {
                showProgressDialog()
            }
        })
        paymentCoroutineScope = CoroutineScope(Dispatchers.IO + paymentCoroutineExceptionHandler)
        paymentCoroutineScope.launch {
            val gatewayType = PaymentGatewayType.DEJAVOO
            val paymentGateway = paymentGatewayFactory.create(gatewayType)

            val amt = String.format("%.2f", (paymentAmount)).toDouble()
            val tip_amt = String.format("%.2f", tipAmountToPaymentDevice).toDouble()

            /*    Test Credentials
                  registerId = "4986101",
                authKey = "kwg2GRbykg",
                tpn = "659324491704"
                authToken= "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0cG4iOiI2NTkzMjQ0OTE3MDQiLCJlbWFpbCI6InN1cHBvcnQrMUBwYXlzcG9zLmNvbSIsImlhdCI6MTczMzc0ODk4M30.spR9JiJS6jt0VMB0MGu9HZQYKUrNaWV-U_pzQ4VCvYw"*/

            /* Process Payment */
            var dejavoo = Dejavoo(
                registerId =  prefProvider.getValue(
                    Constants.DEJAVOO_REGISTER_ID,""
                ),
                authKey = prefProvider.getValue(
                    Constants.DEJAVOO_AUTH_KEY,""
                ),
                tpn = prefProvider.getValue(
                    Constants.DEJAVOO_TPN,""
                ),
                paymentType = "Credit",
                transType = "Sale",
                amount = amt.toString(),
                tip = if (tip_amt > 0) tip_amt.toString() else "",
                refId = "Ref${System.currentTimeMillis()}",
                printReceipt = false,
                performedBy = prefProvider.employeeName(),
                isProd = Constants.paymentLive,
                txnType = TransactionType.CREDIT_SALE
            )

            context?.let {
                paymentGateway.processPayment(
                    it.applicationContext,
                    dejavoo,
                    onSuccess = { tResponse ->
                        dismissProgressDialog()
                        var transactionJsonResponse = Gson().fromJson<String>(
                            tResponse,
                            String::class.java
                        )
                        val factory: XmlPullParserFactory = XmlPullParserFactory.newInstance()
                        factory.setNamespaceAware(true)
                        val xpp: XmlPullParser = factory.newPullParser()
                        xpp.setInput(StringReader(transactionJsonResponse))
                        var eventType = xpp.eventType

                        val parsedXml =
                            parseXml(transactionJsonResponse)/*.getElementsByTagName("xmp").item(0)?.textContent.toString()*/
                        var Message = ""
                        var RefId = ""
                        var RegisterId = ""
                        var TPN = ""
                        var AuthCode = ""
                        var PNRef = ""
                        var TransNum = ""
                        var ResultCode = ""
                        var RespMSG = ""
                        var PaymentType = ""
                        var Voided = ""
                        var TransType = ""
                        var SN = ""
                        var ExtData = ""
                        with(parseXml(transactionJsonResponse).childNodes.item(0).childNodes.item(0).childNodes) {
                            for (i in 0 until this.length) {

                                when ((this.item(i) as Element).tagName.toString()) {
                                    "Message" -> Message =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "RefId" -> RefId =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "RegisterId" -> RegisterId =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "TPN" -> TPN =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "AuthCode" -> AuthCode =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "PNRef" -> PNRef =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "TransNum" -> TransNum =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "ResultCode" -> ResultCode =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "RespMSG" -> RespMSG =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "PaymentType" -> PaymentType =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "Voided" -> Voided =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "TransType" -> TransType =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "SN" -> SN =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    "ExtData" -> ExtData =
                                        this.item(i).childNodes.item(0).nodeValue.intern() ?: ""
                                    else -> {

                                    }
                                }
                            }
                        }

                        // Split the data into key-value pairs
                        val keyValuePairs = ExtData.split(",")
/*

                        // Create a map to store the parsed data
                        val dataMap = mutableMapOf<String, String>()

                        // Process each key-value pair
                        for (pair in keyValuePairs) {
                            val keyValue = pair.split("=")
                            if (keyValue.size == 2) {
                                val key = keyValue[0]
                                val value = keyValue[1]
                                dataMap[key] = value
                            }
                        }
*/


                        // Access the specific values
                        cardLastDigits = getPaymentDetails(keyValuePairs,"AcntLast4")?:"Not Found"
                        EDCType = getPaymentDetails(keyValuePairs,"CardType")?:"Not Found"
//                        EDCType = dataMap["CardType"] ?: "Not Found"

//                    parseXml(transactionJsonResponse).childNodes.item(0).childNodes.item(0).childNodes
                        if (Message.equals("Canceled") || Message.equals("Error")) {
                            dismissProgressDialogWithAlert(RespMSG.replace("%20", " "))

                        } else if (Message.contains("Approved")) {
//                            Check for Order Types
                            if (prefProvider.getValue(
                                    ORDER_TYPE,
                                    TAKEOUT
                                ).equals(GIFT_CARD)
                            ) {
                                if (prefProvider.getValueboolean(
                                        Constants.IS_ADD_VALUE_IN_GIFT_CARD,
                                        false
                                    )
                                ) {
                                    giftCardViewModel.paxResponse = Constants.DEJAVOO
                                    giftCardViewModel.cardNumberLast4 = cardLastDigits
                                    giftCardViewModel.cardNamePax = EDCType
                                    giftCardViewModel.transactionID = RefId
                                    addValueInGiftCardUsingCard(paymentAmount)
                                }else{
                                    giftCardViewModel.paxResponse = Constants.DEJAVOO
                                    giftCardViewModel.cardNumberLast4 = cardLastDigits
                                    giftCardViewModel.cardNamePax = EDCType
                                    giftCardViewModel.transactionID = RefId
                                    sellGiftCardUsingCard(paymentAmount)
                                }

                            }else{
                                makePaymentCreditCardDejavoo(RefId, ExtData)
                            }
                        }
                    },
                    onFailure = { errorMessage ->
                        Log.e("Dejavoo: ", errorMessage)
                        EventBus.getDefault()
                            .post(
                                MessageEvent(
                                    "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew makeDejavooPaymentRequest()-> ${
                                        Gson().toJson(
                                            errorMessage
                                        )
                                    } "
                                )
                            )
                        dismissProgressDialogWithAlert()
                        if (dashboardViewModel.customerGivenTipBefore.value == false) {
                            dashboardViewModel.paymentInProgress.value = false
                            dashboardViewModel.tipBeforeEnabled = true
                            dashboardViewModel.removeMainCart.value = true
                        }
                    }
                )
            }
        }
    }

    private fun getPaymentDetails(keyValuePairs: List<String>, toFind: String): String? {
        try {
            var key = keyValuePairs.filter { it.contains(toFind) }.first()
            return key.substring(key.lastIndexOf('=')+1)
        }catch (e:Exception){
            return ""
        }
    }

    private fun makePaymentCreditCardDejavoo(txnid: String?, extData: String?) {
        paymentviewModel.dejavooRefTxnId = txnid
//        paymentviewModel.valorTransactionNumber = transactionNumber
        paymentAmount -= tipAmount
        paymentAmount = MethodUtils.roundOffAmountDouble(paymentAmount)
        paymentType = "Card"
        Log.d(
            "LOADER::",
            "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
        )

//        LogUtil.logE(TAG, "cartList:  ${Gson().toJson(cartList)}")
//        LogUtil.logE(TAG, "cartListcartItems:  ${Gson().toJson(cartItems)}")
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
        var cartModel: CartModel? = null
        try {
            cartModel =
                Gson().fromJson<CartModel?>(
                    prefProvider.getValue("CART_MODEL1", ""),
                    CartModel::class.java
                )
        } catch (e: Exception) {

        }
        var cartModel2: CartModel? = null
        try {
            cartModel2 = Gson().fromJson<CartModel?>(
                prefProvider.getValue("CART_MODEL2", ""),
                CartModel::class.java
            )
        } catch (e: Exception) {

        }

        if (cartList == null) {
            Log.d(
                "LOADER::",
                "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
            )

            CoroutineScope(Dispatchers.Main).launch {
                getCartModelsList()
            }

            if (cartModel != null) {
                dashboardViewModel.cartModel = cartModel
                cartList = cartModel
            } else if (cartModel2 != null) {
                dashboardViewModel.cartModel = cartModel2
                cartList = cartModel2
            } else if (dashboardViewModel.cartModel == null) {
                runBlocking {
                    try {
                        var model =
                            CoroutineScope(Dispatchers.IO).async { dashboardViewModel.getCartModelBackup() }
                                .await().last().data

                        EventBus.getDefault().post(
                            MessageEvent(
                                "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_CART_BACKUP_MODEL -> model -> ${
                                    Gson().toJson(model)
                                }", true
                            )
                        )
                        cartList = Gson().fromJson(model, CartModel::class.java)
                        dashboardViewModel.cartModel =
                            Gson().fromJson(model, CartModel::class.java)
                        Log.d(
                            "LOADER::",
                            "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
                        )

                    } catch (e: Exception) {
                        var models =
                            CoroutineScope(Dispatchers.IO).async { dashboardViewModel.getAllCartModels() }
                                .await()
                        if (models.isNotEmpty()) {
                            dashboardViewModel.cartModel = models.last()
                            cartList = models.last()
                            Log.d(
                                "LOADER::",
                                "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
                            )

                        } else {
                            Log.d(
                                "LOADER::",
                                "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
                            )

                            /*Continuous loading shall occur due to the cartModel null */

                        }
                    }
                }
            }
        }

        Log.d(
            "LOADER::",
            "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
        )

        oldItems = prefProvider.getValue(Constants.OLD_ITEM_BASE_CUSTOM_ITEM, "")
        val listType = object : TypeToken<java.util.ArrayList<TbCartItem>>() {}.type
        lateinit var oldCartItemsList: ArrayList<TbCartItem>

        if (oldItems.isNotEmpty()) {

            val oldCartItemsJson = prefProvider.getValue(Constants.OLD_ITEM_BASE_CUSTOM_ITEM, "")

            oldCartItemsList = Gson().fromJson(oldCartItemsJson, listType) as ArrayList<TbCartItem>
        } else {
            prefProvider.setValueboolean(DO_PRINT, true)
        }

        EventBus.getDefault().post(
            MessageEvent(
                "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_makePaymentCreditCard() -> cartList -> ${
                    Gson().toJson(cartList)
                }", true
            )
        )
        if (cartList == null || cartList?.items == null || cartList?.items?.isEmpty() == true) {
            Log.d(
                "LOADER::",
                "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
            )

            EventBus.getDefault().post(
                MessageEvent(
                    "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_makePaymentCreditCard() -> null Case"
                )
            )

            var items: ArrayList<TbItem>? = ArrayList()
            /*  for (item in dashboardViewModel.currentCartItems) {*/
            for (item in dashboardViewModel.currentCartItems) {
                var tbItem = TbItem()
                tbItem.apply {

                    isItemEdited = item.isItemEdited

                    itemQuantity = item.itemQuantity
                    id = item.id
                    cartItemId = item.cartItemId
                    itemId = item.itemId
                    categoryId = item.categoryId
                    categoryName = item.categoryName
                    createdAt = item.createdAt
                    customItemCount = item.customItemCount
                    dineInSort = item.dineInSort
                    customItemID = item.customItemCount
                    discountId = item.discountId
                    discountPrice = item.discountPrice
                    discountType = item.discountType
                    guestItemId = item.guestItemId
                    headerPositionDinein = item.headerPositionDinein
                    hide_status = item.hide_status
                    isHide = item.isHide
                    imageUrl = item.imageUrl
                    isChecked = item.isChecked
                    isDeleted = item.isDeleted
                    isDestroy = item.isDestroy
                    isEdited = item.isEdited
                    isFired = item.isFired
                    isManualSales = item.isManualSales
                    isPaid = item.isPaid
                    itemOriginalModifiersList = item.itemOriginalModifiersList
                    modifier_set_ids = item.modifier_set_ids
                    modifiers = item.modifiers
                    name = item.name
                    note = item.note
                    manualSaleId = item.manualSaleId
                    optionSets = item.optionSets
                    website_hide_status = item.website_hide_status
                    variationsAttributes = item.variationsAttributes
                    updatedAt = item.updatedAt
                    timeStamp = item.timeStamp
                    thumbImageUrl = item.thumbImageUrl
                    taxes = item.taxes
                    sort = item.sort
                    sku = item.sku
                    singleItemPrice = item.singleItemPrice
                    shortDescription = item.shortDescription
                    reorder = item.reorder
                    quantity = item.quantity
                    price = item.price
                    orderItemId = item.orderItemId

                    try {
                        if (oldCartItemsList.isNotEmpty()) { // Order is updated

                            prefProvider.setValueboolean(Constants.DO_PRINT, true)

                            oldCartItemsList.forEach { oldItem ->
                                if ((oldItem.cartItemId == item.cartItemId) &&
                                    (oldItem.categoryId == item.categoryId) &&
                                    (oldItem.employeeID == item.employeeID) &&
                                    (oldItem.itemId == item.itemId) &&
                                    (oldItem.name.equals(item.name))
                                ) {

                                    if (item.itemQuantity != oldItem.itemQuantity) {
                                        isItemEdited = true
                                    }

                                }
                            }
                        }
                    } catch (e: Exception) {
                        //order is not updated , its new order
                    }
                }

                items!!.add(tbItem)

            }

            if (oldItems.isNotEmpty()) {

                for (item in dashboardViewModel.currentCartItems) {
                    try {
                        if (oldItems.isNotEmpty()) {
                            /*Added by Rahul to solve the modifiers not removing issue*/

                            val notPresentItems =
                                oldCartItemsList.filter { it.cartItemId != item.cartItemId }

                            if (true) {
                                var isFound = false

                                for (notPresentData in oldCartItemsList) {
                                    if (items != null) {
                                        for (it in items) {
                                            if (it.cartItemId == notPresentData.cartItemId) {
                                                isFound = true
                                                break
                                            } else isFound = false
                                        }
                                    }
                                    if (!isFound) {

                                        prefProvider.setValueboolean(DO_PRINT, true)
//                                        Not found
                                        isFound = false

                                        var tbItemDeleted = TbItem()
                                        tbItemDeleted.id = notPresentData.id
                                        tbItemDeleted.cartItemId = notPresentData.cartItemId
                                        tbItemDeleted.itemId = notPresentData.itemId
                                        tbItemDeleted.itemQuantity = notPresentData.itemQuantity
                                        tbItemDeleted.categoryId = notPresentData.categoryId
                                        tbItemDeleted.categoryName = notPresentData.categoryName
                                        tbItemDeleted.createdAt = notPresentData.createdAt
                                        tbItemDeleted.customItemCount =
                                            notPresentData.customItemCount
                                        tbItemDeleted.dineInSort = notPresentData.dineInSort
                                        tbItemDeleted.customItemID = notPresentData.customItemCount
                                        tbItemDeleted.discountId = notPresentData.discountId
                                        tbItemDeleted.discountPrice = notPresentData.discountPrice
                                        tbItemDeleted.discountType = notPresentData.discountType
                                        tbItemDeleted.guestItemId = notPresentData.guestItemId
                                        tbItemDeleted.headerPositionDinein =
                                            notPresentData.headerPositionDinein
                                        tbItemDeleted.hide_status = notPresentData.hide_status
                                        tbItemDeleted.isHide = notPresentData.isHide
                                        tbItemDeleted.imageUrl = notPresentData.imageUrl
                                        tbItemDeleted.isChecked = notPresentData.isChecked
                                        tbItemDeleted.isDeleted = notPresentData.isDeleted
                                        tbItemDeleted.isDestroy = true
                                        tbItemDeleted.isEdited = notPresentData.isEdited
                                        tbItemDeleted.isFired = notPresentData.isFired
                                        tbItemDeleted.isManualSales = notPresentData.isManualSales
                                        tbItemDeleted.isPaid = notPresentData.isPaid
                                        tbItemDeleted.itemOriginalModifiersList =
                                            notPresentData.itemOriginalModifiersList
                                        tbItemDeleted.quantity = notPresentData.quantity
                                        tbItemDeleted.modifier_set_ids =
                                            notPresentData.modifier_set_ids
                                        tbItemDeleted.modifiers = notPresentData.modifiers
                                        tbItemDeleted.name = notPresentData.name
                                        tbItemDeleted.note = notPresentData.note
                                        tbItemDeleted.manualSaleId = notPresentData.manualSaleId
                                        tbItemDeleted.optionSets = notPresentData.optionSets
                                        tbItemDeleted.website_hide_status =
                                            notPresentData.website_hide_status
                                        tbItemDeleted.variationsAttributes =
                                            notPresentData.variationsAttributes
                                        tbItemDeleted.updatedAt = notPresentData.updatedAt
                                        tbItemDeleted.timeStamp = notPresentData.timeStamp
                                        tbItemDeleted.thumbImageUrl = notPresentData.thumbImageUrl
                                        tbItemDeleted.taxes = notPresentData.taxes
                                        tbItemDeleted.sort = notPresentData.sort
                                        tbItemDeleted.sku = notPresentData.sku
                                        tbItemDeleted.singleItemPrice =
                                            notPresentData.singleItemPrice
                                        tbItemDeleted.shortDescription =
                                            notPresentData.shortDescription
                                        tbItemDeleted.reorder = notPresentData.reorder
                                        tbItemDeleted.price = notPresentData.price
                                        tbItemDeleted.orderItemId = notPresentData.orderItemId

                                        items!!.add(tbItemDeleted)
                                        break
                                    }

                                }

                            }
//                        for (notPresentItem in notPresentItems) {
//                            if (item.itemId != notPresentItem.itemId) {


//                            }
//                        }
                            Log.d("UNCOMMON:::", Gson().toJson(notPresentItems))


                        }
                    } catch (e: Exception) {
                    }

                }

            }


            /*Adding the deleted items*/
            cartList?.items = items
        }

        Log.d(
            "LOADER::",
            "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
        )

        prefProvider.setValue(Constants.OLD_ITEM, "")
        prefProvider.setValue(Constants.OLD_ITEM_BASE, "")
        prefProvider.setValue(Constants.OLD_ITEM_BASE_CUSTOM_ITEM, "")


        val myRequest = cartList?.let {
            txnid?.let {
                if (RefNumber.isEmpty()) {
                    RefNumber = it
                    /*We are adding DEJAVOO in EXT DATA, This will change in further release, we will send the Dejavoo's Ext_Data in this Parameter */
                    ExtData = "${Constants.DEJAVOO} : $extData?"
                }
            }

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
        LogUtil.logE(TAG, "myRequestOriginal ${Gson().toJson(myRequest)}")
        if (myRequest != null) {
            paymentviewModel.totalPayAmount(paymentAmount)

            if (prefProvider.getValue(Constants.GIFT_CARD_TYPE, "")
                    .equals("digital", ignoreCase = true)
            ) {
                myRequest.order.orderTypeId = prefProvider.getValueInt(Constants.ORDER_TYPE_ID, -1)
            }

            EventBus.getDefault().post(
                MessageEvent(
                    "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_makePaymentCreditCard() , myRequest -> ${
                        Gson().toJson(myRequest)
                    } _1"
                )
            )
            Log.d(
                "LOADER::",
                "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
            )

            /* //FILE ASSERTION
             MainActivity.writeToFile(
                 Gson().toJson(myRequest),
                 "Pay_".plus(myRequest.order.offlineId.toString()),
                 activity?.filesDir,
                 activity!!
             )*/

            paymentAttributesRequest(myRequest)
        }
    }

    private fun startCashOrGiftCardTransaction(b: Boolean) {

    }

    private fun transactionInProgress() {
        dashboardViewModel.paymentInProgress.value = true
    }
    //Restrict user from clicking cash value multiple times
    private fun restrictTvCashClicks() {
        binding.apply {
            llCreditCard.isEnabled = false
            tvCash0.isEnabled = false
            tvCash1.isEnabled = false
            tvCash2.isEnabled = false
            tvCash3.isEnabled = false
        }

        Handler().postDelayed({
            binding.apply {
                llCreditCard.isEnabled = true
                tvCash0.isEnabled = true
                tvCash1.isEnabled = true
                tvCash2.isEnabled = true
                tvCash3.isEnabled = true
            }
        }, 5000)

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

        giftCardViewModel.giftCardError.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it.isNotEmpty()) {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        requireActivity(),
                        it,
                        object : DialogInterface.OnClickListener {
                            override fun onClick(p0: DialogInterface?, p1: Int) {
                                giftCardViewModel.clearGiftCardObserver()
                            }
                        })
                }
            }
        }

        giftCardViewModel.giftCardCheckBalanceData.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it.data != null) {
                    if (isAdded) {

                        if (it.data.amount == 0.0) {
//                            binding.edtGiftCardNumber.setText("")
                            prefProvider.setValueboolean(IS_GIFT_CARD_REDEEM, false)
                            AlertUtils.showCustomAlertWithListenerWithOK(
                                requireContext(),
                                message = getString(R.string.msg_insufficient_gift_card_balance)
                            ) { _, _ ->
                                binding.edtGiftCardNumber.text?.clear()
                                binding.frameLayoutId.gone()
                                binding.relativeMain.visible()
                                binding.llManualCard.gone()
                                binding.llGiftCard.gone()

                            }
                        } else {

                            prefProvider.setValueboolean(Constants.SPLIT_ENABLE, true)

                            Log.e("checkTotalAmount","totalPrice:  ${totalPrice}")
                            Log.e("checkDataAmount","amount:  ${it.data.amount}")
                            if (totalPrice.toDouble() > it.data.amount) {

                                splitAllAmounts(
                                    Constants.SUB_TOTAL,
                                    it.data.amount?.toPrecision(2).toDouble()
                                )
                            }
                            else{
                                splitAllAmounts(
                                    Constants.SUB_TOTAL,
                                    totalPrice.toDouble()
                                )
                            }
                            splitAllAmounts(Constants.TOTAL_DISCOUNT, 0.00)
                            splitAllAmounts(Constants.TAX_CHARGE, 0.00)
                            splitAllAmounts(Constants.SERVICE_CHARGE, 0.00)
                            splitAllAmounts(
                                Constants.CASH_DISCOUNT_SURCHARGE,
                                0.00
                            )
                            splitAllAmounts(Constants.TIP, 0.0)

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
                            paymentAmount = it.data.amount
                            paymentviewModel.totalPayAmount(it.data.amount)
                            redeemGiftCard()

                          /*  custom_paymentAmount = 0.0

                            val actualTotalAmountWithTip =
                                (WholetotalPrice / isSelectedCount) + tipAmount

                            val giftCardBalanceAmount = it.data.amount

                            if (actualTotalAmountWithTip <= giftCardBalanceAmount) {
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
                                    message = "Your GiftCard Balance is $${
                                        giftCardBalanceAmount.toPrecision(
                                            2
                                        )
                                    }. Please use split payment."
                                ) { _, _ ->
                                }
                            }
                            binding.edtGiftCardNumber.setText("")*/
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
    }

    @Inject
    lateinit var paymentGatewayFactory: PaymentGatewayFactory
    lateinit var paymentCoroutineScope: CoroutineScope

    val paymentCoroutineExceptionHandler =
        CoroutineExceptionHandler { coroutineContext, exception ->
            EventBus.getDefault()
                .post(
                    MessageEvent(
                        "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew makeValorPaymentRequest()-> ${
                            Gson().toJson(
                                exception
                            )
                        } "
                    )
                )
        }

    private fun makeValorPaymentRequest() {
       /* CoroutineScope(Dispatchers.Main).launch {
            ProgressUtils.showProgressDialog("Please wait...", requireActivity(), 0)
        }*/

        runOnUiThread(object : java.lang.Runnable {
            override fun run() {
                showProgressDialog()
            }
        })

        if (this@CheckoutDetailsFragmentNew::paymentCoroutineScope.isInitialized) {
            if (!paymentCoroutineScope.isActive) {
                paymentCoroutineScope =
                    CoroutineScope(Dispatchers.IO + paymentCoroutineExceptionHandler)
            }
        } else {
            paymentCoroutineScope =
                CoroutineScope(Dispatchers.IO + paymentCoroutineExceptionHandler)
        }

        paymentCoroutineScope.launch {
//            CoroutineScope(Dispatchers.Main).launch {
//                        ProgressUtils.dismissProgressDialog()

            val gatewayType = PaymentGatewayType.VALOR
            val paymentGateway = paymentGatewayFactory.create(gatewayType)

            val amt = ((paymentAmount - tipAmountToPaymentDevice) * 100).roundToInt()
            val tip_amt = (tipAmountToPaymentDevice * 100).roundToInt()

            /*    Test Credentials
                  var apiKey = "k3FhfL$$8vu#NEDlfuJwP62MzIeA7Csz"
                  var appID = "GmehAw69S9TEHKm3Bmz2yvxQybYJLgIp"
                  var channelID = "bd967b4e0ccd6309c5ac16634bd367b6"
                  var epi = "2319995597"
                  var endpoint = "status"
                  var transType = TransactionType.CREDIT_SALE
                  var TRAN_MODE = "1"
                  var TRAN_CODE = "1"
                  var amount =
                  var reqTxnId = */

            /* Process Payment */
            var valor = Valor(
                apiKey = prefProvider.getValue(Constants.VALOR_APP_KEY, ""),
                appID = prefProvider.getValue(Constants.VALOR_APP_ID, ""),
                epi = prefProvider.getValue(Constants.VALOR_EPI, ""),
                endpoint = "status",
                txnType = TransactionType.CREDIT_SALE,
                channelId = prefProvider.getValue(Constants.VALOR_CHANNEL_ID, ""),
                transMode = "1",
                transCode = "1",
                reqTxnId = "INV${System.currentTimeMillis()}",
                amount = amt.toString(),
                tipAmount = if (tip_amt > 0) tip_amt.toString() else "",
                tipEntry = if (tip_amt > 0) "1" else "-1",
                txn_type = "",
                surchargeIndicator = "",
                sale_refund = "",
                ref_txn_id = "",
                isProd = Constants.paymentLive,
                transactionId = ""
            )
            context?.let {
                paymentGateway.processPayment(
                    it.applicationContext,
                    valor,
                    onSuccess = { tResponse ->

                        var transactionJsonResponse = Gson().fromJson<ValorSuccessResponse>(
                            tResponse,
                            ValorSuccessResponse::class.java
                        )
                        transactionJsonResponse.nameValuePairs?.run {
                            this.note?.let {
                                if (it.contains(
                                        "Please Send A New Request",
                                        ignoreCase = true
                                    ) || this.msg?.contains("ready", ignoreCase = true) ?: false
                                ) {
/*
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        ProgressUtils.updateMessage("It is taking longer than usual, Please wait...")
                                    }, 100)*/
                                    makeValorPaymentRequest()
                                    return@processPayment
                                }
                            }

                            this.response?.nameValuePairs?.let {
                                if (it.ERRORMSG?.contains("Transaction Inprogress") ?: false) {
//                                Make a Cancel transaction call
                                    dismissProgressDialogWithAlert("Valor not ready, Please try after sometime...")
                                    return@processPayment
                                }
                            }
                        }

                        transactionJsonResponse.nameValuePairs?.response?.nameValuePairs?.let {
                            if (it.ERRORMSG != null) {
                                dismissProgressDialogWithAlert(it.ERRORMSG)
                            } else {
                                if (it.AUTHRSPTEXT != null) {
                                    if (it.AUTHRSPTEXT!!.contains(
                                            "APPROVAL"
                                        )
                                    ) {
                                        it.MASKEDPAN?.let {
                                            cardLastDigits=it.substring(it.length-4)
                                        }
                                        if (prefProvider.getValue(
                                                ORDER_TYPE,
                                                TAKEOUT
                                            ).equals(GIFT_CARD)
                                        ) {
                                            if (prefProvider.getValueboolean(
                                                    Constants.IS_ADD_VALUE_IN_GIFT_CARD,
                                                    false
                                                )
                                            ) {
                                                giftCardViewModel.paxResponse = Constants.VALOR
                                                giftCardViewModel.cardNumberLast4 = cardLastDigits
                                                giftCardViewModel.cardNamePax = it.ISSUER.toString()
                                                giftCardViewModel.transactionID = it.TXNID.toString()
                                                addValueInGiftCardUsingCard(paymentAmount)
                                            } else {
                                                giftCardViewModel.paxResponse = Constants.VALOR
                                                giftCardViewModel.cardNumberLast4 = cardLastDigits
                                                giftCardViewModel.cardNamePax = it.ISSUER.toString()
                                                giftCardViewModel.transactionID = it.TXNID.toString()
                                                sellGiftCardUsingCard(paymentAmount)
                                            }
                                        } else {
                                            dismissProgressDialogWithAlert()

                                            //Card last four digits and card type

                                            val maskedCard = it.MASKEDPAN.toString()
                                            val cardType = it.ISSUER.toString()
                                            cardLastDigits = maskedCard.substring(maskedCard.length - 5, maskedCard.length) ?: ""
                                            EDCType = cardType


//                                            dismissProgressDialog()
                                            makePaymentCreditCardValor(it.TXNID, it.TRANNO)
                                            /* runOnUiThread(Runnable {
                                                 AlertUtils.showCustomAlert(
                                                     requireContext(),
                                                     it.AUTHRSPTEXT
                                                 )
                                             })*/
                                        }
                                    } else {
                                        dismissProgressDialogWithAlert(getString(R.string.error_something_wrong))
                                    }
                                }
                            }
                        }
                    },
                    onFailure = { errorMessage ->
                        Log.e("Valor: ", errorMessage)
//                        ProgressUtils.dismissProgressDialog()

                        dismissProgressDialogWithAlert(errorMessage)
                        EventBus.getDefault()
                            .post(
                                MessageEvent(
                                    "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew makeValorPaymentRequest()-> ${
                                        Gson().toJson(
                                            errorMessage
                                        )
                                    } "
                                )
                            )
                        if (dashboardViewModel.customerGivenTipBefore.value == false) {
                            dashboardViewModel.paymentInProgress.value = false
                            dashboardViewModel.tipBeforeEnabled = true
                            dashboardViewModel.removeMainCart.value = true
                        }
                    }
                )
            }
        }
    }

    private fun dismissProgressDialogWithAlert(errorMessage: String? = null) {
        runOnUiThread {
            dashboardViewModel.paymentInProgress.value = false
            dashboardViewModel.tipBeforeEnabled = true
            dashboardViewModel.removeMainCart.value = true
            ProgressUtils.dismissProgressDialog()
            dismissProgressDialog()
            if (errorMessage != null) {
                AlertUtils.showCustomAlert(requireContext(), errorMessage)
            }
        }
    }


    // To make card payment using PAX device
    private fun makePaxPaymentRequest() {
        runBlocking {
//            initPOSLink()
            GlobalScope.launch {
                posLink.SetCommSetting(
                    SettingINI.getCommSettingFromFile(
                        requireContext(),
                        "/storage/emulated/0/Download/" + SettingINI.FILENAME
                    )
                )

                val amt = ((paymentAmount - tipAmountToPaymentDevice) * 100).roundToInt()
                val tip_amt = (tipAmountToPaymentDevice * 100).roundToInt()
                ECRRefNumber = System.currentTimeMillis().toString()
                Log.d("Amt: ", "amt $amt tip $tip_amt")
                var broadPOS_version = prefProvider.getValue(
                    Constants.BROADPOS_VERSION,
                    ""
                )

                /*  runOnUiThread(object:java.lang.Runnable{
                      override fun run() {
                          ProgressUtils.showProgressDialog(requireActivity())
                      }
                  })*/

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
                posLink.PaymentRequest = mPaymentRequest
                val result = posLink.ProcessTrans()
                Log.d("PAX_LOADER:: ", result.Code.toString() + " " + result.Msg)
                if (result.Code === ProcessTransResult.ProcessTransResultCode.OK) {
                    Log.d("PAX_LOADER:: ", "2948")
                    val msg = Message()
                    msg.what = Constants.TRANSACTION_SUCCESSED
                    msg.obj = posLink.PaymentResponse
                    msg.obj?.let {
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
                        PAXtoken = response.PaymentTransInfo.Token
                        Log.d("PAX_CARD:", "pax card info > ${response.CardInfo.ProgramType}")
                        Log.d("token:", "token $PAXtoken")
                        Log.d(
                            "Payment Details: ",
                            "$ExtData $resultCode $resultTxt $GlobalUID $RefNumber"
                        )
                        Log.d(
                            "Payment Details: ",
                            "$cardLastDigits $approvedAmount $CARDBIN $EDCType $tipAmount ${
                                Gson().toJson(response)
                            }"
                        )

                        if (resultCode == "000000") {
                            Log.d("PAX_LOADER:: ", "2984")
                            Log.v("PAX_LOADER_5:: ", result.Code.toString() + " " + result.Msg)
                            // Store pax payment data to database
                            val paxData = PAXData(
                                response.PaymentTransInfo.GlobalUid,
                                response.ExtData,
                                response.RefNum,
                                ECRRefNumber,
                                response.PaymentTransInfo.Token,
                                response.BogusAccountNum,
                                response.CardType
                            )
                            paymentviewModel.savePaxPaymentDataLocally(paxData)

                            CoroutineScope(Dispatchers.Main).launch {
//                        ProgressUtils.dismissProgressDialog()
                                coroutineScope {
                                    if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == GIFT_CARD) {
                                        if (prefProvider.getValueboolean(
                                                Constants.IS_ADD_VALUE_IN_GIFT_CARD,
                                                false
                                            )
                                        ) {
                                            giftCardViewModel.paxResponse = response.ExtData
                                            giftCardViewModel.cardNumberLast4 = response.BogusAccountNum
                                            giftCardViewModel.cardNamePax = response.CardType
                                            giftCardViewModel.transactionID =
                                                response.PaymentTransInfo.Token
                                            addValueInGiftCardUsingCard(paymentAmount)
                                        } else {
                                            giftCardViewModel.paxResponse = response.ExtData
                                            giftCardViewModel.cardNumberLast4 = response.BogusAccountNum
                                            giftCardViewModel.cardNamePax = response.CardType
                                            giftCardViewModel.transactionID =
                                                response.PaymentTransInfo.Token
                                            sellGiftCardUsingCard(paymentAmount)
                                        }
                                    } else {
                                        makePaymentCreditCard()
                                    }
//                            makePaymentCreditCard()
                                }
                            }
                        } else {
                            initPOSLink()
                            runOnUiThread(Runnable {
                                if (dashboardViewModel.customerGivenTipBefore.value == false) {
                                    dashboardViewModel.paymentInProgress.value = false
                                    dashboardViewModel.tipBeforeEnabled = true
                                    dashboardViewModel.removeMainCart.value = true
                                }
                                dismissProgressDialog()
                            })
                            CoroutineScope(Dispatchers.Main).launch {
                                Log.d("PAX_LOADER_1:: ", "result.Code.toString() result.Msg")

                                AlertUtils.showCustomAlertWithListenerWithOK(
                                    requireContext(),
                                    resultTxt,
                                    object : DialogInterface.OnClickListener {
                                        override fun onClick(p0: DialogInterface?, p1: Int) {
                                            try {
                                                dismissProgressDialog()
                                                p0?.dismiss()
                                            } catch (e: Exception) {
                                            }
                                        }
                                    })

//                        requireContext().showNormalToast("$resultCode $resultTxt")
//                        connectBP()
                            }
                        }
                    }
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        if (dashboardViewModel.customerGivenTipBefore.value == false) {
                            dashboardViewModel.paymentInProgress.value = false
                            dashboardViewModel.tipBeforeEnabled = true
                            dashboardViewModel.removeMainCart.value = true
                        }
                        ProgressUtils.dismissProgressDialog()
                        /*                    if (retryCount <= 1) {
                                                retryCount++
                                                magtekProViewModel.initPOSLink(requireContext())
                                            } else {
                                                retryCount = 1*/
                        dismissProgressDialog()
/*                    if (retryCount <= 1) {
                        retryCount++
                        magtekProViewModel.initPOSLink(requireContext())
                    } else {
                        retryCount = 1*/
                        Log.d("PAX_LOADER_2:: ", "result.Code.toString() result.Msg")
                        AlertUtils.showCustomAlertWithListenerWithOKCancel(
                            requireContext(),
                            getString(R.string.pax_connect_error), getString(R.string.reconnect),
                        )
                        { _, _ ->
                            // Add connect to PAX logic
                            Log.d("PAX_LOADER_3:: ", "result.Code.toString() result.Msg")
                            magtekProViewModel.initPOSLink(requireContext())
                        }
//                    }
//
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

    }

    private fun getMerchantDataObserver() {
        magtekProViewModel.merchantData.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { response ->
                Log.d("merchantData: ", "merchantData observe")
                val resultCode = response.resultCode
                val status = response.resultTxt
                val mID = response.VarValue
                prefProvider.setValueboolean(Constants.IS_PAX_CONNECTED, true)
                prefProvider.setValue(
                    Constants.MERCHANT_ID,
                    mID
                )
                runOnUiThread(Runnable {
                    dismissProgressDialog()
                })
                CoroutineScope(Dispatchers.Main).launch {
                    makePaxPaymentRequest()
//                    AlertUtils.showCustomAlert(requireContext(), "Merchant $mID is connected successfully")
                }
                Log.d("Merchant Details: ", mID + " " + resultCode + "  " + status)
            }
        }
    }

    private fun connectBP() {
        BroadPOSCommunicator.getInstance(activity)
            .startListeningService(object : BroadPOSCommunicator.StartListenerCallBack {
                override fun onSuccess() {
                    Toast.makeText(context, "Successful StartListenerCallBack", Toast.LENGTH_SHORT)
                        .show()
                }

                override fun onFail(msg: String) {
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            })
    }

    // Offers card payment by manually adding cars details
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

    // To get payment related data srtored in preference
    fun getDataFromPref() {
        redeemLoyaltyInfo = dashboardViewModel.redeemLoyaltyInfo
        if (prefProvider.getValue(Constants.WHOLE_AMOUNT, "")
                .isEmpty() || prefProvider.getValue(
                Constants.WHOLE_AMOUNT,
                ""
            ) == "0.0"
        ) {
            Log.e("AmtviewModeltotalPrice", "totalPrice  ${dashboardViewModel.totalPrice}")
            dashboardViewModel.totalServiceCharge =
                String.format("%.2f", dashboardViewModel.totalServiceCharge).toDouble()

            EventBus.getDefault().post(
                MessageEvent(
                    "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_getDataFromPref()_before_new_calc dashboardViewModel.subTotalPrice-> ${
                        Gson().toJson(dashboardViewModel.subTotalPrice)
                    }"
                )
            )
            /*

                        var subTotal = 0.0
                        var loyaltyAmt = 0.0
                        dashboardViewModel.currentCartItems.forEach {
                            subTotal += it.price * it.itemQuantity
                            if (it.modifiers.isNotEmpty()) {
                                it.modifiers.forEach { modifier ->
                                    subTotal += (modifier.price * modifier.modifier_quantity) * it.itemQuantity
                                }
                            }
                        }
                        dashboardViewModel.redeemLoyaltyInfo?.let { loyalty ->
                            if(loyalty.isLoyaltyApplied == true) loyaltyAmt  = loyalty.usedLoyaltyAmount
                        }
                        dashboardViewModel.subTotalPrice = subTotal - dashboardViewModel.totalDiscount - loyaltyAmt
            */

            EventBus.getDefault().post(
                MessageEvent(
                    "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_getDataFromPref()_after_new_calc dashboardViewModel.subTotalPrice-> ${
                        Gson().toJson(dashboardViewModel.subTotalPrice)
                    }"
                )
            )

            EventBus.getDefault().post(
                MessageEvent(
                    "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_getDataFromPref() dashboardViewModel.totalTax-> ${
                        Gson().toJson(dashboardViewModel.totalTax)
                    }"
                )
            )

            EventBus.getDefault().post(
                MessageEvent(
                    "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_getDataFromPref() dashboardViewModel.totalServiceCharge-> ${
                        Gson().toJson(dashboardViewModel.totalServiceCharge)
                    }"
                )
            )

            WholetotalPrice =
                MethodUtils.getTwoDecimal(dashboardViewModel.subTotalPrice).toPrecision(2)
                    .toDouble() + dashboardViewModel.totalTax + String.format(
                    "%.2f",
                    dashboardViewModel.totalServiceCharge
                ).toDouble()


            if (redeemLoyaltyInfo?.needToApplyLoyalty == true) {
                WholetotalPrice -= redeemLoyaltyInfo?.usedLoyaltyAmount!!
                dashboardViewModel.totalPrice = WholetotalPrice
            } else {
                dashboardViewModel.totalPrice = WholetotalPrice
            }
            Log.e("checkWhole", "WholetotalPrice:  ${WholetotalPrice}")
            Log.e("checkWhole", "subTotalPrice:  ${dashboardViewModel.subTotalPrice}")
            Log.e("checkWhole", "totalServiceCharge:  ${dashboardViewModel.totalServiceCharge}")
            Log.e("checkWhole", "totalTax:  ${dashboardViewModel.totalTax}")
            Log.e("checkWhole", "totalDiscount:  ${dashboardViewModel.totalDiscount}")
            WholetotalPrice = String.format("%.2f", WholetotalPrice).toDouble()
            Log.e("checkWholePrice", "WholetotalPrice:  ${WholetotalPrice}")

            prefProvider.setValue(
                Constants.WHOLE_AMOUNT,
                String.format("%.2f", getTwoDecimal(dashboardViewModel.totalPrice))
            )
        } else {
            WholetotalPrice = prefProvider.getValue(Constants.WHOLE_AMOUNT, "").toDouble()

            EventBus.getDefault().post(
                MessageEvent(
                    "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_getDataFromPref()_WHOLE_AMOUNT in Preference dashboardViewModel.subTotalPrice-> ${
                        Gson().toJson(WholetotalPrice)
                    }"
                )
            )

            dashboardViewModel.totalPrice = WholetotalPrice
        }

        WholetotalPrice = getTwoDecimal(WholetotalPrice)
        if (prefProvider.getValue(Constants.SUB_TOTAL, "").isEmpty() || prefProvider.getValue(
                Constants.SUB_TOTAL,
                ""
            ) == "0.0"
        ) {
            subTotalPrice = dashboardViewModel.subTotalPrice
            EventBus.getDefault().post(
                MessageEvent(
                    "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_getDataFromPref()_1 dashboardViewModel.subTotalPrice-> ${
                        Gson().toJson(dashboardViewModel.subTotalPrice)
                    }", true
                )
            )
            prefProvider.setValue(
                Constants.SUB_TOTAL,
                String.format("%.2f", dashboardViewModel.subTotalPrice)
            )
        } else {
            subTotalPrice = prefProvider.getValue(Constants.SUB_TOTAL, "").toDouble()
            EventBus.getDefault().post(
                MessageEvent(
                    "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_getDataFromPref()_2 dashboardViewModel.subTotalPrice-> ${
                        Gson().toJson(dashboardViewModel.subTotalPrice)
                    }", true
                )
            )
        }

        if (prefProvider.getValue(Constants.TAX_CHARGE, "").isEmpty() || prefProvider.getValue(
                Constants.TAX_CHARGE,
                ""
            ) == "0.0"
        ) {
            totalTax = dashboardViewModel.totalTax
            prefProvider.setValue(
                Constants.TAX_CHARGE,
                String.format("%.2f", dashboardViewModel.totalTax)
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
            totalServiceCharge = dashboardViewModel.totalServiceCharge
            prefProvider.setValue(
                Constants.SERVICE_CHARGE,
                String.format("%.2f", dashboardViewModel.totalServiceCharge)
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
            totalDiscount = dashboardViewModel.totalDiscount
            prefProvider.setValue(
                Constants.TOTAL_DISCOUNT,
                String.format("%.2f", dashboardViewModel.totalDiscount)
            )
        } else {
            totalDiscount = prefProvider.getValue(Constants.TOTAL_DISCOUNT, "").toDouble()
        }


        if (prefProvider.getValue(Constants.TIP, "").isEmpty() || prefProvider.getValue(
                Constants.TIP,
                ""
            ) == "0.0"
        ) {
            tipAmount = dashboardViewModel.tip
            prefProvider.setValue(Constants.TIP, String.format("%.2f", dashboardViewModel.tip))
        } else {
            tipAmount = prefProvider.getValue(Constants.TIP, "").toDouble()
        }

        if (prefProvider.getValue(Constants.TIP, "").isEmpty() || prefProvider.getValue(
                Constants.TIP,
                ""
            ) == "0.0"
        ) {
            tipAmount = dashboardViewModel.tip
            prefProvider.setValue(Constants.TIP, String.format("%.2f", dashboardViewModel.tip))
        } else {
            tipAmount = prefProvider.getValue(Constants.TIP, "").toDouble()
        }

        cashDiscountSurcharge = if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == GIFT_CARD) {
            0.0
        } else {
            MethodUtils.getLatestCashDiscountOrSurCharge(
                WholetotalPrice / isSelectedCount,
                prefProvider,
                requireContext()
            )
        }

        prefProvider.setValue(
            Constants.CASH_DISCOUNT_SURCHARGE,
            String.format("%.2f", cashDiscountSurcharge)
        )
        cashDiscountType = dashboardViewModel.cashDiscountType




        cartList = dashboardViewModel.cartModel

        //Added (&& condition to check name) by Dharmesh to resolve issue BIS-352
        dashboardViewModel.ordertypelist.forEach {
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
            dashboardViewModel.totalPrice,
            dashboardViewModel.subTotalPrice,
            dashboardViewModel.totalTax,
            dashboardViewModel.totalServiceCharge,
            dashboardViewModel.tip,
            dashboardViewModel.totalDiscount,
            dashboardViewModel.cashdiscountAmount,
            dashboardViewModel.totalPrice
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

    // To set different cash payment options and total amount values
    private fun setupPaymentScreen(isSelectCount: Int,cashTip:Double = 0.0,cardTip:Double=0.0) {
        MethodUtils.getCashPaymentOptionList(
//   Commented to solve BIS-4196         getCalCashDiscWithAmount(WholetotalPrice, true) / isSelectCount,
            (getCalCashDiscWithAmount(WholetotalPrice, true) / isSelectCount) + cashTip,
            binding.tvCash1,
            binding.tvCash2,
            binding.tvCash3
        )

        MethodUtils.setPriceTextView(
            binding.tvCash,
            (getCalCashDiscWithAmount(WholetotalPrice, true) / isSelectCount) + cashTip
//     Commented to solve BIS-4196       getCalCashDiscWithAmount(WholetotalPrice, true) / isSelectCount
        )
        MethodUtils.setPriceTextView(
            binding.tvCash0,
            (getCalCashDiscWithAmount(WholetotalPrice, true) / isSelectCount) + cashTip
            //     Commented to solve BIS-4196  getCalCashDiscWithAmount(WholetotalPrice, true) / isSelectCount
        )
        Log.e(TAG, "WholetotalPrice:   ${WholetotalPrice}")
        MethodUtils.setPriceTextView(
            binding.tvCard,
            (getCalCashDiscWithAmount(WholetotalPrice, false) / isSelectCount) + cardTip
            //     Commented to solve BIS-4196  getCalCashDiscWithAmount(WholetotalPrice, false) / isSelectCount
        )
        if (this::presentation.isInitialized) {
            presentation.show()
            /*presentation.updateTotals(
                binding.tvCash.text.toString(),
                binding.tvCard.text.toString()
            )*/
            presentation.updateTotals(
                String.format(
                    "%.3f",
                    (dashboardViewModel.subTotalPrice + dashboardViewModel.totalTax + dashboardViewModel.totalServiceCharge)
                ).toDouble().toString(),
                String.format(
                    "%.3f",
                    (dashboardViewModel.subTotalPrice + dashboardViewModel.totalTax + dashboardViewModel.totalServiceCharge + dashboardViewModel.cashdiscountAmount)
                ).toDouble().toString()
            )
        }

        dashboardViewModel.customerCashAmount.value = binding.tvCash.text.toString()
        dashboardViewModel.customerCardAmount.value = binding.tvCard.text.toString()

        binding.tvCash.text = "Cash (" + binding.tvCash.text + ")"
        binding.tvCard.text = "Card (" + binding.tvCard.text + ")"
    }

    // To calculate tip added by user
    private fun tipAmountCalculation(cashTip:Double = 0.0,cardTip:Double=0.0) {
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
                dashboardViewModel.customerCashAmount.value = binding.tvCash.text.toString()
                dashboardViewModel.customerCardAmount.value = binding.tvCard.text.toString()
                presentation.updateTotals(
                    binding.tvCash.text.toString(),
                    binding.tvCard.text.toString()
                )
            }
            binding.tvCash.text = "Cash (" + binding.tvCash.text + ")"
            binding.tvCard.text = "Card (" + binding.tvCard.text + ")"

            if (this::presentation.isInitialized) {
                presentation.onDisplayChanged()
            }

            MethodUtils.setPriceTextViewDown(
                binding.tvAmount,
                getCalCashDiscWithAmount(
                    prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble(), true
                )
            )
        } else {
            if (this::presentation.isInitialized) {
                presentation.show()
                presentation.showTipsAddedNew(cardTip, cashTip, WholetotalPrice)
            }

            MethodUtils.setPriceTextView(
                binding.tvCash,
                (getCalCashDiscWithAmount(WholetotalPrice, true) / isSelectedCount) + cashTip
            )
            MethodUtils.setPriceTextView(
                binding.tvCash0,
                (getCalCashDiscWithAmount(WholetotalPrice, true) / isSelectedCount) + cashTip
            )
            MethodUtils.setPriceTextView(
                binding.tvCard,
                (getCalCashDiscWithAmount(WholetotalPrice, false) / isSelectedCount) + cardTip
            )

            if (this::presentation.isInitialized) {
                presentation.show()
                presentation.showTipsAddedNew(cardTip, cashTip, WholetotalPrice)
                presentation.updateTotals(
                    binding.tvCash.text.toString(),
                    binding.tvCard.text.toString()
                )
            }

            binding.tvCash.text =
                "Cash (" + binding.tvCash.text + ")"
            binding.tvtipcash?.visible()
            binding.tvtipcash?.text =
                "(" + MethodUtils.roundOffAmount(cashTip) + " Tip Added)"
            binding.tvCard.text =
                "Card (" + binding.tvCard.text + ")"
            binding.tvtipcard?.visible()
            binding.tvtipcard?.text =
                "(" + MethodUtils.roundOffAmount(cardTip) + " Tip Added)"
            MethodUtils.getCashPaymentOptionList(
                (getCalCashDiscWithAmount(WholetotalPrice, true) / isSelectedCount) + cashTip,
                binding.tvCash1,
                binding.tvCash2,
                binding.tvCash3
            )
            MethodUtils.setPriceTextViewDown(
                binding.tvAmount,
                (getCalCashDiscWithAmount(
                    prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble(), true
                ) / isSelectedCount).toDouble() + cardTip
            )
            binding.tvAmount.text =
                binding.tvAmount.text.toString()
            binding.tvsplittip?.visible()
            binding.tvsplittip?.text =
                "(" + MethodUtils.roundOffAmount(tipAmount) + " Tip Added)"

            if (this::presentation.isInitialized) {
                presentation.onDisplayChanged()
            }

        }
    }

    // Split total amount as per user's split choice

    private fun splitAllAmounts(TAG: String, amount: Double) {

        val currentAmountString = prefProvider.getValue(TAG, "")

        // Check if the value is empty, and set a default value (0.0) if it is
        val currentAmount = if (currentAmountString.isNotEmpty()) {
            currentAmountString.toDouble()
        } else {
            0.0 // Default value if the string is empty
        }
        EventBus.getDefault().post(
            MessageEvent(
                "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_splitAllAmounts(...) prefProvider.getValue(TAG...= ${
                    currentAmount
                }, amount= ${amount}"
            )
        )
        // Calculate the remaining value
        val remainingValue = currentAmount - amount

        // Store the remaining value back in prefProvider
        prefProvider.setValue(TAG, String.format(Locale.ROOT, "%.2f", remainingValue))

        // Log the updated value
        Log.d(TAG, "splitAllAmounts: " + prefProvider.getValue(TAG, "").toDouble())
    }

    // Calculate service charge / cash discount on amount
    private fun getCalCashDiscWithAmount(totalprice: Double, isCash: Boolean): Double {
        return if (isCash) {
            if (cashDiscountType == "CashDiscount" && prefProvider.getValue(
                    ORDER_TYPE,
                    TAKEOUT
                ) != GIFT_CARD
            ) {
                if (totalprice - MethodUtils.getLatestCashDiscountOrSurCharge(
                        totalprice,
                        prefProvider,
                        requireContext()
                    ) < 0.0
                ) {
                    0.0
                } else {
                    totalprice - MethodUtils.getLatestCashDiscountOrSurCharge(
                        totalprice,
                        prefProvider,
                        requireContext()
                    )
                }
            } else {
                totalprice
            }
        } else {
//            if (cashDiscountType == "SurCharge" && prefProvider.getValue(
//                    ORDER_TYPE,
//                    TAKEOUT
//                ) != GIFT_CARD
//            )
            if (cashDiscountType == "SurCharge") {
                totalprice + MethodUtils.getLatestCashDiscountOrSurCharge(
                    totalprice,
                    prefProvider,
                    requireContext()
                )
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

    private fun adjustPreAuthPaymentPax() {

        //  paymentAmount = String.format("%.2f", WholetotalPrice / isSelectedCount).toDouble()

//        var paymentDetailsResponse = dashboardViewModel.authPaymentResponse?.payments?.first()
//
//        if(paymentDetailsResponse == null )
//            paymentDetailsResponse


        val paymentDetailsResponse = paymentviewModel.preAuthData

        Log.e("Print", Gson().toJson(paymentDetailsResponse))



        GlobalScope.launch {
            posLink.SetCommSetting(
                SettingINI.getCommSettingFromFile(
                    requireContext(),
                    Constants.FILE_PATH + SettingINI.FILENAME
                )
            )
            val finalAmount = (paymentAmount * 100).toInt()
            Log.d("Amt: ", "tip $finalAmount RefNo ${paymentDetailsResponse?.ecrRefNum}")

            CoroutineScope(Dispatchers.Main).launch {
                // ProgressUtils.showProgressDialog(requireActivity())
            }
            mPaymentRequest = PaymentRequest()
            mPaymentRequest.TransType = mPaymentRequest.ParseTransType("POSTAUTH")
            mPaymentRequest.TenderType = mPaymentRequest.ParseTenderType("CREDIT")
            mPaymentRequest.Amount = finalAmount.toString()
            //Added for TSYS ADJUST issue
            mPaymentRequest.ECRRefNum = paymentDetailsResponse?.refNum
            mPaymentRequest.OrigRefNum = paymentDetailsResponse?.ecrRefNum
            mPaymentRequest.OrigECRRefNum = paymentDetailsResponse?.ecrRefNum
            mPaymentRequest.ExtData = "<Force>T</Force>"

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

                cardLastDigits = response.BogusAccountNum
                EDCType = response.CardType
                CARDBIN = response.CardInfo.CardBin
                var tipAmount = response.ApprovedTipAmount
                val globalUID = response.PaymentTransInfo.GlobalUid

                Log.d(
                    "Payment Details: ",
                    "$ExtData $resultCode $resultTxt $globalUID"
                )
                Log.d(
                    "Payment Details: ",
                    "$cardLastDigits $approvedAmount $CARDBIN $EDCType $tipAmount ${
                        Gson().toJson(response)
                    }"
                )

                msg.what = Constants.TRANSACTION_SUCCESSED
                msg.obj = posLink.PaymentResponse

                ExtData = response.ExtData
                RefNumber = response.RefNum
                ECRRefNumber = paymentDetailsResponse?.ecrRefNum ?: ""

                cardLastDigits = response.BogusAccountNum
                EDCType = response.CardType
                CARDBIN = response.CardInfo.CardBin
                GlobalUID = response.PaymentTransInfo.GlobalUid
                paymentviewModel.setPAXData(RefNumber, GlobalUID)
//                prefProvider.setValue(Constants.GLOBAL_ID, globalUID!!)

                //implementation("org.dom4j:dom4j:2.1.3")
                PAXtoken = response.PaymentTransInfo.Token
                Log.d("PAX_CARD:", "pax card info > ${response.CardInfo.ProgramType}")
                Log.d("token:", "token $PAXtoken")
                Log.d(
                    "Payment Details: ",
                    "$ExtData $resultCode $resultTxt $GlobalUID $RefNumber"
                )
                Log.d(
                    "Payment Details: ",
                    "$cardLastDigits $approvedAmount $CARDBIN $EDCType $tipAmount ${
                        Gson().toJson(response)
                    }"
                )



                if (resultCode == "000000") {
//                    CoroutineScope(Dispatchers.Main).launch {
//                        ProgressUtils.dismissProgressDialog()
//                        coroutineScope {
////                            makePaymentCreditCard()
//                            makePaymentCreditCard()
//                        }
//                    }


                    Log.v("PAX_LOADER_5:: ", result.Code.toString() + " " + result.Msg)
                    // Store pax payment data to database
                    val paxData = PAXData(
                        response.PaymentTransInfo.GlobalUid,
                        response.ExtData,
                        response.RefNum,
                        ECRRefNumber,
                        response.PaymentTransInfo.Token,
                        response.BogusAccountNum,
                        response.CardType
                    )
                    paymentviewModel.savePaxPaymentDataLocally(paxData)

                    CoroutineScope(Dispatchers.Main).launch {
//                        ProgressUtils.dismissProgressDialog()
                        coroutineScope {
                            if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == GIFT_CARD) {
                                if (prefProvider.getValueboolean(
                                        Constants.IS_ADD_VALUE_IN_GIFT_CARD,
                                        false
                                    )
                                ) {
                                    giftCardViewModel.paxResponse = response.ExtData
                                    giftCardViewModel.cardNumberLast4 = response.BogusAccountNum
                                    giftCardViewModel.cardNamePax = response.CardType
                                    giftCardViewModel.transactionID =
                                        response.PaymentTransInfo.Token
                                    addValueInGiftCardUsingCard()
                                } else {
                                    giftCardViewModel.paxResponse = response.ExtData
                                    giftCardViewModel.cardNumberLast4 = response.BogusAccountNum
                                    giftCardViewModel.cardNamePax = response.CardType
                                    giftCardViewModel.transactionID =
                                        response.PaymentTransInfo.Token
                                    sellGiftCardUsingCard()
                                }
                            } else {
                                paymentviewModel.clearPreAuthDetails()
                                makePaymentCreditCard()
                            }
//                            makePaymentCreditCard()
                        }
                    }
                } else {

                    runOnUiThread(object : java.lang.Runnable {
                        override fun run() {
                            dismissProgressDialog()
                            binding.llSavedCard.gone()
                        }
                    })

                    //clear PRE AUTH DATA
                    prefProvider.setValue(PRE_AUTH_DETAILS, "")
//                    dashboardViewModel.apply {
//                        paymentAttributes = null
//                        authPaymentResponse = null
//                        allOrderResponse = null
//                    }
                    paymentviewModel.clearPreAuthDetails()

                    CoroutineScope(Dispatchers.Main).launch {
                        ProgressUtils.dismissProgressDialog()
                        AlertUtils.showCustomAlertWithListenerWithOK(
                            requireContext(),
                            "Card Expired OR " + resultTxt,
                            object :
                                DialogInterface.OnClickListener {
                                override fun onClick(p0: DialogInterface?, p1: Int) {
                                    try {
                                        p0?.dismiss()
                                    } catch (e: Exception) {
                                    }
                                }
                            })
//                        requireActivity().toast("$resultCode $resultTxt", Toast.LENGTH_LONG)
                    }
                }
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    ProgressUtils.dismissProgressDialog()
                    dismissProgressDialog()

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


    private fun setupTabDesign() {

        // Checking Pre-Auth condition , if any pre auth card saved with this order
        try {
//                if (dashboardViewModel.authPaymentResponse!!.payments.isNotEmpty())
//                    binding.llSavedCard.visibility = View.VISIBLE
//                else if(dashboardViewModel.paymentAttributes != null)
//                        binding.llSavedCard.visibility = View.VISIBLE
//                    else binding.llSavedCard.visibility = View.GONE
//

            try {
                val preAuthData = paymentviewModel.preAuthData

                if (preAuthData!!.ecrRefNum.isNotEmpty() && preAuthData.refNum.isNotEmpty() ) {
                    binding.llSavedCard.visibility = View.VISIBLE
                } else {
                    binding.llSavedCard.visibility = View.GONE
                }
            } catch (e: Exception) {
                binding.llSavedCard.visibility = View.GONE
            }


        } catch (e: Exception) {
            binding.llSavedCard.visibility = View.GONE
        }

        if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == GIFT_CARD) {
            PaymentBoldPosFragment.newInstance().addTipHideShow(true)
            binding.linearTab2.gone()
        } else {
            PaymentBoldPosFragment.newInstance().addTipHideShow(false)
            binding.linearTab2.visible()
        }

        binding.linearTab1.setOnSingleClickListener {
            if (!isPaymentScreen) {
                if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) != GIFT_CARD) {
                    PaymentBoldPosFragment.newInstance().addTipHideShow(false)
                }
                /*Solved BIS-4479*/
                dashboardViewModel.isSelectCount = 1
                /*Solved BIS-4479*/

                isSelectedCount = 1
                tipsetupGlobal(tipAmount, isSelectedCount)
                loadPaymentLayout()
                tipAmountCalculation()
            } else {/*
                tipAmount = 0.0
                dashboardViewModel.setTipAmount(0.0)
                dashboardViewModel.totalTipAmount = 0.0
                dashboardViewModel.customerGivenTip.value = false
                prefProvider.setValueboolean(Constants.TIP_ADDED, false)
                prefProvider.setValue(Constants.WHOLE_AMOUNT, "")
                if (this::presentation.isInitialized) {
                    presentation.show()
                    presentation.showTipsAddedNew(tipAmount, tipAmount, WholetotalPrice)
//                        presentation.updateTotals(
//                            binding.tvCash.text.toString(),
//                            binding.tvCard.text.toString()
//                        )
                }

                binding.tvCustom.text = "Custom"
                isSelectedCount = 1
                binding.tvsplittip?.gone()
                binding.tvtipcard?.gone()
                binding.tvtipcash?.gone()
//                tipsetupGlobal(tipAmount, isSelectedCount)
                binding.tvFullAMounttxt.visibility = View.VISIBLE
                binding.tvwaysplit?.invisible()
                isSelectedCount = 1
//                tipsetupGlobal(tipAmount, isSelectedCount)
                binding.tvFullAMounttxt.visibility = View.VISIBLE
                getDataFromPref()
*/
            }
        }
        binding.linearTab2.setOnSingleClickListener {

            if (tipAmount != 0.0 && dashboardViewModel.tipTransactionAmount != 0.0) {
                AlertUtils.showCustomAlertWithListenerWithOKCancel(
                    requireContext(),
                    "If you are going to do split payment then existing tip will be removed."
                ) { _, _ ->
                    PaymentBoldPosFragment.newInstance().addTipHideShow(true)
                    tipAmount = 0.0
                    dashboardViewModel.setTipAmount(0.0)
                    dashboardViewModel.totalTipAmount = 0.0
                    dashboardViewModel.customerGivenTip.value = false
                    prefProvider.setValueboolean(Constants.TIP_ADDED, false)
                    dashboardViewModel.employeeGivenTip = false

                    if (this::presentation.isInitialized) {
                        presentation.show()
                        presentation.showTipsAddedNew(tipAmount, tipAmount, WholetotalPrice)
                        presentation.shouldHighlightNoTip()
//                        presentation.updateTotals(
//                            binding.tvCash.text.toString(),
//                            binding.tvCard.text.toString()
//                        )
                    }
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
                PaymentBoldPosFragment.newInstance().addTipHideShow(true)
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

    private fun loadPaymentLayout(cashTip:Double = 0.0,cardTip:Double=0.0) {
        setupPaymentScreen(isSelectedCount,cashTip,cardTip)
        binding.tab1.setTextColor(resources.getColor(R.color.txt_color_blue))
        binding.view1.setBackgroundColor(resources.getColor(R.color.txt_color_blue))
        binding.tab2.setTextColor(resources.getColor(R.color.white))
        binding.view2.setBackgroundColor(resources.getColor(R.color.backgroundColor))
        isPaymentScreen = true
        isSplitScreen = false
        binding.paymentLinearLayout.visibility = View.VISIBLE
        binding.splitLinearLayout.visibility = View.GONE
        cashDiscountSurcharge = if (prefProvider.getValue(ORDER_TYPE, TAKEOUT) == GIFT_CARD) {
            0.0
        } else {
            MethodUtils.getLatestCashDiscountOrSurCharge(
                WholetotalPrice / isSelectedCount,
                prefProvider,
                requireContext()
            )

        }


        EventBus.getDefault().post(
            MessageEvent(
                "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_loadPaymentLayout() paymentAmount-> WholetotalPrice -> ${
                    Gson().toJson(
                        WholetotalPrice
                    )
                }", true
            )
        )

    }

    private fun makePaymentCreditCardValor(txnid: String?, transactionNumber: String?) {
        paymentviewModel.valorRefTxnId = txnid
        paymentviewModel.valorTransactionNumber = transactionNumber
        paymentAmount -= tipAmount
        paymentAmount = MethodUtils.roundOffAmountDouble(paymentAmount)
        paymentType = "Card"
        Log.d(
            "LOADER::",
            "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
        )

//        LogUtil.logE(TAG, "cartList:  ${Gson().toJson(cartList)}")
//        LogUtil.logE(TAG, "cartListcartItems:  ${Gson().toJson(cartItems)}")
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
        var cartModel: CartModel? = null
        try {
            cartModel =
                Gson().fromJson<CartModel?>(
                    prefProvider.getValue("CART_MODEL1", ""),
                    CartModel::class.java
                )
        } catch (e: Exception) {

        }
        var cartModel2: CartModel? = null
        try {
            cartModel2 = Gson().fromJson<CartModel?>(
                prefProvider.getValue("CART_MODEL2", ""),
                CartModel::class.java
            )
        } catch (e: Exception) {

        }

        if (cartList == null) {
            Log.d(
                "LOADER::",
                "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
            )

            CoroutineScope(Dispatchers.Main).launch {
                getCartModelsList()
            }

            if (cartModel != null) {
                dashboardViewModel.cartModel = cartModel
                cartList = cartModel
            } else if (cartModel2 != null) {
                dashboardViewModel.cartModel = cartModel2
                cartList = cartModel2
            } else if (dashboardViewModel.cartModel == null) {
                runBlocking {
                    try {
                        var model =
                            CoroutineScope(Dispatchers.IO).async { dashboardViewModel.getCartModelBackup() }
                                .await().last().data

                        EventBus.getDefault().post(
                            MessageEvent(
                                "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_CART_BACKUP_MODEL -> model -> ${
                                    Gson().toJson(model)
                                }", true
                            )
                        )
                        cartList = Gson().fromJson(model, CartModel::class.java)
                        dashboardViewModel.cartModel =
                            Gson().fromJson(model, CartModel::class.java)
                        Log.d(
                            "LOADER::",
                            "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
                        )

                    } catch (e: Exception) {
                        var models =
                            CoroutineScope(Dispatchers.IO).async { dashboardViewModel.getAllCartModels() }
                                .await()
                        if (models.isNotEmpty()) {
                            dashboardViewModel.cartModel = models.last()
                            cartList = models.last()
                            Log.d(
                                "LOADER::",
                                "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
                            )

                        } else {
                            Log.d(
                                "LOADER::",
                                "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
                            )

                            /*Continuous loading shall occur due to the cartModel null */

                        }
                    }
                }
            }
        }

        Log.d(
            "LOADER::",
            "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
        )

        oldItems = prefProvider.getValue(Constants.OLD_ITEM_BASE_CUSTOM_ITEM, "")
        val listType = object : TypeToken<java.util.ArrayList<TbCartItem>>() {}.type
        lateinit var oldCartItemsList: ArrayList<TbCartItem>

        if (oldItems.isNotEmpty()) {

            val oldCartItemsJson = prefProvider.getValue(Constants.OLD_ITEM_BASE_CUSTOM_ITEM, "")

            oldCartItemsList = Gson().fromJson(oldCartItemsJson, listType) as ArrayList<TbCartItem>
        } else {
            prefProvider.setValueboolean(DO_PRINT, true)
        }

        EventBus.getDefault().post(
            MessageEvent(
                "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_makePaymentCreditCard() -> cartList -> ${
                    Gson().toJson(cartList)
                }", true
            )
        )
        if (cartList == null || cartList?.items == null || cartList?.items?.isEmpty() == true) {
            Log.d(
                "LOADER::",
                "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
            )

            EventBus.getDefault().post(
                MessageEvent(
                    "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_makePaymentCreditCard() -> null Case"
                )
            )

            var items: ArrayList<TbItem>? = ArrayList()
            /*  for (item in dashboardViewModel.currentCartItems) {*/
            for (item in dashboardViewModel.currentCartItems) {
                var tbItem = TbItem()
                tbItem.apply {

                    isItemEdited = item.isItemEdited

                    itemQuantity = item.itemQuantity
                    id = item.id
                    cartItemId = item.cartItemId
                    itemId = item.itemId
                    categoryId = item.categoryId
                    categoryName = item.categoryName
                    createdAt = item.createdAt
                    customItemCount = item.customItemCount
                    dineInSort = item.dineInSort
                    customItemID = item.customItemCount
                    discountId = item.discountId
                    discountPrice = item.discountPrice
                    discountType = item.discountType
                    guestItemId = item.guestItemId
                    headerPositionDinein = item.headerPositionDinein
                    hide_status = item.hide_status
                    isHide = item.isHide
                    imageUrl = item.imageUrl
                    isChecked = item.isChecked
                    isDeleted = item.isDeleted
                    isDestroy = item.isDestroy
                    isEdited = item.isEdited
                    isFired = item.isFired
                    isManualSales = item.isManualSales
                    isPaid = item.isPaid
                    itemOriginalModifiersList = item.itemOriginalModifiersList
                    modifier_set_ids = item.modifier_set_ids
                    modifiers = item.modifiers
                    name = item.name
                    note = item.note
                    manualSaleId = item.manualSaleId
                    optionSets = item.optionSets
                    website_hide_status = item.website_hide_status
                    variationsAttributes = item.variationsAttributes
                    updatedAt = item.updatedAt
                    timeStamp = item.timeStamp
                    thumbImageUrl = item.thumbImageUrl
                    taxes = item.taxes
                    sort = item.sort
                    sku = item.sku
                    singleItemPrice = item.singleItemPrice
                    shortDescription = item.shortDescription
                    reorder = item.reorder
                    quantity = item.quantity
                    price = item.price
                    orderItemId = item.orderItemId

                    try {
                        if (oldCartItemsList.isNotEmpty()) { // Order is updated

                            prefProvider.setValueboolean(Constants.DO_PRINT, true)

                            oldCartItemsList.forEach { oldItem ->
                                if ((oldItem.cartItemId == item.cartItemId) &&
                                    (oldItem.categoryId == item.categoryId) &&
                                    (oldItem.employeeID == item.employeeID) &&
                                    (oldItem.itemId == item.itemId) &&
                                    (oldItem.name.equals(item.name))
                                ) {

                                    if (item.itemQuantity != oldItem.itemQuantity) {
                                        isItemEdited = true
                                    }

                                }
                            }
                        }
                    } catch (e: Exception) {
                        //order is not updated , its new order
                    }
                }

                items!!.add(tbItem)

            }

            if (oldItems.isNotEmpty()) {

                for (item in dashboardViewModel.currentCartItems) {
                    try {
                        if (oldItems.isNotEmpty()) {
                            /*Added by Rahul to solve the modifiers not removing issue*/

                            val notPresentItems =
                                oldCartItemsList.filter { it.cartItemId != item.cartItemId }

                            if (true) {
                                var isFound = false

                                for (notPresentData in oldCartItemsList) {
                                    if (items != null) {
                                        for (it in items) {
                                            if (it.cartItemId == notPresentData.cartItemId) {
                                                isFound = true
                                                break
                                            } else isFound = false
                                        }
                                    }
                                    if (!isFound) {

                                        prefProvider.setValueboolean(DO_PRINT, true)
//                                        Not found
                                        isFound = false

                                        var tbItemDeleted = TbItem()
                                        tbItemDeleted.id = notPresentData.id
                                        tbItemDeleted.cartItemId = notPresentData.cartItemId
                                        tbItemDeleted.itemId = notPresentData.itemId
                                        tbItemDeleted.itemQuantity = notPresentData.itemQuantity
                                        tbItemDeleted.categoryId = notPresentData.categoryId
                                        tbItemDeleted.categoryName = notPresentData.categoryName
                                        tbItemDeleted.createdAt = notPresentData.createdAt
                                        tbItemDeleted.customItemCount =
                                            notPresentData.customItemCount
                                        tbItemDeleted.dineInSort = notPresentData.dineInSort
                                        tbItemDeleted.customItemID = notPresentData.customItemCount
                                        tbItemDeleted.discountId = notPresentData.discountId
                                        tbItemDeleted.discountPrice = notPresentData.discountPrice
                                        tbItemDeleted.discountType = notPresentData.discountType
                                        tbItemDeleted.guestItemId = notPresentData.guestItemId
                                        tbItemDeleted.headerPositionDinein =
                                            notPresentData.headerPositionDinein
                                        tbItemDeleted.hide_status = notPresentData.hide_status
                                        tbItemDeleted.isHide = notPresentData.isHide
                                        tbItemDeleted.imageUrl = notPresentData.imageUrl
                                        tbItemDeleted.isChecked = notPresentData.isChecked
                                        tbItemDeleted.isDeleted = notPresentData.isDeleted
                                        tbItemDeleted.isDestroy = true
                                        tbItemDeleted.isEdited = notPresentData.isEdited
                                        tbItemDeleted.isFired = notPresentData.isFired
                                        tbItemDeleted.isManualSales = notPresentData.isManualSales
                                        tbItemDeleted.isPaid = notPresentData.isPaid
                                        tbItemDeleted.itemOriginalModifiersList =
                                            notPresentData.itemOriginalModifiersList
                                        tbItemDeleted.quantity = notPresentData.quantity
                                        tbItemDeleted.modifier_set_ids =
                                            notPresentData.modifier_set_ids
                                        tbItemDeleted.modifiers = notPresentData.modifiers
                                        tbItemDeleted.name = notPresentData.name
                                        tbItemDeleted.note = notPresentData.note
                                        tbItemDeleted.manualSaleId = notPresentData.manualSaleId
                                        tbItemDeleted.optionSets = notPresentData.optionSets
                                        tbItemDeleted.website_hide_status =
                                            notPresentData.website_hide_status
                                        tbItemDeleted.variationsAttributes =
                                            notPresentData.variationsAttributes
                                        tbItemDeleted.updatedAt = notPresentData.updatedAt
                                        tbItemDeleted.timeStamp = notPresentData.timeStamp
                                        tbItemDeleted.thumbImageUrl = notPresentData.thumbImageUrl
                                        tbItemDeleted.taxes = notPresentData.taxes
                                        tbItemDeleted.sort = notPresentData.sort
                                        tbItemDeleted.sku = notPresentData.sku
                                        tbItemDeleted.singleItemPrice =
                                            notPresentData.singleItemPrice
                                        tbItemDeleted.shortDescription =
                                            notPresentData.shortDescription
                                        tbItemDeleted.reorder = notPresentData.reorder
                                        tbItemDeleted.price = notPresentData.price
                                        tbItemDeleted.orderItemId = notPresentData.orderItemId

                                        items!!.add(tbItemDeleted)
                                        break
                                    }

                                }

                            }
//                        for (notPresentItem in notPresentItems) {
//                            if (item.itemId != notPresentItem.itemId) {


//                            }
//                        }
                            Log.d("UNCOMMON:::", Gson().toJson(notPresentItems))


                        }
                    } catch (e: Exception) {
                    }

                }

            }


            /*Adding the deleted items*/
            cartList?.items = items
        }

        Log.d(
            "LOADER::",
            "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
        )

        prefProvider.setValue(Constants.OLD_ITEM, "")
        prefProvider.setValue(Constants.OLD_ITEM_BASE, "")
        prefProvider.setValue(Constants.OLD_ITEM_BASE_CUSTOM_ITEM, "")


        val myRequest = cartList?.let {
            txnid?.let {
                if (RefNumber.isEmpty()) {
                    RefNumber = it
                    /*We are adding VALOR in EXT DATA because, this extData variable goes empty, now we can utilize the variable for checking the payment gateway*/
                    ExtData = Constants.VALOR
                }
            }

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
        LogUtil.logE(TAG, "myRequestOriginal ${Gson().toJson(myRequest)}")
        if (myRequest != null) {
            paymentviewModel.totalPayAmount(paymentAmount)

            if (prefProvider.getValue(Constants.GIFT_CARD_TYPE, "")
                    .equals("digital", ignoreCase = true)
            ) {
                myRequest.order.orderTypeId = prefProvider.getValueInt(Constants.ORDER_TYPE_ID, -1)
            }

            EventBus.getDefault().post(
                MessageEvent(
                    "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_makePaymentCreditCard() , myRequest -> ${
                        Gson().toJson(myRequest)
                    } _1"
                )
            )
            Log.d(
                "LOADER::",
                "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
            )

            /* //FILE ASSERTION
             MainActivity.writeToFile(
                 Gson().toJson(myRequest),
                 "Pay_".plus(myRequest.order.offlineId.toString()),
                 activity?.filesDir,
                 activity!!
             )*/

            paymentAttributesRequest(myRequest)
        }
    }

    private fun makePaymentCreditCard() {
        paymentAmount -= tipAmount
        paymentAmount = MethodUtils.roundOffAmountDouble(paymentAmount)
        paymentType = "Card"
        Log.d(
            "LOADER::",
            "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
        )

//        LogUtil.logE(TAG, "cartList:  ${Gson().toJson(cartList)}")
//        LogUtil.logE(TAG, "cartListcartItems:  ${Gson().toJson(cartItems)}")
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
        var cartModel: CartModel? = null
        try {
            cartModel =
                Gson().fromJson<CartModel?>(
                    prefProvider.getValue("CART_MODEL1", ""),
                    CartModel::class.java
                )
        } catch (e: Exception) {

        }
        var cartModel2: CartModel? = null
        try {
            cartModel2 = Gson().fromJson<CartModel?>(
                prefProvider.getValue("CART_MODEL2", ""),
                CartModel::class.java
            )
        } catch (e: Exception) {

        }

        if (cartList == null) {
            Log.d(
                "LOADER::",
                "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
            )

            CoroutineScope(Dispatchers.Main).launch {
                getCartModelsList()
            }

            if (cartModel != null) {
                dashboardViewModel.cartModel = cartModel
                cartList = cartModel
            } else if (cartModel2 != null) {
                dashboardViewModel.cartModel = cartModel2
                cartList = cartModel2
            } else if (dashboardViewModel.cartModel == null) {
                runBlocking {
                    try {
                        var model =
                            CoroutineScope(Dispatchers.IO).async { dashboardViewModel.getCartModelBackup() }
                                .await().last().data

                        EventBus.getDefault().post(
                            MessageEvent(
                                "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_CART_BACKUP_MODEL -> model -> ${
                                    Gson().toJson(model)
                                }", true
                            )
                        )
                        cartList = Gson().fromJson(model, CartModel::class.java)
                        dashboardViewModel.cartModel =
                            Gson().fromJson(model, CartModel::class.java)
                        Log.d(
                            "LOADER::",
                            "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
                        )

                    } catch (e: Exception) {
                        var models =
                            CoroutineScope(Dispatchers.IO).async { dashboardViewModel.getAllCartModels() }
                                .await()
                        if (models.isNotEmpty()) {
                            dashboardViewModel.cartModel = models.last()
                            cartList = models.last()
                            Log.d(
                                "LOADER::",
                                "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
                            )

                        } else {
                            Log.d(
                                "LOADER::",
                                "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
                            )

                            /*Continuous loading shall occur due to the cartModel null */

                        }
                    }
                }
            }
        }

        Log.d(
            "LOADER::",
            "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
        )

        oldItems = prefProvider.getValue(Constants.OLD_ITEM_BASE_CUSTOM_ITEM, "")
        val listType = object : TypeToken<java.util.ArrayList<TbCartItem>>() {}.type
        lateinit var oldCartItemsList: ArrayList<TbCartItem>

        if (oldItems.isNotEmpty()) {

            val oldCartItemsJson = prefProvider.getValue(Constants.OLD_ITEM_BASE_CUSTOM_ITEM, "")

            oldCartItemsList = Gson().fromJson(oldCartItemsJson, listType) as ArrayList<TbCartItem>
        } else {
            prefProvider.setValueboolean(DO_PRINT, true)
        }

        EventBus.getDefault().post(
            MessageEvent(
                "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_makePaymentCreditCard() -> cartList -> ${
                    Gson().toJson(cartList)
                }", true
            )
        )
        if (cartList == null || cartList?.items == null || cartList?.items?.isEmpty() == true) {
            Log.d(
                "LOADER::",
                "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
            )

            EventBus.getDefault().post(
                MessageEvent(
                    "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_makePaymentCreditCard() -> null Case"
                )
            )

            var items: ArrayList<TbItem>? = ArrayList()
            /*  for (item in dashboardViewModel.currentCartItems) {*/
            for (item in dashboardViewModel.currentCartItems) {
                var tbItem = TbItem()
                tbItem.apply {

                    isItemEdited = item.isItemEdited

                    itemQuantity = item.itemQuantity
                    id = item.id
                    cartItemId = item.cartItemId
                    itemId = item.itemId
                    categoryId = item.categoryId
                    categoryName = item.categoryName
                    createdAt = item.createdAt
                    customItemCount = item.customItemCount
                    dineInSort = item.dineInSort
                    customItemID = item.customItemCount
                    discountId = item.discountId
                    discountPrice = item.discountPrice
                    discountType = item.discountType
                    guestItemId = item.guestItemId
                    headerPositionDinein = item.headerPositionDinein
                    hide_status = item.hide_status
                    isHide = item.isHide
                    imageUrl = item.imageUrl
                    isChecked = item.isChecked
                    isDeleted = item.isDeleted
                    isDestroy = item.isDestroy
                    isEdited = item.isEdited
                    isFired = item.isFired
                    isManualSales = item.isManualSales
                    isPaid = item.isPaid
                    itemOriginalModifiersList = item.itemOriginalModifiersList
                    modifier_set_ids = item.modifier_set_ids
                    modifiers = item.modifiers
                    name = item.name
                    note = item.note
                    manualSaleId = item.manualSaleId
                    optionSets = item.optionSets
                    website_hide_status = item.website_hide_status
                    variationsAttributes = item.variationsAttributes
                    updatedAt = item.updatedAt
                    timeStamp = item.timeStamp
                    thumbImageUrl = item.thumbImageUrl
                    taxes = item.taxes
                    sort = item.sort
                    sku = item.sku
                    singleItemPrice = item.singleItemPrice
                    shortDescription = item.shortDescription
                    reorder = item.reorder
                    quantity = item.quantity
                    price = item.price
                    orderItemId = item.orderItemId

                    try {
                        if (oldCartItemsList.isNotEmpty()) { // Order is updated

                            prefProvider.setValueboolean(Constants.DO_PRINT, true)

                            oldCartItemsList.forEach { oldItem ->
                                if ((oldItem.cartItemId == item.cartItemId) &&
                                    (oldItem.categoryId == item.categoryId) &&
                                    (oldItem.employeeID == item.employeeID) &&
                                    (oldItem.itemId == item.itemId) &&
                                    (oldItem.name.equals(item.name))
                                ) {

                                    if (item.itemQuantity != oldItem.itemQuantity) {
                                        isItemEdited = true
                                    }

                                }
                            }
                        }
                    } catch (e: Exception) {
                        //order is not updated , its new order
                    }
                }

                items!!.add(tbItem)

            }

            if (oldItems.isNotEmpty()) {

                for (item in dashboardViewModel.currentCartItems) {
                    try {
                        if (oldItems.isNotEmpty()) {
                            /*Added by Rahul to solve the modifiers not removing issue*/

                            val notPresentItems =
                                oldCartItemsList.filter { it.cartItemId != item.cartItemId }

                            if (true) {
                                var isFound = false

                                for (notPresentData in oldCartItemsList) {
                                    if (items != null) {
                                        for (it in items) {
                                            if (it.cartItemId == notPresentData.cartItemId) {
                                                isFound = true
                                                break
                                            } else isFound = false
                                        }
                                    }
                                    if (!isFound) {

                                        prefProvider.setValueboolean(DO_PRINT, true)
//                                        Not found
                                        isFound = false

                                        var tbItemDeleted = TbItem()
                                        tbItemDeleted.id = notPresentData.id
                                        tbItemDeleted.cartItemId = notPresentData.cartItemId
                                        tbItemDeleted.itemId = notPresentData.itemId
                                        tbItemDeleted.itemQuantity = notPresentData.itemQuantity
                                        tbItemDeleted.categoryId = notPresentData.categoryId
                                        tbItemDeleted.categoryName = notPresentData.categoryName
                                        tbItemDeleted.createdAt = notPresentData.createdAt
                                        tbItemDeleted.customItemCount =
                                            notPresentData.customItemCount
                                        tbItemDeleted.dineInSort = notPresentData.dineInSort
                                        tbItemDeleted.customItemID = notPresentData.customItemCount
                                        tbItemDeleted.discountId = notPresentData.discountId
                                        tbItemDeleted.discountPrice = notPresentData.discountPrice
                                        tbItemDeleted.discountType = notPresentData.discountType
                                        tbItemDeleted.guestItemId = notPresentData.guestItemId
                                        tbItemDeleted.headerPositionDinein =
                                            notPresentData.headerPositionDinein
                                        tbItemDeleted.hide_status = notPresentData.hide_status
                                        tbItemDeleted.isHide = notPresentData.isHide
                                        tbItemDeleted.imageUrl = notPresentData.imageUrl
                                        tbItemDeleted.isChecked = notPresentData.isChecked
                                        tbItemDeleted.isDeleted = notPresentData.isDeleted
                                        tbItemDeleted.isDestroy = true
                                        tbItemDeleted.isEdited = notPresentData.isEdited
                                        tbItemDeleted.isFired = notPresentData.isFired
                                        tbItemDeleted.isManualSales = notPresentData.isManualSales
                                        tbItemDeleted.isPaid = notPresentData.isPaid
                                        tbItemDeleted.itemOriginalModifiersList =
                                            notPresentData.itemOriginalModifiersList
                                        tbItemDeleted.quantity = notPresentData.quantity
                                        tbItemDeleted.modifier_set_ids =
                                            notPresentData.modifier_set_ids
                                        tbItemDeleted.modifiers = notPresentData.modifiers
                                        tbItemDeleted.name = notPresentData.name
                                        tbItemDeleted.note = notPresentData.note
                                        tbItemDeleted.manualSaleId = notPresentData.manualSaleId
                                        tbItemDeleted.optionSets = notPresentData.optionSets
                                        tbItemDeleted.website_hide_status =
                                            notPresentData.website_hide_status
                                        tbItemDeleted.variationsAttributes =
                                            notPresentData.variationsAttributes
                                        tbItemDeleted.updatedAt = notPresentData.updatedAt
                                        tbItemDeleted.timeStamp = notPresentData.timeStamp
                                        tbItemDeleted.thumbImageUrl = notPresentData.thumbImageUrl
                                        tbItemDeleted.taxes = notPresentData.taxes
                                        tbItemDeleted.sort = notPresentData.sort
                                        tbItemDeleted.sku = notPresentData.sku
                                        tbItemDeleted.singleItemPrice =
                                            notPresentData.singleItemPrice
                                        tbItemDeleted.shortDescription =
                                            notPresentData.shortDescription
                                        tbItemDeleted.reorder = notPresentData.reorder
                                        tbItemDeleted.price = notPresentData.price
                                        tbItemDeleted.orderItemId = notPresentData.orderItemId

                                        items!!.add(tbItemDeleted)
                                        break
                                    }

                                }

                            }
//                        for (notPresentItem in notPresentItems) {
//                            if (item.itemId != notPresentItem.itemId) {


//                            }
//                        }
                            Log.d("UNCOMMON:::", Gson().toJson(notPresentItems))


                        }
                    } catch (e: Exception) {
                    }

                }

            }


            /*Adding the deleted items*/
            cartList?.items = items
        }

        Log.d(
            "LOADER::",
            "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
        )

        prefProvider.setValue(Constants.OLD_ITEM, "")
        prefProvider.setValue(Constants.OLD_ITEM_BASE, "")
        prefProvider.setValue(Constants.OLD_ITEM_BASE_CUSTOM_ITEM, "")


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
        LogUtil.logE(TAG, "myRequestOriginal ${Gson().toJson(myRequest)}")
        if (myRequest != null) {
            paymentviewModel.totalPayAmount(paymentAmount)

            if (prefProvider.getValue(Constants.GIFT_CARD_TYPE, "")
                    .equals("digital", ignoreCase = true)
            ) {
                myRequest.order.orderTypeId = prefProvider.getValueInt(Constants.ORDER_TYPE_ID, -1)
            }

            EventBus.getDefault().post(
                MessageEvent(
                    "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_makePaymentCreditCard() , myRequest -> ${
                        Gson().toJson(myRequest)
                    } _1"
                )
            )
            Log.d(
                "LOADER::",
                "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
            )

            /* //FILE ASSERTION
             MainActivity.writeToFile(
                 Gson().toJson(myRequest),
                 "Pay_".plus(myRequest.order.offlineId.toString()),
                 activity?.filesDir,
                 activity!!
             )*/

            paymentAttributesRequest(myRequest)
        }
    }

    suspend fun getCartModelsList() {
        CoroutineScope(Dispatchers.IO).async {
            var cartListFromDb: List<CartModel> = dashboardViewModel.getAllCartModels()
            if (cartListFromDb != null) {
                try {
                    cartList = cartListFromDb.get(0)
                    dashboardViewModel.cartModel = cartList
                } catch (e: Exception) {
                }

            }
        }.await()

    }


    // make order request with payment attributes on cash payment to reflect on server
    private fun makeCashPayment(dynamicPaymentType: String? = "", dynamicPaymentId: Int = -1) {

        /**
         * Added to check tip details
         * **/

        dashboardViewModel.apply {
            totalAmount = paymentAmount
            paymentTypeForTip = "cash"
        }

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

        /*val myRequest = cartList?.let {
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
        }*/
        var cartModel = Gson().fromJson<CartModel?>(
            prefProvider.getValue("CART_MODEL1", ""),
            CartModel::class.java
        )
        var cartModel2 = Gson().fromJson<CartModel?>(
            prefProvider.getValue("CART_MODEL2", ""),
            CartModel::class.java
        )
        if (cartModel != null) {
            EventBus.getDefault()
                .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew, if (cartModel != null)_1"))

            dashboardViewModel.cartModel = cartModel
            val myRequest = cartModel.let {
                paymentviewModel.createOrderRequestNew(
                    oldItems,
                    dashboardViewModel.currentCartItems,
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
                paymentAttributesRequest(myRequest, dynamicPaymentType, dynamicPaymentId)
            }
        } else if (cartModel2 != null) {
            EventBus.getDefault()
                .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew, else if (cartModel2 != null)_1"))

            dashboardViewModel.cartModel = cartModel2
            val myRequest = cartModel2.let {
                paymentviewModel.createOrderRequestNew(
                    oldItems,
                    dashboardViewModel.currentCartItems,
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

                EventBus.getDefault().post(
                    MessageEvent(
                        "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_makeCashPayment() , myRequest -> ${
                            Gson().toJson(myRequest)
                        } _1"
                    )
                )
/*

                //FILE ASSERTION
                MainActivity.writeToFile(
                    Gson().toJson(myRequest),
                    "Pay_".plus(myRequest.order.offlineId.toString()),
                    activity?.filesDir,
                    activity!!
                )
*/
                dashboardViewModel.cartModel?.let {
                    if (it.orderTypeId == 9) {
                        myRequest.order.orderTypeId = it.orderTypeId
                    }
                }
                paymentAttributesRequest(myRequest, dynamicPaymentType, dynamicPaymentId)
            }
        } else {

            /* Added by Rahul to solve the cartModel crash issue, i.e. cartModel is getting null - START*/

            if (dashboardViewModel.cartModel == null) {
                EventBus.getDefault()
                    .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew, if (dashboardViewModel.cartModel == null)_1"))

                var currentCartItems = arrayListOf<TbItem>()
                for (tbItem in dashboardViewModel.currentCartItems) {
                    currentCartItems.add(TbItem().convertCartToItem(tbItem, tbItem))
                }
                var isManual = false
                if (prefProvider.getValue(Constants.REDIRECT_FROM, "").equals("manual_sale")) {
                    isManual = true
                } else {
                    isManual = false
                }
                dashboardViewModel.cartModel = CartModel().apply {
                    terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, -1)
                    employeeID = prefProvider.getValueInt(Constants.EMPLOYEE_ID, -1)
                    locationId = prefProvider.getValueInt(Constants.LOCATION_ID, -1)
                    orderTypeId = prefProvider.getValueInt(Constants.ORDER_TYPE_ID, -1)
                    orderType = prefProvider.getValue(ORDER_TYPE, "").toString()
                    orderTypeName = prefProvider.getValue(Constants.ORDER_TYPE_NAME, "").toString()
                    items = currentCartItems
                    isOpenOrder = false
                    isMaual = isManual
                    isEdited = false
                    customer = Gson().fromJson(
                        prefProvider.getValue("pref_customer", "").toString(),
                        TbCustomer::class.java
                    )
                    taxlistDynamic = Gson().fromJson(
                        prefProvider.getValue("taxlistDynamic", "").toString(),
                        object : TypeToken<List<TaxData>?>() {}.getType()
                    )
                    if (orderTypeId == -1) {
                        runBlocking {
                            CoroutineScope(Dispatchers.IO).async {
                                dashboardViewModel.getOrderTypeBackupList(employeeID)?.let {
                                    orderTypeId = (it.get(0).orderType) ?: -1
                                }
                            }.await()
                        }
                    }
                }

                dashboardViewModel.addCart(dashboardViewModel.cartModel!!)
                EventBus.getDefault()
                    .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew, if (dashboardViewModel.cartModel == null)_2"))
                EventBus.getDefault().post(
                    MessageEvent(
                        "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew, if (dashboardViewModel.cartModel == null) -> ${
                            Gson().toJson(dashboardViewModel.cartModel)
                        }"
                    )
                )

            }

            /* Added by Rahul to solve the cartModel crash issue, i.e. cartModel is getting null - END*/

            EventBus.getDefault().post(
                MessageEvent(
                    "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_1 ->  paymentAmount -> ${
                        Gson().toJson(subTotalPrice) + " paymentAmount -> " + Gson().toJson(
                            paymentAmount
                        )
                    }", true
                )
            )
            val myRequest = dashboardViewModel.cartModel?.let {
                paymentviewModel.createOrderRequestNew(
                    oldItems,
                    dashboardViewModel.currentCartItems,
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
                EventBus.getDefault()
                    .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew, if (myRequest != null)_1"))
                EventBus.getDefault().post(
                    MessageEvent(
                        "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew, if (myRequest != null) -> ${
                            Gson().toJson(myRequest)
                        }"
                    )
                )

                prefProvider.setValue("CART_MODEL1", "")
                prefProvider.setValue("CART_MODEL2", "")

                if (custom_paymentAmount != 0.0) {
                    paymentviewModel.totalPayAmount(custom_paymentAmount)
                    EventBus.getDefault().post(
                        MessageEvent(
                            "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew, if (custom_paymentAmount != 0.0) -> ${
                                Gson().toJson(custom_paymentAmount)
                            }"
                        )
                    )
                }

                EventBus.getDefault().post(
                    MessageEvent(
                        "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_makeCashPayment()_if (myRequest != null) , myRequest -> ${
                            Gson().toJson(myRequest)
                        } _2"
                    )
                )
/*

                //FILE ASSERTION
                MainActivity.writeToFile(
                    Gson().toJson(myRequest),
                    "Pay_".plus(myRequest.order.offlineId.toString()),
                    activity?.filesDir,
                    activity!!
                )
*/
                dashboardViewModel.cartModel?.let {
                    if (it.orderTypeId == 9) {
                        myRequest.order.orderTypeId = it.orderTypeId
                    }
                }

                /*---------totalAmount was being sent half in case of split, so multiplied if the subtotal is greater than the totalAmount----------*/
                if (myRequest.order.subTotal > myRequest.order.totalAmount) {
                    myRequest.order.totalAmount = myRequest.order.totalAmount * 2
                }
                /*---------totalAmount was being sent half in case of split, so multiplied if the subtotal is greater than the totalAmount----------*/


                paymentAttributesRequest(myRequest, dynamicPaymentType, dynamicPaymentId)
            }
        }

    }

    private fun makeDynamicCashPayment(
        dynamicPaymentType: String? = "",
        dynamicPaymentId: Int = -1
    ) {

        /**
         * Added to check tip details
         * **/

        dashboardViewModel.apply {
            totalAmount = paymentAmount
            paymentTypeForTip = "cash"
        }

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

        /*val myRequest = cartList?.let {
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
        }*/
        var cartModel = Gson().fromJson<CartModel?>(
            prefProvider.getValue("CART_MODEL1", ""),
            CartModel::class.java
        )
        var cartModel2 = Gson().fromJson<CartModel?>(
            prefProvider.getValue("CART_MODEL2", ""),
            CartModel::class.java
        )
        if (cartModel != null) {
            EventBus.getDefault()
                .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew, if (cartModel != null)_1"))

            dashboardViewModel.cartModel = cartModel
            val myRequest = cartModel.let {
                paymentviewModel.createOrderRequestNew(
                    oldItems,
                    dashboardViewModel.currentCartItems,
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
                paymentAttributesRequest(myRequest, dynamicPaymentType, dynamicPaymentId)
            }
        } else if (cartModel2 != null) {
            EventBus.getDefault()
                .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew, else if (cartModel2 != null)_1"))

            dashboardViewModel.cartModel = cartModel2
            val myRequest = cartModel2.let {
                paymentviewModel.createOrderRequestNew(
                    oldItems,
                    dashboardViewModel.currentCartItems,
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

                EventBus.getDefault().post(
                    MessageEvent(
                        "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_makeCashPayment() , myRequest -> ${
                            Gson().toJson(myRequest)
                        } _1"
                    )
                )
/*

                //FILE ASSERTION
                MainActivity.writeToFile(
                    Gson().toJson(myRequest),
                    "Pay_".plus(myRequest.order.offlineId.toString()),
                    activity?.filesDir,
                    activity!!
                )
*/
                dashboardViewModel.cartModel?.let {
                    if (it.orderTypeId == 9) {
                        myRequest.order.orderTypeId = it.orderTypeId
                    }
                }
                paymentAttributesRequest(myRequest, dynamicPaymentType, dynamicPaymentId)
            }
        } else {

            /* Added by Rahul to solve the cartModel crash issue, i.e. cartModel is getting null - START*/

            if (dashboardViewModel.cartModel == null) {
                EventBus.getDefault()
                    .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew, if (dashboardViewModel.cartModel == null)_1"))

                var currentCartItems = arrayListOf<TbItem>()
                for (tbItem in dashboardViewModel.currentCartItems) {
                    currentCartItems.add(TbItem().convertCartToItem(tbItem, tbItem))
                }
                var isManual = false
                if (prefProvider.getValue(Constants.REDIRECT_FROM, "").equals("manual_sale")) {
                    isManual = true
                } else {
                    isManual = false
                }
                dashboardViewModel.cartModel = CartModel().apply {
                    terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, -1)
                    employeeID = prefProvider.getValueInt(Constants.EMPLOYEE_ID, -1)
                    locationId = prefProvider.getValueInt(Constants.LOCATION_ID, -1)
                    orderTypeId = prefProvider.getValueInt(Constants.ORDER_TYPE_ID, -1)
                    orderType = prefProvider.getValue(ORDER_TYPE, "").toString()
                    orderTypeName = prefProvider.getValue(Constants.ORDER_TYPE_NAME, "").toString()
                    items = currentCartItems
                    isOpenOrder = false
                    isMaual = isManual
                    isEdited = false
                    customer = Gson().fromJson(
                        prefProvider.getValue("pref_customer", "").toString(),
                        TbCustomer::class.java
                    )
                    taxlistDynamic = Gson().fromJson(
                        prefProvider.getValue("taxlistDynamic", "").toString(),
                        object : TypeToken<List<TaxData>?>() {}.getType()
                    )
                    if (orderTypeId == -1) {
                        runBlocking {
                            CoroutineScope(Dispatchers.IO).async {
                                dashboardViewModel.getOrderTypeBackupList(employeeID)?.let {
                                    orderTypeId = (it.get(0).orderType) ?: -1
                                }
                            }.await()
                        }
                    }
                }

                dashboardViewModel.addCart(dashboardViewModel.cartModel!!)
                EventBus.getDefault()
                    .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew, if (dashboardViewModel.cartModel == null)_2"))
                EventBus.getDefault().post(
                    MessageEvent(
                        "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew, if (dashboardViewModel.cartModel == null) -> ${
                            Gson().toJson(dashboardViewModel.cartModel)
                        }"
                    )
                )

            }

            /* Added by Rahul to solve the cartModel crash issue, i.e. cartModel is getting null - END*/

            EventBus.getDefault().post(
                MessageEvent(
                    "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_1 ->  paymentAmount -> ${
                        Gson().toJson(subTotalPrice) + " paymentAmount -> " + Gson().toJson(
                            paymentAmount
                        )
                    }", true
                )
            )
            val myRequest = dashboardViewModel.cartModel?.let {
                paymentviewModel.createOrderRequestNew(
                    oldItems,
                    dashboardViewModel.currentCartItems,
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
                EventBus.getDefault()
                    .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew, if (myRequest != null)_1"))
                EventBus.getDefault().post(
                    MessageEvent(
                        "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew, if (myRequest != null) -> ${
                            Gson().toJson(myRequest)
                        }"
                    )
                )

                prefProvider.setValue("CART_MODEL1", "")
                prefProvider.setValue("CART_MODEL2", "")

                if (custom_paymentAmount != 0.0) {
                    paymentviewModel.totalPayAmount(custom_paymentAmount)
                    EventBus.getDefault().post(
                        MessageEvent(
                            "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew, if (custom_paymentAmount != 0.0) -> ${
                                Gson().toJson(custom_paymentAmount)
                            }"
                        )
                    )
                }

                EventBus.getDefault().post(
                    MessageEvent(
                        "${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_makeCashPayment()_if (myRequest != null) , myRequest -> ${
                            Gson().toJson(myRequest)
                        } _2"
                    )
                )
/*

                //FILE ASSERTION
                MainActivity.writeToFile(
                    Gson().toJson(myRequest),
                    "Pay_".plus(myRequest.order.offlineId.toString()),
                    activity?.filesDir,
                    activity!!
                )
*/
                dashboardViewModel.cartModel?.let {
                    if (it.orderTypeId == 9) {
                        myRequest.order.orderTypeId = it.orderTypeId
                    }
                }
                paymentAttributesRequest(myRequest, dynamicPaymentType, dynamicPaymentId)
            }
        }

    }

    /* private fun cartModel(order: OpenOrderResponse.Data.Order): CartModel {
         LogUtil.logE("futureDeliveryDate  ", Gson().toJson(order))
         return CartModel().apply {
             terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, -1)
             employeeID = prefProvider.getValueInt(Constants.EMPLOYEE_ID, -1)
             locationId = prefProvider.getValueInt(Constants.LOCATION_ID, -1)
             orderTypeId = prefProvider.getValueInt(Constants.ORDER_TYPE_ID, -1)
             orderType = prefProvider.getValue(ORDER_TYPE, "").toString()
             orderTypeName = prefProvider.getValue(Constants.ORDER_TYPE_NAME, "").toString()
             isOpenOrder = false
             serviceCharge = serviceChargesList(order)
             customer = assignCustomer(order)
             items = inventoryList(order)
             note = order.note
             var itemDiscount = 0.0
             items?.forEach {
                 itemDiscount += it.discountPrice
             }
             discountPrice = order.totalDiscount
             deliveryType = order.deliveryType ?: ""
             taxlistDynamic = getTaxBirfucationList(order.orderItems)

         }
     }*/

    /*   private fun makeCashPayment() {
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
           Log.e("checkCartList", "cartList:  ${Gson().toJson(cartList)}")
           Log.e("checkCartList", "cartList:  ${Gson().toJson(dashboardViewModel.cartModel)}")
           *//*val myRequest = cartList?.let {
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
        }*//*
        val myRequest = dashboardViewModel.cartModel?.let {
            paymentviewModel.createOrderRequestNew(
                dashboardViewModel.currentCartItems,
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
*/
    // generate payment attributes request
    private fun paymentAttributesRequest(
        myRequest: OrderRequestModel,
        dynamicPaymentType: String? = "",
        dynamicPaymentId: Int = -1
    ) {
        Log.d(
            "LOADER::",
            "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
        )

        dynamicPaymentType?.let { payment ->
            if (payment.isNotEmpty() && dynamicPaymentId != -1) {
                myRequest.order.paymentAttributes?.let {
                    it.paymentType = getString(R.string.external)
                    it.dynamicPaymentId = dynamicPaymentId.toString()
                }
            }
        }

        /* This is placed to solve the payment issue happening due to the orderTypeId = -1   */
        if (myRequest.order.orderTypeId == -1) {
            runBlocking {
                CoroutineScope(Dispatchers.IO).async {
                    dashboardViewModel.getOrderTypeBackupList(
                        prefProvider.getValueInt(
                            Constants.EMPLOYEE_ID,
                            -1
                        )
                    )?.let {
                        myRequest.order.apply {
                            if (it.isNotEmpty()) {
                                orderTypeId = (it.get(0).orderType) ?: -1
                                orderTypeName = (it.get(0).orderTypeName) ?: ""
                            } else {
                                if (dashboardViewModel.cartModel != null) {
                                    orderTypeId = dashboardViewModel.cartModel!!.orderTypeId ?: -1
                                    orderTypeName =
                                        dashboardViewModel.cartModel!!.orderTypeName ?: ""
                                } else {
//                                  Fetch the order type name from the cart fragment, fetch the orderType from local database with respect to the order type name of cart fragment
                                    var orderType = prefProvider.getValue(ORDER_TYPE, "")
                                    dashboardViewModel.getOrderTypes.value?.data?.filter {
                                        it.orderType.equals(
                                            orderType
                                        )
                                    }?.let {
                                        orderTypeId = it.first().id ?: -1
                                        orderTypeName = it.first().orderType ?: ""
                                    }

                                }
                            }
                        }
                    }
                }.await()
            }
        }

        Log.d(
            "LOADER::",
            "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
        )

        val orderId = prefProvider.getValueInt("ORDER_ID", -1) //Here
        LogUtil.logE(TAG, "orderIdmyRequestOriginal ${orderId}")
        Log.e("textToPay", textToPay.toString())

        EventBus.getDefault().post(
            MessageEvent(
                "${Constants.LINE_BREAK_TAB} paymentAttributesRequest(myRequest: OrderRequestModel), myRequest=${
                    Gson().toJson(myRequest)
                } _6"
            )
        )
        EventBus.getDefault().post(
            MessageEvent(
                "${Constants.LINE_BREAK_TAB} paymentAttributesRequest(myRequest: OrderRequestModel), orderId=${
                    Gson().toJson(orderId)
                } _6"
            )
        )

        if (orderId == -1) {
            EventBus.getDefault()
                .post(MessageEvent("${Constants.LINE_BREAK_TAB} paymentAttributesRequest(myRequest: OrderRequestModel), if (orderId == -1) _6"))

            if (textToPay) {
                EventBus.getDefault()
                    .post(MessageEvent("${Constants.LINE_BREAK_TAB} paymentAttributesRequest(myRequest: OrderRequestModel), if (textToPay) = ${textToPay} _6"))
                myRequest.completed_all_payments = false
            } else if (prefProvider.getValueboolean(IS_GIFT_CARD_REDEEM, false)) {
                EventBus.getDefault().post(
                    MessageEvent(
                        "${Constants.LINE_BREAK_TAB} paymentAttributesRequest(myRequest: OrderRequestModel), else if (prefProvider.getValueboolean(IS_GIFT_CARD_REDEEM, false)) = ${
                            prefProvider.getValueboolean(
                                IS_GIFT_CARD_REDEEM,
                                false
                            )
                        } _6"
                    )
                )
                myRequest.completed_all_payments = prefProvider.getValueboolean(
                    Constants.IS_ORDER_REDEEMABLE_WITH_GIFT_CARD,
                    false
                ) && isSelectedCount <= 1
            } else {
                EventBus.getDefault()
                    .post(MessageEvent("${Constants.LINE_BREAK_TAB} paymentAttributesRequest(myRequest: OrderRequestModel), else  _6"))
                if (myRequest.order.totalAmount != 0.0) {
                    EventBus.getDefault()
                        .post(MessageEvent("${Constants.LINE_BREAK_TAB} paymentAttributesRequest(myRequest: OrderRequestModel), myRequest.order.totalAmount= ${myRequest.order.totalAmount}  _6"))
                    myRequest.completed_all_payments = isSelectedCount <= 1
                } else {
                    EventBus.getDefault()
                        .post(MessageEvent("${Constants.LINE_BREAK_TAB} paymentAttributesRequest(myRequest: OrderRequestModel), else else  _6"))
                    myRequest.completed_all_payments = true
                }
            }
            println("submit request in case of order id -1")
            EventBus.getDefault().post(
                MessageEvent(
                    "${Constants.LINE_BREAK_TAB} paymentAttributesRequest(myRequest: OrderRequestModel)_Before_paymentviewModel.submit(myRequest), myRequest=${
                        Gson().toJson(myRequest)
                    } _6"
                )
            )
            paymentviewModel.submit(myRequest)
        } else {
            Log.d(
                "LOADER::",
                "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
            )

            EventBus.getDefault()
                .post(MessageEvent("${Constants.LINE_BREAK_TAB} paymentAttributesRequest(myRequest: OrderRequestModel)_else_before_if(textToPay)_6"))

            if (textToPay) {
                Log.d(
                    "LOADER::",
                    "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
                )

                paymentviewModel.textPaySplit(orderId)

            } else {
                Log.d(
                    "LOADER::",
                    "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
                )

                EventBus.getDefault()
                    .post(MessageEvent("${Constants.LINE_BREAK_TAB} paymentAttributesRequest(myRequest: OrderRequestModel)_else_6"))

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

                if (paymentReq?.gift_card_redeemed_amount != null && paymentReq?.gift_card_redeemed_amount?:0.00  > 0.00){
                    Log.e("checkSplit","giftCardwholeTotal ${WholetotalPrice}" )

                    Log.e("checkSplit","viewmodelTotal  ${dashboardViewModel.totalPrice}")
                    if (paymentReq.gift_card_redeemed_amount?.toDouble() != totalPrice.toDouble()) {
                        paymentReq.gift_card_redeemed_amount = dashboardViewModel.totalPrice
                        paymentReq.amount = dashboardViewModel.totalPrice
                        Log.e("AcceptPaymentReq","changedParams")
                    }


                }
                val aa = SpitByOrderRequestModel(
                    orderId, isSelectedCount <= 1,
                    SpitByOrderPaymentModel(
                        listOf(paymentReq) as List<PaymentAttributes>,
                    ),
                    gift_card_redeem = prefProvider.getValueboolean(IS_GIFT_CARD_REDEEM, false),
                    gift_card = giftCardRedeem
                )

                EventBus.getDefault()
                    .post(MessageEvent("${Constants.LINE_BREAK_TAB} paymentAttributesRequest(myRequest: OrderRequestModel)_paymentviewModel.splitByOrder(aa, false)_Before_6"))
                EventBus.getDefault().post(
                    MessageEvent(
                        "${Constants.LINE_BREAK_TAB} paymentAttributesRequest(myRequest: OrderRequestModel)_paymentviewModel.splitByOrder(aa, false), , aa -> ${
                            Gson().toJson(aa)
                        }  _6"
                    )
                )
                paymentviewModel.splitByOrder(aa, false)
                runOnUiThread(Runnable {
                    dismissProgressDialog()
                })
                Log.d(
                    "LOADER::",
                    "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
                )

                EventBus.getDefault()
                    .post(MessageEvent("${Constants.LINE_BREAK_TAB} paymentAttributesRequest(myRequest: OrderRequestModel)_paymentviewModel.splitByOrder(aa, false)_After_6"))
                Log.d(
                    "LOADER::",
                    "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
                )

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
        ProgressUtils.showProgressDialog(
            if (prefProvider.getValueboolean(IS_PAX_PAYMENT_FAILED, false)) {
                getString(R.string.reattempting_the_payment)
            } else {
                "Please wait payment under process"
            }, requireActivity()
        )

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
                                (if (i == 3) {
                                    cardNumber = cardNumber.takeLast(4)
                                } else if (i == 1) {
                                    cardNumber =
                                        (response.body()!![0].dataOutput?.PANLast4).toString()
                                } else if (i == 2) {
                                    cardNumber =
                                        (response.body()!![0].dataOutput?.PANLast4).toString()
                                } else {
                                    cardNumber = ""
                                }).toString()
                            )
                            giftCardViewModel.setMagensaResponse(
                                Gson().toJson(response.body()!![0]),
                                (if (i == 3) {
                                    cardNumber = cardNumber.takeLast(4)
                                } else if (i == 1) {
                                    cardNumber =
                                        (response.body()!![0].dataOutput?.PANLast4).toString()
                                } else if (i == 2) {
                                    cardNumber =
                                        (response.body()!![0].dataOutput?.PANLast4).toString()
                                } else {
                                    cardNumber = ""
                                }).toString()
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

    private fun sellGiftCardUsingCash(dynamicPaymentType: String = "", dynamicPaymentId: Int = 0) {
//        paymentType = "Cash"

        paymentType = if (prefProvider.getValueboolean(IS_GIFT_CARD_REDEEM, false)) {
            Constants.EXTERNAL_PAYMENT
        } else if (dynamicPaymentType.isNotEmpty()) {
            Constants.EXTERNAL_PAYMENT
        } else {
            "Cash"
        }

        giftCardViewModel.customCashAmount = custom_paymentAmount

        if (cartList == null) {
            runBlocking {
                lifecycleScope.async(Dispatchers.IO) {
                    cartList = dashboardViewModel.getAllCartModels().last()
                }.await()
            }
        }
        val myRequest = cartList?.let {
            giftCardViewModel.createSellGiftCardRequestUsingCash(paymentType = paymentType)
        }


        if (prefProvider.getValue(Constants.GIFT_CARD_TYPE, "").equals("Physical", true)) {
            Log.e(TAG, "checkPlastiCard  ${myRequest?.gift_card?.amount}")

            myRequest?.let { giftCardViewModel.sellGiftCard(it) }

        } else {
            if (myRequest != null) {
                giftCardViewModel.sellGiftCard(myRequest)
            }
        }
    }

    private fun sellGiftCardUsingCard(amt: Double=0.0) {
        paymentType = "Card"
        EventBus.getDefault()
            .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ sellGiftCardUsingCard cartList->${cartList}"))
        if (cartList == null) {
            runBlocking {
                lifecycleScope.async(Dispatchers.IO) {
                    cartList = dashboardViewModel.getAllCartModels().last()
                }.await()
            }
        }
        EventBus.getDefault()
            .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ sellGiftCardUsingCard cartList->${cartList}"))
        val myRequest = cartList?.let {
            EventBus.getDefault()
                .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ sellGiftCardUsingCard inside the myRequest = cartList?.let"))
            giftCardViewModel.createSellGiftCardRequestUsingCard(amt)
        }
        EventBus.getDefault()
            .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ sellGiftCardUsingCard before if (myRequest != null)"))
        if (myRequest != null) {
            EventBus.getDefault()
                .post(MessageEvent("${Constants.LINE_BREAK_TAB} CheckoutDetailsFragmentNew.kt_ sellGiftCardUsingCard inside if (myRequest != null)"))
            if (prefProvider.getValue(Constants.GIFT_CARD_TYPE, "").equals("Physical", true)) {
                giftCardViewModel.sellGiftCard(myRequest)
            } else {
                giftCardViewModel.sellGiftCard(myRequest)
            }
        }
    }

    private fun addValueInGiftCardUsingCash(dynamicPaymentName: String = "") {
        paymentType = "Cash"
        if (cartList == null) {
            runBlocking {
                lifecycleScope.async(Dispatchers.IO) {
                    cartList = dashboardViewModel.getAllCartModels().last()
                }.await()
            }
        }
        val myRequest = cartList?.let {
            giftCardViewModel.createAddValueInGiftCardRequestUsingCash(dynamicPaymentName)
        }
        if (myRequest != null) {
            if (prefProvider.getValue(Constants.GIFT_CARD_TYPE, "").equals("Physical", true)) {
                myRequest.gift_card.gift_card_type = "Physical"
                giftCardViewModel.customCashAmount = custom_paymentAmount
                giftCardViewModel.addValueInPhysicalGiftCard(true, myRequest)
            } else {
                giftCardViewModel.customCashAmount = custom_paymentAmount
                giftCardViewModel.addValueInGiftCard(true, myRequest)
            }
        }
    }

    private fun addValueInGiftCardUsingCard(paymentAmount:Double=0.0) {
        paymentType = "Card"
        if (cartList == null) {
            runBlocking {
                lifecycleScope.async(Dispatchers.IO) {
                    cartList = dashboardViewModel.getAllCartModels().last()
                }.await()
            }
        }
        val myRequest = cartList?.let {
            giftCardViewModel.createAddValueInGiftCardRequestUsingCard(paymentAmount)
        }
        if (myRequest != null) {
            if (prefProvider.getValue(Constants.GIFT_CARD_TYPE, "").equals("Physical", true)) {
                myRequest.gift_card.gift_card_type = "Physical"
                giftCardViewModel.addValueInPhysicalGiftCard(false, myRequest)
            } else {
                giftCardViewModel.addValueInGiftCard(false, myRequest)

            }
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
                    dashboardViewModel.subTotalPrice,
                    dashboardViewModel.totalPrice,
                    dashboardViewModel.totalServiceCharge,
                    dashboardViewModel.totalTax,
                    prefProvider.getValue(Constants.ORDER_TYPE, "").toString(),
                    "",
                    "",
                    false,
                    dashboardViewModel.totalDiscount,
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
        Log.d("onInputAccountStart", "onInputAccountStart")
    }

    override fun onEnterExpiryDate() {
        Log.d("onEnterExpiryDate", "onEnterExpiryDate")
    }

    override fun onEnterZip() {
        Log.d("onEnterZip", "onEnterZip")
    }

    override fun onEnterCVV() {
        Log.d("onEnterCVV", "onEnterCVV")
    }

    override fun onSelectEMVApp(p0: MutableList<String>?) {
        Log.d("onSelectEMVApp", "onSelectEMVApp ${p0.toString()}")
    }

    override fun onProcessing(p0: String?, p1: String?) {
        Log.d("onProcessing", "onProcessing $p0 $p1")
    }

    override fun onWarnRemoveCard() {
        Log.d("onWarnRemoveCard", "onWarnRemoveCard")
    }

    override fun onFinish(p0: InputAccount.InputAccountResponse?) {
        Log.d("InputAccount onFinish", "onFinish ${p0.toString()}")
    }

    private fun clearObserver() {
        viewLifecycleOwnerLiveData.removeObservers(viewLifecycleOwner)
        onDestroy()

    }

    override fun onDestroyView() {
        super.onDestroyView()

    }

    private var builder: Dialog? = null

    private fun showProgressDialog() {


        if (builder == null)
            builder = Dialog(requireContext())

        val inflater = LayoutInflater.from(context)

        val dialogView = inflater.inflate(R.layout.view_loading, null)
        builder?.setContentView(dialogView)

        builder?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//        builder?.window?.setBackgroundDrawable(
//            ColorDrawable(Color.WHITE)
//        )
        builder?.setCanceledOnTouchOutside(false)
        builder?.setCancelable(false)
        builder?.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        if (!builder?.isShowing!!) {
            val activity: Activity = requireActivity()
            if (!activity.isFinishing && !activity?.isDestroyed) {
                try {
                    builder?.show()
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }

            }
        }
    }

    private fun dismissProgressDialog() {
        try {
            if (builder != null && builder?.isShowing == true) {
                builder?.dismiss()
                builder = null
            }
        } catch (e: java.lang.Exception) {
            Log.d("pos", "dismissProgressDialog: " + e.message)
        }

    }
}