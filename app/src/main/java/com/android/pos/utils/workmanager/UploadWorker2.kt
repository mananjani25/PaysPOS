package com.android.pos.utils.workmanager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
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
import com.android.pos.data.remote.Constants.CHECK_QUEUE_CANCEL
import com.android.pos.ui.activities.MainActivity
import com.android.pos.utils.addDoubleDotLineForSunmiQueue
import com.android.pos.utils.printGuestByItemForSunmiQueue
import com.epson.epos2.ConnectionListener
import com.epson.epos2.printer.Printer
import com.epson.epos2.printer.PrinterStatusInfo
import com.epson.epos2.printer.ReceiveListener
import com.epson.epos2.printer.StatusChangeListener
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.hosopy.actioncable.ActionCable
import com.hosopy.actioncable.Channel
import com.hosopy.actioncable.Consumer
import com.hosopy.actioncable.Subscription
import com.sunmi.externalprinterlibrary2.ConnectCallback
import com.sunmi.externalprinterlibrary2.ResultCallback
import com.sunmi.externalprinterlibrary2.printer.CloudPrinter
import com.sunmi.externalprinterlibrary2.printer.CloudPrinterBuilder
import com.sunmi.externalprinterlibrary2.style.AlignStyle
import com.sunmi.externalprinterlibrary2.style.CloudPrinterStatus
import com.sunmi.externalprinterlibrary2.style.UnderlineStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.annotations.NotNull
import java.net.URI

class UploadWorker2(@NotNull context: Context, @NotNull params: WorkerParameters) :
    CoroutineWorker(context, params),
    StatusChangeListener, ReceiveListener, ConnectionListener,
    ResultCallback {

    private var isPrinterQueueEnable: Boolean = false
    private var currentCloudPrinter: CloudPrinter? = null
    private var printerQueueData: Boolean = false
    private var globalPrinterQueue: JsonElement? = null
    private val TAG = UploadWorker2::class.java.name
    private val TAG2 = "SYNCSETTINGS"
    private var isConnectedU220: Boolean = false
    val printerQueueModel: PrinterQueueModel = PrinterQueueModel()
    private var printerObjList: HashMap<String, Any> = hashMapOf()
    var listOfPrintersData: ArrayList<PrinterJSONElementData> = arrayListOf()
    var isQueueRunning: Boolean = false
    var notWorkingPrintersName: ArrayList<String> = arrayListOf()

    var printerBreak: Boolean = false
    var printerSize: Int = 0
    var orderSize: Int = 0
    var currentPrinterIndex = 0
    var currentOrderIndex = 0
    val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private var subscription: Subscription? = null
    private var subscription2: Subscription? = null
    private var consumer: Consumer? = null
    private var consumer2: Consumer? = null
    private var locationId: Int = 0
    private var baseUrl = ""
    private var kitchenPrinterList: List<PrinterResponse.Data.KitchenReceiptPrinters> = listOf()
    private var kitchenSettingModel = GetKitchenReceiptSettingsResponse.Data()
    private var mContext: Context = context
    private var isPrinterRunning: Boolean = false
    private var printerBGRunning: Boolean = false
    private var isCancelWork: Boolean = true


    override suspend fun doWork(): Result {

        try {
            locationId = inputData.getInt("location_id", 0)
            baseUrl = inputData.getString("base_url").toString()
            isPrinterQueueEnable = inputData.getBoolean(Constants.IS_PRINTER_QUEUE_ENABLE, false)
            isCancelWork = inputData.getBoolean("is_cancel_work", false)

            Log.d("isPrinterQueueEnable","isPrinterQueueEnable = $isPrinterQueueEnable")

            Log.e(TAG,"CheckQueueCancel  ${mContext.getSharedPreferences(
                mContext.resources.getString(R.string.app_name),
                Context.MODE_PRIVATE
            ).getBoolean(CHECK_QUEUE_CANCEL, false)}")

            if ( mContext.getSharedPreferences(
                    mContext.resources.getString(R.string.app_name),
                    Context.MODE_PRIVATE
                ).getBoolean(CHECK_QUEUE_CANCEL, false) == false ) {
                Log.e(TAG, "checkIsdws")
                if (isInternetAvailable()) {
                    if (isPrinterQueueEnable){
                        connectActionCable()
                    }
                } else {
                    // show popup for network
                    sendNotification("Please check your Network Connectivity.")
                }
            } else {
                Log.e(TAG, "fsfkiwoorm")
                consumer?.disconnect()
                consumer?.subscriptions?.remove(subscription)
            }

            if (isInternetAvailable()) {
                connectActionCableSYNCSETTINGS()
            } else {
                sendNotification("Please check your Network Connectivity.")
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }



        return Result.success()
    }

    private fun connectActionCable() {
        var requestURL =
            baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3
        Log.e(TAG, "checkrequestURL  ${requestURL}")

        val uri = URI(Constants.PRINTER_QUEUE_CONNECTION_URL_SNACKPOS)

        consumer = ActionCable.createConsumer(uri)

        // 2. Create subscription
        val appearanceChannel = Channel("PrinterQueueV4Channel")
        appearanceChannel.addParam("id", locationId)
        // appearanceChannel.addParam("id",prefProvider.getValueInt(LOCATION_ID,0))
        subscription = consumer?.subscriptions?.create(appearanceChannel)

        if (subscription != null) {
            subscription?.onConnected {
                Log.e(TAG, "onActionConnected")
                val params = JsonObject()
                params.addProperty("id", locationId)
                params.addProperty("url", requestURL)
                Log.e(
                    TAG,
                    "checkID 8: ${locationId}  checkURL 8:  ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}"
                )
                subscription?.perform("received", params)

            }?.onRejected {
                Log.e(TAG, "onRejected ")
                Handler(Looper.getMainLooper()).postDelayed(Runnable {
                    if (isInternetAvailable()) {
                        consumer?.connect()
                    } else {
                        sendNotification("Please check your Network Connectivity.")
                    }
                }, 10000)

            }?.onReceived {
                Log.e(TAG, "onActionReceived:  ${Gson().toJson(it)}")
                Log.e(TAG, "onActionReceived checkCancelWeok:  ${isCancelWork}")

                if ( mContext.getSharedPreferences(
                        mContext.resources.getString(R.string.app_name),
                        Context.MODE_PRIVATE
                    ).getBoolean(CHECK_QUEUE_CANCEL, false) == true
                ) {
                    Log.e(TAG, "checkCancelWork")
                    consumer?.disconnect()
                } else {

                    if (it != null && isQueueRunning == false) {
                        isQueueRunning = true
                        listOfPrintersData.clear()
                        listOfPrintersData = arrayListOf()

                        if (it.asJsonObject.has("printer_queue")) {
                            isPrinterRunning = true
                            globalPrinterQueue = it.asJsonObject.get("printer_queue")
                            runBlocking {
                                getQueueDataResponse(it.asJsonObject.get("printer_queue"))
                            }


                        } else {

                            runBlocking {
                                Log.e(TAG, "callActionCalledRun 3")

                                isQueueRunning = false
                                currentOrderIndex = 0
                                currentPrinterIndex = 0
                                delay(5000)

                                val params = JsonObject()
                                params.addProperty("id", locationId)
                                params.addProperty("url", requestURL)
                                Log.e(TAG, "checkID: ${locationId}  checkURL:  ${requestURL}")

                                subscription?.perform("received", params)

                            }


                            /*val intent = Intent()
                    intent.putExtra(Constants.DATA, "")
                    intent.action = PRINTER_QUEUE_DATA_RECEIVED
                    mContext.sendBroadcast(intent)*/


                        }

                    }
                }


            }?.onDisconnected {
                Log.e(TAG, "onDisconnected")


                    Handler(Looper.getMainLooper()).postDelayed(Runnable {
                        if (isInternetAvailable()) {


                            if (mContext.getSharedPreferences(
                                    mContext.resources.getString(R.string.app_name),
                                    Context.MODE_PRIVATE
                                ).getBoolean(CHECK_QUEUE_CANCEL, false) == false) {
                                consumer?.connect()
                            }
                            else{
                                consumer?.subscriptions?.remove(subscription)
                            }
                        } else {
                            sendNotification("Please check your Network Connectivity.")
                        }
                    }, 6000)


            }?.onFailed {
                Log.e(TAG, "onFailed")
                Handler(Looper.getMainLooper()).postDelayed(Runnable {
                    if (isInternetAvailable()) {
                        consumer?.connect()
                    } else {
                        sendNotification("Please check your Network Connectivity.")
                    }
                }, 6000)


            }

        }


        // 3. Establish connection
        if ( mContext.getSharedPreferences(
                mContext.resources.getString(R.string.app_name),
                Context.MODE_PRIVATE
            ).getBoolean(CHECK_QUEUE_CANCEL, false) == false
        ) {
            consumer?.connect()
        }

    }

    private fun getQueueDataResponse(model: JsonElement) {

        if (model.asJsonObject.has("data")) {
            var dataList: JsonArray = model.asJsonObject.get("data").asJsonArray

            if (dataList.size() != 0) {
                isQueueRunning = true
                dataList.forEach {
                    var listofPrinterOrders: ArrayList<PrinterQueueModel> = arrayListOf()

                    if (it.asJsonObject.has("orders")) {

                        var ordersArray = it.asJsonObject.get("orders").asJsonArray
                        ordersArray.forEach {
                            var modelOrder = PrinterQueueModel()

                            modelOrder.id = it.asJsonObject.get("id").asInt
                            modelOrder.orderType =
                                it.asJsonObject.get("order_type").asString

                            try {
                                if (it.asJsonObject.has("delivery_type")){
                                    modelOrder.deliveryType = it.asJsonObject.get("delivery_type").asString
                                }
                            }catch (e:Exception){
                                Log.d("KeyNotFound","Exception")
                            }

                            modelOrder.dateAndTime =
                                it.asJsonObject.get("date_and_time").asString
                            modelOrder.employeeName =
                                it.asJsonObject.get("employee_name").asString
                            modelOrder.orderNote = it.asJsonObject.get("order_note").asString

                            if (it.asJsonObject.get("order_type").asString.equals(
                                    Constants.DINE_IN,
                                    true
                                ) || it.asJsonObject.get("order_type").asString.equals(
                                    Constants.DINE_IN_SPACE,
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
                                    modelOrder.orderID = it.asJsonObject.get("id").asInt.toString()


                                }
                                if (it.asJsonObject.has("customer_details")) {

                                    if (it.asJsonObject.get("customer_details").isJsonObject) {
                                        var objCustomer =
                                            it.asJsonObject.get("customer_details").asJsonObject
                                        modelOrder.customerName =
                                            objCustomer.get("first_name").asString + " " + objCustomer.get(
                                                "last_name"
                                            ).asString
                                        modelOrder.customerPhoneNo =
                                            objCustomer.get("mobile_number").asString
                                        modelOrder.customerAddress =
                                            objCustomer.get("address").asString

                                    }

                                }

                                listofPrinterOrders.add(modelOrder)
                            }

                        }

                    }

                    listOfPrintersData.add(
                        PrinterJSONElementData(
                            printerName = it.asJsonObject.get("printer_name").asString,
                            macAddress = it.asJsonObject.get("mac_address").asString,
                            ipAddress = it.asJsonObject.get("ip_address").asString,
                            modelName = it.asJsonObject.get("modal_name").asString,
                            printerQueueModelList = listofPrinterOrders,
                            portNo = it.asJsonObject.get("port_no").asInt

                        )
                    )
                }
                var isDataGot = false

                for (i in 0 until listOfPrintersData.size) {

                    if (listOfPrintersData.get(i).printerQueueModelList.isNotEmpty()) {
                        currentPrinterIndex = i
                        currentOrderIndex = 0
                        isDataGot = true
                        break
                    }
                }
                if (isDataGot) {

                    if (listOfPrintersData.get(0).printerName.contains("CloudPrint_", true)) {
                        sendDataToPrintToSunmi()

                    } else {
                        //call action cable here

                        runBlocking {
                            isQueueRunning = false

                            currentOrderIndex = 0
                            currentPrinterIndex = 0
                            Log.e(TAG, "callActionCalledRun 4")
                            delay(2000)

                            val params = JsonObject()
                            params.addProperty("id", locationId)
                            params.addProperty(
                                "url",
                                baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3
                            )
                            Log.e(
                                TAG,
                                "checkID 2: ${locationId}  checkURL 2:  ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}"
                            )

                            subscription?.perform("received", params)

                        }

                    }

                } else {
                    //call action cable again here


                    Log.e(TAG, "callActionCalledRun 5")
                    isQueueRunning = false
                    currentOrderIndex = 0
                    currentPrinterIndex = 0

                    runBlocking {
                        delay(4000)

                        val params = JsonObject()
                        params.addProperty("id", locationId)
                        params.addProperty("url", baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3)
                        Log.e(
                            TAG,
                            "checkID 3: ${locationId}  checkURL 3:  ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}"
                        )
                        subscription?.perform("received", params)
                    }


                }

            } else {

                runBlocking {
                    Log.e(TAG, "callActionCalledRun 6")
                    isQueueRunning = false
                    currentOrderIndex = 0
                    currentPrinterIndex = 0
                    delay(5000)
                    val params = JsonObject()
                    params.addProperty("id", locationId)
                    params.addProperty("url", baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3)
                    Log.e(
                        TAG,
                        "checkID 4: ${locationId}  checkURL 4:  ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}"
                    )
                    subscription?.perform("received", params)
                }
            }

        } else {
            runBlocking {
                Log.e(TAG, "callActionCalledRun 6")
                isQueueRunning = false
                currentOrderIndex = 0
                currentPrinterIndex = 0
                delay(5000)
                val params = JsonObject()
                params.addProperty("id", locationId)
                params.addProperty("url", baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3)
                Log.e(
                    TAG,
                    "checkID 5: ${locationId}  checkURL 5:  ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}"
                )
                subscription?.perform("received", params)
            }
        }


    }

    override fun onPtrStatusChange(p0: Printer?, p1: Int) {
    }

    override fun onPtrReceive(p0: Printer?, p1: Int, p2: PrinterStatusInfo?, p3: String?) {
    }

    override fun onConnection(p0: Any?, p1: Int) {
    }

    override fun onComplete() {
    }

    override fun onFailed(p0: CloudPrinterStatus?) {
    }

    private fun sendNotification(messageBody: String) {
        Log.d("sendNotification","message = $messageBody")


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


    private fun sendDataToPrintToSunmi() {
        var cloudPrinter: CloudPrinter = CloudPrinterBuilder.buildPrinter(
            listOfPrintersData.get(currentPrinterIndex).printerName,
            listOfPrintersData.get(currentPrinterIndex).ipAddress,
            listOfPrintersData.get(currentPrinterIndex).portNo
        )

        var checkPassOrder = false
        cloudPrinter.connect(mContext, object : ConnectCallback {
            override fun onConnect() {
                Log.e(TAG, "onConnected 11")
                currentCloudPrinter = cloudPrinter
                sendReceiptToPrintSunmi(cloudPrinter)

            }

            override fun onFailed(p0: String?) {
                Log.e(TAG, "connectionFailed 11  ${p0}")
                if (checkPassOrder == false) {
                    checkPassOrder = true
                    sendNotification("Printer - ${cloudPrinter.cloudPrinterInfo.name}  " + p0)
                    checkForNextOrder(true)
                }
            }

            override fun onDisConnect() {
                Log.e(TAG, "disconnected 11")
                if (checkPassOrder == false) {
                    checkPassOrder = true
                    checkForNextOrder(true)
                }

            }

        })


    }

    private fun sendReceiptToPrintSunmi(cloudPrinter: CloudPrinter) {

        var obj =
            listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.get(currentOrderIndex)

        cloudPrinter.lineFeed(1)
        cloudPrinter.setUnderlineMode(UnderlineStyle.EMPTY)
        cloudPrinter.setBoldMode(true)
        cloudPrinter.setAlignment(AlignStyle.CENTER)
        cloudPrinter.setCharacterSize(2, 2)
        cloudPrinter.printText("Order ID:" + obj.orderID)

        cloudPrinter.lineFeed(1)

        cloudPrinter.setAlignment(AlignStyle.CENTER)
        cloudPrinter.printText(obj.orderType)


        if (obj.deliveryType.isNotEmpty()){
            cloudPrinter.lineFeed(1)
            cloudPrinter.setAlignment(AlignStyle.CENTER)
            cloudPrinter.printText(obj.deliveryType)
        }


        cloudPrinter.lineFeed(1)
        cloudPrinter.setBoldMode(false)
        cloudPrinter.setCharacterSize(1, 1)
        cloudPrinter.setAlignment(AlignStyle.LEFT)
        cloudPrinter.printText("Employee:" + obj.employeeName)

        cloudPrinter.setCharacterSize(1, 1)

        cloudPrinter.setAlignment(AlignStyle.LEFT)
        cloudPrinter.printText(obj.dateAndTime)


        cloudPrinter.let { addDoubleDotLineForSunmiQueue(it) }

        if (obj.orderType.equals(
                Constants.DINE_IN,
                true
            ) || obj.orderType.equals(Constants.DINE_IN_SPACE, true)
        ) {

            Log.e(TAG, "checkInside DINEIN")
            cloudPrinter.let { printGuestByItemForSunmiQueue(obj.guestAttributes, it) }


        } else {
            Log.e(TAG, "checkInside TAKEOUT")
            for (i in 0 until obj.orderItems.size) {

                cloudPrinter.setBoldMode(false)
                cloudPrinter.setCharacterSize(2, 2)
                cloudPrinter.setAlignment(AlignStyle.LEFT)

                if (obj.orderItems[i].timestamp.isNotEmpty()) {
                    var msg = "(" + obj.orderItems[i].timestamp + ")"
                    cloudPrinter.printText("" + obj.orderItems[i].quantity + " " + obj.orderItems[i].itemName + "  " + msg)

                } else {

                    cloudPrinter.printText("" + obj.orderItems[i].quantity + " " + obj.orderItems[i].itemName)
                }

                if (obj.orderItems[i].orderItemModifiers.isNotEmpty()) {
                    obj.orderItems[i].orderItemModifiers.forEach { mod ->


                        cloudPrinter.printText(
                            "  " + if (mod.modifierQuantity == 1) {
                                "   "
                            } else {
                                "" + mod.modifierQuantity + "x "
                            } + mod.name
                        )


                    }


                }
                if (obj.orderItems.get(i).note != null && obj.orderItems.get(i).note.isNotEmpty()) {
                    cloudPrinter.printText("  Note:" + obj.orderItems.get(i).note)
                }

            }
        }

        if (obj.orderNote.isNotEmpty()) {

            cloudPrinter?.lineFeed(1)
            cloudPrinter?.setCharacterSize(2, 1)
            cloudPrinter?.setBoldMode(true)
            cloudPrinter?.printText("Order Note:-" + obj.orderNote)
        }

        Log.e(TAG, "customerName:  ${obj.customerName}")
        if (obj.orderType != Constants.DINE_IN && obj.orderType != Constants.DINE_IN_SPACE && obj.customerName != null && obj.customerName.isNotEmpty()) {
            cloudPrinter.let { addDoubleDotLineForSunmiQueue(it) }
            cloudPrinter.lineFeed(1)
            cloudPrinter.printText("Customer Details:")
            cloudPrinter.setBoldMode(false)
            cloudPrinter.setCharacterSize(1, 1)
            cloudPrinter.setAlignment(AlignStyle.LEFT)
            cloudPrinter.printText(obj.customerName)
            if (obj.customerPhoneNo.isNotEmpty()) {
                cloudPrinter.printText(obj.customerPhoneNo)
            }
            if (obj.customerAddress.isNotEmpty()) {
                cloudPrinter.printText(obj.customerAddress)
            }


        }


        cloudPrinter.lineFeed(3)
        cloudPrinter.cutPaper(true)

        var flagIsComplete: Boolean = false

        if (checkConnect(cloudPrinter)) {
            Log.e(TAG, "checkCommitResultForCloud")
            cloudPrinter.commitTransBuffer(object : ResultCallback {
                override fun onComplete() {
                    //isQueueRunning = false
                    if (flagIsComplete == false) {
                        flagIsComplete = true


                        val params = JsonObject()
                        var deleteUrl =
                            baseUrl + Constants.DELETE_QUEUE_ORDER_PHASE3 + listOfPrintersData.get(
                                currentPrinterIndex
                            ).printerQueueModelList.get(currentOrderIndex).id

                        Log.e(TAG, "DeleteUrl ${deleteUrl}")
                        Log.e(
                            TAG,
                            "checkDeleteSunmi  ${listOfPrintersData.get(currentPrinterIndex).printerName}"
                        )
                        params.addProperty("url", deleteUrl)
                        params.addProperty(
                            "name",
                            listOfPrintersData.get(currentPrinterIndex).printerName
                        )
                        subscription?.perform("delete_order", params)

                        runBlocking {
                            delay(1000)
                            checkForNextOrder()

                        }
                    }

                }

                override fun onFailed(p0: CloudPrinterStatus?) {

                    if (p0?.name.equals("RUNNING", true) == false) {
                        //isQueueRunning = false
                        if (p0?.name.equals("OUT_PAPER", true)) {

                            sendNotification("Please fill the Paper in ${cloudPrinter.cloudPrinterInfo?.name}")
                        } else if (p0?.name.equals("COVER", true)) {
                            sendNotification("Please close the cover of ${cloudPrinter.cloudPrinterInfo?.name}")

                        } else if (p0?.name.equals("UNKNOWN", true)) {
                            sendNotification(
                                "Printer - ${
                                    cloudPrinter.cloudPrinterInfo?.name
                                } is Offline."
                            )

                        }

                        checkForNextOrder()
                    }
                }

            })

        } else {
            checkForNextOrder()
        }


    }

    private fun checkForNextOrder(isCurrentPrinterFailed: Boolean = false) {
        Log.e(TAG, "checkListlistOfPrintersData: ${listOfPrintersData.size}")

        if (listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.isNotEmpty() && listOfPrintersData.get(
                currentPrinterIndex
            ).printerQueueModelList.size - 1 > currentOrderIndex
            && isCurrentPrinterFailed == false
        ) {
            currentOrderIndex += 1
            sendDataToPrintToSunmi()

        } else {
            Log.e(TAG, "checkELseLAs")

            if (listOfPrintersData.size - 1 > currentPrinterIndex) {
                var isBreakIn = false
                for (i in currentPrinterIndex + 1 until listOfPrintersData.size) {
                    if (listOfPrintersData.get(i).printerQueueModelList.isNotEmpty()) {
                        isBreakIn = true
                        currentPrinterIndex = i
                        currentOrderIndex = 0
                    }

                }

                if (isBreakIn) {
                    sendDataToPrintToSunmi()
                } else {
                    //call action cable here
                    runBlocking {


                        Log.e(TAG, "callActionCalledRun 1")
                        isQueueRunning = false
                        currentOrderIndex = 0
                        currentPrinterIndex = 0
                        delay(2000)

                        val params = JsonObject()
                        params.addProperty("id", locationId)
                        params.addProperty(
                            "url",
                            baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3
                        )
                        Log.e(
                            TAG,
                            "checkID 6: ${locationId}  checkURL 6:  ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}"
                        )
                        subscription?.perform("received", params)
                    }
                }

            } else {

                //call action cable here
                runBlocking {


                    Log.e(TAG, "callActionCalledRun 2")
                    isQueueRunning = false
                    currentOrderIndex = 0
                    currentPrinterIndex = 0
                    delay(2000)

                    val params = JsonObject()
                    params.addProperty("id", locationId)
                    params.addProperty(
                        "url",
                        baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3
                    )
                    Log.e(
                        TAG,
                        "checkID 7: ${locationId}  checkURL 7:  ${baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3}"
                    )
                    subscription?.perform("received", params)
                }

            }
        }

    }


    /*else if (listOfPrintersData.size - 1 == currentPrinterIndex) {
        if (listOfPrintersData.get(currentPrinterIndex).printerQueueModelList.isNotEmpty() && listOfPrintersData.get(
                currentPrinterIndex
            ).printerQueueModelList.size - 1 > currentOrderIndex
        ) {

            currentOrderIndex += 1
            sendDataToPrintToSunmi()

        } else {

            //call action cable here
            runBlocking {


                delay(2000)

                val params = JsonObject()
                params.addProperty("id", locationId)
                params.addProperty(
                    "url",
                    baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3
                )
                subscription?.perform("received", params)
            }
        }
    }*/


    private fun checkConnect(cloudPrinter: CloudPrinter): Boolean {
        if (cloudPrinter == null) {
            sendNotification("Please connect the cloud printer first!")
            return false
        }
        if (!cloudPrinter.isConnected()) {
            sendNotification("Please connect the ${cloudPrinter.cloudPrinterInfo.name} printer first!")
            return false
        }
        return true
    }

    fun connectActionCableSYNCSETTINGS() {
        // 1. Setup
        var requestURL =
            baseUrl + Constants.CREATE_QUEUE_PRINTER_PHASE3

        Log.d("PrinterRefreshWorker", "requestURL = $requestURL")

        val uri = URI(Constants.PRINTER_QUEUE_CONNECTION_URL_SNACKPOS)
        consumer2 = ActionCable.createConsumer(uri)

        Log.d("PrinterRefreshWorker", "uri = $uri")

        // 2. Create subscription
        val appearanceChannel = Channel("SyncChannel")
        appearanceChannel.addParam("id", locationId)
        // appearanceChannel.addParam("id",prefProvider.getValueInt(LOCATION_ID,0))
        subscription2 = consumer2?.subscriptions?.create(appearanceChannel)

        if (subscription2 != null) {
            subscription2?.onConnected {
                Log.e(TAG2, "onActionConnected")
                val params = JsonObject()
                params.addProperty("location_id", locationId)
                //  params.addProperty("url", requestURL)
                subscription2?.perform("received", params)
            }?.onRejected {
                Log.e(TAG2, "onRejected")
                if (isInternetAvailable()) {
                    consumer2?.connect()
                } else {
                    sendNotification("Please check your Network Connectivity.")
                }

            }?.onReceived {
                Log.e(TAG2, "onReceived  " + Gson().toJson(it))

                handleUpdatedData(it)


            }?.onDisconnected {


                Log.e(TAG2, "onDisconnected")
                if (isInternetAvailable()) {
                    consumer2?.connect()
                } else {
                    sendNotification("Please check your Network Connectivity.")
                }

            }?.onFailed {
                Log.e(TAG2, "onFailed")
                if (isInternetAvailable()) {
                    consumer2?.connect()
                } else {
                    sendNotification("Please check your Network Connectivity.")
                }

            }
        }


        // 3. Establish connection
        consumer2?.connect()


    }

    private fun handleUpdatedData(it: JsonElement) {

        try {
            if (it.asJsonObject.has("setting_data")) {

                val setting_data = it.asJsonObject.get("setting_data")
                Log.e(TAG2, "call setting_data API")

                if (setting_data.toString() == "true") {
                    if (com.android.pos.ui.fragments.settings.hardware.printer.Printer.updatePrinter == null) {

                        MainActivity.updatePrinter?.updatePrinters()

                    } else {
                        com.android.pos.ui.fragments.settings.hardware.printer.Printer.updatePrinter?.updatePrinters()
                    }

                } else {

                    if (it.asJsonObject.has("message")) {

                        sendNotification(it.asJsonObject.get("message").asString)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG2, "Exception")
        }
    }

    private fun isInternetAvailable(): Boolean {
        var result: Boolean
        val connectivityManager =
            applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        connectivityManager.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                it.getNetworkCapabilities(connectivityManager.activeNetwork)?.apply {
                    result = when {
                        hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                        hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                        else -> false
                    }
                    return result
                }
            } else {
                connectivityManager.activeNetworkInfo.also {
                    return it != null && it.isConnected
                }
            }
        }
        return false
    }

}
