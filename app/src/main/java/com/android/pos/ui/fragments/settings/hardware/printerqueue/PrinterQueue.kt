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
import com.android.pos.data.model.responseModel.PrinterQueueReponse
import com.android.pos.data.remote.Constants.IS_PRINTER_QUEUE_ENABLE
import com.android.pos.data.remote.Constants.LOCATION_ID
import com.android.pos.data.remote.Constants.PENDING
import com.android.pos.databinding.FragmentPrinterQueueBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.PrinterQueueListAdapter
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.SwipeHelper
import com.android.pos.utils.extensions.alert
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
class PrinterQueue : Fragment() {
    private lateinit var binding: FragmentPrinterQueueBinding
    private val list: ArrayList<PrinterQueueModel> = arrayListOf()
    private lateinit var adapter: PrinterQueueListAdapter
    private val TAG = "PrinterQueue"

    @Inject
    lateinit var prefProvider: PrefProvider

    private val viewModel by viewModels<PrinterQueueViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPrinterQueueBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        observeShowProgress()
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
        val consumer: Consumer = ActionCable.createConsumer(uri)

        // 2. Create subscription
        val appearanceChannel = Channel("KitchenChannel")
        // appearanceChannel.addParam("id",prefProvider.getValueInt(LOCATION_ID,0))
        val subscription: Subscription = consumer.subscriptions.create(appearanceChannel)

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
                getQueueDataResponse(it.asJsonObject.get("printer_queue"))

            }.onDisconnected {
                Log.e(TAG, "onActiononDisconnected")
            }.onFailed {
                Log.e(TAG, "onActiononFailed")
            }


        // 3. Establish connection
        consumer.connect();


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
            requireActivity().runOnUiThread {
                if (printerQueuelist.isNotEmpty()) {
                    adapter.setList(printerQueuelist)
                }

            }


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
}