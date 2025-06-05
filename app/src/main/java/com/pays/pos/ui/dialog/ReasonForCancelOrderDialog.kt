package com.pays.pos.ui.dialog

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.epson.epos2.printer.Printer
import com.epson.eposprint.Builder
import com.epson.eposprint.Print
import com.pays.pos.R
import com.pays.pos.data.model.requestModel.RefundRequestModel
import com.pays.pos.data.model.responseModel.VenueDetailsResponse
import com.pays.pos.databinding.DialogCancelOrderReasonBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.adapter.CancelOrderReasonAdapter
import com.pays.pos.ui.fragments.allorders.AllOrdersViewModel
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.extensions.alert
import com.pays.pos.utils.extensions.liveSnackBar
import com.pays.pos.utils.extensions.showAlert
import com.pays.pos.utils.statusUtils.Status
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.pays.pos.data.model.responseModel.GetKitchenReceiptSettingsResponse
import com.pays.pos.data.model.responseModel.OnlineOrderResponseModel
import com.pays.pos.data.model.responseModel.PrinterResponse
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.EMPLOYEE_NAME
import com.pays.pos.data.remote.Constants.LANDI_INNER_PRINTER
import com.pays.pos.data.remote.Constants.ORDER_NUMBER_STARTING_FROM_ONE
import com.pays.pos.data.remote.Constants.SHIPPING_ADDRESS
import com.pays.pos.data.remote.Constants.SUNMI_PRINTER
import com.pays.pos.data.remote.Constants.WIFI
import com.pays.pos.ui.fragments.allorders.AllOrdersListingFragment.OnBluetoothPermissionGranted
import com.pays.pos.ui.fragments.onlineorder.OnlineDetailViewModel
import com.pays.pos.ui.fragments.settings.hardware.printer.BluetoothUtil
import com.pays.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper
import com.pays.pos.ui.fragments.settings.kitchenreceipt.KitchenReceiptSettings
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.PrintSunmiUtils
import com.pays.pos.utils.addBuilderText
import com.pays.pos.utils.addBuilderTextForU220
import com.pays.pos.utils.addHorizontalKitchenLine
import com.pays.pos.utils.addHorizontalKitchenLineForU220
import com.pays.pos.utils.addHorizontalLine
import com.pays.pos.utils.addOrdersForKitchenOnlineOrder
import com.pays.pos.utils.addOrdersForKitchenOnlineOrderLandi
import com.pays.pos.utils.addOrdersForKitchenOnlineOrderSunmi
import com.pays.pos.utils.addOrdersForKitchenOnlineOrderSunmiInner
import com.pays.pos.utils.addOrdersForKitchenOnlineOrderU220
import com.pays.pos.utils.addReprintOrdersForStarKitchen
import com.pays.pos.utils.addSingleReprintOrdersForStarKitchen
import com.pays.pos.utils.checkItemsforPrinterOnlineOrder
import com.pays.pos.utils.extensions.runOnUiThread
import com.pays.pos.utils.landi.LPrint
import com.pays.pos.utils.padLine
import com.pays.pos.utils.printer.PrinterClass
import com.sdksuite.omnidriver.OmniDriver
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.security.InvalidKeyException
import java.security.KeyFactory
import java.security.NoSuchAlgorithmException
import java.security.Signature
import java.security.SignatureException
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date
import java.util.Random
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject


@AndroidEntryPoint
class ReasonForCancelOrderDialog : DialogFragment() {

    private var refundAmount: Double = 0.0
    private lateinit var binding: DialogCancelOrderReasonBinding
    private lateinit var refundData: RefundRequestModel
    private val viewModel by viewModels<AllOrdersViewModel>()
    var cancelOrderReasonsList = ArrayList<VenueDetailsResponse.Data.CancelOrderReason>()
    private lateinit var cancelOrderReasonAdapter: CancelOrderReasonAdapter
    private var itemPos: Int = 0
    var reason_id = 0
    private val onlineDetailsViewModel by activityViewModels<OnlineDetailViewModel>()
    private  var dataModel:OnlineOrderResponseModel.Data?=null
    private val TAG = "ReasonForCancelOrderDialog"
    private var sunmiFrameworkVersion :Array<String>? = null

    var charHSize: Int = 1
    var asciiCharWidth: Int = 12
    var cjkCharWidth: Int = 24
    var orderContent: java.lang.StringBuilder = java.lang.StringBuilder()
    var omniDriver: OmniDriver? = null


    @Inject
    lateinit var prefProvider: PrefProvider
    var orderId: Int? = null
    var startDate: String = ""
    var endDate: String = ""
    private lateinit var kitchenSettingModel :GetKitchenReceiptSettingsResponse.Data

    /*Star label printer - START*/
    lateinit var settings: StarConnectionSettings
    lateinit var printer: StarPrinter

    companion object {
        fun newInstance() = ReasonForCancelOrderDialog()
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.dialog_cancel_order_reason, container, false)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        orderId = arguments?.getInt("orderId")
        startDate = arguments?.getString("startDate").toString()
        endDate = arguments?.getString("endDate").toString()
        dataModel = arguments?.getParcelable<OnlineOrderResponseModel.Data>("data")
        kitchenSettingModel =
            (arguments?.getParcelable<GetKitchenReceiptSettingsResponse.Data>("kitchen_settings") ?: null) as GetKitchenReceiptSettingsResponse.Data


        binding.txtDone.setOnClickListener {

            val reason = binding.etReason.text.toString().trim()

            if (reason.isEmpty()) {
                alert(
                    getString(R.string.app_name),
                    getString(R.string.cancel_order_reason_message)
                ) {
                    positiveButton(getString(R.string.tv_ok)) {

                    }
                    negativeButton(R.string.cancel) {
                        // Do negative stuff here
                    }
                }
            } else {
                alert(
                    getString(R.string.app_name),
                    getString(R.string.cancel_order_message)
                ) {
                    positiveButton(getString(R.string.yes)) {
                        viewModel.cancelOrder(orderId!!, reason, reason_id)
                    }
                    negativeButton(R.string.no) {
                        // Do negative stuff here
                    }
                }
            }

        }

        setupSnackbar()
        observeShowProgress()
        navigate()

        binding.imgBack.setOnClickListener {
            findNavController().navigateUp()
        }

        getCancelOrderReasons()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)


    }

    override fun onResume() {
        super.onResume()

        val window: Window? = dialog!!.window
        val size = Point()
        val display: Display = window?.windowManager?.defaultDisplay!!
        display.getSize(size)
        val width: Int = size.x
        window.setLayout((width * 0.50).toInt(), WindowManager.LayoutParams.MATCH_PARENT)
        window.setGravity(Gravity.CENTER)
    }

    private fun setupSnackbar() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

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

        viewModel.data.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { baseResponse ->

                dataModel?.let { it1 -> getKitchenPrinters(it1,true) }
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, baseResponse.message
                    ) { _, _ ->



                        lifecycleScope.launch {
                            delay(1000)

                            runOnUiThread(kotlinx.coroutines.Runnable {
                            dismiss()
                            })
                            val intent = Intent()
                            intent.action = "cancelled"
                            intent.putExtra("isCount", false)
                            intent.putExtra("start_date", startDate)
                            intent.putExtra("end_date", endDate)
                            intent.putExtra("position", 2)
                            requireContext().sendBroadcast(intent)
                        }
                    }
                }
            }
        }


    }

    private fun navigate() {
    }

    private fun getCancelOrderReasons() {
        viewModel.getcancelOrderReasonsDatabse.observe(viewLifecycleOwner) {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        resource.data?.let { reasonsList ->
                            cancelOrderReasonsList.clear()
                            cancelOrderReasonsList.addAll(it.data as ArrayList<VenueDetailsResponse.Data.CancelOrderReason>)

                            cancelOrderReasonAdapter = CancelOrderReasonAdapter(object :
                                CancelOrderReasonAdapter.CustomerInteface {
                                override fun onReasonSelect(
                                    pos: Int,
                                    model: VenueDetailsResponse.Data.CancelOrderReason
                                ) {
                                    reason_id = model.id
                                    itemPos = model.id
                                    binding.etReason.setText(model.reason)

                                    /*binding.layoutTool.txtSubTitle.setText(model.first_name + " " + model.last_name)
                                    loadFragment(model)*/
                                }

                            })
                            binding.rvReasons.adapter = cancelOrderReasonAdapter

                            cancelOrderReasonAdapter.add(cancelOrderReasonsList.sortedBy { cancelOrderReason ->  cancelOrderReason.sort })


                        }
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


    override fun onDismiss(dialog: DialogInterface) {
        Log.d("ReasonForRefundOnlineOrder.kt", "onDismiss_1")
        super.onDismiss(dialog)

        onlineDetailsViewModel.toggleRefresh(true)

        Log.d("ReasonForRefundOnlineOrder.kt", "onDismiss_2")

        val result = Bundle().apply {
            putString("RESULT_KEY", "Your data here")
        }
        setFragmentResult("RESULT_KEY", result)
    }

    override fun onDestroy() {
        Log.d("ReasonForRefundOnlineOrder.kt", "onDismiss_3")
        super.onDestroy()
        Log.d("ReasonForRefundOnlineOrder.kt", "onDismiss_4")
    }

    override fun onDestroyView() {
        Log.d("ReasonForRefundOnlineOrder.kt", "onDismiss_5")
        super.onDestroyView()
        Log.d("ReasonForRefundOnlineOrder.kt", "onDismiss_6")
    }


    // To get connected kitchen printers
    private fun getKitchenPrinters(data: OnlineOrderResponseModel.Data,isCancelOrder:Boolean = false) {
        CoroutineScope(Dispatchers.IO).launch {
            var it = viewModel.getKitchenPrinterList()




                it?.forEach {
                    if (it.kitchenStatus && checkItemsforPrinterOnlineOrder(
                            data.orderItems, it.printerCategories.toCollection(
                                arrayListOf()
                            )
                        )
                    ) {

                        Log.d("getKitchenPrinterList", "getKitchenPrinterList mmm")

                        initKitchenPrinter(
                            it,
                            Constants.KITCHEN,
                            data,
                            isCancelOrder
                        )

                    }
                }


        }

    }

    private fun initKitchenPrinter(
        data: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        orderData: OnlineOrderResponseModel.Data,
        isCancelOrder: Boolean=false
    ) {
        if (data.name.startsWith(SUNMI_PRINTER,true) && data.printer_type == WIFI){

            val date = Date()
            val random = Random()
            val timestamp = java.lang.String.format("%d", date.time / 1000)

            val body = java.lang.StringBuilder()
            body.append("{")
            body.append(java.lang.String.format("\"sn\":\"%s\"", "${data.ipAddress}"))
            body.append(",")
            body.append(java.lang.String.format("\"shop_id\":%d", 2241))
            body.append("}")

            orderContent.clear()
            orderContent = java.lang.StringBuilder()
            lineFeed(4)
            setAlignment(1)

            appendText("***** CANCELLED *****")
            lineFeed(2)

            if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {

                appendText("OrderID:"+orderData?.custom_order_id)
                lineFeed(2)
            }
            else{

                appendText("OrderID:"+orderData?.id)
                lineFeed(2)

            }


            if (kitchenSettingModel.showOrderType) {
                appendText(orderData?.orderTypeName)
                lineFeed(2)

                if ((orderData.orderType.equals(Constants.PHONE_ORDER, true) ||
                            orderData.orderType.equals("OnlineWebOrder", true) ||
                            orderData.orderType.equals("Online Order", true) ||
                            orderData.orderType.equals("OnlineOrder", true)) &&
                    orderData.deliveryType != null
                ) {

                    appendText(orderData?.deliveryType.toString())
                    lineFeed(2)
                }

            }

            if (kitchenSettingModel.showTeamMember) {
                appendText("Employee:" + orderData?.employee?.name)
                lineFeed(2)
            }


            appendText(
                Constants.getReceiptFormatDateFromUTCServer(
                requireContext(),
                orderData?.createdAt.toString()
            ))
            lineFeed(1)
            appendText("------------------------")

            lineFeed(2)
            setAlignment(0)


            for (i in 0 until orderData?.orderItems.size){
                val obj = orderData?.orderItems.get(i)
                if (obj.itemName.isNotEmpty()){
                    appendText(obj.quantity.toString() + " " + obj.itemName.uppercase())
                    lineFeed(1)

                    if (obj.orderItemModifiers.isNotEmpty()){
                        for (j in 0 until obj.orderItemModifiers.size){

                            val objMod = obj.orderItemModifiers.get(j)
                            appendText("  " + if (objMod.modifier_quantity == 1) {
                                "   "
                            } else {
                                "" + objMod.modifier_quantity + "x "
                            } + objMod.name.uppercase())

                            lineFeed(1)
                        }

                    }


                    if (obj.note.isNotEmpty()){

                        appendText("  Note:" + obj.note)
                        lineFeed(1)
                    }
                   // lineFeed(1)



                }


            }

            if (orderData?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
                lineFeed(2)
                setAlignment(1)
                appendText("Order Note")
                lineFeed(1)
                appendText(orderData.note)
                lineFeed(2)


            }

            lineFeed(1)

            if (kitchenSettingModel.showCustomerAddress != false || kitchenSettingModel.showCustomerPhone != false || kitchenSettingModel.showCustomerName) {

                try{

                    if (orderData?.customer != null){

                        setAlignment(0)
                        appendText("Customer Details")
                        lineFeed(1)
                        appendText("------------------------")
                        lineFeed(1)
                        if (kitchenSettingModel.showCustomerName) {


                            appendText(orderData?.customer?.firstName + " " + orderData?.customer?.lastName)
                            lineFeed(1)
                        }
                        try{
                            if (kitchenSettingModel.showCustomerPhone) {

                                if (orderData?.customer?.phones?.isNotEmpty()) {

                                    orderData?.customer?.phones?.get(0)?.phoneNumber?.let {
                                        appendText(
                                            MethodUtils.formatPhoneNumber(it)
                                        )
                                        lineFeed(1)
                                    }
                                }
                            }
                        }catch (e:Exception){

                        }

                        try{
                            if (kitchenSettingModel.showCustomerAddress) {

                                if (orderData?.orderType.trim()
                                        .lowercase() == "Open Order".trim()
                                        .lowercase() && orderData?.deliveryType.trim()
                                        .lowercase() == "Pickup".trim()
                                        .lowercase()
                                ) {

                                } else if (orderData?.customer?.addresses?.isNotEmpty()) {


//                            receiptModel?.order?.customer?.addresses?.get(0)?.fullAddress?.let {
//                                PrintSunmiUtils.normalTextLarge(
//                                    it
//                                )
//                            }

//                                    orderData?.customer?.addresses?.filter { it.typeOfAddress == Constants.BILLING_ADDRESS }
                                    orderData?.customer?.addresses?.filter { it.typeOfAddress == SHIPPING_ADDRESS }
                                        ?.forEach {

                                            if (it.typeOfAddress.equals(
                                                    SHIPPING_ADDRESS,
                                                    ignoreCase = true
                                                )
                                            ) {
                                                appendText(
                                                    it.fullAddress
                                                )
                                                lineFeed(1)
                                            }
                                        }


                                }
                            }
                        }catch (e:Exception){

                        }

                    }

                }
                catch (e:Exception){
                    e.printStackTrace()
                }
            }




            lineFeed(6)
            cutPaper(true)



            Log.e(TAG,"PushContent ${data.ipAddress}")
            Log.e("checkKey","pushContent: checkSN:${data.ipAddress} ${pushContent(trade_no =
            String.format("%s_%010d", "${data.ipAddress}", System.currentTimeMillis()),
                "${data.ipAddress}", 1, 1, "您有新的订单", 0)}")



        }
        else {


            if (data.name.startsWith(SUNMI_PRINTER, true)) {

                try {
                    SunmiPrinterApi.getInstance()
                        .setPrinter(SunmiPrinter.SunmiBlueToothPrinter, data.ipAddress)
                } catch (e: Exception) {
                    SunmiPrinterApi.getInstance()
                        .setPrinter(SunmiPrinter.SunmiNetPrinter, data.ipAddress)
                }

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

                                Log.d("tracking printers", "In IF")
                                CoroutineScope(Dispatchers.Main).launch {

                                    initKitchenPrinter(data, type, orderData,isCancelOrder)
                                }
                            }

                            override fun onDisconnect() {
                                println("onDisconnect")
                            }

                        })
                } else {
                    Log.d("tracking printers", "In Else")
                    generateKitchenReceiptSunmi(data, type, orderData,isCancelOrder)
                }

            } else if (data.name.startsWith(Constants.SUNMI_INNER_PRINTER, true)) {

                SunmiPrintHelper.getInstance().initSunmiPrinterService(requireContext())
                CoroutineScope(Dispatchers.IO).launch {
                    delay(100)
                    setServiceForKitchen(data, type, orderData)
                }
            } else if (((data.name.contains("TSP", ignoreCase = true))) || ((data.name.contains(
                    "SP",
                    ignoreCase = true
                )))
            ) {
                settings = StarConnectionSettings(InterfaceType.Lan, data.macAddress)
                printer = StarPrinter(settings, requireContext())

                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val builder = StarXpandCommandBuilder()

                        var printerBuilder = PrinterBuilder()

                        with(printerBuilder) {
                            styleInternationalCharacter(InternationalCharacterType.Usa)
                            styleCharacterSpace(0.0)
                            styleAlignment(Alignment.Center)

                            add(
                                PrinterBuilder()
                                    .styleBold(true)
                                    .styleMagnification(
                                        MagnificationParameter(3, 3)
                                    )
                                    .actionPrintText("***** CANCELLED *****")
                            )

                            styleAlignment(Alignment.Center)
                            actionFeedLine(1)

                                add(
                                    PrinterBuilder()
                                        .styleBold(true)
                                        .styleMagnification(
                                            MagnificationParameter(3, 3)
                                        )
                                        .actionPrintText(
                                            "OrderId: ${orderData.custom_order_id}"
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
                                                orderData.orderType
                                            else ""
                                        )
                                )

                                actionFeedLine(1)

                                if ((orderData.orderType.equals(Constants.PHONE_ORDER_)) || (orderData.orderType.equals(
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
                                                orderData.deliveryType
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
                                                orderData.createdAt.toString()
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
                                            content = addReprintOrdersForStarKitchen(
                                                orderData.orderItems!!,
                                                data.printerCategories.toCollection(arrayListOf())
                                            )
                                        )
                                )

                                actionFeedLine(1)
                                if (orderData.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                    add(
                                        PrinterBuilder()
                                            .styleMagnification(
                                                MagnificationParameter(2, 2)
                                            )
                                            .styleAlignment(Alignment.Center)
                                            .styleBold(true)
                                            .actionPrintText(
                                                content = if (orderData.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                                    "--------------------------------------------\nOrder Note\n "
                                                } else ""
                                            )
                                    )
                                }
                                if (orderData.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                    add(
                                        PrinterBuilder()
                                            .styleAlignment(Alignment.Center)
                                            .styleMagnification(
                                                MagnificationParameter(2, 2)
                                            )
                                            .actionPrintText(
                                                content = if (orderData.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                                    orderData.note.toString()
                                                } else ""
                                            )
                                    )
                                }
                                actionFeedLine(1)
                                if (kitchenSettingModel.showCustomerName && (orderData.customer?.firstName != null || orderData.customer?.lastName != null)) {
                                    add(
                                        PrinterBuilder()
                                            .styleAlignment(Alignment.Left)
                                            .styleBold(true)
                                            .styleMagnification(
                                                MagnificationParameter(2, 2)
                                            )
                                            .actionPrintText(
                                                content = if (kitchenSettingModel.showCustomerName && (orderData.customer?.firstName != null || orderData.customer?.lastName != null)) {
                                                    "Customer Details\n"
                                                } else ""
                                            )
                                    )
                                }

                                if (kitchenSettingModel.showCustomerName && (orderData.customer?.firstName != null || orderData.customer?.lastName != null)) {
                                    add(
                                        PrinterBuilder()
                                            .styleAlignment(Alignment.Center)
                                            .actionPrintText(
                                                content = if (kitchenSettingModel.showCustomerName && (orderData.customer?.firstName != null || orderData.customer?.lastName != null)) {
                                                    "--------------------------------------------"
                                                } else ""
                                            )
                                    )
                                }
                                if (kitchenSettingModel.showCustomerName && (orderData.customer?.firstName != null || orderData.customer?.lastName != null)) {
                                    add(
                                        PrinterBuilder()
                                            .styleAlignment(Alignment.Left)
                                            .styleMagnification(
                                                MagnificationParameter(2, 2)
                                            )
                                            .actionPrintText(
                                                content = if (kitchenSettingModel.showCustomerName && (orderData.customer?.firstName != null || orderData.customer?.lastName != null)) {
                                                    orderData.customer?.firstName + " " + orderData.customer?.lastName
                                                } else ""
                                            )
                                    )
                                }
                                try {
                                    if (kitchenSettingModel.showCustomerPhone && orderData.customer?.phones?.get(
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
                                                    content = if (kitchenSettingModel.showCustomerPhone && orderData.customer?.phones?.get(
                                                            0
                                                        ) != null
                                                    ) {

                                                        var phoneNumber =
                                                            orderData.customer?.phones?.get(
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

                                try {
                                    if (kitchenSettingModel.showCustomerAddress && orderData.customer?.addresses?.get(
                                            0
                                        ) != null
                                    ) {
                                        val address = orderData.customer.addresses.get(0).fullAddress

                                        add(
                                            PrinterBuilder()
                                                .styleAlignment(Alignment.Left)
                                                .styleMagnification(
                                                    MagnificationParameter(2, 2)
                                                )
                                                .actionPrintText(
                                                    content = address
                                                )
                                        )
                                    }
                                } catch (e: Exception) {
                                }


                                printerBuilder.actionFeedLine(1).actionCut(CutType.Partial)


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

                        /*try {
                        SunmiPrintHelper.getInstance().openCashBox()
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }*/

                        Log.d("Printing", "Success")
                    } catch (e: Exception) {
                        Log.d("Printing", "Error: ${e}")
                    } finally {
                        printer.closeAsync().await()
                    }


                }

            } else if (data.name.contains(LANDI_INNER_PRINTER, true)) {
                printKitchenFromLandiInner(data, orderData)
            } else {

                if (!data.name.substring(0, 6).toString().lowercase()
                        .contains("TM-m".lowercase())
                ) {
                    var mPrinter = if (data.name.substring(0, 6).toString().lowercase()
                            .contains("TM-m".lowercase())
                    ) {
                        Log.e(TAG, "YesContains")
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

                        Log.e(
                            TAG,
                            "PrinterEvent  ${Gson().toJson(printerStatusInfo)} other1 ${s}  other2 ${i}"
                        )
                        if (printerStatusInfo.online == 1) {
                            try {
                                printer.disconnect()

                            } catch (e: java.lang.Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    try {
                        Log.e(TAG, "printerDataType:  ${data.printer_type}")

                        var printerAdd =
                            if (data.printer_type == Constants.BLUETOOTH) "BT:" + data.macAddress else "TCP:" + data.ipAddress
                        mPrinter.connect(
                            printerAdd,
                            Printer.PARAM_DEFAULT
                        )
                        mPrinter.startMonitor()

                        generateKitchenReceiptU220(data, type, orderData, mPrinter)

                        // generateReceiptForU220(mPrinter, data, type)

                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                } else {
                    PrinterClass.closePrinter()
                    if (PrinterClass.getPrinter() == null) {
                        //  printerDialog.show(requireContext())

                        var printer: Print? = Print(requireContext())
                        /*if (printer != null) {
                   printer.setStatusChangeEventCallback(this)
                   printer.setBatteryStatusChangeEventCallback(this)
               }*/


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
                           // printer?.setStatusChangeEventCallback(this@ReasonForCancelOrderDialog)

                        } catch (e: Exception) {
                            //  printerDialog.dismiss()
                            LogUtil.logE(TAG, "PrinterException: " + e.message)
                            printer = null
                            return
                        }

                        if (printer != null) {
                            PrinterClass.setPrinter(printer)


                            generateKitchenReceipt(data, type, orderData)

                        }

                    } else {
                        LogUtil.logE(TAG, "PrinterIsNotNull:")
                    }

                }

            }

        }


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


    private fun generateKitchenReceiptU220(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        orderData: OnlineOrderResponseModel.Data,
        builder: Printer
    ) {

        try {
            val pname = if (customerReceiptPrinters.name.substring(0, 6).toString()
                    .lowercase() == "TM-m30".lowercase()
            ) {
                "TM-m30"
            } else {
                customerReceiptPrinters.name
            }

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
            builder.addText("***** CANCELLED *****")
            builder.addFeedUnit(30)
            builder.addFeedLine(1)

            if (customerReceiptPrinters.name.substring(0, 4)
                    .equals("TM-U", true) || customerReceiptPrinters.name.contains("U")
            ) {
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
                    builder.addText("OrderID:" + orderData.custom_order_id)
                } else {
                    builder.addText("OrderID:" + orderData.id)

                }
                builder.addFeedUnit(30)
                builder.addFeedLine(1)

                if (kitchenSettingModel.showOrderType) {


                    builder.addFeedLine(0)
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

                    addBuilderTextForU220(builder, orderData.orderTypeName.toString())

                    if ((orderData.orderType.equals(Constants.PHONE_ORDER, true) ||
                                orderData.orderType.equals("OnlineWebOrder", true) ||
                                orderData.orderType.equals("Online Order", true) ||
                                orderData.orderType.equals(
                                    "OnlineOrder",
                                    true
                                )) && orderData.deliveryType != null
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

                        addBuilderTextForU220(builder, orderData.deliveryType.toString())
                        builder.addFeedLine(1)
                    }
                }


                if (kitchenSettingModel.showTeamMember) {

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
                            "Employee:" + prefProvider.getValue(EMPLOYEE_NAME, ""), "",
                            33
                        )
                    )

                }
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
                            orderData.createdAt
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

                addHorizontalKitchenLineForU220(builder)


                addOrdersForKitchenOnlineOrderU220(
                    builder,
                    orderData.orderItems,
                    fontSizeH,
                    fontSizeW,
                    customerReceiptPrinters.printerCategories.toCollection(arrayListOf())
                )


                if (orderData.note.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
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


                    builder.addText(orderData.note.toString())
                }


                if (kitchenSettingModel.showCustomerAddress != false or kitchenSettingModel.showCustomerPhone != false or kitchenSettingModel.showCustomerName != false) {
                    if (orderData.customer != null) {

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
                        addHorizontalKitchenLineForU220(builder)

                        if (kitchenSettingModel.showCustomerName) {

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
                            builder.addText(orderData.customer.firstName + " " + orderData.customer.lastName)

                        }


                        if (kitchenSettingModel.showCustomerPhone) {

                            if (orderData.customer.phones.isNotEmpty() == true) {
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
                                        orderData.customer.phones?.get(
                                            orderData.customer.phones.size - 1
                                        )?.phoneNumber
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


                        if (orderData.customer.addresses.isNotEmpty() == true) {

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

                            orderData.customer.addresses.filter {
                                it.typeOfAddress == Constants.BILLING_ADDRESS
                            }

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
//                            builder.addText(orderData?.data?.customer?.addresses.get(orderData?.data?.customer?.addresses.size - 1).fullAddress)
                        }
                    }


                }
            } else {


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
                    builder.addText("OrderID:" + orderData.custom_order_id)
                } else {
                    builder.addText("OrderID:" + orderData.id)
                }
                builder.addFeedUnit(30)
                builder.addFeedLine(1)

                if (kitchenSettingModel.showOrderType) {


                    builder.addFeedLine(0)
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

                    addBuilderTextForU220(builder, orderData.orderTypeName.toString())

                    if ((orderData.orderType.equals(Constants.PHONE_ORDER, true) ||
                                orderData.orderType.equals("OnlineWebOrder", true) ||
                                orderData.orderType.equals("Online Order", true) ||
                                orderData.orderType.equals(
                                    "OnlineOrder",
                                    true
                                )) && orderData.deliveryType != null
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

                        addBuilderTextForU220(builder, orderData.deliveryType.toString())
                        builder.addFeedLine(1)
                    }
                }



                if (kitchenSettingModel.showTeamMember) {

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
                            "Employee:" + prefProvider.getValue(EMPLOYEE_NAME, ""), "",
                            if (kitchenSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )

                }
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
                            orderData.createdAt
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

                addHorizontalKitchenLineForU220(builder)


                addOrdersForKitchenOnlineOrderU220(
                    builder,
                    orderData.orderItems,
                    fontSizeH,
                    fontSizeW,
                    customerReceiptPrinters.printerCategories.toCollection(arrayListOf())
                )


                if (orderData.note.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
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


                    builder.addText(orderData.note.toString())
                }


                if (kitchenSettingModel.showCustomerAddress != false or kitchenSettingModel.showCustomerPhone != false or kitchenSettingModel.showCustomerName != false) {
                    if (orderData.customer != null) {

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

                        builder.addFeedLine(1)
                        addHorizontalKitchenLineForU220(builder)

                        if (kitchenSettingModel.showCustomerName) {

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
                            builder.addText(orderData.customer?.firstName + " " + orderData.customer?.lastName)

                        }


                        if (kitchenSettingModel.showCustomerPhone) {

                            if (orderData.customer.phones.isNotEmpty() == true) {
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
                                        orderData.customer.phones.get(
                                            orderData.customer.phones.size - 1
                                        )?.phoneNumber
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


                        if (orderData.customer.addresses.isNotEmpty() == true) {

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

                            orderData.customer.addresses.filter { it.typeOfAddress == Constants.BILLING_ADDRESS }
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
//                            builder.addText(orderData?.data?.customer?.addresses.get(orderData?.data?.customer?.addresses.size - 1).fullAddress)
                        }
                    }


                }
            }
        } catch (e: Exception) {
            // printerDialog.dismiss()
            e.printStackTrace()
        }

        builder?.addFeedLine(2)

        builder?.addCut(Builder.CUT_FEED)

        try {
            builder.sendData(Printer.PARAM_DEFAULT)


        } catch (e: java.lang.Exception) {

            e.printStackTrace()
        }

        /*val status = IntArray(1)
        val battery = IntArray(1)

        var timeOut = PrinterClass.SEND_TIMEOUT
        if (customerReceiptPrinters.printer_type == Constants.BLUETOOTH) {
            timeOut = PrinterClass.BLUETOOTH_TIMEOUT
        }

        if (customerReceiptPrinters.name.substring(0, 6).toString()
                .lowercase() == "TM-m30".lowercase() && customerReceiptPrinters.printer_type != Constants.BLUETOOTH
        ) {

            timeOut = 1000
        }


*/
        /*  try {
              PrinterClass.getPrinter()?.sendData(
                  builder,
                  timeOut, status, battery
              )

              //printerDialog.dismiss()
              PrinterClass.closePrinter()

              //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
          } catch (e: Exception) {
  //                printerDialog.dismiss()
              PrinterClass.closePrinter()
              e.printStackTrace()
              LogUtil.logE(TAG, "PrinterError: " + e.localizedMessage)
          }
  */
    }

    private fun generateKitchenReceipt(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        orderData: OnlineOrderResponseModel.Data,
    ) {
        var builder: Builder? = null
        try {
            val pname = if (customerReceiptPrinters.name.substring(0, 6).toString()
                    .lowercase() == "TM-m30".lowercase()
            ) {
                "TM-m30"
            } else {
                customerReceiptPrinters.name
            }

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
            builder.addText("***** CANCELLED *****")
            builder.addFeedUnit(30)
            builder.addFeedLine(2)

            if (customerReceiptPrinters.name.substring(0, 4)
                    .equals("TM-U", true) || customerReceiptPrinters.name.contains("U")
            ) {
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
                    builder.addText("OrderID:" + orderData.custom_order_id)
                } else {
                    builder.addText("OrderID:" + orderData.id)

                }
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addFeedLine(1)

                if (kitchenSettingModel.showOrderType) {


                    builder.addFeedLine(0)
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

                    addBuilderText(builder, orderData.orderTypeName.toString())

                    if ((orderData.orderType.equals(Constants.PHONE_ORDER, true) ||
                                orderData.orderType.equals("OnlineWebOrder", true) ||
                                orderData.orderType.equals("Online Order", true) ||
                                orderData.orderType.equals(
                                    "OnlineOrder",
                                    true
                                )) && orderData.deliveryType != null
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

                        addBuilderText(builder, orderData.deliveryType.toString())
                        builder.addFeedLine(1)
                    }
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
                            "Employee:" + prefProvider.getValue(EMPLOYEE_NAME, ""), "",
                            33
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
                            orderData.createdAt
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


                addOrdersForKitchenOnlineOrder(
                    builder,
                    orderData.orderItems,
                    fontSizeH,
                    fontSizeW,
                    customerReceiptPrinters.printerCategories.toCollection(arrayListOf())
                )


                if (orderData.note.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
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


                    builder.addText(orderData.note.toString())
                }


                if (kitchenSettingModel.showCustomerAddress != false or kitchenSettingModel.showCustomerPhone != false or kitchenSettingModel.showCustomerName != false) {
                    if (orderData.customer != null) {

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
                            builder.addText(orderData.customer.firstName + " " + orderData.customer.lastName)

                        }


                        if (kitchenSettingModel.showCustomerPhone) {

                            if (orderData.customer.phones.isNotEmpty() == true) {
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
                                        orderData.customer.phones.get(
                                            orderData.customer.phones.size - 1
                                        )?.phoneNumber
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


                        if (orderData.customer.addresses.isNotEmpty() == true) {

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

                            orderData.customer.addresses.filter {
                                it.typeOfAddress == Constants.BILLING_ADDRESS
                            }

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
//                            builder.addText(orderData?.data?.customer?.addresses.get(orderData?.data?.customer?.addresses.size - 1).fullAddress)
                        }
                    }


                }
            } else {


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
                    builder.addText("OrderID:" + orderData.custom_order_id)
                } else {
                    builder.addText("OrderID:" + orderData.id)
                }
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addFeedLine(1)

                if (kitchenSettingModel.showOrderType) {


                    builder.addFeedLine(0)
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

                    addBuilderText(builder, orderData.orderTypeName.toString())

                    if ((orderData.orderType.equals(Constants.PHONE_ORDER, true) ||
                                orderData.orderType.equals("OnlineWebOrder", true) ||
                                orderData.orderType.equals("Online Order", true) ||
                                orderData.orderType.equals(
                                    "OnlineOrder",
                                    true
                                )) && orderData.deliveryType != null
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

                        addBuilderText(builder, orderData.deliveryType.toString())
                        builder.addFeedLine(1)
                    }
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
                            "Employee:" + prefProvider.getValue(EMPLOYEE_NAME, ""), "",
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
                            orderData.createdAt
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


                addOrdersForKitchenOnlineOrder(
                    builder,
                    orderData.orderItems,
                    fontSizeH,
                    fontSizeW,
                    customerReceiptPrinters.printerCategories.toCollection(arrayListOf())
                )


                if (orderData.note.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
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


                    builder.addText(orderData.note.toString())
                }


                if (kitchenSettingModel.showCustomerAddress != false or kitchenSettingModel.showCustomerPhone != false or kitchenSettingModel.showCustomerName != false) {
                    if (orderData.customer != null) {

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

                        builder.addFeedLine(1)
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
                            builder.addText(orderData.customer.firstName + " " + orderData.customer.lastName)

                        }


                        if (kitchenSettingModel.showCustomerPhone) {

                            if (orderData.customer.phones.isNotEmpty() == true) {
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
                                        orderData.customer.phones.get(
                                            orderData.customer.phones.size - 1
                                        ).phoneNumber
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


                        if (orderData.customer.addresses.isNotEmpty() == true) {

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

                            orderData.customer.addresses.filter { it.typeOfAddress == Constants.BILLING_ADDRESS }
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
//                            builder.addText(orderData?.data?.customer?.addresses.get(orderData?.data?.customer?.addresses.size - 1).fullAddress)
                        }
                    }


                }
            }
        } catch (e: Exception) {
            // printerDialog.dismiss()
            e.printStackTrace()
        }

        builder?.addFeedLine(2)

        builder?.addCut(Builder.CUT_FEED)

        val status = IntArray(1)
        val battery = IntArray(1)

        var timeOut = PrinterClass.SEND_TIMEOUT
        if (customerReceiptPrinters.printer_type == Constants.BLUETOOTH) {
            timeOut = PrinterClass.BLUETOOTH_TIMEOUT
        }

        if (customerReceiptPrinters.name.substring(0, 6).toString()
                .lowercase() == "TM-m30".lowercase() && customerReceiptPrinters.printer_type != Constants.BLUETOOTH
        ) {

            timeOut = 1000
        }



        try {
            PrinterClass.getPrinter()?.sendData(
                builder,
                timeOut, status, battery
            )

            //printerDialog.dismiss()
            PrinterClass.closePrinter()

            //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
        } catch (e: Exception) {
//                printerDialog.dismiss()
            PrinterClass.closePrinter()
            e.printStackTrace()
            LogUtil.logE(TAG, "PrinterError: " + e.localizedMessage)
        }


    }


    private fun generateKitchenReceiptSunmi(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        orderData: OnlineOrderResponseModel.Data,
        isCancelOrder: Boolean = false
    ) {

        try {



            if (isCancelOrder) {
                PrintSunmiUtils.orderIdLarge("***** CANCELLED *****")
            }
            PrintSunmiUtils.fontSize(kitchenSettingModel.fonts)
            SunmiPrinterApi.getInstance().lineWrap(2)
            if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                PrintSunmiUtils.orderIdSunmi(
                    "OrderID:" + orderData.custom_order_id
                )
            } else {
                PrintSunmiUtils.orderIdSunmi(
                    "OrderID:" + orderData.id
                )
            }

            SunmiPrinterApi.getInstance().lineWrap(1)

            if (kitchenSettingModel.showOrderType) {
                PrintSunmiUtils.printOrderType(orderData.orderTypeName.toString())
                SunmiPrinterApi.getInstance().lineWrap(1)

                if ((orderData.orderType.equals(Constants.PHONE_ORDER, true) ||
                            orderData.orderType.equals("OnlineWebOrder", true) ||
                            orderData.orderType.equals("Online Order", true) ||
                            orderData.orderType.equals(
                                "OnlineOrder",
                                true
                            )) && orderData.deliveryType != null
                ) {
                    PrintSunmiUtils.printOrderType(orderData.deliveryType.toString())
                    SunmiPrinterApi.getInstance().lineWrap(1)
                }
            }

            if (kitchenSettingModel.showTeamMember) {

                PrintSunmiUtils.employee(
                    padLine(
                        "Employee:" + prefProvider.getValue(EMPLOYEE_NAME, ""), "",
                        if (kitchenSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )


            }

            PrintSunmiUtils.orderTime(
                padLine(
                    Constants.getReceiptFormatDateFromUTCServer(
                        requireContext(),
                        orderData.createdAt
                    ),
                    "",
                    if (kitchenSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
            )

            PrintSunmiUtils.addHorizontal()


            addOrdersForKitchenOnlineOrderSunmi(
                orderData.orderItems,
                customerReceiptPrinters.printerCategories.toCollection(arrayListOf())
            )


            if (orderData.note.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
                SunmiPrinterApi.getInstance().lineWrap(1)
                PrintSunmiUtils.orderNote(orderData?.note.toString())
            }


            if (kitchenSettingModel.showCustomerAddress != false or kitchenSettingModel.showCustomerPhone != false or kitchenSettingModel.showCustomerName != false) {
                if (orderData.customer != null) {
                    SunmiPrinterApi.getInstance().lineWrap(1)
                    PrintSunmiUtils.customerDetails()

                    try {
                        if (kitchenSettingModel.showCustomerName) {
                            PrintSunmiUtils.customerName(orderData.customer.firstName + " " + orderData.customer.lastName)

                        }
                    } catch (e: Exception) {

                    }

                    try {
                        if (kitchenSettingModel.showCustomerPhone) {

                            if (orderData.customer.phones.isNotEmpty() == true) {

                                PrintSunmiUtils.customerPhone(
                                    MethodUtils.getUSFormatNumber(
                                        orderData.customer.phones.get(
                                            orderData.customer.phones.size - 1
                                        ).phoneNumber
                                    )

                                )
                            }

                        }
                    } catch (e: Exception) {

                    }


                    try {
                        if (orderData.customer.addresses.isNotEmpty() == true) {

//                        orderData.customer.addresses.filter { typeOfAddress == Constants.BILLING_ADDRESS }
//
//                            .forEach {
//
//                                if (typeOfAddress.equals(
//                                        Constants.BILLING_ADDRESS,
//                                        ignoreCase = true
//                                    )
//                                ) {
//                                    PrintSunmiUtils.customerAddress(
//                                        fullAddress
//                                    )
//                                }
//                            }


                            PrintSunmiUtils.customerAddress(
                                orderData.customer.addresses.get(
                                    orderData.customer.addresses.size - 1
                                ).fullAddress
                            )
                        }
                    } catch (e: Exception) {

                    }
                }
            }

            PrintSunmiUtils.cutPaper()
        } catch (e: Exception) {
            // printerDialog.dismiss()
            e.printStackTrace()
        }


    }

    fun bytesToHexString(bytes: ByteArray): String {
        val hexstr = java.lang.StringBuilder()
        for (i in bytes) hexstr.append(String.format("%02x", i))
        return hexstr.toString()
    }
    @Throws(java.lang.Exception::class)
    fun generateSign(body: String, timestamp: String, nonce: String): String {

        val msg = body + "889a389072224d10b641e90b9cc26856" + timestamp + nonce
        val hmacSha256 = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec("1f486ca8d9a341408c8132b23f82f571".encodeToByteArray(), "HmacSHA256")
        hmacSha256.init(secretKey)
        val result = hmacSha256.doFinal(msg.toByteArray(charset("UTF-8")))
        return bytesToHexString(result)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Throws(
        NoSuchAlgorithmException::class,
        InvalidKeySpecException::class,
        InvalidKeyException::class,
        SignatureException::class
    )
    fun sign(
        body: String,
        appId: String,
        timestamp: String,
        nonce: String,
        rsaPrivateKey: String
    ): String {
        val content = body + appId + timestamp + nonce
        val keyBytes: ByteArray =
            java.util.Base64.getDecoder().decode(rsaPrivateKey.replace("(\\s)|(--.*--)".toRegex(), ""))
        val pkcs8KeySpec = PKCS8EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        val priKey = keyFactory.generatePrivate(pkcs8KeySpec)
        val signature: Signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(priKey)
        signature.update(content.toByteArray())
        return java.util.Base64.getEncoder().encodeToString(signature.sign())
    }

    fun httpPost(path: String, body: String,sn:String?=null): String? {
        var connection: HttpURLConnection? = null
        var `is`: InputStream? = null
        var os: OutputStream? = null
        var br: BufferedReader? = null
        var result: String? = null

        val date = Date()
        val random = Random()
        val timestamp = String.format("%d", date.time / 1000)
        val nonce = String.format("%06d", random.nextInt(1000000))

        try {
            Log.e(TAG,"InitalizeRequestQueue ")
            val url = URL("https://openapi.sunmi.com$path")
            connection = url.openConnection() as HttpURLConnection
            connection!!.requestMethod = "POST"
            connection!!.connectTimeout = 15000
            connection!!.readTimeout = 60000

            connection!!.doOutput = true
            connection!!.doInput = true
            connection!!.setRequestProperty("Sunmi-Appid", Constants.SUNMI_APP_ID)
            connection!!.setRequestProperty("Sunmi-Timestamp", timestamp)
            connection!!.setRequestProperty("Sunmi-Nonce", nonce)
            connection!!.setRequestProperty("Sunmi-Sign", generateSign(body, timestamp, nonce))
            connection!!.setRequestProperty("Source", "openapi")
            connection!!.setRequestProperty("Content-Type", "application/json")
            os = connection!!.outputStream
            os.write(body.toByteArray(charset("UTF-8")))
            if (connection!!.responseCode == 200) {
                Log.e(TAG,"QueueREsponseOK ")

                /*  if (sn.equals("N434227FT0790")) {
                      orderContent.clear()
                       orderContent = java.lang.StringBuilder()
                      cloudQueuePrinting("N434227FT0738", 2241)
                  }*/

                /* if (path.contains("pushContent")){

                     CoroutineScope(Dispatchers.IO).launch {
                         delay(500)
                         clearPrintJob(sn)
                     }
                 }*/

                `is` = connection!!.inputStream
                br = BufferedReader(InputStreamReader(`is`, "UTF-8"))

                val sbf = StringBuffer()
                var temp: String? = null
                while ((br.readLine().also { temp = it }) != null) {
                    sbf.append(temp)
                    sbf.append("\n")
                }
                result = sbf.toString()
            }
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            if (br != null) {
                try {
                    br.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            if (os != null) {
                try {
                    os.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            if (`is` != null) {
                try {
                    `is`.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            connection!!.disconnect()
        }
        return result
    }

    fun bindShop(sn: String?, shop_id: Int): String? {
        val body = java.lang.StringBuilder()
        body.append("{")
        body.append(String.format("\"sn\":\"%s\"", sn))
        body.append(",")
        body.append(String.format("\"shop_id\":%d", shop_id))
        body.append("}")
        return httpPost("/v2/printer/open/open/device/bindShop", body.toString())
    }

    fun onlineStatus(sn: String?): String? {
        val body = java.lang.StringBuilder()
        body.append("{")
        body.append(String.format("\"sn\":\"%s\"", sn))
        body.append("}")
        return httpPost("/v2/printer/open/open/device/onlineStatus", body.toString())
    }

    fun pushContent(
        trade_no: String?,
        sn: String?,
        count: Int,
        order_type: Int,
        media_text: String?,
        cycle: Int
    ): String? {
        val body = java.lang.StringBuilder()
        body.append("{")
        body.append(String.format("\"trade_no\":\"%s\"", "${System.currentTimeMillis()}"))
        body.append(",")
        body.append(String.format("\"sn\":\"%s\"", sn))
        body.append(",")
        body.append(String.format("\"order_type\":%d", order_type))
        body.append(",")
        body.append(java.lang.String.format("\"content\":\"%s\"", orderContent.toString()))
        body.append(",")
        body.append(String.format("\"count\":%d", count))
        body.append(",")
        body.append(String.format("\"media_text\":\"%s\"", media_text))
        body.append(",")
        body.append(String.format("\"cycle\":%d", cycle))
        body.append("}")
        return httpPost("/v2/printer/open/open/device/pushContent", body.toString(),sn)
    }

    fun appendText(text: String) {
        try {
            val bytes = text.toByteArray(charset("UTF-8"))
            for (i in bytes) orderContent.append(String.format("%02x", i))
        } catch (e: UnsupportedEncodingException) {
        }
    }

    fun printAndExitPageMode() {
        orderContent.append("0c")
    }


    fun cutPaper(full_cut: Boolean) {
        orderContent.append("1d56" + (if ((full_cut)) "30" else "31"))
    }
    fun lineFeed(n: Int) {
        for (i in 0 until n) orderContent.append("0a")
    }


    fun setAlignment(n: Int) {
        if (n >= 0 && n <= 2) orderContent.append("1b61" + String.format("%02x", n))
    }

    fun clearPrintJob(sn: String?): String? {
        Log.e(TAG,"chekSNCall: ${sn}")
        val body = java.lang.StringBuilder()
        body.append("{")
        body.append(String.format("\"sn\":\"%s\"", sn))
        body.append("}")
        return httpPost("/v2/printer/open/open/device/clearPrintJob", body.toString(),sn)
    }


    // Append raw data.
    fun appendRawData(bytes: ByteArray) {
        for (i in bytes) orderContent.append(String.format("%02x", i))
    }

    // Append unicode character.
    fun appendUnicode(unicode: Int, count: Int) {
        if (count > 0) {
            val text = StringBuilder()
            for (i in 0 until count) text.append(unicode.toChar())
            appendText(text.toString())
        }

    }

    // [ESC 3] Set line spacing.
    fun setLineSpacing(n: Int) {
        if (n >= 0 && n <= 255) orderContent.append("1b33" + String.format("%02x", n))
    }

    // [ESC !] Set print modes.
    fun setPrintModes(bold: Boolean, double_h: Boolean, double_w: Boolean) {
        var n = 0
        if (bold) n = n or 8
        if (double_h) n = n or 16
        if (double_w) n = n or 32
        charHSize = if ((double_w)) 2 else 1
        orderContent.append("1b21" + String.format("%02x", n))
    }

    // [HT] Jump to next TAB position.
    fun horizontalTab(n: Int) {
        for (i in 0 until n) orderContent.append("09")
    }

    // [ESC $] Set absolute print position.
    fun setAbsolutePrintPosition(n: Int) {
        if (n >= 0 && n <= 65535) orderContent.append(
            "1b24" + String.format(
                "%02x%02x",
                (n and 0xff),
                ((n shr 8) and 0xff)
            )
        )
    }

    // [ESC \] Set relative print position.
    fun setRelativePrintPosition(n: Int) {
        if (n >= -32768 && n <= 32767) orderContent.append(
            "1b5c" + String.format(
                "%02x%02x",
                (n and 0xff),
                ((n shr 8) and 0xff)
            )
        )
    }


    // [ESC -] Set underline mode.
    fun setUnderlineMode(n: Int) {
        if (n >= 0 && n <= 2) orderContent.append("1b2d" + String.format("%02x", n))
    }

    // [GS B] Set black-white reverse mode.
    fun setBlackWhiteReverseMode(enabled: Boolean) {
        orderContent.append("1d42" + (if ((enabled)) "01" else "00"))
    }

    // [ESC {] Set upside down mode.
    fun setUpsideDownMode(enabled: Boolean) {
        orderContent.append("1b7b" + (if ((enabled)) "01" else "00"))
    }

    private fun setServiceForKitchen(
        data: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        orderData: OnlineOrderResponseModel.Data
    ) {
        if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.FoundSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper", "FoundSunmiPrinter")

            if (!BluetoothUtil.isBlueToothPrinter) {

                generateKitchenReceiptSunmiInner(data, type, orderData)

            }

        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.CheckSunmiPrinter) {
            Handler(Looper.getMainLooper()).postDelayed({
                setServiceForKitchen(data, type, orderData)
            }, 2000)
            LogUtil.logE("SunmiPrintHelper", "CheckSunmiPrinter")
        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.LostSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper", "LostSunmiPrinter")
        } else {
            LogUtil.logE("SunmiPrintHelper", "ELSE")
        }
    }

    private fun generateKitchenReceiptSunmiInner(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        orderData: OnlineOrderResponseModel.Data
    ) {

        try {

            SunmiPrintHelper.getInstance().initPrinter()
            SunmiPrintHelper.getInstance().lineWrap(2)

            PrintSunmiUtils.headerText("***** CANCELLED *****")
            SunmiPrintHelper.getInstance().lineWrap(2)


            if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                PrintSunmiUtils.headerText("OrderID:" + orderData.custom_order_id)
            } else {
                PrintSunmiUtils.headerText("OrderID:" + orderData.id)
            }
            SunmiPrintHelper.getInstance().lineWrap(1)


            if (kitchenSettingModel.showOrderType) {
                PrintSunmiUtils.headerText(orderData.orderTypeName.toString())
                SunmiPrintHelper.getInstance().lineWrap(1)

                if ((orderData.orderType.equals(Constants.PHONE_ORDER, true) ||
                            orderData.orderType.equals("OnlineWebOrder", true) ||
                            orderData.orderType.equals("Online Order", true) ||
                            orderData.orderType.equals(
                                "OnlineOrder",
                                true
                            )) && orderData.deliveryType != null
                ) {
                    PrintSunmiUtils.headerText(orderData.deliveryType.toString())
                    SunmiPrintHelper.getInstance().lineWrap(1)
                }
            }

            if (kitchenSettingModel.showTeamMember) {

                PrintSunmiUtils.normalTextLarge(
                    "Employee:" + prefProvider.getValue(
                        EMPLOYEE_NAME,
                        ""
                    )
                )


            }

            PrintSunmiUtils.normalTextLarge(
                Constants.getReceiptFormatDateFromUTCServer(
                    requireContext(),
                    orderData.createdAt
                )
            )


            if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt()!=39){
                PrintSunmiUtils.addHorizontalInnerNew()
            }else{
                PrintSunmiUtils.addHorizontalInner()
            }

            if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt()!=39){
                PrintSunmiUtils.normalText("\n")
            }

            addOrdersForKitchenOnlineOrderSunmiInner(
                orderData.orderItems,
                customerReceiptPrinters.printerCategories.toCollection(arrayListOf())
            )


            if (orderData.note.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {

                PrintSunmiUtils.orderNoteInnerLarge(orderData.note.toString())
            }


            try {
                if (kitchenSettingModel.showCustomerAddress != false or kitchenSettingModel.showCustomerPhone != false or kitchenSettingModel.showCustomerName != false) {
                    if (orderData.customer != null) {
                        if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)
                                ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
                        ) {
                            PrintSunmiUtils.normalText("\n")
                        }
                        PrintSunmiUtils.customerDetailsInner(true,sunmiFrameworkVersion)
                        if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)
                                ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
                        ) {
                            PrintSunmiUtils.addHorizontalInnerNew()
                        }else{
                            PrintSunmiUtils.addHorizontalInnerSmall()
                        }
                        if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt()!=39){
                            PrintSunmiUtils.normalText("\n")
                        }

                        if (kitchenSettingModel.showCustomerName) {
                            PrintSunmiUtils.normalTextLarge(orderData.customer.firstName + " " + orderData.customer.lastName)

                        }


                        if (kitchenSettingModel.showCustomerPhone) {

                            if (orderData.customer.phones.isNotEmpty() == true) {

                                PrintSunmiUtils.normalTextLarge(
                                    MethodUtils.getUSFormatNumber(
                                        orderData.customer.phones?.get(
                                            orderData.customer.phones.size - 1
                                        ).phoneNumber
                                    )

                                )
                            }

                        }


                        if (orderData.customer.addresses.isNotEmpty() == true) {

                            PrintSunmiUtils.normalTextLarge(
                                orderData.customer.addresses.get(0).fullAddress
                            )


//                        PrintSunmiUtils.normalTextLarge(
//                            orderData?.data?.customer?.addresses.get(
//                                orderData?.data?.customer?.addresses.size - 1
//                            ).fullAddress
//                        )


//                            orderData.customer.addresses.filter { it.typeOfAddress == Constants.BILLING_ADDRESS }
//                                .forEach {
//
//                                    if (it.typeOfAddress.equals(
//                                            Constants.BILLING_ADDRESS,
//                                            ignoreCase = true
//                                        )
//                                    ) {
//                                        PrintSunmiUtils.normalTextLarge(
//                                            it.fullAddress
//                                        )
//                                    }
//                                }
                        }
                    }


                }
            } catch (e: Exception) {
            }

            PrintSunmiUtils.cutPaperInner()
        } catch (e: Exception) {
            // printerDialog.dismiss()
            e.printStackTrace()
        }


    }

    private fun printKitchenFromLandiInner(
        kitchenReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        receiptModel: OnlineOrderResponseModel.Data,
    ) {
//        val printer = com.dantsu.escposprinter.EscPosPrinter(BluetoothPrintersConnections.selectFirstPaired(), 203, 48f, 32)

        this.checkBluetoothPermissions(object :
            OnBluetoothPermissionGranted {
            override fun onPermissionsGranted() {
                GlobalScope.launch {
                    LPrint.connectLandiInnerPrinter(kitchenReceiptPrinters.macAddress)
                        ?.let { outputStream ->

                            LPrint.apply {
                                setOutputStream(outputStream)
                                try {

//                                    lineBreak()
//                                    cartModel?.let {
//                                        if (it.isEdited || isOrderUpdate) {
//                                            printCenter("***** UPDATED *****", isBold = true, fontSize = FONT_SIZE_5X)
//
//                                        }
//                                    }
//                                    lineBreak()
//                                    lineBreak()
                                    printCenter("***** CANCELLED *****", isBold = true, fontSize = FONT_SIZE_5X)
                                    lineBreak()

                                    if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                                        printCenter("OrderID:" + receiptModel?.custom_order_id, isBold = true, fontSize = FONT_SIZE_5X)
                                        lineBreak()
                                    } else {
                                        printCenter("OrderID:" + receiptModel?.id, isBold = true, fontSize = FONT_SIZE_5X)
                                    }
                                    lineBreak()

                                    if (kitchenSettingModel.showOrderType) {

                                        printCenter(receiptModel?.orderTypeName, isBold = true, fontSize = FONT_SIZE_5X)
                                        lineBreak()

                                        if ((receiptModel.orderType.equals(Constants.PHONE_ORDER, true) ||
                                                    receiptModel.orderType.equals("OnlineWebOrder", true) ||
                                                    receiptModel.orderType.equals("Online Order", true) ||
                                                    receiptModel.orderType.equals("OnlineOrder", true)) &&
                                            receiptModel.deliveryType != null
                                        ) {
                                            printCenter(receiptModel.deliveryType, isBold = true, fontSize = FONT_SIZE_5X)
                                            lineBreak()
                                        }
                                    }
                                    //   PrintSunmiUtils.headerText(receiptModel?.deliveryType.toString())

                                    lineBreak()


                                    if (kitchenSettingModel.showTeamMember) {
                                        printLeft("Employee:" + receiptModel?.employee?.name, isBold = true, fontSize = FONT_SIZE_5X)
                                    }
                                    lineBreak()


                                    printLeft(
                                        Constants.getReceiptFormatDateFromUTCServer(
                                            requireContext(),
                                            receiptModel?.createdAt.toString()
                                        ),
                                        isBold = true, fontSize = FONT_SIZE_5X
                                    )

                                    lineBreak()
                                    printDashedLineAndBreak()
                                    lineBreak()

//                                    if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt()!=39){
//                                        PrintSunmiUtils.normalText("\n")
//                                    }

                                    receiptModel?.orderItems?.let {

                                        addOrdersForKitchenOnlineOrderLandi(
                                            it,
                                            kitchenReceiptPrinters.printerCategories.toCollection(arrayListOf()),
                                            LPrint
                                        )
                                    }

                                    if (receiptModel?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {

                                        lineBreak()
                                        printCenter("Order Note",isBold = true,
                                            fontSize = FONT_SIZE_5X
                                        )
                                        lineBreak()
                                        printCenter(receiptModel?.note.toString(), isBold = true, fontSize = FONT_SIZE_5X)
                                        lineBreak()
                                    }


                                    if (kitchenSettingModel.showCustomerAddress != false || kitchenSettingModel.showCustomerPhone != false || kitchenSettingModel.showCustomerName) {
                                        try{
                                            if (receiptModel?.customer != null) {

//                                                PrintSunmiUtils.customerDetailsInner(true,sunmiFrameworkVersion)

//                                                if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt()!=39){
//                                                    PrintSunmiUtils.normalText("\n")
//                                                }

                                                lineBreak()
                                                printLeft("Customer Details", isBold = true,
                                                    fontSize = FONT_SIZE_5X
                                                )
                                                lineBreak()
                                                printDashedLineAndBreak()
                                                try{
                                                    if (kitchenSettingModel.showCustomerName) {
                                                        printLeft(receiptModel?.customer?.firstName + " " + receiptModel?.customer?.lastName, isBold = true, fontSize = FONT_SIZE_5X)
                                                    }
                                                }catch (e:Exception){}

                                                lineBreak()
                                                try{
                                                    if (kitchenSettingModel.showCustomerPhone) {

                                                        if (receiptModel?.customer?.phones?.isNotEmpty()) {

                                                            receiptModel?.customer?.phones?.get(0)?.phoneNumber?.let {
                                                                printLeft(
                                                                    MethodUtils.formatPhoneNumber(it), isBold = true, fontSize = FONT_SIZE_5X
                                                                )
                                                            }
                                                        }
                                                    }
                                                }catch (e:Exception){

                                                }

                                                lineBreak()

                                                try{
                                                    if (kitchenSettingModel.showCustomerAddress) {

                                                        if (receiptModel?.orderType.trim()
                                                                .lowercase() == "Open Order".trim()
                                                                .lowercase() && receiptModel?.deliveryType.trim()
                                                                .lowercase() == "Pickup".trim()
                                                                .lowercase()
                                                        ) {

                                                        } else if (receiptModel?.customer?.addresses?.isNotEmpty()) {

                                                            printLeft(
                                                                receiptModel.customer.addresses.get(0).fullAddress,
                                                                isBold = true,
                                                                fontSize = FONT_SIZE_5X
                                                            )

//                                                            receiptModel?.customer?.addresses?.filter { it.typeOfAddress == Constants.BILLING_ADDRESS }
//                                                                ?.forEach {
//
//                                                                    if (it.typeOfAddress.equals(
//                                                                            Constants.BILLING_ADDRESS,
//                                                                            ignoreCase = true
//                                                                        )
//                                                                    ) {
//                                                                        printLeft(
//                                                                            it.fullAddress
//                                                                        )
//                                                                    }
//                                                                }


                                                        }
                                                    }
                                                }catch (e:Exception){

                                                }
                                            }
                                        }catch (e:Exception){

                                        }
                                    }

                                    lineBreak()
                                    paperCut()
                                    disconnectLandiPrinter()

//                                    SunmiPrintHelper.getInstance().lineWrap(2)
//
//                                    PrintSunmiUtils.cutPaperInner()




                                } catch (e: Exception) {
                                    e.printStackTrace()

                                }
                            }
                        }
                }
            }
        })
    }

   // var onBluetoothPermissionGranted: com.pays.pos.ui.fragments.allorders.AllOrdersListingFragment.OnBluetoothPermissionGranted? = null


}