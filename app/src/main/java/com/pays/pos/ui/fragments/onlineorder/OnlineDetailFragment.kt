package com.pays.pos.ui.fragments.onlineorder

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.pays.pos.R
import com.pays.pos.data.model.requestModel.RefundRequestModelOnlineOrder
import com.pays.pos.data.model.responseModel.GetKitchenReceiptSettingsResponse
import com.pays.pos.data.model.responseModel.OnlineOrderStatusUpdateResponse
import com.pays.pos.data.model.responseModel.PrinterResponse
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.EMPLOYEE_NAME
import com.pays.pos.data.remote.Constants.ORDER_NUMBER_STARTING_FROM_ONE
import com.pays.pos.data.remote.Constants.SUNMI_PRINTER
import com.pays.pos.databinding.OnlineDetailFragmentBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.di.RolePermission
import com.pays.pos.ui.adapter.OnlineOrderAdapter
import com.pays.pos.ui.fragments.settings.hardware.printer.BluetoothUtil
import com.pays.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.PrintSunmiUtils
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.addBuilderText
import com.pays.pos.utils.addBuilderTextForU220
import com.pays.pos.utils.addHorizontalKitchenLine
import com.pays.pos.utils.addHorizontalKitchenLineForU220
import com.pays.pos.utils.addHorizontalLine
import com.pays.pos.utils.addOrdersForKitchenOnlineOrder
import com.pays.pos.utils.addOrdersForKitchenOnlineOrderSunmi
import com.pays.pos.utils.addOrdersForKitchenOnlineOrderSunmiInner
import com.pays.pos.utils.addOrdersForKitchenOnlineOrderU220
import com.pays.pos.utils.callback.OrderCallBack
import com.pays.pos.utils.checkItemsforPrinterOnlineOrder
import com.pays.pos.utils.extensions.alert
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.runOnUiThread
import com.pays.pos.utils.extensions.showAlert
import com.pays.pos.utils.extensions.visible
import com.pays.pos.utils.padLine
import com.pays.pos.utils.printer.PrinterClass
import com.pays.pos.utils.statusUtils.Status
import com.epson.epos2.printer.Printer
import com.epson.eposprint.Builder
import com.epson.eposprint.Print
import com.epson.eposprint.StatusChangeEventListener
import com.google.gson.Gson
import com.sunmi.externalprinterlibrary.api.ConnectCallback
import com.sunmi.externalprinterlibrary.api.SunmiPrinter
import com.sunmi.externalprinterlibrary.api.SunmiPrinterApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Random
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class OnlineDetailFragment(
    var param1: String,
    var startDateTime: String?,
    var endDateTime: String?
) : Fragment(),
    OrderCallBack, StatusChangeEventListener {


    private val viewModel by viewModels<OnlineDetailViewModel>()
    lateinit var binding: OnlineDetailFragmentBinding
    private lateinit var refundData: RefundRequestModelOnlineOrder
    private lateinit var startDate: DatePickerDialog.OnDateSetListener
    private lateinit var endDate: DatePickerDialog.OnDateSetListener
    private lateinit var startTime: TimePickerDialog.OnTimeSetListener
    private lateinit var endTime: TimePickerDialog.OnTimeSetListener
    private lateinit var adapter: OnlineOrderAdapter
    var myCalendar = Calendar.getInstance()
    var myCalendar1 = Calendar.getInstance()
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

            "4" -> {
                order_status = "UpComing"
                binding.txtOrderWillAppear?.text = "UpComing order will appear here."
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
                        getOnlineOrders()
                        resource.data?.let {
                            LogUtil.logE(TAG, "getREsponseForOnline  ${Gson().toJson(it)}")
                            if (it.data.orderItems.isNotEmpty()) {
                                getKitchenPrinters(it)
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
                                LogUtil.logE("DATA", data.size.toString())

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
        if (param1 == "4" || (endDateTime != null && SimpleDateFormat(
                "MM/dd/yyyy", Locale.getDefault()
            ).parse(endDateTime).after(Calendar.getInstance().time))
        ) {
            viewModel.setCurrentDate(Calendar.getInstance(), "", "", param1)
        } else {
            viewModel.setCurrentDate(
                Calendar.getInstance(), startDateTime, endDateTime, param1
            )
        }
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

    }

    private fun startDatePickerObserver() {
        viewModel.startDateSelection.observe(requireActivity()) { event ->
            event.getContentIfNotHandled()?.let {
                //currentPage = 1
//                myCalendar = Calendar.getInstance()
//                myCalendar.add(Calendar.DATE, 0)
                Log.d(TAG, "startDatePickerObserver: " + myCalendar.get(Calendar.DAY_OF_MONTH))
                Log.d(TAG, "startDatePickerObserver: " + myCalendar.get(Calendar.MONTH))
                Log.d(
                    TAG, "startDatePickerObserver: " + myCalendar
                        .get(Calendar.YEAR)
                )
                Log.d(TAG, "startDatePickerObserver: " + myCalendar.time)
                var datePickerDialog: DatePickerDialog = DatePickerDialog(
                    requireActivity(),
                    android.R.style.Theme_Material_Light_Dialog,
                    startDate,
                    myCalendar
                        .get(Calendar.YEAR),
                    myCalendar.get(Calendar.MONTH),
                    myCalendar.get(Calendar.DAY_OF_MONTH)

                )
                datePickerDialog.show()
                if (param1 == "4") {
                    datePickerDialog.datePicker.minDate = myCalendar.timeInMillis
                    var temp_calender = Calendar.getInstance()
                    temp_calender.add(Calendar.DATE, 7)
                    datePickerDialog.datePicker.maxDate = temp_calender.timeInMillis
                } else {
                    datePickerDialog.datePicker.maxDate = Date().time
                }


            }

        }
    }

    private fun endDatePickerObserver() {
        viewModel.endDateSelection.observe(requireActivity()) { event ->
            event.getContentIfNotHandled()?.let {
                if (param1 == "4") {
                    myCalendar1 = Calendar.getInstance()
                    myCalendar1.add(Calendar.DATE, 7)
                }
                var datePickerDialog: DatePickerDialog = DatePickerDialog(
                    requireActivity(),
                    android.R.style.Theme_Material_Light_Dialog,
                    endDate,
                    myCalendar1
                        .get(Calendar.YEAR),
                    myCalendar1.get(Calendar.MONTH),
                    myCalendar1.get(Calendar.DAY_OF_MONTH)

                )
                if (param1 == "4") {
                    datePickerDialog.datePicker.minDate = myCalendar1.timeInMillis
                } else {
                    datePickerDialog.datePicker.maxDate = Date().time
                }
                datePickerDialog.show()
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

        adapter = OnlineOrderAdapter(requireContext(), prefProvider)
        adapter.setCallback(this)
        binding.rvOpenOrder?.adapter = adapter
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
            alert("", "Are you sure, you want to complete this order ?") {
                this.positiveButton("YES") {
                    updateOrder(adapter.filterList[0].id, status)
                }
                this.negativeButton("NO") {
                }

            }
        } else {
            alert("", "Are you sure, you want to reject this order ?") {

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
                    bundle.putString("isFrom", "rejectOnlineOrder")
                    if (prefProvider.isAdmin() || prefProvider.isManager()) {
                        if (findNavController().currentDestination?.id == R.id.onlineOrderFragment) {
                            findNavController().navigate(
                                R.id.action_onlineOrder_to_reasonForrefundonline,
                                bundle
                            )
                        }
                    } else {
                        if (findNavController().currentDestination?.id == R.id.onlineOrderFragment) {
                            findNavController().navigate(
                                R.id.action_onlineOrder_to_passcodeManager,
                                bundle
                            )
                        }
                    }


                }
                this.negativeButton("NO") {
                }

            }
        }
    }

    override fun noDataAvailableFilter() {

        runOnUiThread(Runnable {
            binding.llNoData.visible()
            binding.txtNodata.text = requireContext().getText(R.string.no_data_available)
        })

        Log.d("noDataAvailableFilter","no data available")
    }

    override fun hideNoDataAvailable() {
        binding.llNoData.gone()
        Log.d("noDataAvailableFilter","hide")
    }

    private fun getKitchenReceiptSettings() {
        viewModel.getKitchenReceiptSettings().observe(viewLifecycleOwner) {

            if (it != null) {
                kitchenSettingModel = it
            }
        }
    }

    private fun getKitchenPrinters(data: OnlineOrderStatusUpdateResponse) {
        viewModel.getKitchenPrinterList().observe(viewLifecycleOwner) { it ->
            when (it.status) {
                Status.SUCCESS -> {
                    it.data?.forEach {
                        if (it.status && checkItemsforPrinterOnlineOrder(
                                data.data.orderItems, it.printerCategories.toCollection(
                                    arrayListOf()
                                )
                            )
                        ) {

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
                    //findNavController().navigate(R.id.action_dashboardCategoryBoldPOS_to_orders)

                }
            }

        }

    }

    private fun initKitchenPrinter(
        data: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        orderData: OnlineOrderStatusUpdateResponse
    ) {
        if (data.name.startsWith(SUNMI_PRINTER, true)) {

            SunmiPrinterApi.getInstance()
                .setPrinter(SunmiPrinter.SunmiBlueToothPrinter, data.ipAddress)

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

                            generateKitchenReceiptSunmi(data, type, orderData)
                        }

                        override fun onDisconnect() {
                            println("onDisconnect")
                        }

                    })
            } else {
                generateKitchenReceiptSunmi(data, type, orderData)
            }

        } else if (data.name.startsWith(Constants.SUNMI_INNER_PRINTER, true)) {

            _root_ide_package_.com.pays.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper.getInstance().initSunmiPrinterService(requireContext())
            viewLifecycleOwner.lifecycleScope.launch {
                delay(100)
                setService(data, type, orderData)
            }
        } else {

            if (!data.name.substring(0, 6).toString().lowercase().contains("TM-m".lowercase())) {
                var mPrinter = if (data.name.substring(0, 6).toString().lowercase()
                        .contains("TM-m".lowercase())
                ) {
                    Log.e(TAG, "YesContains")
                    Printer(
                        Printer.TM_M30,
                        Printer.MODEL_ANK, requireContext()
                    )
                } else {
                    Printer(
                        Printer.TM_U220,
                        Printer.MODEL_ANK, requireContext()
                    )


                }

                mPrinter.setReceiveEventListener { printer, i, printerStatusInfo, s ->

                    Log.e(
                        TAG,
                        "PrinterEvent  ${Gson().toJson(printerStatusInfo)} other1 ${s}  other2 ${i}"
                    )
                    if (printerStatusInfo.online == 1) {
                        try {
                            printer.disconnect()

                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                try {
                    Log.e(TAG, "printerDataType:  ${data.printer_type}")

                    var printerAdd =
                        if (data.printer_type == Constants.BLUETOOTH) "BT:" + data.macAddress else "TCP:" + data.ipAddress
                    mPrinter.connect(
                        printerAdd,
                        Printer.PARAM_DEFAULT
                    )
                    mPrinter.startMonitor()

                    generateKitchenReceiptU220(data, type, orderData, mPrinter)

                    // generateReceiptForU220(mPrinter, data, type)

                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            } else {
                PrinterClass.closePrinter()
                if (PrinterClass.getPrinter() == null) {
                    //  printerDialog.show(requireContext())

                    var printer: Print? = Print(requireContext())
                    /*if (printer != null) {
                   printer.setStatusChangeEventCallback(this)
                   printer.setBatteryStatusChangeEventCallback(this)
               }*/


                    val enabled = Print.FALSE

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
                        printer?.setStatusChangeEventCallback(this)

                    } catch (e: Exception) {
                        //  printerDialog.dismiss()
                        LogUtil.logE(TAG, "PrinterException: " + e.message)
                        printer = null
                        return
                    }

                    if (printer != null) {
                        PrinterClass.setPrinter(printer)


                        generateKitchenReceipt(data, type, orderData)

                    }

                } else {
                    LogUtil.logE(TAG, "PrinterIsNotNull:")
                }

            }

            /*  PrinterClass.closePrinter()
              if (PrinterClass.getPrinter() == null) {
                  //  printerDialog.show(requireContext())

                  var printer: Print? = Print(requireContext())


                  val enabled = Print.FALSE

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
                      LogUtil.logE(TAG, "PrinterException: " + e.message)
                      printer = null
                      return
                  }

                  if (printer != null) {
                      PrinterClass.setPrinter(printer)


                      generateKitchenReceipt(data, type, orderData)

                  }

              } else {
                  LogUtil.logE(TAG, "PrinterIsNotNull:")
              }*/
        }

    }

    private fun generateKitchenReceiptU220(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        orderData: OnlineOrderStatusUpdateResponse,
        builder: Printer
    ) {

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


            builder.addFeedUnit(30)
            builder.addFeedLine(2)
            if (customerReceiptPrinters.name.substring(0, 4)
                    .equals("TM-U", true) || customerReceiptPrinters.name.contains("U")
            ) {
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
                    builder.addText("OrderID:" + orderData?.data.custom_order_id)
                } else {
                    builder.addText("OrderID:" + orderData?.data.id)

                }
                builder.addFeedUnit(30)
                builder.addFeedLine(1)

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

                    addBuilderTextForU220(builder, orderData.data.orderTypeName)
                }

                if (orderData.data.orderType.equals(Constants.PHONE_ORDER, true) ||
                    orderData.data.orderType.equals("OnlineWebOrder", true) ||
                    orderData.data.orderType.equals("Online Order", true) ||
                    orderData.data.orderType.equals("OnlineOrder", true)
                ) {


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

                    addBuilderTextForU220(builder, orderData.data.deliveryType)
                }

                if (kitchenSettingModel.showTeamMember) {

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

                addHorizontalKitchenLineForU220(builder)


                addOrdersForKitchenOnlineOrderU220(
                    builder,
                    orderData.data.orderItems,
                    fontSizeH,
                    fontSizeW,
                    customerReceiptPrinters.printerCategories.toCollection(arrayListOf())
                )


                if (orderData?.data?.note.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
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


                if (kitchenSettingModel.showCustomerAddress != false || kitchenSettingModel.showCustomerPhone != false || kitchenSettingModel.showCustomerName != false) {
                    if (orderData?.data?.customer != null) {

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
                        addHorizontalKitchenLineForU220(builder)

                        if (kitchenSettingModel.showCustomerName) {

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
                                builder.addText(
                                    MethodUtils.getUSFormatNumber(
                                        orderData?.data?.customer?.phones?.get(
                                            orderData?.data?.customer?.phones.size - 1
                                        )?.phoneNumber
                                    )
                                )
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

                            orderData?.data?.customer?.addresses.filter {
                                it.typeOfAddress == Constants.BILLING_ADDRESS
                            }

                                .forEach {

                                    if (it.typeOfAddress.equals(
                                            Constants.BILLING_ADDRESS,
                                            ignoreCase = true
                                        )
                                    ) {
                                        builder.addText(
                                            it.fullAddress
                                        )
                                    }
                                }
//                            builder.addText(orderData?.data?.customer?.addresses.get(orderData?.data?.customer?.addresses.size - 1).fullAddress)
                        }
                    }


                }
            } else {


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
                    builder.addText("OrderID:" + orderData?.data.custom_order_id)
                } else {
                    builder.addText("OrderID:" + orderData?.data.id)
                }
                builder.addFeedUnit(30)
                builder.addFeedLine(1)

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

                    addBuilderTextForU220(builder, "Online Order")
                }



                if (kitchenSettingModel.showTeamMember) {

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
                            if (kitchenSettingModel.fonts == Constants.LARGE) {
                                24
                            } else {
                                48
                            }
                        )
                    )

                }
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
                        if (kitchenSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )


                builder.addFeedLine(1)

                addHorizontalKitchenLineForU220(builder)


                addOrdersForKitchenOnlineOrderU220(
                    builder,
                    orderData.data.orderItems,
                    fontSizeH,
                    fontSizeW,
                    customerReceiptPrinters.printerCategories.toCollection(arrayListOf())
                )


                if (orderData?.data?.note.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
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


                if (kitchenSettingModel.showCustomerAddress != false || kitchenSettingModel.showCustomerPhone != false || kitchenSettingModel.showCustomerName != false) {
                    if (orderData?.data?.customer != null) {

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

                        builder.addFeedLine(1)
                        addHorizontalKitchenLineForU220(builder)

                        if (kitchenSettingModel.showCustomerName) {

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
                                builder.addText(
                                    MethodUtils.getUSFormatNumber(
                                        orderData?.data?.customer?.phones?.get(
                                            orderData?.data?.customer?.phones.size - 1
                                        )?.phoneNumber
                                    )
                                )
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

                            orderData?.data?.customer?.addresses.filter { it.typeOfAddress == Constants.BILLING_ADDRESS }
                                .forEach {

                                    if (it.typeOfAddress.equals(
                                            Constants.BILLING_ADDRESS,
                                            ignoreCase = true
                                        )
                                    ) {
                                        builder.addText(
                                            it.fullAddress
                                        )
                                    }
                                }
//                            builder.addText(orderData?.data?.customer?.addresses.get(orderData?.data?.customer?.addresses.size - 1).fullAddress)
                        }
                    }


                }
            }
        } catch (e: Exception) {
            // printerDialog.dismiss()
            e.printStackTrace()
        }

        builder?.addFeedLine(2)

        builder?.addCut(Builder.CUT_FEED)

        try {
            builder.sendData(Printer.PARAM_DEFAULT)


        } catch (e: java.lang.Exception) {

            e.printStackTrace()
        }

        /*val status = IntArray(1)
        val battery = IntArray(1)

        var timeOut = PrinterClass.SEND_TIMEOUT
        if (customerReceiptPrinters.printer_type == Constants.BLUETOOTH) {
            timeOut = PrinterClass.BLUETOOTH_TIMEOUT
        }

        if (customerReceiptPrinters.name.substring(0, 6).toString()
                .lowercase() == "TM-m30".lowercase() && customerReceiptPrinters.printer_type != Constants.BLUETOOTH
        ) {

            timeOut = 1000
        }


*/
        /*  try {
              PrinterClass.getPrinter()?.sendData(
                  builder,
                  timeOut, status, battery
              )

              //printerDialog.dismiss()
              PrinterClass.closePrinter()

              //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
          } catch (e: Exception) {
  //                printerDialog.dismiss()
              PrinterClass.closePrinter()
              e.printStackTrace()
              LogUtil.logE(TAG, "PrinterError: " + e.localizedMessage)
          }
  */
    }

    private fun generateKitchenReceipt(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        orderData: OnlineOrderStatusUpdateResponse,
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

            builder = Builder(pname, PrinterClass.language, requireActivity())
            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)
            builder.addFeedLine(2)
            if (customerReceiptPrinters.name.substring(0, 4)
                    .equals("TM-U", true) || customerReceiptPrinters.name.contains("U")
            ) {
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
                    builder.addText("OrderID:" + orderData?.data.custom_order_id)
                } else {
                    builder.addText("OrderID:" + orderData?.data.id)

                }
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addFeedLine(1)

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
                LogUtil.logE(TAG, "LowerCAse ${tmps.trimmedLength()}")


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

                addBuilderText(builder, orderData?.data.deliveryType)



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
                    fontSizeW,
                    customerReceiptPrinters.printerCategories.toCollection(arrayListOf())
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


                if (kitchenSettingModel.showCustomerAddress != false || kitchenSettingModel.showCustomerPhone != false || kitchenSettingModel.showCustomerName != false) {
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
                                builder.addText(
                                    MethodUtils.getUSFormatNumber(
                                        orderData?.data?.customer?.phones?.get(
                                            orderData?.data?.customer?.phones.size - 1
                                        )?.phoneNumber
                                    )
                                )
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

                            orderData?.data?.customer?.addresses.filter {
                                it.typeOfAddress == Constants.BILLING_ADDRESS
                            }

                                .forEach {

                                    if (it.typeOfAddress.equals(
                                            Constants.BILLING_ADDRESS,
                                            ignoreCase = true
                                        )
                                    ) {
                                        builder.addText(
                                            it.fullAddress
                                        )
                                    }
                                }
//                            builder.addText(orderData?.data?.customer?.addresses.get(orderData?.data?.customer?.addresses.size - 1).fullAddress)
                        }
                    }


                }
            } else {


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
                    builder.addText("OrderID:" + orderData?.data.custom_order_id)
                } else {
                    builder.addText("OrderID:" + orderData?.data.id)
                }
                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addFeedLine(1)

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

                addBuilderText(builder, orderData?.data.deliveryType)

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
                            if (kitchenSettingModel.fonts == Constants.LARGE) {
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
                        if (kitchenSettingModel.fonts == Constants.LARGE) {
                            24
                        } else {
                            48
                        }
                    )
                )


                builder.addFeedLine(1)

                addHorizontalLine(builder)


                addOrdersForKitchenOnlineOrder(
                    builder,
                    orderData.data.orderItems,
                    fontSizeH,
                    fontSizeW,
                    customerReceiptPrinters.printerCategories.toCollection(arrayListOf())
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


                if (kitchenSettingModel.showCustomerAddress != false || kitchenSettingModel.showCustomerPhone != false || kitchenSettingModel.showCustomerName != false) {
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

                        // Commented below line to resolve BIS-389 issue
                        //builder.addFeedLine(1)
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
                                builder.addText(
                                    MethodUtils.getUSFormatNumber(
                                        orderData?.data?.customer?.phones?.get(
                                            orderData?.data?.customer?.phones.size - 1
                                        )?.phoneNumber
                                    )
                                )
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
                        if (kitchenSettingModel.showCustomerAddress) {

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

                                orderData?.data?.customer?.addresses.filter { it.typeOfAddress == Constants.BILLING_ADDRESS }
                                    .forEach {

                                        if (it.typeOfAddress.equals(
                                                Constants.BILLING_ADDRESS,
                                                ignoreCase = true
                                            )
                                        ) {
                                            builder.addText(
                                                it.fullAddress
                                            )
                                        }
                                    }
//                            builder.addText(orderData?.data?.customer?.addresses.get(orderData?.data?.customer?.addresses.size - 1).fullAddress)
                            }
                        }
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

        var timeOut = PrinterClass.SEND_TIMEOUT
        if (customerReceiptPrinters.printer_type == Constants.BLUETOOTH) {
            timeOut = PrinterClass.BLUETOOTH_TIMEOUT
        }

        if (customerReceiptPrinters.name.substring(0, 6).toString()
                .lowercase() == "TM-m30".lowercase() && customerReceiptPrinters.printer_type != Constants.BLUETOOTH
        ) {

            timeOut = 10000
        }



        try {
            PrinterClass.getPrinter()?.sendData(
                builder,
                timeOut, status, battery
            )

            //printerDialog.dismiss()
            PrinterClass.closePrinter()

            //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
        } catch (e: Exception) {
//                printerDialog.dismiss()
            PrinterClass.closePrinter()
            e.printStackTrace()
            LogUtil.logE(TAG, "PrinterError: " + e.localizedMessage)
        }


    }


    private fun generateKitchenReceiptSunmi(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        orderData: OnlineOrderStatusUpdateResponse
    ) {

        try {

            PrintSunmiUtils.fontSize(kitchenSettingModel.fonts)
            SunmiPrinterApi.getInstance().lineWrap(2)
            if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                PrintSunmiUtils.orderIdSunmi(
                    "OrderID:" + orderData?.data.custom_order_id
                )
            } else {
                PrintSunmiUtils.orderIdSunmi(
                    "OrderID:" + orderData?.data.id
                )
            }

            SunmiPrinterApi.getInstance().lineWrap(1)

            if (kitchenSettingModel.showOrderType) {
                PrintSunmiUtils.printOrderType(orderData.data.orderTypeName)
            }

            if (orderData.data.orderType.equals(Constants.PHONE_ORDER, true) ||
                orderData.data.orderType.equals("OnlineWebOrder", true) ||
                orderData.data.orderType.equals("Online Order", true) ||
                orderData.data.orderType.equals("OnlineOrder", true)
            ) {
                SunmiPrinterApi.getInstance().lineWrap(1)
                PrintSunmiUtils.printOrderType(orderData.data.deliveryType)
            }

            SunmiPrinterApi.getInstance().lineWrap(1)

            if (kitchenSettingModel.showTeamMember) {

                PrintSunmiUtils.employee(
                    padLine(
                        "Employee:" + prefProvider.getValue(EMPLOYEE_NAME, ""), "",
                        if (kitchenSettingModel.fonts == Constants.LARGE) {
                            23
                        } else {
                            48
                        }
                    ).toString()
                )


            }

            PrintSunmiUtils.orderTime(
                padLine(
                    Constants.getReceiptFormatDateFromUTCServer(
                        requireContext(),
                        orderData.data.createdAt
                    ),
                    "",
                    if (kitchenSettingModel.fonts == Constants.LARGE) {
                        23
                    } else {
                        48
                    }
                ).toString()
            )



            PrintSunmiUtils.addHorizontal()


            addOrdersForKitchenOnlineOrderSunmi(
                orderData.data.orderItems,
                customerReceiptPrinters.printerCategories.toCollection(arrayListOf())
            )


            if (orderData?.data?.note.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {
                SunmiPrinterApi.getInstance().lineWrap(1)
                PrintSunmiUtils.orderNote(orderData?.data?.note.toString())
            }


            if (kitchenSettingModel.showCustomerAddress != false || kitchenSettingModel.showCustomerPhone != false || kitchenSettingModel.showCustomerName != false) {
                if (orderData?.data?.customer != null) {
                    SunmiPrinterApi.getInstance().lineWrap(1)
                    PrintSunmiUtils.customerDetails()

                    if (kitchenSettingModel.showCustomerName) {
                        PrintSunmiUtils.customerName(orderData?.data?.customer?.firstName + " " + orderData?.data?.customer?.lastName)

                    }


                    if (kitchenSettingModel.showCustomerPhone) {

                        if (orderData?.data?.customer?.phones?.isNotEmpty() == true) {

                            PrintSunmiUtils.customerPhone(
                                MethodUtils.getUSFormatNumber(
                                    orderData?.data?.customer?.phones?.get(
                                        orderData?.data?.customer?.phones.size - 1
                                    )?.phoneNumber
                                )

                            )
                        }

                    }


                    if (orderData?.data?.customer?.addresses?.isNotEmpty() == true) {

                        orderData?.data?.customer?.addresses.filter { it.typeOfAddress == Constants.BILLING_ADDRESS }

                            .forEach {

                                if (it.typeOfAddress.equals(
                                        Constants.BILLING_ADDRESS,
                                        ignoreCase = true
                                    )
                                ) {
                                    PrintSunmiUtils.customerAddress(
                                        it.fullAddress
                                    )
                                }
                            }
//                        PrintSunmiUtils.customerAddress(
//                            orderData?.data?.customer?.addresses.get(
//                                orderData?.data?.customer?.addresses.size - 1
//                            ).fullAddress
//                        )
                    }
                }


            }

            PrintSunmiUtils.cutPaper()
        } catch (e: Exception) {
            // printerDialog.dismiss()
            e.printStackTrace()
        }


    }

    private fun setService(
        data: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        orderData: OnlineOrderStatusUpdateResponse
    ) {
        if (_root_ide_package_.com.pays.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper.getInstance().sunmiPrinter == _root_ide_package_.com.pays.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper.FoundSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper", "FoundSunmiPrinter")

            if (!_root_ide_package_.com.pays.pos.ui.fragments.settings.hardware.printer.BluetoothUtil.isBlueToothPrinter) {

                generateKitchenReceiptSunmiInner(data, type, orderData)

            }

        } else if (_root_ide_package_.com.pays.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper.getInstance().sunmiPrinter == _root_ide_package_.com.pays.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper.CheckSunmiPrinter) {
            Handler(Looper.getMainLooper()).postDelayed({
                setService(data, type, orderData)
            }, 2000)
            LogUtil.logE("SunmiPrintHelper", "CheckSunmiPrinter")
        } else if (_root_ide_package_.com.pays.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper.getInstance().sunmiPrinter == _root_ide_package_.com.pays.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper.LostSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper", "LostSunmiPrinter")
        } else {
            LogUtil.logE("SunmiPrintHelper", "ELSE")
        }
    }


    private fun generateKitchenReceiptSunmiInner(
        customerReceiptPrinters: PrinterResponse.Data.KitchenReceiptPrinters,
        type: String,
        orderData: OnlineOrderStatusUpdateResponse
    ) {

        try {

            _root_ide_package_.com.pays.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper.getInstance().initPrinter()
            _root_ide_package_.com.pays.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper.getInstance().lineWrap(2)
            if (prefProvider.getValueboolean(ORDER_NUMBER_STARTING_FROM_ONE, false)) {
                PrintSunmiUtils.headerText("OrderID:" + orderData?.data.custom_order_id)
            } else {
                PrintSunmiUtils.headerText("OrderID:" + orderData?.data.id)
            }
            _root_ide_package_.com.pays.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper.getInstance().lineWrap(1)


            if (kitchenSettingModel.showOrderType) {
                PrintSunmiUtils.headerText(orderData.data.orderTypeName)
            }

            if (orderData.data.orderType.equals(Constants.PHONE_ORDER, true) ||
                orderData.data.orderType.equals("OnlineWebOrder", true) ||
                orderData.data.orderType.equals("Online Order", true) ||
                orderData.data.orderType.equals("OnlineOrder", true)
            ) {
                _root_ide_package_.com.pays.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper.getInstance().lineWrap(1)
                PrintSunmiUtils.headerText(orderData.data.deliveryType)
            }

            if (kitchenSettingModel.showTeamMember) {

                PrintSunmiUtils.normalTextLarge(
                    "Employee:" + prefProvider.getValue(
                        EMPLOYEE_NAME,
                        ""
                    )
                )


            }

            PrintSunmiUtils.normalTextLarge(
                Constants.getReceiptFormatDateFromUTCServer(
                    requireContext(),
                    orderData.data.createdAt
                )
            )



            PrintSunmiUtils.addHorizontalInner()


            addOrdersForKitchenOnlineOrderSunmiInner(
                orderData.data.orderItems,
                customerReceiptPrinters.printerCategories.toCollection(arrayListOf())
            )

            _root_ide_package_.com.pays.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper.getInstance().lineWrap(1)

            if (orderData?.data?.note.isNotEmpty() == true && kitchenSettingModel.showOrderNote) {

                PrintSunmiUtils.orderNoteInnerLarge(orderData?.data?.note.toString())
            }


            if (kitchenSettingModel.showCustomerAddress != false || kitchenSettingModel.showCustomerPhone != false || kitchenSettingModel.showCustomerName != false) {
                if (orderData?.data?.customer != null) {

                    PrintSunmiUtils.customerDetailsInner()

                    if (kitchenSettingModel.showCustomerName) {
                        PrintSunmiUtils.normalTextLarge(orderData?.data?.customer?.firstName + " " + orderData?.data?.customer?.lastName)

                    }


                    if (kitchenSettingModel.showCustomerPhone) {

                        if (orderData?.data?.customer?.phones?.isNotEmpty() == true) {

                            PrintSunmiUtils.normalTextLarge(
                                MethodUtils.getUSFormatNumber(
                                    orderData?.data?.customer?.phones?.get(
                                        orderData?.data?.customer?.phones.size - 1
                                    )?.phoneNumber
                                )

                            )
                        }

                    }


                    if (orderData?.data?.customer?.addresses?.isNotEmpty() == true) {


//                        PrintSunmiUtils.normalTextLarge(
//                            orderData?.data?.customer?.addresses.get(
//                                orderData?.data?.customer?.addresses.size - 1
//                            ).fullAddress
//                        )


                        orderData?.data?.customer?.addresses.filter { it.typeOfAddress == Constants.BILLING_ADDRESS }
                            .forEach {

                                if (it.typeOfAddress.equals(
                                        Constants.BILLING_ADDRESS,
                                        ignoreCase = true
                                    )
                                ) {
                                    PrintSunmiUtils.normalTextLarge(
                                        it.fullAddress
                                    )
                                }
                            }
                    }
                }


            }

            PrintSunmiUtils.cutPaperInner()
        } catch (e: Exception) {
            // printerDialog.dismiss()
            e.printStackTrace()
        }


    }

    override fun onStatusChangeEvent(p0: String?, p1: Int) {

    }

}
