package com.android.pos.utils.workmanager

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.android.pos.data.model.PrinterQueueModel
import com.android.pos.data.model.responseModel.CreateOrderResponse
import com.android.pos.data.model.responseModel.GetKitchenReceiptSettingsResponse
import com.android.pos.data.model.responseModel.PrinterResponse
import com.android.pos.data.remote.Constants
import com.android.pos.utils.addBuilderText
import com.android.pos.utils.addHorizontalLine
import com.android.pos.utils.addOrdersForKitchenCustomer
import com.android.pos.utils.padLine
import com.android.pos.utils.printer.PrinterClass
import com.epson.eposprint.Builder
import com.epson.eposprint.Print
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.hosopy.actioncable.ActionCable
import com.hosopy.actioncable.Channel
import com.hosopy.actioncable.Consumer
import com.hosopy.actioncable.Subscription
import org.jetbrains.annotations.NotNull
import java.net.URI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class UploadWorker(@NotNull context: Context, @NotNull params: WorkerParameters) :
    Worker(context, params) {
    private var globalPrinterQueue: JsonElement?=null
    private val TAG = UploadWorker::class.java.name


    private var subscription: Subscription? = null
    private var consumer: Consumer? = null
    private var locationId: Int = 0
    private var baseUrl = ""
    private var printerQueuelist: ArrayList<PrinterQueueModel> = arrayListOf()
    private var kitchenPrinterList: List<PrinterResponse.Data.KitchenReceiptPrinters> = listOf()
    private var kitchenSettingModel = GetKitchenReceiptSettingsResponse.Data()
    private var mContext: Context = context
    private var isPrinterRunning: Boolean = false
    override fun doWork(): Result {

        locationId = inputData.getInt("location_id", 0)
        baseUrl = inputData.getString("base_url").toString()
        var serializeObjKitchenPrinters = inputData.getString("kitchenPrinterList")
        Log.e(TAG, "serializeObjKitchenPrinters  ${Gson().toJson(serializeObjKitchenPrinters)}")
        if (serializeObjKitchenPrinters?.isNotEmpty() == true) {
            val gson = Gson()
            val type =
                object : TypeToken<List<PrinterResponse.Data.KitchenReceiptPrinters>?>() {}.type
            kitchenPrinterList =
                gson.fromJson<Any>(
                    serializeObjKitchenPrinters,
                    type
                ) as ArrayList<PrinterResponse.Data.KitchenReceiptPrinters>


        }
        connectActionCable()
        //  connectPrinter(context = applicationContext, data)


        return Result.success()

    }

    fun connectPrinter(context: Context, data: String) {
        var printer: Print = Print(context)
        if (printer != null) {
            /*  printer.setStatusChangeEventCallback(this)
              printer.setBatteryStatusChangeEventCallback(this)
              printer.setStatusChangeEventCallback(this)*/
        }

        val enabled = Print.TRUE
        try {
            printer?.openPrinter(
                Print.DEVTYPE_TCP,
                "192.168.3.201",
                enabled,
                10000
            )


        } catch (e: Exception) {

            try {

            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            Log.e(TAG, "PrinterException: " + e.message)

            return
        }

        if (printer != null) {
            PrinterClass.setPrinter(printer)

            // generateKitchenReceipt(data, "", printerQueueModel, index)

        }


    }


    private fun connectActionCable() {
        // 1. Setup
        var requestURL =
            baseUrl + Constants.CREATE_QUEUE_PRINTER
        Log.e(TAG, "requestURL:  ${requestURL}")
        val uri = URI("wss://hugepos.com/cable")
        consumer = ActionCable.createConsumer(uri)

        // 2. Create subscription
        val appearanceChannel = Channel("KitchenChannel")
        // appearanceChannel.addParam("id",prefProvider.getValueInt(LOCATION_ID,0))
        subscription = consumer?.subscriptions?.create(appearanceChannel)

        if (subscription != null) {
            subscription?.onConnected {
                Log.e(TAG, "onActionConnected")
                val params = JsonObject()
                params.addProperty("id", locationId)
                params.addProperty("url", requestURL)
                subscription?.perform("received", params)
            }?.onRejected {
                Log.e(TAG, "onActiononRejected")
                subscription = consumer?.subscriptions?.create(appearanceChannel)
                val params = JsonObject()
                params.addProperty("id", locationId)
                subscription?.perform("received", params)
            }?.onReceived {
                Log.e(TAG, "onActiononReceived  " + Gson().toJson(it))
                if (it != null && !isPrinterRunning) {

                    if (it.asJsonObject.has("printer_queue")) {
                        isPrinterRunning = true
                        globalPrinterQueue = it.asJsonObject.get("printer_queue")
                        getQueueDataResponse(it.asJsonObject.get("printer_queue"))


                    } else {
                        isPrinterRunning = false
                        Log.e(TAG, "NoPrinterQueueData")
                        /* val dailyWorkRequest = OneTimeWorkRequest.Builder(UploadWorker::class.java)
                             .setInitialDelay(5, TimeUnit.SECONDS)
                             .addTag("empty_data")
                             .build()
                         WorkManager.getInstance(applicationContext)
                             .enqueue(dailyWorkRequest)*/
                    }

                }

            }?.onDisconnected {
                Log.e(TAG, "onActiononDisconnected")
                subscription = consumer?.subscriptions?.create(appearanceChannel)
                val params = JsonObject()
                params.addProperty("id", locationId)
                subscription?.perform("received", params)
            }?.onFailed {
                Log.e(TAG, "onActiononFailed")
                //subscription = consumer?.subscriptions?.create(appearanceChannel)
                val params = JsonObject()
                params.addProperty("id", locationId)
                subscription?.perform("received", params)

            }
        }


        // 3. Establish connection
        consumer?.connect()


    }

    private fun getQueueDataResponse(model: JsonElement) {
        if (model.asJsonObject.has("data")) {


            var dataList = model.asJsonObject.get("data").asJsonArray

            printerQueuelist.clear()
            printerQueuelist = arrayListOf()

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
                    printerQueueModel.orderID = ""+it.asJsonObject.get("orderid").asInt
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


                    printerQueuelist.add(printerQueueModel)
                }


            }
            if (printerQueuelist.size != 0) {
                for (i in 0 until printerQueuelist.size) {

                    configurePrinter(printerQueuelist.get(i), i)
                }
            } else {
                isPrinterRunning = false
            }
        }
    }

    private fun configurePrinter(printerQueueModel: PrinterQueueModel, pos: Int) {


        kitchenPrinterList.forEachIndexed { index, it ->


            initKitchenPrinter(it, printerQueueModel, pos)

        }
        if (kitchenPrinterList.size == 0) {
            isPrinterRunning = false
        }


    }

    private fun initKitchenPrinter(
        data: PrinterResponse.Data.KitchenReceiptPrinters,
        printerQueueModel: PrinterQueueModel,
        index: Int
    ) {

        PrinterClass.setPrinter(null)

        var printer: Print? = Print(mContext)
        /*  if (printer != null) {
              printer.setStatusChangeEventCallback(this)
              printer.setBatteryStatusChangeEventCallback(this)
              printer.setStatusChangeEventCallback(this)
          }*/

        val enabled = Print.FALSE

        try {
            printer?.openPrinter(
                if (data.printer_type == Constants.BLUETOOTH) {
                    Print.DEVTYPE_BLUETOOTH
                } else {
                    Print.DEVTYPE_TCP
                },
                data.ipAddress
            )


        } catch (e: Exception) {
            isPrinterRunning = false

            Log.e(TAG, "PrinterException: " + e.message)
            printer = null
            globalPrinterQueue?.let { getQueueDataResponse(it) }


        }

        if (printer != null) {
            Log.e(TAG,"PrinterIsNotConnected")
            PrinterClass.setPrinter(printer)

            generateKitchenReceipt(data, "", printerQueueModel, index)

        }


    }

    private fun generateKitchenReceipt(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        printerQueueModel: PrinterQueueModel,
        index: Int
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

            builder = Builder(pname, PrinterClass.language, mContext)


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

            addBuilderText(builder, printerQueueModel.orderType)

            if (printerQueueModel?.data?.order_type?.toString()?.lowercase() == "OpenOrder".trim()
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
            }

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

            if (printerQueueModel?.data?.order_data?.note?.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
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
            }

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


            try {
                PrinterClass.getPrinter()?.sendData(
                    builder,
                    10000, status
                )

                PrinterClass.closePrinter()

                printerQueueModel.id?.let {
                    val params = JsonObject()
                    var deleteUrl = baseUrl + Constants.CREATE_QUEUE_PRINTER + "/" + it
                    Log.e(TAG, "DeleteUrl ${deleteUrl}")
                    params.addProperty("url", deleteUrl)
                    subscription?.perform("delete_order", params)

                    /* viewModel.deleteQueuePrinter(
                         it,
                         printerQueueModel.position
                     )*/
                }

            isPrinterRunning = false
                //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
            } catch (e: Exception) {


                PrinterClass.closePrinter()
                /* printerQueueModel.id?.let {
                     val params = JsonObject()
                     var deleteUrl = baseUrl + Constants.CREATE_QUEUE_PRINTER + "/" + it
                     Log.e(TAG, "DeleteUrl ${deleteUrl}")
                     params.addProperty("url", deleteUrl)
                     subscription?.perform("delete_order", params)

                     *//* viewModel.deleteQueuePrinter(
                         it,
                         printerQueueModel.position
                     )*//*
                }*/
                e.printStackTrace()
                val params = JsonObject()
                params.addProperty("id", locationId)
                subscription?.perform("received", params)
                //isPrinterRunning = false

            }


        } catch (e: Exception) {
            isPrinterRunning = false

            e.printStackTrace()
        }


    }
}


