package com.android.pos.ui.fragments.settings.hardware.printerqueue

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.android.pos.R
import com.android.pos.data.model.PrinterQueueModel
import com.android.pos.data.model.responseModel.CreateOrderResponse
import com.android.pos.data.model.responseModel.GetKitchenReceiptSettingsResponse
import com.android.pos.data.model.responseModel.PrinterResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.CREATE_QUEUE_PRINTER
import com.android.pos.data.remote.Constants.IN_PROCESS
import com.android.pos.data.remote.Constants.LOCATION_ID
import com.android.pos.data.remote.Constants.PENDING
import com.android.pos.databinding.FragmentPrinterQueueBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.PrinterQueueListAdapter
import com.android.pos.utils.*
import com.android.pos.utils.extensions.alert
import com.android.pos.utils.printer.PrinterClass
import com.android.pos.utils.statusUtils.Status
import com.android.pos.utils.workmanager.UploadWorker
import com.epson.eposprint.BatteryStatusChangeEventListener
import com.epson.eposprint.Builder
import com.epson.eposprint.Print
import com.epson.eposprint.StatusChangeEventListener
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.hosopy.actioncable.ActionCable
import com.hosopy.actioncable.Channel
import com.hosopy.actioncable.Consumer
import com.hosopy.actioncable.Subscription
import dagger.hilt.android.AndroidEntryPoint
import java.net.URI
import java.util.concurrent.TimeUnit
import javax.inject.Inject


@AndroidEntryPoint
class PrinterQueue : Fragment(), StatusChangeEventListener, BatteryStatusChangeEventListener {
    private var printerQueuelist: ArrayList<PrinterQueueModel> = arrayListOf()
    private var subscription: Subscription? = null
    private var consumer: Consumer? = null
    private lateinit var binding: FragmentPrinterQueueBinding
    private val list: ArrayList<PrinterQueueModel> = arrayListOf()
    private lateinit var adapter: PrinterQueueListAdapter
    private val TAG = "PrinterQueue"
    private var kitchenSettingModel = GetKitchenReceiptSettingsResponse.Data()
    private var isPrintRunning: Boolean = false

    @Inject
    lateinit var prefProvider: PrefProvider

    private val viewModel by viewModels<PrinterQueueViewModel>()
    private var kitchenPrinterList: List<PrinterResponse.Data.KitchenReceiptPrinters> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPrinterQueueBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        PrinterClass.setPrinter(null)
        adapter = PrinterQueueListAdapter()


        getKitchenReceiptSettings()
        observeShowProgress()
        deleteQueueItemObserver()
        deleteAllQueueObserver()
        getKitchenPrinters()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setAdapter()
        onClick()
        connectActionCable()


    }


    private fun connectActionCable() {
        // 1. Setup
        var requestURL = prefProvider.getValue(Constants.BASE_URL_NEW, "") + CREATE_QUEUE_PRINTER
        val uri = URI(Constants.PRINTER_QUEUE_CONNECTION_URL_SNACKPOS)
        consumer = ActionCable.createConsumer(uri)

        // 2. Create subscription
        val appearanceChannel = Channel("printer_queue_data_channel_")
        // appearanceChannel.addParam("id",prefProvider.getValueInt(LOCATION_ID,0))
        subscription = consumer?.subscriptions?.create(appearanceChannel)

        if (subscription != null) {
            subscription?.onConnected {
                LogUtil.logE(TAG, "onActionConnected")
                val params = JsonObject()
                params.addProperty("id", prefProvider.getValueInt(LOCATION_ID, 0))
                params.addProperty("url", requestURL)
                subscription?.perform("received", params)
            }?.onRejected {
                LogUtil.logE(TAG, "onActiononRejected")
            }?.onReceived {
                LogUtil.logE(TAG, "onActiononReceived  " + Gson().toJson(it))
                if (it != null && !isPrintRunning) {

                    if (it.asJsonObject.has("printer_queue")) {
                        isPrintRunning = true


                        getQueueDataResponse(it.asJsonObject.get("printer_queue"))


                    }

                }

            }?.onDisconnected {
                LogUtil.logE(TAG, "onActiononDisconnected")
            }?.onFailed {
                LogUtil.logE(TAG, "onActiononFailed")
                subscription = consumer?.subscriptions?.create(appearanceChannel)
                val params = JsonObject()
                params.addProperty("id", prefProvider.getValueInt(LOCATION_ID, 0))
                subscription?.perform("received", params)

            }
        }


        // 3. Establish connection
        consumer?.connect();


    }

    override fun onPause() {
        consumer?.disconnect()
        super.onPause()
    }

    private fun getQueueDataResponse(model: JsonElement) {

        var dataList = model.asJsonObject.get("data").asJsonArray

        printerQueuelist.clear()
        printerQueuelist = arrayListOf()
        dataList.forEachIndexed { index, it ->
            val printerQueueModel: PrinterQueueModel = PrinterQueueModel()

            val obj = it.asJsonObject.get("order_data").asJsonObject
            LogUtil.logE(TAG, "getOrderData: ${Gson().toJson(obj)}")

            if (obj.asJsonObject.has("order_items_attributes")) {
                var itemArray = obj.asJsonObject.get("order_items_attributes").asJsonArray
                var itemAttribute: ArrayList<CreateOrderResponse.Data.Order.OrderItem> =
                    arrayListOf()
                var itemModifiers: ArrayList<CreateOrderResponse.Data.Order.OrderItem.OrderItemModifiers> =
                    arrayListOf()


                itemArray.forEach {
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
                printerQueueModel.orderType = obj.asJsonObject.get("open_order_type").asString
                printerQueueModel.id = it.asJsonObject.get("id").asInt
                printerQueueModel.offlineId = obj.asJsonObject.get("offline_id").asString
                printerQueueModel.paymentType = "Cash"
                printerQueueModel.status = PENDING
                printerQueueModel.totalAmt = obj.asJsonObject.get("total_amount").asDouble
                printerQueueModel.orderID = ""
                //obj.asJsonObject.get("terminal_name")?.asString ?: ""
                printerQueueModel.position = index


                printerQueuelist.add(printerQueueModel)
            }


        }

        try {
            requireActivity().runOnUiThread {
                if (printerQueuelist.isNotEmpty()) {

                    adapter.clearList()
                    binding.rvPrinterQueueList.adapter = adapter

                    adapter.setList(printerQueuelist)
                    adapter.notifyDataSetChanged()


                } else {
                    adapter.clearList()
                    isPrintRunning = false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }






        if (printerQueuelist.size != 0) {
            for (i in 0 until printerQueuelist.size) {

                 configurePrinter(printerQueuelist.get(i), i)
            }
        }
        /*printerQueuelist.forEachIndexed { index, printerQueueModel ->
            configurePrinter(printerQueueModel, index)
        }*/

    }

    private fun configurePrinter(printerQueueModel: PrinterQueueModel, pos: Int) {

        activity?.runOnUiThread {
            adapter.updatePrintStatus(0, IN_PROCESS)
            adapter.notifyDataSetChanged()
        }
        kitchenPrinterList.forEachIndexed { index, it ->


            initKitchenPrinter(it, printerQueueModel, pos)

        }


    }

    @SuppressLint("RestrictedApi")
    private fun onClick() {
        binding.btnStartService?.setOnClickListener {
            LogUtil.logE(TAG, "kitchenPrinterSize:  ${kitchenPrinterList.size}")
            val data = Data.Builder()
                .putString("kitchenPrinterList", Gson().toJson(kitchenPrinterList))
                .put("kitchenSettingData", Gson().toJson(kitchenSettingModel))
                .put("location_id", prefProvider.getValueInt(LOCATION_ID, 0))
                .put("base_url", prefProvider.getValue(Constants.BASE_URL_NEW, ""))
                .build()


            /*val uploadWorkRequest =
                OneTimeWorkRequest.Builder(UploadWorker::class.java).setInputData(data).build()*/


            val uploadWorkRequest =
                PeriodicWorkRequest.Builder(UploadWorker::class.java, 5, TimeUnit.SECONDS)
                    .setInputData(data)
                    .build()


            val workManager = WorkManager.getInstance(requireActivity().applicationContext)
            try {


                workManager.enqueueUniquePeriodicWork(
                    "demo",
                    ExistingPeriodicWorkPolicy.REPLACE,
                    uploadWorkRequest
                )
            } catch (e: java.lang.Exception) {
                LogUtil.logE(TAG, "printerQueueLog  ${e.message.toString()}")
                e.printStackTrace()
            }
            // workManager.enqueueUniquePeriodicWork(System.currentTimeMillis().toString(),ExistingPeriodicWorkPolicy.KEEP,uploadWorkRequest)


            //workManager.enqueue(uploadWorkRequest)

        }
        binding.imgSync.setOnClickListener {
            //var printerQueuelist = adapter.getList()

            if (printerQueuelist.isNotEmpty() && !isPrintRunning) {
                for (i in 0 until printerQueuelist.size) {
                    configurePrinter(printerQueuelist[i], i)
                }
            }

            /*   printerQueuelist.forEachIndexed { index, printerQueueModel ->
                   configurePrinter(printerQueueModel, index)
               }*/

        }
        binding.txtDelete.setOnClickListener {
            val list = adapter.getList()
            var ids: ArrayList<Int> = arrayListOf()
            list.forEachIndexed { index, printerQueueModel ->
                printerQueueModel.id?.let { it1 -> ids.add(it1) }
            }


            if (ids.isNotEmpty()) {
                var deleteQueueIds: Array<Int> = ids.toTypedArray()
                viewModel.deleteAllQueuePrinter(deleteQueueIds)
            }

        }
        binding.imgClose.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.txtHome.setOnClickListener {
            findNavController().navigate(R.id.action_printerQueue_to_dashboardCategoryNew)
        }
    }

    private fun setAdapter() {


        object : SwipeHelper(activity, binding.rvPrinterQueueList) {
            override fun instantiateUnderlayButton(
                viewHolder: RecyclerView.ViewHolder?,
                underlayButtons: MutableList<UnderlayButton>?
            ) {
                underlayButtons?.add(
                    UnderlayButton(
                        "Delete",
                        ContextCompat.getColor(context, R.color.swipe_text_color),
                        ContextCompat.getColor(context, R.color.swipe_bg_delete)
                    ) {
                        LogUtil.logE(TAG, "position  ${it}")
                        adapter.getList().get(it).id?.let { it1 -> deletePrinterQueue(it1, it) }
                    })
            }

        }

    }

    private fun getKitchenPrinters() {
        viewModel.getKitchenPrinterList().observe(viewLifecycleOwner) { it ->
            when (it.status) {
                Status.SUCCESS -> {
                    ProgressUtils.dismissProgressDialog()
                    if (it.data != null) {
                        kitchenPrinterList = it.data


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

    private fun observeShowProgress() {

        viewModel.showProgress.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        })
    }

    private fun deletePrinterQueue(id: Int, pos: Int) {
        alert(
            getString(R.string.tv_pos),
            getString(R.string.delete_printer_message)
        ) {
            positiveButton(getString(R.string.tv_delete)) {
                viewModel.deleteQueuePrinter(id, pos)

            }
            negativeButton(R.string.tv_cancel) {

            }
        }
    }

    private fun initKitchenPrinter(
        data: PrinterResponse.Data.KitchenReceiptPrinters,
        printerQueueModel: PrinterQueueModel,
        index: Int
    ) {


        var printer: Print? = Print(requireContext())
        if (printer != null) {
            printer.setStatusChangeEventCallback(this)
            printer.setBatteryStatusChangeEventCallback(this)
            printer.setStatusChangeEventCallback(this)
        }

        val enabled = Print.TRUE

        try {
            printer?.openPrinter(
                if (data.printer_type == Constants.BLUETOOTH) {
                    Print.DEVTYPE_BLUETOOTH
                } else {
                    Print.DEVTYPE_TCP
                },
                data.ipAddress,
                enabled,
                10000
            )


        } catch (e: Exception) {
            isPrintRunning = false
            try {
                requireActivity().runOnUiThread {
                    adapter.updatePrintStatus(0, "FAILED")
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            LogUtil.logE(TAG, "PrinterException: " + e.message)
            printer = null
            return
        }

        if (printer != null) {
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
            LogUtil.logE(TAG, "KitchenPrinterName ${customerReceiptPrinters.name}")
            val pname = if (customerReceiptPrinters.name.substring(0, 6).toString()
                    .lowercase() == "TM-m30".lowercase()
            ) {
                "TM-m30"
            } else {
                customerReceiptPrinters.name
            }

            builder = Builder(pname, PrinterClass.language, requireActivity())
            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)
            builder.addFeedLine(2)
            if (kitchenSettingModel.showOrderType) {


                builder.addFeedLine(0)
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

                addBuilderText(builder, printerQueueModel?.orderType)
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
                    "OrderID:" + printerQueueModel.orderIdN,
                    "",
                    33
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
                    33
                )
            )
            if (kitchenSettingModel.showTeamMember) {

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
                        "Employee:" + "", "",
                        33
                    )
                )

            }
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


            /*  builder.addText(
                  padLine(
                      Constants.getReceiptFormatDateFromUTCServer(receiptModel?.order?.createdAt.toString()),
                      "",
                      33
                  )
              )*/

            builder.addFeedLine(1)

            builder.addTextFont(Builder.FONT_B)
            //builder.addTextLineSpace(20)
            builder.addTextLang(Builder.LANG_EN)
            builder.addTextSize(1, 1)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )

            addHorizontalKitchenLine(builder)

            printerQueueModel?.orderItems?.let { addOrdersForKitchen(builder, it) }

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


            if (kitchenSettingModel.showCustomerAddress != false or kitchenSettingModel.showCustomerPhone != false or kitchenSettingModel.showCustomerName) {
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
            }

            builder.addFeedLine(2)

            builder.addCut(Builder.CUT_FEED)

            val status = IntArray(1)
            val battery = IntArray(1)


            try {
                PrinterClass.getPrinter()?.sendData(
                    builder,
                    PrinterClass.TEST_PRINT_LAN_TIME, status, battery
                )

                PrinterClass.closePrinter()

                printerQueueModel.id?.let {
                    val params = JsonObject()
                    var deleteUrl = prefProvider?.getValue(Constants.BASE_URL_NEW, "") + Constants.CREATE_QUEUE_PRINTER + "/" + it
                    LogUtil.logE(TAG, "DeleteUrl ${deleteUrl}")
                    params.addProperty("url", deleteUrl)
                    subscription?.perform("delete_order", params)

                    /* viewModel.deleteQueuePrinter(
                         it,
                         printerQueueModel.position
                     )*/
                }




                //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
            } catch (e: Exception) {

                isPrintRunning = false
                PrinterClass.closePrinter()
                e.printStackTrace()
                /*val params = JsonObject()
                params.addProperty("id", prefProvider.getValueInt(LOCATION_ID, 0))
                subscription?.perform("received", params)*/

            }


        } catch (e: Exception) {

            isPrintRunning = false
            e.printStackTrace()
        }


    }

    override fun onStatusChangeEvent(p0: String?, p1: Int) {

    }

    private fun getKitchenReceiptSettings() {
        viewModel.getKitchenReceiptSettings().observe(viewLifecycleOwner, {

            if (it != null) {
                kitchenSettingModel = it
                LogUtil.logE(TAG, "getKitchenData")
                getKitchenPrinters()
            }
        })
    }

    override fun onBatteryStatusChangeEvent(p0: String?, p1: Int) {

    }

    private fun deleteQueueItemObserver() {
        viewModel.deleteQueue.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { position ->
                requireActivity().runOnUiThread {
                    adapter.removeItemAt(0)
                    adapter.notifyDataSetChanged()
                    isPrintRunning = false
                    val params = JsonObject()
                    params.addProperty("id", prefProvider.getValueInt(LOCATION_ID, 0))
                    subscription?.perform("received", params)
                }
                /*
  */

            }
        })
    }

    private fun deleteAllQueueObserver() {
        viewModel.deleteAllQueue.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { data ->
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, data.toString()
                    ) { _, _ ->
                        requireActivity().runOnUiThread {
                            adapter.clearList()
                        }
                        val params = JsonObject()
                        params.addProperty("id", prefProvider.getValueInt(LOCATION_ID, 0))
                        subscription?.perform("received", params)

                    }
                }

            }
        })
    }

    private fun buildInputDataForFilter(): Data {
        val builder = Data.Builder()
        builder.putString("itemName", "Pizza")

        return builder.build()
    }

}