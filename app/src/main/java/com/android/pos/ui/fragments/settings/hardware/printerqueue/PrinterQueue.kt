package com.android.pos.ui.fragments.settings.hardware.printerqueue

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.Modifier
import com.android.pos.data.entities.TaxData
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.DineInModel
import com.android.pos.data.model.PrinterQueueModel
import com.android.pos.data.model.responseModel.CreateOrderResponse
import com.android.pos.data.model.responseModel.GetKitchenReceiptSettingsResponse
import com.android.pos.data.model.responseModel.PrinterResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.LOCATION_ID
import com.android.pos.data.remote.Constants.PENDING
import com.android.pos.databinding.FragmentPrinterQueueBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.PrinterQueueListAdapter
import com.android.pos.utils.*
import com.android.pos.utils.extensions.alert
import com.android.pos.utils.printer.PrinterClass
import com.android.pos.utils.statusUtils.Status
import com.epson.eposprint.BatteryStatusChangeEventListener
import com.epson.eposprint.Builder
import com.epson.eposprint.Print
import com.epson.eposprint.StatusChangeEventListener
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.hosopy.actioncable.ActionCable
import com.hosopy.actioncable.Channel
import com.hosopy.actioncable.Consumer
import com.hosopy.actioncable.Subscription
import java.net.URI

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.google.gson.JsonObject


@AndroidEntryPoint
class PrinterQueue : Fragment(), StatusChangeEventListener, BatteryStatusChangeEventListener {
    private var subscription: Subscription? = null
    private var consumer: Consumer? = null
    private lateinit var binding: FragmentPrinterQueueBinding
    private val list: ArrayList<PrinterQueueModel> = arrayListOf()
    private lateinit var adapter: PrinterQueueListAdapter
    private val TAG = "PrinterQueue"
    private var kitchenSettingModel = GetKitchenReceiptSettingsResponse.Data()

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
        observeShowProgress()
        getKitchenReceiptSettings()
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
        val uri = URI("wss://possoft.io/cable")
        consumer = ActionCable.createConsumer(uri)

        // 2. Create subscription
        val appearanceChannel = Channel("KitchenChannel")
        // appearanceChannel.addParam("id",prefProvider.getValueInt(LOCATION_ID,0))
        subscription = consumer?.subscriptions?.create(appearanceChannel)

        if (subscription != null) {
            subscription?.onConnected {
                Log.e(TAG, "onActionConnected")
                val params = JsonObject()
                params.addProperty("id", prefProvider.getValueInt(LOCATION_ID, 0))
                subscription?.perform("received", params)
            }?.onRejected {
                Log.e(TAG, "onActiononRejected")
            }?.onReceived {
                Log.e(TAG, "onActiononReceived  " + Gson().toJson(it))
                if (it != null) {

                    if (it.asJsonObject.has("printer_queue")) {

                        getQueueDataResponse(it.asJsonObject.get("printer_queue"))

                    }

                }

            }?.onDisconnected {
                Log.e(TAG, "onActiononDisconnected")
            }?.onFailed {
                Log.e(TAG, "onActiononFailed")
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

        var printerQueuelist: ArrayList<PrinterQueueModel> = arrayListOf()
        dataList.forEach {
            val printerQueueModel: PrinterQueueModel = PrinterQueueModel()

            val obj = it.asJsonObject.get("order_data").asJsonObject
            Log.e(TAG, "getOrderData: ${Gson().toJson(obj)}")

            var itemArray = obj.asJsonObject.get("order_items_attributes").asJsonArray
            var itemAttribute: ArrayList<CreateOrderResponse.Data.Order.OrderItem> = arrayListOf()
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
            printerQueueModel.terminalName = ""
            printerQueueModel.orderType = obj.asJsonObject.get("open_order_type").asString
            printerQueueModel.id = it.asJsonObject.get("id").asInt
            printerQueueModel.offlineId = obj.asJsonObject.get("offline_id").asString
            printerQueueModel.paymentType = "Cash"
            printerQueueModel.status = PENDING
            printerQueueModel.totalAmt = obj.asJsonObject.get("total_amount").asDouble
            printerQueueModel.terminalName =
                obj.asJsonObject.get("terminal_name")?.asString ?: ""

            printerQueuelist.add(printerQueueModel)


        }
        requireActivity().runOnUiThread {
            if (printerQueuelist.isNotEmpty()) {

                adapter.setList(printerQueuelist)
            } else {
                adapter.clearList()
            }


        }




        for (i in 0 until adapter.getList().size) {
            configurePrinter(adapter.getList().get(i), i)
        }
        /*printerQueuelist.forEachIndexed { index, printerQueueModel ->
            configurePrinter(printerQueueModel, index)
        }*/

    }

    private fun configurePrinter(printerQueueModel: PrinterQueueModel, pos: Int) {
        kitchenPrinterList.forEach {

            initKitchenPrinter(it, printerQueueModel, pos)

        }

    }

    private fun onClick() {
        binding.imgSync.setOnClickListener {
            var printerQueuelist = adapter.getList()
            for (i in 0 until printerQueuelist.size) {
                configurePrinter(printerQueuelist[i], i)
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

        adapter = PrinterQueueListAdapter()
        binding.rvPrinterQueueList.adapter = adapter
        adapter.setList(list)

        object : SwipeHelper(activity, binding.rvPrinterQueueList) {
            override fun instantiateUnderlayButton(
                viewHolder: RecyclerView.ViewHolder?,
                underlayButtons: MutableList<UnderlayButton>?
            ) {
                underlayButtons?.add(UnderlayButton("Delete", 0, Color.parseColor("#FF3C30")) {
                    Log.e(TAG, "position  ${it}")
                    adapter.getList().get(it).id?.let { it1 -> deletePrinterQueue(it1, it) }
                })
            }

        }

    }

    private fun getKitchenPrinters() {
        viewModel.getKitchenPrinterList().observe(viewLifecycleOwner, { it ->
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

        })

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
        pos: Int
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
            Log.e(TAG, "PrinterException: " + e.message)
            printer = null
            return
        }

        if (printer != null) {
            PrinterClass.setPrinter(printer)

            generateKitchenReceipt(data, "", printerQueueModel, pos)

        }


    }

    private fun generateKitchenReceipt(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        printerQueueModel: PrinterQueueModel,
        pos: Int
    ) {
        var builder: Builder? = null
        try {
            Log.e(TAG, "KitchenPrinterName ${customerReceiptPrinters.name}")
            val pname = if (customerReceiptPrinters.name.substring(0, 6).toString()
                    .lowercase() == "TM-m30".lowercase()
            ) {
                "TM-m30"
            } else {
                customerReceiptPrinters.name
            }

            builder = Builder(pname, PrinterClass.language, requireActivity())

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
                    "OrderID:" + printerQueueModel.orderId,
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
                printerQueueModel.id?.let { viewModel.deleteQueuePrinter(it, pos) }

                //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
            } catch (e: Exception) {
                PrinterClass.closePrinter()
                e.printStackTrace()
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun onStatusChangeEvent(p0: String?, p1: Int) {

    }

    private fun getKitchenReceiptSettings() {
        viewModel.getKitchenReceiptSettings().observe(viewLifecycleOwner, {

            if (it != null) {
                kitchenSettingModel = it
                getKitchenPrinters()
            }
        })
    }

    override fun onBatteryStatusChangeEvent(p0: String?, p1: Int) {

    }

    private fun deleteQueueItemObserver() {
        viewModel.deleteQueue.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { position ->
                adapter.removeItemAt(0)


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
                        val params = JsonObject()
                        params.addProperty("id", prefProvider.getValueInt(LOCATION_ID, 0))
                        subscription?.perform("received", params)

                    }
                }

            }
        })
    }

}