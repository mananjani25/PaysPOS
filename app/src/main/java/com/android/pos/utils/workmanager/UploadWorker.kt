package com.android.pos.utils.workmanager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.android.pos.R
import com.android.pos.data.model.GuestAttrQueue
import com.android.pos.data.model.PrinterJSONElementData
import com.android.pos.data.model.PrinterQueueModel
import com.android.pos.data.model.responseModel.CreateOrderResponse
import com.android.pos.data.model.responseModel.GetKitchenReceiptSettingsResponse
import com.android.pos.data.model.responseModel.PrinterResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.CREATE_QUEUE_PRINTER_PHASE3
import com.android.pos.data.remote.Constants.DELETE_QUEUE_ORDER_PHASE3
import com.android.pos.data.remote.Constants.DINE_IN
import com.android.pos.data.remote.Constants.IS_MASTER_TERMINAL
import com.android.pos.data.remote.Constants.createCloudPrinterWithName
import com.android.pos.utils.LogUtil
import com.android.pos.utils.addBuilderText
import com.android.pos.utils.addDoubleDotLineForSunmiQueue
import com.android.pos.utils.addHorizontalLine
import com.android.pos.utils.addHorizontalLineNew
import com.android.pos.utils.addHorizontalLineNewU220
import com.android.pos.utils.addOrdersForKitchenCustomer
import com.android.pos.utils.addOrdersForKitchenCustomerNewPrinter
import com.android.pos.utils.padLine
import com.android.pos.utils.printGuestByItemForQueue
import com.android.pos.utils.printGuestByItemForSunmiQueue
import com.android.pos.utils.printer.PrinterClass
import com.epson.epos2.ConnectionListener
import com.epson.epos2.Epos2Exception
import com.epson.epos2.printer.Printer
import com.epson.epos2.printer.PrinterStatusInfo
import com.epson.epos2.printer.ReceiveListener
import com.epson.epos2.printer.StatusChangeListener
import com.epson.eposprint.Builder
import com.epson.eposprint.Print
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.hosopy.actioncable.ActionCable
import com.hosopy.actioncable.Channel
import com.hosopy.actioncable.Consumer
import com.hosopy.actioncable.Subscription
import com.sunmi.externalprinterlibrary2.ConnectCallback
import com.sunmi.externalprinterlibrary2.ResultCallback
import com.sunmi.externalprinterlibrary2.StatusCallback
import com.sunmi.externalprinterlibrary2.printer.CloudPrinter
import com.sunmi.externalprinterlibrary2.printer.CloudPrinterBuilder
import com.sunmi.externalprinterlibrary2.style.AlignStyle
import com.sunmi.externalprinterlibrary2.style.CloudPrinterStatus
import com.sunmi.externalprinterlibrary2.style.UnderlineStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.NotNull
import java.net.URI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class UploadWorker(@NotNull context: Context, @NotNull params: WorkerParameters) :
    CoroutineWorker(context, params), StatusChangeListener, ReceiveListener, ConnectionListener,
    ResultCallback {
    private var printerQueueData: Boolean = false
    private var globalPrinterQueue: JsonElement? = null
    private val TAG = UploadWorker::class.java.name
    private var isConnectedU220: Boolean = false
    val printerQueueModel: PrinterQueueModel = PrinterQueueModel()
    private var printerObjList: HashMap<String, Any> = hashMapOf()
    var listOfPrintersData: ArrayList<PrinterJSONElementData> = arrayListOf()
    var isQueueRunning: Boolean = false

    var printerBreak: Boolean = false
    var printerSize: Int = 0
    var orderSize: Int = 0
    var currentPrinterIndex = 0
    var currentOrderIndex = 0
    val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var subscription: Subscription? = null
    private var consumer: Consumer? = null
    private var locationId: Int = 0
    private var baseUrl = ""
    private var kitchenPrinterList: List<PrinterResponse.Data.KitchenReceiptPrinters> = listOf()
    private var kitchenSettingModel = GetKitchenReceiptSettingsResponse.Data()
    private var mContext: Context = context
    private var isPrinterRunning: Boolean = false
    private var printerBGRunning: Boolean = false


    override suspend fun doWork(): Result {

        try {


            withContext(Dispatchers.IO) {

                printerObjList.clear()

                locationId = inputData.getInt("location_id", 0)
                baseUrl = inputData.getString("base_url").toString()


                var serializeObjKitchenPrinters = mContext.getSharedPreferences(
                    mContext.resources.getString(R.string.app_name),
                    Context.MODE_PRIVATE
                ).getString(Constants.KITCHEN_PRINTER_LIST_PREF, "")
                if (serializeObjKitchenPrinters?.isNotEmpty() == true) {
                    val gson = Gson()
                    val type =
                        object :
                            TypeToken<List<PrinterResponse.Data.KitchenReceiptPrinters>?>() {}.type
                    kitchenPrinterList =
                        gson.fromJson<Any>(
                            serializeObjKitchenPrinters,
                            type
                        ) as ArrayList<PrinterResponse.Data.KitchenReceiptPrinters>


                }

                Log.e(TAG, "checkKit:  ${kitchenPrinterList.size}")
                if (kitchenPrinterList.isNotEmpty() && kitchenPrinterList.get(0).name.contains(
                        "U220",
                        true
                    ) == true
                ) {

                    Log.e(TAG, "checkContainU330")
                    for (i in 0 until kitchenPrinterList.size) {
                        var printer1: Printer =
                            Printer(Printer.TM_U220, Printer.MODEL_ANK, mContext)
                        printer1.setConnectionEventListener(this@UploadWorker)
                        printer1.setReceiveEventListener(object : ReceiveListener {
                            override fun onPtrReceive(
                                p0: Printer?,
                                p1: Int,
                                p2: PrinterStatusInfo?,
                                p3: String?
                            ) {
                                Log.e(
                                    TAG,
                                    "checkConnectionPhase2  getAdmin${Gson().toJson(p0?.admin)}  location: ${
                                        Gson().toJson(p0?.location)
                                    } checkStatus:${
                                        com.google.gson.Gson().toJson(p0?.status)
                                    }"
                                )
                            }

                        })

                        try {
                            printer1.connect(
                                "TCP:" + kitchenPrinterList.get(i).macAddress,
                                Printer.PARAM_DEFAULT
                            )

                        } catch (e: Epos2Exception) {
                            var errorCode = e.errorStatus

                            // Log.e(TAG, "errorCode:  ${errorCode}")
                            // sendNotification("TM-U220 is Offline.Please check ${errorCode}")
                            e.printStackTrace()
                        }

                        printerObjList.set(kitchenPrinterList.get(i).macAddress ?: "", printer1)


                    }
                } else {
                    Log.e(TAG, "CloudPrinter  ")
                    for (i in 0 until kitchenPrinterList.size) {

                        var cloudPrinter: CloudPrinter = CloudPrinterBuilder.buildPrinter(
                            kitchenPrinterList.get(i).modalName,
                            kitchenPrinterList.get(i).macAddress
                        )
                        cloudPrinter.connect(mContext, object : ConnectCallback {
                            override fun onConnect() {
                                printerObjList.set(
                                    kitchenPrinterList.get(i).macAddress ?: "",
                                    cloudPrinter
                                )
                            }

                            override fun onFailed(p0: String?) {
                            }

                            override fun onDisConnect() {
                            }

                        })


                    }


                }





                LogUtil.logE(TAG, "onActionCableStarts")
                connectActionCable()

            }
            return Result.success()
        } catch (e: java.lang.Exception) {
            return Result.failure()
        }

        //  connectPrinter(context = applicationContext, data)


    }

    suspend fun connectActionCable() {
        // 1. Setup
        var requestURL =
            baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3
        val uri = URI("wss://hugepos.com/cable")
        consumer = ActionCable.createConsumer(uri)

        // 2. Create subscription
        val appearanceChannel = Channel("PrinterQueueV4Channel")
        appearanceChannel.addParam("id", locationId)
        // appearanceChannel.addParam("id",prefProvider.getValueInt(LOCATION_ID,0))
        subscription = consumer?.subscriptions?.create(appearanceChannel)

        if (subscription != null) {
            subscription?.onConnected {
                LogUtil.logE(TAG, "onActionConnected")
                val params = JsonObject()
                params.addProperty("id", locationId)
                params.addProperty("url", requestURL)
                subscription?.perform("received", params)
            }?.onRejected {
                LogUtil.logE(TAG, "onActiononRejected")
                if (mContext.getSharedPreferences(
                        mContext.resources.getString(R.string.app_name),
                        Context.MODE_PRIVATE
                    ).getBoolean(IS_MASTER_TERMINAL, false) == true
                ) {
                    subscription = consumer?.subscriptions?.create(appearanceChannel)
                    val params = JsonObject()
                    params.addProperty("id", locationId)
                    subscription?.perform("received", params)
                }
            }?.onReceived {
                LogUtil.logE(TAG, "onActiononReceived  " + Gson().toJson(it))

                if (mContext.getSharedPreferences(
                        mContext.resources.getString(R.string.app_name),
                        Context.MODE_PRIVATE
                    ).getBoolean(IS_MASTER_TERMINAL, false) == true
                ) {

                    if (it != null) {

                        if (it.asJsonObject.has("printer_queue")) {
                            isPrinterRunning = true
                            globalPrinterQueue = it.asJsonObject.get("printer_queue")
                            runBlocking {
                                getQueueDataResponse(it.asJsonObject.get("printer_queue"))
                            }


                        } else {

                            runBlocking {


                                delay(2000)

                                val params = JsonObject()
                                params.addProperty("id", locationId)
                                params.addProperty("url", requestURL)
                                subscription?.perform("received", params)
                            }


                            /*val intent = Intent()
                        intent.putExtra(Constants.DATA, "")
                        intent.action = PRINTER_QUEUE_DATA_RECEIVED
                        mContext.sendBroadcast(intent)*/


                        }

                    } else {
                        /*  val params = JsonObject()
                      params.addProperty("id", locationId)
                      params.addProperty("url", requestURL)
                      subscription?.perform("received", params)*/

                        GlobalScope.launch(Dispatchers.IO) {
                            delay(10000)
                            /*val params2 = JsonObject()
                        params2.addProperty("id", locationId)
                        params2.addProperty(
                            "url", baseUrl + Constants.CREATE_QUEUE_PRINTER
                        )
                        subscription?.perform("received", params2)*/
                        }
                    }
                } else {
                    consumer?.disconnect()
                    subscription?.onDisconnected(object : Subscription.DisconnectedCallback {
                        override fun call() {
                            Log.e(TAG, "Action Cable is Disconnected..")
                        }

                    })
                }

            }?.onDisconnected {
                LogUtil.logE(TAG, "onActiononDisconnected")
                if (mContext.getSharedPreferences(
                        mContext.resources.getString(R.string.app_name),
                        Context.MODE_PRIVATE
                    ).getBoolean(IS_MASTER_TERMINAL, false) == true
                ) {
                    subscription = consumer?.subscriptions?.create(appearanceChannel)
                    val params = JsonObject()
                    params.addProperty("id", locationId)
                    subscription?.perform("received", params)
                }
            }?.onFailed {
                LogUtil.logE(TAG, "onActiononFailed")
                //subscription = consumer?.subscriptions?.create(appearanceChannel)
                try {

                    /*subscription = consumer?.subscriptions?.create(appearanceChannel)
                    val params = JsonObject()
                    params.addProperty("id", locationId)
                    subscription?.perform("received", params)*/
                    if (mContext.getSharedPreferences(
                            mContext.resources.getString(R.string.app_name),
                            Context.MODE_PRIVATE
                        ).getBoolean(IS_MASTER_TERMINAL, false) == true
                    ) {

                        sendNotification("Please check your Network Connectivity.")
                        consumer?.connect()


                    }

                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }

            }
        }


        // 3. Establish connection
        consumer?.connect()


    }

    private suspend fun getActionCableData(model: JsonElement) {
        if (model.asJsonObject.has("data")) {
            var dataList: JsonArray = model.asJsonObject.get("data").asJsonArray

            dataList.forEachIndexed { index, it ->
                val printerQueueModel: PrinterQueueModel = PrinterQueueModel()

                val obj = it.asJsonObject.get("order_data").asJsonObject

                if (obj.asJsonObject.has("order_items_attributes")) {
                    var itemArray = obj.asJsonObject.get("order_items_attributes").asJsonArray
                    var itemAttribute: ArrayList<CreateOrderResponse.Data.Order.OrderItem> =
                        arrayListOf()
                    var itemModifiers: ArrayList<CreateOrderResponse.Data.Order.OrderItem.OrderItemModifiers> =
                        arrayListOf()


                    itemArray.forEach {
                        if (it.asJsonObject.has("order_item_modifiers_attributes")) {
                            var modifiersList =
                                it.asJsonObject.get("order_item_modifiers_attributes").asJsonArray

                            if (modifiersList.size() != 0) {
                                modifiersList.forEach {
                                    val jsonObj = it.asJsonObject
                                    itemModifiers.add(
                                        CreateOrderResponse.Data.Order.OrderItem.OrderItemModifiers(
                                            name = jsonObj.get("name").asString,
                                            id = 0,
                                            orderItemId = 0,
                                            orderId = 0,
                                            quantity = jsonObj.get("quantity").asInt,
                                            price = 0.0,
                                            modifierSetId = 0,
                                            updatedAt = "",
                                            createdAt = "",
                                            totalPrice = 0.0,
                                            isModifier = false
                                        )
                                    )


                                }

                            }
                        }
                        var orderItem = CreateOrderResponse.Data.Order.OrderItem(
                            categoryId = it.asJsonObject.get("category_id").asInt,
                            completedInKitchen = false,
                            discountAmount = 0.0,
                            discountId = 0,
                            discountType = "",
                            employeeId = it.asJsonObject.get("employee_id").asInt,
                            float = 0.0,
                            id = 0,
                            isPaid = false,
                            isPrinted = false,
                            itemId = it.asJsonObject.get("item_id").asInt,
                            itemName = it.asJsonObject.get("item_name").asString,
                            note = it.asJsonObject.get("note").asString,
                            orderItemModifiers = itemModifiers,
                            price = it.asJsonObject.get("price").asDouble,
                            quantity = it.asJsonObject.get("quantity").asInt,
                            timestamp = "",
                            totalPrice = 0.0,
                            orderId = 0
                        )

                        itemAttribute.add(orderItem)


                    }
                    printerQueueModel.orderItems = itemAttribute
                    printerQueueModel.orderID = ""
                    printerQueueModel.orderType = it.asJsonObject.get("order_type").asString
                    printerQueueModel.id = it.asJsonObject.get("id").asInt
                    printerQueueModel.offlineId = obj.asJsonObject.get("offline_id").asString
                    printerQueueModel.paymentType = "Cash"
                    printerQueueModel.status = Constants.PENDING
                    printerQueueModel.totalAmt = obj.asJsonObject.get("total_amount").asDouble
                    printerQueueModel.orderID = "" + it.asJsonObject.get("orderid").asInt
                    //obj.asJsonObject.get("terminal_name")?.asString ?: ""
                    printerQueueModel.position = index

                    /*if (it.asJsonObject.has("customer_data")) {
                        if (it.asJsonObject.get("customer_data").asJsonObject.has("first_name") && it.asJsonObject.get(
                                "customer_data"
                            ).asJsonObject.get("first_name").toString().isNotEmpty()
                        ) {
                            printerQueueModel.customerName =
                                it.asJsonObject.get("customer_data").asJsonObject.get("first_name")
                                    .toString() + " " + it.asJsonObject.get("customer_data").asJsonObject.get(
                                    "last_name"
                                ).toString()
                            if (it.asJsonObject.get("customer_data").asJsonObject.has("phone")) {
                                var phoneNo =
                                    it.asJsonObject.get("customer_data").asJsonObject.get("phone").asJsonObject

                                if (phoneNo.has("phone_number") && phoneNo.get("phone_number")
                                        .toString().isNotEmpty()
                                ) {
                                    printerQueueModel.customerPhoneNo =
                                        phoneNo.get("phone_number").toString()

                                }

                            }

                            if (it.asJsonObject.get("customer_data").asJsonObject.has("address")) {
                                var address =
                                    it.asJsonObject.get("customer_data").asJsonObject.get("address").asJsonObject

                                if (address.has("address1") && address.get("address1")
                                        .toString().isNotEmpty()
                                ) {
                                    printerQueueModel.customerPhoneNo = address.get("address1")
                                        .toString() + " " + address.get("address2")
                                        .toString() + " " + address.get("city")
                                        .toString() + " " + address.get("state")
                                        .toString() + " " + address.get("country")
                                        .toString() + " " + address.get("postcode").toString()

                                }

                            }
                        }


                    }*/


                }


            }


        }

    }

    private suspend fun getQueueDataResponse(model: JsonElement) {
        if (model.asJsonObject.has("data")) {

            isPrinterRunning = true


            var dataList: JsonArray = model.asJsonObject.get("data").asJsonArray

            if (dataList.size() != 0) {
                if (printerObjList.size != dataList.size()) {
                    Log.e(TAG, "sizeNotEquall")
                    //printerObjList.clear()
                    for (i in 0 until dataList.size()) {

                        Log.e(
                            TAG,
                            "getMAcAddressPrintOB:   ${
                                printerObjList.get(
                                    dataList.get(i).asJsonObject.get("mac_address").asString
                                )
                            }"
                        )

                        if (printerObjList.get(dataList.get(i).asJsonObject.get("mac_address").asString) == null) {
                            if (dataList.get(i).asJsonObject.get("printer_brand").asString.equals(
                                    Constants.SUNMIBRAND
                                )
                            ) {
                                var cloudPrinter: CloudPrinter = createCloudPrinterWithName(
                                    dataList.get(i).asJsonObject.get("printer_name").asString,
                                    dataList.get(i).asJsonObject.get("mac_address").asString,
                                    dataList.get(i).asJsonObject.get("port_no").asInt
                                )


                                printerObjList.set(
                                    dataList.get(i).asJsonObject.get("mac_address").asString
                                        ?: "",
                                    cloudPrinter
                                )
                                /*cloudPrinter.connect(mContext, object : ConnectCallback {
                                    override fun onConnect() {


                                        printerObjList.set(
                                            dataList.get(i).asJsonObject.get("mac_address").asString
                                                ?: "",
                                            cloudPrinter
                                        )
                                    }

                                    override fun onFailed(p0: String?) {


                                        printerObjList.set(
                                            dataList.get(i).asJsonObject.get("mac_address").asString
                                                ?: "",
                                            cloudPrinter
                                        )
                                    }

                                    override fun onDisConnect() {

                                        printerObjList.set(
                                            dataList.get(i).asJsonObject.get("mac_address").asString
                                                ?: "",
                                            cloudPrinter
                                        )

                                    }

                                })*/


                            } else {

                                var printer1: Printer =
                                    Printer(Printer.TM_U220, Printer.MODEL_ANK, mContext)
                                printer1.setConnectionEventListener(this@UploadWorker)
                                printer1.setReceiveEventListener(object : ReceiveListener {
                                    override fun onPtrReceive(
                                        p0: Printer?,
                                        p1: Int,
                                        p2: PrinterStatusInfo?,
                                        p3: String?
                                    ) {
                                        Log.e(
                                            TAG,
                                            "checkConnectionPhase2  getAdmin${Gson().toJson(p0?.admin)}  location: ${
                                                Gson().toJson(p0?.location)
                                            } checkStatus:${
                                                com.google.gson.Gson().toJson(p0?.status)
                                            }"
                                        )
                                    }

                                })

                                try {
                                    printer1.connect(
                                        "TCP:" + dataList.get(i).asJsonObject.get("mac_address").asString,
                                        Printer.PARAM_DEFAULT
                                    )

                                } catch (e: Epos2Exception) {
                                    try {
                                        printer1.disconnect()
                                        printer1.connect(
                                            "TCP:" + dataList.get(i).asJsonObject.get("mac_address").asString,
                                            Printer.PARAM_DEFAULT
                                        )
                                    } catch (e: Exception) {
                                        try {
                                            printer1.connect(
                                                "TCP:" + dataList.get(i).asJsonObject.get("mac_address").asString,
                                                Printer.PARAM_DEFAULT
                                            )
                                        } catch (e: java.lang.Exception) {
                                            e.printStackTrace()
                                        }
                                        e.printStackTrace()
                                    }
                                    var errorCode = e.errorStatus

                                    // Log.e(TAG, "errorCode:  ${errorCode}")
                                    // sendNotification("TM-U220 is Offline.Please check ${errorCode}")
                                    e.printStackTrace()
                                }

                                printerObjList.set(
                                    dataList.get(i).asJsonObject.get("mac_address").asString ?: "",
                                    printer1
                                )

                            }

                        }
                    }


                    delay(2000)
                    listOfPrintersData.clear()
                    listOfPrintersData = arrayListOf()

                    dataList.forEach {

                        var listofPrinterOrders: ArrayList<PrinterQueueModel> = arrayListOf()
                        if (it.asJsonObject.has("orders")) {
                            var ordersArray = it.asJsonObject.get("orders").asJsonArray

                            ordersArray.forEach {
                                var modelOrder = PrinterQueueModel()
                                modelOrder.id = it.asJsonObject.get("id").asInt
                                modelOrder.orderType =
                                    it.asJsonObject.get("order_type").asString
                                modelOrder.dateAndTime =
                                    it.asJsonObject.get("date_and_time").asString
                                modelOrder.employeeName =
                                    it.asJsonObject.get("employee_name").asString
                                if (it.asJsonObject.get("order_type").asString.equals(
                                        DINE_IN,
                                        true
                                    )
                                ) {

                                    if (it.asJsonObject.has("guest_attributes")) {
                                        var guestAttributes: ArrayList<GuestAttrQueue> =
                                            arrayListOf()
                                        var guestAtr =
                                            it.asJsonObject.get("guest_attributes").asJsonArray



                                        guestAtr.forEach { it5 ->
                                            if (it5.asJsonObject.has("order_items")) {

                                                var orderItems: ArrayList<CreateOrderResponse.Data.Order.OrderItem> =
                                                    arrayListOf()

                                                var itemsArray =
                                                    it5.asJsonObject.get("order_items").asJsonArray

                                                if (itemsArray.size() != 0) {

                                                    var listOfMod: ArrayList<CreateOrderResponse.Data.Order.OrderItem.OrderItemModifiers> =
                                                        arrayListOf()
                                                    itemsArray.forEach { it9 ->
                                                        if (it9.asJsonObject.has("modifiers")) {

                                                            var iteMod =
                                                                it9.asJsonObject.get("modifiers").asJsonArray


                                                            iteMod.forEach {
                                                                listOfMod.add(
                                                                    CreateOrderResponse.Data.Order.OrderItem.OrderItemModifiers(
                                                                        id = it.asJsonObject.get("id").asInt,
                                                                        orderItemId = it.asJsonObject.get(
                                                                            "order_item_id"
                                                                        ).asInt,
                                                                        name = it.asJsonObject.get("name").asString,
                                                                        quantity = it.asJsonObject.get(
                                                                            "quantity"
                                                                        ).asInt,
                                                                        modifierSetId = 0,
                                                                        isModifier = true,
                                                                        modifierQuantity = it.asJsonObject.get(
                                                                            "modifier_quantity"
                                                                        ).asInt,
                                                                        createdAt = "",
                                                                        updatedAt = "",
                                                                        totalPrice = 0.0,
                                                                        orderId = 0,
                                                                        price = 0.0
                                                                    )
                                                                )

                                                            }


                                                        }


                                                        orderItems.add(
                                                            CreateOrderResponse.Data.Order.OrderItem(
                                                                categoryId = it9.asJsonObject.get("category_id").asInt,
                                                                completedInKitchen = false,
                                                                discountType = "",
                                                                discountAmount = 0.0,
                                                                discountId = 0,
                                                                employeeId = 0,
                                                                float = 0.0,
                                                                id = it9.asJsonObject.get("order_item_id").asInt,
                                                                isPrinted = false,
                                                                isPaid = false,
                                                                itemId = it9.asJsonObject.get("item_id").asInt,
                                                                orderId = it.asJsonObject.get("id").asInt,
                                                                itemName = it9.asJsonObject.get("name").asString,
                                                                totalPrice = 0.0,
                                                                timestamp = if (it9.asJsonObject.has(
                                                                        "message"
                                                                    )
                                                                ) {
                                                                    it9.asJsonObject.get("message").asString
                                                                } else {
                                                                    ""
                                                                },
                                                                quantity = it9.asJsonObject.get("quantity").asInt,
                                                                price = 0.0,
                                                                orderItemModifiers = listOfMod,
                                                                note = it9.asJsonObject.get("item_note").asString


                                                            )
                                                        )


                                                    }

                                                    guestAttributes.add(
                                                        GuestAttrQueue(
                                                            id = 0,
                                                            name = it5.asJsonObject.get("name").asString,
                                                            listOfItems = orderItems


                                                        )
                                                    )
                                                }

                                            }


                                        }

                                        modelOrder.guestAttributes = guestAttributes
                                        modelOrder.orderID =
                                            it.asJsonObject.get("id").asInt.toString()


                                    }

                                    listofPrinterOrders.add(modelOrder)


                                } else {

                                    var orderItems: ArrayList<CreateOrderResponse.Data.Order.OrderItem> =
                                        arrayListOf()
                                    if (it.asJsonObject.has("order_items")) {
                                        var orderItemsArray =
                                            it.asJsonObject.get("order_items").asJsonArray
                                        orderItemsArray.forEach { it1 ->
                                            var listOfMod: ArrayList<CreateOrderResponse.Data.Order.OrderItem.OrderItemModifiers> =
                                                arrayListOf()
                                            if (it1.asJsonObject.has("modifiers")) {
                                                var modList =
                                                    it1.asJsonObject.get("modifiers").asJsonArray
                                                if (modList.size() != 0) {
                                                    modList.forEach {
                                                        listOfMod.add(
                                                            CreateOrderResponse.Data.Order.OrderItem.OrderItemModifiers(
                                                                id = it.asJsonObject.get("id").asInt,
                                                                orderItemId = it.asJsonObject.get("order_item_id").asInt,
                                                                name = it.asJsonObject.get("name").asString,
                                                                quantity = it.asJsonObject.get("quantity").asInt,
                                                                modifierSetId = 0,
                                                                isModifier = true,
                                                                modifierQuantity = it.asJsonObject.get(
                                                                    "modifier_quantity"
                                                                ).asInt,
                                                                createdAt = "",
                                                                updatedAt = "",
                                                                totalPrice = 0.0,
                                                                orderId = 0,
                                                                price = 0.0
                                                            )
                                                        )

                                                    }

                                                }


                                            }

                                            orderItems.add(
                                                CreateOrderResponse.Data.Order.OrderItem(
                                                    categoryId = it1.asJsonObject.get("category_id").asInt,
                                                    completedInKitchen = false,
                                                    discountType = "",
                                                    discountAmount = 0.0,
                                                    discountId = 0,
                                                    employeeId = 0,
                                                    float = 0.0,
                                                    id = it1.asJsonObject.get("order_item_id").asInt,
                                                    isPrinted = false,
                                                    isPaid = false,
                                                    itemId = it1.asJsonObject.get("item_id").asInt,
                                                    orderId = it.asJsonObject.get("id").asInt,
                                                    itemName = it1.asJsonObject.get("name").asString,
                                                    totalPrice = 0.0,
                                                    timestamp = if (it1.asJsonObject.has("message")) {
                                                        it1.asJsonObject.get("message").asString
                                                    } else {
                                                        ""
                                                    },
                                                    quantity = it1.asJsonObject.get("quantity").asInt,
                                                    price = 0.0,
                                                    orderItemModifiers = listOfMod,
                                                    note = it1.asJsonObject.get("item_note").asString


                                                )
                                            )

                                        }

                                        modelOrder.orderItems = orderItems
                                        modelOrder.orderID =
                                            it.asJsonObject.get("id").asInt.toString()


                                    }

                                }


                                listofPrinterOrders.add(modelOrder)
                            }


                        }
                        var modelPrinterParser = PrinterJSONElementData(
                            printerName = it.asJsonObject.get("printer_name").asString,
                            macAddress = it.asJsonObject.get("mac_address").asString,
                            ipAddress = it.asJsonObject.get("ip_address").asString,
                            modelName = it.asJsonObject.get("modal_name").asString,
                            printerQueueModelList = listofPrinterOrders,
                            portNo = it.asJsonObject.get("port_no").asInt

                        )

                        listOfPrintersData.add(modelPrinterParser)


                    }

                    /*Log.e(
                    TAG,
                    "checkFirstIndexdataAraay  ${listOfPrintersData.get(0).printerQueueModelList.size}"
                )*/

                    printerSize = listOfPrintersData.size
                    orderSize = listOfPrintersData.get(0).printerQueueModelList.size

                    currentOrderIndex = 0
                    currentPrinterIndex = 0

                    if (listOfPrintersData.get(0).printerName.contains("CloudPrint_", true)) {

                        if (listOfPrintersData.get(0).printerQueueModelList.size != 0) {
                            Log.e(TAG, "sendData1st:  ")
                            sendDataToPrintToSunmi(
                                listOfPrintersData,
                                currentPrinterIndex,
                                printerObjList.get(listOfPrintersData[0].macAddress) as CloudPrinter?,
                                listOfPrintersData.get(0).printerQueueModelList,
                                listOfPrintersData[0].macAddress,
                                currentOrderIndex
                            )
                        } else if (listOfPrintersData.size - 1 > currentPrinterIndex) {
                            Log.e(TAG, "sendData2nd:  ")
                            var isBreak = false
                            for (i in currentPrinterIndex++ until listOfPrintersData.size) {
                                if (listOfPrintersData.get(i).printerQueueModelList.isNotEmpty()) {

                                    isBreak = true
                                    currentPrinterIndex = i
                                    currentOrderIndex = 0
                                    sendDataToPrintToSunmi(
                                        listOfPrintersData,
                                        i,
                                        printerObjList.get(listOfPrintersData[i].macAddress) as CloudPrinter?,
                                        listOfPrintersData.get(i).printerQueueModelList,
                                        listOfPrintersData[i].macAddress,
                                        currentOrderIndex
                                    )
                                    break
                                }
                            }
                            if (isBreak == false) {
                                delay(2000)
                                val params = JsonObject()
                                params.addProperty("id", locationId)
                                params.addProperty(
                                    "url",
                                    baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3
                                )

                                Log.e(
                                    TAG,
                                    "checkReuestURL: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                                )
                                subscription?.perform("received", params)
                            }


                        } else {

                            // Log.e(TAG, "sendData3rd:  ")
                            delay(2000)
                            val params = JsonObject()
                            params.addProperty("id", locationId)
                            params.addProperty(
                                "url",
                                baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3
                            )
                            Log.e(
                                TAG,
                                "checkReuestURL: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                            )
                            subscription?.perform("received", params)
                        }
                    } else {

                        if (listOfPrintersData.get(0).printerQueueModelList.size != 0) {
                            Log.e(TAG, "sendData1st:  ")
                            sendDataToPrint(
                                listOfPrintersData,
                                currentPrinterIndex,
                                printerObjList.get(listOfPrintersData[0].macAddress) as Printer?,
                                listOfPrintersData.get(0).printerQueueModelList,
                                listOfPrintersData[0].macAddress,
                                currentOrderIndex
                            )
                        } else if (listOfPrintersData.size - 1 > currentPrinterIndex) {
                            Log.e(TAG, "sendData2nd:  ")
                            var isBreak = false
                            for (i in currentPrinterIndex++ until listOfPrintersData.size) {
                                if (listOfPrintersData.get(i).printerQueueModelList.isNotEmpty()) {

                                    isBreak = true
                                    currentPrinterIndex = i
                                    currentOrderIndex = 0
                                    sendDataToPrint(
                                        listOfPrintersData,
                                        i,
                                        printerObjList.get(listOfPrintersData[i].macAddress) as Printer?,
                                        listOfPrintersData.get(i).printerQueueModelList,
                                        listOfPrintersData[i].macAddress,
                                        currentOrderIndex
                                    )
                                    break
                                }
                            }
                            if (isBreak == false) {
                                delay(2000)
                                val params = JsonObject()
                                params.addProperty("id", locationId)
                                params.addProperty(
                                    "url",
                                    baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3
                                )

                                Log.e(
                                    TAG,
                                    "checkReuestURL: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                                )
                                subscription?.perform("received", params)
                            }


                        } else {

                            // Log.e(TAG, "sendData3rd:  ")
                            delay(2000)
                            val params = JsonObject()
                            params.addProperty("id", locationId)
                            params.addProperty(
                                "url",
                                baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3
                            )
                            Log.e(
                                TAG,
                                "checkReuestURL: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                            )
                            subscription?.perform("received", params)
                        }
                    }


                } else {


                    listOfPrintersData.clear()
                    listOfPrintersData = arrayListOf()

                    dataList.forEach {


                        var listofPrinterOrders: ArrayList<PrinterQueueModel> = arrayListOf()
                        if (it.asJsonObject.has("orders")) {
                            var ordersArray = it.asJsonObject.get("orders").asJsonArray

                            ordersArray.forEach {
                                var modelOrder = PrinterQueueModel()
                                modelOrder.id = it.asJsonObject.get("id").asInt
                                modelOrder.orderType =
                                    it.asJsonObject.get("order_type").asString
                                modelOrder.dateAndTime =
                                    it.asJsonObject.get("date_and_time").asString
                                modelOrder.employeeName =
                                    it.asJsonObject.get("employee_name").asString
                                if (it.asJsonObject.get("order_type").asString.equals(
                                        DINE_IN,
                                        true
                                    )
                                ) {

                                    if (it.asJsonObject.has("guest_attributes")) {
                                        var guestAttributes: ArrayList<GuestAttrQueue> =
                                            arrayListOf()
                                        var guestAtr =
                                            it.asJsonObject.get("guest_attributes").asJsonArray



                                        guestAtr.forEach { it5 ->
                                            if (it5.asJsonObject.has("order_items")) {

                                                var orderItems: ArrayList<CreateOrderResponse.Data.Order.OrderItem> =
                                                    arrayListOf()

                                                var itemsArray =
                                                    it5.asJsonObject.get("order_items").asJsonArray

                                                if (itemsArray.size() != 0) {

                                                    var listOfMod: ArrayList<CreateOrderResponse.Data.Order.OrderItem.OrderItemModifiers> =
                                                        arrayListOf()
                                                    itemsArray.forEach { it9 ->
                                                        if (it9.asJsonObject.has("modifiers")) {

                                                            var iteMod =
                                                                it9.asJsonObject.get("modifiers").asJsonArray


                                                            iteMod.forEach {
                                                                listOfMod.add(
                                                                    CreateOrderResponse.Data.Order.OrderItem.OrderItemModifiers(
                                                                        id = it.asJsonObject.get("id").asInt,
                                                                        orderItemId = it.asJsonObject.get(
                                                                            "order_item_id"
                                                                        ).asInt,
                                                                        name = it.asJsonObject.get("name").asString,
                                                                        quantity = it.asJsonObject.get(
                                                                            "quantity"
                                                                        ).asInt,
                                                                        modifierSetId = 0,
                                                                        isModifier = true,
                                                                        modifierQuantity = it.asJsonObject.get(
                                                                            "modifier_quantity"
                                                                        ).asInt,
                                                                        createdAt = "",
                                                                        updatedAt = "",
                                                                        totalPrice = 0.0,
                                                                        orderId = 0,
                                                                        price = 0.0
                                                                    )
                                                                )

                                                            }


                                                        }


                                                        orderItems.add(
                                                            CreateOrderResponse.Data.Order.OrderItem(
                                                                categoryId = it9.asJsonObject.get("category_id").asInt,
                                                                completedInKitchen = false,
                                                                discountType = "",
                                                                discountAmount = 0.0,
                                                                discountId = 0,
                                                                employeeId = 0,
                                                                float = 0.0,
                                                                id = it9.asJsonObject.get("order_item_id").asInt,
                                                                isPrinted = false,
                                                                isPaid = false,
                                                                itemId = it9.asJsonObject.get("item_id").asInt,
                                                                orderId = it.asJsonObject.get("id").asInt,
                                                                itemName = it9.asJsonObject.get("name").asString,
                                                                totalPrice = 0.0,
                                                                timestamp = if (it9.asJsonObject.has(
                                                                        "message"
                                                                    )
                                                                ) {
                                                                    it9.asJsonObject.get("message").asString
                                                                } else {
                                                                    ""
                                                                },
                                                                quantity = it9.asJsonObject.get("quantity").asInt,
                                                                price = 0.0,
                                                                orderItemModifiers = listOfMod,
                                                                note = it9.asJsonObject.get("item_note").asString


                                                            )
                                                        )


                                                    }

                                                    guestAttributes.add(
                                                        GuestAttrQueue(
                                                            id = 0,
                                                            name = it5.asJsonObject.get("name").asString,
                                                            listOfItems = orderItems


                                                        )
                                                    )
                                                }

                                            }


                                        }

                                        modelOrder.guestAttributes = guestAttributes
                                        modelOrder.orderID =
                                            it.asJsonObject.get("id").asInt.toString()
                                    }


                                    listofPrinterOrders.add(modelOrder)

                                } else {


                                    var orderItems: ArrayList<CreateOrderResponse.Data.Order.OrderItem> =
                                        arrayListOf()
                                    if (it.asJsonObject.has("order_items")) {
                                        var orderItemsArray =
                                            it.asJsonObject.get("order_items").asJsonArray
                                        orderItemsArray.forEach { it1 ->
                                            var listOfMod: ArrayList<CreateOrderResponse.Data.Order.OrderItem.OrderItemModifiers> =
                                                arrayListOf()
                                            if (it1.asJsonObject.has("modifiers")) {
                                                var modList =
                                                    it1.asJsonObject.get("modifiers").asJsonArray
                                                if (modList.size() != 0) {
                                                    modList.forEach {
                                                        listOfMod.add(
                                                            CreateOrderResponse.Data.Order.OrderItem.OrderItemModifiers(
                                                                id = it.asJsonObject.get("id").asInt,
                                                                orderItemId = it.asJsonObject.get("order_item_id").asInt,
                                                                name = it.asJsonObject.get("name").asString,
                                                                quantity = it.asJsonObject.get("quantity").asInt,
                                                                modifierSetId = 0,
                                                                isModifier = true,
                                                                modifierQuantity = it.asJsonObject.get(
                                                                    "modifier_quantity"
                                                                ).asInt,
                                                                createdAt = "",
                                                                updatedAt = "",
                                                                totalPrice = 0.0,
                                                                orderId = 0,
                                                                price = 0.0
                                                            )
                                                        )

                                                    }

                                                }


                                            }

                                            orderItems.add(
                                                CreateOrderResponse.Data.Order.OrderItem(
                                                    categoryId = it1.asJsonObject.get("category_id").asInt,
                                                    completedInKitchen = false,
                                                    discountType = "",
                                                    discountAmount = 0.0,
                                                    discountId = 0,
                                                    employeeId = 0,
                                                    float = 0.0,
                                                    id = it1.asJsonObject.get("order_item_id").asInt,
                                                    isPrinted = false,
                                                    isPaid = false,
                                                    itemId = it1.asJsonObject.get("item_id").asInt,
                                                    orderId = it.asJsonObject.get("id").asInt,
                                                    itemName = it1.asJsonObject.get("name").asString,
                                                    totalPrice = 0.0,
                                                    timestamp = it1.asJsonObject.get("message").asString,
                                                    quantity = it1.asJsonObject.get("quantity").asInt,
                                                    price = 0.0,
                                                    orderItemModifiers = listOfMod,
                                                    note = it1.asJsonObject.get("item_note").asString


                                                )
                                            )

                                        }

                                        modelOrder.orderItems = orderItems
                                        modelOrder.orderID =
                                            it.asJsonObject.get("id").asInt.toString()


                                    }


                                    listofPrinterOrders.add(modelOrder)
                                }
                            }


                        }
                        var modelPrinterParser = PrinterJSONElementData(
                            printerName = it.asJsonObject.get("printer_name").asString,
                            macAddress = it.asJsonObject.get("mac_address").asString,
                            ipAddress = it.asJsonObject.get("ip_address").asString,
                            modelName = it.asJsonObject.get("modal_name").asString,
                            printerQueueModelList = listofPrinterOrders,
                            portNo = it.asJsonObject.get("port_no").asInt

                        )

                        listOfPrintersData.add(modelPrinterParser)


                    }

                    /*Log.e(
                    TAG,
                    "checkFirstIndexdataAraay  ${listOfPrintersData.get(0).printerQueueModelList.size}"
                )*/

                    printerSize = listOfPrintersData.size
                    orderSize = listOfPrintersData.get(0).printerQueueModelList.size

                    if (orderSize == 0) {
                        listOfPrintersData.forEachIndexed { index, it ->
                            if (it.printerQueueModelList.isNotEmpty()) {
                                orderSize = it.printerQueueModelList.size
                                currentOrderIndex = 0
                                currentPrinterIndex = index
                                return@forEachIndexed
                            }

                        }
                    } else {

                        currentOrderIndex = 0
                        currentPrinterIndex = 0
                    }
                    if (listOfPrintersData.get(0).printerName.contains("CloudPrint_", true)) {
                        Log.e(TAG, "ActionCableContainSunmi")
                        if (orderSize != 0) {

                            sendDataToPrintToSunmi(
                                listOfPrintersData,
                                currentPrinterIndex,
                                printerObjList.get(listOfPrintersData[currentPrinterIndex].macAddress) as CloudPrinter?,
                                listOfPrintersData.get(currentPrinterIndex).printerQueueModelList,
                                listOfPrintersData[currentPrinterIndex].macAddress,
                                currentOrderIndex

                            )
                        } else {
                            delay(2000)
                            val params = JsonObject()
                            params.addProperty("id", locationId)
                            params.addProperty(
                                "url",
                                baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3
                            )
                            Log.e(
                                TAG,
                                "checkReuestURL: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                            )
                            subscription?.perform("received", params)
                        }


                    } else {


                        if (orderSize != 0) {
                            //  Log.e(TAG, "sendData1st:  ")
                            sendDataToPrint(
                                listOfPrintersData,
                                currentPrinterIndex,
                                printerObjList.get(listOfPrintersData[0].macAddress) as Printer?,
                                listOfPrintersData.get(0).printerQueueModelList,
                                listOfPrintersData[0].macAddress,
                                currentOrderIndex
                            )
                        } else if (listOfPrintersData.size - 1 > currentPrinterIndex) {
                            // Log.e(TAG, "sendData2nd:  ")
                            var isBreak = false
                            for (i in currentPrinterIndex++ until listOfPrintersData.size) {
                                if (listOfPrintersData.get(i).printerQueueModelList.isNotEmpty()) {

                                    isBreak = true
                                    currentPrinterIndex = i
                                    currentOrderIndex = 0
                                    sendDataToPrint(
                                        listOfPrintersData,
                                        currentPrinterIndex,
                                        printerObjList.get(listOfPrintersData[i].macAddress) as Printer?,
                                        listOfPrintersData.get(i).printerQueueModelList,
                                        listOfPrintersData[i].macAddress,
                                        currentOrderIndex
                                    )
                                    break
                                }
                            }
                            if (isBreak == false) {
                                delay(2000)
                                val params = JsonObject()
                                params.addProperty("id", locationId)
                                params.addProperty(
                                    "url",
                                    baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3
                                )
                                Log.e(
                                    TAG,
                                    "checkReuestURL: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                                )
                                subscription?.perform("received", params)
                            }


                        } else {

                            // Log.e(TAG, "sendData3rd:  ")
                            delay(2000)
                            val params = JsonObject()
                            params.addProperty("id", locationId)
                            params.addProperty(
                                "url",
                                baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3
                            )
                            Log.e(
                                TAG,
                                "checkReuestURL: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                            )
                            subscription?.perform("received", params)
                        }
                    }
                }

            } else {
                delay(2000)
                val params = JsonObject()
                params.addProperty("id", locationId)
                params.addProperty("url", baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3)
                Log.e(
                    TAG,
                    "checkReuestURL: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                )
                subscription?.perform("received", params)
            }


            //

            ///////////////////////////////////////PHASE 1 Code///////////////////////////////////////////////////////////
            /*if (dataList?.asJsonArray?.size() != 0) {


                *//*dataList.forEachIndexed { index, it ->*//*


                val obj = dataList.get(0).asJsonObject.get("order_data").asJsonObject

              *//*  if (obj.asJsonObject.has("order_items_attributes")) {
                    var itemArray = obj.asJsonObject.get("order_items_attributes").asJsonArray
                    var itemAttribute: ArrayList<CreateOrderResponse.Data.Order.OrderItem> =
                        arrayListOf()
                    var itemModifiers: ArrayList<CreateOrderResponse.Data.Order.OrderItem.OrderItemModifiers> =
                        arrayListOf()


                    itemArray.forEach {
                        if (it.asJsonObject.has("order_item_modifiers_attributes")) {
                            var modifiersList =
                                it.asJsonObject.get("order_item_modifiers_attributes").asJsonArray

                            if (modifiersList.size() != 0) {
                                modifiersList.forEach {
                                    val jsonObj = it.asJsonObject
                                    itemModifiers.add(
                                        CreateOrderResponse.Data.Order.OrderItem.OrderItemModifiers(
                                            name = jsonObj.get("name").asString,
                                            id = 0,
                                            orderItemId = 0,
                                            orderId = 0,
                                            quantity = jsonObj.get("quantity").asInt,
                                            price = 0.0,
                                            modifierSetId = 0,
                                            updatedAt = "",
                                            createdAt = "",
                                            totalPrice = 0.0,
                                            isModifier = false
                                        )
                                    )


                                }

                            }
                        }
                        var orderItem = CreateOrderResponse.Data.Order.OrderItem(
                            categoryId = it.asJsonObject.get("category_id").asInt,
                            completedInKitchen = false,
                            discountAmount = 0.0,
                            discountId = 0,
                            discountType = "",
                            employeeId = it.asJsonObject.get("employee_id").asInt,
                            float = 0.0,
                            id = 0,
                            isPaid = false,
                            isPrinted = it.asJsonObject.get("is_printed").asBoolean,
                            itemId = it.asJsonObject.get("item_id").asInt,
                            itemName = it.asJsonObject.get("item_name").asString,
                            note = it.asJsonObject.get("note").asString,
                            orderItemModifiers = itemModifiers,
                            price = it.asJsonObject.get("price").asDouble,
                            quantity = it.asJsonObject.get("quantity").asInt,
                            timestamp = "",
                            totalPrice = 0.0,
                            orderId = 0
                        )

                        itemAttribute.add(orderItem)


                    }
                    printerQueueModel.orderItems = itemAttribute
                    printerQueueModel.orderID = ""
                    printerQueueModel.orderType =
                        dataList.get(0).asJsonObject.get("order_type").asString
                    printerQueueModel.id = dataList.get(0).asJsonObject.get("id").asInt
                    printerQueueModel.offlineId = obj.asJsonObject.get("offline_id").asString
                    printerQueueModel.paymentType = "Cash"
                    printerQueueModel.status = Constants.PENDING
                    printerQueueModel.totalAmt = obj.asJsonObject.get("total_amount").asDouble
                    printerQueueModel.orderID =
                        "" + dataList.get(0).asJsonObject.get("orderid").asInt
                    //obj.asJsonObject.get("terminal_name")?.asString ?: ""
                    printerQueueModel.position = 0
                    var str = dataList.get(0).asJsonObject.getAsJsonArray("printer_list")
                    printerQueueModel.printSuccessData = Gson().toJson(str).toString()
                    Log.e(
                        TAG,
                        "checkPrinterListSucees  ${Gson().toJson(printerQueueModel.printSuccessData)}"
                    )
                    *//**//*if (it.asJsonObject.has("customer_data")) {
                    if (it.asJsonObject.get("customer_data").asJsonObject.has("first_name") && it.asJsonObject.get(
                            "customer_data"
                        ).asJsonObject.get("first_name").toString().isNotEmpty()
                    ) {
                        printerQueueModel.customerName =
                            it.asJsonObject.get("customer_data").asJsonObject.get("first_name")
                                .toString() + " " + it.asJsonObject.get("customer_data").asJsonObject.get(
                                "last_name"
                            ).toString()
                        if (it.asJsonObject.get("customer_data").asJsonObject.has("phone")) {
                            var phoneNo =
                                it.asJsonObject.get("customer_data").asJsonObject.get("phone").asJsonObject

                            if (phoneNo.has("phone_number") && phoneNo.get("phone_number")
                                    .toString().isNotEmpty()
                            ) {
                                printerQueueModel.customerPhoneNo =
                                    phoneNo.get("phone_number").toString()

                            }

                        }

                        if (it.asJsonObject.get("customer_data").asJsonObject.has("address")) {
                            var address =
                                it.asJsonObject.get("customer_data").asJsonObject.get("address").asJsonObject

                            if (address.has("address1") && address.get("address1")
                                    .toString().isNotEmpty()
                            ) {
                                printerQueueModel.customerPhoneNo = address.get("address1")
                                    .toString() + " " + address.get("address2")
                                    .toString() + " " + address.get("city")
                                    .toString() + " " + address.get("state")
                                    .toString() + " " + address.get("country")
                                    .toString() + " " + address.get("postcode").toString()

                            }

                        }
                    }


                }*//**//*


                }*//*

                val gson = Gson()
                val itemType = object : TypeToken<List<String>>() {}.type
                var printMACAddress =
                    gson.fromJson<List<String>>(printerQueueModel.printSuccessData, itemType)


                Log.e(TAG, "printMACAddress:  ${Gson().toJson(printMACAddress)}")
                var oneTimeInside: Boolean = false

                for (m in 0 until kitchenPrinterList.size) {

                    if (checkPrinterHasCatOrNot(
                            kitchenPrinterList[m],
                            printerQueueModel
                        ) && !(printMACAddress.contains(kitchenPrinterList[m].macAddress))
                    ) {


                        printerObjList.get(kitchenPrinterList.get(m).ipAddress)
                            ?.let {
                                oneTimeInside = true
                                Log.e(TAG, "checkPrinterObjNullCheck:  ")
                                callPrinter(it, printerQueueModel, kitchenPrinterList.get(m))
                            }
                    }

                    *//* if (m == kitchenPrinterList.size - 1 && oneTimeInside == false) {
                         delay(2000)
                         val params = JsonObject()
                         params.addProperty("id", locationId)
                         params.addProperty("url", baseUrl + Constants.CREATE_QUEUE_PRINTER)
                         subscription?.perform("received", params)

                     }*//*

                }


            } else {
                runBlocking {
                    delay(5000)
                    Log.e(
                        TAG,
                        "requestUrl:  ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3} locationID ${locationId}"
                    )
                    val params = JsonObject()
                    params.addProperty("id", locationId)
                    params.addProperty("url", baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3)
                    subscription?.perform("received", params)
                }
            }*/
            /////////////////////////////////PHASE 1 Code///////////////////////////////////////////////////////////


            /*if (printerQueuelist.isNotEmpty()) {
                val printer = Printer(Printer.TM_U220, Printer.MODEL_ANK, mContext)
                callPrinter(printer, printerQueueModel)


                *//*for (i in 0 until printerQueuelist.size) {*//*

            }*/
        } else {

            runBlocking {
                delay(5000)
                val params = JsonObject()
                params.addProperty("id", locationId)
                params.addProperty("url", baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3)
                subscription?.perform("received", params)
            }


            /*   if (printerQueuelist.size != 0) {
                   withContext(Dispatchers.Default) {

                       val intent = Intent()
                       intent.putExtra(Constants.DATA, Gson().toJson(printerQueuelist))
                       intent.action = PRINTER_QUEUE_DATA_RECEIVED
                       mContext.sendBroadcast(intent)
                   }


                   *//* configurePrinter(
                     printerQueuelist.get(printerQueuelist.size - 1),
                     printerQueuelist.size - 1
                 )*//*

            } else {
                isPrinterRunning = false
            }*/

            /*if (!printerBGRunning) {
              //  delay(2000)

                getQueueLocalData()
            }*/

        } /*else {
            runBlocking {
                delay(5000)
                val params = JsonObject()
                params.addProperty("id", locationId)
                params.addProperty("url", baseUrl + Constants.CREATE_QUEUE_PRINTER)
                subscription?.perform("received", params)
            }
            *//* val intent = Intent()
             intent.putExtra(Constants.DATA, "")
             intent.action = PRINTER_QUEUE_DATA_RECEIVED
             mContext.sendBroadcast(intent)
             isPrinterRunning = false*//*
        }
*/
    }

    private fun sendDataToPrintToSunmi(
        listOfPrintersData: ArrayList<PrinterJSONElementData>,
        currentPrinterIndexF: Int,
        cloudPrinter: CloudPrinter?,
        printerQueueModelList: ArrayList<PrinterQueueModel>,
        macAddress: String,
        currentOrderIndexF: Int
    ) {
        Log.e(
            TAG,
            "checkSunmi Called  ${cloudPrinter?.isConnected}  check Name  ${
                listOfPrintersData.get(currentPrinterIndexF).printerName
            } checkQueueRunning  ${isQueueRunning}"
        )
        if (isQueueRunning == false) {

            if (cloudPrinter?.isConnected == false) {

                cloudPrinter.connect(mContext, object : ConnectCallback {
                    override fun onConnect() {
                        sendDataToCloudPrint(
                            listOfPrintersData,
                            currentPrinterIndexF,
                            cloudPrinter,
                            printerQueueModelList,
                            macAddress,
                            currentOrderIndexF
                        )

                    }

                    override fun onFailed(p0: String?) {
                        Log.e(TAG, "checkFailed")

                        isQueueRunning = false
                        if (listOfPrintersData.size - 1 == currentPrinterIndex) {
                            sendNotification("Printer - ${listOfPrintersData[currentPrinterIndex].modelName} is Offline.")

                            /*  Log.e(
                                  TAG,
                                  "listOfPrinerData:   ${listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.size}"
                              )
                              Log.e(TAG, "listOfcurrentOrderIndex:   ${currentOrderIndex}")
                  */


                            if (listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.size - 1 == currentOrderIndex) {
                                // Log.e(TAG, "checkLastORderPRint  ")
                                runBlocking {
                                    delay(3000)

                                    val params = JsonObject()
                                    params.addProperty("id", locationId)
                                    params.addProperty(
                                        "url",
                                        baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3
                                    )
                                    Log.e(
                                        TAG,
                                        "checkReuestURL: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                                    )
                                    subscription?.perform("received", params)
                                }

                            } else {

                                runBlocking {
                                    currentOrderIndex = currentOrderIndex + 1
                                    if (listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.isNotEmpty()) {

                                        sendDataToPrintToSunmi(
                                            listOfPrintersData,
                                            currentPrinterIndex,
                                            printerObjList.get(
                                                listOfPrintersData.get(
                                                    currentPrinterIndex
                                                ).macAddress
                                            ) as CloudPrinter?,
                                            listOfPrintersData.get(currentPrinterIndex).printerQueueModelList,
                                            listOfPrintersData.get(currentPrinterIndex).macAddress,
                                            currentOrderIndex

                                        )
                                    } else {
                                        runBlocking {
                                            delay(3000)
                                            val params = JsonObject()
                                            params.addProperty("id", locationId)
                                            params.addProperty(
                                                "url",
                                                baseUrl + CREATE_QUEUE_PRINTER_PHASE3
                                            )
                                            Log.e(
                                                TAG,
                                                "checkReuestURL: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                                            )
                                            subscription?.perform("received", params)
                                        }


                                    }
                                }
                            }

                        } else if (listOfPrintersData.size - 1 > currentPrinterIndex) {
                            sendNotification("Printer - ${listOfPrintersData[currentPrinterIndex].modelName} is Offline.")
                            /* Log.e(
                                 TAG,
                                 "checkLog: ${currentPrinterIndex}  orderIndex: ${
                                     listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.size - 1
                                 }  currentOrderInd: ${currentOrderIndex}"
                             )*/
                            if (listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.size - 1 == currentOrderIndex) {


                                currentOrderIndex = 0
                                currentPrinterIndex = currentPrinterIndex + 1
                                if (listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.isNotEmpty()) {
                                    runBlocking {
                                        sendDataToPrintToSunmi(
                                            listOfPrintersData,
                                            currentPrinterIndex,
                                            printerObjList.get(
                                                listOfPrintersData.get(
                                                    currentPrinterIndex
                                                ).macAddress
                                            ) as CloudPrinter?,
                                            listOfPrintersData.get(currentPrinterIndex).printerQueueModelList,
                                            listOfPrintersData.get(currentPrinterIndex).macAddress,
                                            currentOrderIndex

                                        )
                                    }
                                } else {
                                    runBlocking {
                                        delay(3000)
                                        val params = JsonObject()
                                        params.addProperty("id", locationId)
                                        params.addProperty(
                                            "url",
                                            baseUrl + CREATE_QUEUE_PRINTER_PHASE3
                                        )
                                        Log.e(
                                            TAG,
                                            "checkReuestURL: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                                        )
                                        subscription?.perform("received", params)
                                    }
                                }

                            } else {

                                currentOrderIndex = currentOrderIndex + 1
                                runBlocking {
                                    sendDataToPrintToSunmi(
                                        listOfPrintersData,
                                        currentPrinterIndex,
                                        printerObjList.get(
                                            listOfPrintersData.get(
                                                currentPrinterIndex
                                            ).macAddress
                                        ) as CloudPrinter?,
                                        listOfPrintersData.get(currentPrinterIndex).printerQueueModelList,
                                        listOfPrintersData.get(currentPrinterIndex).macAddress,
                                        currentOrderIndex

                                    )
                                }

                            }

                        } else {
                            runBlocking {
                                delay(3000)
                                val params = JsonObject()
                                params.addProperty("id", locationId)
                                params.addProperty("url", baseUrl + CREATE_QUEUE_PRINTER_PHASE3)
                                Log.e(
                                    TAG,
                                    "checkReuestURL: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                                )
                                subscription?.perform("received", params)
                            }

                        }

                    }

                    override fun onDisConnect() {
                        Log.e(TAG, "checkDisconnect")
                        isQueueRunning = false
                        if (listOfPrintersData.size - 1 == currentPrinterIndex) {
                            /*  Log.e(
                                  TAG,
                                  "listOfPrinerData:   ${listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.size}"
                              )
                              Log.e(TAG, "listOfcurrentOrderIndex:   ${currentOrderIndex}")
                  */


                            if (listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.size - 1 == currentOrderIndex) {
                                // Log.e(TAG, "checkLastORderPRint  ")

                                val params = JsonObject()
                                params.addProperty("id", locationId)
                                params.addProperty(
                                    "url",
                                    baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3
                                )
                                Log.e(
                                    TAG,
                                    "checkReuestURL: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                                )
                                subscription?.perform("received", params)


                            } else {


                                currentOrderIndex = currentOrderIndex + 1
                                if (listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.isNotEmpty()) {

                                    sendDataToPrintToSunmi(
                                        listOfPrintersData,
                                        currentPrinterIndex,
                                        printerObjList.get(
                                            listOfPrintersData.get(
                                                currentPrinterIndex
                                            ).macAddress
                                        ) as CloudPrinter?,
                                        listOfPrintersData.get(currentPrinterIndex).printerQueueModelList,
                                        listOfPrintersData.get(currentPrinterIndex).macAddress,
                                        currentOrderIndex

                                    )
                                } else {
                                    runBlocking {
                                        delay(3000)
                                        val params = JsonObject()
                                        params.addProperty("id", locationId)
                                        params.addProperty(
                                            "url",
                                            baseUrl + CREATE_QUEUE_PRINTER_PHASE3
                                        )
                                        Log.e(
                                            TAG,
                                            "checkReuestURL: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                                        )
                                        subscription?.perform("received", params)
                                    }


                                }

                            }

                        } else if (listOfPrintersData.size - 1 > currentPrinterIndex) {
                            /* Log.e(
                                 TAG,
                                 "checkLog: ${currentPrinterIndex}  orderIndex: ${
                                     listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.size - 1
                                 }  currentOrderInd: ${currentOrderIndex}"
                             )*/
                            if (listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.size - 1 == currentOrderIndex) {


                                currentOrderIndex = 0
                                currentPrinterIndex = currentPrinterIndex + 1
                                if (listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.isNotEmpty()) {

                                    sendDataToPrintToSunmi(
                                        listOfPrintersData,
                                        currentPrinterIndex,
                                        printerObjList.get(
                                            listOfPrintersData.get(
                                                currentPrinterIndex
                                            ).macAddress
                                        ) as CloudPrinter?,
                                        listOfPrintersData.get(currentPrinterIndex).printerQueueModelList,
                                        listOfPrintersData.get(currentPrinterIndex).macAddress,
                                        currentOrderIndex

                                    )

                                } else {

                                    //   delay(3000)
                                    val params = JsonObject()
                                    params.addProperty("id", locationId)
                                    params.addProperty(
                                        "url",
                                        baseUrl + CREATE_QUEUE_PRINTER_PHASE3
                                    )
                                    Log.e(
                                        TAG,
                                        "checkReuestURL: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                                    )
                                    subscription?.perform("received", params)
                                }


                            } else {

                                currentOrderIndex = currentOrderIndex + 1

                                sendDataToPrintToSunmi(
                                    listOfPrintersData,
                                    currentPrinterIndex,
                                    printerObjList.get(
                                        listOfPrintersData.get(
                                            currentPrinterIndex
                                        ).macAddress
                                    ) as CloudPrinter?,
                                    listOfPrintersData.get(currentPrinterIndex).printerQueueModelList,
                                    listOfPrintersData.get(currentPrinterIndex).macAddress,
                                    currentOrderIndex

                                )


                            }

                        } else {


                            val params = JsonObject()
                            params.addProperty("id", locationId)
                            params.addProperty("url", baseUrl + CREATE_QUEUE_PRINTER_PHASE3)
                            Log.e(
                                TAG,
                                "checkReuestURL: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                            )
                            subscription?.perform("received", params)
                        }


                    }

                })
            } else if (cloudPrinter == null) {

                Log.e(TAG, "checkCLoudObjNull")
                var cloudPrinter: CloudPrinter = createCloudPrinterWithName(
                    listOfPrintersData.get(currentPrinterIndexF).printerName,
                    macAddress,
                    listOfPrintersData.get(currentPrinterIndexF).portNo
                )


                cloudPrinter.connect(mContext, object : ConnectCallback {
                    override fun onConnect() {
                        Log.e(TAG, "CheckConnectDone 2nd")
                        printerObjList.put(
                            listOfPrintersData.get(currentPrinterIndexF).macAddress,
                            cloudPrinter
                        )

                        sendDataToCloudPrint(
                            listOfPrintersData,
                            currentPrinterIndexF,
                            cloudPrinter,
                            printerQueueModelList,
                            macAddress,
                            currentOrderIndexF
                        )

                    }

                    override fun onFailed(p0: String?) {
                        Log.e(TAG, "checkFailed")

                        if (listOfPrintersData.size - 1 == currentPrinterIndex) {
                            sendNotification("Printer - ${listOfPrintersData[currentPrinterIndex].modelName} is Offline.")
                            /*  Log.e(
                                  TAG,
                                  "listOfPrinerData:   ${listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.size}"
                              )
                              Log.e(TAG, "listOfcurrentOrderIndex:   ${currentOrderIndex}")
                  */


                            if (listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.size - 1 == currentOrderIndex) {
                                // Log.e(TAG, "checkLastORderPRint  ")
                                runBlocking {
                                    delay(3000)

                                    val params = JsonObject()
                                    params.addProperty("id", locationId)
                                    params.addProperty(
                                        "url",
                                        baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3
                                    )
                                    Log.e(
                                        TAG,
                                        "checkReuestURL: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                                    )
                                    subscription?.perform("received", params)
                                }

                            } else {

                                runBlocking {
                                    currentOrderIndex = currentOrderIndex + 1
                                    if (listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.isNotEmpty()) {

                                        sendDataToPrintToSunmi(
                                            listOfPrintersData,
                                            currentPrinterIndex,
                                            printerObjList.get(
                                                listOfPrintersData.get(
                                                    currentPrinterIndex
                                                ).macAddress
                                            ) as CloudPrinter?,
                                            listOfPrintersData.get(currentPrinterIndex).printerQueueModelList,
                                            listOfPrintersData.get(currentPrinterIndex).macAddress,
                                            currentOrderIndex

                                        )
                                    } else {
                                        runBlocking {
                                            delay(3000)
                                            val params = JsonObject()
                                            params.addProperty("id", locationId)
                                            params.addProperty(
                                                "url",
                                                baseUrl + CREATE_QUEUE_PRINTER_PHASE3
                                            )
                                            Log.e(
                                                TAG,
                                                "checkReuestURL: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                                            )
                                            subscription?.perform("received", params)
                                        }


                                    }
                                }
                            }

                        } else if (listOfPrintersData.size - 1 > currentPrinterIndex) {
                            sendNotification("Printer - ${listOfPrintersData[currentPrinterIndex].modelName} is Offline.")
                            /* Log.e(
                                 TAG,
                                 "checkLog: ${currentPrinterIndex}  orderIndex: ${
                                     listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.size - 1
                                 }  currentOrderInd: ${currentOrderIndex}"
                             )*/
                            if (listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.size - 1 == currentOrderIndex) {


                                currentOrderIndex = 0
                                currentPrinterIndex = currentPrinterIndex + 1
                                if (listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.isNotEmpty()) {
                                    runBlocking {
                                        sendDataToPrintToSunmi(
                                            listOfPrintersData,
                                            currentPrinterIndex,
                                            printerObjList.get(
                                                listOfPrintersData.get(
                                                    currentPrinterIndex
                                                ).macAddress
                                            ) as CloudPrinter?,
                                            listOfPrintersData.get(currentPrinterIndex).printerQueueModelList,
                                            listOfPrintersData.get(currentPrinterIndex).macAddress,
                                            currentOrderIndex

                                        )
                                    }
                                } else {
                                    runBlocking {
                                        delay(3000)
                                        val params = JsonObject()
                                        params.addProperty("id", locationId)
                                        params.addProperty(
                                            "url",
                                            baseUrl + CREATE_QUEUE_PRINTER_PHASE3
                                        )
                                        Log.e(
                                            TAG,
                                            "checkReuestURL: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                                        )
                                        subscription?.perform("received", params)
                                    }
                                }

                            } else {

                                currentOrderIndex = currentOrderIndex + 1
                                runBlocking {
                                    sendDataToPrintToSunmi(
                                        listOfPrintersData,
                                        currentPrinterIndex,
                                        printerObjList.get(
                                            listOfPrintersData.get(
                                                currentPrinterIndex
                                            ).macAddress
                                        ) as CloudPrinter?,
                                        listOfPrintersData.get(currentPrinterIndex).printerQueueModelList,
                                        listOfPrintersData.get(currentPrinterIndex).macAddress,
                                        currentOrderIndex

                                    )
                                }

                            }

                        } else {
                            runBlocking {
                                delay(3000)
                                val params = JsonObject()
                                params.addProperty("id", locationId)
                                params.addProperty("url", baseUrl + CREATE_QUEUE_PRINTER_PHASE3)
                                Log.e(
                                    TAG,
                                    "checkReuestURL: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                                )
                                subscription?.perform("received", params)
                            }

                        }

                    }

                    override fun onDisConnect() {
                        Log.e(TAG, "checkDisconnect")
                        if (listOfPrintersData.size - 1 == currentPrinterIndex) {
                            /*  Log.e(
                                  TAG,
                                  "listOfPrinerData:   ${listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.size}"
                              )
                              Log.e(TAG, "listOfcurrentOrderIndex:   ${currentOrderIndex}")
                  */


                            if (listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.size - 1 == currentOrderIndex) {
                                // Log.e(TAG, "checkLastORderPRint  ")
                                runBlocking {
                                    delay(3000)

                                    val params = JsonObject()
                                    params.addProperty("id", locationId)
                                    params.addProperty(
                                        "url",
                                        baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3
                                    )
                                    Log.e(
                                        TAG,
                                        "checkReuestURL: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                                    )
                                    subscription?.perform("received", params)
                                }

                            } else {

                                runBlocking {
                                    currentOrderIndex = currentOrderIndex + 1
                                    if (listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.isNotEmpty()) {

                                        sendDataToPrintToSunmi(
                                            listOfPrintersData,
                                            currentPrinterIndex,
                                            printerObjList.get(
                                                listOfPrintersData.get(
                                                    currentPrinterIndex
                                                ).macAddress
                                            ) as CloudPrinter?,
                                            listOfPrintersData.get(currentPrinterIndex).printerQueueModelList,
                                            listOfPrintersData.get(currentPrinterIndex).macAddress,
                                            currentOrderIndex

                                        )
                                    } else {
                                        runBlocking {
                                            delay(3000)
                                            val params = JsonObject()
                                            params.addProperty("id", locationId)
                                            params.addProperty(
                                                "url",
                                                baseUrl + CREATE_QUEUE_PRINTER_PHASE3
                                            )
                                            Log.e(
                                                TAG,
                                                "checkReuestURL: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                                            )
                                            subscription?.perform("received", params)
                                        }


                                    }
                                }
                            }

                        } else if (listOfPrintersData.size - 1 > currentPrinterIndex) {
                            /* Log.e(
                                 TAG,
                                 "checkLog: ${currentPrinterIndex}  orderIndex: ${
                                     listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.size - 1
                                 }  currentOrderInd: ${currentOrderIndex}"
                             )*/
                            if (listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.size - 1 == currentOrderIndex) {


                                currentOrderIndex = 0
                                currentPrinterIndex = currentPrinterIndex + 1
                                if (listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.isNotEmpty()) {
                                    runBlocking {
                                        sendDataToPrintToSunmi(
                                            listOfPrintersData,
                                            currentPrinterIndex,
                                            printerObjList.get(
                                                listOfPrintersData.get(
                                                    currentPrinterIndex
                                                ).macAddress
                                            ) as CloudPrinter?,
                                            listOfPrintersData.get(currentPrinterIndex).printerQueueModelList,
                                            listOfPrintersData.get(currentPrinterIndex).macAddress,
                                            currentOrderIndex

                                        )
                                    }
                                } else {
                                    runBlocking {
                                        delay(3000)
                                        val params = JsonObject()
                                        params.addProperty("id", locationId)
                                        params.addProperty(
                                            "url",
                                            baseUrl + CREATE_QUEUE_PRINTER_PHASE3
                                        )
                                        Log.e(
                                            TAG,
                                            "checkReuestURL: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                                        )
                                        subscription?.perform("received", params)
                                    }
                                }

                            } else {

                                currentOrderIndex = currentOrderIndex + 1
                                runBlocking {
                                    sendDataToPrintToSunmi(
                                        listOfPrintersData,
                                        currentPrinterIndex,
                                        printerObjList.get(
                                            listOfPrintersData.get(
                                                currentPrinterIndex
                                            ).macAddress
                                        ) as CloudPrinter?,
                                        listOfPrintersData.get(currentPrinterIndex).printerQueueModelList,
                                        listOfPrintersData.get(currentPrinterIndex).macAddress,
                                        currentOrderIndex

                                    )
                                }

                            }

                        } else {
                            runBlocking {
                                delay(3000)
                                val params = JsonObject()
                                params.addProperty("id", locationId)
                                params.addProperty("url", baseUrl + CREATE_QUEUE_PRINTER_PHASE3)
                                Log.e(
                                    TAG,
                                    "checkReuestURL: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                                )
                                subscription?.perform("received", params)
                            }

                        }
                    }

                })

            } else {
                Log.e(TAG, "checkLastElse ")

                sendDataToCloudPrint(
                    listOfPrintersData,
                    currentPrinterIndexF,
                    cloudPrinter,
                    printerQueueModelList,
                    macAddress,
                    currentOrderIndexF
                )
                /* cloudPrinter?.printText("Test Print")
                 cloudPrinter?.lineFeed(2)
                 cloudPrinter?.cutPaper(true)
                 cloudPrinter?.commitTransBuffer(this@UploadWorker)*/
            }
        } else {


            //delay(3000)
            val params = JsonObject()
            params.addProperty("id", locationId)
            params.addProperty("url", baseUrl + CREATE_QUEUE_PRINTER_PHASE3)
            Log.e(
                TAG,
                "checkReuestURL: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
            )
            subscription?.perform("received", params)

        }

    }

    private fun sendDataToCloudPrint(
        listOfPrintersData: ArrayList<PrinterJSONElementData>,
        currentPrinterIndex: Int,
        cloudPrinter: CloudPrinter?,
        printerQueueModelList: ArrayList<PrinterQueueModel>,
        macAddress: String,
        currentOrderIndex: Int
    ) {
        isQueueRunning = true
        var obj = printerQueueModelList.get(currentOrderIndex)
        cloudPrinter?.lineFeed(1)
        cloudPrinter?.setUnderlineMode(UnderlineStyle.EMPTY)
        cloudPrinter?.setBoldMode(true)
        cloudPrinter?.setAlignment(AlignStyle.CENTER)
        cloudPrinter?.setCharacterSize(2, 2)
        cloudPrinter?.printText("Order ID:" + obj.orderID)

        cloudPrinter?.lineFeed(1)

        cloudPrinter?.setAlignment(AlignStyle.CENTER)
        cloudPrinter?.printText(obj.orderType)

        cloudPrinter?.lineFeed(1)
        cloudPrinter?.setBoldMode(false)
        cloudPrinter?.setCharacterSize(1, 1)
        cloudPrinter?.setAlignment(AlignStyle.LEFT)
        cloudPrinter?.printText("Employee:" + obj.employeeName)

        cloudPrinter?.setCharacterSize(1, 1)

        cloudPrinter?.setAlignment(AlignStyle.LEFT)
        cloudPrinter?.printText(obj.dateAndTime)


        cloudPrinter?.let { addDoubleDotLineForSunmiQueue(it) }

        if (obj.orderType == DINE_IN) {

            cloudPrinter?.let { printGuestByItemForSunmiQueue(obj.guestAttributes, it) }


        } else {

            for (i in 0 until obj.orderItems.size) {

                cloudPrinter?.setBoldMode(false)
                cloudPrinter?.setCharacterSize(2, 2)
                cloudPrinter?.setAlignment(AlignStyle.LEFT)

                if (obj.orderItems[i].timestamp.isNotEmpty()) {
                    var msg = "(" + obj.orderItems[i].timestamp + ")"
                    cloudPrinter?.printText("" + obj.orderItems[i].quantity + " " + obj.orderItems[i].itemName + "  " + msg)

                } else {

                    cloudPrinter?.printText("" + obj.orderItems[i].quantity + " " + obj.orderItems[i].itemName)
                }

                if (obj.orderItems[i].orderItemModifiers.isNotEmpty()) {
                    obj.orderItems[i].orderItemModifiers.forEach { mod ->


                        cloudPrinter?.printText(
                            "  " + if (mod.modifierQuantity == 1) {
                                "   "
                            } else {
                                "" + mod.modifierQuantity + "x "
                            } + mod.name
                        )


                    }


                }
            }
        }

        if (obj.orderType != DINE_IN && obj.customerName != null && obj.customerName.isNotEmpty()) {


        }


        cloudPrinter?.lineFeed(3)
        cloudPrinter?.cutPaper(true)

        Log.e(
            TAG,
            "getTransaction  " + Gson().toJson(cloudPrinter?.cloudPrinterInfo) + "isCloudConnected:  ${cloudPrinter?.isConnected}"
        )
        cloudPrinter?.getDeviceState(object : StatusCallback {
            override fun onResult(p0: CloudPrinterStatus?) {
                Log.e(TAG, "cloudPrinterStatus:   ${Gson().toJson(p0)}")
                if (p0?.name.equals("OUT_PAPER", true) || p0?.name.equals(
                        "UNKNOWN",
                        true
                    ) || p0?.name.equals("COVER", true)
                ) {
                    cloudPrinter.clearTransBuffer()
                    cloudPrinter.release(mContext)
                    isQueueRunning = false
                } else {
                    try {
                        cloudPrinter.commitTransBuffer(
                            this@UploadWorker
                        )
                    }catch (e:Exception){
                        e.printStackTrace()
                    }
                }

            }

        })


    }

    private suspend fun sendDataToPrint(
        listOfPrintersData: ArrayList<PrinterJSONElementData>,
        currentPrinterIndex: Int,
        printerObj: Printer?,
        printerQueueModelList: ArrayList<PrinterQueueModel>,
        macAddress: String,
        orderIndex: Int
    ) {
        Log.e(TAG, "checkPrinterQueue  ${printerQueueModelList.size}")
        if (isQueueRunning == false) {
            isQueueRunning = true

            printerObj?.let { callPrinter(it, printerQueueModelList.get(orderIndex), macAddress) }
        }

    }

    private fun checkPrinterHasCatOrNot(
        kitchenReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        printerQueueModel: PrinterQueueModel
    ): Boolean {

        var itemCategoriesId: ArrayList<Int> = arrayListOf()
        var printCategoriesId: ArrayList<Int> = arrayListOf()

        printerQueueModel.orderItems.forEach {
            itemCategoriesId.add(it.categoryId)
        }

        var filterList =
            kitchenReceiptPrinters.printerCategories.filter { it.printerEnable == true }

        filterList.forEach {
            printCategoriesId.add(it.id)
        }


        var fList = printCategoriesId.distinctBy { itemCategoriesId }
        Log.e(TAG, "fListSize:   ${fList.size}")

        if (printCategoriesId.containsAll(itemCategoriesId)) {
            Log.e(TAG, "categoryCheckYES")
            return true
        } else {
            Log.e(TAG, "categoryCheckNO")
            return false
        }

    }

    suspend fun callPrinter(
        printer: Printer,
        printerQueueModel: PrinterQueueModel,
        macAddress: String
    ) {
        try {
            var obj = printerQueueModel


            printerBreak = false
            var printerAdd =
                "TCP:" + macAddress


            printer.setStatusChangeEventListener(this)
            printer.setReceiveEventListener(this)

            //Log.e(TAG, "connection: ${printer.status.connection}")

            try {
                if (printer.status.connection == 0) {
                    printer.connect(
                        printerAdd,
                        Printer.PARAM_DEFAULT
                    )

                }

            } catch (e: Exception) {

                printerBreak = true
                sendNotification("Printer - ${listOfPrintersData[currentPrinterIndex].modelName} is Offline.")

                /* try {
                     // printer?.disconnect()
                     delay(1000)
                     printer.connect(
                         printerAdd,
                         Printer.PARAM_DEFAULT
                     )
                     printer.startMonitor()
                 } catch (e: Exception) {

                     e.printStackTrace()

                 }
 */
                e.printStackTrace()


            }

            Log.e(TAG, "checkprinterBreak: ${printerBreak}")
            if (printerBreak) {
                isQueueRunning = false

                try {

                    printer.disconnect()

                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }

                if (listOfPrintersData.size - 1 != currentPrinterIndex) {
                    /* Log.e(
                         TAG,
                         "checkLog: ${currentPrinterIndex}  orderIndex: ${
                             listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.size - 1
                         }  currentOrderInd: ${currentOrderIndex}"
                     )*/

                    currentOrderIndex = 0
                    currentPrinterIndex = currentPrinterIndex + 1

                    var isBreakDown = false

                    for (k in currentPrinterIndex until listOfPrintersData.size) {
                        if (listOfPrintersData[k].printerQueueModelList.isNotEmpty()) {

                            currentPrinterIndex = k
                            currentOrderIndex = 0
                            isBreakDown = true

                            //  Log.e(TAG,"checkInsideBreak: printerIndex: ${currentPrinterIndex}  orderIndex: ${currentOrderIndex}")

                            sendDataToPrint(
                                listOfPrintersData,
                                currentPrinterIndex,
                                printerObjList.get(listOfPrintersData.get(currentPrinterIndex).macAddress) as Printer?,
                                listOfPrintersData.get(currentPrinterIndex).printerQueueModelList,
                                listOfPrintersData.get(currentPrinterIndex).macAddress,
                                currentOrderIndex

                            )

                            break

                        }


                    }

                    Log.e(TAG, "checkIsBreak:   ${isBreakDown}")

                    if (isBreakDown == false) {
                        runBlocking {
                            delay(3000)
                            val params = JsonObject()
                            params.addProperty("id", locationId)
                            params.addProperty("url", baseUrl + CREATE_QUEUE_PRINTER_PHASE3)
                            subscription?.perform("received", params)
                        }

                    }


                } else {
                    runBlocking {
                        delay(3000)
                        val params = JsonObject()
                        params.addProperty("id", locationId)
                        params.addProperty("url", baseUrl + CREATE_QUEUE_PRINTER_PHASE3)
                        subscription?.perform("received", params)
                    }

                }


            } else {
                try {
                    if (printer.status.connection == 0) {
                        printer.interval = 1000
                        printer.startMonitor()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                printer.addFeedUnit(30)
                printer.addFeedLine(2)

                printer.addTextFont(Builder.FONT_E)
                printer.addTextAlign(Builder.ALIGN_CENTER)
                printer.addTextLang(Builder.LANG_EN)
                printer.addTextSize(2, 2)
                printer.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                printer.addText("OrderID:" + obj.orderID)


                printer.addFeedLine(1)
                printer.addTextFont(Builder.FONT_E)
                printer.addTextAlign(Builder.ALIGN_CENTER)
                printer.addTextLang(Builder.LANG_EN)
                printer.addTextSize(2, 2)
                printer.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                printer.addText(obj.orderType)

                printer.addFeedUnit(30)
                printer.addFeedLine(1)

                printer.addTextFont(Builder.FONT_E)
                printer.addTextAlign(Builder.ALIGN_LEFT)
                printer.addTextLang(Builder.LANG_EN)
                printer.addTextSize(1, 1)
                printer.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                printer.addText("Employee:" + obj.employeeName)

                printer.addFeedLine(1)

                printer.addTextFont(Builder.FONT_E)
                printer.addTextAlign(Builder.ALIGN_LEFT)
                printer.addTextLang(Builder.LANG_EN)
                printer.addTextSize(1, 1)
                printer.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )


                printer.addText(obj.dateAndTime)
                printer.addFeedLine(1)
                addHorizontalLineNewU220(printer)

                if (obj.orderType == DINE_IN) {

                    printGuestByItemForQueue(obj.guestAttributes, printer)


                } else {


                    for (m in 0 until obj.orderItems.size) {
                        printer.addFeedLine(1)
                        printer.addTextFont(Builder.FONT_E)
                        printer.addTextAlign(Builder.ALIGN_LEFT)
                        printer.addTextLang(Builder.LANG_EN)
                        printer.addTextSize(1, 1)
                        printer.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.TRUE,
                            Builder.COLOR_1
                        )

                        if (obj.orderItems[m].timestamp.isNotEmpty()) {
                            var msg = "(" + obj.orderItems[m].timestamp + ")"
                            printer.addText("" + obj.orderItems[m].quantity + " " + obj.orderItems[m].itemName + "  " + msg)

                        } else {

                            printer.addText("" + obj.orderItems[m].quantity + " " + obj.orderItems[m].itemName)
                        }

                        if (obj.orderItems[m].orderItemModifiers.isNotEmpty()) {
                            obj.orderItems[m].orderItemModifiers.forEach { mod ->

                                printer.addFeedLine(1)
                                printer.addTextFont(Builder.FONT_E)
                                printer.addTextAlign(Builder.ALIGN_LEFT)
                                printer.addTextLang(Builder.LANG_EN)
                                printer.addTextSize(1, 1)
                                printer.addTextStyle(
                                    Builder.FALSE,
                                    Builder.FALSE,
                                    Builder.TRUE,
                                    Builder.COLOR_1
                                )

                                printer.addText(
                                    "  " + if (mod.modifierQuantity == 1) {
                                        "   "
                                    } else {
                                        "" + mod.modifierQuantity + "x "
                                    } + mod.name
                                )


                            }


                        }

                    }

                }
                printer.addFeedLine(1)

                printer.addCut(Builder.CUT_FEED)


                // printer.beginTransaction()


                if (printer.status.connection == 1) {
                    printer.sendData(Printer.PARAM_DEFAULT)
                }

            }


        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

    }


    private fun newKitchenPrinterInit(
        printerQueueModel: PrinterQueueModel,
        index: Int,
        arrayItems: ArrayList<PrinterQueueModel>
    ) {
        LogUtil.logE(TAG, "kitchenPrinters  ${kitchenPrinterList.size}")
        for (i in 0 until kitchenPrinterList.size) {
            var modelName = -1
            if (kitchenPrinterList[i].modalName.equals("TM-M30", true)) {
                modelName = Printer.TM_M30
            } else if (kitchenPrinterList[i].modalName.equals("TM-U220", true)) {
                modelName = Printer.TM_U220
            }

            LogUtil.logE(TAG, "modelName  ${modelName}")
            if (modelName != -1) {

                val mPrinter =
                    com.epson.epos2.printer.Printer(modelName, Printer.MODEL_ANK, mContext)

                /*mPrinter.setReceiveEventListener { printarrayItemser, i, printerStatusInfo, s ->

                    LogUtil.logE(
                        "PrinterDataCh",
                        "   int: ${i}  printerInfo: ${
                            Gson().toJson(printerStatusInfo)
                        }  string: ${s}"
                    )
                }*/

                mPrinter.setReceiveEventListener(object : ReceiveListener {
                    override fun onPtrReceive(
                        p0: Printer?,
                        p1: Int,
                        p2: PrinterStatusInfo?,
                        p3: String?
                    ) {
                        LogUtil.logE(TAG, "online ${Gson().toJson(p2)}  data${p3}")
                        try {

                            printerQueueModel.id?.let {
                                val params = JsonObject()
                                var deleteUrl =
                                    baseUrl + Constants.CREATE_QUEUE_PRINTER + "/" + it
                                LogUtil.logE(TAG, "DeleteUrl ${deleteUrl}")
                                params.addProperty("url", deleteUrl)
                                subscription?.perform("delete_order", params)
                            }
                            mPrinter.endTransaction()
                            mPrinter.disconnect()
                            printerBGRunning = false
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                        }

                        val intent = Intent()
                        /* printerQueueModel.printSuccessData.toCollection(arrayListOf())
                             .add(kitchenPrinterList[i].id)*/
                        intent.putExtra(Constants.DATA, Gson().toJson(printerQueueModel))

                        intent.action = Constants.PRITNER_QUEUE_DATA_DELETE
                        mContext.sendBroadcast(intent)
                        arrayItems.removeAt(index)
                        if (arrayItems.isNotEmpty()) {
                            newKitchenPrinterInit(
                                arrayItems.get(arrayItems.size - 1),
                                arrayItems.size - 1,
                                arrayItems
                            )
                        }

                    }

                })

                var containsFlag: Boolean = true
                if (printerQueueModel.printSuccessData.isNotEmpty()) {
                    /*if (printerQueueModel.printSuccessData.contains(kitchenPrinterList[i].id)) {
                        containsFlag = true
                    } else {
                        containsFlag = false
                    }*/
                } else {
                    containsFlag = false
                }
                LogUtil.logE(TAG, "containsFlag:   ${containsFlag}")
                if (!containsFlag) {
                    try {
                        mPrinter.connect(kitchenPrinterList[i].macAddress, Printer.PARAM_DEFAULT)

                    } catch (e: java.lang.Exception) {
                        printerBGRunning = false
                        e.printStackTrace()
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
                    mPrinter.addFeedLine(1)


                    if (kitchenSettingModel.showOrderType) {


                        mPrinter.addFeedLine(0)
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
                        mPrinter.addText(printerQueueModel.orderType)

                    }

                    mPrinter.addFeedLine(2)
                    mPrinter.addTextFont(Builder.FONT_E)
                    //  builder.addTextAlign(Builder.ALIGN_LEFT)
                    mPrinter.addTextLang(Builder.LANG_EN)
                    mPrinter.addTextSize(1, 1)
                    mPrinter.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )

                    mPrinter.addText(
                        padLine(
                            "OrderID:" + printerQueueModel.orderID,
                            "",
                            48
                        )
                    )


                    mPrinter.addFeedUnit(30)
                    mPrinter.addTextFont(Builder.FONT_E)
                    //  builder.addTextAlign(Builder.ALIGN_LEFT)
                    mPrinter.addTextLang(Builder.LANG_EN)
                    mPrinter.addTextSize(1, 1)
                    mPrinter.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )


                    mPrinter.addText(
                        padLine(
                            "ReceiptID:" + printerQueueModel.offlineId,
                            "",
                            48
                        )
                    )


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

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val current = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy hh:mm:a")
                        val formatted = current.format(formatter)
                        mPrinter.addText(
                            "Print Time:" + Constants.getCurrentTimeFromTimeZone(
                                mContext,
                                formatted
                            )
                        )
                    }
                    mPrinter.addFeedLine(1)
                    addHorizontalLineNewU220(mPrinter)

                    printerQueueModel.orderItems.let {
                        addOrdersForKitchenCustomerNewPrinter(
                            mPrinter,
                            it,
                            fontSizeH,
                            fontSizeW
                        )
                    }

                    if (kitchenSettingModel.showCustomerAddress != false or kitchenSettingModel.showCustomerPhone != false or kitchenSettingModel.showCustomerName != false) {
                        if (printerQueueModel.customerName.isNotEmpty()) {

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
                            addHorizontalLineNew(mPrinter)

                            if (kitchenSettingModel.showCustomerName) {

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
                                mPrinter.addText(printerQueueModel.customerName)

                            }


                            if (kitchenSettingModel.showCustomerPhone) {

                                if (printerQueueModel?.customerPhoneNo.isNotEmpty()) {
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
                                    mPrinter.addText(printerQueueModel.customerPhoneNo)
                                }

                            }

                            if (kitchenSettingModel.showCustomerAddress) {


                                if (printerQueueModel.customerAddress.isNotEmpty()) {

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

                                    mPrinter.addText(printerQueueModel.customerAddress)
                                }

                            }

                        }
                    }

                    mPrinter.addFeedLine(2)
                    mPrinter.addCut(Builder.CUT_FEED)
                    mPrinter.beginTransaction()
                    mPrinter.sendData(Printer.PARAM_DEFAULT)

                } else {
                    printerBGRunning = false
                }
            } else {
                printerBGRunning = false
            }
        }


    }

    private suspend fun configurePrinter(printerQueueModel: PrinterQueueModel, pos: Int) {

        initKitchenPrinter(kitchenPrinterList.get(0), printerQueueModel, pos, 0)


        if (kitchenPrinterList.size == 0) {
            isPrinterRunning = false
        }


    }

    private suspend fun initKitchenPrinter(
        data: PrinterResponse.Data.KitchenReceiptPrinters,
        printerQueueModel: PrinterQueueModel,
        index: Int,
        printerPos: Int
    ) {
        PrinterClass.closePrinter()
        delay(5000)

        val printer: Print? = Print(mContext)

        try {

            printer?.openPrinter(
                if (data.printer_type == Constants.BLUETOOTH) {
                    Print.DEVTYPE_BLUETOOTH
                } else {
                    Print.DEVTYPE_TCP
                },
                data.macAddress
            )

        } catch (e: Exception) {
            e.printStackTrace()
            LogUtil.logE(TAG, "PrinterOpenFailed")
            consumer?.disconnect()
            isPrinterRunning = false
            delay(1000)
            connectActionCable()

            return
        }


        if (printer != null) {
            LogUtil.logE(TAG, "GoingToStart")
            PrinterClass.setPrinter(printer)

            generateKitchenReceipt(data, "", printerQueueModel, index, printerPos)

        }


    }

    private suspend fun generateKitchenReceipt(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        printerQueueModel: PrinterQueueModel,
        index: Int,
        printerPos: Int
    ) {
        LogUtil.logE(TAG, "printerPosprinterPos  ${printerPos}")
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

            builder = Builder(pname, PrinterClass.language, mContext)

            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)
            builder.addFeedLine(2)
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

            addBuilderText(builder, printerQueueModel.orderType)

            /*     if (printerQueueModel?.data?.order_type?.toString()?.lowercase() == "OpenOrder".trim()
                         .toString().lowercase() || printerQueueModel?.data?.order_type?.toString()
                         ?.lowercase() == "Open Order".trim()
                         .toString().lowercase()
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
                         printerQueueModel.data.order_data.open_order_type.toString() ?: ""
                     )
                 }*/

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

                addBuilderText(builder, printerQueueModel.orderType)
            }


            builder.addFeedLine(2)
            builder.addTextFont(Builder.FONT_E)
            //  builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 1)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )

            builder.addText(
                padLine(
                    "OrderID:" + printerQueueModel.orderID,
                    "",
                    48
                )
            )

            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)
            builder.addTextFont(Builder.FONT_E)
            //  builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 1)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )


            builder.addText(
                padLine(
                    "ReceiptID:" + printerQueueModel.offlineId,
                    "",
                    48
                )
            )
            /*  if (kitchenSettingModel.showTeamMember) {

                  builder.addTextLineSpace(30)
                  builder.addFeedUnit(30)
                  builder.addTextFont(Builder.FONT_E)
                  //  builder.addTextAlign(Builder.ALIGN_LEFT)
                  builder.addTextLang(Builder.LANG_EN)
                  builder.addTextSize(1, 1)
                  builder.addTextStyle(
                      Builder.FALSE,
                      Builder.FALSE,
                      Builder.FALSE,
                      Builder.COLOR_1
                  )
                  builder.addText(

                          "Employee:"+printerQueueModel.data.
                  )

              }*/

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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val current = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy hh:mm:a")
                val formatted = current.format(formatter)
                builder.addText(
                    "Print Time:" + Constants.getCurrentTimeFromTimeZone(
                        mContext,
                        formatted
                    )
                )
            }
            builder.addFeedLine(1)
            addHorizontalLine(builder)

            printerQueueModel?.orderItems?.let {
                addOrdersForKitchenCustomer(
                    builder,
                    it,
                    fontSizeH,
                    fontSizeW
                )
            }

            /*  if (printerQueueModel?.data?.order_data?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
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


                  builder.addText(printerQueueModel.data.order_data.note)
              }*/

            if (kitchenSettingModel.showCustomerAddress != false or kitchenSettingModel.showCustomerPhone != false or kitchenSettingModel.showCustomerName != false) {
                if (printerQueueModel.customerName.isNotEmpty()) {

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
                        builder.addText(printerQueueModel.customerName)

                    }


                    if (kitchenSettingModel.showCustomerPhone) {

                        if (printerQueueModel?.customerPhoneNo.isNotEmpty()) {
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
                            builder.addText(printerQueueModel.customerPhoneNo)
                        }

                    }

                    if (kitchenSettingModel.showCustomerAddress) {


                        if (printerQueueModel.customerAddress.isNotEmpty()) {

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

                            builder.addText(printerQueueModel.customerAddress)
                        }

                    }

                }
            }

            /*  if (receiptModel?.order?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
                  builder.addTextLineSpace(30)
                  builder.addFeedUnit(30)
                  builder.addFeedLine(1)
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
                  builder.addText("Order Note")

                  builder.addTextLineSpace(30)
                  builder.addFeedUnit(30)

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


                  builder.addText(receiptModel?.order?.note.toString())
              }*/


            //           if (kitchenSettingModel.showCustomerAddress != false or kitchenSettingModel.showCustomerPhone != false or kitchenSettingModel.showCustomerName) {
//                if (receiptModel?.order?.customer != null) {
//
//                    builder.addTextLineSpace(30)
//                    builder.addFeedUnit(30)
//                    builder.addFeedLine(1)
//                    builder.addTextFont(Builder.FONT_E)
//                    //builder.addTextLineSpace(20)
//                    builder.addTextAlign(Builder.ALIGN_LEFT)
//                    builder.addTextLang(Builder.LANG_EN)
//                    builder.addTextSize(1, 1)
//                    builder.addTextStyle(
//                        Builder.FALSE,
//                        Builder.FALSE,
//                        Builder.TRUE,
//                        Builder.COLOR_1
//                    )
//                    builder.addText("Customer Details" + "\n")
//
//                    builder.addTextFont(Builder.FONT_B)
//                    //builder.addTextLineSpace(20)
//                    builder.addTextLang(Builder.LANG_EN)
//                    builder.addTextSize(1, 1)
//                    builder.addTextStyle(
//                        Builder.FALSE,
//                        Builder.FALSE,
//                        Builder.FALSE,
//                        Builder.COLOR_1
//                    )
//                    addHorizontalKitchenLine(builder)
//
//                    if (kitchenSettingModel.showCustomerName) {
//
//                        builder.addTextLineSpace(30)
//                        builder.addFeedUnit(30)
//                        builder.addTextFont(Builder.FONT_E)
//                        builder.addTextAlign(Builder.ALIGN_LEFT)
//                        //builder.addTextLineSpace(20)
//                        builder.addTextLang(Builder.LANG_EN)
//                        builder.addTextSize(1, 1)
//                        builder.addTextStyle(
//                            Builder.FALSE,
//                            Builder.FALSE,
//                            Builder.TRUE,
//                            Builder.COLOR_1
//                        )
//                        builder.addText(receiptModel?.order?.customer?.firstName + " " + receiptModel?.order?.customer?.lastName)
//
//                    }
//
//
//                    if (kitchenSettingModel.showCustomerPhone) {
//
//                        if (receiptModel?.order?.customer?.phones?.isNotEmpty() == true) {
//                            builder.addTextLineSpace(30)
//                            builder.addFeedUnit(30)
//                            builder.addTextFont(Builder.FONT_E)
//                            builder.addTextAlign(Builder.ALIGN_LEFT)
//                            //builder.addTextLineSpace(20)
//                            builder.addTextLang(Builder.LANG_EN)
//                            builder.addTextSize(1, 1)
//                            builder.addTextStyle(
//                                Builder.FALSE,
//                                Builder.FALSE,
//                                Builder.TRUE,
//                                Builder.COLOR_1
//                            )
//                            builder.addText(receiptModel?.order?.customer?.phones?.get(0)?.phoneNumber)
//                        }
//
//                    }
//                    /* builder.addTextLineSpace(30)
//                 builder.addFeedUnit(30)
//                 builder.addTextFont(Builder.FONT_E)
//                 builder.addTextAlign(Builder.ALIGN_LEFT)
//                 //builder.addTextLineSpace(20)
//                 builder.addTextLang(Builder.LANG_EN)
//                 builder.addTextSize(1, 1)
//                 builder.addTextStyle(
//                     Builder.FALSE,
//                     Builder.FALSE,
//                     Builder.TRUE,
//                     Builder.COLOR_1
//                 )
//                 builder.addText(receiptModel?.order?.customer?.email)*/
//
//                    if (kitchenSettingModel.showCustomerAddress) {
//                        if (receiptModel?.order?.customer?.addresses?.isNotEmpty() == true) {
//
//                            builder.addTextLineSpace(30)
//                            builder.addFeedUnit(30)
//                            builder.addTextFont(Builder.FONT_E)
//                            builder.addTextAlign(Builder.ALIGN_LEFT)
//                            //builder.addTextLineSpace(20)
//                            builder.addTextLang(Builder.LANG_EN)
//                            builder.addTextSize(1, 1)
//                            builder.addTextStyle(
//                                Builder.FALSE,
//                                Builder.FALSE,
//                                Builder.TRUE,
//                                Builder.COLOR_1
//                            )
//
//                            builder.addText(receiptModel?.order?.customer?.addresses?.get(0)?.fullAddress)
//                        }
//                    }
//
//                }
//            }

            builder.addFeedLine(2)

            builder.addCut(Builder.CUT_FEED)

            val status = IntArray(1)
            status[0] = 0

            PrinterClass.getPrinter()?.sendData(
                builder, 10000, status
            )


//            PrinterClass.closePrinter()

            try {


                //printerQueuelist.removeAt(index)

                if (printerPos == (kitchenPrinterList.size - 1)) {
                    globalPrinterQueue = null
                    printerQueueModel.id?.let {
                        val params = JsonObject()
                        var deleteUrl = baseUrl + Constants.CREATE_QUEUE_PRINTER + "/" + it
                        LogUtil.logE(TAG, "DeleteUrl ${deleteUrl}")
                        params.addProperty("url", deleteUrl)
                        subscription?.perform("delete_order", params)

                        /* viewModel.deleteQueuePrinter(
                     it,
                     printerQueueModel.position
                 )*/

                        /* val params2 = JsonObject()
                 params2.addProperty("id", locationId)
                 params2.addProperty("url", baseUrl + Constants.CREATE_QUEUE_PRINTER)
                 subscription?.perform("received", params2)*/


                    }
                }



                delay(2000)

                if (kitchenPrinterList.size > printerPos + 1) {

                    initKitchenPrinter(
                        kitchenPrinterList.get(printerPos + 1),
                        printerQueueModel,
                        0,
                        printerPos + 1
                    )
                }


                /*if (printerPos == (kitchenPrinterList.size - 1)) {
                    delay(5000)
                    isPrinterRunning = false
                    val params2 = JsonObject()
                    params2.addProperty("id", locationId)
                    params2.addProperty("url", baseUrl + Constants.CREATE_QUEUE_PRINTER)
                    subscription?.perform("received", params2)
                }*/


                /*var requestURL =
                    baseUrl + Constants.CREATE_QUEUE_PRINTER
                LogUtil.logE(TAG, "requestURL:  ${requestURL}")
                val uri = URI("wss://hugepos.com/cable")
                consumer = ActionCable.createConsumer(uri)

                // 2. Create subscription
                val appearanceChannel = Channel("KitchenChannel")
                // appearanceChannel.addParam("id",prefProvider.getValueInt(LOCATION_ID,0))
                subscription = consumer?.subscriptions?.create(appearanceChannel)
                consumer?.connect()*/


                //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
            } catch (e: Exception) {

                if (kitchenPrinterList.size > printerPos) {

                    initKitchenPrinter(
                        kitchenPrinterList.get(printerPos),
                        printerQueueModel,
                        0,
                        printerPos
                    )
                }




                globalPrinterQueue = null
                printerQueueModel.id?.let {
                    val params = JsonObject()
                    var deleteUrl = baseUrl + Constants.CREATE_QUEUE_PRINTER + "/" + it
                    LogUtil.logE(TAG, "DeleteUrl ${deleteUrl}")
                    params.addProperty("url", deleteUrl)
                    subscription?.perform("delete_order", params)

                    /* viewModel.deleteQueuePrinter(
                     it,
                     printerQueueModel.position
                 )*/

                    /*
                val params2 = JsonObject()
                 params2.addProperty("id", locationId)
                 params2.addProperty("url", baseUrl + Constants.CREATE_QUEUE_PRINTER)
                 subscription?.perform("received", params2)*/


//                    PrinterClass.closePrinter()
                }


                isPrinterRunning = false
                delay(5000)

                val params2 = JsonObject()
                params2.addProperty("id", locationId)
                params2.addProperty("url", baseUrl + Constants.CREATE_QUEUE_PRINTER)
                subscription?.perform("received", params2)


                //  globalPrinterQueue = null


                /*  var requestURL =
                      baseUrl + Constants.CREATE_QUEUE_PRINTER
                  LogUtil.logE(TAG, "requestURL:  ${requestURL}")
                  val uri = URI("wss://hugepos.com/cable")
                  consumer = ActionCable.createConsumer(uri)

                  // 2. Create subscription
                  val appearanceChannel = Channel("KitchenChannel")
                  // appearanceChannel.addParam("id",prefProvider.getValueInt(LOCATION_ID,0))
                  subscription = consumer?.subscriptions?.create(appearanceChannel)
                  consumer?.connect()*/
                // printerQueuelist.removeAt(index)


                /* printerQueueModel.id?.let {
                     val params = JsonObject()
                     var deleteUrl = baseUrl + Constants.CREATE_QUEUE_PRINTER + "/" + it
                     LogUtil.logE(TAG, "DeleteUrl ${deleteUrl}")
                     params.addProperty("url", deleteUrl)
                     subscription?.perform("delete_order", params)

                     *//* viewModel.deleteQueuePrinter(
                         it,
                         printerQueueModel.position
                     )*//*
                }*/


                e.printStackTrace()
                /*
                val params = JsonObject()
                params.addProperty("id", locationId)
                subscription?.perform("received", params)
                */
                //isPrinterRunning = false

            }


        } catch (e: Exception) {
            isPrinterRunning = false

            e.printStackTrace()
        }


    }

    override fun onPtrStatusChange(printer: Printer?, p1: Int) {
        Log.e(TAG, "statusInfoNewPrinter:  ${Gson().toJson(printer?.status)} print ${p1}")


        /* printer?.stopMonitor()
         printer?.disconnect()*/

        if (p1 == 21) {

            isConnectedU220 = true


        }

    }

    override fun onPtrReceive(p0: Printer?, p1: Int, p2: PrinterStatusInfo?, p3: String?) {
        Log.e(
            TAG,
            "onPrinterSucces str: ${p3}  intCode ${p1} getStatusInfo ${Gson().toJson(p2)}  getAdminData: ${
                Gson().toJson(p0?.admin)
            }  getPrntStatus: ${Gson().toJson(p0?.status)} getLocation: ${p0?.location}"
        )


        isQueueRunning = false

        if (p1 >= 0) {
            p0?.clearCommandBuffer()
            try {


//                p0?.endTransaction()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (listOfPrintersData.size - 1 >= currentPrinterIndex) {

                try {
                    val params = JsonObject()
                    var deleteUrl =
                        baseUrl + DELETE_QUEUE_ORDER_PHASE3 + listOfPrintersData.get(
                            currentPrinterIndex
                        ).printerQueueModelList.get(
                            currentOrderIndex
                        ).id
                    LogUtil.logE(TAG, "DeleteUrl ${deleteUrl}")
                    params.addProperty("url", deleteUrl)
                    params.addProperty(
                        "mac_address",
                        listOfPrintersData.get(currentPrinterIndex).macAddress
                    )

                    subscription?.perform("delete_order", params)
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }

            }

            //p0?.endTransaction()


            ////////////////////////////////////////////////////////PHASE 1 CODE////////////////////////////////////////////////////
            /*for (i in 0 until kitchenPrinterList.size) {
                printerObjList.get(kitchenPrinterList.get(i).macAddress)
                    ?.let {
                        if (printerObjList.get(kitchenPrinterList.get(i).macAddress) == p0) {
                            Log.e(TAG, "checkPrinterObjOnPrntReceive:  ")


                            val params = JsonObject()
                            var deleteUrl =
                                baseUrl + CREATE_QUEUE_PRINTER + "/" + printerQueueModel.id + "/" + Constants.UPDATE_PRITNER_QUEUE_TRACK
                            LogUtil.logE(TAG, "DeleteUrl ${deleteUrl}")
                            params.addProperty("url", deleteUrl)
                            params.addProperty(
                                "printed_mac_add",
                                kitchenPrinterList.get(i).macAddress
                            )

                            val gson = Gson()
                            val itemType = object : TypeToken<List<String>>() {}.type
                            var printMACAddress =
                                gson.fromJson<List<String>>(
                                    printerQueueModel.printSuccessData,
                                    itemType
                                )


                            Log.e(
                                TAG,
                                "printMACAddressonResponse:  ${Gson().toJson(printMACAddress)}"
                            )


                            printMACAddress.toCollection(arrayListOf())
                                .add(kitchenPrinterList[i].macAddress)
                            Log.e(TAG, "checkPrinterDeleteSize  ${printMACAddress.size}")
                            Log.e(TAG, "checkPrinterDeleteSizekitc  ${kitchenPrinterList.size}")
                            if (kitchenPrinterList.size == printMACAddress.size) {
                                Log.e(TAG, "orderDeleted")

                                params.addProperty("order_to_be_delete", true)
                            } else {
                                Log.e(TAG, "orderDeletedNot")
                                params.addProperty("order_to_be_delete", true)
                            }
                            subscription?.perform("updated_order_item_status", params)


                        }

                    }
            }*/
            ////////////////////////////////////////////////////////PHASE 1 CODE////////////////////////////////////////////////////


            // subscription?.perform("delete_order", params)

        } else if (p2?.paper ?: 0 > 0) {

            sendNotification("Please fill the Paper in ${listOfPrintersData.get(currentPrinterIndex).modelName}.")
            try {
                p0?.clearCommandBuffer()
                p0?.endTransaction()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }

        }


        Log.e(
            TAG,
            "checkPrinterDataSize ${listOfPrintersData.size}  currentPrinterIndex: ${currentPrinterIndex}"
        )
        if (listOfPrintersData.size - 1 == currentPrinterIndex) {
            /*  Log.e(
                  TAG,
                  "listOfPrinerData:   ${listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.size}"
              )
              Log.e(TAG, "listOfcurrentOrderIndex:   ${currentOrderIndex}")
  */


            if (listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.size - 1 == currentOrderIndex) {
                // Log.e(TAG, "checkLastORderPRint  ")
                runBlocking {
                    delay(3000)

                    val params = JsonObject()
                    params.addProperty("id", locationId)
                    params.addProperty("url", baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3)
                    Log.e(
                        TAG,
                        "checkReuestURL: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                    )
                    subscription?.perform("received", params)
                }

            } else {

                runBlocking {
                    currentOrderIndex = currentOrderIndex + 1
                    if (listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.isNotEmpty()) {

                        sendDataToPrint(
                            listOfPrintersData,
                            currentPrinterIndex,
                            printerObjList.get(listOfPrintersData.get(currentPrinterIndex).macAddress) as Printer?,
                            listOfPrintersData.get(currentPrinterIndex).printerQueueModelList,
                            listOfPrintersData.get(currentPrinterIndex).macAddress,
                            currentOrderIndex

                        )
                    } else {
                        runBlocking {
                            delay(3000)
                            val params = JsonObject()
                            params.addProperty("id", locationId)
                            params.addProperty("url", baseUrl + CREATE_QUEUE_PRINTER_PHASE3)
                            Log.e(
                                TAG,
                                "checkReuestURL: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                            )
                            subscription?.perform("received", params)
                        }


                    }
                }
            }

        } else if (listOfPrintersData.size - 1 != currentPrinterIndex) {
            /* Log.e(
                 TAG,
                 "checkLog: ${currentPrinterIndex}  orderIndex: ${
                     listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.size - 1
                 }  currentOrderInd: ${currentOrderIndex}"
             )*/
            if (listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.size - 1 == currentOrderIndex) {


                currentOrderIndex = 0
                currentPrinterIndex = currentPrinterIndex + 1
                if (listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.isNotEmpty()) {
                    runBlocking {
                        sendDataToPrint(
                            listOfPrintersData,
                            currentPrinterIndex,
                            printerObjList.get(listOfPrintersData.get(currentPrinterIndex).macAddress) as Printer?,
                            listOfPrintersData.get(currentPrinterIndex).printerQueueModelList,
                            listOfPrintersData.get(currentPrinterIndex).macAddress,
                            currentOrderIndex

                        )
                    }
                } else {
                    runBlocking {
                        delay(3000)
                        val params = JsonObject()
                        params.addProperty("id", locationId)
                        params.addProperty("url", baseUrl + CREATE_QUEUE_PRINTER_PHASE3)
                        Log.e(
                            TAG,
                            "checkReuestURL: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                        )
                        subscription?.perform("received", params)
                    }
                }

            } else {

                currentOrderIndex = currentOrderIndex + 1
                runBlocking {
                    sendDataToPrint(
                        listOfPrintersData,
                        currentPrinterIndex,
                        printerObjList.get(listOfPrintersData.get(currentPrinterIndex).macAddress) as Printer?,
                        listOfPrintersData.get(currentPrinterIndex).printerQueueModelList,
                        listOfPrintersData.get(currentPrinterIndex).macAddress,
                        currentOrderIndex

                    )
                }

            }

        } else {
            runBlocking {
                delay(3000)
                val params = JsonObject()
                params.addProperty("id", locationId)
                params.addProperty("url", baseUrl + CREATE_QUEUE_PRINTER_PHASE3)
                Log.e(
                    TAG,
                    "checkReuestURL: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                )
                subscription?.perform("received", params)
            }

        }

        /*   if (p0 == printerObjList.get(kitchenPrinterList.get(kitchenPrinterList.size - 1).macAddress)) {
               delay(5000)

               val params = JsonObject()
               params.addProperty("id", locationId)
               params.addProperty("url", baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3)
               subscription?.perform("received", params)
           }*/

    }

    override fun onConnection(p0: Any?, p1: Int) {
        Log.e(TAG, "checkConnectionListner  p0: ${p0}  p1: ${p1}")

        when (p1) {
            Printer.EVENT_DISCONNECT -> {

            }

            Printer.EVENT_RECONNECT -> {

            }

            Printer.EVENT_RECONNECTING -> {

            }

            Printer.EVENT_OFFLINE -> {
                val mBuilder = NotificationCompat.Builder(mContext).setSmallIcon(
                    R.drawable.ic_launcher_foreground
                ).setContentTitle("Printer Queue is Running in background.")
                    .setContentText("TM-U220 is offline.").setAutoCancel(true)
                mgr.notify(101, mBuilder.build())


            }

            Printer.EVENT_ONLINE -> {


            }


        }

    }


    private fun sendNotification(messageBody: String) {


        val channelId = mContext.getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(mContext, channelId)
            .setSmallIcon(R.drawable.ic_baseline_notifications_24)
            .setContentTitle(mContext.getString(R.string.app_name))
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)

        val notificationManager =
            mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(
            System.currentTimeMillis().toInt()/* ID of notification */,
            notificationBuilder.build()
        )
    }

    override fun onComplete() {
        isQueueRunning = false
        Log.e(
            TAG,
            "checkListData  ${listOfPrintersData.size}  currentPrinter  ${currentPrinterIndex}"
        )
        if (listOfPrintersData.size - 1 >= currentPrinterIndex) {

            try {
                val params = JsonObject()
                var deleteUrl =
                    baseUrl + DELETE_QUEUE_ORDER_PHASE3 + listOfPrintersData.get(
                        currentPrinterIndex
                    ).printerQueueModelList.get(
                        currentOrderIndex
                    ).id
                LogUtil.logE(TAG, "DeleteUrl ${deleteUrl}")
                params.addProperty("url", deleteUrl)
                Log.e(
                    TAG,
                    "checkDeleteSunmi  ${listOfPrintersData.get(currentPrinterIndex).printerName}"
                )
                params.addProperty(
                    "name",
                    listOfPrintersData.get(currentPrinterIndex).printerName
                )

                subscription?.perform("delete_order", params)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }

        }


        if (listOfPrintersData.size - 1 == currentPrinterIndex) {
            /*  Log.e(
                  TAG,
                  "listOfPrinerData:   ${listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.size}"
              )
              Log.e(TAG, "listOfcurrentOrderIndex:   ${currentOrderIndex}")
  */


            if (listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.size - 1 == currentOrderIndex) {
                // Log.e(TAG, "checkLastORderPRint  ")
                runBlocking {
                    delay(3000)

                    val params = JsonObject()
                    params.addProperty("id", locationId)
                    params.addProperty("url", baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3)
                    Log.e(
                        TAG,
                        "checkReuestURLREquestews: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                    )
                    subscription?.perform("received", params)
                }

            } else {

                runBlocking {
                    currentOrderIndex = currentOrderIndex + 1
                    if (listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.isNotEmpty()) {

                        sendDataToPrintToSunmi(
                            listOfPrintersData,
                            currentPrinterIndex,
                            printerObjList.get(listOfPrintersData.get(currentPrinterIndex).macAddress) as CloudPrinter?,
                            listOfPrintersData.get(currentPrinterIndex).printerQueueModelList,
                            listOfPrintersData.get(currentPrinterIndex).macAddress,
                            currentOrderIndex

                        )
                    } else {
                        runBlocking {
                            delay(3000)
                            val params = JsonObject()
                            params.addProperty("id", locationId)
                            params.addProperty("url", baseUrl + CREATE_QUEUE_PRINTER_PHASE3)
                            Log.e(
                                TAG,
                                "checkReuestURL: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                            )
                            subscription?.perform("received", params)
                        }


                    }
                }
            }

        } else if (listOfPrintersData.size - 1 != currentPrinterIndex) {
            /* Log.e(
                 TAG,
                 "checkLog: ${currentPrinterIndex}  orderIndex: ${
                     listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.size - 1
                 }  currentOrderInd: ${currentOrderIndex}"
             )*/
            Log.e(
                TAG,
                "cehckereCurrent  ${listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.size}"
            )
            if (listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.size - 1 == currentOrderIndex) {


                currentOrderIndex = 0
                currentPrinterIndex = currentPrinterIndex + 1
                if (listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.isNotEmpty()) {
                    runBlocking {
                        sendDataToPrintToSunmi(
                            listOfPrintersData,
                            currentPrinterIndex,
                            printerObjList.get(listOfPrintersData.get(currentPrinterIndex).macAddress) as CloudPrinter?,
                            listOfPrintersData.get(currentPrinterIndex).printerQueueModelList,
                            listOfPrintersData.get(currentPrinterIndex).macAddress,
                            currentOrderIndex

                        )
                    }
                } else {
                    runBlocking {
                        delay(3000)
                        val params = JsonObject()
                        params.addProperty("id", locationId)
                        params.addProperty("url", baseUrl + CREATE_QUEUE_PRINTER_PHASE3)
                        Log.e(
                            TAG,
                            "checkReuestURL: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                        )
                        subscription?.perform("received", params)
                    }
                }

            } else {

                currentOrderIndex = currentOrderIndex + 1
                runBlocking {
                    sendDataToPrintToSunmi(
                        listOfPrintersData,
                        currentPrinterIndex,
                        printerObjList.get(listOfPrintersData.get(currentPrinterIndex).macAddress) as CloudPrinter?,
                        listOfPrintersData.get(currentPrinterIndex).printerQueueModelList,
                        listOfPrintersData.get(currentPrinterIndex).macAddress,
                        currentOrderIndex

                    )
                }

            }

        } else {
            runBlocking {
                delay(3000)
                val params = JsonObject()
                params.addProperty("id", locationId)
                params.addProperty("url", baseUrl + CREATE_QUEUE_PRINTER_PHASE3)
                Log.e(
                    TAG,
                    "checkReuestURL: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                )
                subscription?.perform("received", params)
            }

        }

    }

    override fun onFailed(p0: CloudPrinterStatus?) {
        Log.e(TAG, "check failed  ${Gson().toJson(p0)}")

        (printerObjList.get(listOfPrintersData.get(currentPrinterIndex).macAddress) as CloudPrinter).clearTransBuffer()
        (printerObjList.get(listOfPrintersData.get(currentPrinterIndex).macAddress) as CloudPrinter).release(
            mContext
        )
        isQueueRunning = false
        if (p0?.name.equals("RUNNING", true) == false) {

            if (isPrinterRunning == false) {

                val params = JsonObject()
                params.addProperty("id", locationId)
                params.addProperty(
                    "url",
                    baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3
                )
                Log.e(
                    TAG,
                    "checkReuestURLREquestews: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                )
                // subscription?.perform("received", params)
            }


            if (p0?.name.equals("UNKNOWN", true)) {
                sendNotification("Printer - ${listOfPrintersData[currentPrinterIndex].modelName} is Offline.")


            } else if (p0?.name.equals("OUT_PAPER", true)) {


                sendNotification(
                    "Please fill the Paper in ${
                        listOfPrintersData.get(
                            currentPrinterIndex
                        ).modelName
                    }."
                )


            } else if (p0?.name.equals("COVER", true)) {
                sendNotification(
                    "Please close the cover of ${
                        listOfPrintersData.get(
                            currentPrinterIndex
                        ).modelName
                    }."
                )
            }


            /*   if (listOfPrintersData.size - 1 < currentPrinterIndex) {

                   var getOrders = false
                   for (l in currentPrinterIndex until listOfPrintersData.size) {

                       if (listOfPrintersData.get(l).printerQueueModelList.isNotEmpty()) {
                           getOrders = true
                           currentPrinterIndex = l
                           currentOrderIndex = 0
                           break
                       }
                   }

                   if (getOrders == true) {
                       sendDataToPrintToSunmi(
                           listOfPrintersData,
                           currentPrinterIndex,
                           printerObjList.get(listOfPrintersData.get(currentPrinterIndex).macAddress) as CloudPrinter?,
                           listOfPrintersData.get(currentPrinterIndex).printerQueueModelList,
                           listOfPrintersData.get(currentPrinterIndex).macAddress,
                           currentOrderIndex

                       )

                   } else {
                       runBlocking {
                           delay(3000)

                           val params = JsonObject()
                           params.addProperty("id", locationId)
                           params.addProperty(
                               "url",
                               baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3
                           )
                           Log.e(
                               TAG,
                               "checkReuestURLREquestews: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                           )
                           subscription?.perform("received", params)
                       }

                   }

               } else {



                       val params = JsonObject()
                       params.addProperty("id", locationId)
                       params.addProperty(
                           "url",
                           baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3
                       )
                       Log.e(
                           TAG,
                           "checkReuestURLREquestews: ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}  locationID: ${locationId}"
                       )
                       subscription?.perform("received", params)
                   }*/


        } else {
            Log.e(TAG, "check failed 2 in else")
        }


    }

}


