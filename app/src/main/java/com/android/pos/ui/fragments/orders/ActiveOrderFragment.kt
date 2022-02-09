package com.android.pos.ui.fragments.orders

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.pos.R
import com.android.pos.data.entities.*
import com.android.pos.data.model.requestModel.OrderItemVariationAttribute
import com.android.pos.data.model.responseModel.GetCustomerReceiptSettingsResponse
import com.android.pos.data.model.responseModel.GetTipReponse
import com.android.pos.data.model.responseModel.OpenOrderResponse
import com.android.pos.data.model.responseModel.PrinterResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.ARG_PARAM1
import com.android.pos.data.remote.Constants.BUSINESS_ADDRESS
import com.android.pos.data.remote.Constants.PRINT_PAID
import com.android.pos.data.remote.Constants.PRINT_UNPAID
import com.android.pos.databinding.FragmentActiveOrdersBinding
import com.android.pos.di.PrefProvider
import com.android.pos.di.RolePermission
import com.android.pos.ui.adapter.OpenOrderAdapter
import com.android.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.android.pos.utils.*
import com.android.pos.utils.callback.OrderCallBack
import com.android.pos.utils.extensions.alert
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.printer.PrinterClass
import com.android.pos.utils.statusUtils.Status
import com.epson.eposprint.Builder
import com.epson.eposprint.Print
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@AndroidEntryPoint
class ActiveOrderFragment : Fragment(), OrderCallBack {
    private var param1: String = "Unpaid"

    private var itemPos: Int = 0
    private lateinit var binding: FragmentActiveOrdersBinding
    private val viewModel by viewModels<ActiveOrderViewModel>()
    private val dashboardViewModel by activityViewModels<DashBoardCategoryViewModel>()
    private lateinit var adapter: OpenOrderAdapter
    private var customerSettingModel = GetCustomerReceiptSettingsResponse.Data()
    private val TAG = "ActiveOrderFragment"
    private var tipsList: List<GetTipReponse.Data> = listOf()

    @Inject
    lateinit var prefProvider: PrefProvider

    @Inject
    lateinit var rolePermission: RolePermission

    companion object {
        @JvmStatic
        fun newInstance(param1: String) =
            ActiveOrderFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1).toString()
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentActiveOrdersBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        getCustomerReceiptSettings()
        observeTipsList()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        getOpenOrders()
        observeShowProgress()


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

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { baseResponse ->
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, baseResponse.message
                    ) { _, _ ->
                        var intent = Intent()
                        intent.action = "cancelled"
                        intent.putExtra("position", 3)
                        requireContext().sendBroadcast(intent)
                    }
                }
            }
        })
    }

    private fun setupAdapter() {

        binding.rvOpenOrder.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayoutManager.VERTICAL
            )
        )

        adapter = OpenOrderAdapter(requireContext())
        adapter.setCallback(this)
        binding.rvOpenOrder.adapter = adapter
    }

    private fun getOpenOrders() {


        viewModel.openOrders(param1).observe(viewLifecycleOwner, { it ->

            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        resource.data?.let {
                            if (it.data.orders.isNotEmpty()) {
                                binding.rvOpenOrder.visibility = View.VISIBLE
                                binding.txtNodata.visibility = View.GONE
                                val data = it.data.orders
                                adapter.add(data)
                            } else {
                                binding.txtNodata.visibility = View.VISIBLE
                                binding.txtNodata.text = it.message
                                binding.rvOpenOrder.visibility = View.GONE

                            }
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
        })

    }

    override fun onItemClickListener(view: View?, pos: Int, status: String) {
        val order = adapter.getItem(pos)
        when (status) {
            "UPDATE" -> {
                prefProvider.setValue(Constants.ORDER_TYPE, order.orderType)


                if (order.customer != null) {
                    prefProvider.setValue(
                        Constants.CUSTOMER_NAME,
                        order.customer.firstName + " " + order.customer.lastName
                    )
                    prefProvider.saveCustomerData(TbCustomer.customerMapping(order.customer))
                }

                dashboardViewModel.addCart(
                    cartModel(order)
                )
                val bundle = Bundle()
                bundle.putBoolean("update", true)
                bundle.putInt("orderId", order.id)
                if (!order.payments.isNullOrEmpty()) {
                    bundle.putInt("paymentId", order.payments[0].id)
                    bundle.putString("paymentOfflineId", order.payments[0].offlineId)
                } else {
                    bundle.putString("paymentOfflineId", randomOfflineId())
                }
                bundle.putString("orderOfflineId", order.offlineId)
                bundle.putBoolean("isLoyaltyApplied", order.isLoyaltyApplied)
                findNavController().navigate(
                    R.id.action_orders_to_dashboardCategoryNew, bundle
                )

            }
            "PAY" -> {

                val cartModel = cartModel(order)
                prefProvider.setValue(
                    Constants.CUSTOMER_NAME,
                    order.customer?.firstName + " " + order.customer?.lastName
                )

                val bundle = Bundle()
                bundle.putDouble("totalPrice", order.totalAmount)
                bundle.putDouble("finalprice", order.totalAmount)
                bundle.putDouble(
                    "cashDiscountSurcharge",
                    MethodUtils.calculateCashDiscount(
                        order.subTotal,
                        prefProvider,
                        requireContext()
                    )
                )
                bundle.putDouble("subTotalPrice", order.subTotal)
                bundle.putDouble("totalTax", order.totalTaxAmount)
                bundle.putDouble("totalDiscount", order.totalDiscount)
                bundle.putDouble("totalServiceCharge", order.totalServiceCharges)
                bundle.putString("future_delivery_date", order.futureDeliveryDate)
                bundle.putString("future_delivery_time", order.futureDeliveryTime)
                bundle.putParcelable("cartList", cartModel)


                bundle.putBoolean("update", true)
                bundle.putInt("orderId", order.id)
                if (order.payments.isNotEmpty()) {
                    bundle.putInt("paymentId", order.payments[0].id)
                    bundle.putString("paymentOfflineId", order.payments[0].offlineId)
                } else {
                    bundle.putString("paymentOfflineId", randomOfflineId())
                }
                bundle.putString("orderOfflineId", order.offlineId)

                val redeemLoyaltyInfo = RedeemLoyaltyInfo()
                val loyaltyProgramsModel = LoyaltyProgramsModel(
                    0.0,
                    "",
                    order.loyaltyProgramId,
                    order.isLoyaltyApplied,
                    order.locationId,
                    "",
                    0,
                    "",
                    ""
                )
                redeemLoyaltyInfo.loyaltyProgramsModel = loyaltyProgramsModel
                //redeemLoyaltyInfo.isLoyaltyApplied = order.isLoyaltyApplied
                redeemLoyaltyInfo.needToApplyLoyalty = order.isLoyaltyApplied
                redeemLoyaltyInfo.usedLoyaltyAmount = order.loyaltyAmount
                redeemLoyaltyInfo.usedLoyaltyPoints = order.usedRewardPoints

                bundle.putString(
                    "redeemLoyalty",
                    Gson().toJson(redeemLoyaltyInfo)
                )
                bundle.putBoolean("isFromActiveOrder", true)

                findNavController().navigate(
                    R.id.action_orders_to_paymentFragment,
                    bundle
                )

            }
            PRINT_UNPAID -> {

                getCustomerPrinters(order, status)
            }
            PRINT_PAID -> {
                getCustomerPrinters(order, status)
            }

            else -> {
                if (rolePermission.hasCancelOrderPermission(binding.root)) {
                    alert(
                        getString(R.string.app_name),
                        getString(R.string.cancel_order_message)
                    ) {
                        positiveButton(getString(R.string.yes)) {

                            itemPos = pos
                            viewModel.cancelOrder(order.id)
                        }
                        negativeButton(R.string.no) {
                            // Do negative stuff here
                        }
                    }
                }
            }
        }

    }

    private fun cartModel(order: OpenOrderResponse.Data.Order): CartModel {
        Log.e("futureDeliveryDate  ", Gson().toJson(order))
        return CartModel().apply {
            terminalId = order.terminalId
            employeeID = order.employeeId
            locationId = order.locationId
            orderTypeId = order.orderTypeId
            orderType = order.orderType
            orderTypeName = order.orderType
            futureDeliveryDate = order.futureDeliveryDate.toString()
            isOpenOrder = true
            serviceCharge = serviceChargesList(order)
            customer = assignCustomer(order)
            items = inventoryList(order)
            note = order.note
            var itemDiscount = 0.0
            items?.forEach {
                itemDiscount += it.discountPrice
            }
            discountPrice = (order.totalDiscount - itemDiscount)
        }
    }

    private fun inventoryList(order: OpenOrderResponse.Data.Order): List<TbItem>? {

        val inventoryModelList = ArrayList<TbItem>()

        order.orderItems.forEach {

            val items = TbItem().apply {
                orderItemId = it.id
                itemId = it.itemId
                name = it.itemName
                cost = it.price
                price = it.price
                priceType = ""
                itemQuantity = it.quantity
                kitchenName = ""
                productCode = ""
                sku = ""
                isHide = false
                sort = 0
                imageUrl = ""
                thumbImageUrl = ""
                categoryId = it.categoryId
                categoryName = ""
                taxes = taxes(it.orderItemTax, order.locationId)
                modifier_set_ids = modifiersIds(it.orderItemModifiers)
                modifiers = modifierSets(it.orderItemModifiers)
                discountPrice = it.discountAmount
                discountType = it.discountType
                if (it.discountId != null)
                    discountId = it.discountId
                if (it.order_item_variation != null)
                    variationsAttributes = variationAtt(it.order_item_variation)
                note = it.note
            }

            inventoryModelList.add(items)

        }

        return inventoryModelList
    }

    private fun variationAtt(variation: OrderItemVariationAttribute): List<VariationsAttribute> {

        val variationsAttributeList = ArrayList<VariationsAttribute>()

        val variationsAttribute = VariationsAttribute()
        variationsAttribute.id = variation.variationId
        variationsAttribute.name = variation.name
        variationsAttribute.price = variation.price
        variationsAttribute.orderVariationId = variation.id
        variationsAttributeList.add(variationsAttribute)

        return variationsAttributeList
    }

    private fun modifierSets(orderItemModifiers: List<OpenOrderResponse.Data.Order.OrderItem.OrderItemModifier>): List<Modifier> {

        val modifierList = ArrayList<Modifier>()

        orderItemModifiers.forEach {

            val modifier = Modifier().apply {
                id = it.modifierId
                modifierSetId = it.modifierSetId
                name = it.name
                price = it.price
                itemQuantity = it.quantity
                orderModifierId = it.id

            }
            modifierList.add(modifier)
        }

        return modifierList
    }

    private fun modifiersIds(orderItemModifiers: List<OpenOrderResponse.Data.Order.OrderItem.OrderItemModifier>): List<Int> {

        val selectedIds = ArrayList<Int>()
        if (orderItemModifiers.isNotEmpty()) {
            orderItemModifiers.forEach {
                selectedIds.add(it.id)
            }
        }
        return selectedIds
    }

    private fun taxes(
        taxs: List<OpenOrderResponse.Data.Order.OrderItem.OrderItemTax>,
        locationId: Int
    ): List<TaxData>? {
        val taxList = ArrayList<TaxData>()

        taxs.forEach {
            val tax = TaxData(
                it.createdAt,
                it.taxId,
                locationId,
                it.name,
                it.rate,
                it.taxType,
                it.updatedAt,
                true,
                it.isDefault,
                false,
                "",
                listOf(),
                it.id
            )
            taxList.add(tax)
        }

        return taxList
    }

    private fun assignCustomer(order: OpenOrderResponse.Data.Order): TbCustomer {

        val phoneList = ArrayList<TbPhones>()
        order.customer?.phones?.forEach {
            val phone = TbPhones(it.id, it.phoneNumber)
            phoneList.add(phone)
        }

        val addressList = ArrayList<TbAddress>()
        order.customer?.addresses?.forEach {
            val address = TbAddress(
                it.id,
                it.address1,
                it.address2,
                it.city,
                it.state,
                it.country ?: "",
                it.postcode ?: "",
                "",
                it.latitude ?: "",
                it.longitude ?: "",
                "",
                it.fullAddress,
                it.street
            )
            addressList.add(address)
        }

        return TbCustomer(
            order.customer?.id,
            order.customer?.firstName.toString(),
            order.customer?.lastName.toString(),
            order.customer?.birthDate.toString(),
            order.customer?.email.toString(),
            false,
            0,
            order.customer?.company.toString(),
            phoneList,
            addressList
        )
    }


    private fun serviceChargesList(order: OpenOrderResponse.Data.Order): List<TbServiceCharge> {

        val serviceChargeList = ArrayList<TbServiceCharge>()

        order.orderServiceCharges.forEach {
            val serviceCharge = TbServiceCharge(
                it.createdAt,
                it.serviceChargeId,
                true,
                order.locationId,
                it.name,
                it.rate,
                it.updatedAt,
                isActive = false,
                isChecked = true,
                order_service_charge_id = it.id
            )
            serviceChargeList.add(serviceCharge)
        }

        return serviceChargeList
    }

    private fun getCustomerReceiptSettings() {
        viewModel.getCustomerReceiptSettings().observe(viewLifecycleOwner, {
            if (it != null) {
                customerSettingModel = it


            }

        })

    }

    private fun getCustomerPrinters(order: OpenOrderResponse.Data.Order, type: String) {

        viewModel.getCustomerPrinterList().observe(viewLifecycleOwner, {
            when (it.status) {
                Status.SUCCESS -> {
                    ProgressUtils.dismissProgressDialog()
                    if (it.data != null) {
                        val customerList = it.data

                        customerList.forEach {
                            initPrinter(it, Constants.CUSTOMER, order, type)


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

        })

    }

    private fun initPrinter(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String,
        order: OpenOrderResponse.Data.Order,
        printType: String
    ) {
        PrinterClass.closePrinter()
        if (PrinterClass.getPrinter() == null) {
            var printer: Print? = Print(requireContext())
            if (printer != null) {
                //  printer.setStatusChangeEventCallback(this)
                // printer.setBatteryStatusChangeEventCallback(this)
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
                //printer?.setStatusChangeEventCallback(this)

            } catch (e: Exception) {
                Log.e(TAG, "PrinterException: " + e.message)
                printer = null
                return
            }
            try {

                if (printer != null) {
                    PrinterClass.setPrinter(printer)

                    generatePrint(customerReceiptPrinters, type, order, printType)

                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            Log.e(TAG, "PrinterIsNotNull:")
        }

    }

    private fun generatePrint(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String,
        receiptModel: OpenOrderResponse.Data.Order,
        printType: String
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

            builder.addTextLang(Builder.LANG_EN)

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
            if (printType == PRINT_PAID) {

                builder.addText("Paid" + "\n")
            } else {
                builder.addText("Unpaid" + "\n")

            }
            builder.addFeedLine(1)


            builder.addTextSize(2, 2)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            builder.addTextAlign(Builder.ALIGN_CENTER)

            addBuilderText(builder, prefProvider.getValue(Constants.BUSINESS_NAME, "").toString())
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
                prefProvider.getValue(
                    Constants.BUSINESS_ADDRESS, prefProvider.getValue(
                        BUSINESS_ADDRESS, ""
                    )
                ).toString()
            )
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
                prefProvider.getValue(Constants.BUSINESS_PHONE_NO, "").toString()
            )

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
            builder.addText(receiptModel?.orderType + "\n")

            if (receiptModel?.orderType?.lowercase() == Constants.OPEN_ORDER_.lowercase()
                || receiptModel?.orderType?.lowercase() == Constants.OPEN_ORDER.lowercase()
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
                builder.addText(receiptModel?.deliveryType + "\n")


            }




            if (customerSettingModel.fonts == Constants.LARGE) {

                if (customerSettingModel.showOrderIdTop) {
                    builder.addFeedLine(1)
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

                    builder.addText("OrderID:" + receiptModel?.id)

                }
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

                builder.addText("ReceiptID:" + receiptModel?.offlineId)

                if (customerSettingModel.showTeam) {
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


                    builder.addText("Employee:" + receiptModel?.employee?.name)

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
                            receiptModel?.createdAt.toString()
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
                        builder.addText("Print Time:" + formatted)
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
                        if (customerSettingModel.showOrderIdTop) {
                            "OrderID:" + receiptModel?.id
                        } else {
                            ""
                        },
                        "ReceiptID:" + receiptModel?.offlineId,
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
                            if (customerSettingModel.showTeam) {
                                "Employee:" + receiptModel.employee.name
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
                            if (customerSettingModel.showTeam) {
                                "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                                    receiptModel?.createdAt.toString()
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
                                if (customerSettingModel.showTeam) {
                                    "Print Time:" + formatted
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



            receiptModel.orderItems?.let {
                addOrderItemOpenOrder(
                    builder,
                    it,
                    customerSettingModel.fonts,
                    customerSettingModel.showModifiers
                )
            }

            builder.addFeedLine(2)

            if (receiptModel?.totalDiscount != null) {
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

                        if (receiptModel.totalDiscount == 0.0) {
                            "$" + MethodUtils.roundOffAmountString(receiptModel.totalDiscount)
                        } else {
                            "-$" + MethodUtils.roundOffAmountString(receiptModel.totalDiscount)
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

            builder.addText(
                padLine(
                    "Sub Total",
                    "$" + MethodUtils.roundOffAmountString(receiptModel.subTotal),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        24
                    } else {
                        48
                    }
                )
            )


            if (receiptModel?.totalTaxAmount != null) {
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
                        "$" + MethodUtils.roundOffAmountString(receiptModel.totalTaxAmount),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }

            if (receiptModel.totalServiceCharges != null) {
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
                        "$" + MethodUtils.roundOffAmountString(receiptModel.totalServiceCharges),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }

            if (receiptModel?.totalTips != 0.0) {

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
                        "Tips",
                        "$" + receiptModel.totalTips?.let {
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




            if (receiptModel.cash_discount_or_surcharge != null) {
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
                        "Cash Discount",
                        if (receiptModel.cash_discount_or_surcharge == 0.0) {
                            "$" + MethodUtils.roundOffAmountString(receiptModel.cash_discount_or_surcharge!!)
                        } else {
                            "-$" + MethodUtils.roundOffAmountString(receiptModel.cash_discount_or_surcharge!!)
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


            if (receiptModel.totalAmount != null) {
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
                var totalAmt = MethodUtils.roundOffAmountDouble(receiptModel.totalAmount)
                /*if (receiptModel.totalDiscount != 0.0) {
                    totalAmt =
                        (totalAmt - MethodUtils.roundOffAmountDouble(receiptModel.totalDiscount))

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

            }

            if (customerSettingModel.showRefundAmount && printType == PRINT_PAID) {
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
                        "Change Amount",
                        "$" + MethodUtils.roundOffAmountString(0.00),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }

            if (receiptModel?.totalTips == 0.0 && printType == PRINT_PAID) {
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

                if (receiptModel.totalTips != 0.0) {
                    tip = receiptModel.totalTips.toString()
                }
                builder.addText(
                    padLine(
                        "Tips",
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
                        if (receiptModel.totalDiscount != 0.0) {
                            (receiptModel.totalAmount.toDouble() - receiptModel.totalDiscount.toDouble())
                        } else {
                            receiptModel.totalAmount
                        },
                        customerSettingModel.fonts
                    )

                }
            }

            if (printType == PRINT_PAID) {
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
                        receiptModel.payments.get(0).transactionId,
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )
            }

            if (printType == PRINT_PAID) {
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
                        receiptModel.payments.get(0).paymentType,
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

            }
            if (customerSettingModel.showCustomerAddress or customerSettingModel.showCustomerPhone or customerSettingModel.showCustomerName) {

                if (receiptModel.customer != null) {

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

                        builder.addText(receiptModel.customer.firstName + " " + receiptModel.customer.lastName)
                    }

                    if (customerSettingModel.showCustomerPhone) {
                        if (receiptModel?.customer?.phones?.isNotEmpty()) {
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
                                receiptModel?.customer?.phones?.get(receiptModel?.customer?.phones?.size - 1).phoneNumber
                            )
                            Log.e(TAG, "phoneNoFormatted:  ${phoneNoFormatted}")
                            builder.addText(phoneNoFormatted)

                        }


                    }




                    if (customerSettingModel.showCustomerAddress) {
                        if (receiptModel.customer?.addresses?.isNotEmpty() == true) {

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

                            builder.addText(receiptModel.customer?.addresses?.get(0)?.fullAddress)
                        }
                    }

                }
            }


            if (receiptModel.note != null && receiptModel.note != "" && customerSettingModel.showOrderNote) {

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

                builder.addText(receiptModel.note)
            }


            if (customerSettingModel.showQrCode) {
                builder.addFeedLine(1)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                val bitmap = generateQRCode(receiptModel.digitalReceiptUrl)
                Log.e(TAG, "BitmapHeight ${bitmap.height}")
                Log.e(TAG, "BitmapWidth ${bitmap.width}")
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
                findNavController().navigate(R.id.action_orderCompleteFragment_to_dashboardCategoryNew)
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

    private fun observeTipsList() {
        viewModel.getTipsList().observe(viewLifecycleOwner, {
            if (it.isNotEmpty()) {
                tipsList = it

            }


        })
    }

    private fun generateQRCode(qrcodeStaticUrl: String): Bitmap {

        val manager = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager?

        // initializing a variable for default display.

        // initializing a variable for default display.
        val display: Display = manager!!.defaultDisplay

        // creating a variable for point which
        // is to be displayed in QR Code.

        // creating a variable for point which
        // is to be displayed in QR Code.
        val point = Point()
        display.getSize(point)

        // getting width and
        // height of a point

        // getting width and
        // height of a point
        val width: Int = point.x
        val height: Int = point.y

        // generating dimension from width and height.

        // generating dimension from width and height.
        var dimen = if (width < height) width else height
        dimen = dimen * 3 / 4

        Log.e(TAG, "getDimen:  ${dimen}")
        return net.glxn.qrgen.android.QRCode.from(qrcodeStaticUrl).bitmap()


    }

    fun randomOfflineId(): String {

        val locationId = prefProvider.getValueInt(Constants.LOCATION_ID, -1).toString()
        val timestamp = System.currentTimeMillis().toString()
        val ss = locationId + timestamp.takeLast(4)
        val reqLent = 12 - ss.length
        val Alphabet = getSaltString(reqLent)
        val timeStampFinal = Alphabet + ss
        Log.e("timeStampFinal", timeStampFinal)

        return timeStampFinal
    }

    fun getSaltString(reqLent: Int): String? {
        val SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
        val salt = StringBuilder()
        val rnd = Random()
        while (salt.length < reqLent) { // length of the random string.
            val index = (rnd.nextFloat() * SALTCHARS.length).toInt()
            salt.append(SALTCHARS[index])
        }
        return salt.toString()
    }
}