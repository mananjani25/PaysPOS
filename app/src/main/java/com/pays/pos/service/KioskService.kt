package com.pays.pos.service

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.epson.epos2.printer.Printer
import com.epson.eposprint.Builder
import com.epson.eposprint.Print
import com.epson.eposprint.StatusChangeEventListener
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.hosopy.actioncable.ActionCable
import com.hosopy.actioncable.Channel
import com.hosopy.actioncable.Consumer
import com.hosopy.actioncable.Subscription
import com.pays.pos.R
import com.pays.pos.data.db.AppDatabase
import com.pays.pos.data.entities.TbLabelPrinterSettings
import com.pays.pos.data.model.responseModel.*
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.BILLING_ADDRESS
import com.pays.pos.data.remote.Constants.LANDI_INNER_PRINTER
import com.pays.pos.data.remote.Constants.ORDER_NUMBER_STARTING_FROM_ONE
import com.pays.pos.data.remote.Constants.PHONE_ORDER
import com.pays.pos.data.remote.Constants.WIFI
import com.pays.pos.data.remote.Constants.getReceiptFormatDateFromUTCServer
import com.pays.pos.data.repositories.KioskRepository
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.di.ApiModule
import com.pays.pos.di.PrefProvider
import com.pays.pos.logger.MessageEvent
import com.pays.pos.ui.activities.MainActivity
import com.pays.pos.ui.fragments.payment.OrderCompleteFragment.OnBluetoothPermissionGranted
import com.pays.pos.ui.fragments.settings.hardware.printer.BluetoothUtil
import com.pays.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper
import com.pays.pos.utils.*
import com.pays.pos.utils.InternetUtils.isInternetAvailable
import com.pays.pos.utils.TimeFormatUtils.prefProvider
import com.pays.pos.utils.landi.LPrint
import com.pays.pos.utils.printer.PrinterClass
import com.starmicronics.stario10.*
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
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URI
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
class KioskService : Service(), StatusChangeEventListener {

    private val TAG = "KioskService"
    private var locationId = 0
    private var terminalId = 0
    private var terminalName = ""
    private var baseUrl = ""
    private var consumer2: Consumer? = null
    private var subscription2: Subscription? = null
    private var kitchenSettingModel = GetKitchenReceiptSettingsResponse.Data()

    /*Star label printer - START*/
    lateinit var settings: StarConnectionSettings
    lateinit var printer: StarPrinter
    /*Star label printer - END*/

    var charHSize: Int = 1
    var asciiCharWidth: Int = 12
    var cjkCharWidth: Int = 24
    var orderContent: java.lang.StringBuilder = java.lang.StringBuilder()

    @Inject
    lateinit var posRepository: PosRepository

    private val notificationChannelId = "foreground_service_channel"

    private var oneItemPerReceipt: Boolean = true

    private var sunmiFrameworkVersion: Array<String>? = null
    private val TAG2="KioskPRinter"

    /*This variable will be used to check if the orderID is to be printed in the sticky receipt */
    private var printOrderIDInStickyPrinter: Boolean = true

    override fun onCreate() {
        super.onCreate()

        sunmiFrameworkVersion =
            PrefProvider(this).getValue(Constants.SUNMI_FRAMEWORK_VERSION, "").toString().split(".")
                .toTypedArray()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                printOrderIDInStickyPrinter =
                    posRepository.getLabelPrinterSettingsData().printOrderId
            } catch (e: Exception) {

            }
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
    }

    @Nullable
    override fun onBind(intent: Intent?): IBinder? {
        return null // We don't intend to bind to this service
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            locationId = it.getIntExtra(Constants.LOCATION_ID, 0)
            terminalId = it.getIntExtra(Constants.TERMINAL_ID, 0)
            terminalName = it.getStringExtra(Constants.TERMINAL_NAME) ?: ""
            baseUrl = it.getStringExtra(Constants.BASE_URL_NEW) ?: ""
        }

        handleData()

        startForeground(1, createNotification())

        return START_STICKY // Or START_NOT_STICKY depending on your needs
    }

    fun handleData() {
        // 1. Setup
        var requestURL = baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3

        Log.e("PrinterRefreshWorker", "requestURL = $requestURL")

        val uri = URI(Constants.PRINTER_QUEUE_CONNECTION_URL_SNACKPOS)
        consumer2 = ActionCable.createConsumer(uri)

        Log.d("PrinterRefreshWorker", "uri = $uri")

        // 2. Create subscription
        val appearanceChannel = Channel("SyncChannel")
        appearanceChannel.addParam("id", locationId)
        // appearanceChannel.addParam("id",prefProvider.getValueInt(LOCATION_ID,0))
        subscription2 =
            consumer2?.subscriptions?.create(appearanceChannel)

        if (subscription2 != null) {
            subscription2?.onConnected {
                val params = JsonObject()
                params.addProperty("location_id", locationId)
                //  params.addProperty("url", requestURL)
                subscription2?.perform("received", params)
            }?.onRejected {
                if (isInternetAvailable(applicationContext)) {
                    consumer2?.connect()
                }

            }?.onReceived {
                Log.e("KioskService", "onReceived")
                /*if (it.asJsonObject.has("location_id")) {
                    if ((PrefProvider(baseContext).getLocationId() == it.asJsonObject.get("location_id").asInt) && (it.asJsonObject.get(
                            "new_order"
                        ).toString().equals("true", ignoreCase = true))
                    ) {
                        getOrderFromServer(it.asJsonObject.get("order_id").asInt)
                    }
                }*/
                if (PrefProvider(applicationContext).getValueboolean(
                        Constants.IS_MASTER_TERMINAL,
                        false
                    )
                ) {
                    try {
                        if (it.asJsonObject.has("location_id")) {
                            if ((PrefProvider(baseContext).getLocationId() == it.asJsonObject.get("location_id").asInt) && (it.asJsonObject.get(
                                    "new_order"
                                ).toString().equals(
                                    "true",
                                    ignoreCase = true
                                ) && (it.asJsonObject.get("order_type").toString()
                                    .equals("\"KioskTakeout\"", true)))
                            ) {
                                getOrderFromServer(it.asJsonObject.get("order_id").asInt)
                            }
                        }
                    } catch (e: Exception) {

                    }
                }

//                {"new_order":true,"order_id":525,"location_id":386}

            }?.onDisconnected {

            }?.onFailed {
                if (isInternetAvailable(applicationContext)) {
                    Handler(Looper.getMainLooper()).postDelayed(kotlinx.coroutines.Runnable {
                        try {
                            consumer2?.connect()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, 25000)

                } else {

                }
            }
        }

        // 3. Establish connection
        consumer2?.connect()
    }

    private fun getOrderFromServer(orderId: Int) {
        CoroutineScope(Dispatchers.IO).launch {

            val authToken = PrefProvider(applicationContext).getValue(Constants.AUTH_TOKEN, "")
            val url = PrefProvider(applicationContext).getValue(
                Constants.BASE_URL_NEW,
                ApiModule.BASE_URL
            )
            if (authToken.isNotEmpty()) {
                val queue = Volley.newRequestQueue(applicationContext)
                val getRequest: StringRequest = object : StringRequest(
                    Request.Method.GET, url.plus("orders/").plus(orderId),
                    object : com.android.volley.Response.Listener<String?> {
                        override fun onResponse(response: String?) {
                            // response
                            Log.d("HEY", "ONResponse")
                            response?.let {
                                try {
                                    EventBus.getDefault().post(
                                        MessageEvent(
                                            "${Constants.LINE_BREAK_TAB} KioskService.kt getOrderFromServer()_SUCCESS-> ${
                                                Gson().fromJson<KioskOrderResponse>(
                                                    it,
                                                    KioskOrderResponse::class.java
                                                )
                                            }"
                                        )
                                    )
                                    getKitchenPrinters(
                                        Gson().fromJson<KioskOrderResponse>(
                                            it,
                                            KioskOrderResponse::class.java
                                        )
                                    )
                                } catch (e: Exception) {
                                    EventBus.getDefault().post(
                                        MessageEvent(
                                            "${Constants.LINE_BREAK_TAB} KioskService.kt getOrderFromServer()_EXCEPTION-> ${
                                                e.printStackTrace()
                                            }"
                                        )
                                    )
                                }

                            }
                        }
                    },
                    object : com.android.volley.Response.ErrorListener {
                        override fun onErrorResponse(error: VolleyError) {
                            // TODO Auto-generated method stub
                            Log.d("ERROR", "error => $error")
                        }
                    }
                ) {
                    @Throws(AuthFailureError::class)
                    override fun getHeaders(): Map<String, String> {
                        val params: MutableMap<String, String> = HashMap()
                        params["host"] =
                            url.substring(url.indexOf("/") + 2, url.lastIndexOf("/api"))
                        params["accept-encoding"] = "application/json"
                        params["connection"] = "keep-alive"
                        params["token"] = authToken
                        return params
                    }
                }

                queue.add(getRequest)
            }
        }
    }

    private fun getKitchenPrinters(response: KioskOrderResponse) {
        var kioskRepository = KioskRepository(
            PrefProvider(applicationContext),
            AppDatabase.getDatabase(applicationContext)
        )
        response?.let {
            if (it.data?.paymentStatus.equals("paid", ignoreCase = true)) {
                CoroutineScope(Dispatchers.IO).launch {
                    var kitchenList = kioskRepository.getKitchenPrintersList()

                    async {
                        kitchenSettingModel = kioskRepository.getKitchenSettingsNormalData()
                    }.await()

                    printKitchenReceipt(kitchenList, it, 0)
                }
            }
        }
    }

    private fun printKitchenReceipt(
        kitchenList: List<PrinterResponse.Data.KitchenReceiptPrinters>,
        createOrderResponse: KioskOrderResponse,
        pos: Int
    ) {
        if (pos < kitchenList.size) {
            kitchenList.get(pos).let {
                if (it.receiptPrintType.equals(
                        "Kitchen",
                        ignoreCase = true
                    ) || it.receiptPrintType.equals(
                        "KitchenAndCustomer",
                        ignoreCase = true
                    ) && it.status
                ) {
                    Log.d("Hey", Gson().toJson(it))
                    initKitchenPrinter(it, createOrderResponse)
                }
            }

            printKitchenReceipt(kitchenList, createOrderResponse, pos.inc())
        }
    }

    private fun initKitchenPrinter(
        data: PrinterResponse.Data.KitchenReceiptPrinters,
        orderData: KioskOrderResponse
    ) {
        Log.d("initKitchenPrinter", "initKitchenPrinter")
        if (data.name.startsWith(Constants.SUNMI_PRINTER,true) && data.printer_type == WIFI){
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



//-----------

            lineFeed(4)
            setAlignment(1)

                if (!oneItemPerReceipt) {
                    orderData.data?.orderItems?.forEach { item ->
                        data.printerCategories.forEach { category ->
                            if (category.id == item.categoryId && category.printerEnable) {
                                for (singularity in 1..item.quantity!!) {

                                    if (printOrderIDInStickyPrinter) {
                                        setAlignment(1)
                                        lineFeed(2)

                                        appendText("OrderId:${orderData.data?.customOrderId}")
                                    }

                                    lineFeed(1)

                                    setCharacterSize(1,1)
                                    setAlignment(1)

                                    appendText(

                                                "${orderData.data?.orderType ?: ""}"

                                    )

                                    lineFeed(1)

                                    if (orderData.data?.orderType?.contains(
                                            "Phone",
                                            true
                                        ) == true
                                    ) {
                                        lineFeed(1)
                                        appendText("${orderData.data?.deliveryType}"

                                        )

                                        lineFeed(2)
                                    }

                                    setAlignment(0)
                                    lineFeed(1)
                                    appendText(addSingleReprintOrdersForStarKitchenKiosk(
                                                    1,
                                                    item,
                                                    data.printerCategories.toCollection(
                                                        arrayListOf()
                                                    )
                                                )

                                    )

                                    lineFeed(1)

                                    if (orderData.data?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                        appendText(

                                                    if (orderData.data?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                                        "--------------------------------------------\nOrder Note"
                                                    } else {""})
                                        lineFeed(1)


                                    }
                                    if (orderData.data?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                        appendText(
                                           if (orderData.data?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                                        orderData.data?.note.toString()
                                                    } else ""
                                                )
                                        lineFeed(1)

                                    }

                                    lineFeed(2)


                                    var printedName = StringBuilder("")
                                    orderData.data?.customer?.firstName?.let { firstName ->
                                        orderData.data?.customer?.lastName?.let { lastName ->
                                            if (kitchenSettingModel.showCustomerName) {
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
                                                    appendText(

                                                                "Customer Details"

                                                    )
                                                    lineFeed(1)

                                                    appendText(

                                                                "--------------------------------------------"
                                                            )
                                                    lineFeed(1)


                                                    appendText(

                                                                 printedName.toString()
                                                            )


                                                }
                                            }

                                        }
                                    }



                                    lineFeed(2)

                                    appendText(

                                                    orderData?.data?.createdAt.toString()
                                                )



                                    lineFeed(1)

                                }
                            }
                        }
                    }
                }

                else {
                    setAlignment(1)
                    lineFeed(2)

                    appendText("OrderId:${orderData.data?.customOrderId}")


                    lineFeed(1)

                    setCharacterSize(1,1)

                    appendText(if (kitchenSettingModel.showOrderType)
                                    orderData.data?.orderType ?: ""
                                else "")

                    lineFeed(1)

                    if (orderData.data?.orderType == Constants.PHONE_ORDER_) {
                        appendText(

                                    orderData.data?.deliveryType ?: ""

                        )

                        lineFeed(1)
                    }

                    appendText(

                                "Employee:${
                                    PrefProvider(applicationContext).getValue(
                                        Constants.EMPLOYEE_NAME,
                                        ""
                                    )
                                }"
                            )

                    lineFeed(1)

                    appendText(

                                Constants.getReceiptFormatDateFromUTCServer(
                                    applicationContext,
                                    orderData.data?.createdAt.toString()
                                )

                    )

                    lineFeed(1)
                    setCharacterSize(2,2)

                    appendText(

//                                "--------------------------------------------"
                                "------------------------"

                    )

                    lineFeed(1)

                    setCharacterSize(2,2)
                    setAlignment(0)
                    appendText(
                                addReprintOrdersForStarKitchenKiosk(
                                    orderData.data?.orderItems!!,
                                    data.printerCategories.toCollection(arrayListOf())
                                )

                    )

                    setCharacterSize(2,2)

                    lineFeed(1)
                    setAlignment(1)
                    if (orderData.data?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                        appendText(
                          if (orderData.data?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
//                                        "--------------------------------------------\nOrder Note "
                                        "------------------------\nOrder Note "
                                    } else ""
                                )
                        lineFeed(1)

                    }
                    if (orderData.data?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                        appendText(
                            if (orderData.data?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                        orderData.data?.note.toString()
                                    } else ""
                                )
                        lineFeed(1)

                    }
                    lineFeed(1)

                    setCharacterSize(1,1)

                    setAlignment(0)
                    if (kitchenSettingModel.showCustomerName && (orderData.data?.customer?.firstName != null || orderData.data?.customer?.lastName != null)) {
                        appendText(
                         if (kitchenSettingModel.showCustomerName && (orderData.data?.customer?.firstName != null || orderData.data?.customer?.lastName != null)) {
                                        "Customer Details\n"
                                    } else ""

                        )
                    }

                    if (kitchenSettingModel.showCustomerName && (orderData.data?.customer?.firstName != null || orderData.data?.customer?.lastName != null)) {
                        appendText(
                            if (kitchenSettingModel.showCustomerName && (orderData.data?.customer?.firstName != null || orderData.data?.customer?.lastName != null)) {
                                        "--------------------------------------------"
//                                        "-----------------------"
                                    } else ""
                                )
                        lineFeed(1)

                    }
                    if (kitchenSettingModel.showCustomerName && (orderData.data?.customer?.firstName != null || orderData.data?.customer?.lastName != null)) {
                        appendText(
                          if (kitchenSettingModel.showCustomerName && (orderData.data?.customer?.firstName != null || orderData.data?.customer?.lastName != null)) {
                                        orderData.data?.customer?.firstName + " " + orderData.data?.customer?.lastName
                                    } else ""

                        )
                        lineFeed(1)
                    }

                    try {
                        if (kitchenSettingModel.showCustomerPhone && orderData.data?.customer?.phones?.get(
                                0
                            ) != null
                        ) {
                            appendText(
                                if (kitchenSettingModel.showCustomerPhone && orderData.data?.customer?.phones?.get(
                                                0
                                            ) != null
                                        ) {

                                            var phoneNumber =
                                                orderData.data?.customer?.phones?.get(
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

                        }
                    } catch (e: Exception) {

                    }
                    setCharacterSize(2,2)

                    lineFeed(6)
                    cutPaper(true)




                    Log.e("checkKey","pushContent: checkSN:${data.ipAddress} ${pushContent(trade_no =
                    String.format("%s_%010d", "${data.ipAddress}", System.currentTimeMillis()),
                        "${data.ipAddress}", 1, 1, "您有新的订单", 0)}")


                }








        }
        else{

        if (data.name.startsWith(Constants.SUNMI_PRINTER, true)) {

            try {
                SunmiPrinterApi.getInstance()
                    .setPrinter(SunmiPrinter.SunmiBlueToothPrinter, data.ipAddress)
            } catch (e: Exception) {
                SunmiPrinterApi.getInstance()
                    .setPrinter(SunmiPrinter.SunmiNetPrinter, data.ipAddress)
            }

            if (!SunmiPrinterApi.getInstance().isConnected) {
                SunmiPrinterApi.getInstance()
                    .connectPrinter(applicationContext, object : ConnectCallback {

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
                                initKitchenPrinter(data, orderData)
                            }
                        }

                        override fun onDisconnect() {
                            println("onDisconnect")
                        }

                    })
            } else {
                Log.d("tracking printers", "In Else")
                generateKitchenReceiptSunmi(data, orderData)
            }

        } else if (data.name.startsWith(Constants.SUNMI_INNER_PRINTER, true)) {

            SunmiPrintHelper.getInstance().initSunmiPrinterService(applicationContext)
            CoroutineScope(Dispatchers.IO).launch {
                delay(100)
                setServiceForKitchen(data, orderData)
            }
        } else if (((data.name.contains("TSP", ignoreCase = true))) || ((data.name.contains(
                "SP",
                ignoreCase = true
            )))
        ) {
            settings = StarConnectionSettings(InterfaceType.Lan, data.macAddress)
            printer = StarPrinter(settings, applicationContext)

            CoroutineScope(Dispatchers.IO).launch {
                var tbLabelPrinterSettings: TbLabelPrinterSettings? =
                    AppDatabase.getDatabase(applicationContext).labelPrinterSettings()
                        .getLabelPrinterSettingsData()
                oneItemPerReceipt =
                    if (tbLabelPrinterSettings != null) tbLabelPrinterSettings.oneItemPerReciept else true
            }
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    delay(6000)

                    val builder = StarXpandCommandBuilder()

                    var printerBuilder = PrinterBuilder()

                    with(printerBuilder) {
                        styleInternationalCharacter(InternationalCharacterType.Usa)
                        styleCharacterSpace(0.0)
                        styleAlignment(Alignment.Center)

                        if (!oneItemPerReceipt) {
                            orderData.data?.orderItems?.forEach { item ->
                                data.printerCategories.forEach { category ->
                                    if (category.id == item.categoryId && category.printerEnable) {
                                        for (singularity in 1..item.quantity!!) {

                                            if (printOrderIDInStickyPrinter) {
                                                add(
                                                    PrinterBuilder()
                                                        .styleBold(true)
                                                        .styleMagnification(
                                                            MagnificationParameter(2, 2)
                                                        )
                                                        .actionPrintText(
                                                            "OrderId:${orderData.data?.customOrderId}"
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
                                                        "${orderData.data?.orderType ?: ""}"
                                                    )
                                            )

                                            actionFeedLine(1)

                                            if (orderData.data?.orderType?.contains(
                                                    "Phone",
                                                    true
                                                ) == true
                                            ) {
                                                add(
                                                    PrinterBuilder()
                                                        .styleBold(true)
                                                        .styleMagnification(
                                                            MagnificationParameter(2, 2)
                                                        )
                                                        .actionPrintText(
                                                            "${orderData.data?.deliveryType}"
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
                                                        content = addSingleReprintOrdersForStarKitchenKiosk(
                                                            1,
                                                            item,
                                                            data.printerCategories.toCollection(
                                                                arrayListOf()
                                                            )
                                                        )
                                                    )
                                            )

                                            actionFeedLine(1)

                                            if (orderData.data?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                                add(
                                                    PrinterBuilder()
                                                        .styleAlignment(Alignment.Center)
                                                        .styleBold(true)
                                                        .styleMagnification(
                                                            MagnificationParameter(
                                                                2,
                                                                2
                                                            )
                                                        )

                                                        .actionPrintText(
                                                            content = if (orderData.data?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                                                "--------------------------------------------\nOrder Note"
                                                            } else ""
                                                        )
                                                )
                                            }
                                            if (orderData.data?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                                add(
                                                    PrinterBuilder()
                                                        .styleMagnification(
                                                            MagnificationParameter(
                                                                2,
                                                                2
                                                            )
                                                        )

                                                        .styleAlignment(Alignment.Center)
                                                        .actionPrintText(
                                                            content = if (orderData.data?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                                                orderData.data?.note.toString()
                                                            } else ""
                                                        )
                                                )
                                            }

                                            actionFeedLine(1)
                                            actionFeedLine(1)

                                            var printedName = StringBuilder("")
                                            orderData.data?.customer?.firstName?.let { firstName ->
                                                orderData.data?.customer?.lastName?.let { lastName ->
                                                    if (kitchenSettingModel.showCustomerName) {
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
                                                                    .styleMagnification(
                                                                        MagnificationParameter(2, 2)
                                                                    )

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
                                                                    .styleMagnification(
                                                                        MagnificationParameter(2, 2)
                                                                    )

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
                                                            applicationContext,
                                                            orderData?.data?.createdAt.toString()
                                                        )
                                                    )
                                            )

                                            actionFeedLine(1)
                                            actionCut(CutType.Partial)
                                        }
                                    }
                                }
                            }
                        } else {

                            add(
                                PrinterBuilder()
                                    .styleBold(true)
                                    .styleMagnification(MagnificationParameter(3, 3))
                                    .actionPrintText(
                                        "OrderId:${orderData.data?.customOrderId}"
                                    )
                            )

                            styleAlignment(Alignment.Center)

                            add(
                                PrinterBuilder()
                                    .styleBold(true)
                                    .styleMagnification(MagnificationParameter(1, 1))
                                    .actionPrintText(
                                        if (kitchenSettingModel.showOrderType)
                                            orderData.data?.orderType ?: ""
                                        else ""
                                    )
                            )

                            actionFeedLine(1)

                            if (orderData.data?.orderType == Constants.PHONE_ORDER_) {
                                add(
                                    PrinterBuilder()
                                        .styleBold(true)
                                        .styleMagnification(MagnificationParameter(2, 2))
                                        .actionPrintText(
                                            orderData.data?.deliveryType ?: ""
                                        )
                                )

                                actionFeedLine(1)
                            }

                            add(
                                PrinterBuilder()
                                    .actionPrintText(
                                        "Employee:${
                                            PrefProvider(applicationContext).getValue(
                                                Constants.EMPLOYEE_NAME,
                                                ""
                                            )
                                        }"
                                    )
                            )
                            actionFeedLine(1)

                            add(
                                PrinterBuilder()
                                    .actionPrintText(
                                        Constants.getReceiptFormatDateFromUTCServer(
                                            applicationContext,
                                            orderData.data?.createdAt.toString()
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
                                PrinterBuilder()
                                    .styleMagnification(MagnificationParameter(2, 2))
                                    .styleAlignment(Alignment.Left)
                                    .actionPrintText(
                                        content = addReprintOrdersForStarKitchenKiosk(
                                            orderData.data?.orderItems!!,
                                            data.printerCategories.toCollection(arrayListOf())
                                        )
                                    )
                            )

                            actionFeedLine(1)
                            if (orderData.data?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                add(
                                    PrinterBuilder()
                                        .styleMagnification(MagnificationParameter(2, 2))
                                        .styleAlignment(Alignment.Center)
                                        .styleBold(true)
                                        .actionPrintText(
                                            content = if (orderData.data?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                                "--------------------------------------------\nOrder Note "
                                            } else ""
                                        )
                                )
                            }
                            if (orderData.data?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                add(
                                    PrinterBuilder()
                                        .styleMagnification(MagnificationParameter(2, 2))
                                        .styleAlignment(Alignment.Center)
                                        .actionPrintText(
                                            content = if (orderData.data?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote == true) {
                                                orderData.data?.note.toString()
                                            } else ""
                                        )
                                )
                            }
                            actionFeedLine(1)
                            if (kitchenSettingModel.showCustomerName && (orderData.data?.customer?.firstName != null || orderData.data?.customer?.lastName != null)) {
                                add(
                                    PrinterBuilder()
                                        .styleMagnification(MagnificationParameter(2, 2))
                                        .styleAlignment(Alignment.Left)
                                        .styleBold(true)
                                        .actionPrintText(
                                            content = if (kitchenSettingModel.showCustomerName && (orderData.data?.customer?.firstName != null || orderData.data?.customer?.lastName != null)) {
                                                "Customer Details\n"
                                            } else ""
                                        )
                                )
                            }

                            if (kitchenSettingModel.showCustomerName && (orderData.data?.customer?.firstName != null || orderData.data?.customer?.lastName != null)) {
                                add(
                                    PrinterBuilder()
                                        .styleAlignment(Alignment.Center)
                                        .actionPrintText(
                                            content = if (kitchenSettingModel.showCustomerName && (orderData.data?.customer?.firstName != null || orderData.data?.customer?.lastName != null)) {
                                                "--------------------------------------------"
                                            } else ""
                                        )
                                )
                            }
                            if (kitchenSettingModel.showCustomerName && (orderData.data?.customer?.firstName != null || orderData.data?.customer?.lastName != null)) {
                                add(
                                    PrinterBuilder()
                                        .styleMagnification(MagnificationParameter(2, 2))
                                        .styleAlignment(Alignment.Left)
                                        .actionPrintText(
                                            content = if (kitchenSettingModel.showCustomerName && (orderData.data?.customer?.firstName != null || orderData.data?.customer?.lastName != null)) {
                                                orderData.data?.customer?.firstName + " " + orderData.data?.customer?.lastName
                                            } else ""
                                        )
                                )
                            }

                            try {
                                if (kitchenSettingModel.showCustomerPhone && orderData.data?.customer?.phones?.get(
                                        0
                                    ) != null
                                ) {
                                    add(
                                        PrinterBuilder()
                                            .styleMagnification(MagnificationParameter(2, 2))
                                            .styleAlignment(Alignment.Left)
                                            .actionPrintText(
                                                content = if (kitchenSettingModel.showCustomerPhone && orderData.data?.customer?.phones?.get(
                                                        0
                                                    ) != null
                                                ) {

                                                    var phoneNumber =
                                                        orderData.data?.customer?.phones?.get(
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

                        try {
                            if (kitchenSettingModel.showCustomerPhone && orderData.data?.customer?.phones?.get(
                                    0
                                ) != null
                            ) {

                            }
                        } catch (e: Exception) {
                        }


                    }


                    var document = DocumentBuilder()
                        .addPrinter(printerBuilder)
                    builder.addDocument(
                        document
                    )

                    val commands = builder.getCommands()
//                    val jobSettings = StarSpoolJobSettings(true, 30, "Print from Android")

                    printer.openAsync().await()
                    printer.printAsync(commands).await()





                    Log.d("Printing", "Success")
                } catch (e: Exception) {
                    Log.d("Printing", "Error: ${e}")
                } finally {

                    printer.closeAsync().await()

                }
            }

        } else if (data.name.contains(LANDI_INNER_PRINTER, true)) {
            generateKitchenReceiptLandiInner(data, orderData)
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
                        Printer.MODEL_ANK, applicationContext
                    )
                } else {
                    Printer(
                        Printer.TM_U220,
                        Printer.MODEL_ANK, applicationContext
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

                    generateKitchenReceiptU220(data, orderData, mPrinter)

                    // generateReceiptForU220(mPrinter, data, type)

                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            } else {
                PrinterClass.closePrinter()
                if (PrinterClass.getPrinter() == null) {
                    //  printerDialog.show(requireContext())

                    var printer: Print? = Print(applicationContext)
                    /* if (printer != null) {
                         printer.setStatusChangeEventCallback(this)
                         printer.setBatteryStatusChangeEventCallback(this)
                     }
*/

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
                        printer?.setStatusChangeEventCallback(this)

                    } catch (e: Exception) {
                        //  printerDialog.dismiss()
                        LogUtil.logE(TAG, "PrinterException: " + e.message)
                        printer = null
                        return
                    }

                    if (printer != null) {
                        PrinterClass.setPrinter(printer)


                        generateKitchenReceipt(data, orderData)

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
                applicationContext,
                Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                Activity(),
                arrayOf<String>(Manifest.permission.BLUETOOTH),
                Constants.PERMISSION_BLUETOOTH
            )
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.BLUETOOTH_ADMIN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                Activity(),
                arrayOf<String>(Manifest.permission.BLUETOOTH_ADMIN),
                Constants.PERMISSION_BLUETOOTH_ADMIN
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                Activity(),
                arrayOf<String>(Manifest.permission.BLUETOOTH_CONNECT),
                Constants.PERMISSION_BLUETOOTH_CONNECT
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                Activity(),
                arrayOf<String>(Manifest.permission.BLUETOOTH_SCAN),
                Constants.PERMISSION_BLUETOOTH_SCAN
            )
        } else {
            onBluetoothPermissionGranted.onPermissionsGranted()
        }
    }

    private fun generateKitchenReceiptLandiInner(
        kitchenReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        orderData: KioskOrderResponse
    ) {
//        val printer = com.dantsu.escposprinter.EscPosPrinter(BluetoothPrintersConnections.selectFirstPaired(), 203, 48f, 32)

        val receiptModel = orderData.data

        this.checkBluetoothPermissions(object : OnBluetoothPermissionGranted {
            override fun onPermissionsGranted() {
                GlobalScope.launch {
                    LPrint.connectLandiInnerPrinter(kitchenReceiptPrinters.macAddress)
                        ?.let { outputStream ->

                            LPrint.apply {
                                setOutputStream(outputStream)
                                try {
                                    // PrintSunmiUtils.fontSizeInner(LARGE)
//                                    SunmiPrintHelper.getInstance().initPrinter()
                                    /*Added By Rahul */
                                    /*try {
                                        if (isOrderUpdated == true || cartList!!.isEdited == true) {
                                            printCenter(
                                                "***** UPDATED *****",
                                                isBold = true,
                                                fontSize = FONT_SIZE_5X
                                            )
//                                            PrintSunmiUtils.headerText("***** UPDATED *****")
                                        }
                                    } catch (e: java.lang.NullPointerException) {

                                    }

                                    lineBreak()
                                    lineBreak()*/

                                    try {

                                        if (prefProvider.getValueboolean(
                                                ORDER_NUMBER_STARTING_FROM_ONE,
                                                false
                                            )
                                        ) {
                                            printCenter(
                                                "OrderID:" + receiptModel?.customOrderId,
                                                isBold = true,
                                                fontSize = FONT_SIZE_5X
                                            )
                                        } else {
                                            printCenter(
                                                "OrderID:" + receiptModel?.id, isBold = true,
                                                fontSize = FONT_SIZE_5X
                                            )
                                        }
                                        lineBreak()
                                    } catch (e: Exception) {
                                    }

                                    lineBreak()

                                    if (kitchenSettingModel.showOrderType) {
                                        printCenter(
                                            receiptModel?.orderTypeName.toString(), isBold = true,
                                            fontSize = FONT_SIZE_5X
                                        )
                                    }
                                    lineBreak()

                                    if (receiptModel?.orderType.equals(
                                            "Online Order",
                                            true
                                        ) ||
                                        receiptModel?.orderType.equals(
                                            "OnlineWebOrder",
                                            true
                                        ) ||
                                        receiptModel?.orderType.equals(PHONE_ORDER, true)
                                    ) {
                                        printCenter(
                                            receiptModel?.deliveryType.toString(), isBold = true,
                                            fontSize = FONT_SIZE_5X
                                        )
                                        lineBreak()
                                    }
                                    lineBreak()

                                    /*if (kitchenSettingModel.showTeamMember) {
                                        printLeft("Employee:" + receiptModel?.employee?.name,isBold = true,
                                            fontSize = FONT_SIZE_5X
                                        )
                                    }
                                    lineBreak()*/

                                    printLeft(
                                        getReceiptFormatDateFromUTCServer(
                                            applicationContext,
                                            receiptModel?.createdAt.toString()
                                        ),
                                        isBold = true,
                                        fontSize = FONT_SIZE_5X
                                    )
                                    lineBreak()

                                    printDashedLineAndBreak()
                                    lineBreak()

//                                    if (sunmiFrameworkVersion?.get(0)
//                                            ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)
//                                            ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)
//                                            ?.toInt() != 39
//                                    ) {
//                                        PrintSunmiUtils.addHorizontalInnerNew()
//                                    } else {
//                                        PrintSunmiUtils.addHorizontalInner()
//                                    }
//                                    SunmiPrintHelper.getInstance().lineWrap(1)

//                                    if (sunmiFrameworkVersion?.get(0)
//                                            ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)
//                                            ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)
//                                            ?.toInt() != 39
//                                    ) {
//                                        PrintSunmiUtils.normalText("\n")
//                                    }


                                    receiptModel?.orderItems?.let {

                                        addOrdersForKitchenOnlineOrderLandiInnerKiosk(
                                            it,
                                            kitchenReceiptPrinters.printerCategories.toCollection(
                                                arrayListOf()
                                            ),
                                            lprint = LPrint
                                        )
                                    }


                                    if (receiptModel?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {

                                        lineBreak()
                                        printCenter(
                                            "Order Note", isBold = true,
                                            fontSize = FONT_SIZE_5X
                                        )
                                        lineBreak()
                                        printCenter(
                                            receiptModel?.note.toString(), isBold = false,
                                            fontSize = FONT_SIZE_5X
                                        )
                                        lineBreak()
                                    }



                                    if (kitchenSettingModel.showCustomerAddress != false || kitchenSettingModel.showCustomerPhone != false || kitchenSettingModel.showCustomerName != false) {
                                        if (receiptModel?.customer != null) {

                                            lineBreak()
                                            printLeft(
                                                "Customer Details", isBold = true,
                                                fontSize = FONT_SIZE_5X
                                            )
                                            lineBreak()
                                            printDashedLineAndBreak()

                                            /*PrintSunmiUtils.customerDetailsInner(
                                                true,
                                                sunmiFrameworkVersion
                                            )
                                            if (sunmiFrameworkVersion?.get(0)
                                                    ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)
                                                    ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)
                                                    ?.toInt() != 39
                                            ) {
                                                PrintSunmiUtils.addHorizontalInnerNew()
                                            } else {
                                                PrintSunmiUtils.addHorizontalInner()
                                            }*/
                                            try {
                                                if (kitchenSettingModel.showCustomerName) {
                                                    printLeft(
                                                        receiptModel?.customer?.firstName + " " + receiptModel?.customer?.lastName,
                                                        isBold = false,
                                                        fontSize = FONT_SIZE_5X
                                                    )
                                                }
                                            } catch (e: Exception) {
                                            }

                                            lineBreak()

                                            try {
                                                if (kitchenSettingModel.showCustomerPhone) {

                                                    if (receiptModel?.customer?.phones?.isNotEmpty() == true) {

                                                        receiptModel?.customer?.phones?.get(0)?.phoneNumber?.let {
                                                            printLeft(
                                                                MethodUtils.formatPhoneNumber(it),
                                                                isBold = false,
                                                                fontSize = FONT_SIZE_5X
                                                            )
                                                        }
                                                    }

                                                }
                                            } catch (e: Exception) {
                                            }

                                            if (kitchenSettingModel.showCustomerAddress) {
                                                if (receiptModel?.orderType?.trim()
                                                        .toString()
                                                        .lowercase() == "Open Order".trim()
                                                        .toString().lowercase()
                                                    && receiptModel?.deliveryType?.trim()
                                                        .toString()
                                                        .lowercase() == "Pickup".trim().lowercase()
                                                ) {

                                                } else {
                                                    try {
                                                        if (receiptModel?.customer?.addresses?.isNotEmpty() == true) {


//                                receiptModel?.customer?.addresses?.get(0)?.fullAddress?.let {
//                                    PrintSunmiUtils.normalTextLarge(
//                                        it
//                                    )
//                                }

                                                            receiptModel?.customer?.addresses?.filter { it.typeOfAddress == BILLING_ADDRESS }
                                                                ?.forEach {

                                                                    if (it.typeOfAddress.equals(
                                                                            BILLING_ADDRESS,
                                                                            ignoreCase = true
                                                                        )
                                                                    ) {
                                                                        it.fullAddress?.let { it1 ->
                                                                            printLeft(
                                                                                it1
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                        }
                                                    } catch (e: Exception) {
                                                    }
                                                }
                                            }

                                        }
                                    }

                                    lineBreak()
                                    lineBreak()
                                    paperCut()
                                    disconnectLandiPrinter()

//                                    SunmiPrintHelper.getInstance().lineWrap(1)
//                                    PrintSunmiUtils.cutPaperInner()

                                    //  SunmiPrinterApi.getInstance().disconnectPrinter(requireContext())

                                } catch (e: Exception) {
                                    // printerDialog.dismiss()
                                    e.printStackTrace()
                                }
                            }
                        }
                }
            }
        })
    }


    private fun generateKitchenReceiptU220(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        orderData: KioskOrderResponse,
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
                if (PrefProvider(applicationContext).getValueboolean(
                        Constants.ORDER_NUMBER_STARTING_FROM_ONE,
                        false
                    )
                ) {
                    builder.addText("OrderID:" + orderData.data?.customOrderId)
                } else {
                    builder.addText("OrderID:" + orderData.data?.id)

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

                    addBuilderTextForU220(builder, orderData.data?.orderTypeName.toString())

                    if ((orderData.data?.orderType.equals(Constants.PHONE_ORDER, true) ||
                                orderData.data?.orderType.equals("OnlineWebOrder", true) ||
                                orderData.data?.orderType.equals("Online Order", true) ||
                                orderData.data?.orderType.equals(
                                    "OnlineOrder",
                                    true
                                )) && orderData.data != null
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

                        addBuilderTextForU220(builder, orderData.data?.deliveryType.toString())
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
                            "Employee:" + PrefProvider(applicationContext).getValue(
                                Constants.EMPLOYEE_NAME,
                                ""
                            ), "",
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
                            applicationContext,
                            orderData.data?.createdAt ?: ""
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


                orderData.data?.orderItems?.let {
                    addOrdersForKitchenOnlineOrderU220Kiosk(
                        builder,
                        it,
                        fontSizeH,
                        fontSizeW,
                        customerReceiptPrinters.printerCategories.toCollection(arrayListOf())
                    )
                }

                if (orderData.data?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
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


                    builder.addText(orderData.data?.note.toString())
                }


                if (kitchenSettingModel.showCustomerAddress != false or kitchenSettingModel.showCustomerPhone != false or kitchenSettingModel.showCustomerName != false) {
                    if (orderData.data?.customer != null) {

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
                            builder.addText(orderData.data?.customer?.firstName + " " + orderData.data?.customer?.lastName)

                        }


                        if (kitchenSettingModel.showCustomerPhone) {

                            if (orderData.data?.customer?.phones?.isNotEmpty() == true) {
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
                                        orderData.data?.customer?.phones?.get(
                                            orderData.data?.customer?.phones?.size?.minus(1) ?: 0
                                        )?.phoneNumber ?: ""
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


                        if (orderData.data?.customer?.addresses?.isNotEmpty() == true) {

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

                if (PrefProvider(applicationContext).getValueboolean(
                        Constants.ORDER_NUMBER_STARTING_FROM_ONE,
                        false
                    )
                ) {
                    builder.addText("OrderID:" + orderData.data?.customOrderId)
                } else {
                    builder.addText("OrderID:" + orderData.data?.id)
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

                    addBuilderTextForU220(builder, orderData.data?.orderTypeName.toString())

                    if ((orderData.data?.orderType.equals(Constants.PHONE_ORDER, true) ||
                                orderData.data?.orderType.equals("OnlineWebOrder", true) ||
                                orderData.data?.orderType.equals("Online Order", true) ||
                                orderData.data?.orderType.equals(
                                    "OnlineOrder",
                                    true
                                )) && orderData.data?.deliveryType != null
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

                        addBuilderTextForU220(builder, orderData.data?.deliveryType.toString())
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
                            "Employee:" + PrefProvider(applicationContext).getValue(
                                Constants.EMPLOYEE_NAME,
                                ""
                            ), "",
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
                            applicationContext,
                            orderData.data?.createdAt ?: ""
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

                orderData.data?.orderItems?.let {
                    addOrdersForKitchenOnlineOrderU220Kiosk(
                        builder,
                        it,
                        fontSizeH,
                        fontSizeW,
                        customerReceiptPrinters.printerCategories.toCollection(arrayListOf())
                    )
                }

                if (orderData.data?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
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


                    builder.addText(orderData.data?.note?.toString())
                }


                if (kitchenSettingModel.showCustomerAddress != false or kitchenSettingModel.showCustomerPhone != false or kitchenSettingModel.showCustomerName != false) {
                    if (orderData.data?.customer != null) {

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
                            builder.addText(orderData.data?.customer?.firstName + " " + orderData.data?.customer?.lastName)

                        }


                        if (kitchenSettingModel.showCustomerPhone) {

                            if (orderData.data?.customer?.phones?.isNotEmpty() == true) {
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
                                        orderData.data?.customer?.phones?.get(
                                            orderData.data?.customer?.phones?.size?.minus(1) ?: 0
                                        )?.phoneNumber ?: ""
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


                        if (orderData.data?.customer?.addresses?.isNotEmpty() == true) {

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


    private fun setServiceForKitchen(
        data: PrinterResponse.Data.KitchenReceiptPrinters,
        orderData: KioskOrderResponse
    ) {
        if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.FoundSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper", "FoundSunmiPrinter")

            if (!BluetoothUtil.isBlueToothPrinter) {

                generateKitchenReceiptSunmiInner(data, orderData)

            }

        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.CheckSunmiPrinter) {
            Handler(Looper.getMainLooper()).postDelayed({
                setServiceForKitchen(data, orderData)
            }, 2000)
            LogUtil.logE("SunmiPrintHelper", "CheckSunmiPrinter")
        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.LostSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper", "LostSunmiPrinter")
        } else {
            LogUtil.logE("SunmiPrintHelper", "ELSE")
        }
    }

    private fun generateKitchenReceipt(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        orderData: KioskOrderResponse
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

            builder = Builder(pname, PrinterClass.language, applicationContext)
            builder.addTextLineSpace(30)
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
                if (PrefProvider(applicationContext).getValueboolean(
                        Constants.ORDER_NUMBER_STARTING_FROM_ONE,
                        false
                    )
                ) {
                    builder.addText("OrderID:" + orderData.data?.customOrderId)
                } else {
                    builder.addText("OrderID:" + orderData.data?.id)

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

                    addBuilderText(builder, orderData.data?.orderTypeName.toString())

                    if ((orderData.data?.orderType.equals(Constants.PHONE_ORDER, true) ||
                                orderData.data?.orderType.equals("OnlineWebOrder", true) ||
                                orderData.data?.orderType.equals("Online Order", true) ||
                                orderData.data?.orderType.equals(
                                    "OnlineOrder",
                                    true
                                )) && orderData.data?.deliveryType != null
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

                        addBuilderText(builder, orderData.data?.deliveryType.toString())
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
                            "Employee:" + PrefProvider(applicationContext).getValue(
                                Constants.EMPLOYEE_NAME,
                                ""
                            ), "",
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
                            applicationContext,
                            orderData.data?.createdAt ?: ""
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


                orderData.data?.orderItems?.let {
                    addOrdersForKitchenOnlineOrderKiosk(
                        builder,
                        it,
                        fontSizeH,
                        fontSizeW,
                        customerReceiptPrinters.printerCategories.toCollection(arrayListOf())
                    )
                }

                if (orderData.data?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
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


                    builder.addText(orderData.data?.note.toString())
                }


                if (kitchenSettingModel.showCustomerAddress != false or kitchenSettingModel.showCustomerPhone != false or kitchenSettingModel.showCustomerName != false) {
                    if (orderData.data?.customer != null) {

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
                            builder.addText(orderData.data?.customer?.firstName + " " + orderData.data?.customer?.lastName)

                        }


                        if (kitchenSettingModel.showCustomerPhone) {

                            if (orderData.data?.customer?.phones?.isNotEmpty() == true) {
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
                                        orderData.data?.customer?.phones?.get(
                                            orderData.data?.customer?.phones?.size?.minus(1) ?: 0
                                        )?.phoneNumber ?: ""
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


                        if (orderData.data?.customer?.addresses?.isNotEmpty() == true) {

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

                if (PrefProvider(applicationContext).getValueboolean(
                        Constants.ORDER_NUMBER_STARTING_FROM_ONE,
                        false
                    )
                ) {
                    builder.addText("OrderID:" + orderData.data?.customOrderId)
                } else {
                    builder.addText("OrderID:" + orderData.data?.id)
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

                    addBuilderText(builder, orderData.data?.orderTypeName.toString())

                    if ((orderData.data?.orderType.equals(Constants.PHONE_ORDER, true) ||
                                orderData.data?.orderType.equals("OnlineWebOrder", true) ||
                                orderData.data?.orderType.equals("Online Order", true) ||
                                orderData.data?.orderType.equals(
                                    "OnlineOrder",
                                    true
                                )) && orderData.data?.deliveryType != null
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

                        addBuilderText(builder, orderData.data?.deliveryType.toString())
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
                            "Employee:" + PrefProvider(applicationContext).getValue(
                                Constants.EMPLOYEE_NAME,
                                ""
                            ), "",
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
                            applicationContext,
                            orderData.data?.createdAt ?: ""
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


                orderData.data?.orderItems?.let {
                    addOrdersForKitchenOnlineOrderKiosk(
                        builder,
                        it,
                        fontSizeH,
                        fontSizeW,
                        customerReceiptPrinters.printerCategories.toCollection(arrayListOf())
                    )
                }

                if (orderData.data?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
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

                    builder.addText(orderData.data?.note.toString())
                }


                if (kitchenSettingModel.showCustomerAddress != false or kitchenSettingModel.showCustomerPhone != false or kitchenSettingModel.showCustomerName != false) {
                    if (orderData.data?.customer != null) {

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
                            builder.addText(orderData.data?.customer?.firstName + " " + orderData.data?.customer?.lastName)

                        }


                        if (kitchenSettingModel.showCustomerPhone) {

                            if (orderData.data?.customer?.phones?.isNotEmpty() == true) {
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
                                        orderData.data?.customer?.phones?.get(
                                            orderData.data?.customer?.phones?.size?.minus(1) ?: 0
                                        )?.phoneNumber ?: ""
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


                        if (orderData.data?.customer?.addresses?.isNotEmpty() == true) {

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
        orderData: KioskOrderResponse
    ) {

        try {

            PrintSunmiUtils.fontSize(kitchenSettingModel.fonts)
            SunmiPrinterApi.getInstance().lineWrap(2)
            if (PrefProvider(applicationContext).getValueboolean(
                    Constants.ORDER_NUMBER_STARTING_FROM_ONE,
                    false
                )
            ) {
                PrintSunmiUtils.orderIdSunmi(
                    "OrderID:" + orderData.data?.customOrderId
                )
            } else {
                PrintSunmiUtils.orderIdSunmi(
                    "OrderID:" + orderData.data?.id
                )
            }

            SunmiPrinterApi.getInstance().lineWrap(1)

            if (kitchenSettingModel.showOrderType) {
                PrintSunmiUtils.printOrderType(orderData.data?.orderTypeName.toString())
                SunmiPrinterApi.getInstance().lineWrap(1)

                if ((orderData.data?.orderType.equals(Constants.PHONE_ORDER, true) ||
                            orderData.data?.orderType.equals("OnlineWebOrder", true) ||
                            orderData.data?.orderType.equals("Online Order", true) ||
                            orderData.data?.orderType.equals(
                                "OnlineOrder",
                                true
                            )) && orderData.data?.deliveryType != null
                ) {
                    PrintSunmiUtils.printOrderType(orderData.data?.deliveryType.toString())
                    SunmiPrinterApi.getInstance().lineWrap(1)
                }
            }

            if (kitchenSettingModel.showTeamMember) {

                PrintSunmiUtils.employee(
                    padLine(
                        "Employee:" + PrefProvider(applicationContext).getValue(
                            Constants.EMPLOYEE_NAME,
                            ""
                        ), "",
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
                        applicationContext,
                        orderData.data?.createdAt ?: ""
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


            orderData.data?.orderItems?.let {
                addOrdersForKitchenOnlineOrderSunmiKiosk(
                    it,
                    customerReceiptPrinters.printerCategories.toCollection(arrayListOf())
                )
            }

            if (orderData.data?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
                SunmiPrinterApi.getInstance().lineWrap(1)
                PrintSunmiUtils.orderNote(orderData?.data?.note.toString())
            }


            if (kitchenSettingModel.showCustomerAddress != false or kitchenSettingModel.showCustomerPhone != false or kitchenSettingModel.showCustomerName != false) {
                if (orderData.data?.customer != null) {
                    SunmiPrinterApi.getInstance().lineWrap(1)
                    PrintSunmiUtils.customerDetails()

                    if (kitchenSettingModel.showCustomerName) {
                        PrintSunmiUtils.customerName(orderData.data?.customer?.firstName + " " + orderData.data?.customer?.lastName)

                    }


                    try {
                        if (kitchenSettingModel.showCustomerPhone) {

                            if (orderData.data?.customer?.phones?.isNotEmpty() == true) {

                                PrintSunmiUtils.customerPhone(
                                    MethodUtils.getUSFormatNumber(
                                        orderData.data?.customer?.phones?.get(
                                            orderData.data?.customer?.phones?.size?.minus(1) ?: 0
                                        )?.phoneNumber ?: ""
                                    )

                                )
                            }

                        }
                    } catch (e: Exception) {

                    }

                    try {
                        if (orderData.data?.customer?.addresses?.isNotEmpty() == true) {


                            PrintSunmiUtils.customerAddress(
                                orderData.data?.customer?.addresses?.get(
                                    orderData.data?.customer?.addresses?.size?.minus(1) ?: 0
                                )?.fullAddress ?: ""
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

    private fun generateKitchenReceiptSunmiInner(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        orderData: KioskOrderResponse
    ) {
        sunmiFrameworkVersion =
            PrefProvider(this).getValue(Constants.SUNMI_FRAMEWORK_VERSION, "").toString().split(".")
                .toTypedArray()

        Log.d(
            "KITCHEN_RECEIPT::",
            "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
        )

        try {

            SunmiPrintHelper.getInstance().initPrinter()
            SunmiPrintHelper.getInstance().lineWrap(2)
//            PrintSunmiUtils.headerText("OrderID:" + orderData.data?.id)

            if (PrefProvider(applicationContext).getValueboolean(
                    Constants.ORDER_NUMBER_STARTING_FROM_ONE,
                    false
                )
            ) {
                PrintSunmiUtils.headerText("OrderID:" + orderData.data?.customOrderId)
            } else {
                PrintSunmiUtils.headerText("OrderID:" + orderData.data?.id)
            }
            SunmiPrintHelper.getInstance().lineWrap(1)

            Log.d(
                "KITCHEN_RECEIPT::",
                "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
            )

            if (kitchenSettingModel.showOrderType) {
                PrintSunmiUtils.headerText(orderData.data?.orderTypeName.toString())
                SunmiPrintHelper.getInstance().lineWrap(1)

                if ((orderData.data?.orderType.equals(Constants.PHONE_ORDER, true) ||
                            orderData.data?.orderType.equals("OnlineWebOrder", true) ||
                            orderData.data?.orderType.equals("Online Order", true) ||
                            orderData.data?.orderType.equals(
                                "OnlineOrder",
                                true
                            )) && orderData.data?.deliveryType != null
                ) {
                    PrintSunmiUtils.headerText(orderData.data?.deliveryType.toString())
                    SunmiPrintHelper.getInstance().lineWrap(1)
                }
            }

            if (kitchenSettingModel.showTeamMember) {

                PrintSunmiUtils.normalTextLarge(
                    "Employee:" + PrefProvider(applicationContext).getValue(
                        Constants.EMPLOYEE_NAME,
                        ""
                    )
                )


            }
            Log.d(
                "KITCHEN_RECEIPT::",
                "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
            )

            try {
                try {
                    PrintSunmiUtils.normalTextLarge(
                        Constants.getReceiptFormatDateFromUTCServer(
                            applicationContext,
                            orderData.data?.createdAt ?: ""
                        )
                    )
                } catch (e: Exception) {
                    Log.d(
                        "KITCHEN_RECEIPT::",
                        "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber} -> ${
                            Gson().toJson(e)
                        }"
                    )
                }


                try {
                    Log.d(
                        "KITCHEN_RECEIPT::",
                        "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
                    )
//            PrintSunmiUtils.addHorizontalInner()
                    if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(
                            1
                        )
                            ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
                    ) {
                        Log.d(
                            "KITCHEN_RECEIPT::",
                            "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
                        )

                        try {
                            PrintSunmiUtils.addHorizontalInnerNew()
                        } catch (e: Exception) {
                            Log.d(
                                "KITCHEN_RECEIPT::",
                                "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber} -> ${
                                    Gson().toJson(e)
                                }"
                            )
                        }
                    } else {
                        Log.d(
                            "KITCHEN_RECEIPT::",
                            "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
                        )

                        try {
                            PrintSunmiUtils.addHorizontalInnerSmall()
                        } catch (e: Exception) {
                            Log.d(
                                "KITCHEN_RECEIPT::",
                                "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber} -> ${
                                    Gson().toJson(e)
                                }"
                            )

                        }
                    }
                } catch (e: Exception) {
                    Log.d(
                        "KITCHEN_RECEIPT::",
                        "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber} -> ${
                            Gson().toJson(e)
                        }"
                    )
                }

                try {
                    if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(
                            1
                        )?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
                    ) {
                        Log.d(
                            "KITCHEN_RECEIPT::",
                            "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
                        )

                        PrintSunmiUtils.normalTextNew("\n")
                    }
                } catch (e: Exception) {
                    Log.d(
                        "KITCHEN_RECEIPT::",
                        "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber} -> ${
                            Gson().toJson(e)
                        }"
                    )
                }
                Log.d(
                    "KITCHEN_RECEIPT::",
                    "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
                )

                try {
                    if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(
                            1
                        )?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
                    ) {
                        Log.d(
                            "KITCHEN_RECEIPT::",
                            "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
                        )

                        try {
                            orderData.data?.orderItems?.let {
                                addOrdersForKitchenOnlineOrderSunmiInnerKioskNew(
                                    it,
                                    customerReceiptPrinters.printerCategories.toCollection(
                                        arrayListOf()
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            Log.d(
                                "KITCHEN_RECEIPT::",
                                "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}-> ${
                                    Gson().toJson(e)
                                }"
                            )
                        }
                    } else {
                        try {
                            orderData.data?.orderItems?.let {
                                addOrdersForKitchenOnlineOrderSunmiInnerKiosk(
                                    it,
                                    customerReceiptPrinters.printerCategories.toCollection(
                                        arrayListOf()
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            Log.d(
                                "KITCHEN_RECEIPT::",
                                "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}-> ${
                                    Gson().toJson(e)
                                }"
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.d(
                        "KITCHEN_RECEIPT::",
                        "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber} -> ${
                            Gson().toJson(e)
                        }"
                    )
                }
            } catch (e: Exception) {
                Log.d(
                    "KITCHEN_RECEIPT::",
                    "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber} -> ${
                        Gson().toJson(e)
                    }"
                )
            }
            Log.d(
                "KITCHEN_RECEIPT::",
                "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
            )

            if (orderData.data?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {

                PrintSunmiUtils.orderNoteInnerLarge(orderData.data?.note.toString())
            }
            Log.d(
                "KITCHEN_RECEIPT::",
                "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
            )


            if (kitchenSettingModel.showCustomerAddress != false or kitchenSettingModel.showCustomerPhone != false or kitchenSettingModel.showCustomerName != false) {
                if (orderData.data?.customer != null) {

                    PrintSunmiUtils.customerDetailsInner(true, sunmiFrameworkVersion)

                    if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(
                            1
                        )?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
                    ) {
                        PrintSunmiUtils.normalTextNew("\n")
                    }

                    if (kitchenSettingModel.showCustomerName) {
                        PrintSunmiUtils.normalTextLarge(orderData.data?.customer?.firstName + " " + orderData.data?.customer?.lastName)
                    }


                    try {
                        if (kitchenSettingModel.showCustomerPhone) {

                            if (orderData.data?.customer?.phones?.isNotEmpty() == true) {

                                PrintSunmiUtils.normalTextLarge(
                                    MethodUtils.getUSFormatNumber(
                                        orderData.data?.customer?.phones?.get(
                                            orderData.data?.customer?.phones?.size?.minus(1) ?: 0
                                        )?.phoneNumber ?: ""
                                    )

                                )
                            }

                        }
                    } catch (e: Exception) {
                    }

                }


            }
            Log.d(
                "KITCHEN_RECEIPT::",
                "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
            )

            PrintSunmiUtils.cutPaperInner()
            Log.d(
                "KITCHEN_RECEIPT::",
                "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber}"
            )

        } catch (e: Exception) {
            // printerDialog.dismiss()
            Log.d(
                "KITCHEN_RECEIPT::",
                "${Exception().stackTrace[0].fileName} -> ${Exception().stackTrace[0].lineNumber} -> ${
                    Gson().toJson(e)
                }"
            )
            e.printStackTrace()
        }


    }

    private fun createNotification(): Notification {

        var contentIntent: PendingIntent? = null
        contentIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_MUTABLE
            )
        } else {
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_ONE_SHOT
            )
        }

        /*  val contentIntent = PendingIntent.getActivity(
              this,
              0,
              Intent(this, MainActivity::class.java),
              0
          ) */

        val notification = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("Kiosk Sync")
            .setSmallIcon(R.drawable.ic_notification) // Replace with your icon resource
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()

        notification.flags = Notification.FLAG_NO_CLEAR

        return notification
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                notificationChannelId,
                "Kiosk Sync",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationChannel.description = "To Fetch The Kiosk orders"
            val notificationManager =
                getSystemService(NotificationManager::class.java) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true) // Remove the notification when the service stops
    }

    override fun onStatusChangeEvent(p0: String?, p1: Int) {


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
            Log.e(TAG2,"InitalizeRequestQueue ")
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
                Log.e(TAG2,"QueueREsponseOK ")

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

    fun setCharacterSize(h: Int, w: Int) {
        var n = 0
        if (h >= 1 && h <= 8) n = n or (h - 1)
        if (w >= 1 && w <= 8) {
            n = n or ((w - 1) shl 4)
            charHSize = w
        }
        orderContent.append("1d21" + String.format("%02x", n))
    }


}