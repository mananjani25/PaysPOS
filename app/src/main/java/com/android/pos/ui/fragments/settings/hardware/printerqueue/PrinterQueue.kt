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
        val subscription: Subscription? = consumer?.subscriptions?.create(appearanceChannel)

        if (subscription != null) {
            subscription
                .onConnected {
                    Log.e(TAG, "onActionConnected")
                    val params = JsonObject()
                    params.addProperty("id", prefProvider.getValueInt(LOCATION_ID, 0))
                    subscription.perform("received", params)
                }.onRejected {
                    Log.e(TAG, "onActiononRejected")
                }.onReceived {
                    Log.e(TAG, "onActiononReceived  " + Gson().toJson(it))
                    if (it != null) {

                        if (it.asJsonObject.has("printer_queue")) {
                            requireActivity().runOnUiThread {
                                getQueueDataResponse(it.asJsonObject.get("printer_queue"))
                            }
                        }

                    }

                }.onDisconnected {
                    Log.e(TAG, "onActiononDisconnected")
                }.onFailed {
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
                printerQueueModel.orderItems = itemAttribute
                printerQueueModel.terminalName = ""
                printerQueueModel.orderType = obj.asJsonObject.get("open_order_type").asString
                printerQueueModel.offlineId = obj.asJsonObject.get("offline_id").asString
                printerQueueModel.paymentType = "Cash"
                printerQueueModel.status = PENDING
                printerQueueModel.totalAmt = obj.asJsonObject.get("total_amount").asDouble
                printerQueueModel.terminalName =
                    it.asJsonObject.get("terminal_name")?.asString ?: ""

                printerQueuelist.add(printerQueueModel)

            }

            if (printerQueuelist.isNotEmpty()) {
                adapter.setList(printerQueuelist)



            }


        }
        printerQueuelist.forEach {
            configurePrinter(it)
        }

    }

    private fun configurePrinter(printerQueueModel: PrinterQueueModel) {
        kitchenPrinterList.forEach {

            initKitchenPrinter(it, printerQueueModel)

        }

    }

    private fun onClick() {
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
                    // deletePrinterQueue(adapter.getList().get(it).id)
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

    private fun deletePrinterQueue(id: Int) {
        alert(
            getString(R.string.tv_pos),
            getString(R.string.delete_printer_message)
        ) {
            positiveButton(getString(R.string.tv_delete)) {
                viewModel.deleteQueuePrinter(id)

            }
            negativeButton(R.string.tv_cancel) {

            }
        }
    }

    private fun initKitchenPrinter(
        data: PrinterResponse.Data.KitchenReceiptPrinters,
        printerQueueModel: PrinterQueueModel
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
                1000
            )


        } catch (e: Exception) {
            Log.e(TAG, "PrinterException: " + e.message)
            printer = null
            return
        }

        if (printer != null) {
            PrinterClass.setPrinter(printer)

            generateKitchenReceipt(data, "", printerQueueModel)

        }


    }

    private fun generateKitchenReceipt(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        printerQueueModel: PrinterQueueModel
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
                    PrinterClass.SEND_TIMEOUT, status, battery
                )

                PrinterClass.closePrinter()

                //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
            } catch (e: Exception) {
                PrinterClass.closePrinter()
                e.printStackTrace()
                Log.e(TAG, "PrinterError: " + e.localizedMessage)
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

}