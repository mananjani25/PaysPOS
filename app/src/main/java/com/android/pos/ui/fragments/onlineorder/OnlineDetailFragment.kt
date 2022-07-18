package com.android.pos.ui.fragments.onlineorder

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.text.trimmedLength
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.pos.R
import com.android.pos.data.model.requestModel.RefundRequestModelOnlineOrder
import com.android.pos.data.model.responseModel.GetKitchenReceiptSettingsResponse
import com.android.pos.data.model.responseModel.OnlineOrderStatusUpdateResponse
import com.android.pos.data.model.responseModel.PrinterResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.EMPLOYEE_NAME
import com.android.pos.databinding.OnlineDetailFragmentBinding
import com.android.pos.di.PrefProvider
import com.android.pos.di.RolePermission
import com.android.pos.ui.adapter.OnlineOrderAdapter
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
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class OnlineDetailFragment(
    var param1: String,
    var startDateTime: String?,
    var endDateTime: String?
) : Fragment(),
    OrderCallBack {


    private val viewModel by viewModels<OnlineDetailViewModel>()
    lateinit var binding: OnlineDetailFragmentBinding
    private lateinit var refundData: RefundRequestModelOnlineOrder
    private lateinit var startDate: DatePickerDialog.OnDateSetListener
    private lateinit var endDate: DatePickerDialog.OnDateSetListener
    private lateinit var startTime: TimePickerDialog.OnTimeSetListener
    private lateinit var endTime: TimePickerDialog.OnTimeSetListener
    private lateinit var adapter: OnlineOrderAdapter
    val myCalendar = Calendar.getInstance()
    val myCalendar1 = Calendar.getInstance()
    val myCalendar2 = Calendar.getInstance()
    val myCalendar3 = Calendar.getInstance()
    var order_status = "Pending"
    private val TAG = "OnlineDetailFragment"
    private var kitchenSettingModel = GetKitchenReceiptSettingsResponse.Data()


    @Inject
    lateinit var prefProvider: PrefProvider

    @Inject
    lateinit var rolePermission: RolePermission
    var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var refresh = intent?.getBooleanExtra("refresh", false)
            if (refresh == true) {
                adapter.orderList.clear()
                adapter.filterList.clear()
                getOnlineOrders()
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        requireActivity().unregisterReceiver(broadcastReceiver)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapter()
        observeShowProgress()
        requireActivity().registerReceiver(
            broadcastReceiver,
            IntentFilter(Constants.ONLINE_ORDER_REFRESH)
        )
        when (param1) {
            "0" -> {
                order_status = "Pending"
                binding.txtOrderWillAppear?.text = "Pending order will appear here."
            }
            "1" -> {
                order_status = "InProgress"
                binding.txtOrderWillAppear?.text = "InProgress order will appear here."
            }
            "2" -> {
                order_status = "Completed"
                binding.txtOrderWillAppear?.text = "Completed order will appear here."
            }
            "3" -> {
                order_status = "Rejected"
                binding.txtOrderWillAppear?.text = "Rejected order will appear here."
            }

        }
        searchFilter()
        getOnlineOrders()

        requireActivity().supportFragmentManager.setFragmentResultListener(
            "request_key_time",
            viewLifecycleOwner
        ) { requestKey: String, bundle: Bundle ->
            var time = bundle.getInt("time")
            var order_id = bundle.getInt("order_id")
            acceptedAndDeclineOrder(time, order_id, true)
        }

        requireActivity().supportFragmentManager.setFragmentResultListener(
            "request_for_rejectOrder",
            viewLifecycleOwner
        ) { requestKey: String, bundle: Bundle ->
            var order_id = bundle.getInt("order_id")
            acceptedAndDeclineOrder(0, order_id, false)
        }
    }

    private fun acceptedAndDeclineOrder(time: Int, orderId: Int, is_accepted: Boolean) {
        var employee_id = prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
        var terminal_id = prefProvider.getValueInt(Constants.TERMINAL_ID, 0)
        viewModel.acceptedAndDeclineOrder(
            time,
            orderId,
            is_accepted,
            employee_id,
            terminal_id
        ).observe(viewLifecycleOwner) { it ->

            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        resource.data?.let {
                            Log.e(TAG, "getREsponseForOnline  ${Gson().toJson(it)}")
                            getKitchenPrinters(it)
                            getOnlineOrders()
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

    private fun updateOrder(orderId: Int, order_status: String) {
        viewModel.updateOnlineOrder(
            orderId,
            order_status
        ).observe(viewLifecycleOwner) { it ->

            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        resource.data?.let {
                            getOnlineOrders()
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

    private fun getOnlineOrders() {
        viewModel.onlineOrders(
            viewModel.startDate.value.toString(),
            viewModel.endDate.value.toString(),
            order_status
        ).observe(viewLifecycleOwner) { it ->

            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        resource.data?.let {

                            if (it.data.isNotEmpty()) {
                                binding.rvOpenOrder.visibility = View.VISIBLE
                                binding.llNoData.visibility = View.GONE
                                val data = it.data

                                adapter.add(data)
                                Log.e("DATA", data.size.toString())

                            } else {
                                binding.llNoData.visibility = View.VISIBLE
                                binding.txtNodata.text = it.message
                                binding.rvOpenOrder.visibility = View.GONE

                            }
                            val intent = Intent()
                            intent.action = "onlineOrder"
                            intent.putExtra("isCount", true)
                            intent.putExtra("param1", param1)
                            intent.putExtra("count", it.data.size)
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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setCurrentDate(myCalendar, startDateTime, endDateTime)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = OnlineDetailFragmentBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        getKitchenReceiptSettings()
        startDatePickerObserver()
        endDatePickerObserver()

        startTime = TimePickerDialog.OnTimeSetListener { view, hour, minute ->
            val timecalender = Calendar.getInstance()
            timecalender.set(Calendar.HOUR_OF_DAY, hour)
            timecalender.set(Calendar.MINUTE, minute)
            viewModel.startDate.value = timeCalculateForStartEndTime(hour, minute, "isstart")
            if (differnceTrue(viewModel.startDate.value!!, viewModel.endDate.value) <= 30) {
                /*checkFilter = true
                currentPage = 1
                apiCallTimeSheet()*/
                getOnlineOrders()
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
                getOnlineOrders()
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

    }

    private fun startDatePickerObserver() {
        viewModel.startDateSelection.observe(requireActivity()) { event ->
            event.getContentIfNotHandled()?.let {
                //currentPage = 1
                DatePickerDialog(
                    requireActivity(),
                    android.R.style.Theme_Material_Light_Dialog,
                    startDate,
                    myCalendar
                        .get(Calendar.YEAR),
                    myCalendar.get(Calendar.MONTH),
                    myCalendar.get(Calendar.DAY_OF_MONTH)

                ).show()
            }

        }
    }

    private fun endDatePickerObserver() {
        viewModel.endDateSelection.observe(requireActivity()) { event ->
            event.getContentIfNotHandled()?.let {
                //currentPage = 1
                DatePickerDialog(
                    requireActivity(),
                    android.R.style.Theme_Material_Light_Dialog,
                    endDate,
                    myCalendar1
                        .get(Calendar.YEAR),
                    myCalendar1.get(Calendar.MONTH),
                    myCalendar1.get(Calendar.DAY_OF_MONTH)

                ).show()
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

        adapter = OnlineOrderAdapter(requireContext())
        adapter.setCallback(this)
        binding.rvOpenOrder?.adapter = adapter
    }

    private fun searchFilter() {

        binding.autoSearch?.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun afterTextChanged(s: Editable) {

                adapter.filter.filter(s.toString().trim())

            }
        })
    }

    override fun onItemClickListener(view: View?, pos: Int, status: String) {
        if (status == "accepted") {
            if (findNavController().currentDestination?.id == R.id.onlineOrderFragment) {
                findNavController().navigate(
                    R.id.action_onlineOrder_to_addOnlneTime,
                    bundleOf(
                        "order_id" to adapter.filterList[pos].id
                    )
                )
            }
        } else if (status == "Completed") {
            alert("", "Are you sure you want to Complete this Order?") {
                this.positiveButton("YES") {
                    updateOrder(adapter.filterList[0].id, status)
                }
                this.negativeButton("NO") {
                }

            }
        } else {
            alert("", "Are you sure you want to Reject this Order?") {

                this.positiveButton("YES") {
                    var employeeIdtemp = prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
                    var terminal_id = prefProvider.getValueInt(Constants.TERMINAL_ID, 0)
                    var orderItemRefundsAttributesList =
                        ArrayList<RefundRequestModelOnlineOrder.PaymentRefund.OrderItemRefundsAttribute>()
                    adapter.orderList[pos].orderItems.forEach { item ->
                        val orderItemRefundsAttributeModel =
                            RefundRequestModelOnlineOrder.PaymentRefund.OrderItemRefundsAttribute()

                        orderItemRefundsAttributeModel.amount = item.totalPrice
                        orderItemRefundsAttributeModel.employeeId = employeeIdtemp
                        orderItemRefundsAttributeModel.orderId = item.orderId
                        orderItemRefundsAttributeModel.refundType = 0
                        orderItemRefundsAttributeModel.paymentId =
                            adapter.orderList[pos].payments[0].id
                        orderItemRefundsAttributeModel.orderItemId = item.id
                        orderItemRefundsAttributeModel.quantity = item.quantity
                        orderItemRefundsAttributesList.add(orderItemRefundsAttributeModel)
                    }

                    refundData = RefundRequestModelOnlineOrder().apply {
                        paymentRefund = RefundRequestModelOnlineOrder.PaymentRefund().apply {
                            amount =
                                adapter.orderList[pos].payments[0].amount + adapter.orderList[pos].payments[0].tips
                            orderId = adapter.orderList[pos].id
                            paymentId = adapter.orderList[pos].payments[0].id
                            employeeId = employeeIdtemp
                            taxRefunded = adapter.orderList[pos].payments[0].taxAmount
                            tipsRefunded = adapter.orderList[pos].payments[0].tips
                            terminalId = terminal_id
                            serviceChargeRefunded =
                                adapter.orderList[pos].payments[0].serviceChargeAmount
                            cash_discount_or_surcharge_refunded =
                                adapter.orderList[pos].payments[0].cashDiscount
                            subtotal_refunded = adapter.orderList[pos].payments[0].subTotal
                            orderItemRefundsAttributes = orderItemRefundsAttributesList
                        }
                    }
                    val bundle = Bundle().apply {
                        putParcelable("refundData", refundData)
                        putDouble(
                            "refundAmount",
                            adapter.orderList[pos].payments[0].amount + adapter.orderList[pos].payments[0].tips
                        )
                        putString("paymentType", adapter.orderList[pos].payments[0].paymentType)
                        putString(
                            "magensa_response_data",
                            adapter.orderList[pos].magensa_response_data
                        )
                    }
                    findNavController().navigate(
                        R.id.action_onlineOrder_to_reasonForrefundonline,
                        bundle
                    )

                }
                this.negativeButton("NO") {
                }

            }
        }
    }

    private fun getKitchenReceiptSettings() {
        viewModel.getKitchenReceiptSettings().observe(viewLifecycleOwner, {

            if (it != null) {
                kitchenSettingModel = it

            }
        })
    }

    private fun getKitchenPrinters(data: OnlineOrderStatusUpdateResponse) {
        viewModel.getKitchenPrinterList().observe(viewLifecycleOwner) { it ->
            when (it.status) {
                Status.SUCCESS -> {
                    it.data?.forEach {
                        if (it.status) {
                            initKitchenPrinter(
                                it,
                                Constants.KITCHEN,
                                data
                            )
                        }

                    }


                }
                Status.LOADING -> {
                    ProgressUtils.showProgressDialog(requireActivity())
                }
                Status.ERROR -> {
                    ProgressUtils.dismissProgressDialog()
                    findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_orders)

                }
            }

        }

    }

    private fun initKitchenPrinter(
        data: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        orderData: OnlineOrderStatusUpdateResponse
    ) {

        PrinterClass.closePrinter()
        if (PrinterClass.getPrinter() == null) {
            //  printerDialog.show(requireContext())

            var printer: Print? = Print(requireContext())


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
                //  printerDialog.dismiss()
                Log.e(TAG, "PrinterException: " + e.message)
                printer = null
                return
            }

            if (printer != null) {
                PrinterClass.setPrinter(printer)


                generateKitchenReceipt(data, type, orderData)

            }

        } else {
            Log.e(TAG, "PrinterIsNotNull:")
        }

    }

    private fun generateKitchenReceipt(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        orderData: OnlineOrderStatusUpdateResponse
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

            builder = Builder(pname, PrinterClass.language, requireActivity())

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

                addBuilderText(builder, "Online Order")
            }
            var tmps = "Open Order".toString().trim()
                .toString().lowercase()
            Log.e(TAG, "LowerCAse ${tmps.trimmedLength()}")




            builder.addFeedLine(2)
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
                    "OrderID:" + orderData?.data.id,
                    "",
                    33
                )
            )

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
                    "ReceiptID:" + orderData.data.offlineId,
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
                builder.addTextSize(fontSizeH, fontSizeW)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                builder.addText(
                    padLine(
                        "Employee:" + prefProvider.getValue(EMPLOYEE_NAME, ""), "",
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
                        requireContext(),
                        orderData.data.createdAt
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


            addOrdersForKitchenOnlineOrder(
                builder,
                orderData.data.orderItems,
                fontSizeH,
                fontSizeW
            )


            if (orderData?.data?.note.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
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


                builder.addText(orderData?.data?.note.toString())
            }


            if (kitchenSettingModel.showCustomerAddress != false or kitchenSettingModel.showCustomerPhone != false or kitchenSettingModel.showCustomerName != false) {
                if (orderData?.data?.customer != null) {

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
                        builder.addText(orderData?.data?.customer?.firstName + " " + orderData?.data?.customer?.lastName)

                    }


                    if (kitchenSettingModel.showCustomerPhone) {

                        if (orderData?.data?.customer?.phones?.isNotEmpty() == true) {
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
                            builder.addText(orderData?.data?.customer?.phones?.get(orderData?.data?.customer?.phones.size - 1)?.phoneNumber)
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


                    if (orderData?.data?.customer?.addresses?.isNotEmpty() == true) {

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

                        builder.addText(orderData?.data?.customer?.addresses.get(orderData?.data?.customer?.addresses.size - 1).fullAddress)
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


        try {
            PrinterClass.getPrinter()?.sendData(
                builder,
                PrinterClass.SEND_TIMEOUT, status, battery
            )

            //printerDialog.dismiss()
            PrinterClass.closePrinter()

            //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
        } catch (e: Exception) {
//                printerDialog.dismiss()
            PrinterClass.closePrinter()
            e.printStackTrace()
            Log.e(TAG, "PrinterError: " + e.localizedMessage)
        }


    }


}
