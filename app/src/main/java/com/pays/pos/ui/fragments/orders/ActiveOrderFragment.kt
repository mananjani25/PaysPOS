package com.pays.pos.ui.fragments.orders

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.pays.pos.R
import com.pays.pos.data.entities.*
import com.pays.pos.data.model.requestModel.OrderItemVariationAttribute
import com.pays.pos.data.model.responseModel.GetCustomerReceiptSettingsResponse
import com.pays.pos.data.model.responseModel.GetTipReponse
import com.pays.pos.data.model.responseModel.OpenOrderResponse
import com.pays.pos.data.model.responseModel.PrinterResponse
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.BUSINESS_ADDRESS
import com.pays.pos.data.remote.Constants.OPEN_ORDER
import com.pays.pos.data.remote.Constants.OPEN_ORDER_
import com.pays.pos.data.remote.Constants.ORDER_NUMBER_STARTING_FROM_ONE
import com.pays.pos.data.remote.Constants.PRINT_PAID
import com.pays.pos.data.remote.Constants.PRINT_UNPAID
import com.pays.pos.data.remote.Constants.SERVICECHARGE_TAKEOUT_OPENORDER
import com.pays.pos.data.remote.Constants.SHIPPING_ADDRESS
import com.pays.pos.data.remote.Constants.SUNMI_INNER_PRINTER
import com.pays.pos.data.remote.Constants.SUNMI_PRINTER
import com.pays.pos.databinding.FragmentActiveOrdersBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.di.RolePermission
import com.pays.pos.ui.adapter.OpenOrderAdapter
import com.pays.pos.ui.fragments.dashboard.DashBoardCategoryViewModel
import com.pays.pos.ui.fragments.settings.hardware.printer.BluetoothUtil
import com.pays.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper
import com.pays.pos.utils.*
import com.pays.pos.utils.callback.OrderCallBack
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.runOnUiThread
import com.pays.pos.utils.extensions.showAlert
import com.pays.pos.utils.extensions.visible
import com.pays.pos.utils.printer.PrinterClass
import com.pays.pos.utils.statusUtils.Status
import com.epson.eposprint.Builder
import com.epson.eposprint.Print
import com.google.gson.Gson
import com.sunmi.externalprinterlibrary.api.ConnectCallback
import com.sunmi.externalprinterlibrary.api.SunmiPrinter
import com.sunmi.externalprinterlibrary.api.SunmiPrinterApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class ActiveOrderFragment(
    var param1: String,
    var startDateTime: String?,
    var endDateTime: String?
) : Fragment(), OrderCallBack {
    private var paramStartDate: String = ""
    private var paramEndDate: String = ""

    private var itemPos: Int = 0
    private lateinit var binding: FragmentActiveOrdersBinding
    private val viewModel by viewModels<ActiveOrderViewModel>()
    private val dashboardViewModel by activityViewModels<DashBoardCategoryViewModel>()
    private lateinit var adapter: OpenOrderAdapter
    private var customerSettingModel = GetCustomerReceiptSettingsResponse.Data()
    private val TAG = "ActiveOrderFragment"
    private var tipsList: List<GetTipReponse.Data> = listOf()

    private lateinit var startDate: DatePickerDialog.OnDateSetListener
    private lateinit var endDate: DatePickerDialog.OnDateSetListener
    private lateinit var startTime: TimePickerDialog.OnTimeSetListener
    private lateinit var endTime: TimePickerDialog.OnTimeSetListener

    val myCalendar = Calendar.getInstance()
    val myCalendar1 = Calendar.getInstance()
    val myCalendar2 = Calendar.getInstance()
    val myCalendar3 = Calendar.getInstance()


    @Inject
    lateinit var prefProvider: PrefProvider

    @Inject
    lateinit var rolePermission: RolePermission

    /*
        companion object {
            @JvmStatic
            fun newInstance(param1: String, startTime: String?, endTime: String?) =
                ActiveOrderFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_PARAM1, param1)
                        putString(ARG_PARAM2, startTime)
                        putString(ARG_PARAM3, endTime)
                    }
                }
        }
    */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*
                arguments?.let {
                    param1 = it.getString(ARG_PARAM1).toString()
                    paramStartDate = it.getString(ARG_PARAM2).toString()
                    paramEndDate = it.getString(ARG_PARAM3).toString()
                }
        */
        viewModel.setCurrentDate(myCalendar, startDateTime, endDateTime)
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentActiveOrdersBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this



        startDatePickerObserver()
        endDatePickerObserver()
        getCustomerReceiptSettings()
        observeTipsList()

        startTime = TimePickerDialog.OnTimeSetListener { view, hour, minute ->
            val timecalender = Calendar.getInstance()
            timecalender.set(Calendar.HOUR_OF_DAY, hour)
            timecalender.set(Calendar.MINUTE, minute)
            viewModel.startDate.value = timeCalculateForStartEndTime(hour, minute, "isstart")
            if (differnceTrue(viewModel.startDate.value!!, viewModel.endDate.value) <= 30) {
                /*checkFilter = true
                currentPage = 1
                apiCallTimeSheet()*/
                getOpenOrders()
            } else {
                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireActivity(),
                    "Please Select date in 30 Days."
                ) { _, _ ->
                }
            }
        }

        endTime = TimePickerDialog.OnTimeSetListener { view, hour, minute ->
            val timecalender = Calendar.getInstance()
            timecalender.set(Calendar.HOUR_OF_DAY, hour)
            timecalender.set(Calendar.MINUTE, minute)
            viewModel.endDate.value = timeCalculateForStartEndTime(hour, minute, "isend")
            /*checkFilter = true
            currentPage = 1*/
            if (differnceTrue(viewModel.endDate.value!!, viewModel.startDate.value) <= 30)
                getOpenOrders()
            else {
                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireActivity(),
                    "Please Select date in 30 Days."
                ) { _, _ ->
                }
            }

        }

        startDate = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            myCalendar.set(Calendar.YEAR, year)
            myCalendar.set(Calendar.MONTH, monthOfYear)
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            TimePickerDialog(
                requireActivity(),
                android.R.style.Theme_Material_Light_Dialog,
                startTime,
                myCalendar2.get(2),
                myCalendar2.get(2),
                false
            ).show()
        }

        endDate = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            myCalendar1.set(Calendar.YEAR, year)
            myCalendar1.set(Calendar.MONTH, monthOfYear)
            myCalendar1.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            TimePickerDialog(
                requireActivity(),
                android.R.style.Theme_Material_Light_Dialog,
                endTime,
                myCalendar3.get(2),
                myCalendar3.get(2),
                false
            ).show()

        }


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapter()
        getOpenOrders()
        observeShowProgress()
        when (param1) {
            "0" -> {
                binding.txtOrderWillAppear.text = "Active order will appear here."
            }

            "1" -> {
                binding.txtOrderWillAppear.text = "Completed order will appear here."
            }

            "2" -> {
                binding.txtOrderWillAppear.text = "Cancelled order will appear here."
            }
        }

        searchFilter()
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
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, baseResponse.message
                    ) { _, _ ->
                        var intent = Intent()
                        intent.action = "cancelled"
                        intent.putExtra("isCount", false)
                        intent.putExtra("position", 2)
                        intent.putExtra("start_date", viewModel.startDate.value.toString())
                        intent.putExtra("end_date", viewModel.endDate.value.toString())
                        requireContext().sendBroadcast(intent)
                    }
                }
            }
        }
    }

    private fun setupAdapter() {

        binding.rvOpenOrder.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayoutManager.VERTICAL
            )
        )

        adapter = OpenOrderAdapter(requireContext(), prefProvider)
        adapter.setCallback(this)
        binding.rvOpenOrder.adapter = adapter
    }

    private fun getOpenOrders() {
        var startTime = getDateByTimeZone(viewModel.startDate.value.toString())
        var endTime = getDateByTimeZone(viewModel.endDate.value.toString())
        LogUtil.logE(TAG, "startTime  ${startTime}")
        LogUtil.logE(TAG, "endTime  ${endTime}")

        viewModel.openOrders(
            param1,
            viewModel.startDate.value.toString(),
            viewModel.endDate.value.toString()
        ).observe(viewLifecycleOwner) { it ->

            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        resource.data?.let {

                            if (it.data.orders.isNotEmpty()) {
                                binding.rvOpenOrder.visibility = View.VISIBLE
                                binding.llNoData.visibility = View.GONE
                                val data = it.data.orders

                                adapter.add(data)
                                LogUtil.logE("DATA", data.size.toString())
                            } else {
                                binding.llNoData.visibility = View.VISIBLE
                                binding.txtNodata.text = it.message
                                binding.rvOpenOrder.visibility = View.GONE

                            }


                            val intent = Intent()
                            intent.action = "cancelled"
                            intent.putExtra("isCount", true)
                            intent.putExtra("param1", param1)
                            intent.putExtra("count", it.data.orders.size)
                            intent.putExtra("start_date", viewModel.startDate.value.toString())
                            intent.putExtra("end_date", viewModel.endDate.value.toString())
                            requireContext().sendBroadcast(intent)
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

    private fun getDateByTimeZone(date: String): String {
        LogUtil.logE(TAG, "gotDate ${date}")
        val myFormat = "MM/dd/yyyy HH:mm a"
        val sdf = SimpleDateFormat(myFormat)
        sdf.timeZone = TimeZone.getDefault()

        var parseDate = sdf.parse(date)

        val outputFormat = SimpleDateFormat(myFormat)
        outputFormat.timeZone = TimeZone.getTimeZone(
            prefProvider.getValue(
                Constants.SYSTEM_TIMEZONE,
                ""
            )
        )
        return outputFormat.format(parseDate)
        // return outputFormat.format(parseDate)


    }

    override fun onItemClickListener(view: View?, pos: Int, status: String) {
        val order = adapter.getItem(pos)
        when (status) {
            "UPDATE" -> {
                var itemDiscountTotal: Double = 0.0
                var itemPassDis: Double = 0.0
                order.orderItems.forEach {
                    if (it.discountAmount != 0.0) {
                        itemDiscountTotal += MethodUtils.roundOffAmountDouble(it.discountAmount)
                    }
                    if (it.discountAmount != 0.0 && it.quantity > 1) {

                        //  itemDiscountTotal += MethodUtils.roundOffAmountDouble(it.discountAmount / it.quantity)
                        it.discountAmount =
                            MethodUtils.roundOffAmountDouble(it.discountAmount / it.quantity)
                    }
                }
                LogUtil.logE(TAG, "itemDiscountTotal:  ${itemDiscountTotal}")
                LogUtil.logE(TAG, "totalOrderDiscount  ${order.totalDiscount}")

                order.totalDiscount = order.totalDiscount - itemDiscountTotal

                LogUtil.logE(TAG, "OpenORderUpdateOrder:  ${Gson().toJson(order.orderItems)}")

                prefProvider.setValue(Constants.ORDER_TYPE, OPEN_ORDER)
                prefProvider.setValue(Constants.ORDER_TYPE_NAME, order.orderTypeName)
                prefProvider.setValueInt(Constants.ORDER_TYPE_ID, order.orderTypeId)

                if (order.customer != null) {
                    prefProvider.setValue(
                        Constants.CUSTOMER_NAME,
                        order.customer.firstName + " " + order.customer.lastName
                    )
                    prefProvider.setValue(
                        Constants.RECEIPT_CUSTOMER_NAME,
                        order.customer.firstName + " " + order.customer.lastName
                    )
                    prefProvider.setValueInt(Constants.CUSTOMER_ID, order.customer.id)
                    prefProvider.saveCustomerData(TbCustomer.customerMapping(order.customer))
                }
                prefProvider.setValue(Constants.OPEN_ORDER_ITEMS, Gson().toJson(order.orderItems))
                prefProvider.setValueboolean(Constants.OPEN_ORDER_UPDATE_FOR_PRINT, true)

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
                bundle.putBoolean("isFromActiveOrder", true)
                bundle.putBoolean("isLoyaltyApplied", order.isLoyaltyApplied)

                prefProvider.setValueboolean(Constants.IS_UPDATE_ORDER, true)
                prefProvider.setValueboolean(
                    Constants.IS_UPDATE_ORDER_LOYALTY_APPLIED,
                    order.isLoyaltyApplied
                )
                prefProvider.setValueboolean(
                    Constants.LOYALTY_ADDED,
                    order.isLoyaltyApplied
                )
                prefProvider.setValueboolean(Constants.IS_UPDATE_ORDER_FROM_ACTIVE_ORDER, true)
                prefProvider.setValueInt(Constants.IS_UPDATE_ORDER_ID, order.id)

                if (!order.payments.isNullOrEmpty()) {
                    prefProvider.setValueInt(
                        Constants.IS_UPDATE_ORDER_PAYMENT_ID,
                        order.payments[0].id
                    )
                    prefProvider.setValue(
                        Constants.IS_UPDATE_ORDER_PAY_OFFLINE_ID,
                        order.payments[0].offlineId
                    )
                } else {
                    prefProvider.setValue(
                        Constants.IS_UPDATE_ORDER_PAY_OFFLINE_ID,
                        randomOfflineId()
                    )
                }
                prefProvider.setValue(Constants.IS_UPDATE_ORDER_OFFLINE_ID, order.offlineId)


                if (findNavController().currentDestination?.id == R.id.orders) {
                    findNavController().navigate(
                        R.id.action_orders_to_dashboardCategoryBoldPOS, bundle
                    )
                }

//                findNavController().navigateUp()

            }

            "PAY" -> {

                prefProvider.setValue("PaidAmount", "")
                prefProvider.setValue(Constants.WHOLE_AMOUNT, "")
                prefProvider.setValueInt("cardCount", 0)
                prefProvider.setValue(Constants.SUB_TOTAL, "")
                prefProvider.setValue(Constants.CASH_DISCOUNT_SURCHARGE, "")
                prefProvider.setValue(Constants.TOTAL_DISCOUNT, "")
                prefProvider.setValue(Constants.TIP, "")
                prefProvider.setValue(Constants.TAX_CHARGE, "")
                prefProvider.setValue(Constants.SERVICE_CHARGE, "")

                dashboardViewModel.deleteCart()
                prefProvider.setValue(Constants.ORDER_TYPE, OPEN_ORDER)
                prefProvider.setValue(Constants.ORDER_TYPE_NAME, order.orderTypeName)
                prefProvider.setValueInt(Constants.ORDER_TYPE_ID, order.orderTypeId)

                var itemDiscountTotal: Double = 0.0
                var itemPassDis: Double = 0.0
                order.orderItems.forEach {
                    if (it.discountAmount != 0.0) {
                        itemDiscountTotal += MethodUtils.roundOffAmountDouble(it.discountAmount)
                    }
                    if (it.discountAmount != 0.0 && it.quantity > 1) {

                        //  itemDiscountTotal += MethodUtils.roundOffAmountDouble(it.discountAmount / it.quantity)
                        it.discountAmount =
                            MethodUtils.roundOffAmountDouble(it.discountAmount / it.quantity)
                    }
                }
                LogUtil.logE(TAG, "itemDiscountTotal:  ${itemDiscountTotal}")
                LogUtil.logE(TAG, "totalOrderDiscount  ${order.totalDiscount}")

                order.totalDiscount = order.totalDiscount - itemDiscountTotal


                if (order.customer != null) {
                    prefProvider.setValue(
                        Constants.CUSTOMER_NAME,
                        order.customer.firstName + " " + order.customer.lastName
                    )
                    prefProvider.setValue(
                        Constants.RECEIPT_CUSTOMER_NAME,
                        order.customer.firstName + " " + order.customer.lastName
                    )
                    prefProvider.setValueInt(Constants.CUSTOMER_ID, order.customer.id)
                    prefProvider.saveCustomerData(TbCustomer.customerMapping(order.customer))
                }

                LogUtil.logE(TAG, "getOrder  ${Gson().toJson(order)}")
                dashboardViewModel.addCart(
                    cartModel(order)
                )

                val bundle = Bundle()
                bundle.putBoolean("update", true)
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
                bundle.putParcelable("cartList", cartModel(order))

                bundle.putInt("orderId", order.id)
                LogUtil.logE("orderId :: ", order.id.toString())
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
                bundle.putBoolean("isLoyaltyApplied", order.isLoyaltyApplied)
                prefProvider.setValueboolean(
                    Constants.IS_UPDATE_ORDER_LOYALTY_APPLIED,
                    order.isLoyaltyApplied
                )
                prefProvider.setValueboolean(
                    Constants.LOYALTY_ADDED,
                    order.isLoyaltyApplied
                )

                findNavController().navigate(R.id.action_orders_to_paymentBoldPosFragment, bundle)


            }

            PRINT_UNPAID -> {

                getCustomerPrinters(order, status)
            }

            PRINT_PAID -> {
                getCustomerPrinters(order, status)
            }

            else -> {
                if (rolePermission.hasCancelOrderPermission(binding.root)) {
                    val bundle = Bundle().apply {
                        /* putParcelable("refundData", refundData)
                         putDouble("refundAmount", subTotalPrice)*/

                        putInt("orderId", order.id)
                        putString("startDate", viewModel.startDate.value.toString())
                        putString("endDate", viewModel.endDate.value.toString())
                    }

                    findNavController().navigate(
                        R.id.action_order_fragment_to_reason_for_cancel_order_dialog,
                        bundle
                    )
                }
            }
        }

    }

    override fun noDataAvailableFilter() {

        runOnUiThread(Runnable {
            binding.llNoData.visible()
            binding.txtNodata.text = requireContext().getText(R.string.no_data_available)
        })

        Log.d("noDataAvailableFilter", "no data available")
    }

    override fun hideNoDataAvailable() {
        binding.llNoData.gone()
        Log.d("noDataAvailableFilter", "hide")
    }

    private fun cartModel(order: OpenOrderResponse.Data.Order): CartModel {
        LogUtil.logE("futureDeliveryDate  ", Gson().toJson(order))
        return CartModel().apply {
            terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, -1)
            employeeID = prefProvider.getValueInt(Constants.EMPLOYEE_ID, -1)
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
            discountPrice = order.totalDiscount
            deliveryType = order.deliveryType ?: ""
            taxlistDynamic = getTaxBirfucationList(order.orderItems)

        }
    }

    private fun getTaxBirfucationList(orderItems: List<OpenOrderResponse.Data.Order.OrderItem>): ArrayList<TaxData> {
        var taxListDynamic: ArrayList<TaxData> = arrayListOf()
        if (orderItems.isNotEmpty()) {
            orderItems.forEach { orderItem ->
                var totalPrice =
                    (orderItem.price * orderItem.quantity) - (orderItem.discountAmount * orderItem.quantity)
                orderItem.orderItemModifiers.forEach { orderItemModifier ->
                    totalPrice += orderItemModifier.price * orderItemModifier.quantity
                }
                Log.d(TAG, "navigate: itemPrice : $totalPrice")
                var totaltaxtemp = 0.0
                orderItem.orderItemTax.forEach { orderItemTaxe ->
                    if (taxListDynamic?.isNotEmpty() == true) {
                        var found = -1
                        taxListDynamic.forEachIndexed { index, taxData ->
                            if (taxData.orderTaxId == orderItemTaxe.taxId) {
                                found = index
                                return@forEachIndexed
                            }
                        }
                        if (found == -1) {
                            var taxData: TaxData = TaxData(
                                orderItemTaxe.createdAt,
                                orderItemTaxe.taxId,
                                0,
                                orderItemTaxe.name,
                                orderItemTaxe.rate,
                                orderItemTaxe.taxType,
                                orderItemTaxe.updatedAt,
                                true,
                                orderItemTaxe.isDefault,
                                false,
                                "",
                                listOf(orderItemTaxe.orderItemId),
                                orderItemTaxe.taxId,
                                false,
                                getTaxFromTotalPrice(
                                    orderItemTaxe,
                                    totalPrice,
                                    orderItem
                                ),
                                totalPrice
                            )
                            taxListDynamic?.add(taxData)
                        } else {
                            taxListDynamic!![found].totalTaxTypePrice =
                                taxListDynamic!![found].totalTaxTypePrice + getTaxFromTotalPrice(
                                    orderItemTaxe,
                                    totalPrice,
                                    orderItem
                                )
                            taxListDynamic!![found].subTotalAmount =
                                taxListDynamic!![found].subTotalAmount + totalPrice
                        }
                        Log.d(TAG, "found : " + found)
                    } else {
                        var taxData: TaxData = TaxData(
                            orderItemTaxe.createdAt,
                            orderItemTaxe.taxId,
                            0,
                            orderItemTaxe.name,
                            orderItemTaxe.rate,
                            orderItemTaxe.taxType,
                            orderItemTaxe.updatedAt,
                            true,
                            orderItemTaxe.isDefault,
                            false,
                            "",
                            listOf(orderItemTaxe.orderItemId),
                            orderItemTaxe.taxId,
                            false,
                            getTaxFromTotalPrice(
                                orderItemTaxe,
                                totalPrice,
                                orderItem
                            ),
                            totalPrice
                        )
                        taxListDynamic.add(taxData)
                    }


                    Log.d(TAG, "navigate: " + totaltaxtemp)
                }

            }

            Log.d(TAG, "navigate: list " + Gson().toJson(taxListDynamic))
        }
        return taxListDynamic
    }


    fun getTaxFromTotalPrice(
        orderItemTaxe: OpenOrderResponse.Data.Order.OrderItem.OrderItemTax,
        totalPrice: Double,
        item: OpenOrderResponse.Data.Order.OrderItem
    ): Double {
        var totaltaxtemp = 0.0


        totaltaxtemp += if (orderItemTaxe.taxType == "Percentage") {
            if (totalPrice < 0.0) {

                String.format("%.2f", 0.00)
                    .toDouble()
            } else {
                val itemTaxPrice =
                    (orderItemTaxe.rate * totalPrice) / 100
                LogUtil.logE("itemTaxPrice", "" + itemTaxPrice)
                itemTaxPrice
            }

        } else {
            Log.d("yash", "taxCalculation: " + orderItemTaxe.taxType)
            if (totalPrice <= 0.0) {
                String.format("%.2f", 0.00)
                    .toDouble()
            } else {
                String.format("%.2f", orderItemTaxe.rate * item.quantity)
                    .toDouble()
            }
        }
        return totaltaxtemp
    }

    private fun inventoryList(order: OpenOrderResponse.Data.Order): List<TbItem>? {

        val inventoryModelList = ArrayList<TbItem>()

        order.orderItems.forEach {
            var ismanualsale = false
            var mannual_Sale_ID = ""
            if (it.itemId == 1) {
                mannual_Sale_ID = UUID.randomUUID().toString()
                ismanualsale = true
            }
            val items = TbItem().apply {
                orderItemId = it.id
                itemId = it.itemId
                id = it.custom_item_id
                name = it.itemName
                isManualSales = ismanualsale
                manualSaleId = mannual_Sale_ID
                price = it.price
                isEdited = it.isEdited
                itemQuantity = it.quantity
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
                modifier_quantity = it.modifier_quantity

            }
            modifierList.add(modifier)
        }

        return modifierList
    }

    private fun modifiersIds(orderItemModifiers: List<OpenOrderResponse.Data.Order.OrderItem.OrderItemModifier>): List<Int> {

        val selectedIds = ArrayList<Int>()
        if (orderItemModifiers.isNotEmpty()) {
            orderItemModifiers.forEach {
                selectedIds.add(it.modifierSetId)
            }
        }
        var uniqueSelectedId = HashSet<Int>(selectedIds)
        return uniqueSelectedId.toList()
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
                it.createdAt.toString(),
                it.serviceChargeId,
                true,
                order.locationId,
                it.max_guest_count,
                it.min_guest_count,
                it.name,
                it.order_type,
                it.rate,
                it.updatedAt.toString(),
                isActive = false,
                isChecked = true,
                order_service_charge_id = it.id
            )
            serviceChargeList.add(serviceCharge)
        }

        return serviceChargeList
    }

    private fun getCustomerReceiptSettings() {
        viewModel.getCustomerReceiptSettings().observe(viewLifecycleOwner) {
            if (it != null) {
                customerSettingModel = it


            }

        }

    }

    private fun getCustomerPrinters(order: OpenOrderResponse.Data.Order, type: String) {

        viewModel.getCustomerPrinterList().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    ProgressUtils.dismissProgressDialog()
                    if (it.data != null) {
                        val customerList = it.data

                        customerList.forEach {
                            if (it.status) {
                                initPrinter(it, Constants.CUSTOMER, order, type)
                            }


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

        }

    }


    private fun initPrinter(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String,
        order: OpenOrderResponse.Data.Order,
        printType: String
    ) {

        if (customerReceiptPrinters.name.startsWith(SUNMI_PRINTER, true)) {

            SunmiPrinterApi.getInstance()
                .setPrinter(SunmiPrinter.SunmiBlueToothPrinter, customerReceiptPrinters.ipAddress)


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
                            generatePrintSunmi(customerReceiptPrinters, type, order, printType)


                        }

                        override fun onDisconnect() {
                            println("onDisconnect")
                        }

                    })
            } else {
                generatePrintSunmi(customerReceiptPrinters, type, order, printType)


            }


        } else if (customerReceiptPrinters.name.startsWith(SUNMI_INNER_PRINTER, true)) {

            SunmiPrintHelper.getInstance().initSunmiPrinterService(requireContext())
            viewLifecycleOwner.lifecycleScope.launch {
                delay(100)
                setService(customerReceiptPrinters, type, order, printType)
            }


        } else {
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
                    LogUtil.logE(TAG, "PrinterException: " + e.message)
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
                LogUtil.logE(TAG, "PrinterIsNotNull:")
            }
        }

    }

    private fun setService(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String,
        order: OpenOrderResponse.Data.Order,
        printType: String
    ) {
        if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.FoundSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper", "FoundSunmiPrinter")

            if (!BluetoothUtil.isBlueToothPrinter) {

                generatePrintSunmiInner(customerReceiptPrinters, type, order, printType)

            }

        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.CheckSunmiPrinter) {
            Handler(Looper.getMainLooper()).postDelayed({
                setService(
                    customerReceiptPrinters,
                    type,
                    order,
                    printType
                )
            }, 2000)
            LogUtil.logE("SunmiPrintHelper", "CheckSunmiPrinter")
        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.LostSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper", "LostSunmiPrinter")
        } else {
            LogUtil.logE("SunmiPrintHelper", "ELSE")
        }
    }

    private fun generatePrint(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String,
        receiptModel: OpenOrderResponse.Data.Order,
        printType: String
    ) {
        var builder: Builder? = null
        LogUtil.logE(TAG, "customerSettingModel:  ${Gson().toJson(customerSettingModel)}")
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

            LogUtil.logE(TAG, "getVanueLogo:  ${prefProvider.getValue(Constants.VENUE_LOGO, "")}")


            if (customerSettingModel.showOrderIdTop) {
                builder.addFeedLine(1)
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
                    builder.addText("OrderID:" + receiptModel.custom_order_id)
                } else {
                    builder.addText("OrderID:" + receiptModel.id)
                }

                builder.addFeedLine(1)
            }


            if (customerSettingModel.showVenueLogo && prefProvider.getValue(
                    Constants.VENUE_LOGO,
                    ""
                )
                    .isNotEmpty()
            ) {
                builder.addFeedLine(1)
                builder.addTextAlign(Builder.ALIGN_CENTER)

                /* var bitmap = getBitmapFromURL(prefProvider.getValue(VENUE_LOGO, ""))*/

                val decodedString: ByteArray = android.util.Base64.decode(
                    prefProvider.getValue(Constants.VENUE_LOGO, ""),
                    android.util.Base64.DEFAULT
                )
                val bitmap: Bitmap =
                    BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

                val newBitmap = Bitmap.createScaledBitmap(bitmap!!, 210, 210, true)
                builder.addImage(
                    newBitmap, 0, 0,
                    newBitmap.width, newBitmap.height, Builder.COLOR_1, Builder.MODE_MONO,
                    Builder.HALFTONE_DITHER, 1.0
                )
            }

            builder.addFeedLine(1)
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

            if (customerSettingModel.showVenueAddress) {
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
            }
            if (customerSettingModel.showVenuePhone) {
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
                    MethodUtils.getUSFormatNumber(
                        prefProvider.getValue(Constants.BUSINESS_PHONE_NO, "").toString()
                    )
                )
            }

            if (customerSettingModel.showWebsiteAddress) {
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
                    prefProvider.getValue(Constants.BUSINESS_WEBSITE, "")
                )
            }

            if (customerSettingModel.showOrderType) {
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
                builder.addText(receiptModel?.orderTypeName + "\n")
            }

            /*if (receiptModel?.orderType?.lowercase() == Constants.OPEN_ORDER.lowercase()
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
//                builder.addText(receiptModel?.deliveryType + "\n")


            }*/




            if (customerSettingModel.fonts == Constants.LARGE) {


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
                            requireContext(),
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
                        "ReceiptID:" + receiptModel?.offlineId,
                        "",
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
                            if (customerSettingModel.showOrderTime) {
                                "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                                    requireContext(),
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
                                if (customerSettingModel.showPrintTime) {
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
//                            "-$" + MethodUtils.roundOffAmountString(receiptModel.totalDiscount)
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

            if (receiptModel.totalServiceCharges != null && receiptModel.serviceChargeEnabled && prefProvider.getValueboolean(
                    SERVICECHARGE_TAKEOUT_OPENORDER, false
                )
            ) {
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




            if (receiptModel.cash_discount_or_surcharge != 0.0 && customerSettingModel.showCashDisSurCharg) {
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


                if (receiptModel.payments.isNotEmpty() && receiptModel.payments.get(receiptModel.payments.size - 1).paymentType.lowercase() == "Card".lowercase()) {
                    builder.addText(
                        padLine(
                            Constants.SURCHARGE_TEXT,
                            "$" + MethodUtils.roundOffAmountString(receiptModel.cash_discount_or_surcharge!!),
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )
                } else {

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
            }

            if (receiptModel?.isLoyaltyApplied == true && receiptModel?.loyaltyAmount != 0.0) {


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
                        "Used Loyalty Amount",
                        "-$" + receiptModel?.loyaltyAmount?.let {
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
                        "Used Loyalty Points",
                        receiptModel?.usedRewardPoints.toString(),
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

            if (receiptModel.cashDiscountType == "CashDiscount"
            ) {

                var cashdiscountAmount = 0.0
                if (MethodUtils.isEnableCashDiscount(requireContext())) {
                    cashdiscountAmount = MethodUtils.calculateCashDiscount(
                        receiptModel.totalAmount,
                        prefProvider,
                        requireContext()
                    )
                } else {
                    cashdiscountAmount = 0.0
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
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                val totalAmt = MethodUtils.roundOffAmountDouble(receiptModel.totalAmount)

                builder.addText(
                    padLine(
                        "Pay by Cash",
                        "$" + MethodUtils.roundOffAmountString(totalAmt - cashdiscountAmount),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

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
                        "Pay by Card",
                        "$" + MethodUtils.roundOffAmountString(totalAmt),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )


            } else if (receiptModel.cashDiscountType == "SurCharge"
            ) {


                var cashdiscountAmount = 0.0
                if (MethodUtils.isEnableCashDiscount(requireContext())) {
                    cashdiscountAmount = MethodUtils.calculateCashDiscount(
                        receiptModel.totalAmount,
                        prefProvider,
                        requireContext()
                    )
                } else {
                    cashdiscountAmount = 0.0
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
                    Builder.TRUE,
                    Builder.COLOR_1
                )

                val totalAmt = MethodUtils.roundOffAmountDouble(receiptModel.totalAmount)

                builder.addText(
                    padLine(
                        "Pay by Cash",
                        "$" + MethodUtils.roundOffAmountString(totalAmt),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )

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
                        "Pay by Card",
                        "$" + MethodUtils.roundOffAmountString(totalAmt + cashdiscountAmount),
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
                        receiptModel.totalAmount.toDouble(),
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
                        receiptModel.payments.get(0).id.toString(),
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
                            LogUtil.logE(TAG, "phoneNoFormatted:  ${phoneNoFormatted}")
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
                            receiptModel.customer?.addresses.filter { it.typeOfAddress == SHIPPING_ADDRESS }
                                .forEach {

                                    if (it.typeOfAddress.equals(
                                            SHIPPING_ADDRESS,
                                            ignoreCase = true
                                        )
                                    ) {
                                        builder.addText(
                                            it.fullAddress
                                        )
                                    }
                                }

//                            builder.addText(receiptModel.customer?.addresses?.get(receiptModel.customer?.addresses?.size - 1)?.fullAddress)
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
                builder.addTextAlign(Builder.ALIGN_CENTER)
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

            //customer signature line.
            builder.addText(
                padLine(
                    "Customer Signature",
                    addHorizontalHalfCustomerReceiptLine(customerSettingModel.fonts),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        24
                    } else {
                        48
                    }
                )
            )




            if (customerSettingModel.showQrCode) {
                builder.addFeedLine(1)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                val bitmap = generateQRCode(receiptModel.digitalReceiptUrl)
                LogUtil.logE(TAG, "BitmapHeight ${bitmap.height}")
                LogUtil.logE(TAG, "BitmapWidth ${bitmap.width}")
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
                LogUtil.logE(TAG, "PrinterError: " + e.localizedMessage)
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun generatePrintSunmi(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String,
        receiptModel: OpenOrderResponse.Data.Order,
        printType: String
    ) {
        try {

            PrintSunmiUtils.fontSize(customerSettingModel.fonts)

            if (customerSettingModel.showOrderIdTop) {
                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    PrintSunmiUtils.orderIdLarge("OrderID:" + receiptModel.custom_order_id)
                } else {
                    PrintSunmiUtils.orderIdLarge("OrderID:" + receiptModel?.id)
                }
                SunmiPrinterApi.getInstance().lineWrap(1)
            }


            if (customerSettingModel.showVenueLogo && prefProvider.getValue(
                    Constants.VENUE_LOGO,
                    ""
                )
                    .isNotEmpty()
            ) {

                printBusinessLogo()
            }


            if (printType == PRINT_PAID) {
                PrintSunmiUtils.paidStatus("Paid")
            } else {
                PrintSunmiUtils.paidStatus("Unpaid")
            }

            PrintSunmiUtils.printBusinessDetails(
                prefProvider.getValue(Constants.BUSINESS_NAME, ""),
                if (customerSettingModel.showVenueAddress) prefProvider.getValue(
                    BUSINESS_ADDRESS,
                    ""
                ) else "",
                if (customerSettingModel.showVenuePhone) prefProvider.getValue(
                    Constants.BUSINESS_PHONE_NO,
                    ""
                ) else ""
            )

            if (customerSettingModel.showWebsiteAddress) {
                PrintSunmiUtils.venueWebsite(prefProvider.getValue(Constants.BUSINESS_WEBSITE, ""))
            }

            if (customerSettingModel.showOrderType) {
                PrintSunmiUtils.printOrderType(receiptModel?.orderTypeName?.trim())
            }


            if (receiptModel?.orderType?.lowercase() == Constants.OPEN_ORDER.lowercase()
                || receiptModel?.orderType?.lowercase() == Constants.OPEN_ORDER.lowercase()
            ) {

//                PrintSunmiUtils.deliveryType(receiptModel?.deliveryType)

            }


            SunmiPrinterApi.getInstance().lineWrap(1)


            if (customerSettingModel.fonts == Constants.LARGE) {


                PrintSunmiUtils.receiptID("ReceiptID:" + receiptModel?.offlineId)


                if (customerSettingModel.showTeam) {

                    PrintSunmiUtils.employee("Employee:" + receiptModel?.employee?.name)

                }

                if (customerSettingModel.showOrderTime) {


                    PrintSunmiUtils.orderTime(
                        "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            receiptModel?.createdAt.toString()
                        )
                    )

                }

                if (customerSettingModel.showPrintTime) {


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        PrintSunmiUtils.orderTime(
                            "Print Time:" + Constants.getCurrentTimeFromTimeZone(
                                requireContext(),
                                MethodUtils.formatted()
                            )
                        )
                    }


                }
            } else {


                val str = padLine(

                    "ReceiptID:" + receiptModel?.offlineId,
                    "",
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString().trim()

                PrintSunmiUtils.orderId(str)
                if (customerSettingModel.showTeam) {


                    val empName = padLine(
                        if (customerSettingModel.showTeam) {
                            "Employee:" + receiptModel.employee.name
                        } else {
                            ""
                        },
                        "",
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()

                    PrintSunmiUtils.employee(empName)
                }
                if (customerSettingModel.showOrderTime) {


                    val orderTime = padLine(
                        if (customerSettingModel.showOrderTime) {
                            "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                                requireContext(),
                                receiptModel?.createdAt.toString()
                            )
                        } else {
                            ""
                        },
                        "",
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()

                    PrintSunmiUtils.orderTime(orderTime)
                }


                if (customerSettingModel.showPrintTime) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {


                        val current = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy hh:mm:a")
                        val formatted = current.format(formatter)


                        val printTime = padLine(
                            if (customerSettingModel.showPrintTime) {
                                "Print Time:$formatted"
                            } else {
                                ""
                            },
                            "",
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                23
                            } else {
                                48
                            }
                        ).toString()

                        PrintSunmiUtils.orderTime(printTime)

                    }
                }
            }


            PrintSunmiUtils.addHorizontal()



            receiptModel.orderItems.let {
                addOrderItemOpenOrderSunmi(
                    it,
                    customerSettingModel.fonts,
                    customerSettingModel.showModifiers
                )
            }

            SunmiPrinterApi.getInstance().lineWrap(2)


            if (receiptModel?.totalDiscount != null) {

                val str1 = padLine(
                    "Total Discount",

                    if (receiptModel.totalDiscount == 0.0) {
//                        "-$" + MethodUtils.roundOffAmountString(receiptModel.totalDiscount)
                        "$" + MethodUtils.roundOffAmountString(receiptModel.totalDiscount)
                    } else {
                        "-$" + MethodUtils.roundOffAmountString(receiptModel.totalDiscount)
                    },
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.totalDiscount(str1)

            }


            val str2 = padLine(
                "Sub Total",
                "$" + MethodUtils.roundOffAmountString(receiptModel.subTotal),
                if (customerSettingModel.fonts == Constants.LARGE) {
                    23
                } else {
                    48
                }
            ).toString()

            PrintSunmiUtils.subTotal(str2)


            if (receiptModel?.totalTaxAmount != null) {


                val str3 = padLine(
                    "Tax",
                    "$" + MethodUtils.roundOffAmountString(receiptModel.totalTaxAmount),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.tax(str3)
            }

            if (receiptModel.totalServiceCharges != null && receiptModel.serviceChargeEnabled && prefProvider.getValueboolean(
                    SERVICECHARGE_TAKEOUT_OPENORDER, false
                )
            ) {


                val str4 = padLine(
                    "Service Charge",
                    "$" + MethodUtils.roundOffAmountString(receiptModel.totalServiceCharges),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.serviceCharge(str4)
            }

            if (receiptModel?.totalTips != 0.0) {


                val str8 = padLine(
                    "Tips",
                    "$" + receiptModel.totalTips?.let {
                        MethodUtils.roundOffAmountString(
                            it
                        )
                    },
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.tip(str8)


            }




            if (receiptModel.cash_discount_or_surcharge != 0.0 && customerSettingModel.showCashDisSurCharg) {


                if (receiptModel.payments.isNotEmpty() && receiptModel.payments.get(receiptModel.payments.size - 1).paymentType.lowercase() == "Card".lowercase()) {

                    val str8 = padLine(
                        Constants.SURCHARGE_TEXT,
                        "$" + MethodUtils.roundOffAmountString(receiptModel.cash_discount_or_surcharge!!),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                    PrintSunmiUtils.surCharge(str8)

                } else {

                    val str8 = padLine(
                        "Cash Discount",
                        if (receiptModel.cash_discount_or_surcharge == 0.0) {
                            "$" + MethodUtils.roundOffAmountString(receiptModel.cash_discount_or_surcharge!!)
                        } else {
                            "-$" + MethodUtils.roundOffAmountString(receiptModel.cash_discount_or_surcharge!!)
                        },
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                    PrintSunmiUtils.cashDiscount(str8)

                }
            }

            if (receiptModel?.isLoyaltyApplied == true && receiptModel?.loyaltyAmount != 0.0) {

                val str8 = padLine(
                    "Used Loyalty Amount",
                    "-$" + receiptModel?.loyaltyAmount?.let {
                        MethodUtils.roundOffAmountString(
                            it
                        )
                    },
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()

                PrintSunmiUtils.loyaltyAmount(str8)

                val str9 = padLine(
                    "Used Loyalty Points",
                    receiptModel?.usedRewardPoints.toString(),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()

                PrintSunmiUtils.loyaltyPoint(str9)


            }




            SunmiPrinterApi.getInstance().lineWrap(1)

            if (receiptModel.totalAmount != null) {

                val totalAmt = MethodUtils.roundOffAmountDouble(receiptModel.totalAmount)

                val str5 = padLine(
                    "Total Price",
                    "$" + MethodUtils.roundOffAmountString(totalAmt),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.totalPrice(str5)

            }




            if (receiptModel.cashDiscountType == "CashDiscount"
            ) {

                var cashdiscountAmount = 0.0
                if (MethodUtils.isEnableCashDiscount(requireContext())) {
                    cashdiscountAmount = MethodUtils.calculateCashDiscount(
                        receiptModel.totalAmount,
                        prefProvider,
                        requireContext()
                    )
                } else {
                    cashdiscountAmount = 0.0
                }

                val totalAmt = MethodUtils.roundOffAmountDouble(receiptModel.totalAmount)
                val str5 = padLine(
                    "Pay by Cash",
                    "$" + MethodUtils.roundOffAmountString(totalAmt - cashdiscountAmount),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.totalPrice(str5)

                val totalAmt1 = MethodUtils.roundOffAmountDouble(totalAmt)
                val str51 = padLine(
                    "Pay by Card",
                    "$" + MethodUtils.roundOffAmountString(totalAmt1),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.totalPrice(str51)
            } else if (receiptModel.cashDiscountType == "SurCharge"
            ) {

                var cashdiscountAmount = 0.0
                cashdiscountAmount = MethodUtils.calculateCashDiscount(
                    receiptModel.totalAmount,
                    prefProvider,
                    requireContext()
                )

                val totalAmt = MethodUtils.roundOffAmountDouble(receiptModel.totalAmount)
                val str5 = padLine(
                    "Pay by Cash",
                    "$" + MethodUtils.roundOffAmountString(totalAmt),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.totalPrice(str5)

                val totalAmt1 = MethodUtils.roundOffAmountDouble(totalAmt + cashdiscountAmount)
                val str51 = padLine(
                    "Pay by Card",
                    "$" + MethodUtils.roundOffAmountString(totalAmt1),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.totalPrice(str51)

            }



            if (customerSettingModel.showRefundAmount && printType == PRINT_PAID) {

                val str7 = padLine(
                    "Change Amount",
                    "$" + MethodUtils.roundOffAmountString(0.00),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.changeAmount(str7)
                SunmiPrinterApi.getInstance().lineWrap(2)


            }

            if (receiptModel?.totalTips == 0.0 && printType == PRINT_PAID) {


                if (customerSettingModel.showTipLineForCash) {

                    if (customerSettingModel.fonts == Constants.LARGE) {
                        PrintSunmiUtils.tips("Tips      _____________")
                        SunmiPrinterApi.getInstance().lineWrap(1)
                    } else {
                        PrintSunmiUtils.tips("Tips                              _____________")
                    }

                }

            }

            SunmiPrinterApi.getInstance().lineWrap(1)


            if (customerSettingModel.showTipSuggestion) {


                PrintSunmiUtils.additionalTips()

                if (tipsList.isNotEmpty()) {
                    addTipsList(
                        tipsList,
                        receiptModel.totalAmount.toDouble(),
                        customerSettingModel.fonts
                    )

                }
            }

            if (printType == PRINT_PAID) {

                val str10 = padLine(
                    "Transaction ID",
                    receiptModel.payments.get(0).transactionId,
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.transactionId(str10)
            }

            if (printType == PRINT_PAID) {

                val str11 = padLine(
                    "Transaction Type",
                    receiptModel.payments.get(0).paymentType,
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.transactionType(str11)
                SunmiPrinterApi.getInstance().lineWrap(1)
            }
            if (customerSettingModel.showCustomerAddress or customerSettingModel.showCustomerPhone or customerSettingModel.showCustomerName) {

                SunmiPrinterApi.getInstance().lineWrap(1)

                if (receiptModel.customer != null) {

                    PrintSunmiUtils.customerDetails()

                    if (customerSettingModel.showCustomerName) {
                        PrintSunmiUtils.customerName(receiptModel.customer.firstName + " " + receiptModel.customer.lastName)
                    }

                    if (customerSettingModel.showCustomerPhone) {
                        if (receiptModel?.customer?.phones?.isNotEmpty()) {

                            val phoneNoFormatted = MethodUtils.getUSFormatNumber(
                                receiptModel?.customer?.phones?.get(receiptModel?.customer?.phones?.size - 1).phoneNumber
                            )
                            PrintSunmiUtils.customerPhone(phoneNoFormatted)

                        }
                    }

                    if (customerSettingModel.showCustomerAddress) {
                        if (receiptModel.customer?.addresses?.isNotEmpty() == true) {

//                            PrintSunmiUtils.customerAddress(
//                                receiptModel.customer?.addresses?.get(receiptModel.customer?.addresses?.size - 1)?.fullAddress
//                            )

                            receiptModel.customer?.addresses.filter { it.typeOfAddress == SHIPPING_ADDRESS }
                                .forEach {

                                    if (it.typeOfAddress.equals(
                                            SHIPPING_ADDRESS,
                                            ignoreCase = true
                                        )
                                    ) {
                                        PrintSunmiUtils.customerAddress(
                                            it.fullAddress
                                        )
                                    }
                                }
                        }
                    }

                }
            }


            if (receiptModel.note != null && receiptModel.note != "" && customerSettingModel.showOrderNote) {
                SunmiPrinterApi.getInstance().lineWrap(1)
                PrintSunmiUtils.orderNote(receiptModel.note)
            }
            SunmiPrinterApi.getInstance().lineWrap(2)
            val str8 = padLine(
                "Customer Signature",
                "     _________________________",
                48
            ).toString()

            PrintSunmiUtils.customerSignature(str8)

            if (customerSettingModel.showQrCode) {

                PrintSunmiUtils.qrCode(receiptModel.digitalReceiptUrl)
            }

            PrintSunmiUtils.cutPaper()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun generatePrintSunmiInner(
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters,
        type: String,
        receiptModel: OpenOrderResponse.Data.Order,
        printType: String
    ) {
        try {

            PrintSunmiUtils.fontSizeInner(customerSettingModel.fonts)

            SunmiPrintHelper.getInstance().initPrinter()
            if (customerSettingModel.showOrderIdTop) {
                if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                    PrintSunmiUtils.headerText("OrderID:" + receiptModel?.custom_order_id)
                } else {
                    PrintSunmiUtils.headerText("OrderID:" + receiptModel?.id)
                }
                SunmiPrintHelper.getInstance().lineWrap(1)
            }


            if (customerSettingModel.showVenueLogo && prefProvider.getValue(
                    Constants.VENUE_LOGO,
                    ""
                )
                    .isNotEmpty()
            ) {

                PrintSunmiUtils.printLogoInner(
                    prefProvider.getValue(
                        Constants.VENUE_LOGO,
                        ""
                    )
                )
            }


            if (printType == PRINT_PAID) {
                PrintSunmiUtils.headerText("Paid")
            } else {
                PrintSunmiUtils.headerText("Unpaid")
            }

            PrintSunmiUtils.printBusinessDetailsInner(
                prefProvider.getValue(Constants.BUSINESS_NAME, ""),
                if (customerSettingModel.showVenueAddress) prefProvider.getValue(
                    BUSINESS_ADDRESS,
                    ""
                ) else "", prefProvider.getValue(
                    Constants.BUSINESS_PHONE_NO,
                    ""
                )
                /*if (customerSettingModel.showVenuePhone) prefProvider.getValue(
                    Constants.BUSINESS_PHONE_NO,
                    ""
                ) else ""*/
            )
            if (customerSettingModel.showWebsiteAddress) {
                PrintSunmiUtils.venueWebsiteInner(
                    prefProvider.getValue(
                        Constants.BUSINESS_WEBSITE,
                        ""
                    )
                )
            } else {
                SunmiPrintHelper.getInstance().lineWrap(1)
            }
            if (customerSettingModel.showOrderType) {
                PrintSunmiUtils.headerText(receiptModel?.orderTypeName?.trim())
            }


            if (receiptModel?.orderType?.lowercase() == Constants.OPEN_ORDER.lowercase()
                || receiptModel?.orderType?.lowercase() == Constants.OPEN_ORDER.lowercase()
            ) {
//                PrintSunmiUtils.headerText(receiptModel?.deliveryType)

            }
            SunmiPrintHelper.getInstance().lineWrap(1)




            if (customerSettingModel.fonts == Constants.LARGE) {


                PrintSunmiUtils.normalText("ReceiptID:" + receiptModel?.offlineId)


                if (customerSettingModel.showTeam) {

                    PrintSunmiUtils.normalText("Employee:" + receiptModel?.employee?.name)

                }

                if (customerSettingModel.showOrderTime) {


                    PrintSunmiUtils.normalText(
                        "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                            requireContext(),
                            receiptModel?.createdAt.toString()
                        )
                    )

                }

                if (customerSettingModel.showPrintTime) {


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        PrintSunmiUtils.normalText(
                            "Print Time:" + Constants.getCurrentTimeFromTimeZone(
                                requireContext(),
                                MethodUtils.formatted()
                            )
                        )
                    }


                }
            } else {

                PrintSunmiUtils.normalText("ReceiptID:" + receiptModel?.offlineId)
                if (customerSettingModel.showTeam) {


                    val empName = padLine(
                        if (customerSettingModel.showTeam) {
                            "Employee:" + receiptModel.employee.name
                        } else {
                            ""
                        },
                        "",
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()

                    PrintSunmiUtils.normalText(empName)
                }
                if (customerSettingModel.showOrderTime) {


                    val orderTime = padLine(
                        if (customerSettingModel.showOrderTime) {
                            "Order Time:" + Constants.getReceiptFormatDateFromUTCServer(
                                requireContext(),
                                receiptModel?.createdAt.toString()
                            )
                        } else {
                            ""
                        },
                        "",
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()

                    PrintSunmiUtils.normalText(orderTime)
                }


                if (customerSettingModel.showPrintTime) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                        val printTime = padLine(
                            if (customerSettingModel.showPrintTime) {
                                "Print Time:${MethodUtils.formatted()}"
                            } else {
                                ""
                            },
                            "",
                            if (customerSettingModel.fonts == Constants.LARGE) {
                                23
                            } else {
                                48
                            }
                        ).toString()

                        PrintSunmiUtils.normalText(printTime)

                    }
                }
            }


            PrintSunmiUtils.addHorizontalInner()



            receiptModel.orderItems.let {
                addOrderItemOpenOrderSunmiInner(
                    it,
                    customerSettingModel.fonts,
                    customerSettingModel.showModifiers
                )
            }

            SunmiPrintHelper.getInstance().lineWrap(2)


            if (receiptModel?.totalDiscount != null) {

                val str1 = padLine(
                    "Total Discount",

                    if (receiptModel.totalDiscount == 0.0) {
//                        "-$" + MethodUtils.roundOffAmountString(receiptModel.totalDiscount)
                        "$" + MethodUtils.roundOffAmountString(receiptModel.totalDiscount)
                    } else {
                        "-$" + MethodUtils.roundOffAmountString(receiptModel.totalDiscount)
                    },
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.normalText(str1)

            }


            val str2 = padLine(
                "Sub Total",
                "$" + MethodUtils.roundOffAmountString(receiptModel.subTotal),
                if (customerSettingModel.fonts == Constants.LARGE) {
                    23
                } else {
                    48
                }
            ).toString()

            PrintSunmiUtils.normalText(str2)


            if (receiptModel?.totalTaxAmount != null) {


                val str3 = padLine(
                    "Tax",
                    "$" + MethodUtils.roundOffAmountString(receiptModel.totalTaxAmount),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.normalText(str3)
            }

            if (receiptModel.totalServiceCharges != null && receiptModel.serviceChargeEnabled && prefProvider.getValueboolean(
                    SERVICECHARGE_TAKEOUT_OPENORDER, false
                )
            ) {


                val str4 = padLine(
                    "Service Charge",
                    "$" + MethodUtils.roundOffAmountString(receiptModel.totalServiceCharges),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.normalText(str4)
            }

            if (receiptModel?.totalTips != 0.0) {


                val str8 = padLine(
                    "Tips",
                    "$" + receiptModel.totalTips?.let {
                        MethodUtils.roundOffAmountString(
                            it
                        )
                    },
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.normalText(str8)


            }




            if (receiptModel.cash_discount_or_surcharge != 0.0 && customerSettingModel.showCashDisSurCharg) {


                if (receiptModel.payments.isNotEmpty() && receiptModel.payments.get(receiptModel.payments.size - 1).paymentType.lowercase() == "Card".lowercase()) {

                    val str8 = padLine(
                        Constants.SURCHARGE_TEXT,
                        "$" + MethodUtils.roundOffAmountString(receiptModel.cash_discount_or_surcharge!!),
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                    PrintSunmiUtils.normalText(str8)

                } else {

                    val str8 = padLine(
                        "Cash Discount",
                        if (receiptModel.cash_discount_or_surcharge == 0.0) {
                            "$" + MethodUtils.roundOffAmountString(receiptModel.cash_discount_or_surcharge!!)
                        } else {
                            "-$" + MethodUtils.roundOffAmountString(receiptModel.cash_discount_or_surcharge!!)
                        },
                        if (customerSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                    PrintSunmiUtils.normalText(str8)

                }
            }

            if (receiptModel?.isLoyaltyApplied == true && receiptModel?.loyaltyAmount != 0.0) {

                val str8 = padLine(
                    "Used Loyalty Amount",
                    "-$" + receiptModel?.loyaltyAmount?.let {
                        MethodUtils.roundOffAmountString(
                            it
                        )
                    },
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()

                PrintSunmiUtils.normalText(str8)

                val str9 = padLine(
                    "Used Loyalty Points",
                    receiptModel?.usedRewardPoints.toString(),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()

                PrintSunmiUtils.normalText(str9)


            }




            SunmiPrintHelper.getInstance().lineWrap(1)

            if (receiptModel.totalAmount != null) {

                val totalAmt = MethodUtils.roundOffAmountDouble(receiptModel.totalAmount)

                val str5 = padLine(
                    "Total Price",
                    "$" + MethodUtils.roundOffAmountString(totalAmt),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.boldText(str5)

            }


            if (receiptModel.cashDiscountType == "CashDiscount"
            ) {

                var cashdiscountAmount = 0.0
                if (MethodUtils.isEnableCashDiscount(requireContext())) {
                    cashdiscountAmount = MethodUtils.calculateCashDiscount(
                        receiptModel.totalAmount,
                        prefProvider,
                        requireContext()
                    )
                } else {
                    cashdiscountAmount = 0.0
                }

                val totalAmt = MethodUtils.roundOffAmountDouble(receiptModel.totalAmount)
                val str5 = padLine(
                    "Pay by Cash",
                    "$" + MethodUtils.roundOffAmountString(totalAmt - cashdiscountAmount),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.totalPrice(str5)

                val totalAmt1 = MethodUtils.roundOffAmountDouble(totalAmt)
                val str51 = padLine(
                    "Pay by Card",
                    "$" + MethodUtils.roundOffAmountString(totalAmt1),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.boldText(str51)
            } else if (receiptModel.cashDiscountType == "SurCharge"
            ) {

                var cashdiscountAmount = 0.0
                cashdiscountAmount = MethodUtils.calculateCashDiscount(
                    receiptModel.totalAmount,
                    prefProvider,
                    requireContext()
                )

                val totalAmt = MethodUtils.roundOffAmountDouble(receiptModel.totalAmount)
                val str5 = padLine(
                    "Pay by Cash",
                    "$" + MethodUtils.roundOffAmountString(totalAmt),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.totalPrice(str5)

                val totalAmt1 = MethodUtils.roundOffAmountDouble(totalAmt + cashdiscountAmount)
                val str51 = padLine(
                    "Pay by Card",
                    "$" + MethodUtils.roundOffAmountString(totalAmt1),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.boldText(str51)

            }


            if (customerSettingModel.showRefundAmount && printType == PRINT_PAID) {

                val str7 = padLine(
                    "Change Amount",
                    "$" + MethodUtils.roundOffAmountString(0.00),
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.boldText(str7)
                SunmiPrintHelper.getInstance().lineWrap(2)


            }

            if (receiptModel?.totalTips == 0.0 && printType == PRINT_PAID) {


                var tip = ""

                if (receiptModel.totalTips != 0.0) {
                    tip = receiptModel.totalTips.toString()
                }
                if (customerSettingModel.showTipLineForCash) {

                    if (customerSettingModel.fonts == Constants.LARGE) {
                        PrintSunmiUtils.boldText("Tips      _____________")
                        SunmiPrintHelper.getInstance().lineWrap(1)
                    } else {
                        PrintSunmiUtils.boldText("Tips                              _____________")
                    }
                }

            }

            SunmiPrintHelper.getInstance().lineWrap(1)


            if (customerSettingModel.showTipSuggestion) {


                PrintSunmiUtils.additionalTipsInner()

                if (tipsList.isNotEmpty()) {
                    addTipsListInner(
                        tipsList,
                        receiptModel.totalAmount.toDouble(),
                        customerSettingModel.fonts
                    )

                }
            }

            if (printType == PRINT_PAID) {

                val str10 = padLine(
                    "Transaction ID",
                    receiptModel.payments.get(0).transactionId,
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.normalText(str10)
            }

            if (printType == PRINT_PAID) {

                val str11 = padLine(
                    "Transaction Type",
                    receiptModel.payments.get(0).paymentType,
                    if (customerSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
                PrintSunmiUtils.normalText(str11)
            }
            if (customerSettingModel.showCustomerAddress or customerSettingModel.showCustomerPhone or customerSettingModel.showCustomerName) {

                SunmiPrintHelper.getInstance().lineWrap(1)

                if (receiptModel.customer != null) {

                    PrintSunmiUtils.customerDetailsInner()

                    if (customerSettingModel.showCustomerName) {
                        PrintSunmiUtils.normalText(receiptModel.customer.firstName + " " + receiptModel.customer.lastName)
                    }

                    if (customerSettingModel.showCustomerPhone) {
                        if (receiptModel?.customer?.phones?.isNotEmpty()) {

                            val phoneNoFormatted = MethodUtils.getUSFormatNumber(
                                receiptModel?.customer?.phones?.get(receiptModel?.customer?.phones?.size - 1).phoneNumber
                            )
                            PrintSunmiUtils.normalText(phoneNoFormatted)

                        }
                    }

                    if (customerSettingModel.showCustomerAddress) {
                        if (receiptModel.customer?.addresses?.isNotEmpty() == true) {

                            receiptModel.customer?.addresses.filter { it.typeOfAddress == SHIPPING_ADDRESS }
                                .forEach {

                                    if (it.typeOfAddress.equals(
                                            SHIPPING_ADDRESS,
                                            ignoreCase = true
                                        )
                                    ) {
                                        PrintSunmiUtils.normalText(
                                            it.fullAddress
                                        )
                                    }
                                }

//                            PrintSunmiUtils.normalText(
//                                receiptModel.customer?.addresses?.get(receiptModel.customer?.addresses?.size - 1)?.fullAddress
//                            )
                        }
                    }

                }
            }


            if (receiptModel.note != null && receiptModel.note != "" && customerSettingModel.showOrderNote) {
                SunmiPrintHelper.getInstance().lineWrap(1)
                PrintSunmiUtils.orderNoteInner(receiptModel.note)
            }
            SunmiPrintHelper.getInstance().lineWrap(2)


            if (customerSettingModel.fonts == Constants.LARGE) {
                PrintSunmiUtils.boldText("Customer Signature ____")
            } else {
                PrintSunmiUtils.boldText("Customer Signature           __________________")
            }

            SunmiPrintHelper.getInstance().lineWrap(2)
            if (customerSettingModel.showQrCode) {

                PrintSunmiUtils.qrCodeInner(receiptModel.digitalReceiptUrl)
            }

            PrintSunmiUtils.cutPaperInner()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun observeTipsList() {
        viewModel.getTipsList().observe(viewLifecycleOwner) {
            if (it.isNotEmpty()) {
                tipsList = it
            }
        }
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

        LogUtil.logE(TAG, "getDimen:  ${dimen}")
        return net.glxn.qrgen.android.QRCode.from(qrcodeStaticUrl).bitmap()


    }

    fun randomOfflineId(): String {

        val locationId = prefProvider.getValueInt(Constants.LOCATION_ID, -1).toString()
        val timestamp = System.currentTimeMillis().toString()
        val ss = locationId + timestamp.takeLast(4)
        val reqLent = 12 - ss.length
        val Alphabet = getSaltString(reqLent)
        val timeStampFinal = Alphabet + ss
        LogUtil.logE("timeStampFinal", timeStampFinal)

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

    fun timeCalculateForStartEndTime(hour: Int, minute: Int, isStart: String): String {
        var timestring = ""
        var hoursfinal: Int = 0
        if ((hour == 12 && minute > 0) || (hour > 12 && minute > 0) || (hour > 12 && minute == 0)) {
            if (hour == 12) {
                hoursfinal = hour
            } else {
                hoursfinal = hour - 12
            }
            if (hoursfinal < 10) {
                if (minute < 10) {
                    timestring = "0$hoursfinal:0$minute PM"
                } else {
                    timestring = "0$hoursfinal:$minute PM"
                }
            } else {
                if (minute < 10) {
                    timestring = "$hoursfinal:0$minute PM"
                } else {
                    timestring = "$hoursfinal:$minute PM"
                }
            }
        } else {
            if (hour == 0) {
                if (minute < 10) {
                    timestring = "${hour.plus(12)}:0$minute AM"
                } else {
                    timestring = "${hour.plus(12)}:$minute AM"
                }
            } else {
                if (hour < 10) {
                    if (minute < 10) {
                        timestring = "0$hour:0$minute AM"
                    } else {
                        timestring = "0$hour:$minute AM"
                    }
                } else {
                    if (minute < 10) {
                        timestring = "$hour:0$minute AM"
                    } else {
                        timestring = "$hour:$minute AM"
                    }
                }
            }

        }

        val myFormat = "MM/dd/yyyy" //In which you need put here
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        var startDatestring = ""
        if (isStart == "isstart") {
            startDatestring = sdf.format(myCalendar.time)
        } else {
            startDatestring = sdf.format(myCalendar1.time)
        }
        return "$startDatestring $timestring"
    }

    private fun differnceTrue(date1: String, date2: String?): Long {
        var dateType1: Date
        var dateType2: Date
        var daydifference = "0".toLong()
//        11/30/2021 09:40 AM
        try {
            var dates = SimpleDateFormat("MM/dd/yyyy")
            dateType1 = dates.parse(date1.substringBefore(" "))
            dateType2 = dates.parse(date2?.substringBefore(" "))
            var differencedate = abs(dateType1.time - dateType2.time)
            daydifference = differencedate / (24 * 60 * 60 * 1000)
            Log.d("yash", "differnceTrue: " + daydifference)
            return daydifference
        } catch (e: Exception) {
        }
        return daydifference
    }

    private fun startDatePickerObserver() {
        viewModel.startDateSelection.observe(requireActivity()) { event ->
            event.getContentIfNotHandled()?.let {
                //currentPage = 1
                val dialog = DatePickerDialog(
                    requireActivity(),
                    android.R.style.Theme_Material_Light_Dialog,
                    startDate,
                    myCalendar
                        .get(Calendar.YEAR),
                    myCalendar.get(Calendar.MONTH),
                    myCalendar.get(Calendar.DAY_OF_MONTH)

                )
                dialog.datePicker.maxDate = Date().time
                dialog.show()
            }

        }
    }

    private fun endDatePickerObserver() {
        viewModel.endDateSelection.observe(requireActivity()) { event ->
            event.getContentIfNotHandled()?.let {
                //currentPage = 1
                val dialog = DatePickerDialog(
                    requireActivity(),
                    android.R.style.Theme_Material_Light_Dialog,
                    endDate,
                    myCalendar1
                        .get(Calendar.YEAR),
                    myCalendar1.get(Calendar.MONTH),
                    myCalendar1.get(Calendar.DAY_OF_MONTH)

                )
                dialog.datePicker.maxDate = Date().time
                dialog.show()
            }
        }
    }

    private fun searchFilter() {

        binding.autoSearch.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.toString() == " ") {
                    binding.autoSearch.setText("")
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun afterTextChanged(s: Editable) {

                adapter.filter.filter(s.toString().trim())

            }
        })
    }

    private fun printBusinessLogo() {
        val decodedString: ByteArray = Base64.decode(
            prefProvider.getValue(Constants.VENUE_LOGO, ""),
            Base64.DEFAULT
        )
        val bitmap: Bitmap =
            BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

        val newBitmap = Bitmap.createScaledBitmap(bitmap!!, 210, 210, true)

        PrintSunmiUtils.printLogo(newBitmap)

    }

}