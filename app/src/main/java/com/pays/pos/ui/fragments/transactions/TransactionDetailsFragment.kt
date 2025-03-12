package com.pays.pos.ui.fragments.transactions

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.*
import android.util.Base64
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.data.entities.TaxData
import com.pays.pos.data.entities.TbServiceCharge
import com.pays.pos.data.model.GetPaymentOrderDetailsResponse
import com.pays.pos.data.model.requestModel.RefundRequestModelOnlineOrder
import com.pays.pos.data.model.responseModel.*
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.DINE_IN
import com.pays.pos.data.remote.Constants.KEY
import com.pays.pos.data.remote.Constants.ORDER_NUMBER_STARTING_FROM_ONE
import com.pays.pos.data.remote.Constants.PHONE_ORDER
import com.pays.pos.data.remote.Constants.SHIPPING_ADDRESS
import com.pays.pos.data.remote.Constants.SUNMI_INNER_PRINTER
import com.pays.pos.data.remote.Constants.SUNMI_PRINTER
import com.pays.pos.data.remote.Constants.getCurrentTimeFromTimeZone
import com.pays.pos.databinding.FragmentTransactionDetailsBinding
import com.pays.pos.di.ApiModule1
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.adapter.OrderDetailsItemListAdapter
import com.pays.pos.ui.adapter.boldpos.TaxBirfurcationAdapter
import com.pays.pos.ui.fragments.magtek.MagtekRequestUtils
import com.pays.pos.ui.fragments.magtek.MagtekViewModel
import com.pays.pos.ui.fragments.magtek.PaymentResponse
import com.pays.pos.ui.fragments.settings.hardware.printer.BluetoothUtil
import com.pays.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper
import com.pays.pos.utils.*
import com.pays.pos.utils.TimeFormatUtils.convertCurrentDate
import com.pays.pos.utils.TimeFormatUtils.convertCurrentTime
import com.pays.pos.utils.extensions.*
import com.pays.pos.utils.paxUtils.AppThreadPool
import com.pays.pos.utils.paxUtils.POSLinkCreatorWrapper
import com.pays.pos.utils.paxUtils.SettingINI
import com.pays.pos.utils.printer.PrinterClass
import com.pays.pos.utils.statusUtils.Status
import com.epson.epos2.printer.Printer
import com.epson.eposprint.Builder
import com.epson.eposprint.Print
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.pax.poslink.PaymentRequest
import com.pax.poslink.PosLink
import com.pax.poslink.ProcessTransResult
import com.google.gson.reflect.TypeToken
import com.pax.poslink.ReportRequest
import com.pays.payments.design.*
import com.pays.payments.gateways.dejavoo.DejavooPaymentGateway
import com.pays.payments.gateways.valor.ValorPaymentGateway
import com.pays.pos.data.model.requestModel.RefundRequestModel
import com.pays.pos.data.model.valor.ValorSuccessResponse
import com.pays.pos.data.model.valor.ValorTransactionsList
import com.pays.pos.data.remote.Constants.BUSINESS_ADDRESS
import com.pays.pos.data.remote.Constants.BUSINESS_PHONE_NO
import com.pays.pos.data.remote.Constants.LANDI_INNER_PRINTER
import com.pays.pos.data.remote.Constants.VENUE_LOGO
import com.pays.pos.data.remote.Constants.getReceiptFormatDateFromUTCServer
import com.pays.pos.logger.CashBoxEvent
import com.pays.pos.logger.MessageEvent
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.pays.pos.utils.landi.LPrint
import com.sdksuite.omnidriver.OmniConnection
import com.sdksuite.omnidriver.OmniDriver
import com.sdksuite.omnidriver.aidl.printer.Align
import com.sdksuite.omnidriver.api.OnPrintListener
import com.starmicronics.stario10.InterfaceType
import com.starmicronics.stario10.StarConnectionSettings
import com.starmicronics.stario10.StarPrinter
import com.starmicronics.stario10.starxpandcommand.DocumentBuilder
import com.starmicronics.stario10.starxpandcommand.MagnificationParameter
import com.starmicronics.stario10.starxpandcommand.PrinterBuilder
import com.starmicronics.stario10.starxpandcommand.StarXpandCommandBuilder
import com.starmicronics.stario10.starxpandcommand.printer.Alignment
import com.starmicronics.stario10.starxpandcommand.printer.CutType
import com.starmicronics.stario10.starxpandcommand.printer.InternationalCharacterType
import com.sunmi.externalprinterlibrary.api.ConnectCallback
import com.sunmi.externalprinterlibrary.api.SunmiPrinter
import com.sunmi.externalprinterlibrary.api.SunmiPrinterApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.StringReader
import java.lang.ref.WeakReference
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory

@AndroidEntryPoint
class TransactionDetailsFragment : Fragment() {

    private var isPrintCustomer = false
    private lateinit var binding: FragmentTransactionDetailsBinding
    private val viewModel by viewModels<TransactionDetailsViewModel>()
    private val dashboardCategoryViewModel by viewModels<DashBoardCategoryViewModel>()
    private val transactionViewModel by viewModels<TransactionViewModel>()
    private var isPrint: Boolean = false
    private val magtekProViewModel by viewModels<MagtekViewModel>()
    lateinit var weakContext: WeakReference<Context>

    private lateinit var orderDetailsItemAdapter: OrderDetailsItemListAdapter
    private lateinit var taxBirfurcationAdapter: TaxBirfurcationAdapter
    private var orderIDglobal = 0
    var taxClickable = false

    private var sunmiFrameworkVersion: Array<String>? = null
    private var oneItemPerReceipt: Boolean = true

    /*Star label printer - START*/
    lateinit var settings: StarConnectionSettings
    lateinit var printer: StarPrinter
    /*Star label printer - END*/

    @Inject
    lateinit var paymentGatewayFactory: PaymentGatewayFactory

    /*This variable will be used to check if the orderID is to be printed in the sticky receipt */
    private var printOrderIDInStickyPrinter: Boolean = true

    @Inject
    lateinit var apiModule1: ApiModule1

    @Inject
    lateinit var magtekRequestUtils: MagtekRequestUtils

    private var tipAmount: Double = 0.0
    private var kitchenPrinterList: List<PrinterResponse.Data.KitchenReceiptPrinters> = listOf()

    //    private lateinit var orderDetailsResponse: GetOrderDetailsResponse
    private var customerSettingModel = GetCustomerReceiptSettingsResponse.Data()
    private var kitchenSettingModel = GetKitchenReceiptSettingsResponse.Data()
    private lateinit var paymentDetailsResponse: GetPaymentOrderDetailsResponse
    private var orderId: Int = -1
    private var orderType: String = ""
    var mLastClickTime: Long = 0
    private val TAG = "TransactionDetailsFr"
    private var tipsList: List<GetTipReponse.Data> = listOf()
    private var paymentId: Int = -1
    private var isFromTrans: Boolean = false
    private var isFromOnlineOrderRefund: Boolean = false
    private var serviceChargesList: ArrayList<TbServiceCharge>? = arrayListOf()
    private var taxlistbirfurcation: ArrayList<TaxData>? = arrayListOf()
    private var receiptModel: CreateOrderResponse.Data? = null
    private var isSplitPayment = false

    // PAX variables
    private lateinit var mPaymentRequest: PaymentRequest
    private var posLink: PosLink = PosLink()
    var CARDBIN = ""
    var cardLastDigits = ""
    var CardName = ""
    var EDCType = ""
    var omniDriver: OmniDriver? = null

    @Inject
    lateinit var prefProvider: PrefProvider

    private fun initOmniDriver() {
        omniDriver = OmniDriver.me(requireContext())
        omniDriver?.init(object : OmniConnection {
            override fun onConnected() {
            }

            override fun onDisconnected(error: Int) {
            }
        })
    }

    lateinit var venueUrlByteArray:ByteArray

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_transaction_details,
            container,
            false
        )

        weakContext = WeakReference<Context>(context)
        lifecycleScope.launch {
            try {
                runBlocking {
                    oneItemPerReceipt =
                        dashboardCategoryViewModel.getLabelPrinterSettingsData().oneItemPerReciept
                }
            } catch (e: Exception) {
                oneItemPerReceipt = false
            }
        }

        lifecycleScope.launch(Dispatchers.Main) {
            try {
                printOrderIDInStickyPrinter =
                    dashboardCategoryViewModel.getLabelPrinterSettingsData().printOrderId
            } catch (e: Exception) {

            }
        }

        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        orderId = arguments?.getInt("orderId")!!
        paymentId = arguments?.getInt("paymentId")!!
        isFromTrans = arguments?.getBoolean("isFromTrans")!!
        isFromOnlineOrderRefund = arguments?.getBoolean("isFromOnlineOrderRefund")!!
//        if (isFromTrans) {
        viewModel.apiCallPaymentDetails(paymentId)
//        } else {
//            viewModel.apiCallOrderDetails(orderId)
//
//        }

        if (prefProvider.getValue(Constants.VENUE_LOGO_URL,"").isNotEmpty()) {
            runBlocking {
                lifecycleScope.async {
                    venueUrlByteArray=LPrint.processImageForPrinting(prefProvider.getValue(Constants.VENUE_LOGO_URL, "")!!, 200,200)!!
                }.await()
            }
        }

        observeTipsList()
        setupSnackbar()
        observeShowProgress()
        setUpRecyclerView()
        navigate()

        getKitchenReceiptSettings()
        getCustomerReceiptSettings()
        orderUpdateTips()
        if (isFromOnlineOrderRefund) {
            acceptedAndDeclineOrder()
        }

        initPOSLink()
        getMerchantDataObserver()

        return binding.root
    }


    private fun setUpRecyclerView() {
        orderDetailsItemAdapter = OrderDetailsItemListAdapter()
        binding.rvOrderItems.adapter = orderDetailsItemAdapter

        taxBirfurcationAdapter = TaxBirfurcationAdapter("transaction")
        binding.rvTax.adapter = taxBirfurcationAdapter
    }

    private fun orderUpdateTips() {
        viewModel.data1.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireContext(), it.message.toString()
                ) { _, _ ->
                    viewModel.apiCallPaymentDetails(paymentId)
                }

            }
        }


    }

    private fun cashLogEventCall(tippedAmount: Double) {
//        if (bundle.containsKey("tipAmount")) {
            if (tippedAmount > 0.0) {
                getTipDetails(tippedAmount,orderId)
//                makeCashEventCallToUpdateTip(bundle.getDouble("tipAmount"))
            }
//        }
    }

    private fun getTipDetails(tippedAmount: Double, orderId: Int?){
        CoroutineScope(Dispatchers.Main).launch {
          transactionViewModel.getCashEventDetails(tippedAmount, orderId?:-1,paymentId?:-1,"in",0)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var selectedorderType = arguments?.getInt("selectedorderType")
        var selectedtransactionType = arguments?.getInt("selectedtransactionType")
        var selectedroleType = arguments?.getInt("selectedroleType")
        var selectedemployeeType = arguments?.getInt("selectedemployeeType")
        var selectedterminalType = arguments?.getInt("selectedterminalType")

        initOmniDriver()

        sunmiFrameworkVersion =
            prefProvider?.getValue(Constants.SUNMI_FRAMEWORK_VERSION, "").toString().split(".")
                .toTypedArray()

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true /* enabled by default */) {
                override fun handleOnBackPressed() {
                    var bundle: Bundle = Bundle()
                    bundle.putInt("selectedorderType", selectedorderType!!)
                    bundle.putInt("selectedtransactionType", selectedtransactionType!!)
                    bundle.putInt("selectedroleType", selectedroleType!!)
                    bundle.putInt("selectedemployeeType", selectedemployeeType!!)
                    bundle.putInt("selectedterminalType", selectedterminalType!!)
                    sendBackData(bundle)

                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        binding.imgBack.setOnClickListener {
            var bundle: Bundle = Bundle()
            bundle.putInt("selectedorderType", selectedorderType!!)
            bundle.putInt("selectedtransactionType", selectedtransactionType!!)
            bundle.putInt("selectedroleType", selectedroleType!!)
            bundle.putInt("selectedemployeeType", selectedemployeeType!!)
            bundle.putInt("selectedterminalType", selectedterminalType!!)
            sendBackData(bundle)
        }

        binding.txtHome.setOnClickListener {
            val navControll = findNavController()
            navControll.navigate(R.id.action_transactionDetailsFragment_to_dashboardboldpos)
        }

        binding.txtPrintReceipt.setOnClickListener {
            isPrintCustomer = true
            getCustomerPrinters()

        }
        binding.txtPrintKitchenReceipt.setOnClickListener {
            isPrint = true
            getKitchenPrinters()
        }
        binding.linearTaxDetail.setOnClickListener {
            if (taxBirfurcationAdapter.taxlist.size > 0) {
                if (!taxClickable) {
                    Log.d(TAG, "onViewCreated: " + taxBirfurcationAdapter.taxlist.size)
                    taxClickable = true
                    binding.imgDropdown.setImageResource(R.drawable.ic_solid_up_arrow)
                    binding.relativeDynamicTax.visible()
                } else {
                    taxClickable = false
                    binding.imgDropdown.setImageResource(R.drawable.ic_arrow_drop_down)
                    binding.relativeDynamicTax.gone()
                }
            }

        }

        binding.tvtipadd.setOnClickListener {

            if (paymentDetailsResponse.data.payment_type == "External") {
                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireContext(),
                    "Tip cannot be adjusted for this transaction."
                ) { _, _ ->
                }
            } else {
                if (this::paymentDetailsResponse.isInitialized) {
                    if (!paymentDetailsResponse.data.payable_type.equals(
                            "GiftCard",
                            true
                        ) && !paymentDetailsResponse.data.payable_type.equals(
                            "Invoice", true
                        )
                    ) {
                        val bundle = Bundle()
                        bundle.putDouble("totalTip", paymentDetailsResponse.data.tips)
                        bundle.putBoolean("isFromTransaction", true)
                        paymentDetailsResponse.data.amount.let { bundle.putDouble("totalPrice", it) }
                        findNavController().navigate(
                            R.id.action_transactionDetailsFragment_to_tipdialog,
                            bundle
                        )
                    }
                }

            }
        }
        setFragmentResultListener("request_key_tips") { requestKey: String, bundle: Bundle ->
            tipAmount = bundle.getDouble("tipAmount")
            if (tipAmount > 0) {
                val percentageTip = MethodUtils.calculatePercentageFromAmount(
                    tipAmount,
                    paymentDetailsResponse.data.amount
                )
                binding.tvTipLabel.text = "Tip (${
                    String.format(
                        if (percentageTip > 1) "%.0f" else "%.2f",
                        percentageTip
                    )
                }%)"
            } else {
                binding.tvTipLabel.text = "Tip (0%)"
            }

            if (paymentDetailsResponse.data?.payment_type == "Card") {
                Log.d("RefNum11: ", "RefNum ${paymentDetailsResponse.data?.ref_num}")
                when(prefProvider.getValue(Constants.PAYMENT_GATEWAY_TYPE,"")){
                    Constants.PAX->{
                        if (!paymentDetailsResponse.data?.ref_num.isNullOrEmpty() && prefProvider.getValueboolean(
                                Constants.IS_PAX_CONNECTED,
                                false
                            )
                        ) {
                            adjustPaxTips()
                        }else{
                            AlertUtils.showCustomAlert(
                                requireContext(),
                                "Please connect to PAX device"
                            )
                        }
                    }
                    Constants.VALOR, Constants.VELOR->{
                        adjustValorTips(paymentDetailsResponse)
                    }
                    Constants.DEJAVOO->{
                        adjustDejavooTips()
                    }else->{
                    if (paymentDetailsResponse.data?.ref_num.isNullOrEmpty()) {
                        magtekCall(tipAmount)
                    }else{
                        if (!paymentDetailsResponse.data?.ref_num.isNullOrEmpty() && !prefProvider.getValueboolean(
                                Constants.IS_PAX_CONNECTED,
                                false
                            )
                        ) {
                            AlertUtils.showCustomAlert(
                                requireContext(),
                                "Please connect to PAX device"
                            )
                        }
                    }

                }

                }
               /* if (paymentDetailsResponse.data?.ref_num.isNullOrEmpty()) {
                    magtekCall(tipAmount)
                } else if (!paymentDetailsResponse.data?.ref_num.isNullOrEmpty() && prefProvider.getValueboolean(
                        Constants.IS_PAX_CONNECTED,
                        false
                    )
                ) {
                    adjustPaxTips()
                } else if (prefProvider.getValue(Constants.VALOR_APP_KEY, "").isNotEmpty()) {
                    adjustValorTips(paymentDetailsResponse)
                } else if (!paymentDetailsResponse.data?.ref_num.isNullOrEmpty() && !prefProvider.getValueboolean(
                        Constants.IS_PAX_CONNECTED,
                        false
                    )
                ) {
                    AlertUtils.showCustomAlert(
                        requireContext(),
                        "Please connect to PAX device"
                    )
                }*/
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    launch {
                        tipCall(false)
                    }
                    launch {
                        cashLogEventCall(tipAmount)
                    }
                    launch {
                        openCashDrawer()
                    }
                }
            }
        }
        binding.tvIssueRefund.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {

                if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                    return
                }
                mLastClickTime = SystemClock.elapsedRealtime()
                try {

                    if (!paymentDetailsResponse.data.ext_data.isNullOrEmpty()) {
                        if (((paymentDetailsResponse.data.ext_data.equals(Constants.VALOR)) || (paymentDetailsResponse.data.ext_data.equals(Constants.VELOR))) && prefProvider.getValue(
                                Constants.VALOR_APP_ID, ""
                            ).isNotEmpty()
                        ) {
                            weakContext.get()?.let {
                                AlertUtils.showCustomAlertWithListenerWithOKCancel(it,
                                    getString(R.string.proceed_with_refund_void),
                                    getString(android.R.string.ok),
                                    object : DialogInterface.OnClickListener {
                                        override fun onClick(p0: DialogInterface?, p1: Int) {
                                            ProgressUtils.showProgressDialog(requireActivity())
                                            checkIfValorTransactionEligibleForVoid(
                                                paymentDetailsResponse
                                            )
                                            p0?.dismiss()
                                        }
                                    })
                            }


                        } else if (paymentDetailsResponse.data.ext_data.contains(Constants.DEJAVOO)) {
                            weakContext.get()?.let {
                                AlertUtils.showCustomAlertWithListenerWithOKCancel(it,
                                    getString(R.string.proceed_with_refund_void),
                                    getString(android.R.string.ok),
                                    object : DialogInterface.OnClickListener {
                                        override fun onClick(p0: DialogInterface?, p1: Int) {
                                            ProgressUtils.showProgressDialog(requireActivity())
                                            checkIfDejavooTransactionEligibleForVoid(paymentDetailsResponse)
                                            p0?.dismiss()
                                        }
                                    })
                            }

                        }
//                    Check if the the PAX is connected or not then perform the void checking
                        else if (prefProvider.getValueboolean(Constants.IS_PAX_CONNECTED, false)) {
//                    Check if the transaction is void or not
                            CoroutineScope(Dispatchers.Main).launch {
                                ProgressUtils.showProgressDialog(requireActivity())
                            }
                            checkIfTransactionIsVoided()
                        } else {
                            ProgressUtils.dismissProgressDialog()
                            activity?.let {
                                AlertUtils.showCustomAlertWithListenerWithOK(
                                    it,
                                    getString(R.string.pax_connect_error),
                                    null
                                )
                            }
                        }

                    } else {
                        startRefund()
                    }
                } catch (e: Exception) {
                    activity?.let {
                        Toast.makeText(it, "Try after sometime", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })


        binding.txtTextReceipt.setOnClickListener {
            // 1 : text
            openReceiptDialog(1)
        }

        binding.txtEmailReceipt.setOnClickListener {
            // 1 : email
            openReceiptDialog(2)

        }

    }

    private fun openCashDrawer() {
        if (android.os.Build.BRAND.contains("Landi", ignoreCase = true)) {
            EventBus.getDefault().post(CashBoxEvent(Constants.CASHBOX))
        } else {
            SunmiPrintHelper.getInstance().openCashBox()
        }
    }

    private fun adjustDejavooTips() {
        paymentCoroutineScope = CoroutineScope(Dispatchers.IO + paymentCoroutineExceptionHandler)
        paymentCoroutineScope.launch {
            val gatewayType = PaymentGatewayType.DEJAVOO
            val paymentGateway = PaymentGatewayFactory(
                ValorPaymentGateway(),
                DejavooPaymentGateway()
            ).create(gatewayType)

            paymentDetailsResponse?.data.ref_num.let { dejavooRefTxnId ->
                var dejavoo= Dejavoo(
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
                    transType="TipAdjust",
                    amount= paymentDetailsResponse?.data.amount.toString(),
                    tip = tipAmount.toString(),
                    refId= dejavooRefTxnId,
                    printReceipt= false,
                    performedBy=  prefProvider.employeeName(),
                    isProd=  Constants.paymentLive,
                    txnType = TransactionType.TIP_ADJUSTMENT
                )
                paymentGateway.processPayment(
                    requireContext().applicationContext,
                    dejavoo,
                    onSuccess = { tResponse->
                        var transactionJsonResponse = Gson().fromJson<String>(
                            tResponse,
                            String::class.java
                        )
                        tipCall(true)

                    },
                    onFailure = {
                        ProgressUtils.dismissProgressDialog()

                        /*runOnUiThread(Runnable {
                                    AlertUtils.showCustomAlert(
                                        requireContext(),
                                        errorMessage
                                    )
                                })*/

                    }
                )
                /* Process Tip Adjust */
            }
        }
    }

    private fun checkIfValorTransactionEligibleForVoid(paymentDetailsResponse: GetPaymentOrderDetailsResponse) {
        paymentCoroutineScope.launch {
            val gatewayType = PaymentGatewayType.VALOR
            val paymentGateway = paymentGatewayFactory.create(gatewayType)


            /*      var apiKey = "k3FhfL$$8vu#NEDlfuJwP62MzIeA7Csz"
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
            context?.let {
                var valor = Valor(
                    apiKey = prefProvider.getValue(Constants.VALOR_APP_KEY, ""),
                    appID = prefProvider.getValue(Constants.VALOR_APP_ID, ""),
                    epi = prefProvider.getValue(Constants.VALOR_EPI, ""),
                    endpoint = Constants.VALOR_OPEN_BATCH,
                    channelId = prefProvider.getValue(Constants.VALOR_CHANNEL_ID, ""),
                    limit = 200,
                    offset = 0,
                    isProd = Constants.paymentLive
                )

                paymentGateway.getTransactionsDetails(
                    it,
                    valor,
                    onSuccess = { tResponse ->
                        var transactionJsonResponse = Gson().fromJson<ValorTransactionsList>(
                            tResponse,
                            ValorTransactionsList::class.java
                        )

                        transactionJsonResponse?.nameValuePairs?.let {
                            if (it.batchSummaryDetails.values.isNotEmpty()) {
                                var transaction = it.batchSummaryDetails.values.filter {
                                    it.nameValuePairs.txnId.toString()
                                        .equals(paymentDetailsResponse.data.ref_num)
                                }
                                if (transaction.isEmpty()) {
                                    //Refund
                                    startRefund()
                                } else {
                                    //Void
                                    startVoidWithValor(paymentDetailsResponse)
                                }
                            } else {
                                startRefund()
//                              dismissProgressDialogWithAlert()
                            }
                        }
                    },
                    onFailure = { errorMessage ->
                        Log.e("Valor:", errorMessage)
                        dismissProgressDialogWithAlert(errorMessage)
                    }
                )
            }
//            }
        }
    }

    lateinit var paymentCoroutineScope: CoroutineScope
    var paymentCoroutineExceptionHandler =
        CoroutineExceptionHandler { coroutineContext, exception ->
            dismissProgressDialogWithAlert()
            EventBus.getDefault()
                .post(
                    MessageEvent(
                        "${Constants.LINE_BREAK_TAB} TransactionDetailsFragment startVoidWithValor()-> ${
                            Gson().toJson(
                                exception
                            )
                        } "
                    )
                )
        }

    private fun adjustValorTips(paymentDetailsResponse: GetPaymentOrderDetailsResponse) {

        GlobalScope.launch {
            val tip_amt = (tipAmount * 100).toInt()

            withContext(Dispatchers.Main) {
                ProgressUtils.showProgressDialog(requireActivity())
            }

            val gatewayType = PaymentGatewayType.VALOR
            val paymentGateway = paymentGatewayFactory.create(gatewayType)

            paymentDetailsResponse.data?.ref_num.let { valorRefTxId ->
                context?.let {
                    var valor = Valor(
                        apiKey = prefProvider.getValue(Constants.VALOR_APP_KEY, ""),
                        appID = prefProvider.getValue(Constants.VALOR_APP_ID, ""),
                        epi = prefProvider.getValue(Constants.VALOR_EPI, ""),
                        endpoint = Constants.VALOR_TIP_ADJUST,
                        txnType = TransactionType.TIP_ADJUSTMENT,
                        channelId = prefProvider.getValue(Constants.VALOR_CHANNEL_ID, ""),
                        transMode = "",
                        transCode = "",
                        reqTxnId = valorRefTxId.toString(),
                        amount = "",
                        tipAmount = tipAmount.toString(),
                        tipEntry = "1",
                        txn_type = "",
                        surchargeIndicator = "",
                        sale_refund = "",
                        ref_txn_id = "",
                        isProd = Constants.paymentLive,
                        transactionId = ""
                    )

                    paymentGateway.processPayment(
                        context = it.applicationContext,
                        valor,
                        onSuccess = { tResponse ->
                            var transactionJsonResponse = Gson().fromJson<ValorSuccessResponse>(
                                tResponse,
                                ValorSuccessResponse::class.java
                            )
                            transactionJsonResponse.nameValuePairs?.let {
                                if (it.msg != null) {
                                    if (it.msg!!.contains(
                                            "APPROVED"
                                        )
                                    ) {
                                        tipCall(true)
                                    } else {
                                        dismissProgressDialogWithAlert()
                                    }
                                }
                            }
                        },
                        onFailure = { errorMessage ->
                            Log.e("Valor: ", errorMessage)
                            dismissProgressDialogWithAlert(errorMessage)
                        },
                    )
                }
            }
            /* Process Tip Adjust */

        }
    }

    fun parseXml(xmlContent: String): Document {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        return builder.parse(xmlContent.byteInputStream())
    }


    private fun checkIfDejavooTransactionEligibleForVoid(paymentDetailsResponse: GetPaymentOrderDetailsResponse) {
        paymentCoroutineScope = CoroutineScope(Dispatchers.IO + paymentCoroutineExceptionHandler)
        paymentCoroutineScope.launch {
            val gatewayType = PaymentGatewayType.DEJAVOO
            val paymentGateway = paymentGatewayFactory.create(gatewayType)

            /*  context?.let {
                  var dejavoo = Dejavoo(
                      authKey = "kwg2GRbykg",
                      registerId = "4986101",
                      tpn = "659324491704",
                      amount = paymentDetailsResponse.data.amount.toString(),
                      isProd = false,
                      paymentType = "Credit",
                      performedBy = "",
                      printReceipt = false,
                      refId = paymentDetailsResponse.data.ref_num,
                      tip = "",
                      transType = *//*"Return"*//*"Void",
                    txnType = TransactionType.VOID)

                paymentGateway.voidPayment(
                    it,
                    dejavoo,
                    paymentCallback
                )
            }*/


//            ---------------------------------------------- To Fetch Transaction Details--------------
            context?.let {
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
                    amount = "",
                    isProd = Constants.paymentLive,
                    paymentType = "Credit",
                    performedBy = "",
                    printReceipt = false,
                    refId = paymentDetailsResponse.data.ref_num,
                    tip = "",
                    transType = "Status",
                    txnType = TransactionType.VOID
                )

                paymentGateway.voidPayment(
                    it,
                    dejavoo,
                    onSuccess = { tRequest ->
                        var transactionJsonResponse = Gson().fromJson<String>(
                            tRequest,
                            String::class.java
                        )
                        val factory: XmlPullParserFactory = XmlPullParserFactory.newInstance()
                        factory.setNamespaceAware(true)
                        val xpp: XmlPullParser = factory.newPullParser()
                        xpp.setInput(StringReader(transactionJsonResponse))
                        var eventType = xpp.eventType

                        var Message = ""
                        var ResultCode = ""
                        var RespMSG = ""
                        with(parseXml(transactionJsonResponse).childNodes.item(0).childNodes.item(0).childNodes) {
                            for (i in 0 until this.length) {

                                when ((this.item(i) as Element).tagName.toString()) {
                                    "Message" -> Message =
                                        this.item(i).childNodes.item(0).nodeValue ?: ""
                                    "ResultCode" -> ResultCode =
                                        this.item(i).childNodes.item(0).nodeValue ?: ""
                                    "RespMSG" -> RespMSG =
                                        this.item(i).childNodes.item(0).nodeValue ?: ""
                                    else -> {

                                    }
                                }
                            }
                        }
//                    parseXml(transactionJsonResponse).childNodes.item(0).childNodes.item(0).childNodes
                        if (Message.equals("Canceled") || Message.equals("Error")) {
                            ProgressUtils.dismissProgressDialog()
                            AlertUtils.showCustomAlert(
                                requireContext(),
                                RespMSG.replace("%20", " ")
                            )
                        } else if (Message.contains(
                                "approved",
                                ignoreCase = true
                            )
                        ) { // Found the transaction, proceed with VOID
                            CoroutineScope(Dispatchers.Main).launch {
                                startVoidWithDejavoo()
                            }
                        } else if (Message.contains("Not found", ignoreCase = true)) {
                            startRefund()
                        }
                    },
                    onFailure = { errorMessage ->
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
                        ProgressUtils.dismissProgressDialog()

                    }
                )
            }
//            }
        }
    }

    private fun startVoidWithDejavoo() {
        paymentCoroutineScope = CoroutineScope(Dispatchers.IO + paymentCoroutineExceptionHandler)
        paymentCoroutineScope.launch {
            val gatewayType = PaymentGatewayType.DEJAVOO
            val paymentGateway = paymentGatewayFactory.create(gatewayType)

            context?.let {
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
                    amount = paymentDetailsResponse.data.amount.toString(),
                    isProd = Constants.paymentLive,
                    paymentType = "Credit",
                    performedBy = "",
                    printReceipt = false,
                    refId = paymentDetailsResponse.data.ref_num,
                    tip = "",
                    transType = "Void",
                    txnType = TransactionType.VOID
                )

                paymentGateway.voidPayment(
                    it,
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
                                        this.item(i).childNodes.item(0).nodeValue ?: ""
                                    "RefId" -> RefId =
                                        this.item(i).childNodes.item(0).nodeValue ?: ""
                                    "RegisterId" -> RegisterId =
                                        this.item(i).childNodes.item(0).nodeValue ?: ""
                                    "TPN" -> TPN = this.item(i).childNodes.item(0).nodeValue ?: ""
                                    "AuthCode" -> AuthCode =
                                        this.item(i).childNodes.item(0).nodeValue ?: ""
                                    "PNRef" -> PNRef =
                                        this.item(i).childNodes.item(0).nodeValue ?: ""
                                    "TransNum" -> TransNum =
                                        this.item(i).childNodes.item(0).nodeValue ?: ""
                                    "ResultCode" -> ResultCode =
                                        this.item(i).childNodes.item(0).nodeValue ?: ""
                                    "RespMSG" -> RespMSG =
                                        this.item(i).childNodes.item(0).nodeValue ?: ""
                                    "PaymentType" -> PaymentType =
                                        this.item(i).childNodes.item(0).nodeValue ?: ""
                                    "Voided" -> Voided =
                                        this.item(i).childNodes.item(0).nodeValue ?: ""
                                    "TransType" -> TransType =
                                        this.item(i).childNodes.item(0).nodeValue ?: ""
                                    "SN" -> SN = this.item(i).childNodes.item(0).nodeValue ?: ""
                                    "ExtData" -> ExtData =
                                        this.item(i).childNodes.item(0).nodeValue ?: ""
                                    else -> {

                                    }
                                }
                            }
                        }
//                    parseXml(transactionJsonResponse).childNodes.item(0).childNodes.item(0).childNodes
                        if (Message.equals("Canceled") || Message.equals("Error")) {
                            ProgressUtils.dismissProgressDialog()
                            AlertUtils.showCustomAlert(
                                requireContext(),
                                RespMSG.replace("%20", " ")
                            )
                        } else if (ResultCode.equals("0")) { // Found the transaction, proceed with VOID
                            CoroutineScope(Dispatchers.Main).launch {
                                var refundAmount:Double = paymentDetailsResponse.data.amount
                                if (paymentDetailsResponse.data.tips>0.0){
                                    refundAmount+=paymentDetailsResponse.data.tips
                                }
                                refundCall(refundAmount)
                            }
                        } else if (ResultCode.equals("0")) {
                            startRefund()
                        }
                    },
                    onFailure = { errorMessage ->
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
                        ProgressUtils.dismissProgressDialog()
                    }
                )
            }
//            }
        }
    }

    private fun startVoidWithValor(paymentDetailsResponse: GetPaymentOrderDetailsResponse) {
        paymentCoroutineScope = CoroutineScope(Dispatchers.IO + paymentCoroutineExceptionHandler)
        paymentCoroutineScope.launch {
            val gatewayType = PaymentGatewayType.VALOR
            val paymentGateway = paymentGatewayFactory.create(gatewayType)


            /*      var apiKey = "k3FhfL$$8vu#NEDlfuJwP62MzIeA7Csz"
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
            context?.let {
                var valor = Valor(
                    apiKey = prefProvider.getValue(Constants.VALOR_APP_KEY, ""),
                    appID = prefProvider.getValue(Constants.VALOR_APP_ID, ""),
                    epi = prefProvider.getValue(Constants.VALOR_EPI, ""),
                    endpoint = Constants.VALOR_VOID,
                    txnType = TransactionType.TIP_ADJUSTMENT,
                    channelId = prefProvider.getValue(Constants.VALOR_CHANNEL_ID, ""),
                    amount = paymentDetailsResponse.data.amount.toString(),
                    txn_type = Constants.VALOR_VOID,
                    ref_txn_id = paymentDetailsResponse.data.ref_num,
                    surchargeIndicator = "",
                    sale_refund = "",
                    isProd = Constants.paymentLive,
                    transactionId = ""
                )

                paymentGateway.voidPayment(
                    it,
                    valor,
                    onSuccess = { tResponse ->
                        var transactionJsonResponse = Gson().fromJson<ValorSuccessResponse>(
                            tResponse,
                            ValorSuccessResponse::class.java
                        )

                        transactionJsonResponse.nameValuePairs?.let {
                            if (it.msg != null) {
                                if (it.msg.equals("APPROVED", ignoreCase = true)) {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        refundCall(paymentDetailsResponse.data.amount)
                                    }
                                } else {
                                    startRefund()
                                }
                            } else {
                                dismissProgressDialogWithAlert()
                            }
                        }
                    },
                    onFailure = { errorMessage ->
                        Log.e("Valor:", errorMessage)
                        dismissProgressDialogWithAlert(errorMessage)
                    }
                )
            }
//            }
        }
    }

    private fun dismissProgressDialogWithAlert(errorMessage: String? = null) {
        runOnUiThread {
            ProgressUtils.dismissProgressDialog()
            if (errorMessage != null) {
                AlertUtils.showCustomAlert(requireContext(), errorMessage)
            }
        }
    }


    private fun startRefund() {
        CoroutineScope(Dispatchers.Main).launch {
            if (paymentDetailsResponse.data.order.order_type.equals("OnlineWebOrder")) {
                lateinit var refundData: RefundRequestModelOnlineOrder
                var employeeIdtemp = prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
                var terminal_id = prefProvider.getValueInt(Constants.TERMINAL_ID, 0)
                var orderItemRefundsAttributesList =
                    ArrayList<RefundRequestModelOnlineOrder.PaymentRefund.OrderItemRefundsAttribute>()
                paymentDetailsResponse.data.order.order_items.forEach { item ->
                    val orderItemRefundsAttributeModel =
                        RefundRequestModelOnlineOrder.PaymentRefund.OrderItemRefundsAttribute()
                    orderItemRefundsAttributeModel.amount = item.totalPrice
                    orderItemRefundsAttributeModel.employeeId = employeeIdtemp
                    orderItemRefundsAttributeModel.orderId = item.orderId
                    orderItemRefundsAttributeModel.refundType = 0
                    orderItemRefundsAttributeModel.paymentId =
                        paymentDetailsResponse.data.id
                    orderItemRefundsAttributeModel.orderItemId = item.id
                    orderItemRefundsAttributeModel.quantity = item.quantity
                    orderItemRefundsAttributesList.add(orderItemRefundsAttributeModel)
                }

                refundData = RefundRequestModelOnlineOrder().apply {
                    paymentRefund = RefundRequestModelOnlineOrder.PaymentRefund().apply {
                        amount =
                            paymentDetailsResponse.data.amount + paymentDetailsResponse.data.tips
                        refunded_amount = amount
                        orderId = paymentDetailsResponse.data.order_id
                        paymentId = paymentDetailsResponse.data.id
                        employeeId = employeeIdtemp
                        taxRefunded = paymentDetailsResponse.data.tax_amount
                        tipsRefunded = paymentDetailsResponse.data.tips
                        terminalId = terminal_id
                        serviceChargeRefunded =
                            paymentDetailsResponse.data.service_charge_amount
                        cash_discount_or_surcharge_refunded =
                            paymentDetailsResponse.data.cash_discount_or_surcharge
                        subtotal_refunded = paymentDetailsResponse.data.sub_total
                        orderItemRefundsAttributes = orderItemRefundsAttributesList
                    }
                }


                val bundle = Bundle().apply {

                    if (paymentDetailsResponse.data.pax_data != null) {
                        /*Online order refund should pass a new parameter so that the next screen will detect the parameter and process the operation accordingly, because there are two processes
                        * 1. PAX Gateway refund
                        * 2. NAB Server POST API Call */
                        putString("pax_data", paymentDetailsResponse.data.pax_data)
                        putBoolean("requiredNABServerPostAPICall", true)
                    }

                    putParcelable("refundData", refundData)
                    putDouble(
                        "refundAmount",
                        paymentDetailsResponse.data.amount + paymentDetailsResponse.data.tips
                    )
                    putString("paymentType", paymentDetailsResponse.data.payment_type)
                    putBoolean("isfromTransaction", true)
                    putString(
                        "magensa_response_data",
                        paymentDetailsResponse.data.magensa_response_data
                    )
                    putInt("guestCount", paymentDetailsResponse.data.guestCount ?: 0)

                }

                ProgressUtils.dismissProgressDialog()

                bundle.putString("isFrom", "refundOnline")
                if (prefProvider.isManager() || prefProvider.isAdmin()) {
                    findNavController().navigate(
                        R.id.action_transaction_to_reasonForrefundonline,
                        bundle
                    )

                } else {
                    findNavController().navigate(
                        R.id.action_transactionDetailsFragment_to_pascodeManagerDailog,
                        bundle
                    )
                }

            } else {

                var isItemRefund = false
                var isAmountRefund = false

                paymentDetailsResponse.data.order.order_items.forEach {
                    if (it.refundedAmount != 0.0)
                        isItemRefund = true
                }

                if (!isItemRefund) {
                    if (paymentDetailsResponse.data.order.refund_detail.refunded_amount != 0.0) {
                        isAmountRefund = true
                    }
                }

                val bundle = Bundle().apply {
                    paymentDetailsResponse.data.order.order_items.forEach {
                        it.isChecked = true
                    }
                    putInt("paymentId", paymentId)
                    putParcelable("orderDetailsResponse", paymentDetailsResponse)
                    putBoolean("isSplitPayment", isSplitPayment)
                    if (paymentDetailsResponse.data.order.order_type == "OnlineOrder" && paymentDetailsResponse.data.pax_data != null) {
                        /*Online order refund should pass a new parameter so that the next screen will detect the parameter and process the operation accordingly, because there are two processes
                        * 1. PAX Gateway refund
                        * 2. NAB Server POST API Call */
                        putString("pax_data", paymentDetailsResponse.data.pax_data)
                        putBoolean("requiredNABServerPostAPICall", true)
                    }
                    putParcelableArrayList("serviceChargesList", serviceChargesList)
                    putInt("guestCount", paymentDetailsResponse.data.guestCount ?: 0)

                    putBoolean("isItemRefund", isItemRefund)
                    putBoolean("isAmountRefund", isAmountRefund)
                    putString(
                        "screenTotalAmount",
                        (paymentDetailsResponse.data.amount + paymentDetailsResponse.data.tips).toString()
                    )
                }
                bundle.putString("isFrom", "refund")

                ProgressUtils.dismissProgressDialog()

                if (prefProvider.isManager() || prefProvider.isAdmin()) {
                    findNavController().navigate(
                        R.id.action_transactionDetailsFragment_to_issueRefundFragment,
                        bundle
                    )
                } else {
                    findNavController().navigate(
                        R.id.action_transactionDetailsFragment_to_pascodeManagerDailog,
                        bundle
                    )
                }

            }
        }
    }

    private fun checkIfTransactionIsVoided() {
        GlobalScope.launch {
            posLink.SetCommSetting(
                SettingINI.getCommSettingFromFile(
                    context!!,
                    Constants.FILE_PATH + SettingINI.FILENAME
                )
            )

            val report = ReportRequest()
            report.TransType = report.ParseTransType("LOCALDETAILREPORT") //recommend
            report.EDCType = report.ParseEDCType("CREDIT")
            report.RefNum = paymentDetailsResponse.data.ref_num
            report.ECRRefNum = paymentDetailsResponse.data.ecr_ref_num

            posLink.ReportRequest = report
            val result = posLink.ProcessTrans()
            Log.d("result batch: ", result.Code.toString() + " " + result.Msg)
            try {
                if (result.Code === ProcessTransResult.ProcessTransResultCode.OK) {
                    val msg = Message()
                    msg.what = Constants.TRANSACTION_SUCCESSED
                    msg.obj = posLink.ReportResponse
                    if (posLink.ReportResponse == null) {
                        CoroutineScope(Dispatchers.Main).launch {
                            ProgressUtils.dismissProgressDialog()
                        }
                        showConfirmationAlertDialog()
                        Log.d("Data::", "void transaction")
                    } else {
                        val response = msg.obj as com.pax.poslink.ReportResponse
                        val resultCode = response.ResultCode
                        val resultTxt = response.ResultTxt
                        if (resultCode == "000000") {
                            CoroutineScope(Dispatchers.Main).launch {
                                ProgressUtils.dismissProgressDialog()
                            }
                            showConfirmationAlertDialog()
                            Log.d("Data::", "void transaction")
                        } else if (resultCode == "100023") {
                            //Transaction not found in current batch
                            //refundViaPAX()
                            startRefund()
                        } else {
                            CoroutineScope(Dispatchers.Main).launch {
                                ProgressUtils.dismissProgressDialog()
                                AlertUtils.showCustomAlertWithListenerWithOK(
                                    requireContext(),
                                    resultTxt,
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

                        Log.d(
                            "Params:",
                            "Report $resultCode $resultTxt ${response.ExtData}  ${
                                Gson().toJson(
                                    response
                                )
                            }"
                        )
                    }
                }
            } catch (e: Exception) {
//                posLink.s
            }
        }
    }

    private fun enableDisableTipButton() {
        try {
            when (paymentDetailsResponse.data.ext_data) {
                "VALOR" -> {
                    paymentCoroutineScope =
                        CoroutineScope(Dispatchers.IO + paymentCoroutineExceptionHandler)
                    paymentCoroutineScope.launch {
                        val gatewayType = PaymentGatewayType.VALOR
                        val paymentGateway = paymentGatewayFactory.create(gatewayType)
                        /* Process Payment */
                        context?.let {
                            var valor = Valor(
                                apiKey = prefProvider.getValue(Constants.VALOR_APP_KEY, ""),
                                appID = prefProvider.getValue(Constants.VALOR_APP_ID, ""),
                                epi = prefProvider.getValue(Constants.VALOR_EPI, ""),
                                endpoint = Constants.VALOR_TXN_FETCH,
                                channelId = prefProvider.getValue(Constants.VALOR_CHANNEL_ID, ""),
                                ref_txn_id = paymentDetailsResponse.data.ref_num,
                                limit = 200,
                                offset = 0,
                                isProd = Constants.paymentLive
                            )

                            (paymentGateway as ValorPaymentGateway).getTransactionsList(
                                it,
                                valor,
                                onSuccess = { tResponse ->
                                    Log.d("Resp: ",tResponse)

                                    try{
                                        var tipAmount= JSONObject(JSONObject(JSONArray(JSONObject(JSONObject(JSONObject(tResponse).get("nameValuePairs").toString()).get("data").toString()).get("values").toString()).get(0).toString()).get("nameValuePairs").toString()).get("TIP_AMOUNT")
                                        if (!tipAmount.equals("0")) {
                                            runOnUiThread(Runnable {
                                                binding.tvtipadd.visibility = View.GONE
                                            })
                                        }
                                    }catch (e:Exception){

                                    }
                                },
                                onFailure = { errorMessage ->
                                    Log.e("Valor:", errorMessage)
                                    dismissProgressDialogWithAlert(errorMessage)
                                }
                            )
                        }
//            }
                    }
                }
                else -> {
                    paymentDetailsResponse.data.ref_num?.let {
                        if (it.isNotEmpty()) {
                            GlobalScope.launch {
                                posLink.SetCommSetting(
                                    SettingINI.getCommSettingFromFile(
                                        context!!,
                                        Constants.FILE_PATH + SettingINI.FILENAME
                                    )
                                )

                                val report = ReportRequest()
                                report.TransType =
                                    report.ParseTransType("LOCALDETAILREPORT") //recommend
                                report.EDCType = report.ParseEDCType("CREDIT")
                                report.RefNum = paymentDetailsResponse.data.ref_num
                                report.ECRRefNum = paymentDetailsResponse.data.ecr_ref_num

                                posLink.ReportRequest = report
                                val result = posLink.ProcessTrans()
                                Log.d("result batch: ", result.Code.toString() + " " + result.Msg)
                                try {
                                    if (result.Code === ProcessTransResult.ProcessTransResultCode.OK) {
                                        val msg = Message()
                                        msg.what = Constants.TRANSACTION_SUCCESSED
                                        msg.obj = posLink.ReportResponse
                                        if (posLink.ReportResponse == null) {
                                            CoroutineScope(Dispatchers.Main).launch {
                                                binding.tvtipadd.visibility = View.GONE
                                            }
                                            Log.d("Data::", "void transaction")
                                        } else {
                                            val response = msg.obj as com.pax.poslink.ReportResponse
                                            val resultCode = response.ResultCode
                                            val resultTxt = response.ResultTxt
                                            if (resultCode == "000000") {
                                                CoroutineScope(Dispatchers.Main).launch {
                                                    binding.tvtipadd.visibility = View.VISIBLE
                                                }

                                            } else if (resultCode == "100023") {
                                                CoroutineScope(Dispatchers.Main).launch {
                                                    binding.tvtipadd.visibility = View.GONE
                                                }
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    //                posLink.s
                                }
                            }
                        }
                    }
                }
            }

        } catch (e: Exception) {

        }
    }

    private fun showConfirmationAlertDialog() {
        CoroutineScope(Dispatchers.Main).launch {
            AlertUtils.showCustomAlertWithListenerWithOKCancel(
                requireContext(),
                getString(R.string.single_void_message_1),
                getString(android.R.string.ok)
            ) { _, _ ->
                voidViaPAX()
            }
        }
    }

    private fun voidViaPAX() {
        var refundAmount = paymentDetailsResponse.data.amount
        paymentDetailsResponse.data.tips?.let {
            refundAmount += it
        }

        if (refundAmount != 0.0) {
            if (paymentDetailsResponse.data.payment_type.equals("Card", ignoreCase = true)) {
                GlobalScope.launch {
                    posLink.SetCommSetting(
                        SettingINI.getCommSettingFromFile(
                            context!!,
                            Constants.FILE_PATH + SettingINI.FILENAME
                        )
                    )

                    CoroutineScope(Dispatchers.Main).launch {
                        ProgressUtils.showProgressDialog(requireActivity())
                    }
                    val amt = (refundAmount * 100).toInt()
                    Log.d("amt: ", "amtxx $amt")
                    val refund = PaymentRequest()
                    refund.TenderType = refund.ParseTenderType("CREDIT")
                    refund.TransType = refund.ParseTransType("VOID")
                    refund.ECRRefNum = System.currentTimeMillis().toString()
                    refund.OrigRefNum = paymentDetailsResponse.data.ref_num

//                    refund.Amount = amt.toString()
                    posLink.PaymentRequest = refund
                    val result = posLink.ProcessTrans()
                    Log.d("result void: ", result.Code.toString() + " " + result.Msg)
                    if (result.Code === ProcessTransResult.ProcessTransResultCode.OK) {
                        val msg = Message()
                        msg.what = Constants.TRANSACTION_SUCCESSED
                        msg.obj = posLink.PaymentResponse

                        val response = msg.obj as com.pax.poslink.PaymentResponse
                        val resultCode = response.ResultCode
                        val resultTxt = response.ResultTxt

                        if (resultCode.equals("000000")) {
                            CoroutineScope(Dispatchers.Main).launch {
                                refundCall(refundAmount)
                            }
                        } else if (resultCode.equals("100021")) {
                        } else {
                            CoroutineScope(Dispatchers.Main).launch {
                                ProgressUtils.dismissProgressDialog()
                                AlertUtils.showCustomAlertWithListenerWithOK(
                                    requireContext(),
                                    resultTxt,
                                    object : DialogInterface.OnClickListener {
                                        override fun onClick(p0: DialogInterface?, p1: Int) {
                                            try {
                                                p0?.dismiss()
                                            } catch (e: Exception) {
                                            }
                                        }
                                    })
//                                requireActivity().toast("$resultCode $resultTxt", Toast.LENGTH_LONG)
                            }
                        }
                    } else {
                        CoroutineScope(Dispatchers.Main).launch {
                            ProgressUtils.dismissProgressDialog()
                            AlertUtils.showCustomAlertWithListenerWithOKCancel(
                                requireContext(),
                                getString(R.string.pax_connect_error),
                                getString(R.string.reconnect),
                            )
                            { _, _ ->
                                // Add connect to PAX logic
                                magtekProViewModel.initPOSLink(requireContext())
                            }
                        }
                    }
                }
            } else {
                refundCall(refundAmount)
            }
        } else {
            AlertUtils.showCustomAlert(requireActivity(), getString(R.string.msg_amount_refund))
        }

    }

    private fun refundCall(refundAmount: Double) {
        val ordersItemList =
            mutableListOf<RefundRequestModel.PaymentRefund.OrderItemRefundsAttribute>()

        orderDetailsItemAdapter.taxList.forEach {
            val order =
                RefundRequestModel.PaymentRefund.OrderItemRefundsAttribute()

            order.orderId = it.orderId
            order.orderItemId = it.id
            order.paymentId = paymentId
            order.amount = it.totalPrice
            order.quantity = it.quantity
            order.refundType = 0
            order.employeeId =
                prefProvider.getValueInt(
                    Constants.EMPLOYEE_ID,
                    0
                )
            ordersItemList.add(order)
        }

//        paymentDetailsResponse.data.order.order_items.forEach {
//            val order =
//                RefundRequestModel.PaymentRefund.OrderItemRefundsAttribute()
//
//            order.orderId = it.orderId
//            order.orderItemId = it.id
//            order.paymentId = paymentId
//            order.amount = it.totalPrice
//            order.quantity = it.quantity
//            order.refundType = 0
//            order.employeeId =
//                prefProvider.getValueInt(
//                    Constants.EMPLOYEE_ID,
//                    0
//                )
//            ordersItemList.add(order)
//
//        }

        var refundData = RefundRequestModel()
        var paymentRefund = RefundRequestModel.PaymentRefund()
        refundData.paymentRefund = paymentRefund


        paymentRefund.amount = paymentDetailsResponse.data.sub_total
        paymentRefund.orderId = paymentDetailsResponse.data.order_id
        paymentRefund.paymentId = paymentId
        paymentRefund.employeeId = paymentDetailsResponse.data.employee_id
        paymentRefund.terminalId = paymentDetailsResponse.data.terminal_id
        paymentRefund.taxRefunded = paymentDetailsResponse.data.tax_amount
        paymentRefund.orderItemRefundsAttributes = ordersItemList
        paymentRefund.tipsRefunded =
            if (paymentDetailsResponse.data.payment_type == "Cash") 0.0 else paymentDetailsResponse.data.tips
        paymentRefund.serviceChargeRefunded =
            paymentDetailsResponse.data.service_charge_amount
        paymentRefund.cash_discount_or_surcharge_refunded =
            paymentDetailsResponse.data.cash_discount_or_surcharge
        paymentRefund.subtotal_refunded = 0.0


        viewModel.refundPaymentApiCall(
            refundAmount,
            refundData,
            "Void Transaction",
            paymentDetailsResponse.data.payment_type
        )
    }

    private fun initPOSLink() {
        POSLinkCreatorWrapper.createSync(
            context!!,
            object : AppThreadPool.FinishInMainThreadCallback<PosLink?> {
                override fun onFinish(result: PosLink?) {
                    posLink = result!!
                    Log.d("initPOSLink: ", "onFinish")
                }
            })
    }

    // get merchant details of PAX device
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
                CoroutineScope(Dispatchers.Main).launch {
                    ProgressUtils.dismissProgressDialog()
                    adjustPaxTips()
//                    AlertUtils.showCustomAlert(requireContext(), "Merchant $mID is connected successfully")
                }
                Log.d("Merchant Details: ", mID + " " + resultCode + "  " + status)
            }
        }
    }

    // Adjust tip on transactions done via PAX
    private fun adjustPaxTips() {
        GlobalScope.launch {
            posLink.SetCommSetting(
                SettingINI.getCommSettingFromFile(
                    context!!,
                    Constants.FILE_PATH + SettingINI.FILENAME
                )
            )
            val tip_amt = (tipAmount * 100).toInt()
            Log.d("Amt: ", "tip $tip_amt RefNo ${paymentDetailsResponse.data?.ref_num}")

            CoroutineScope(Dispatchers.Main).launch {
                ProgressUtils.showProgressDialog(requireActivity())
            }
            mPaymentRequest = PaymentRequest()
            mPaymentRequest.TransType = mPaymentRequest.ParseTransType("ADJUST")
            mPaymentRequest.TenderType = mPaymentRequest.ParseTenderType("CREDIT")
            mPaymentRequest.Amount = tip_amt.toString()
            //Added for TSYS ADJUST issue
            mPaymentRequest.ECRRefNum = paymentDetailsResponse.data?.ref_num
            mPaymentRequest.OrigRefNum = paymentDetailsResponse.data?.ref_num
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
                val ExtData = response.ExtData

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

                if (resultCode == "000000") {
                    CoroutineScope(Dispatchers.Main).launch {
                        ProgressUtils.dismissProgressDialog()
                        coroutineScope {
//                            makePaymentCreditCard()
                            tipCall(true)
                        }
                    }
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        ProgressUtils.dismissProgressDialog()
                        AlertUtils.showCustomAlertWithListenerWithOK(
                            requireContext(),
                            resultTxt,
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

    // Update tip in order
    private fun tipCall(isCard: Boolean) {
        paymentDetailsResponse.data.let { viewModel.orderUpdateTip(it.id, tipAmount, isCard) }
    }

    // to pay using magtek device
    private fun magtekCall(refundAmount: Double) {
        if (paymentDetailsResponse.data.order.order_type == "OnlineWebOrder") {
            val model = Gson().fromJson(
                paymentDetailsResponse.data.magensa_response_data,
                MagtekOnlineOrderRefundResponse::class.java
            )
            val jsonArray: JsonArray?

            when {

                Constants.FIRST_DATA_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    // commented by Mansi for task > Tip to be captured through RAPID CONNECT
                    /*val amount = refundAmount

                    if (model != null) {
                        jsonArray =
                            model.transactionOutput?.token.let { it1 ->
                                amount.times(100).let {
                                    magtekRequestUtils.processTokenFirstData(
                                        it,
                                        it1,
                                        model.customerTransactionID ?: "",
                                        model.transactionOutput.transactionOutputDetails[0].value,
                                        Constants.CAPTURE
                                    )
                                }
                            }

                        networkCall(jsonArray, 0)
                    }*/
                    tipCall(true)
                }

                // not support CAPTURE
                Constants.ELAVON_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    jsonArray =
                        model.transactionOutput.token?.let { it1 ->
                            magtekRequestUtils.processTokenElavon(
                                (refundAmount * 100),
                                it1,
                                model.customerTransactionID ?: "",
                                model.transactionOutput.transactionOutputDetails[0].value

                            )
                        }

                    networkCall(jsonArray, 0)
                }

                Constants.EPX_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    jsonArray = model.transactionOutput?.transactionID.let { it1 ->
                        paymentDetailsResponse.data?.amount?.times(100)?.let {
                            magtekRequestUtils.processReferenceIDEPXForce(
                                it,
                                model.customerTransactionID ?: "", it1, Constants.CAPTURE,
                                (tipAmount * 100).toString()
                            )
                        }
                    }
                    networkCall(jsonArray, 1)
                }

                Constants.VANIT_EXORESS_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    jsonArray = model.transactionOutput?.transactionID?.let { it1 ->
                        magtekRequestUtils.processReferenceIDCapture(
                            (refundAmount * 100),
                            model.customerTransactionID ?: "", it1,
                            model.transactionOutput.authCode,
                            ""
                        )
                    }
                    networkCall(jsonArray, 1)
                }

                Constants.CHASE_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    val amount = paymentDetailsResponse.data.amount.plus(refundAmount)

                    jsonArray = amount.times(100)?.let {
                        magtekRequestUtils.processTokenChase(
                            it,
                            model.transactionOutput?.token ?: "",
                            model.customerTransactionID ?: "",
                            model.transactionOutput?.authCode ?: "",
                            Constants.CAPTURE
                        )
                    }

                    networkCall(jsonArray, 0)
                }

                Constants.HEARTLAND_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    val amount = paymentDetailsResponse.data.amount.plus(refundAmount)

                    jsonArray = model.transactionOutput.transactionID.let { it1 ->
                        amount?.times(100).let {
                            magtekRequestUtils.processReferenceIdHeartlandCapture(
                                it,
                                model.customerTransactionID ?: "",
                                it1,
                                model.transactionOutput.authCode,
                                (tipAmount * 100).toString()
                            )
                        }
                    }
                    networkCall(jsonArray, 1)
                }

                Constants.TSYS_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    jsonArray = model.transactionOutput.transactionID.let { it1 ->
                        paymentDetailsResponse.data.amount.let {
                            magtekRequestUtils.processReferenceIDTSYSCapture(
                                it,
                                model.customerTransactionID ?: "",
                                it1,
                                (tipAmount)
                            )
                        }
                    }
                    networkCall(jsonArray, 1)
                }


            }
        } else {
            val model = Gson().fromJson(
                paymentDetailsResponse.data.magensa_response_data,
                PaymentResponse.PaymentResponseItem::class.java
            )


            val jsonArray: JsonArray?

            when {

                Constants.FIRST_DATA_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    // commented by Mansi for task > Tip to be captured through RAPID CONNECT
                    /*val amount = refundAmount

                    if (model != null) {
                        jsonArray =
                            model.transactionOutput?.token?.let { it1 ->
                                amount.times(100).let {
                                    magtekRequestUtils.processTokenFirstData(
                                        it,
                                        it1,
                                        model.customerTransactionID ?: "",
                                        model.transactionOutput.transactionOutputDetails[0].value,
                                        Constants.CAPTURE
                                    )
                                }
                            }

                        networkCall(jsonArray, 0)
                    }*/
                    tipCall(true)
                }

                // not support CAPTURE
                Constants.ELAVON_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    jsonArray =
                        model.transactionOutput?.token?.let { it1 ->
                            magtekRequestUtils.processTokenElavon(
                                (refundAmount * 100),
                                it1,
                                model.customerTransactionID ?: "",
                                model.transactionOutput.transactionOutputDetails[0].value

                            )
                        }

                    networkCall(jsonArray, 0)
                }

                Constants.EPX_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    jsonArray = model.transactionOutput?.transactionID?.let { it1 ->
                        paymentDetailsResponse.data.amount.times(100).let {
                            magtekRequestUtils.processReferenceIDEPXForce(
                                it,
                                model.customerTransactionID ?: "", it1, Constants.CAPTURE,
                                (tipAmount * 100).toString()
                            )
                        }
                    }
                    networkCall(jsonArray, 1)
                }

                Constants.VANIT_EXORESS_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    jsonArray = model.transactionOutput?.transactionID?.let { it1 ->
                        magtekRequestUtils.processReferenceIDCapture(
                            (refundAmount * 100),
                            model.customerTransactionID ?: "", it1,
                            model.transactionOutput.authCode,
                            ""
                        )
                    }
                    networkCall(jsonArray, 1)
                }

                Constants.CHASE_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    val amount = paymentDetailsResponse.data.amount.plus(refundAmount)

                    jsonArray = amount?.times(100).let {
                        magtekRequestUtils.processTokenChase(
                            it,
                            model.transactionOutput?.token ?: "",
                            model.customerTransactionID ?: "",
                            model.transactionOutput?.authCode ?: "",
                            Constants.CAPTURE
                        )
                    }

                    networkCall(jsonArray, 0)
                }

                Constants.HEARTLAND_GATEWAY == magtekRequestUtils.gatewayName() -> {

                    val amount = paymentDetailsResponse.data.amount.plus(refundAmount)

                    jsonArray = model.transactionOutput?.transactionID?.let { it1 ->
                        amount.times(100).let {
                            magtekRequestUtils.processReferenceIdHeartlandCapture(
                                it,
                                model.customerTransactionID ?: "",
                                it1,
                                model.transactionOutput.authCode,
                                (tipAmount * 100).toString()
                            )
                        }
                    }
                    networkCall(jsonArray, 1)
                }

                Constants.TSYS_GATEWAY == magtekRequestUtils.gatewayName() -> {


                    jsonArray = model.transactionOutput?.transactionID?.let { it1 ->
                        paymentDetailsResponse.data.amount.let {
                            magtekRequestUtils.processReferenceIDTSYSCapture(
                                it,
                                model.customerTransactionID ?: "",
                                it1,
                                (tipAmount)
                            )
                        }
                    }
                    networkCall(jsonArray, 1)
                }


            }
        }

    }

    private fun networkCall(jsonArray1: JsonArray?, i: Int) {

        ProgressUtils.showProgressDialog(requireActivity())

        val call = if (i == 1) {
            jsonArray1?.let { apiModule1.getRetrofit1().processReferenceID(it) }
        } else {
            jsonArray1?.let { apiModule1.getRetrofit1().processToken(it) }
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

                        if (response.body()!![0].transactionOutput?.isTransactionApproved == true) {

                            tipCall(true)

                        } else {
                            AlertUtils.showCustomAlert(
                                requireContext(),
                                response.body()!![0].transactionOutput?.transactionMessage
                            )
                        }


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

    private fun sendBackData(bundle: Bundle) {
        findNavController().previousBackStackEntry?.savedStateHandle?.set(KEY, bundle)
        findNavController().popBackStack(R.id.transactionFragment, false)
    }

    private fun openReceiptDialog(type: Int) {
        val bundle = Bundle()
        bundle.putInt("orderId", orderId)
        bundle.putInt("type", type)
        findNavController().navigate(
            R.id.action_transactionDetailsFragment_to_sendReceiptFragment,
            bundle
        )
    }


    private fun reloadScreen() {
        val bundle = Bundle().apply {
            putInt("orderId", orderId)
            putInt("paymentId", paymentId)
        }

        val id = findNavController().currentDestination?.id
        findNavController().popBackStack(id!!, true)
        findNavController().navigate(id, bundle)
    }


    @SuppressLint("SetTextI18n")
    private fun navigate() {
        ProgressUtils.showProgressDialog(requireActivity())

        viewModel.dataRefundDone.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { createTaxResponse ->
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it,
                        createTaxResponse.message,
                        object :
                            DialogInterface.OnClickListener {
                            override fun onClick(p0: DialogInterface?, p1: Int) {
                                try {
                                    reloadScreen()
                                    p0?.dismiss()
                                } catch (e: Exception) {
                                }
                            }
                        })

                }
            }
        }

        viewModel.dataPayment.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                paymentDetailsResponse = it
                enableDisableRefundButton()
                enableDisableTipButton()
                val jsonString = Gson().toJson(paymentDetailsResponse)
                Log.e("paymentDetailsResponse", "paymentDetailsResponse result = $jsonString")

                if (paymentDetailsResponse.data.order.order_split_type == "OrderAmountTab" || paymentDetailsResponse.data.order.order_split_type == "OrderGuestTab") {
                    isSplitPayment = true
                }

                if (paymentDetailsResponse.data.payment_type == "Card")
                    binding.txtCardDetails.text =
                        paymentDetailsResponse.data.card_name + " " + paymentDetailsResponse.data.card_number


                binding.tvDate.text =
                    convertCurrentDate(
                        it.data.order.created_at,
                        context
                    ) + " " + convertCurrentTime(
                        it.data.order.created_at,
                        context
                    )


                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    binding.txtSubTitle.text = "Order ${it.data.custom_order_id}"
                    binding.orderDetailOrderId.text = it.data.custom_order_id.toString()

                } else {
                    binding.txtSubTitle.text = "Order ${it.data.order_id}"
                    binding.orderDetailOrderId.text = it.data.order_id.toString()
                }

                binding.tvTransactionTime.text =
                    convertCurrentTime(
                        it.data.order.created_at,
                        context
                    )

                binding.tvTransactionDate.text = convertCurrentDate(
                    it.data.order.created_at,
                    context
                )
                if (it.data.order.note.isNotEmpty()) {
                    binding.llNotes.visibility = View.VISIBLE
                    binding.tvNote.text = it.data.order.note.toString()
                }
                if (it.data.service_charge_details.isNotEmpty()) {
                    serviceChargesList = arrayListOf()
                    var serviceOrderType = ""
                    if (it.data.order.order_type == DINE_IN) {
                        serviceOrderType = Constants.SERVICECHARGE_DINEIN_ORDER
                    } else {
                        serviceOrderType = Constants.SERVICECHARGE_TAKEOUT_OPENORDER
                    }
                    it.data.service_charge_details.forEach { service ->
                        var data: TbServiceCharge = TbServiceCharge(
                            id = service.id!!,
                            order_service_charge_id = service.serviceChargeId,
                            name = service.name,
                            percentage = service.rate,
                            createdAt = service.created_at.toString(),
                            updatedAt = service.updated_at,
                            min_guest_count = service.min_guest_count,
                            max_guest_count = service.max_guest_count,
                            isChecked = true,
                            isActive = true,
                            isEnabled = true,
                            order_type = serviceOrderType,
                            locationId = it.data.order.location_id
                        )
                        serviceChargesList?.add(data)
                    }

                } else {
                    serviceChargesList = arrayListOf()
                }

                if (paymentDetailsResponse.data.tips > 0) {
                    val amount = if (paymentDetailsResponse.data.payment_type.equals(
                            "cash",
                            true
                        )
                    ) (paymentDetailsResponse.data.amount + paymentDetailsResponse.data.cash_discount_or_surcharge) else (paymentDetailsResponse.data.amount - paymentDetailsResponse.data.cash_discount_or_surcharge)
                    val percentageTip = MethodUtils.calculatePercentageFromAmount(
                        paymentDetailsResponse.data.tips,
                        amount
                    )
                    binding.tvTipLabel.text = "Tip (${
                        String.format(
                            if (percentageTip > 1) "%.0f" else "%.2f",
                            percentageTip
                        )
                    }%)"
                } else {
                    binding.tvTipLabel.text = "Tip (0%)"
                }

                if (it.data.order.customer != null) {
                    binding.tvCustomerName.text =
                        it.data.order.customer.firstName + " " + it.data.order.customer.lastName
                } else {
                    binding.tvCustomerName.text = ""
                }
                binding.orderDetails = it
                orderDetailsItemAdapter.addOrderDetailsItems(it.data.order.order_items)
                Log.e("OrderTypeId", it.data.order.order_type_id.toString())
                if (it.data.order.order_type_id.equals(5) || it.data.order.order_type_id.equals(2) || it.data.order.order_type_id.equals(
                        6
                    )
                ) {   // order_id 3 is for To go Open Order and order_id 1 for takeout
                    binding.txtPrintKitchenReceipt.visibility = View.GONE
                } else {
                    binding.txtPrintKitchenReceipt.visibility = View.VISIBLE
                }
                binding.llDiscount.visibility = View.VISIBLE
                if (paymentDetailsResponse.data.total_discount != 0.0) {
                    binding.txtDiscount.text = "- $" + String.format(
                        "%.2f",
                        paymentDetailsResponse.data.total_discount
                    )
                } else {
                    binding.txtDiscount.text = "- $" + String.format(
                        "%.2f",
                        paymentDetailsResponse.data.order.total_discount
                    )
                }
                if (!isSplitPayment) {
                    if (paymentDetailsResponse.data.tax_amount != 0.0) {
                        binding.imgDropdown.visible()
                    }
                    if (paymentDetailsResponse.data.order.order_items.isNotEmpty()) {
                        paymentDetailsResponse.data.order.order_items.forEach { orderItem ->
                            var totalPrice = orderItem.price * orderItem.quantity
                            totalPrice -= orderItem.discountAmount
                            orderItem.orderItemModifiers.forEach { orderItemModifier ->
                                totalPrice += orderItemModifier.price * orderItemModifier.quantity
                            }
                            Log.d(TAG, "navigate: itemPrice : $totalPrice")
                            var totaltaxtemp = 0.0
                            orderItem.orderItemTaxes.forEach { orderItemTaxe ->
                                if (taxlistbirfurcation?.isNotEmpty() == true) {
                                    var found = -1
                                    taxlistbirfurcation?.forEachIndexed { index, taxData ->
                                        if (taxData.orderTaxId == orderItemTaxe.taxId) {
                                            found = index
                                            return@forEachIndexed
                                        }
                                    }
                                    if (found == -1) {
                                        var taxData: TaxData = TaxData(
                                            orderItemTaxe.createdAt,
                                            orderItemTaxe.id,
                                            0,
                                            orderItemTaxe.name,
                                            orderItemTaxe.rate,
                                            orderItemTaxe.taxType,
                                            orderItemTaxe.updatedAt,
                                            true,
                                            orderItemTaxe.isDefault,
                                            false,
                                            "",
                                            listOf(orderItemTaxe.orderItemId),
                                            orderItemTaxe.taxId,
                                            false,
                                            getTaxFromTotalPrice(
                                                orderItemTaxe,
                                                totalPrice,
                                                orderItem
                                            ),
                                            totalPrice
                                        )
                                        taxlistbirfurcation?.add(taxData)
                                    } else {
                                        taxlistbirfurcation!![found].totalTaxTypePrice =
                                            taxlistbirfurcation!![found].totalTaxTypePrice + getTaxFromTotalPrice(
                                                orderItemTaxe,
                                                totalPrice,
                                                orderItem
                                            )
                                        taxlistbirfurcation!![found].subTotalAmount =
                                            taxlistbirfurcation!![found].subTotalAmount + totalPrice
                                    }
                                    Log.d(TAG, "found : " + found)
                                } else {
                                    var taxData: TaxData = TaxData(
                                        orderItemTaxe.createdAt,
                                        orderItemTaxe.id,
                                        0,
                                        orderItemTaxe.name,
                                        orderItemTaxe.rate,
                                        orderItemTaxe.taxType,
                                        orderItemTaxe.updatedAt,
                                        true,
                                        orderItemTaxe.isDefault,
                                        false,
                                        "",
                                        listOf(orderItemTaxe.orderItemId),
                                        orderItemTaxe.taxId,
                                        false,
                                        getTaxFromTotalPrice(
                                            orderItemTaxe,
                                            totalPrice,
                                            orderItem
                                        ),
                                        totalPrice
                                    )
                                    taxlistbirfurcation?.add(taxData)
                                }


                                Log.d(TAG, "navigate: " + totaltaxtemp)
                            }

                        }

                        taxBirfurcationAdapter.setList(taxlistbirfurcation!!)
                        Log.d(TAG, "navigate: list " + Gson().toJson(taxlistbirfurcation))
                    }
                } else {
                    binding.relativeDynamicTax.gone()
                    binding.imgDropdown.gone()
                    binding.linearPaymentinfo.layoutParams.height =
                        resources.getDimension(R.dimen._78sdp).toInt()
                    binding.linearSummary.layoutParams.height =
                        resources.getDimension(R.dimen._85sdp).toInt()
                }


                if (paymentDetailsResponse.data.is_loyalty_applied == true) {
                    binding.llLoyalty.visibility = View.VISIBLE
                    binding.llLoyaltyPoints.visibility = View.VISIBLE
                }

                Log.d("Data_paymentDetailsResponse: ",Gson().toJson(paymentDetailsResponse.data.order.refund_detail.refunded_amount))
                if (!paymentDetailsResponse.data.order.refund_detail.refunded_amount.equals(0.0) && paymentDetailsResponse.data.order.payment_status != "Cancelled") {
                    binding.llRefundAmount.visibility = View.VISIBLE
                    binding.tvtipadd.visibility = View.GONE
                    binding.tvIssueRefund.visibility = View.VISIBLE
                }


                if (paymentDetailsResponse.data.payment_type.equals(
                        "Card",
                        true
                    ) || paymentDetailsResponse.data.payment_type.equals("Cash", true)
                ) {
                    val total = String.format("%.2f", paymentDetailsResponse.data.amount)
                    val refundedAmount = String.format(
                        "%.2f",
                        paymentDetailsResponse.data.order.refund_detail.refunded_amount - paymentDetailsResponse.data.tips
                    )

                    /*
                    * "total <= refundedAmount" This is commented so that a transaction can be refunded only once even if it was refunded partially.
                     */

                    if (total.toDouble() <= refundedAmount.toDouble() /*refundedAmount.toDouble() > 0.0*/
                        || (total.toDouble()-0.30) <= refundedAmount.toDouble()   || paymentDetailsResponse.data.order.payment_status == "Cancelled"
                    ) {
                        binding.tvIssueRefund.visibility = View.GONE
                        binding.tvtipadd.visibility = View.GONE
                    }
                }

                //Tips can't be refunded in CASH , added this logic to check amount without tip
                if (paymentDetailsResponse.data.payment_type.equals("Cash")) {
                    if (String.format(
                            "%.2f",
                            paymentDetailsResponse.data.order.refund_detail.refunded_amount
                        ) ==
                        String.format("%.2f", paymentDetailsResponse.data.amount)
                    ) {
                        binding.tvIssueRefund.visibility = View.GONE
                        binding.tvtipadd.visibility = View.GONE
                    }
                }


                if (paymentDetailsResponse.data.order.open_order_type.equals(
                        "Open Order",
                        ignoreCase = true
                    ) &&
                    (paymentDetailsResponse.data.order.open_order_type.equals(
                        "unpaid",
                        ignoreCase = true
                    ) ||
                            paymentDetailsResponse.data.order.open_order_type.equals(
                                "cancelled",
                                ignoreCase = true
                            ))
                ) {
                    binding.tvIssueRefund.visibility = View.GONE
                    binding.tvtipadd.visibility = View.GONE
                }


                if (MethodUtils.isEnableCashDiscount(requireContext())) {
                    if (paymentDetailsResponse.data.payment_type == "Card") {
                        if (paymentDetailsResponse.data.cash_discount_type == "SurCharge") {
                            binding.linearCashDiscount.visibility = View.VISIBLE
                            binding.labelCashsurcharge?.text = Constants.SURCHARGE_TEXT
                            binding.txtCashAmounntDiscount.setTextColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.txtColor
                                )
                            )
                            binding.txtCashAmounntDiscount.text = "+ $" + String.format(
                                "%.2f",
                                paymentDetailsResponse.data.cash_discount_or_surcharge
                            )
                        } else {
                            binding.linearCashDiscount.visibility = View.GONE
                        }
                    } else {
                        if (paymentDetailsResponse.data.cash_discount_type == "CashDiscount") {
                            binding.linearCashDiscount.visibility = View.VISIBLE
                            binding.labelCashsurcharge?.text = "Cash Discount"
                            binding.txtCashAmounntDiscount.setTextColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.colorRed
                                )
                            )
                            binding.txtCashAmounntDiscount.text = "- $" + String.format(
                                "%.2f",
                                paymentDetailsResponse.data.cash_discount_or_surcharge
                            )
                        } else {
                            binding.linearCashDiscount.visibility = View.GONE
                        }
                    }
                } else {
                    binding.linearCashDiscount.visibility = View.GONE
                }

                ProgressUtils.dismissProgressDialog()
            }
        }

    }

    private fun enableDisableRefundButton() {
        if (paymentDetailsResponse.data.payment_type.contains(
                getString(R.string.external),
                ignoreCase = true
            )
        ) {
            binding.tvIssueRefund.gone()
        }
    }

    private fun acceptedAndDeclineOrder() {
        var employee_id = prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
        var terminal_id = prefProvider.getValueInt(Constants.TERMINAL_ID, 0)
        viewModel.acceptedAndDeclineOrder(
            0,
            orderId,
            false,
            employee_id,
            terminal_id
        ).observe(viewLifecycleOwner) { it ->

            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                    }

                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.root.showAlert(resource.message)

                    }

                    Status.LOADING -> {
                        ProgressUtils.showProgressDialog(requireActivity())
                    }
                }
            }
        }
    }

    fun getTaxFromTotalPrice(
        orderItemTaxe: GetOrderDetailsResponse.Data.OrderItem.OrderItemTaxe,
        totalPrice: Double,
        item: GetOrderDetailsResponse.Data.OrderItem
    ): Double {
        var totaltaxtemp = 0.0


        totaltaxtemp += if (orderItemTaxe.taxType == "Percentage") {
            if (totalPrice < 0.0) {

                String.format("%.2f", 0.00)
                    .toDouble()
            } else {
                val itemTaxPrice =
                    (orderItemTaxe.rate * totalPrice) / 100
                String.format("%.2f", itemTaxPrice)
                    .toDouble()
            }

        } else {
            Log.d("yash", "taxCalculation: " + orderItemTaxe.taxType)
            if (totalPrice <= 0.0) {
                String.format("%.2f", 0.00)
                    .toDouble()
            } else {
                String.format("%.2f", orderItemTaxe.rate * item.quantity)
                    .toDouble()
            }
        }
        return totaltaxtemp
    }

    private fun observeShowProgress() {

        viewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }

    }

    private fun setupSnackbar() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    }

    private fun getCustomerReceiptSettings() {
        viewModel.getCustomerReceiptSettings().observe(viewLifecycleOwner) {
            if (it != null) {
                customerSettingModel = it
            }
        }

    }

    private fun getKitchenReceiptSettings() {
        viewModel.getKitchenReceiptSettings().observe(viewLifecycleOwner) {

            if (it != null) {
                kitchenSettingModel = it
            }
        }
    }

    private fun getCustomerPrinters() {

        viewModel.getCustomerPrinterList().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    ProgressUtils.dismissProgressDialog()
                    if (it.data != null) {
                        val customerList = it.data

                        if (isPrintCustomer) {
                            isPrintCustomer = false
                            customerList.forEach {
                                if (it.customerStatus) {
                                    initPrinter(it, Constants.CUSTOMER)
                                }
                            }
                        }

                    }
                }

                Status.ERROR -> {
                    ProgressUtils.dismissProgressDialog()
                }

                Status.LOADING -> {
                    ProgressUtils.showProgressDialog(requireActivity())
                }
            }
        }
    }

    private fun getKitchenPrinters() {
        viewModel.getKitchenPrinterList().observe(viewLifecycleOwner) { it ->
            when (it.status) {
                Status.SUCCESS -> {
                    ProgressUtils.dismissProgressDialog()
                    if (it.data != null && isPrint == true) {
                        isPrint = false
                        kitchenPrinterList = it.data
                        val remain = requireArguments().getDouble("remainingAmount")

                        if (!prefProvider.getValueboolean(
                                Constants.IS_PRINTER_QUEUE_ENABLE,
                                false
                            )
                        ) {
                            if (!requireArguments().getBoolean("isSpilt")) {
                                if (!requireArguments().getBoolean("isDineIn") && !requireArguments().getBoolean(
                                        "isFromActiveOrder"
                                    )
                                ) {
                                    if (paymentDetailsResponse.data.order.order_type == Constants.OPEN_ORDER
                                    ) {

                                        var noItem: Boolean = false

                                        if (prefProvider.getValueboolean(
                                                Constants.OPEN_ORDER_UPDATE_FOR_PRINT,
                                                false
                                            ) == true
                                        ) {
                                            var list = checkOrderItemsForOpenORderUpdate()
                                            if (list.isEmpty()) {
                                                noItem = true
                                            }
                                        }
                                        if (kitchenPrinterList.isNotEmpty() && noItem == false) {
                                            for (i in 0 until kitchenPrinterList.size) {
                                                if (kitchenPrinterList[i].kitchenStatus) {
                                                    kitchenPrinterList[i].orderTypes.forEach {

                                                        if (it.orderTypeId == paymentDetailsResponse.data.order.order_type_id
                                                        ) {
                                                            it.printerSettings.forEach {
                                                                if (it.printType.lowercase()
                                                                        .equals(Constants.KITCHEN.lowercase()) && it.autoPrinting
                                                                ) {

//                                                                    initKitchenPrinter(
//                                                                        kitchenPrinterList.get(i),
//                                                                        Constants.KITCHEN
//                                                                    )
                                                                    if (checkItemsforTransactionPrinter(
                                                                            ((paymentDetailsResponse.data.order.order_items
                                                                                ?: arrayListOf()) as List<GetOrderDetailsResponse.Data.OrderItem>),
                                                                            kitchenPrinterList[i].printerCategories.toCollection(
                                                                                arrayListOf()
                                                                            )
                                                                        )
                                                                    ) {
                                                                        initKitchenPrinter(
                                                                            kitchenPrinterList.get(i),
                                                                            Constants.KITCHEN
                                                                        )
                                                                    }

                                                                }
                                                            }

                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else if (prefProvider.getValue(
                                            Constants.ORDER_TYPE,
                                            ""
                                        ) != Constants.OPEN_ORDER
                                    ) {
                                        if (kitchenPrinterList.isNotEmpty()) {
                                            for (i in 0 until kitchenPrinterList.size) {
                                                if (kitchenPrinterList[i].kitchenStatus) {
                                                    kitchenPrinterList[i].orderTypes.forEach {
                                                        if (it.orderTypeId == paymentDetailsResponse.data.order.order_type_id

                                                        ) {

                                                            it.printerSettings.forEach {
                                                                if (it.printType.lowercase()
                                                                        .equals(Constants.KITCHEN.lowercase()) && it.autoPrinting
                                                                ) {

                                                                    try {
                                                                        if (checkItemsforTransactionPrinter(
                                                                                (paymentDetailsResponse.data.order.order_items
                                                                                    ?: arrayListOf()) as List<GetOrderDetailsResponse.Data.OrderItem>,
                                                                                kitchenPrinterList[i].printerCategories.toCollection(
                                                                                    arrayListOf()
                                                                                )
                                                                            )
                                                                        ) {

                                                                            initKitchenPrinter(
                                                                                kitchenPrinterList.get(
                                                                                    i
                                                                                ),
                                                                                Constants.KITCHEN
                                                                            )
                                                                        }
                                                                    } catch (e: Exception) {

                                                                    }

                                                                }
                                                            }

                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }


                                }
                            }
                        }
                    }


                }

                Status.LOADING -> {
                    ProgressUtils.showProgressDialog(requireActivity())
                }

                Status.ERROR -> {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }
    }

    private fun initPrinter(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String
    ) {

        if (customerReceiptPrinters.name.startsWith(SUNMI_PRINTER, true)) {

            SunmiPrinterApi.getInstance()
                .setPrinter(SunmiPrinter.SunmiBlueToothPrinter, customerReceiptPrinters.ipAddress)

            if (!SunmiPrinterApi.getInstance().isConnected) {
                SunmiPrinterApi.getInstance()
                    .connectPrinter(requireContext(), object : ConnectCallback {

                        override fun onFound() {
                            println("onFound")
                        }

                        override fun onUnfound() {
                            println("onUnfound")
                        }

                        override fun onConnect() {
                            println("onConnect")
                            generatePrintSunmi(customerReceiptPrinters, type)

                        }

                        override fun onDisconnect() {
                            println("onDisconnect")
                        }

                    })
            } else {
                generatePrintSunmi(customerReceiptPrinters, type)
            }

        } else if (customerReceiptPrinters.name.startsWith(SUNMI_INNER_PRINTER, true)) {

            SunmiPrintHelper.getInstance().initSunmiPrinterService(requireContext())
            viewLifecycleOwner.lifecycleScope.launch {
                delay(100)
                setService()
            }


        } else if (customerReceiptPrinters.name.startsWith(LANDI_INNER_PRINTER, true)) {
            printFromLandiInnerPrinter(customerReceiptPrinters)
        } else {

            viewLifecycleOwner.lifecycleScope.launch {
                PrinterClass.closePrinter()
                if (PrinterClass.getPrinter() == null) {
                    var printer: Print? = Print(requireContext())
                    if (printer != null) {
                        //printer.setStatusChangeEventCallback(this)
                        //printer.setBatteryStatusChangeEventCallback(this)
                    }

                    val enabled = Print.FALSE

                    try {
                        var interval: Int = 1000
                        if (customerReceiptPrinters.printer_type == Constants.BLUETOOTH) {
                            interval = PrinterClass.BLUETOOTH_TIMEOUT
                        }
                        printer?.openPrinter(

                            if (customerReceiptPrinters.printer_type == Constants.BLUETOOTH) {
                                Print.DEVTYPE_BLUETOOTH
                            } else {
                                Print.DEVTYPE_TCP
                            },
                            customerReceiptPrinters.ipAddress,
                            enabled,
                            1000
                        )
                        // printer?.setStatusChangeEventCallback(this)

                    } catch (e: Exception) {
                        LogUtil.logE(TAG, "PrinterException: " + e.message)
                        printer = null
                        return@launch
                    }
                    try {

                        if (printer != null) {
                            PrinterClass.setPrinter(printer)

                            generatePrint(customerReceiptPrinters, type)

                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    LogUtil.logE(TAG, "PrinterIsNotNull:")
                }
            }
        }

    }

    private fun initKitchenPrinter(
        data: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String
    ) {
        if (data.name.startsWith(SUNMI_PRINTER, true)) {
            SunmiPrinterApi.getInstance()
                .setPrinter(SunmiPrinter.SunmiBlueToothPrinter, data.ipAddress)
            if (!SunmiPrinterApi.getInstance().isConnected) {
                SunmiPrinterApi.getInstance()
                    .connectPrinter(requireContext(), object : ConnectCallback {
                        override fun onFound() {
                            println("onFound")
                        }

                        override fun onUnfound() {
                            println("onUnfound")
                        }

                        override fun onConnect() {
                            println("onConnect")
                            viewLifecycleOwner.lifecycleScope.launch {
                                delay(200)
                                generateKitchenReceiptSunmi(data, type)
                            }
                        }

                        override fun onDisconnect() {
                            println("onDisconnect")
                        }
                    })
            } else {
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(200)
                    generateKitchenReceiptSunmi(data, type)
                }
            }
        } else if (((data.name.contains("TSP", ignoreCase = true))) || ((data.name.contains(
                "SP",
                ignoreCase = true
            )))
        ) {
            settings = StarConnectionSettings(InterfaceType.Lan, data.macAddress)
            printer = StarPrinter(settings, requireContext())

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val builder = StarXpandCommandBuilder()

                    var printerBuilder = PrinterBuilder()

                    with(printerBuilder) {
                        styleInternationalCharacter(InternationalCharacterType.Usa)
                        styleCharacterSpace(0.0)
                        styleAlignment(Alignment.Center)

                        if (!oneItemPerReceipt) {
                            paymentDetailsResponse.data.order.order_items.forEach { item ->
                                data.printerCategories.toCollection(arrayListOf())?.forEach {
                                    if (it?.id == item.categoryId) {
                                        if (it.categoryActive && it.printerEnable) {
                                            for (singularity in 1..item.quantity) {
                                                if (printOrderIDInStickyPrinter) {
                                                    add(
                                                        PrinterBuilder()
                                                            .styleBold(true)
                                                            .styleMagnification(
                                                                MagnificationParameter(3, 3)
                                                            )
                                                            .actionPrintText(
                                                                "OrderId: ${paymentDetailsResponse.data.custom_order_id}"
                                                            )
                                                    )
                                                }

                                                actionFeedLine(1)

                                                add(
                                                    PrinterBuilder()
                                                        .styleBold(true)
                                                        .styleMagnification(
                                                            MagnificationParameter(2, 2)
                                                        )
                                                        .actionPrintText(
                                                            "${paymentDetailsResponse.data.order.order_type_name}"
                                                        )
                                                )

                                                actionFeedLine(1)

                                                if (paymentDetailsResponse.data.order.order_type_name.contains(
                                                        "Phone",
                                                        true
                                                    ) || (paymentDetailsResponse.data.order.order_type_name.equals(
                                                        "OnlineWebOrder",
                                                        ignoreCase = true
                                                    ))
                                                ) {
                                                    add(
                                                        PrinterBuilder()
                                                            .styleBold(true)
                                                            .styleMagnification(
                                                                MagnificationParameter(2, 2)
                                                            )
                                                            .actionPrintText(
                                                                "${paymentDetailsResponse.data.order.delivery_type}"
                                                            )
                                                    )

                                                    actionFeedLine(1)
                                                }

                                                add(
                                                    PrinterBuilder()
                                                        .styleAlignment(Alignment.Left)
                                                        .styleMagnification(
                                                            MagnificationParameter(2, 2)
                                                        )
                                                        .actionPrintText(
                                                            content = addSingleReprintTransactionOrdersForStarKitchen(
                                                                1,
                                                                item,
                                                                data.printerCategories.toCollection(
                                                                    arrayListOf()
                                                                )
                                                            )
                                                        )
                                                )

                                                actionFeedLine(1)
                                                if (paymentDetailsResponse.data.order.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                                    add(
                                                        PrinterBuilder()
                                                            .styleAlignment(Alignment.Center)
                                                            .styleBold(true)
                                                            .actionPrintText(
                                                                content = if (paymentDetailsResponse.data.order.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                                                    "--------------------------------------------\nOrder Note"
                                                                } else ""
                                                            )
                                                    )
                                                }
                                                if (paymentDetailsResponse.data.order.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                                    add(
                                                        PrinterBuilder()
                                                            .styleAlignment(Alignment.Center)
                                                            .actionPrintText(
                                                                content = if (paymentDetailsResponse.data.order.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                                                    paymentDetailsResponse.data.order.note.toString()
                                                                } else ""
                                                            )
                                                    )
                                                }

                                                actionFeedLine(1)
                                                actionFeedLine(1)

                                                var printedName = StringBuilder("")
                                                paymentDetailsResponse.data.order.customer?.firstName?.let { firstName ->
                                                    paymentDetailsResponse.data.order.customer?.lastName?.let { lastName ->
                                                        if (kitchenSettingModel.showCustomerName || paymentDetailsResponse.data.order.order_type.equals(
                                                                "KioskOpenorder", true
                                                            ) || paymentDetailsResponse.data.order.order_type.equals(
                                                                "OnlineWebOrder",
                                                                true
                                                            ) || paymentDetailsResponse.data.order.order_type.equals(
                                                                "OnlineOrder",
                                                                true
                                                            )
                                                        ) {
                                                            if (!firstName.contains(
                                                                    "customer",
                                                                    ignoreCase = true
                                                                )
                                                            ) {
                                                                printedName.append(firstName)
                                                                printedName.append(" ")
                                                            }

                                                            if (!lastName.isBlank()) {
                                                                printedName.append(lastName)
                                                            }

                                                            if (printedName.isNotEmpty()) {
                                                                add(
                                                                    PrinterBuilder()
                                                                        .styleAlignment(Alignment.Left)
                                                                        .styleBold(true)
                                                                        .actionPrintText(
                                                                            content = "Customer Details\n"
                                                                        )
                                                                )

                                                                add(
                                                                    PrinterBuilder()
                                                                        .styleAlignment(Alignment.Center)
                                                                        .actionPrintText(
                                                                            content =
                                                                            "--------------------------------------------"
                                                                        )
                                                                )

                                                                add(
                                                                    PrinterBuilder()
                                                                        .styleAlignment(Alignment.Left)
                                                                        .actionPrintText(
                                                                            content = printedName.toString()
                                                                        )
                                                                )

                                                            }
                                                        }

                                                    }
                                                }

                                                actionFeedLine(1)

                                                add(
                                                    PrinterBuilder()
                                                        .actionPrintText(
                                                            Constants.getReceiptFormatDateFromUTCServer(
                                                                requireContext(),
                                                                paymentDetailsResponse.data.order?.created_at.toString()
                                                            )
                                                        )
                                                )

                                                printerBuilder.actionFeedLine(1)
                                                actionCut(CutType.Partial)

                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            add(
                                PrinterBuilder()
                                    .styleBold(true)
                                    .styleMagnification(
                                        MagnificationParameter(3, 3)
                                    )
                                    .actionPrintText(
                                        "OrderId: ${paymentDetailsResponse.data.custom_order_id}"
                                    )
                            )

                            styleAlignment(Alignment.Center)

                            add(
                                PrinterBuilder()
                                    .styleBold(true)
                                    .styleMagnification(
                                        MagnificationParameter(2, 2)
                                    )
                                    .actionPrintText(
                                        if (kitchenSettingModel.showOrderType)
                                            paymentDetailsResponse.data.order.order_type
                                        else ""
                                    )
                            )

                            actionFeedLine(1)

                            if ((paymentDetailsResponse.data.order.order_type.equals(Constants.PHONE_ORDER_)) || (paymentDetailsResponse.data.order.order_type.equals(
                                    "OnlineWebOrder",
                                    ignoreCase = true
                                ))
                            ) {
                                add(
                                    PrinterBuilder()
                                        .styleBold(true)
                                        .styleMagnification(
                                            MagnificationParameter(2, 2)
                                        )
                                        .actionPrintText(
                                            paymentDetailsResponse.data.order.delivery_type
                                        )
                                )

                                actionFeedLine(1)
                            }

                            add(
                                PrinterBuilder()
                                    .styleMagnification(
                                        MagnificationParameter(2, 2)
                                    )
                                    .actionPrintText(
                                        "Employee:${
                                            prefProvider.getValue(
                                                Constants.EMPLOYEE_NAME,
                                                ""
                                            )
                                        }"
                                    )
                            )
                            actionFeedLine(1)

                            add(
                                PrinterBuilder()
                                    .styleMagnification(
                                        MagnificationParameter(2, 2)
                                    )
                                    .actionPrintText(
                                        Constants.getReceiptFormatDateFromUTCServer(
                                            requireContext(),
                                            paymentDetailsResponse.data.order.created_at.toString()
                                        )
                                    )
                            )

                            actionFeedLine(1)

                            add(
                                PrinterBuilder()
                                    .styleBold(true)
                                    .actionPrintText(
                                        "--------------------------------------------"
                                    )
                            )

                            actionFeedLine(1)

                            add(
                                PrinterBuilder().styleMagnification(
                                    MagnificationParameter(2, 2)
                                ).styleAlignment(Alignment.Left)
                                    .actionPrintText(
                                        content = addReprintTransactionOrdersForStarKitchen(
                                            paymentDetailsResponse.data.order.order_items!!,
                                            data.printerCategories.toCollection(arrayListOf())
                                        )
                                    )
                            )

                            actionFeedLine(1)
                            if (paymentDetailsResponse.data.order.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                add(
                                    PrinterBuilder()
                                        .styleMagnification(
                                            MagnificationParameter(2, 2)
                                        )
                                        .styleAlignment(Alignment.Center)
                                        .styleBold(true)
                                        .actionPrintText(
                                            content = if (paymentDetailsResponse.data.order.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                                "--------------------------------------------\nOrder Note\n "
                                            } else ""
                                        )
                                )
                            }
                            if (paymentDetailsResponse.data.order.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                add(
                                    PrinterBuilder()
                                        .styleAlignment(Alignment.Center)
                                        .styleMagnification(
                                            MagnificationParameter(2, 2)
                                        )
                                        .actionPrintText(
                                            content = if (paymentDetailsResponse.data.order.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                                paymentDetailsResponse.data.order.note.toString()
                                            } else ""
                                        )
                                )
                            }
                            actionFeedLine(1)
                            if (kitchenSettingModel.showCustomerName && (paymentDetailsResponse.data.order.customer?.firstName != null || paymentDetailsResponse.data.order.customer?.lastName != null)) {
                                add(
                                    PrinterBuilder()
                                        .styleAlignment(Alignment.Left)
                                        .styleBold(true)
                                        .styleMagnification(
                                            MagnificationParameter(2, 2)
                                        )
                                        .actionPrintText(
                                            content = if (kitchenSettingModel.showCustomerName && (paymentDetailsResponse.data.order.customer?.firstName != null || paymentDetailsResponse.data.order.customer?.lastName != null)) {
                                                "Customer Details\n"
                                            } else ""
                                        )
                                )
                            }

                            if (kitchenSettingModel.showCustomerName && (paymentDetailsResponse.data.order.customer?.firstName != null || paymentDetailsResponse.data.order.customer?.lastName != null)) {
                                add(
                                    PrinterBuilder()
                                        .styleAlignment(Alignment.Center)
                                        .actionPrintText(
                                            content = if (kitchenSettingModel.showCustomerName && (paymentDetailsResponse.data.order.customer?.firstName != null || paymentDetailsResponse.data.order.customer?.lastName != null)) {
                                                "--------------------------------------------"
                                            } else ""
                                        )
                                )
                            }
                            if (kitchenSettingModel.showCustomerName && (paymentDetailsResponse.data.order.customer?.firstName != null || paymentDetailsResponse.data.order.customer?.lastName != null)) {
                                add(
                                    PrinterBuilder()
                                        .styleAlignment(Alignment.Left)
                                        .styleMagnification(
                                            MagnificationParameter(2, 2)
                                        )
                                        .actionPrintText(
                                            content = if (kitchenSettingModel.showCustomerName && (paymentDetailsResponse.data.order.customer?.firstName != null || paymentDetailsResponse.data.order.customer?.lastName != null)) {
                                                paymentDetailsResponse.data.order.customer?.firstName + " " + paymentDetailsResponse.data.order.customer?.lastName
                                            } else ""
                                        )
                                )
                            }
                            try {
                                if (kitchenSettingModel.showCustomerPhone && paymentDetailsResponse.data.order.customer?.phones?.get(
                                        0
                                    ) != null
                                ) {
                                    add(
                                        PrinterBuilder()
                                            .styleAlignment(Alignment.Left)
                                            .styleMagnification(
                                                MagnificationParameter(2, 2)
                                            )
                                            .actionPrintText(
                                                content = if (kitchenSettingModel.showCustomerPhone && paymentDetailsResponse.data.order.customer?.phones?.get(
                                                        0
                                                    ) != null
                                                ) {

                                                    var phoneNumber =
                                                        paymentDetailsResponse.data.order.customer?.phones?.get(
                                                            0
                                                        )?.phoneNumber.toString()
                                                    if (phoneNumber.length != 10) {
                                                        // Handle invalid input (must be 10 digits)
                                                        "Invalid phone number"
                                                    }

                                                    val areaCode = phoneNumber.substring(0, 3)
                                                    val firstPart = phoneNumber.substring(3, 6)
                                                    val secondPart = phoneNumber.substring(6)

                                                    "($areaCode)$firstPart-$secondPart"

                                                } else ""
                                            )
                                    )
                                }
                            } catch (e: Exception) {

                            }
                            printerBuilder.actionFeedLine(1).actionCut(CutType.Partial)
                        }

                    }


//                    printerBuilder.actionFeedLine(1).actionCut(CutType.Partial)

                    var document = DocumentBuilder()
                        .addPrinter(printerBuilder)
                    builder.addDocument(
                        document
                    )

                    val commands = builder.getCommands()

                    printer.openAsync().await()

//                    val jobSettings = StarSpoolJobSettings(true, 30, "Print from Android")

                    printer.printAsync(commands).await()

                    try {
                        SunmiPrintHelper.getInstance().openCashBox()
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }

                    Log.d("Printing", "Success")
                } catch (e: Exception) {
                    Log.d("Printing", "Error: ${e}")
                } finally {
                    printer.closeAsync().await()
                }

            }

        } else if (data.name.startsWith(SUNMI_INNER_PRINTER, true)) {
            SunmiPrintHelper.getInstance().initSunmiPrinterService(requireContext())
            viewLifecycleOwner.lifecycleScope.launch {
                delay(200)
                setService2(data, type)
            }
        } else {
            if (!data.name.substring(0, 6).toString().lowercase().contains("TM-m".lowercase())) {

                var mPrinter = if (data.name.substring(0, 6).toString().lowercase()
                        .contains("TM-m".lowercase())
                ) {
                    Printer(
                        Printer.TM_M30,
                        Printer.MODEL_ANK, requireContext()
                    )
                } else {
                    Printer(
                        Printer.TM_U220,
                        Printer.MODEL_ANK, requireContext()
                    )
                }

                mPrinter.setReceiveEventListener { printer, i, printerStatusInfo, s ->

                    if (printerStatusInfo.connection == 1) {
                        try {
                            printer.disconnect()

                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                try {

                    var printerAdd =
                        if (data.printer_type == Constants.BLUETOOTH) "BT:" + data.macAddress else "TCP:" + data.ipAddress
                    if (mPrinter.status.connection == 0) {

                        mPrinter.connect(
                            printerAdd,
                            Printer.PARAM_DEFAULT
                        )
                    }
                    generateReceiptForU220(mPrinter, data, type)
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            } else {
                PrinterClass.closePrinter()
                if (PrinterClass.getPrinter() == null) {
                    var printer: Print? = Print(requireContext())
                    val enabled = Print.FALSE
                    try {
                        printer?.openPrinter(
                            if (data.printer_type == Constants.BLUETOOTH) {
                                Print.DEVTYPE_BLUETOOTH
                            } else {
                                Print.DEVTYPE_TCP
                            },
                            data.ipAddress,
                            enabled,
                            1000
                        )
                    } catch (e: Exception) {
                        //  printerDialog.dismiss()
                        printer = null
                        return
                    }

                    if (printer != null) {
                        PrinterClass.setPrinter(printer)
                        generateKitchenReceipt(data, type)
                    }
                } else {
                    LogUtil.logE(TAG, "PrinterIsNotNull:")
                }
            }
        }
    }

    private fun generateKitchenReceipt(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String
    ) {
        var builder: Builder? = null
        try {
            LogUtil.logE(TAG, "KitchenPrinterName ${customerReceiptPrinters.name}")
            val pname = if (customerReceiptPrinters.name.substring(0, 6).toString()
                    .lowercase() == "TM-m30".lowercase()
            ) {
                "TM-m30"
            } else {
                customerReceiptPrinters.name
            }

            if (customerReceiptPrinters.name.substring(0, 4)
                    .equals("TM-U", true) || customerReceiptPrinters.name.contains("U")
            ) {
                var fontSizeH = 1
                var fontSizeW = 1
                when (kitchenSettingModel.fonts) {
                    Constants.SMALL -> {
                        fontSizeH = 1
                        fontSizeW = 1
                    }

                    Constants.MEDIUM -> {
                        fontSizeH = 1
                        fontSizeW = 2
                    }

                    Constants.LARGE -> {
                        fontSizeH = 2
                        fontSizeW = 2
                    }
                }

                builder = Builder(pname, PrinterClass.language, requireActivity())
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addFeedLine(4)

                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(2, 2)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    builder.addText("OrderID:" + paymentDetailsResponse.data.custom_order_id)
                } else {
                    builder.addText("OrderID:" + paymentDetailsResponse.data.order.id)
                }

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addFeedLine(1)

                if (kitchenSettingModel.showOrderType) {


                    builder.addFeedLine(1)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    builder.addTextAlign(Builder.ALIGN_CENTER)

                    addBuilderText(
                        builder,
                        paymentDetailsResponse.data.order.order_type_name.toString()
                    )
                }
                var tmps = "Open Order".toString().trim()
                    .toString().lowercase()

                if (paymentDetailsResponse.data.order.order_type.equals(PHONE_ORDER, true) ||
                    paymentDetailsResponse.data.order.order_type.equals("OnlineWebOrder", true) ||
                    paymentDetailsResponse.data.order.order_type.equals("Online Order", true) ||
                    paymentDetailsResponse.data.order.order_type.equals("OnlineOrder", true)
                ) {
                    builder.addFeedLine(1)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    builder.addTextAlign(Builder.ALIGN_CENTER)

                    addBuilderText(
                        builder,
                        paymentDetailsResponse.data.order.delivery_type.toString()
                    )
                }

                if (kitchenSettingModel.showTeamMember && paymentDetailsResponse.data.order.employee != null) {

                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    //  builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )
                    builder.addText(
                        padLine(
                            "Employee:" + paymentDetailsResponse.data.order.employee, "",
                            33
                        )
                    )

                }
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(fontSizeH, fontSizeW)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        Constants.getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            paymentDetailsResponse.data.order.created_at.toString()
                        ),
                        "",
                        33
                    )
                )

                builder.addFeedLine(1)

                builder.addTextFont(Builder.FONT_B)
                //builder.addTextLineSpace(20)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(fontSizeH, fontSizeW)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                addHorizontalKitchenLine(builder)


                paymentDetailsResponse.data.order.order_items.let {
                    addOrdersForKitchenTransition(
                        builder!!,
                        it,
                        fontSizeH,
                        fontSizeW,
                        customerReceiptPrinters.printerCategories.toCollection(arrayListOf())
                    )
                }

                if (paymentDetailsResponse.data.order.note.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addFeedLine(1)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_LEFT)
                    //builder.addTextLineSpace(20)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    builder.addText("Order Note")

                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)

                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_CENTER)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )


                    builder.addText(paymentDetailsResponse.data.order.note.toString())
                }

                addHorizontalKitchenLine(builder)
                if (kitchenSettingModel.showCustomerAddress != false || kitchenSettingModel.showCustomerPhone != false || kitchenSettingModel.showCustomerName != false) {
                    if (paymentDetailsResponse.data.order.customer != null) {

                        builder.addTextLineSpace(30)
                        builder.addFeedUnit(30)
                        builder.addFeedLine(1)
                        builder.addTextFont(Builder.FONT_E)
                        //builder.addTextLineSpace(20)
                        builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        builder.addTextSize(fontSizeH, fontSizeW)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.TRUE,
                            Builder.COLOR_1
                        )
                        builder.addText("Customer Details" + "\n")

                        builder.addTextFont(Builder.FONT_B)
                        //builder.addTextLineSpace(20)
                        builder.addTextLang(Builder.LANG_EN)
                        builder.addTextSize(fontSizeH, fontSizeW)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )
                        addHorizontalKitchenLine(builder)

                        if (kitchenSettingModel.showCustomerName) {

                            builder.addTextLineSpace(30)
                            builder.addFeedUnit(30)
                            builder.addTextFont(Builder.FONT_E)
                            builder.addTextAlign(Builder.ALIGN_LEFT)
                            //builder.addTextLineSpace(20)
                            builder.addTextLang(Builder.LANG_EN)
                            builder.addTextSize(fontSizeH, fontSizeW)
                            builder.addTextStyle(
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.TRUE,
                                Builder.COLOR_1
                            )
                            builder.addText(paymentDetailsResponse.data.order.customer.firstName + " " + paymentDetailsResponse.data.order.customer.lastName)
                        }

                        if (kitchenSettingModel.showCustomerPhone) {

                            if (paymentDetailsResponse.data.order.customer.phones.isNotEmpty() == true) {
                                builder.addTextLineSpace(30)
                                builder.addFeedUnit(30)
                                builder.addTextFont(Builder.FONT_E)
                                builder.addTextAlign(Builder.ALIGN_LEFT)
                                //builder.addTextLineSpace(20)
                                builder.addTextLang(Builder.LANG_EN)
                                builder.addTextSize(fontSizeH, fontSizeW)
                                builder.addTextStyle(
                                    Builder.FALSE,
                                    Builder.FALSE,
                                    Builder.TRUE,
                                    Builder.COLOR_1
                                )
                                builder.addText(
                                    MethodUtils.getUSFormatNumber(
                                        paymentDetailsResponse.data.order.customer.phones.get(
                                            0
                                        )?.phoneNumber.toString()
                                    )
                                )
                            }
                        }

                        if (kitchenSettingModel.showCustomerAddress) {
                            if (paymentDetailsResponse.data.order.order_type.trim().toString()
                                    .lowercase() == "Open Order".trim()
                                    .toString().lowercase()
                                && paymentDetailsResponse.data.order.delivery_type.trim().toString()
                                    .lowercase() == "Pickup".trim().lowercase()
                            ) {

                            } else {

                                if (paymentDetailsResponse.data.order.customer.addresses.isNotEmpty() == true) {

                                    builder.addTextLineSpace(30)
                                    builder.addFeedUnit(30)
                                    builder.addTextFont(Builder.FONT_E)
                                    builder.addTextAlign(Builder.ALIGN_LEFT)
                                    //builder.addTextLineSpace(20)
                                    builder.addTextLang(Builder.LANG_EN)
                                    builder.addTextSize(fontSizeH, fontSizeW)
                                    builder.addTextStyle(
                                        Builder.FALSE,
                                        Builder.FALSE,
                                        Builder.TRUE,
                                        Builder.COLOR_1
                                    )
                                    paymentDetailsResponse.data.order.customer.addresses.filter { it.typeOfAddress == Constants.BILLING_ADDRESS }
                                        .forEach {
                                            if (it.typeOfAddress.equals(
                                                    Constants.BILLING_ADDRESS,
                                                    ignoreCase = true
                                                )
                                            ) {
                                                builder!!.addText(
                                                    it.fullAddress
                                                )
                                            }
                                        }
                                }
                            }
                        }

                    }
                }
            } else {

                var fontSizeH = 1
                var fontSizeW = 1
                when (kitchenSettingModel.fonts) {
                    Constants.SMALL -> {
                        fontSizeH = 1
                        fontSizeW = 1
                    }

                    Constants.MEDIUM -> {
                        fontSizeH = 1
                        fontSizeW = 2
                    }

                    Constants.LARGE -> {
                        fontSizeH = 2
                        fontSizeW = 2
                    }
                }

                builder = Builder(pname, PrinterClass.language, requireActivity())
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addFeedLine(2)
                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(2, 2)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )
                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    builder.addText(
                        "OrderID:" + paymentDetailsResponse.data.custom_order_id
                    )
                } else {
                    builder.addText(
                        "OrderID:" + paymentDetailsResponse.data.order.id
                    )
                }

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addFeedLine(1)

                if (kitchenSettingModel.showOrderType) {
                    builder.addFeedLine(1)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    builder.addTextAlign(Builder.ALIGN_CENTER)

                    addBuilderText(
                        builder,
                        paymentDetailsResponse.data.order.order_type_name.toString()
                    )
                }
                var tmps = "Open Order".toString().trim()
                    .toString().lowercase()



                if (paymentDetailsResponse.data.order.order_type.equals(PHONE_ORDER, true) ||
                    paymentDetailsResponse.data.order.order_type.equals("OnlineWebOrder", true) ||
                    paymentDetailsResponse.data.order.order_type.equals("Online Order", true) ||
                    paymentDetailsResponse.data.order.order_type.equals("OnlineOrder", true)
                ) {
                    builder.addFeedLine(1)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    builder.addTextAlign(Builder.ALIGN_CENTER)

                    addBuilderText(
                        builder,
                        paymentDetailsResponse.data.order.delivery_type.toString()
                    )
                }

                if (kitchenSettingModel.showTeamMember) {

                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    //  builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )
                    builder.addText(
                        padLine(
                            "Employee:" + paymentDetailsResponse.data.order.employee, "",
                            if (kitchenSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )

                }
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                //  builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(fontSizeH, fontSizeW)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        Constants.getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            paymentDetailsResponse.data.order.created_at.toString()
                        ),
                        "",
                        if (kitchenSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

                builder.addFeedLine(1)



                addHorizontalLine(builder)

                paymentDetailsResponse.data.order.order_items.let {
                    addOrdersForKitchenTransition(
                        builder,
                        it,
                        fontSizeH,
                        fontSizeW,
                        customerReceiptPrinters.printerCategories.toCollection(arrayListOf())
                    )
                }

                if (paymentDetailsResponse.data.order.note.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addFeedLine(1)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_LEFT)
                    //builder.addTextLineSpace(20)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )
                    builder.addText("Order Note")

                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)

                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    builder.addTextSize(fontSizeH, fontSizeW)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )


                    builder.addText(paymentDetailsResponse.data.order.note.toString())
                }


                if (kitchenSettingModel.showCustomerAddress != false || kitchenSettingModel.showCustomerPhone != false || kitchenSettingModel.showCustomerName != false) {
                    if (paymentDetailsResponse.data.order.customer != null) {

                        builder.addTextLineSpace(30)
                        builder.addFeedUnit(30)
                        builder.addFeedLine(1)
                        builder.addTextFont(Builder.FONT_E)
                        //builder.addTextLineSpace(20)
                        builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        builder.addTextSize(fontSizeH, fontSizeW)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.TRUE,
                            Builder.COLOR_1
                        )
                        builder.addText("Customer Details" + "\n")

                        builder.addTextFont(Builder.FONT_B)
                        //builder.addTextLineSpace(20)
                        builder.addTextLang(Builder.LANG_EN)
                        builder.addTextSize(fontSizeH, fontSizeW)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )
                        addHorizontalLine(builder)

                        if (kitchenSettingModel.showCustomerName) {

                            builder.addTextLineSpace(30)
                            builder.addFeedUnit(30)
                            builder.addTextFont(Builder.FONT_E)
                            builder.addTextAlign(Builder.ALIGN_LEFT)
                            //builder.addTextLineSpace(20)
                            builder.addTextLang(Builder.LANG_EN)
                            builder.addTextSize(fontSizeH, fontSizeW)
                            builder.addTextStyle(
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.TRUE,
                                Builder.COLOR_1
                            )
                            builder.addText(paymentDetailsResponse.data.order.customer.firstName + " " + paymentDetailsResponse.data.order.customer.lastName)

                        }


                        if (kitchenSettingModel.showCustomerPhone) {

                            if (paymentDetailsResponse.data.order.customer.phones.isNotEmpty() == true) {
                                builder.addTextLineSpace(30)
                                builder.addFeedUnit(30)
                                builder.addTextFont(Builder.FONT_E)
                                builder.addTextAlign(Builder.ALIGN_LEFT)
                                //builder.addTextLineSpace(20)
                                builder.addTextLang(Builder.LANG_EN)
                                builder.addTextSize(fontSizeH, fontSizeW)
                                builder.addTextStyle(
                                    Builder.FALSE,
                                    Builder.FALSE,
                                    Builder.TRUE,
                                    Builder.COLOR_1
                                )
                                builder.addText(
                                    MethodUtils.getUSFormatNumber(
                                        paymentDetailsResponse.data.order.customer.phones.get(
                                            0
                                        )?.phoneNumber.toString()
                                    )
                                )
                            }

                        }
                        /* builder.addTextLineSpace(30)
                 builder.addFeedUnit(30)
                 builder.addTextFont(Builder.FONT_E)
                 builder.addTextAlign(Builder.ALIGN_LEFT)
                 //builder.addTextLineSpace(20)
                 builder.addTextLang(Builder.LANG_EN)
                 builder.addTextSize(1, 1)
                 builder.addTextStyle(
                     Builder.FALSE,
                     Builder.FALSE,
                     Builder.TRUE,
                     Builder.COLOR_1
                 )
                 builder.addText(receiptModel?.order?.customer?.email)*/

                        if (kitchenSettingModel.showCustomerAddress) {
                            if (paymentDetailsResponse.data.order.order_type.trim().toString()
                                    .lowercase() == "Open Order".trim()
                                    .toString().lowercase()
                                && paymentDetailsResponse.data.order.delivery_type.trim().toString()
                                    .lowercase() == "Pickup".trim().lowercase()
                            ) {

                            } else {

                                if (paymentDetailsResponse.data.order.customer.addresses.isNotEmpty() == true) {

                                    builder.addTextLineSpace(30)
                                    builder.addFeedUnit(30)
                                    builder.addTextFont(Builder.FONT_E)
                                    builder.addTextAlign(Builder.ALIGN_LEFT)
                                    //builder.addTextLineSpace(20)
                                    builder.addTextLang(Builder.LANG_EN)
                                    builder.addTextSize(fontSizeH, fontSizeW)
                                    builder.addTextStyle(
                                        Builder.FALSE,
                                        Builder.FALSE,
                                        Builder.TRUE,
                                        Builder.COLOR_1
                                    )

                                    paymentDetailsResponse.data.order.customer.addresses.filter { it.typeOfAddress == Constants.BILLING_ADDRESS }
                                        .forEach {

                                            if (it.typeOfAddress.equals(
                                                    Constants.BILLING_ADDRESS,
                                                    ignoreCase = true
                                                )
                                            ) {
                                                builder.addText(
                                                    it.fullAddress
                                                )
                                            }
                                        }
                                    // builder.addText(receiptModel?.order?.customer?.addresses?.get(0)?.fullAddress)
                                }
                            }
                        }

                    }
                }

            }

            builder.addFeedLine(5)

            builder.addCut(Builder.CUT_FEED)

            val status = IntArray(1)
            val battery = IntArray(1)

            var timeOut = PrinterClass.SEND_TIMEOUT
            if (customerReceiptPrinters.printer_type == Constants.BLUETOOTH) {
                timeOut = PrinterClass.BLUETOOTH_TIMEOUT
            }

            if (customerReceiptPrinters.name.substring(0, 6).toString()
                    .lowercase() == "TM-m30".lowercase() && customerReceiptPrinters.printer_type != Constants.BLUETOOTH
            ) {

                timeOut = 10000
            }

            try {
                PrinterClass.getPrinter()?.sendData(
                    builder,
                    timeOut, status, battery
                )

                Handler(Looper.getMainLooper()).postDelayed(Runnable {

                }, 100)
                //printerDialog.dismiss()
                PrinterClass.closePrinter()

                //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
            } catch (e: Exception) {
//                printerDialog.dismiss()
                PrinterClass.closePrinter()
                e.printStackTrace()
                LogUtil.logE(TAG, "PrinterError: " + e.localizedMessage)
            }


        } catch (e: Exception) {
            // printerDialog.dismiss()
            e.printStackTrace()
        }

    }

    private fun generateKitchenReceiptSunmi(
        kitchenReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String
    ) {
        try {
            PrintSunmiUtils.fontSize(kitchenSettingModel.fonts)
            SunmiPrinterApi.getInstance().printerInit()
            SunmiPrinterApi.getInstance().lineWrap(4)
            if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                PrintSunmiUtils.orderIdLarge("OrderID:" + paymentDetailsResponse.data.custom_order_id)
            } else {
                PrintSunmiUtils.orderIdLarge("OrderID:" + paymentDetailsResponse.data.order.id)
            }
            SunmiPrinterApi.getInstance().lineWrap(1)

            if (kitchenSettingModel.showOrderType) {
                PrintSunmiUtils.printOrderType(paymentDetailsResponse.data.order.order_type_name.toString())
                SunmiPrinterApi.getInstance().lineWrap(1)
            }

            if (paymentDetailsResponse.data.order.order_type.equals(PHONE_ORDER, true) ||
                paymentDetailsResponse.data.order.order_type.equals("OnlineWebOrder", true) ||
                paymentDetailsResponse.data.order.order_type.equals("Online Order", true) ||
                paymentDetailsResponse.data.order.order_type.equals("OnlineOrder", true)
            ) {
                PrintSunmiUtils.printOrderType(paymentDetailsResponse.data.order.delivery_type.toString())
                SunmiPrinterApi.getInstance().lineWrap(1)
            }


            /*  if (receiptModel?.order?.orderType.toString().lowercase() == "OpenOrder".trim()
                      .toString().lowercase() || receiptModel?.order?.orderType.toString()
                      .lowercase() == "Open Order".trim()
                      .toString().lowercase()
              ) {

                  PrintSunmiUtils.printOrderType(receiptModel?.order?.deliveryType.toString())
                  SunmiPrinterApi.getInstance().lineWrap(1)
              }*/


            if (kitchenSettingModel.showTeamMember && paymentDetailsResponse.data.order.employee != null) {
                PrintSunmiUtils.employee(
                    padLine(
                        "Employee:" + paymentDetailsResponse.data.order.employee, "",
                        if (kitchenSettingModel.fonts == Constants.LARGE) 23 else 48
                    ).toString()
                )
            }
            SunmiPrinterApi.getInstance().lineWrap(1)
            PrintSunmiUtils.orderTime(
                padLine(
                    Constants.getReceiptFormatDateFromUTCServer(
                        requireContext(),
                        paymentDetailsResponse.data.order.created_at.toString()
                    ),
                    "",
                    if (kitchenSettingModel.fonts == Constants.LARGE) 23 else 48
                ).toString()
            )

            PrintSunmiUtils.addHorizontal()
            SunmiPrinterApi.getInstance().lineWrap(1)

            Log.e(
                TAG,
                "getValueUpdate:  ${
                    prefProvider.getValueboolean(
                        Constants.OPEN_ORDER_UPDATE_FOR_PRINT,
                        false
                    )
                }"
            )
            if (prefProvider.getValueboolean(
                    Constants.OPEN_ORDER_UPDATE_FOR_PRINT,
                    false
                ) == true
            ) {
                paymentDetailsResponse.data.order.order_items.let {
//                    var printOrderItems = checkOrderItemsForOpenORderUpdate()
//                    Log.e(TAG, "printeOrderItems  ${Gson().toJson(printOrderItems)}")
//                    addOrdersForKitchenTransition(
//                        if (printOrderItems.isNotEmpty()) printOrderItems else it,
//                        kitchenReceiptPrinters.printerCategories.toCollection(arrayListOf())
//                    )
                }
            } else {
                paymentDetailsResponse.data.order.order_items.let {
                    addOrdersForKitchenTransition(
                        it,
                        kitchenReceiptPrinters.printerCategories.toCollection(arrayListOf())
                    )
                }
            }

            SunmiPrinterApi.getInstance().lineWrap(1)
            if (paymentDetailsResponse.data.order.note.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
                PrintSunmiUtils.orderNote(paymentDetailsResponse.data.order.note.toString())
            }

            SunmiPrinterApi.getInstance().lineWrap(1)
            if (kitchenSettingModel.showCustomerAddress != false || kitchenSettingModel.showCustomerPhone != false || kitchenSettingModel.showCustomerName != false) {
                if (paymentDetailsResponse.data.order.customer != null) {
                    PrintSunmiUtils.customerDetails()
                    if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(
                            1
                        )
                            ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
                    ) {
                        PrintSunmiUtils.normalText("\n")
                    }
                    if (kitchenSettingModel.showCustomerName) {
                        PrintSunmiUtils.customerName(paymentDetailsResponse.data.order.customer.firstName + " " + paymentDetailsResponse.data.order.customer.lastName)
                    }

                    if (kitchenSettingModel.showCustomerPhone) {
                        if (paymentDetailsResponse.data.order.customer.phones.isNotEmpty() == true) {
                            paymentDetailsResponse.data.order.customer.phones.get(0).phoneNumber.let {
                                PrintSunmiUtils.customerPhone(
                                    MethodUtils.getUSFormatNumber(it)
                                )
                            }
                        }
                    }

                    if (kitchenSettingModel.showCustomerAddress) {
                        if (paymentDetailsResponse.data.order.order_type.trim().toString()
                                .lowercase() == "Open Order".trim()
                                .toString().lowercase()
                            && paymentDetailsResponse.data.order.delivery_type.trim().toString()
                                .lowercase() == "Pickup".trim().lowercase()
                        ) {
                        } else {
                            if (paymentDetailsResponse.data.order.customer.addresses.isNotEmpty() == true) {
                                paymentDetailsResponse.data.order.customer.addresses.filter { it.typeOfAddress == Constants.BILLING_ADDRESS }
                                    .forEach {
                                        if (it.typeOfAddress.equals(
                                                Constants.BILLING_ADDRESS,
                                                ignoreCase = true
                                            )
                                        ) {
                                            PrintSunmiUtils.customerAddress(
                                                it.fullAddress
                                            )
                                        }
                                    }
                            }
                        }
                    }

                }
            }

            SunmiPrinterApi.getInstance().lineWrap(2)
            PrintSunmiUtils.cutPaper()

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    private fun generateReceiptForU220(
        mPrinter: Printer,
        data: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String
    ) {

        var fontSizeH = 1
        var fontSizeW = 1
        when (kitchenSettingModel.fonts) {
            Constants.SMALL -> {
                fontSizeH = 1
                fontSizeW = 1
            }

            Constants.MEDIUM -> {
                fontSizeH = 1
                fontSizeW = 2
            }

            Constants.LARGE -> {
                fontSizeH = 2
                fontSizeW = 2
            }
        }

        mPrinter.addFeedUnit(30)
        mPrinter.addFeedLine(2)
        mPrinter.addTextFont(Builder.FONT_E)
        mPrinter.addTextAlign(Builder.ALIGN_CENTER)
        mPrinter.addTextLang(Builder.LANG_EN)
        mPrinter.addTextSize(2, 2)
        mPrinter.addTextStyle(
            Builder.FALSE,
            Builder.FALSE,
            Builder.TRUE,
            Builder.COLOR_1
        )

        mPrinter.addText("OrderID:" + paymentDetailsResponse.data.custom_order_id)
        mPrinter.addFeedLine(1)
        mPrinter.addFeedUnit(30)
        mPrinter.addFeedLine(1)

        if (kitchenSettingModel.showOrderType) {
            mPrinter.addFeedLine(1)
            mPrinter.addTextFont(Builder.FONT_E)
            mPrinter.addTextLang(Builder.LANG_EN)
            mPrinter.addTextSize(fontSizeH, fontSizeW)
            mPrinter.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            mPrinter.addTextAlign(Builder.ALIGN_CENTER)
            addBuilderTextForU220(
                mPrinter,
                paymentDetailsResponse.data.order.order_type_name.toString()
            )
        }
        var tmps = "Open Order".toString().trim()
            .toString().lowercase()

        if (paymentDetailsResponse.data.order.order_type_name.equals(PHONE_ORDER, true) ||
            paymentDetailsResponse.data.order.order_type_name.equals("OnlineWebOrder", true) ||
            paymentDetailsResponse.data.order.order_type_name.equals("Online Order", true) ||
            paymentDetailsResponse.data.order.order_type_name.equals("OnlineOrder", true)
        ) {
            mPrinter.addFeedLine(1)
            mPrinter.addTextFont(Builder.FONT_E)
            mPrinter.addTextLang(Builder.LANG_EN)
            mPrinter.addTextSize(fontSizeH, fontSizeW)
            mPrinter.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            mPrinter.addTextAlign(Builder.ALIGN_CENTER)
            addBuilderTextForU220(
                mPrinter,
                paymentDetailsResponse.data.order.delivery_type.toString()
            )
        }

        if (kitchenSettingModel.showTeamMember) {
            mPrinter.addFeedLine(1)
            mPrinter.addFeedUnit(30)
            mPrinter.addTextFont(Builder.FONT_E)
            //  builder.addTextAlign(Builder.ALIGN_LEFT)
            mPrinter.addTextLang(Builder.LANG_EN)
            mPrinter.addTextSize(fontSizeH, fontSizeW)
            mPrinter.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )
            mPrinter.addText(
                padLine(
                    "Employee:" + paymentDetailsResponse.data.order.employee, "",
                    33
                )
            )
        }
        mPrinter.addFeedLine(1)
        mPrinter.addFeedUnit(30)
        mPrinter.addTextFont(Builder.FONT_E)
        //  builder.addTextAlign(Builder.ALIGN_LEFT)
        mPrinter.addTextLang(Builder.LANG_EN)
        mPrinter.addTextSize(fontSizeH, fontSizeW)
        mPrinter.addTextStyle(
            Builder.FALSE,
            Builder.FALSE,
            Builder.FALSE,
            Builder.COLOR_1
        )

        mPrinter.addText(
            padLine(
                Constants.getReceiptFormatDateFromUTCServer(
                    requireContext(),
                    paymentDetailsResponse.data.order.created_at.toString()
                ),
                "",
                33
            )
        )

        mPrinter.addFeedLine(1)
        mPrinter.addTextFont(Builder.FONT_B)
        //builder.addTextLineSpace(20)
        mPrinter.addTextLang(Builder.LANG_EN)
        mPrinter.addTextSize(fontSizeH, fontSizeW)
        mPrinter.addTextStyle(
            Builder.FALSE,
            Builder.FALSE,
            Builder.FALSE,
            Builder.COLOR_1
        )

        addHorizontalKitchenLineForU220(mPrinter)
        paymentDetailsResponse.data.order.order_items.let {
            addOrdersForKitchenTransitionU220(
                mPrinter,
                it,
                fontSizeH,
                fontSizeW,
                data.printerCategories.toCollection(arrayListOf())
            )
        }
        mPrinter.addFeedLine(1)

        if (paymentDetailsResponse.data.order.note.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
            mPrinter.addFeedUnit(30)
            mPrinter.addFeedLine(1)
            mPrinter.addTextFont(Builder.FONT_E)
            mPrinter.addTextAlign(Builder.ALIGN_LEFT)
            mPrinter.addTextLang(Builder.LANG_EN)
            mPrinter.addTextSize(fontSizeH, fontSizeW)
            mPrinter.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            mPrinter.addText("Order Note")
            mPrinter.addFeedLine(1)
            mPrinter.addFeedUnit(30)
            mPrinter.addTextFont(Builder.FONT_E)
            mPrinter.addTextAlign(Builder.ALIGN_LEFT)
            mPrinter.addTextLang(Builder.LANG_EN)
            mPrinter.addTextSize(fontSizeH, fontSizeW)
            mPrinter.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )
            mPrinter.addText(paymentDetailsResponse.data.order.note)
        }

        mPrinter.addFeedLine(1)
        if (kitchenSettingModel.showCustomerAddress != false || kitchenSettingModel.showCustomerPhone != false || kitchenSettingModel.showCustomerName != false) {
            mPrinter.addTextFont(Builder.FONT_B)
            //builder.addTextLineSpace(20)
            mPrinter.addTextLang(Builder.LANG_EN)
            mPrinter.addTextSize(fontSizeH, fontSizeW)
            mPrinter.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )
            addHorizontalKitchenLineForU220(mPrinter)
            if (paymentDetailsResponse.data.order.customer != null) {
                mPrinter.addFeedLine(1)
                mPrinter.addFeedUnit(30)
                mPrinter.addFeedLine(1)
                mPrinter.addTextFont(Builder.FONT_E)
                //builder.addTextLineSpace(20)
                mPrinter.addTextAlign(Builder.ALIGN_LEFT)
                mPrinter.addTextLang(Builder.LANG_EN)
                mPrinter.addTextSize(fontSizeH, fontSizeW)
                mPrinter.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )
                mPrinter.addText("Customer Details" + "\n")
                mPrinter.addTextFont(Builder.FONT_B)
                //builder.addTextLineSpace(20)
                mPrinter.addTextLang(Builder.LANG_EN)
                mPrinter.addTextSize(fontSizeH, fontSizeW)
                mPrinter.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                addHorizontalKitchenLineForU220(mPrinter)

                if (kitchenSettingModel.showCustomerName) {
                    mPrinter.addFeedLine(1)
                    mPrinter.addFeedUnit(30)
                    mPrinter.addTextFont(Builder.FONT_E)
                    mPrinter.addTextAlign(Builder.ALIGN_LEFT)
                    //builder.addTextLineSpace(20)
                    mPrinter.addTextLang(Builder.LANG_EN)
                    mPrinter.addTextSize(fontSizeH, fontSizeW)
                    mPrinter.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    mPrinter.addText(paymentDetailsResponse.data.order.customer.firstName + " " + paymentDetailsResponse.data.order.customer.lastName)
                }

                if (kitchenSettingModel.showCustomerPhone) {

                    if (paymentDetailsResponse.data.order.customer.phones.isNotEmpty() == true) {
                        mPrinter.addFeedLine(1)
                        mPrinter.addFeedUnit(30)
                        mPrinter.addTextFont(Builder.FONT_E)
                        mPrinter.addTextAlign(Builder.ALIGN_LEFT)
                        //builder.addTextLineSpace(20)
                        mPrinter.addTextLang(Builder.LANG_EN)
                        mPrinter.addTextSize(fontSizeH, fontSizeW)
                        mPrinter.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.TRUE,
                            Builder.COLOR_1
                        )
                        mPrinter.addText(
                            MethodUtils.getUSFormatNumber(
                                paymentDetailsResponse.data.order.customer.phones.get(
                                    0
                                )?.phoneNumber.toString()
                            )
                        )
                    }

                }

                if (kitchenSettingModel.showCustomerAddress) {
                    if (paymentDetailsResponse.data.order.order_type.trim()
                            .lowercase() == "Open Order".trim()
                            .toString().lowercase()
                        && paymentDetailsResponse.data.order.delivery_type?.trim().toString()
                            .lowercase() == "Pickup".trim().lowercase()
                    ) {

                    } else {
                        if (paymentDetailsResponse.data.order.customer.addresses.isNotEmpty() == true) {
                            mPrinter.addFeedLine(1)
                            mPrinter.addFeedUnit(30)
                            mPrinter.addTextFont(Builder.FONT_E)
                            mPrinter.addTextAlign(Builder.ALIGN_LEFT)
                            //builder.addTextLineSpace(20)
                            mPrinter.addTextLang(Builder.LANG_EN)
                            mPrinter.addTextSize(fontSizeH, fontSizeW)
                            mPrinter.addTextStyle(
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.TRUE,
                                Builder.COLOR_1
                            )
                            paymentDetailsResponse.data.order.customer.addresses.filter { it.typeOfAddress == Constants.BILLING_ADDRESS }
                                .forEach {
                                    if (it.typeOfAddress.equals(
                                            Constants.BILLING_ADDRESS,
                                            ignoreCase = true
                                        )
                                    ) {
                                        mPrinter.addText(
                                            it.fullAddress
                                        )
                                    }
                                }
                        }
                    }
                }

            }
        }
        mPrinter.addFeedLine(4)
        mPrinter.addCut(Builder.CUT_FEED)
        try {
            mPrinter.sendData(Printer.PARAM_DEFAULT)
            mPrinter.clearCommandBuffer()
            mPrinter.endTransaction()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun checkOrderItemsForOpenORderUpdate(): ArrayList<CreateOrderResponse.Data.Order.OrderItem> {
        var printOrderItems: ArrayList<CreateOrderResponse.Data.Order.OrderItem> =
            arrayListOf()
        var orderItemsToPrint: ArrayList<CreateOrderResponse.Data.Order.OrderItem> = arrayListOf()
        receiptModel?.order?.orderItems?.let { orderItemsToPrint.addAll(it) }

        val serializedObject: String =
            prefProvider.getValue(
                Constants.OPEN_ORDER_ITEMS,
                ""
            )

        Log.e(
            TAG, "getserializedObject:  ${
                prefProvider.getValue(
                    Constants.OPEN_ORDER_ITEMS,
                    ""
                )
            }"
        )
        if (serializedObject.isNotEmpty()) {
            val gson = Gson()
            val type = object :
                TypeToken<List<CreateOrderResponse.Data.Order.OrderItem?>?>() {}.type
            var arrayItems: ArrayList<CreateOrderResponse.Data.Order.OrderItem> =
                gson.fromJson<Any>(
                    serializedObject,
                    type
                ) as ArrayList<CreateOrderResponse.Data.Order.OrderItem>

            LogUtil.logE(
                TAG,
                "arrayItems:  ${
                    Gson().toJson(
                        arrayItems
                    )
                }"
            )

            Log.e(
                TAG,
                "updafwwewe  ${Gson().toJson(Gson().toJson(receiptModel?.order?.orderItems))}"
            )
            var itemIds: ArrayList<Int> =
                arrayListOf()
            arrayItems.forEach {
                itemIds.add(it.itemId)
            }


            orderItemsToPrint?.forEachIndexed { index, orderItem ->

                if (itemIds.contains(
                        orderItem.itemId
                    )
                ) {

                    arrayItems.forEachIndexed { index, orderItemJ ->

                        if (orderItemJ.itemId == orderItem.itemId) {
                            if (orderItemJ.quantity != orderItem.quantity) {
                                if (orderItem.quantity > orderItemJ.quantity) {
                                    orderItem.quantity =
                                        orderItem.quantity - orderItemJ.quantity

                                    Log.e(TAG, "ItemColdQty ${orderItemJ.quantity}")
                                    Log.e(TAG, "ItemCnewQty ${orderItem.quantity}")
                                    Log.e(TAG, "FinalOrderItemQty  ${orderItem.quantity}")
                                    if (!printOrderItems.contains(
                                            orderItem
                                        )
                                    ) {
                                        printOrderItems.add(
                                            orderItem
                                        )
                                        return@forEachIndexed

                                    }
                                }
                            }


                        }
                    }


                } else {
                    printOrderItems.add(
                        orderItem
                    )
                }


            }
        }
        /*  prefProvider.setValue(
              Constants.OPEN_ORDER_ITEMS,
              ""
          )*/

        Log.e(TAG, "ReturnListForPrint  ${Gson().toJson(printOrderItems)}")

        return printOrderItems


    }

    private fun generateKitchenReceiptSunmiInner(
        kitchenReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String
    ) {
        try {
            // PrintSunmiUtils.fontSizeInner(LARGE)
            SunmiPrintHelper.getInstance().initPrinter()
            SunmiPrintHelper.getInstance().lineWrap(4)
            if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                PrintSunmiUtils.headerText("OrderID:" + paymentDetailsResponse.data.custom_order_id)
            } else {
                PrintSunmiUtils.headerText("OrderID:" + paymentDetailsResponse.data.order.id)
            }
            SunmiPrintHelper.getInstance().lineWrap(1)

            if (kitchenSettingModel.showOrderType) {
                PrintSunmiUtils.headerText(paymentDetailsResponse.data.order.order_type_name.toString())
                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            if (paymentDetailsResponse.data.order.order_type.equals("Online Order", true) ||
                paymentDetailsResponse.data.order.order_type.equals("OnlineWebOrder", true) ||
                paymentDetailsResponse.data.order.order_type.equals(PHONE_ORDER, true)
            ) {
                PrintSunmiUtils.headerText(paymentDetailsResponse.data.order.delivery_type.toString())
                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            if (kitchenSettingModel.showTeamMember && paymentDetailsResponse.data.order.employee != null) {
                PrintSunmiUtils.normalTextLarge("Employee:" + paymentDetailsResponse.data.order.employee)
            }
            PrintSunmiUtils.normalTextLarge(
                Constants.getReceiptFormatDateFromUTCServer(
                    requireContext(),
                    paymentDetailsResponse.data.order.created_at.toString()
                )
            )

            if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)
                    ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
            ) {
                PrintSunmiUtils.addHorizontalInnerNew()
            } else {
                PrintSunmiUtils.addHorizontalInner()
            }

            SunmiPrintHelper.getInstance().lineWrap(1)

//            receiptModel?.order?.orderItems?.let {
//                addOrdersForKitchenInner(
//                    it,
//                    kitchenReceiptPrinters.printerCategories.toCollection(arrayListOf())
//                )
//            }

            paymentDetailsResponse.data.order.order_items.let {
                addOrdersForKitchenTransitionInner(
                    it,
                    kitchenReceiptPrinters.printerCategories.toCollection(arrayListOf())
                )
            }


            if (paymentDetailsResponse.data.order.note.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
                PrintSunmiUtils.orderNoteInnerLarge(paymentDetailsResponse.data.order.note.toString())
            }

            SunmiPrintHelper.getInstance().lineWrap(1)
            if (kitchenSettingModel.showCustomerAddress != false || kitchenSettingModel.showCustomerPhone != false || kitchenSettingModel.showCustomerName != false) {
                if (paymentDetailsResponse.data.order.customer != null) {
                    PrintSunmiUtils.customerDetailsInner(true, sunmiFrameworkVersion)
                    if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(
                            1
                        )
                            ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
                    ) {
                        PrintSunmiUtils.addHorizontalInnerNew()
                    } else {
                        PrintSunmiUtils.addHorizontalInner()
                    }

                    if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(
                            1
                        )
                            ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
                    ) {
                        PrintSunmiUtils.normalText("\n")
                    }
                    if (kitchenSettingModel.showCustomerName) {
                        PrintSunmiUtils.normalTextLarge(paymentDetailsResponse.data.order.customer.firstName + " " + paymentDetailsResponse.data.order.customer.lastName)
                    }

                    if (kitchenSettingModel.showCustomerPhone) {
                        if (paymentDetailsResponse.data.order.customer.phones.isNotEmpty() == true) {
                            paymentDetailsResponse.data.order.customer.phones.get(0).phoneNumber.let {
                                PrintSunmiUtils.normalTextLarge(
                                    MethodUtils.getUSFormatNumber(it)
                                )
                            }
                        }
                    }

                    if (kitchenSettingModel.showCustomerAddress) {
                        if (paymentDetailsResponse.data.order.order_type.trim().toString()
                                .lowercase() == "Open Order".trim()
                                .toString().lowercase()
                            && paymentDetailsResponse.data.order.delivery_type.trim().toString()
                                .lowercase() == "Pickup".trim().lowercase()
                        ) {
                        } else {
                            if (paymentDetailsResponse.data.order.customer.addresses.isNotEmpty() == true) {
//                                receiptModel?.order?.customer?.addresses?.get(0)?.fullAddress?.let {
//                                    PrintSunmiUtils.normalTextLarge(
//                                        it
//                                    )
//                                }
                                paymentDetailsResponse.data.order.customer.addresses.filter { it.typeOfAddress == Constants.BILLING_ADDRESS }
                                    .forEach {

                                        if (it.typeOfAddress.equals(
                                                Constants.BILLING_ADDRESS,
                                                ignoreCase = true
                                            )
                                        ) {
                                            PrintSunmiUtils.normalTextLarge(
                                                it.fullAddress
                                            )
                                        }
                                    }
                            }
                        }
                    }

                }
            }

            SunmiPrintHelper.getInstance().lineWrap(1)
            PrintSunmiUtils.cutPaperInner()

            //  SunmiPrinterApi.getInstance().disconnectPrinter(requireContext())

        } catch (e: Exception) {
            // printerDialog.dismiss()
            e.printStackTrace()
        }

    }

    private fun setService2(
        kitchenReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String
    ) {

        if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.FoundSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper1", "FoundSunmiPrinter")

            if (!BluetoothUtil.isBlueToothPrinter) {

                LogUtil.logE("SunmiPrintHelpe1r", "isBlueToothPrinter")

                generateKitchenReceiptSunmiInner(kitchenReceiptPrinters, type)


            }

        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.CheckSunmiPrinter) {
            Handler(Looper.getMainLooper()).postDelayed({
                setService2(kitchenReceiptPrinters, type)
            }, 2000)
            LogUtil.logE("SunmiPrintHelper", "CheckSunmiPrinter")
        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.LostSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper", "LostSunmiPrinter")
        } else {
            LogUtil.logE("SunmiPrintHelper", "ELSE")
        }
    }

    private fun setService(
    ) {
        if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.FoundSunmiPrinter) {
            LogUtil.logE("SunmiPrintHelper1", "FoundSunmiPrinter")
            if (!BluetoothUtil.isBlueToothPrinter) {
                LogUtil.logE("SunmiPrintHelpe1r", "isBlueToothPrinter")
                generateInnerPrintSunmi()
            }
        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.CheckSunmiPrinter) {
            Handler(Looper.getMainLooper()).postDelayed({
                setService(
                )
            }, 2000)
            LogUtil.logE("SunmiPrintHelper", "CheckSunmiPrinter")
        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.LostSunmiPrinter) {
            LogUtil.logE("SunmiPrintHelper", "LostSunmiPrinter")
        } else {
            LogUtil.logE("SunmiPrintHelper", "ELSE")
        }
    }

    private fun generatePrint(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String
    ) {
        var builder: Builder? = null
        try {
            builder =
                Builder(
                    if (customerReceiptPrinters.name.substring(0, 6).toString()
                            .lowercase() == "TM-m30".lowercase()
                    ) {
                        "TM-m30"
                    } else {
                        customerReceiptPrinters.name
                    }, PrinterClass.language, requireActivity()
                )

            LogUtil.logE(TAG, "getVanueLogo:  ${prefProvider.getValue(Constants.VENUE_LOGO, "")}")

            if (customerSettingModel.showOrderIdTop) {
                builder.addFeedLine(1)
                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(2, 2)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    builder.addText("OrderID:" + paymentDetailsResponse.data.custom_order_id)
                } else {
                    builder.addText("OrderID:" + paymentDetailsResponse.data.order_id)
                }

                builder.addFeedLine(1)
            }


            if (customerSettingModel.showVenueLogo && prefProvider.getValue(
                    Constants.VENUE_LOGO,
                    ""
                )
                    .isNotEmpty()
            ) {
                builder.addFeedLine(1)
                builder.addTextAlign(Builder.ALIGN_CENTER)

                /* var bitmap = getBitmapFromURL(prefProvider.getValue(VENUE_LOGO, ""))*/

                val decodedString: ByteArray = android.util.Base64.decode(
                    prefProvider.getValue(Constants.VENUE_LOGO, ""),
                    android.util.Base64.DEFAULT
                )
                val bitmap: Bitmap =
                    BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

                val newBitmap = Bitmap.createScaledBitmap(bitmap!!, 210, 210, true)
                builder.addImage(
                    newBitmap, 0, 0,
                    newBitmap.width, newBitmap.height, Builder.COLOR_1, Builder.MODE_MONO,
                    Builder.HALFTONE_DITHER, 1.0
                )
            }

            builder.addFeedLine(1)


            builder.addTextFont(Builder.FONT_E)

            builder.addTextLang(Builder.LANG_EN)


            builder.addTextSize(2, 2)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            builder.addTextAlign(Builder.ALIGN_CENTER)

            addBuilderText(builder, prefProvider.getValue(Constants.BUSINESS_NAME, "").toString())

            if (customerSettingModel.showVenueAddress) {
                builder.addFeedLine(1)
                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)

                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )


                addBuilderText(
                    builder,
                    prefProvider.getValue(Constants.BUSINESS_ADDRESS, "")
                        .toString()
                )
            }
            if (customerSettingModel.showVenuePhone) {
                builder.addFeedLine(1)

                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                addBuilderText(
                    builder,
                    MethodUtils.getUSFormatNumber(
                        prefProvider.getValue(Constants.BUSINESS_PHONE_NO, "").toString()
                    )
                )
            }


            if (customerSettingModel.showWebsiteAddress) {
                paymentDetailsResponse?.data?.order?.venue_website?.let {
                    builder.addFeedLine(1)

                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_CENTER)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )
                    addBuilderText(builder, it)
                }
            }
            if (customerSettingModel.showOrderType) {
                builder.addFeedLine(1)

                builder.addTextFont(Builder.FONT_E)

                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(2, 2)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                builder.addTextAlign(Builder.ALIGN_CENTER)

                builder.addText(paymentDetailsResponse.data.order.order_type_name + "\n")
            }

            if (paymentDetailsResponse.data?.order?.order_type.equals(PHONE_ORDER, true) ||
                paymentDetailsResponse.data?.order?.order_type.equals("OnlineWebOrder", true) ||
                paymentDetailsResponse.data?.order?.order_type.equals("Online Order", true) ||
                paymentDetailsResponse.data?.order?.order_type.equals("OnlineOrder", true)
            ) {
                builder.addFeedLine(1)

                builder.addTextFont(Builder.FONT_E)

                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(2, 2)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                builder.addTextAlign(Builder.ALIGN_CENTER)

                builder.addText(paymentDetailsResponse.data.order.delivery_type + "\n")
            }

            if (customerSettingModel.fonts == Constants.LARGE) {


                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText("ReceiptID:" + paymentDetailsResponse.data.order.offline_id)

                if (customerSettingModel.showTeam && paymentDetailsResponse.data.order.employee != null) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )


                    builder.addText("Employee:" + paymentDetailsResponse?.data.order.employee)

                }

                if (customerSettingModel.showOrderTime) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )


                    builder.addText(
                        "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            paymentDetailsResponse?.data.order.created_at.toString()
                        )
                    )

                }

                if (customerSettingModel.showPrintTime) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val current = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy hh:mm:a")
                        val formatted = current.format(formatter)

                        builder.addTextLineSpace(30)
                        builder.addFeedUnit(30)
                        builder.addTextFont(Builder.FONT_E)
                        builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        addCustomerTextSize(builder, customerSettingModel.fonts)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )

                        builder.addText(
                            "Print Time:" + getCurrentTimeFromTimeZone(
                                requireContext(),
                                formatted
                            )
                        )
                    }


                }
            } else {


                builder.addFeedLine(1)
                builder.addTextFont(Builder.FONT_E)
                //  builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "ReceiptID:" + paymentDetailsResponse?.data.order.offline_id,
                        "",
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

                if (customerSettingModel.showTeam) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    //  builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )


                    builder.addText(
                        padLine(
                            if (customerSettingModel.showTeam && paymentDetailsResponse.data.order.employee != null) {
                                "Employee:" + paymentDetailsResponse?.data?.order.employee
                            } else {
                                ""
                            },
                            "",
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )

                }
                if (customerSettingModel.showOrderTime) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    //  builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )



                    builder.addText(
                        padLine(
                            if (customerSettingModel.showOrderTime) {
                                "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                                    requireContext(),
                                    paymentDetailsResponse?.data.order?.created_at.toString()
                                )
                            } else {
                                ""
                            },
                            "",
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )

                }

                if (customerSettingModel.showPrintTime) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {


                        val current = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy hh:mm:a")
                        val formatted = current.format(formatter)
                        builder.addTextLineSpace(30)
                        builder.addFeedUnit(30)
                        builder.addTextFont(Builder.FONT_E)
                        //  builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        addCustomerTextSize(builder, customerSettingModel.fonts)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )

                        builder.addText(
                            padLine(
                                if (customerSettingModel.showPrintTime) {
                                    "Print Time:" + getCurrentTimeFromTimeZone(
                                        requireContext(),
                                        formatted
                                    )
                                } else {
                                    ""
                                },
                                "",
                                if (customerSettingModel.fonts == Constants.LARGE) {
                                    24
                                } else {
                                    48
                                }
                            )
                        )

                    }
                }
            }

            builder.addFeedLine(1)

            addHorizontalLine(builder)

            paymentDetailsResponse?.data.order.order_items?.let {
                addOrderItemsTransaction(
                    builder,
                    it,
                    customerSettingModel.fonts,
                    customerSettingModel.showModifiers
                )
            }

            builder.addFeedLine(2)

            if (paymentDetailsResponse?.data.order.total_discount != null) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                builder.addText(
                    padLine(
                        "Total Discount",

                        if (paymentDetailsResponse?.data.total_discount != 0.0) {
//                            "-$" + MethodUtils.roundOffAmountString(paymentDetailsResponse?.data.order.total_discount)
                            "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse?.data.order.total_discount)
                        } else {
                            "-$" + MethodUtils.roundOffAmountString(paymentDetailsResponse?.data.order.total_discount)
                        },
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

            }

            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)
            builder.addTextFont(Builder.FONT_E)
            // builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            addCustomerTextSize(builder, customerSettingModel.fonts)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )

            var totalDiscount: Double = 0.0
            paymentDetailsResponse?.data.order.total_discount?.let {
                totalDiscount = it
            }
            builder.addText(
                padLine(
                    "Sub Total",
                    "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.sub_total),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        24
                    } else {
                        48
                    }
                )
            )


            if (paymentDetailsResponse.data?.tax_amount != null) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Tax",
                        "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.tax_amount),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }

            if (paymentDetailsResponse.data?.service_charge_amount != null && paymentDetailsResponse.data.order.service_charge_enabled) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Service Charge",
                        "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.service_charge_amount),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }

            if (paymentDetailsResponse.data?.tips != 0.0) {

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Tip",
                        "$" + paymentDetailsResponse.data.tips?.let {
                            MethodUtils.roundOffAmountString(
                                it
                            )
                        },
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }




            if (paymentDetailsResponse.data?.cash_discount_or_surcharge != null && paymentDetailsResponse.data?.cash_discount_or_surcharge != 0.0
                && customerSettingModel.showCashDisSurCharg
            ) {

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                if (paymentDetailsResponse?.data?.payment_type.lowercase() == "Card".lowercase() && paymentDetailsResponse.data.cash_discount_type.lowercase() == "SurCharge".lowercase()) {
                    builder.addText(
                        padLine(
                            Constants.SURCHARGE_TEXT,
                            if (paymentDetailsResponse.data.cash_discount_or_surcharge != 0.0) {
                                "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data?.cash_discount_or_surcharge)
                            } else {
                                "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.order?.cash_discount_or_surcharge)
                            },
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )

                } else if (paymentDetailsResponse?.data?.payment_type.lowercase() == "Cash".lowercase() && paymentDetailsResponse.data.cash_discount_type.lowercase() == "CashDiscount".lowercase()) {

                    builder.addText(
                        padLine(
                            "Cash Discount",
                            if (paymentDetailsResponse.data.cash_discount_or_surcharge != 0.0) {
                                "-$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data?.cash_discount_or_surcharge)
                            } else {
                                "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.order?.cash_discount_or_surcharge)
                            },
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
                }
            }


            if (paymentDetailsResponse?.data?.is_loyalty_applied == true) {

                if (paymentDetailsResponse?.data?.loyalty_amount != 0.0) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )

                    builder.addText(
                        padLine(
                            "Used Loyalty Amount",
                            "-$" + paymentDetailsResponse.data?.loyalty_amount?.let {
                                MethodUtils.roundOffAmountString(
                                    it.toDouble()
                                )
                            },
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
                }

                if (paymentDetailsResponse?.data?.used_reward_points != 0) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )

                    builder.addText(
                        padLine(
                            "Used Loyalty Points",
                            paymentDetailsResponse?.data?.used_reward_points.toString(),
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
                }
            }

            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)



            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)

            builder.addTextFont(Builder.FONT_E)
            // builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            addCustomerTextSize(builder, customerSettingModel.fonts)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            var totalAmt =
                MethodUtils.roundOffAmountDouble(paymentDetailsResponse.data.amount + paymentDetailsResponse.data.tips)
            /* if (receiptModel?.order?.totalDiscount != 0.0) {
                 totalAmt =
                     (totalAmt - MethodUtils.roundOffAmountDouble(receiptModel?.order?.totalDiscount!!))

             }*/

            builder.addText(
                padLine(
                    "Total Price",
                    "$" + MethodUtils.roundOffAmountString(totalAmt),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        24
                    } else {
                        48
                    }
                )
            )



            if (paymentDetailsResponse?.data?.order.refund_detail != null && paymentDetailsResponse?.data?.order?.refund_detail?.refunded_amount != 0.0 && customerSettingModel.showRefundAmount) {
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Refund Amount",
                        "-$" + MethodUtils.roundOffAmountString(paymentDetailsResponse?.data?.order?.refund_detail?.refunded_amount),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }

            if (paymentDetailsResponse.data.order?.total_tips == 0.0) {
                builder.addFeedLine(1)
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                var tip = ""

                if (paymentDetailsResponse.data.order?.total_tips != 0.0) {
                    tip = paymentDetailsResponse.data.order?.total_tips.toString()
                }
                builder.addText(
                    padLine(
                        "Tip",
                        if (customerSettingModel.showTipLineForCash) {
                            "_____________"
                        } else {
                            ""
                        },
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }


            if (customerSettingModel.showTipSuggestion) {
                builder.addFeedLine(1)
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)

                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )
                builder.addText(
                    padLine(
                        "Additional Tips",
                        "",
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )


                builder.addFeedLine(1)

                addHorizontalLine(builder)

                if (tipsList.isNotEmpty()) {
                    addTipsList(
                        builder,
                        tipsList,
                        paymentDetailsResponse.data.order.total_amount,
                        customerSettingModel.fonts
                    )

                }
            }

            builder.addFeedLine(1)

            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)

            builder.addTextFont(Builder.FONT_E)
            // builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            addCustomerTextSize(builder, customerSettingModel.fonts)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )



            builder.addText(
                padLine(
                    "Transaction ID",
                    "" + paymentDetailsResponse.data.id,
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        24
                    } else {
                        48
                    }
                )
            )



            if (paymentDetailsResponse.data.payment_type.lowercase() == "Card".lowercase()) {

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Transaction Type",
                        "Card",
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

                if (!(paymentDetailsResponse.data.card_name).isNullOrBlank()) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )

                    builder.addText(
                        padLine(
                            "",
                            paymentDetailsResponse.data.card_name,
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
                }

                if (!(paymentDetailsResponse.data.card_type).isNullOrBlank()) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )

                    builder.addText(
                        padLine(
                            "",
                            paymentDetailsResponse.data.card_type,
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
                }

                if (!(paymentDetailsResponse.data.card_number).isNullOrBlank()) {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )

                    builder.addText(
                        padLine(
                            "",
                            paymentDetailsResponse.data.card_number,
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
                }

            } else {


                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                builder.addText(
                    padLine(
                        "Transaction Type",
                        paymentDetailsResponse.data.payment_type ?: "Cash",
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }
            if (customerSettingModel.showCustomerAddress or customerSettingModel.showCustomerPhone or customerSettingModel.showCustomerName) {


                if (paymentDetailsResponse?.data.order?.customer != null) {


                    builder.addFeedLine(1)
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)

                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, customerSettingModel.fonts)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.TRUE,
                        Builder.COLOR_1
                    )
                    builder.addText(
                        padLine(
                            "Customer Details",
                            "",
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )

                    builder.addFeedLine(1)

                    addHorizontalLine(builder)
                    builder.addFeedLine(1)
                    if (customerSettingModel.showCustomerName) {

                        builder.addTextFont(Builder.FONT_E)
                        // builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        addCustomerTextSize(builder, customerSettingModel.fonts)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )

                        builder.addText(paymentDetailsResponse?.data.order?.customer.firstName + " " + paymentDetailsResponse?.data.order?.customer.lastName)
                    }

                    if (customerSettingModel.showCustomerPhone) {
                        if (paymentDetailsResponse?.data?.order?.customer?.phones?.isNotEmpty() && paymentDetailsResponse?.data?.order?.customer?.phones?.get(
                                paymentDetailsResponse?.data?.order?.customer.phones?.size - 1
                            ).phoneNumber.isNotEmpty()
                        ) {
                            builder.addTextLineSpace(30)
                            builder.addFeedUnit(30)
                            builder.addTextFont(Builder.FONT_E)
                            //builder.addTextAlign(Builder.ALIGN_LEFT)
                            builder.addTextLang(Builder.LANG_EN)
                            addCustomerTextSize(builder, customerSettingModel.fonts)
                            builder.addTextStyle(
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.COLOR_1
                            )


                            var phoneNoFormatted = MethodUtils.getUSFormatNumber(
                                paymentDetailsResponse?.data?.order?.customer?.phones?.get(
                                    paymentDetailsResponse?.data?.order?.customer.phones?.size - 1
                                ).phoneNumber
                            )
                            builder.addText(phoneNoFormatted)

                        }


                    }

                    if (customerSettingModel.showCustomerAddress) {
                        if (paymentDetailsResponse?.data?.order?.customer?.addresses?.isNotEmpty() == true) {

                            builder.addTextLineSpace(30)
                            builder.addFeedUnit(30)
                            builder.addTextFont(Builder.FONT_E)
                            //builder.addTextAlign(Builder.ALIGN_LEFT)
                            builder.addTextLang(Builder.LANG_EN)
                            addCustomerTextSize(builder, customerSettingModel.fonts)
                            builder.addTextStyle(
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.FALSE,
                                Builder.COLOR_1
                            )

                            paymentDetailsResponse.data.order.customer.addresses.filter { it.typeOfAddress == SHIPPING_ADDRESS }
                                .forEach {

                                    if (it.typeOfAddress.equals(
                                            SHIPPING_ADDRESS,
                                            ignoreCase = true
                                        )
                                    ) {
                                        builder.addText(
                                            it.fullAddress
                                        )
                                    }
                                }


                        }
                    }


                    /*  if (customerSettingModel.showCustomerAddress) {
                          if (paymentDetailsResponse?.data.order?.customer.addresses?.isNotEmpty() == true) {

                              builder.addTextLineSpace(30)
                              builder.addFeedUnit(30)
                              builder.addTextFont(Builder.FONT_E)
                              //builder.addTextAlign(Builder.ALIGN_LEFT)
                              builder.addTextLang(Builder.LANG_EN)
                              addCustomerTextSize(builder, customerSettingModel.fonts)
                              builder.addTextStyle(
                                  Builder.FALSE,
                                  Builder.FALSE,
                                  Builder.FALSE,
                                  Builder.COLOR_1
                              )

                              builder.addText(orderDetailsResponse?.data?.customer?.addresses?.get(0)?.fullAddress)
                          }
                      }*/

                }
            }


            if (paymentDetailsResponse?.data.order?.note != null && paymentDetailsResponse?.data.order?.note != "" && customerSettingModel.showOrderNote) {

                builder.addFeedLine(2)
                builder.addTextFont(Builder.FONT_B)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, customerSettingModel.fonts)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )
                builder.addText("Order Note")
                builder.addFeedLine(1)

                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                builder.addTextSize(1, 1)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )

                builder.addText(paymentDetailsResponse?.data.order?.note)


            }

            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)

            builder.addTextFont(Builder.FONT_E)
            // builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            addCustomerTextSize(builder, customerSettingModel.fonts)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )

            //customer signature line.
            builder.addText(
                padLine(
                    "Customer Signature",
                    addHorizontalHalfCustomerReceiptLine(customerSettingModel.fonts),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        24
                    } else {
                        48
                    }
                )
            )


            if (customerSettingModel.showQrCode) {
                builder.addFeedLine(1)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                val bitmap =
                    generateQRCode(paymentDetailsResponse?.data.order?.digital_receipt_url.toString())
                val newBitmap = Bitmap.createScaledBitmap(bitmap, 210, 210, true)
                builder.addImage(
                    newBitmap, 0, 0,
                    newBitmap.width, newBitmap.height, Builder.COLOR_1, Builder.MODE_MONO,
                    Builder.HALFTONE_DITHER, 1.0
                )
            }

            builder.addFeedLine(2)

            builder.addCut(Builder.CUT_FEED)

            val status = IntArray(1)
            val battery = IntArray(1)


            try {
                PrinterClass.getPrinter()?.sendData(
                    builder,
                    PrinterClass.BLUETOOTH_TIMEOUT, status, battery
                )

                PrinterClass.closePrinter()

                //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
            } catch (e: Exception) {
                PrinterClass.closePrinter()
                e.printStackTrace()
                LogUtil.logE(TAG, "PrinterError: " + e.localizedMessage)
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun generatePrintSunmi(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String
    ) {
        try {

            PrintSunmiUtils.fontSize(customerSettingModel.fonts)

            SunmiPrinterApi.getInstance().printerInit()

            if (customerSettingModel.showOrderIdTop) {
                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    PrintSunmiUtils.orderIdLarge("OrderID:" + paymentDetailsResponse.data.custom_order_id)
                } else {
                    PrintSunmiUtils.orderIdLarge("OrderID:" + paymentDetailsResponse.data.order_id)
                }
                SunmiPrinterApi.getInstance().lineWrap(1)
            }

            if (customerSettingModel.showVenueLogo && prefProvider.getValue(
                    Constants.VENUE_LOGO,
                    ""
                )
                    .isNotEmpty()
            ) {

                printBusinessLogo()

            }

            PrintSunmiUtils.printBusinessDetails(
                prefProvider.getValue(Constants.BUSINESS_NAME, ""),
                if (customerSettingModel.showVenueAddress) prefProvider.getValue(
                    Constants.BUSINESS_ADDRESS,
                    ""
                ) else "",
                if (customerSettingModel.showVenuePhone) prefProvider.getValue(
                    Constants.BUSINESS_PHONE_NO,
                    ""
                ) else ""
            )

            if (paymentDetailsResponse.data.order.venue_website.isNotEmpty() && customerSettingModel.showWebsiteAddress) {
                PrintSunmiUtils.venueWebsite(paymentDetailsResponse.data.order.venue_website)
            } else {
                SunmiPrinterApi.getInstance().lineWrap(1)
            }
            SunmiPrinterApi.getInstance().lineWrap(1)
            if (customerSettingModel.showOrderType) {
                PrintSunmiUtils.printOrderType(paymentDetailsResponse.data.order.order_type_name.trim())
                SunmiPrinterApi.getInstance().lineWrap(1)
            }

            if (paymentDetailsResponse.data.order.order_type.equals(PHONE_ORDER, true) ||
                paymentDetailsResponse.data.order.order_type.equals("OnlineWebOrder", true) ||
                paymentDetailsResponse.data.order.order_type.equals("Online Order", true) ||
                paymentDetailsResponse.data.order.order_type.equals("OnlineOrder", true)
            ) {
                PrintSunmiUtils.printOrderType(paymentDetailsResponse.data.order.delivery_type.trim())
                SunmiPrinterApi.getInstance().lineWrap(1)
            }


            if (customerSettingModel.fonts == Constants.LARGE) {


                PrintSunmiUtils.receiptID("ReceiptID:" + paymentDetailsResponse.data.order.offline_id)

                if (customerSettingModel.showTeam && paymentDetailsResponse.data.order.employee != null) {
                    PrintSunmiUtils.employee("Employee:" + paymentDetailsResponse?.data.order.employee)
                }

                if (customerSettingModel.showOrderTime) {

                    PrintSunmiUtils.orderId(
                        "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            paymentDetailsResponse?.data.order.created_at.toString()
                        )
                    )

                    if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(
                            1
                        )
                            ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
                    ) {
                        PrintSunmiUtils.normalText("\n")
                    }
                }

                if (customerSettingModel.showPrintTime) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val current = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy hh:mm:a")
                        val formatted = current.format(formatter)

                        PrintSunmiUtils.orderId(
                            "Print Time:" + getCurrentTimeFromTimeZone(
                                requireContext(),
                                formatted
                            )
                        )
                    }
                    if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(
                            1
                        )
                            ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
                    ) {
                        PrintSunmiUtils.normalText("\n")
                    }

                }
            } else {

                val str = padLine(
                    "ReceiptID:" + paymentDetailsResponse?.data.order.offline_id,
                    "",
                    if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                ).toString().trim()

                PrintSunmiUtils.orderId(str.trim())

                if (customerSettingModel.showTeam) {


                    val empName = padLine(
                        if (customerSettingModel.showTeam && paymentDetailsResponse.data.order.employee != null) {
                            "Employee:" + paymentDetailsResponse?.data?.order.employee
                        } else {
                            ""
                        },
                        "", if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                    ).toString()

                    PrintSunmiUtils.employee(empName)

                }
                if (customerSettingModel.showOrderTime) {


                    val orderTime = padLine(
                        if (customerSettingModel.showOrderTime) {
                            "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                                requireContext(),
                                paymentDetailsResponse?.data.order?.created_at.toString()
                            )
                        } else {
                            ""
                        },
                        "", if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                    ).toString()

                    PrintSunmiUtils.orderTime(orderTime)


                }

                if (customerSettingModel.showPrintTime) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        val printTime = padLine(
                            if (customerSettingModel.showPrintTime) {
                                "Print Time:" + getCurrentTimeFromTimeZone(
                                    requireContext(),
                                    MethodUtils.formatted()
                                )
                            } else {
                                ""
                            },
                            "", if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                        ).toString()

                        PrintSunmiUtils.orderTime(printTime)

                    }
                }
            }

            PrintSunmiUtils.addHorizontal()
            if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)
                    ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
            ) {
                PrintSunmiUtils.normalText("\n")
            }

            paymentDetailsResponse.data.order.order_items.let {
                addOrderItemsTransaction(
                    it,
                    customerSettingModel.fonts,
                    customerSettingModel.showModifiers
                )
            }

            SunmiPrinterApi.getInstance().lineWrap(2)


            if (paymentDetailsResponse?.data.order.total_discount != null) {


                val str1 = padLine(
                    "Total Discount",

                    if (paymentDetailsResponse?.data.total_discount != 0.0) {
//                        "-$" + MethodUtils.roundOffAmountString(paymentDetailsResponse?.data.order.total_discount)
                        "-$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.order.total_discount)
                    } else {
                        "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.order.total_discount)
                    }, if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                ).toString()
                PrintSunmiUtils.totalDiscount(str1)

            }

            val sub = padLine(
                "Sub Total",
                "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.sub_total),
                if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
            ).toString()

            PrintSunmiUtils.subTotal(sub)



            if (paymentDetailsResponse.data?.tax_amount != null) {


                PrintSunmiUtils.tax(
                    padLine(
                        "Tax",
                        "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.tax_amount),
                        if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                    ).toString()
                )
            }

            if (paymentDetailsResponse.data?.service_charge_amount != null && paymentDetailsResponse.data.order.service_charge_enabled) {

                PrintSunmiUtils.serviceCharge(
                    padLine(
                        "Service Charge",
                        "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.service_charge_amount),
                        if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                    ).toString()
                )
            }

            if (paymentDetailsResponse.data?.tips != 0.0) {

                PrintSunmiUtils.tip(
                    padLine(
                        "Tip",
                        "$" + paymentDetailsResponse.data.tips?.let {
                            MethodUtils.roundOffAmountString(
                                it
                            )
                        }, if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                    ).toString()
                )
            }




            if (paymentDetailsResponse.data?.cash_discount_or_surcharge != null && customerSettingModel.showCashDisSurCharg) {

                if (paymentDetailsResponse?.data?.payment_type.lowercase() == "Card".lowercase() && paymentDetailsResponse?.data?.cash_discount_type.lowercase() == "SurCharge".lowercase()) {
                    val surCharge =
                        padLine(
                            Constants.SURCHARGE_TEXT,
                            if (paymentDetailsResponse.data.cash_discount_or_surcharge != 0.0) {
                                "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data?.cash_discount_or_surcharge)
                            } else {
                                "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.order?.cash_discount_or_surcharge)
                            }, if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                        ).toString()

                    PrintSunmiUtils.surCharge(surCharge)


                } else if (paymentDetailsResponse?.data?.payment_type.lowercase() == "Cash".lowercase() && paymentDetailsResponse?.data?.cash_discount_type.lowercase() == "CashDiscount".lowercase()) {


                    val cashDisc = padLine(
                        "Cash Discount",
                        if (paymentDetailsResponse.data.cash_discount_or_surcharge != 0.0) {
                            "-$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data?.cash_discount_or_surcharge)
                        } else {
                            "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.order?.cash_discount_or_surcharge)
                        }, if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                    ).toString()


                    PrintSunmiUtils.cashDiscount(cashDisc)

                }
            }


            if (paymentDetailsResponse?.data?.is_loyalty_applied == true) {

                if (paymentDetailsResponse?.data?.loyalty_amount != 0.0) {

                    val loyaltyAmount = padLine(
                        "Used Loyalty Amount",
                        "-$" + paymentDetailsResponse.data?.loyalty_amount?.let {
                            MethodUtils.roundOffAmountString(
                                it.toDouble()
                            )
                        }, if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                    ).toString()
                    PrintSunmiUtils.loyaltyAmount(loyaltyAmount)

                }

                if (paymentDetailsResponse?.data?.used_reward_points != 0) {

                    val loyaltyPoint = padLine(
                        "Used Loyalty Points",
                        paymentDetailsResponse?.data?.used_reward_points.toString(),
                        if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                    ).toString()

                    PrintSunmiUtils.loyaltyPoint(loyaltyPoint)

                }
            }

            SunmiPrinterApi.getInstance().lineWrap(1)
            val totalAmt =
                MethodUtils.roundOffAmountDouble(paymentDetailsResponse.data.amount + paymentDetailsResponse.data.tips)

            PrintSunmiUtils.totalPrice(
                padLine(
                    "Total Price",
                    "$" + MethodUtils.roundOffAmountString(totalAmt),
                    if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                ).toString()
            )


            if (paymentDetailsResponse?.data?.order.refund_detail != null && paymentDetailsResponse?.data?.order?.refund_detail?.refunded_amount != 0.0 && customerSettingModel.showRefundAmount) {

                PrintSunmiUtils.refundAmount(
                    padLine(
                        "Refund Amount",
                        "-$" + MethodUtils.roundOffAmountString(paymentDetailsResponse?.data?.order?.refund_detail?.refunded_amount),
                        if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                    ).toString()
                )
                SunmiPrinterApi.getInstance().lineWrap(1)
            } else {
                SunmiPrinterApi.getInstance().lineWrap(1)
            }

            if (paymentDetailsResponse.data.order?.total_tips == 0.0) {
                if (customerSettingModel.showTipLineForCash) {

                    if (customerSettingModel.fonts == Constants.LARGE) {
                        PrintSunmiUtils.tips("Tip       _____________")
                        SunmiPrinterApi.getInstance().lineWrap(1)
                    } else {
                        PrintSunmiUtils.tips("Tip                               _____________")
                    }

                }
            }


            if (customerSettingModel.showTipSuggestion) {
                SunmiPrinterApi.getInstance().lineWrap(1)
                PrintSunmiUtils.additionalTips()
                if (tipsList.isNotEmpty()) {
                    PrintSunmiUtils.addTipList(
                        tipsList,
                        paymentDetailsResponse.data.order.total_amount,
                        customerSettingModel.fonts
                    )
                }
                SunmiPrinterApi.getInstance().lineWrap(1)
            }


            val tranId = padLine(
                "Transaction ID",
                "" + paymentDetailsResponse.data.id,
                if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
            ).toString()

            PrintSunmiUtils.transactionId(tranId)



            if (paymentDetailsResponse.data.payment_type.lowercase() == "Card".lowercase()) {

                val tranType = padLine(
                    "Transaction Type",
                    "Card", if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                ).toString()

                PrintSunmiUtils.transactionType(tranType)

                val str12 =
                    paymentDetailsResponse.data.card_name ?: ""


                val str13 =
                    paymentDetailsResponse.data.card_type ?: ""


                val str14 =
                    paymentDetailsResponse.data.card_number ?: ""


                PrintSunmiUtils.cardDetails(
                    str12, str13, str14
                )


            } else {

                PrintSunmiUtils.transactionType(
                    padLine(
                        "Transaction Type",
                        paymentDetailsResponse.data.payment_type ?: "Cash",
                        if (customerSettingModel.fonts == Constants.LARGE) 23 else 48
                    ).toString()
                )

            }


            SunmiPrinterApi.getInstance().lineWrap(1)

            if (customerSettingModel.showCustomerAddress or customerSettingModel.showCustomerPhone or customerSettingModel.showCustomerName) {


                if (paymentDetailsResponse?.data.order?.customer != null) {

                    PrintSunmiUtils.customerDetails()
                    if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(
                            1
                        )
                            ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
                    ) {
                        PrintSunmiUtils.normalText("\n")
                    }

                    if (customerSettingModel.showCustomerName) {

                        PrintSunmiUtils.customerName(paymentDetailsResponse?.data.order?.customer.firstName + " " + paymentDetailsResponse?.data.order?.customer.lastName)
                    }

                    if (customerSettingModel.showCustomerPhone) {
                        if (paymentDetailsResponse?.data?.order?.customer?.phones?.isNotEmpty()) {

                            val phoneNoFormatted = MethodUtils.getUSFormatNumber(
                                paymentDetailsResponse.data.order.customer.phones.get(
                                    paymentDetailsResponse.data.order.customer.phones.size - 1
                                ).phoneNumber
                            )
                            PrintSunmiUtils.customerPhone(phoneNoFormatted)

                        }


                    }

                    if (customerSettingModel.showCustomerAddress) {
                        if (paymentDetailsResponse?.data?.order?.customer?.addresses?.isNotEmpty() == true) {


                            paymentDetailsResponse.data.order.customer.addresses.filter { it.typeOfAddress == SHIPPING_ADDRESS }
                                .forEach {

                                    if (it.typeOfAddress.equals(
                                            SHIPPING_ADDRESS,
                                            ignoreCase = true
                                        )
                                    ) {
                                        PrintSunmiUtils.customerAddress(
                                            it.fullAddress
                                        )
                                    }
                                }


//                            PrintSunmiUtils.customerAddress(
//                                paymentDetailsResponse?.data?.order?.customer?.addresses?.get(
//                                    paymentDetailsResponse?.data?.order?.customer?.addresses?.size - 1
//                                )?.fullAddress
//                            )
                        }
                    }

                    SunmiPrinterApi.getInstance().lineWrap(2)

                }
            }


            if (paymentDetailsResponse?.data.order?.note != null && paymentDetailsResponse?.data.order?.note != "" && customerSettingModel.showOrderNote) {
                PrintSunmiUtils.orderNote(paymentDetailsResponse?.data.order?.note)
                SunmiPrinterApi.getInstance().lineWrap(2)
            }

            val str8 = padLine(
                "Customer Signature",
                "     _________________________",
                48
            ).toString()

            PrintSunmiUtils.customerSignature(str8)
            if (customerSettingModel.showQrCode) {

                PrintSunmiUtils.qrCode(paymentDetailsResponse?.data.order?.digital_receipt_url.toString())

            }

            PrintSunmiUtils.cutPaper()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun generateInnerPrintSunmi(
    ) {
        try {

            PrintSunmiUtils.fontSizeInner(customerSettingModel.fonts)

            SunmiPrintHelper.getInstance().initPrinter()

            if (customerSettingModel.showOrderIdTop) {
                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    PrintSunmiUtils.headerText("OrderID:" + paymentDetailsResponse.data.custom_order_id)
                } else {
                    PrintSunmiUtils.headerText("OrderID:" + paymentDetailsResponse.data.order_id)
                }
            }
            SunmiPrintHelper.getInstance().lineWrap(1)


            if (customerSettingModel.showVenueLogo && prefProvider.getValue(
                    Constants.VENUE_LOGO,
                    ""
                )
                    .isNotEmpty()
            ) {


                PrintSunmiUtils.printLogoInner(prefProvider.getValue(Constants.VENUE_LOGO, ""))

            }

            PrintSunmiUtils.printBusinessDetailsInner(
                prefProvider.getValue(Constants.BUSINESS_NAME, ""),
                if (customerSettingModel.showVenueAddress) prefProvider.getValue(
                    Constants.BUSINESS_ADDRESS,
                    ""
                ) else "", prefProvider.getValue(
                    Constants.BUSINESS_PHONE_NO,
                    ""
                )
                /* if (customerSettingModel.showVenuePhone) prefProvider.getValue(
                     Constants.BUSINESS_PHONE_NO,
                     ""
                 ) else ""*/
            )

            if (paymentDetailsResponse.data.order.venue_website.isNotEmpty() && customerSettingModel.showWebsiteAddress) {
                PrintSunmiUtils.normalTextCenter(paymentDetailsResponse.data.order.venue_website)
            } else {
                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            if (customerSettingModel.showOrderType) {
                PrintSunmiUtils.headerText(paymentDetailsResponse.data.order.order_type_name.trim())
                if (!paymentDetailsResponse.data.order.order_type_name.trim()
                        .contains("Phone", true)
                ) {
                    SunmiPrintHelper.getInstance().lineWrap(1)
                }
            }

            if (paymentDetailsResponse.data.order.order_type.trim()
                    .equals("Online Order", true) ||
                paymentDetailsResponse.data.order.order_type.trim()
                    .equals("OnlineWebOrder", true) ||
                paymentDetailsResponse.data.order.order_type.trim().equals(PHONE_ORDER, true)
            ) {
                PrintSunmiUtils.headerText(paymentDetailsResponse.data.order.delivery_type)
            }

            if (paymentDetailsResponse.data.order.order_type_name.trim().contains("Phone", true)) {
                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            if (customerSettingModel.fonts == Constants.LARGE) {


                PrintSunmiUtils.normalText("ReceiptID:" + paymentDetailsResponse.data.order.offline_id)


                if (customerSettingModel.showTeam && paymentDetailsResponse.data.order.employee != null) {
                    PrintSunmiUtils.normalText("Employee:" + paymentDetailsResponse?.data.order.employee)
                }

                if (customerSettingModel.showOrderTime) {

                    PrintSunmiUtils.normalText(
                        "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            paymentDetailsResponse?.data.order.created_at.toString()
                        )
                    )


                }

                if (customerSettingModel.showPrintTime) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        PrintSunmiUtils.normalText(
                            "Print Time:" + getCurrentTimeFromTimeZone(
                                requireContext(),
                                MethodUtils.formatted()
                            )
                        )
                    }
                }

            } else {

                PrintSunmiUtils.normalText("ReceiptID:" + paymentDetailsResponse?.data.order.offline_id)

                if (customerSettingModel.showTeam) {


                    val empName = padLine(
                        if (customerSettingModel.showTeam && paymentDetailsResponse.data.order.employee != null) {
                            "Employee:" + paymentDetailsResponse?.data?.order.employee
                        } else {
                            ""
                        },
                        "", PrintSunmiUtils.lineChar()
                    ).toString()

                    PrintSunmiUtils.normalText(empName)

                }
                if (customerSettingModel.showOrderTime) {


                    val orderTime = padLine(
                        if (customerSettingModel.showOrderTime) {
                            "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                                requireContext(),
                                paymentDetailsResponse?.data.order?.created_at.toString()
                            )
                        } else {
                            ""
                        },
                        "", PrintSunmiUtils.lineChar()
                    ).toString()

                    PrintSunmiUtils.normalText(orderTime)

                }

                if (customerSettingModel.showPrintTime) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        val printTime = padLine(
                            if (customerSettingModel.showPrintTime) {
                                "Print Time:" + getCurrentTimeFromTimeZone(
                                    requireContext(),
                                    MethodUtils.formatted()
                                )
                            } else {
                                ""
                            },
                            "", PrintSunmiUtils.lineChar()
                        ).toString()

                        PrintSunmiUtils.normalText(printTime)

                    }
                }
            }

            if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)
                    ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
            ) {
                PrintSunmiUtils.addHorizontalInnerNew()
                PrintSunmiUtils.normalText("\n")
            } else {
                PrintSunmiUtils.addHorizontalInner()
            }


            if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)
                    ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
            ) {
                paymentDetailsResponse.data.order.order_items.let {
                    addOrderItemsTransactionInnerNew(
                        it,
                        customerSettingModel.fonts,
                        customerSettingModel.showModifiers
                    )
                }
            } else {
                paymentDetailsResponse.data.order.order_items.let {
                    addOrderItemsTransactionInner(
                        it,
                        customerSettingModel.fonts,
                        customerSettingModel.showModifiers
                    )
                }
            }

            SunmiPrintHelper.getInstance().lineWrap(2)


            if (paymentDetailsResponse?.data.order.total_discount != null) {


                val str1 = padLine(
                    "Total Discount",

                    if (paymentDetailsResponse.data.total_discount != 0.0) {
                        "-$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.total_discount)
                    } else {
                        "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.order.total_discount)
                    }, PrintSunmiUtils.lineChar()
                ).toString()

                if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)
                        ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
                ) {
                    PrintSunmiUtils.normalTextNew(str1)
                } else {
                    PrintSunmiUtils.normalText(str1)
                }
            }

            val sub = padLine(
                "Sub Total",
                "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.sub_total),
                PrintSunmiUtils.lineChar()
            ).toString()

            if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)
                    ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
            ) {
                PrintSunmiUtils.normalTextNew(sub)
            } else {
                PrintSunmiUtils.normalText(sub)
            }



            if (paymentDetailsResponse.data?.tax_amount != null) {

                if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)
                        ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
                ) {
                    PrintSunmiUtils.normalTextNew(
                        padLine(
                            "Tax",
                            "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.tax_amount),
                            PrintSunmiUtils.lineChar()
                        ).toString()
                    )
                } else {
                    PrintSunmiUtils.normalText(
                        padLine(
                            "Tax",
                            "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.tax_amount),
                            PrintSunmiUtils.lineChar()
                        ).toString()
                    )
                }

            }

            if (paymentDetailsResponse.data?.service_charge_amount != null && paymentDetailsResponse.data.order.service_charge_enabled) {


                if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)
                        ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
                ) {

                    PrintSunmiUtils.normalTextNew(
                        padLine(
                            "Service Charge",
                            "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.service_charge_amount),
                            PrintSunmiUtils.lineChar()
                        ).toString()
                    )
                } else {

                    PrintSunmiUtils.normalText(
                        padLine(
                            "Service Charge",
                            "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.service_charge_amount),
                            PrintSunmiUtils.lineChar()
                        ).toString()
                    )
                }

            }

            if (paymentDetailsResponse.data?.tips != 0.0) {


                if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)
                        ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
                ) {
                    PrintSunmiUtils.normalTextNew(
                        padLine(
                            "Tip",
                            "$" + paymentDetailsResponse.data.tips?.let {
                                MethodUtils.roundOffAmountString(
                                    it
                                )
                            }, PrintSunmiUtils.lineChar()
                        ).toString()
                    )
                } else {
                    PrintSunmiUtils.normalText(
                        padLine(
                            "Tip",
                            "$" + paymentDetailsResponse.data.tips?.let {
                                MethodUtils.roundOffAmountString(
                                    it
                                )
                            }, PrintSunmiUtils.lineChar()
                        ).toString()
                    )
                }
            }




            if (paymentDetailsResponse.data?.cash_discount_or_surcharge != null && customerSettingModel.showCashDisSurCharg) {

                if (paymentDetailsResponse?.data?.payment_type.lowercase() == "Card".lowercase() && paymentDetailsResponse?.data?.cash_discount_type.lowercase() == "SurCharge".lowercase()) {
                    val surCharge =
                        padLine(
                            Constants.SURCHARGE_TEXT,
                            if (paymentDetailsResponse.data.cash_discount_or_surcharge != 0.0) {
                                "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data?.cash_discount_or_surcharge)
                            } else {
                                "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.order?.cash_discount_or_surcharge)
                            }, PrintSunmiUtils.lineChar()
                        ).toString()


                    if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(
                            1
                        )
                            ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
                    ) {
                        PrintSunmiUtils.normalTextNew(surCharge)
                    } else {
                        PrintSunmiUtils.normalText(surCharge)
                    }
                } else if (paymentDetailsResponse?.data?.payment_type.lowercase() == "Cash".lowercase() && paymentDetailsResponse?.data?.cash_discount_type.lowercase() == "CashDiscount".lowercase()) {


                    val cashDisc = padLine(
                        "Cash Discount",
                        if (paymentDetailsResponse.data.cash_discount_or_surcharge != 0.0) {
                            "-$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data?.cash_discount_or_surcharge)
                        } else {
                            "$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.order?.cash_discount_or_surcharge)
                        }, PrintSunmiUtils.lineChar()
                    ).toString()

                    if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(
                            1
                        )
                            ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
                    ) {
                        PrintSunmiUtils.normalTextNew(cashDisc)
                    } else {
                        PrintSunmiUtils.normalText(cashDisc)
                    }
                }
            }


            if (paymentDetailsResponse?.data?.is_loyalty_applied == true) {

                if (paymentDetailsResponse?.data?.loyalty_amount != 0.0) {

                    val loyaltyAmount = padLine(
                        "Used Loyalty Amount",
                        "-$" + paymentDetailsResponse.data?.loyalty_amount?.let {
                            MethodUtils.roundOffAmountString(
                                it.toDouble()
                            )
                        }, PrintSunmiUtils.lineChar()
                    ).toString()
                    if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(
                            1
                        )
                            ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
                    ) {
                        PrintSunmiUtils.normalTextNew(loyaltyAmount)
                    } else {
                        PrintSunmiUtils.normalText(loyaltyAmount)
                    }

                }

                if (paymentDetailsResponse?.data?.used_reward_points != 0) {

                    val loyaltyPoint = padLine(
                        "Used Loyalty Points",
                        paymentDetailsResponse?.data?.used_reward_points.toString(),
                        PrintSunmiUtils.lineChar()
                    ).toString()

                    if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(
                            1
                        )
                            ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
                    ) {
                        PrintSunmiUtils.normalTextNew(loyaltyPoint)
                    } else {
                        PrintSunmiUtils.normalText(loyaltyPoint)
                    }
                }
            }

            SunmiPrintHelper.getInstance().lineWrap(1)
            val totalAmt =
                MethodUtils.roundOffAmountDouble(paymentDetailsResponse.data.amount + paymentDetailsResponse.data.tips)

            if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)
                    ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
            ) {
                PrintSunmiUtils.boldTextNew(
                    padLine(
                        "Total Price",
                        "$" + MethodUtils.roundOffAmountString(totalAmt),
                        PrintSunmiUtils.lineChar()
                    ).toString()
                )
            } else {
                PrintSunmiUtils.boldText(
                    padLine(
                        "Total Price",
                        "$" + MethodUtils.roundOffAmountString(totalAmt),
                        PrintSunmiUtils.lineChar()
                    ).toString()
                )
            }

            if (paymentDetailsResponse?.data?.order.refund_detail != null && paymentDetailsResponse.data.order.refund_detail.refunded_amount != 0.0 && customerSettingModel.showRefundAmount) {


                if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)
                        ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
                ) {
                    PrintSunmiUtils.boldTextNew(
                        padLine(
                            "Refund Amount",
                            "-$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.order.refund_detail.refunded_amount),
                            PrintSunmiUtils.lineChar()
                        ).toString()
                    )
                } else {
                    PrintSunmiUtils.boldText(
                        padLine(
                            "Refund Amount",
                            "-$" + MethodUtils.roundOffAmountString(paymentDetailsResponse.data.order.refund_detail.refunded_amount),
                            PrintSunmiUtils.lineChar()
                        ).toString()
                    )
                }

                SunmiPrintHelper.getInstance().lineWrap(1)
            } else {
                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            if (paymentDetailsResponse.data.order?.total_tips == 0.0) {
                if (customerSettingModel.showTipLineForCash) {


                    if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(
                            1
                        )
                            ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
                    ) {
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            PrintSunmiUtils.boldTextNew("Tip       _____________")
                            SunmiPrintHelper.getInstance().lineWrap(1)
                        } else {
                            PrintSunmiUtils.boldTextNew("Tip                               _____________")
                        }
                    } else {
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            PrintSunmiUtils.boldText("Tip       _____________")
                            SunmiPrintHelper.getInstance().lineWrap(1)
                        } else {
                            PrintSunmiUtils.boldText("Tip                               _____________")
                        }
                    }
                }
            }

            if (customerSettingModel.showTipSuggestion) {
                SunmiPrintHelper.getInstance().lineWrap(1)
                PrintSunmiUtils.additionalTipsInner()
                if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)
                        ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
                ) {
                    PrintSunmiUtils.addHorizontalInnerNew()
                    PrintSunmiUtils.normalText("\n")
                } else {
                    PrintSunmiUtils.addHorizontalInner()
                }

                if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)
                        ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
                ) {
                    if (tipsList.isNotEmpty()) {
                        addTipsListInnerNew(
                            tipsList,
                            paymentDetailsResponse.data.order.total_amount,
                            customerSettingModel.fonts
                        )
                    }
                } else {
                    if (tipsList.isNotEmpty()) {
                        PrintSunmiUtils.addTipListInner(
                            tipsList,
                            paymentDetailsResponse.data.order.total_amount,
                            customerSettingModel.fonts
                        )
                    }
                }

                SunmiPrintHelper.getInstance().lineWrap(1)
            }


            val tranId = padLine(
                "Transaction ID",
                "" + paymentDetailsResponse.data.id,
                PrintSunmiUtils.lineChar()
            ).toString()

            if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)
                    ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
            ) {
                PrintSunmiUtils.normalTextNew(tranId)
            } else {
                PrintSunmiUtils.normalText(tranId)
            }

            if (paymentDetailsResponse.data.payment_type.lowercase() == "Card".lowercase()) {

                val tranType = padLine(
                    "Transaction Type",
                    "Card", PrintSunmiUtils.lineChar()
                ).toString()

                if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)
                        ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
                ) {
                    PrintSunmiUtils.normalTextNew(tranType)
                } else {
                    PrintSunmiUtils.normalText(tranType)
                }

                try {
                    if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(
                            1
                        )
                            ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
                    ) {
                        PrintSunmiUtils.cardDetailsInnerNew(
                            paymentDetailsResponse.data.card_name,
                            /*paymentDetailsResponse.data.card_type ?: ""*/
                            MethodUtils.getCardType(paymentDetailsResponse.data.ext_data),
                            "  " + paymentDetailsResponse.data.card_number,
                            customerSettingModel.fonts
                        )
                    } else {
                        PrintSunmiUtils.cardDetailsInner(
                            paymentDetailsResponse.data.card_name,
                            /*paymentDetailsResponse.data.card_type ?: ""*/
                            MethodUtils.getCardType(paymentDetailsResponse.data.ext_data),
                            paymentDetailsResponse.data.card_number,
                            customerSettingModel.fonts
                        )
                    }


                } catch (e: Exception) {

                }

            } else {

                if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)
                        ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
                ) {
                    PrintSunmiUtils.normalTextNew(
                        padLine(
                            "Transaction Type",
                            paymentDetailsResponse.data.payment_type ?: "Cash",
                            PrintSunmiUtils.lineChar()
                        ).toString()
                    )

                } else {
                    PrintSunmiUtils.normalText(
                        padLine(
                            "Transaction Type",
                            paymentDetailsResponse.data.payment_type ?: "Cash",
                            PrintSunmiUtils.lineChar()
                        ).toString()
                    )
                }

            }


            SunmiPrintHelper.getInstance().lineWrap(1)

            if (customerSettingModel.showCustomerAddress or customerSettingModel.showCustomerPhone or customerSettingModel.showCustomerName) {


                if (paymentDetailsResponse?.data.order?.customer != null) {

                    PrintSunmiUtils.customerDetailsInner(false, sunmiFrameworkVersion)

                    if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(
                            1
                        )
                            ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
                    ) {
                        PrintSunmiUtils.normalText("\n")
                    }
                    if (customerSettingModel.showCustomerName) {

                        PrintSunmiUtils.normalText(paymentDetailsResponse.data.order.customer.firstName + " " + paymentDetailsResponse.data.order.customer.lastName)
                    }

                    if (customerSettingModel.showCustomerPhone) {
                        if (!paymentDetailsResponse.data.order.customer.phones.isNullOrEmpty()) {

                            val phoneNoFormatted = MethodUtils.formatPhoneNumber(
                                paymentDetailsResponse.data.order.customer.phones.get(
                                    paymentDetailsResponse.data.order.customer.phones.size - 1
                                ).phoneNumber
                            )
                            PrintSunmiUtils.normalText(phoneNoFormatted)

                        }


                    }

                    if (customerSettingModel.showCustomerAddress) {
                        if (paymentDetailsResponse.data.order.customer.addresses.isNotEmpty()) {


                            paymentDetailsResponse.data.order.customer.addresses.filter { it.typeOfAddress == SHIPPING_ADDRESS }
                                .forEach {

                                    if (it.typeOfAddress.equals(
                                            SHIPPING_ADDRESS,
                                            ignoreCase = true
                                        )
                                    ) {
                                        PrintSunmiUtils.normalText(
                                            it.fullAddress
                                        )
                                    }
                                }
//                            PrintSunmiUtils.normalText(
//                                paymentDetailsResponse?.data?.order?.customer?.addresses?.get(
//                                    paymentDetailsResponse?.data?.order?.customer?.addresses?.size - 1
//                                )?.fullAddress
//                            )
                        }
                    }

                    SunmiPrintHelper.getInstance().lineWrap(2)

                }
            }


            if (paymentDetailsResponse?.data.order?.note != null && paymentDetailsResponse?.data.order?.note != "" && customerSettingModel.showOrderNote) {
                PrintSunmiUtils.orderNoteInner(paymentDetailsResponse?.data.order?.note)
                SunmiPrintHelper.getInstance().lineWrap(2)
            }

            if (customerSettingModel.fonts == Constants.LARGE) {
                PrintSunmiUtils.boldText("Customer Signature ____")
            } else {
                PrintSunmiUtils.boldText("Customer Signature           __________________")
            }

            SunmiPrintHelper.getInstance().lineWrap(2)
            if (customerSettingModel.showQrCode) {

                PrintSunmiUtils.qrCodeInner(paymentDetailsResponse?.data.order?.digital_receipt_url.toString())

            }

            PrintSunmiUtils.cutPaperInner()

            SunmiPrintHelper.getInstance().deInitSunmiPrinterService(requireContext())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun printFromLandiInnerPrinter(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters
    ) {
//        val printer = com.dantsu.escposprinter.EscPosPrinter(BluetoothPrintersConnections.selectFirstPaired(), 203, 48f, 32)

        val order = receiptModel?.order

        this.checkBluetoothPermissions(object : OnBluetoothPermissionGranted {
            override fun onPermissionsGranted() {
                val order = paymentDetailsResponse.data
                GlobalScope.launch {
                    LPrint.connectLandiInnerPrinter(customerReceiptPrinters.macAddress)
                        ?.let { outputStream ->
                            /*---------------------Sample---------------------*/
                            /*   outputStream.write(LPrint.CENTER_ALIGN)  // Center align
                        outputStream.write(LPrint.BOLD_ON)       // Enable bold
                        outputStream.write("Bold Text\n".toByteArray())
                        outputStream.write(LPrint.BOLD_OFF)     // Disable bold

                        outputStream.write(LPrint.UNDERLINE_ON)  // Enable underline
                        outputStream.write("Underlined Text\n".toByteArray())
                        outputStream.write(LPrint.BOLD_OFF) // Disable underline

                        outputStream.write(LPrint.DOUBLE_HEIGHT_WIDTH)     // Set large font size
                        outputStream.write("Large Text\n".toByteArray())
                        outputStream.write(LPrint.RESET_FONT_SIZE) // Reset font size to normal

                        outputStream.write("Normal Text\n".toByteArray())
*/
                            /*---------------------Sample---------------------*/
                            LPrint.apply {
                                setOutputStream(outputStream)

                                var landiPrinter = omniDriver!!.getPrinter(Bundle())
                                landiPrinter.openDevice(1)

                                val pWidth: Int = landiPrinter.getValidWidth()

                                try {

                                    if (customerSettingModel.showOrderIdTop) {

                                        val orderIdToPrint = if (prefProvider.getValueboolean(
                                                ORDER_NUMBER_STARTING_FROM_ONE,
                                                false
                                            )
                                        ) {
                                            "OrderID: ${order?.custom_order_id}"
                                        } else {
                                            "OrderID: ${order?.id}"
                                        }

                                        lineBreak()
                                        printCenter(
                                            orderIdToPrint,
                                            FONT_SIZE_5X,
                                            isBold = true
                                        )
                                        lineBreak()

                                    }


                                    if (customerSettingModel.showVenueLogo && prefProvider.getValue(
                                            VENUE_LOGO,
                                            ""
                                        )
                                            .isNotEmpty()
                                    ) {

//                            PrintSunmiUtils.printLogoInner(prefProvider.getValue(VENUE_LOGO, ""))
                                        try {

                                            landiPrinter.addImage(venueUrlByteArray, Align.CENTER, 0)

                                            landiPrinter.startPrint(object : OnPrintListener {
                                                override fun onSuccess() {

                                                }

                                                override fun onFail(i: Int) {

                                                }
                                            })

                                        } catch (ex: java.lang.Exception) {
                                            Log.d("DMJ", "Error getting image bytes to print")
                                        }

                                    }


                                    printCenter(
                                        prefProvider.getValue(
                                            Constants.BUSINESS_NAME,
                                            ""
                                        ),
                                        fontSize = FONT_SIZE_5X,
                                        isBold = true,
                                        printOnNewLine = true
                                    )
                                    lineBreak()

                                    var venueAddress = if (customerSettingModel.showVenueAddress) {
                                        prefProvider.getValue(BUSINESS_ADDRESS, "")
                                    } else ""

                                    var businessPhoneNumber = MethodUtils.getUSFormatNumber(
                                        prefProvider.getValue(
                                            BUSINESS_PHONE_NO,
                                            ""
                                        )
                                    )

                                    printCenter(venueAddress, fontSize = SMALL_SIZE)
                                    lineBreak()

                                    printCenter(businessPhoneNumber, fontSize = SMALL_SIZE)
                                    lineBreak()

                                    printCenter(
                                        prefProvider.getValue(
                                            Constants.BUSINESS_WEBSITE,
                                            ""
                                        ), fontSize = SMALL_SIZE
                                    )
                                    lineBreak()

                                    if (customerSettingModel.showOrderType) {
                                        order?.order.order_type_name.trim()?.let {
//                                        outputStream.write(LPrint.FONT_SIZE_5X)
//
//                                        outputStream.write(it.toByteArray())
                                            printCenter(
                                                it,
                                                isBold = true,
                                                fontSize = FONT_SIZE_5X
                                            )

                                            if (!it.contains("Phone", true)) {
//                                            outputStream.write(LPrint.LINE_FEED)
                                                lineBreak()

                                            }
                                        }
                                    }
                                    lineBreak()

                                    if (paymentDetailsResponse.data.order.order_type.trim()
                                            .equals("Online Order", true) ||
                                        paymentDetailsResponse.data.order.order_type.trim()
                                            .equals("OnlineWebOrder", true) ||
                                        paymentDetailsResponse.data.order.order_type.trim()
                                            .equals(PHONE_ORDER, true)
                                    ) {
                                        printCenter(
                                            paymentDetailsResponse.data.order.delivery_type,
                                            isBold = true,
                                            fontSize = FONT_SIZE_5X
                                        )
                                        lineBreak()
                                        lineBreak()
                                    }


                                    printLeft("ReceiptID : ${order.order.offline_id.trim()}")
                                    lineBreak()


                                    printLeft("Employee : ${order.order.employee.trim()}")
                                    lineBreak()

                                    printLeft(
                                        "Order Time : ${
                                            getReceiptFormatDateFromUTCServer(
                                                requireContext(),
                                                order.order.created_at.toString()
                                            )
                                        }"
                                    )

                                    if (customerSettingModel.showPrintTime) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                                            lineBreak()

                                            printLeft(
                                                "Print Time : ${
                                                    getCurrentTimeFromTimeZone(
                                                        requireContext(),
                                                        MethodUtils.formatted()
                                                    )
                                                }"
                                            )

                                        }
                                    }

                                    lineBreak()
                                    printDashedLineAndBreak()

                                    order?.order?.order_items?.let {
                                        addOrderItemsTransactionInnerLandi(
                                            it,
                                            customerSettingModel.fonts,
                                            showModifiers = customerSettingModel.showModifiers,
                                            LPrint
                                        )
                                    }
                                    lineBreak()
                                    lineBreak()


                                    if (paymentDetailsResponse?.data.order.total_discount != null) {

                                        val discountToPrint =
                                            padLine(
                                                "Total Discount",

                                                if (order.order.total_discount == 0.0) {
//                            "-$" + MethodUtils.roundOffAmountString(0.00)
                                                    "$" + MethodUtils.roundOffAmountString(0.00)
                                                } else {
                                                    order.order.total_discount.let {
                                                        "-$" + MethodUtils.roundOffAmountString(it)
                                                    }
                                                },
                                                48
                                            ).toString()

                                        printLeft(discountToPrint)
                                        lineBreak()
                                    }

                                    /**
                                     * Print Subtotal
                                     */

                                    var subTotalToPrint = ""




                                    if(order.order.order_type_name.lowercase(Locale.ROOT) == "dine in" && order.payable_type == "Guest") {

                                        val totalAmount = order.order.sub_total.let {
                                            MethodUtils.roundOffAmountString(
                                                it
                                            )}

                                        subTotalToPrint =  padLine(
                                                "Sub Total",
                                                "$" + paymentDetailsResponse?.data.sub_total.let { it } + "($$totalAmount)",
                                                if (customerSettingModel.fonts == Constants.LARGE) {
                                                    23
                                                } else {
                                                    48
                                                }
                                            ).toString()

                                    } else {

                                        //other Order types than dine in

                                               subTotalToPrint =  padLine(
                                                    "Sub Total",
                                                    "$" + order.order.sub_total.let {
                                                        MethodUtils.roundOffAmountString(
                                                            it
                                                        )
                                                    },
                                                    if (customerSettingModel.fonts == Constants.LARGE) {
                                                        23
                                                    } else {
                                                        48
                                                    }
                                                ).toString()
                                    }


                                    printLeft(subTotalToPrint)
                                    lineBreak()

                                    /**
                                     * Print Tax Amount
                                     */
                                    if (paymentDetailsResponse.data?.tax_amount != null) {
                                        printLeft(
                                            padLine(
                                                "Tax",
                                                "$" + MethodUtils.roundOffAmountString(
                                                    paymentDetailsResponse.data.tax_amount
                                                ),
                                                PrintSunmiUtils.lineChar()
                                            ).toString()
                                        )
                                        lineBreak()
                                    }

                                    /**
                                     * Print Serivce charge
                                     */

                                    if (paymentDetailsResponse.data?.service_charge_amount != null && paymentDetailsResponse.data.order.service_charge_enabled) {

                                        printLeft(
                                            padLine(
                                                "Service Charge",
                                                "$" + MethodUtils.roundOffAmountString(
                                                    paymentDetailsResponse.data.service_charge_amount
                                                ),
                                                PrintSunmiUtils.lineChar()
                                            ).toString()
                                        )
                                        lineBreak()
                                    }

                                    if (paymentDetailsResponse.data?.tips != 0.0) {

                                        printLeft(
                                            padLine(
                                                "Tip",
                                                "$" + paymentDetailsResponse.data.tips?.let {
                                                    MethodUtils.roundOffAmountString(
                                                        it
                                                    )
                                                }, PrintSunmiUtils.lineChar()
                                            ).toString()
                                        )
                                        lineBreak()
                                    }




                                    if (paymentDetailsResponse.data?.cash_discount_or_surcharge != null && customerSettingModel.showCashDisSurCharg) {

                                        if (paymentDetailsResponse?.data?.payment_type.lowercase() == "Card".lowercase() && paymentDetailsResponse?.data?.cash_discount_type.lowercase() == "SurCharge".lowercase()) {
                                            val surCharge =
                                                padLine(
                                                    Constants.SURCHARGE_TEXT,
                                                    if (paymentDetailsResponse.data.cash_discount_or_surcharge != 0.0) {
                                                        "$" + MethodUtils.roundOffAmountString(
                                                            paymentDetailsResponse.data?.cash_discount_or_surcharge
                                                        )
                                                    } else {
                                                        "$" + MethodUtils.roundOffAmountString(
                                                            paymentDetailsResponse.data.order?.cash_discount_or_surcharge
                                                        )
                                                    }, PrintSunmiUtils.lineChar()
                                                ).toString()
                                            printLeft(surCharge)
                                            lineBreak()
                                        } else if (paymentDetailsResponse?.data?.payment_type.lowercase() == "Cash".lowercase() && paymentDetailsResponse?.data?.cash_discount_type.lowercase() == "CashDiscount".lowercase()) {


                                            val cashDisc = padLine(
                                                "Cash Discount",
                                                if (paymentDetailsResponse.data.cash_discount_or_surcharge != 0.0) {
                                                    "-$" + MethodUtils.roundOffAmountString(
                                                        paymentDetailsResponse.data?.cash_discount_or_surcharge
                                                    )
                                                } else {
                                                    "$" + MethodUtils.roundOffAmountString(
                                                        paymentDetailsResponse.data.order?.cash_discount_or_surcharge
                                                    )
                                                }, PrintSunmiUtils.lineChar()
                                            ).toString()

                                            printLeft(cashDisc)
                                            lineBreak()
                                        }
                                    }


                                    if (paymentDetailsResponse?.data?.is_loyalty_applied == true) {

                                        if (paymentDetailsResponse?.data?.loyalty_amount != 0.0) {

                                            val loyaltyAmount = padLine(
                                                "Used Loyalty Amount",
                                                "-$" + paymentDetailsResponse.data?.loyalty_amount?.let {
                                                    MethodUtils.roundOffAmountString(
                                                        it.toDouble()
                                                    )
                                                }, PrintSunmiUtils.lineChar()
                                            ).toString()
                                            printLeft(loyaltyAmount)
                                            lineBreak()

                                        }

                                        if (paymentDetailsResponse?.data?.used_reward_points != 0) {

                                            val loyaltyPoint = padLine(
                                                "Used Loyalty Points",
                                                paymentDetailsResponse?.data?.used_reward_points.toString(),
                                                PrintSunmiUtils.lineChar()
                                            ).toString()

                                            printLeft(loyaltyPoint)
                                            lineBreak()
                                        }
                                    }

                                    lineBreak()
                                    val totalAmt =
                                        MethodUtils.roundOffAmountDouble(paymentDetailsResponse.data.amount + paymentDetailsResponse.data.tips)

                                    printBoldLeft(
                                        padLine(
                                            "Total Price",
                                            "$" + MethodUtils.roundOffAmountString(totalAmt),
                                            PrintSunmiUtils.lineChar()
                                        ).toString()
                                    )
                                    lineBreak()

                                    if (paymentDetailsResponse?.data?.order.refund_detail != null && paymentDetailsResponse?.data?.order?.refund_detail?.refunded_amount != 0.0 && customerSettingModel.showRefundAmount) {

                                        printBoldLeft(
                                            padLine(
                                                "Refund Amount",
                                                "-$" + MethodUtils.roundOffAmountString(
                                                    paymentDetailsResponse?.data?.order?.refund_detail?.refunded_amount
                                                ),
                                                PrintSunmiUtils.lineChar()
                                            ).toString()
                                        )
                                        lineBreak()
                                    } else {
                                        lineBreak()
                                    }

                                    if (paymentDetailsResponse.data.order?.total_tips == 0.0) {
                                        if (customerSettingModel.showTipLineForCash) {

                                            if (customerSettingModel.fonts == Constants.LARGE) {
                                                printBoldLeft("Tip       _____________")
                                                lineBreak()
                                            } else {
                                                printBoldLeft("Tip                               _____________")
                                            }

                                        }
                                    }

                                    /**
                                     * Tips suggestion
                                     */
                                    if (customerSettingModel.showTipSuggestion) {
                                        lineBreak()
                                        printBoldLeft("Additional Tips")
                                        lineBreak()

                                        printDashedLineAndBreak()

                                        if (tipsList.isNotEmpty()) {
                                            val tipsToPrint = addTipsListInnerLandi(
                                                tipsList,
                                                totalAmt,
                                                customerSettingModel.fonts
                                            )

                                            printLeft(tipsToPrint)

                                        }
                                    }
                                    lineBreak()
                                    /**
                                     * Print transaction details
                                     */
                                    val tranId = padLine(
                                        "Transaction ID",
                                        "" + paymentDetailsResponse.data.id,
                                        PrintSunmiUtils.lineChar()
                                    ).toString()

                                    printLeft(tranId)
                                    lineBreak()

                                    if (paymentDetailsResponse.data.payment_type.lowercase() == "Card".lowercase()) {

                                        printLeft(
                                            padLine(
                                                "Transaction Type",
                                                paymentDetailsResponse.data.payment_type,
                                                PrintSunmiUtils.lineChar()
                                            ).toString()
                                        )
                                        lineBreak()


                                        var strCardType =
                                            paymentDetailsResponse.data.card_type


                                        printLeft(
                                            padLine(
                                                "",
                                                strCardType,
                                                48
                                            ).toString()
                                        )
                                        lineBreak()

                                        printLeft(
                                            padLine(
                                                "",
                                                paymentDetailsResponse.data.card_number,
                                                48
                                            ).toString()
                                        )
                                        lineBreak()

                                    } else {
                                        printLeft(
                                            padLine(
                                                "Transaction Type",
                                                paymentDetailsResponse.data.payment_type,
                                                PrintSunmiUtils.lineChar()
                                            ).toString()
                                        )
                                        lineBreak()
                                    }

                                    lineBreak()

                                    /**
                                     * Print customer details
                                     */
                                    if (customerSettingModel.showCustomerAddress or customerSettingModel.showCustomerPhone or customerSettingModel.showCustomerName) {


                                        if (paymentDetailsResponse?.data.order?.customer != null) {

                                            PrintSunmiUtils.customerDetailsInnerLandi(false, LPrint)

                                            if (customerSettingModel.showCustomerName) {

                                                printLeft(paymentDetailsResponse?.data.order?.customer.firstName + " " + paymentDetailsResponse?.data.order?.customer.lastName)
                                                lineBreak()
                                            }

                                            if (customerSettingModel.showCustomerPhone) {
                                                if (paymentDetailsResponse?.data?.order?.customer?.phones?.isNotEmpty()) {

                                                    val phoneNoFormatted =
                                                        MethodUtils.formatPhoneNumber(
                                                            paymentDetailsResponse.data.order.customer.phones.get(
                                                                paymentDetailsResponse.data.order.customer.phones.size - 1
                                                            ).phoneNumber
                                                        )
                                                    printLeft(phoneNoFormatted)
                                                    lineBreak()

                                                }


                                            }

                                            if (customerSettingModel.showCustomerAddress) {
                                                if (paymentDetailsResponse?.data?.order?.customer?.addresses?.isNotEmpty() == true) {


                                                    paymentDetailsResponse.data.order.customer.addresses.filter { it.typeOfAddress == SHIPPING_ADDRESS }
                                                        .forEach {

                                                            if (it.typeOfAddress.equals(
                                                                    SHIPPING_ADDRESS,
                                                                    ignoreCase = true
                                                                )
                                                            ) {
                                                                printLeft(
                                                                    it.fullAddress
                                                                )
                                                                lineBreak()
                                                            }
                                                        }
                                                }
                                            }

                                            lineBreak()
                                            lineBreak()

                                        }
                                    }


                                    /**
                                     * Print order note
                                     */
                                    if (paymentDetailsResponse?.data.order?.note != null && paymentDetailsResponse?.data.order?.note != "" && customerSettingModel.showOrderNote) {
                                        printCenter("ORDER NOTE\n${paymentDetailsResponse?.data.order?.note}")
                                        lineBreak()
                                    }
                                    lineBreak()

                                    /**
                                     * Print customer signature
                                     */
                                    if (customerSettingModel.fonts == Constants.LARGE) {
                                        printBoldLeft("Customer Signature ____")
                                    } else {
                                        printBoldLeft("Customer Signature           __________________")
                                    }

                                    lineBreak()
                                    lineBreak()
                                    if (customerSettingModel.showQrCode) {

                                        LPrint.printQRCode(
                                            outputStream,
                                            order?.order?.digital_receipt_url.toString(),
                                            LPrint.CENTER_ALIGN
                                        )
//                                        PrintSunmiUtils.qrCodeInner(paymentDetailsResponse?.data.order?.digital_receipt_url.toString())

                                    }

//                                    PrintSunmiUtils.cutPaperInner()
//                                    SunmiPrintHelper.getInstance().deInitSunmiPrinterService(requireContext())
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                lineBreak()
                                paperCut()
                                disconnectLandiPrinter()
                            }
                        }
                }
            }
        })


    }

    interface OnBluetoothPermissionGranted {
        fun onPermissionsGranted()
    }

    var onBluetoothPermissionGranted: OnBluetoothPermissionGranted? = null

    fun checkBluetoothPermissions(onBluetoothPermissionGranted: OnBluetoothPermissionGranted) {
        this.onBluetoothPermissionGranted = onBluetoothPermissionGranted
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf<String>(Manifest.permission.BLUETOOTH),
                Constants.PERMISSION_BLUETOOTH
            )
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.BLUETOOTH_ADMIN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf<String>(Manifest.permission.BLUETOOTH_ADMIN),
                Constants.PERMISSION_BLUETOOTH_ADMIN
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf<String>(Manifest.permission.BLUETOOTH_CONNECT),
                Constants.PERMISSION_BLUETOOTH_CONNECT
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf<String>(Manifest.permission.BLUETOOTH_SCAN),
                Constants.PERMISSION_BLUETOOTH_SCAN
            )
        } else {
            onBluetoothPermissionGranted.onPermissionsGranted()
        }
    }

    private fun observeTipsList() {
        viewModel.getTipsList().observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                tipsList = it
            }
        }
    }

    private fun generateQRCode(qrcodeStaticUrl: String): Bitmap {

        val manager = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager?
        val display: Display = manager!!.defaultDisplay
        val point = Point()
        display.getSize(point)
        val width: Int = point.x
        val height: Int = point.y
        var dimen = if (width < height) width else height
        dimen = dimen * 3 / 4
        return net.glxn.qrgen.android.QRCode.from(qrcodeStaticUrl).bitmap()

    }

    private fun printBusinessLogo() {
        val decodedString: ByteArray = Base64.decode(
            prefProvider.getValue(Constants.VENUE_LOGO, ""),
            Base64.DEFAULT
        )
        val bitmap: Bitmap =
            BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

        val newBitmap = Bitmap.createScaledBitmap(bitmap!!, 210, 210, true)

        PrintSunmiUtils.printLogo(newBitmap)

    }

    override fun onStop() {
        if (this@TransactionDetailsFragment::paymentCoroutineScope.isInitialized) {
            paymentCoroutineScope.cancel()
        }

        super.onStop()
    }
}