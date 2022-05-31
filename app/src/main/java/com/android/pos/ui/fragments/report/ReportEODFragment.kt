package com.android.pos.ui.fragments.report

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.android.pos.R
import com.android.pos.data.entities.Employee
import com.android.pos.data.model.ShiftRportConfiguration
import com.android.pos.data.model.responseModel.EodReportResponse
import com.android.pos.data.model.responseModel.PrinterResponse
import com.android.pos.data.model.responseModel.report.KeyValue
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.MEDIUM
import com.android.pos.data.remote.Constants.SMALL
import com.android.pos.databinding.FragmentReportEodBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.*
import com.android.pos.ui.adapter.boldpos.SalesPerCategorySummary
import com.android.pos.ui.fragments.loginscreen.ClockInOwnerViewModel
import com.android.pos.utils.*
import com.android.pos.utils.extensions.*
import com.android.pos.utils.printer.PrinterClass
import com.android.pos.utils.statusUtils.Status
import com.epson.eposprint.Builder
import com.epson.eposprint.Print
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.math.abs

@AndroidEntryPoint
class ReportEODFragment : Fragment(), AdapterView.OnItemSelectedListener {

    private var eodReportData: EodReportResponse.Data? = null
    private var shiftReportsSettingModel: ShiftRportConfiguration? = null
    private var defaultEmployeePos: Int = 0
    private lateinit var binding: FragmentReportEodBinding
    private val viewModel by viewModels<ReportEODViewModel>()
    private val viewModelClockOut by viewModels<ClockInOwnerViewModel>()
    private val TAG = "ReportEODFragment"

    private lateinit var startDate: DatePickerDialog.OnDateSetListener
    private lateinit var endDate: DatePickerDialog.OnDateSetListener
    private var customerList: List<PrinterResponse.Data.CustomerReceiptPrinters> = listOf()


    private lateinit var startTime: TimePickerDialog.OnTimeSetListener
    private lateinit var endTime: TimePickerDialog.OnTimeSetListener

    private val terminalAdapter by lazy { TerminalAdapter() }
    private val salesReportAdapter by lazy { SalesReportAdapter() }
    private val refundDetailsAdapter by lazy { SalesReportAdapter() }
    private val creditPaymentDetailsAdapter by lazy { SalesReportAdapter() }
    private val taxDetailsAdapter by lazy { SalesReportAdapter() }
    private val cashLogAdapter by lazy { SalesReportAdapter() }
    private val discountDetailsAdapter by lazy { SalesReportAdapter() }
    private val salesTaxSummaryAdapter by lazy { SalesReportAdapter() }
    private val cashEventSummaryAdapter by lazy { PaymentDetailsAdapter(hideRefund = true) }
    private val totalPaymentsAdapter by lazy { SalesReportAdapter() }
    private val cashPaymentsAdapter by lazy { SalesReportAdapter() }
    private val paymentDetailsAdapter by lazy { PaymentDetailsAdapter(hideRefund = false) }
    private val otherDetailsAdapter by lazy { SalesReportAdapter() }
    private val serviceChargeDetailsAdapter by lazy { ServiceChargeDetailsAdapter() }
    private val employeeGuestDetailsAdapter by lazy { EmployeeGuestDetailsAdapter() }
    private val creditTipAuditAdapter by lazy { CreditTipAuditAdapter() }
    private val tipDetailsAdapter by lazy { PaymentDetailsAdapter(hideRefund = false) }
    private val saleCategorySummaryAdapter by lazy { SalesPerCategorySummary() }


    private val salesOrderDetailsAdapter by lazy { SalesOrderDetailsAdapter() }

    private val creditCardBreakdownAdapter by lazy { CreditCardBreakDownAdapter(hideRefund = false) }

    private lateinit var teamEmployeeListGlobal: ArrayList<Employee>

    val myCalendar = Calendar.getInstance()
    val myCalendar1 = Calendar.getInstance()

    val myCalendar2 = Calendar.getInstance()
    val myCalendar3 = Calendar.getInstance()

    @set:Inject
    internal var prefProvider: PrefProvider? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentReportEodBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            initControls()
            initObservers()
            loadTerminals()
            customerPrinters()

            binding.imgPrintEODReport?.setOnClickListener {
                generateEODReport()

            }

            binding.txtHome.setOnClickListener {
                findNavController().navigate(R.id.action_settings_to_dashboardCategory)
            }
            binding.imgBack.setOnClickListener {
                findNavController().navigateUp()
            }
            binding.txtSearch.setOnClickListener {
                viewModel.getReportSummary("")
            }

            binding.txtEmail.setOnClickListener {


                if (viewModel.selectedTerminalId.isNotEmpty())
                    viewModel.getEmployeeEmail(viewModel.selectedTerminalId.toInt())
                        .observe(viewLifecycleOwner) {

                            if (it.status == Status.SUCCESS) {
                                val bundle = Bundle()
                                bundle.putBoolean("EOD", true)
                                bundle.putInt("type", 2)
                                bundle.putString("email", it.data?.email)
                                findNavController().navigate(
                                    R.id.action_reportEODFragment_to_sendReceiptFragment,
                                    bundle
                                )
                            }
                        }


                //  viewModel.getReportSummary("")
            }

            setFragmentResultListener("request_key_eod") { _: String, bundle: Bundle ->


                bundle.getString("email")?.let { viewModel.getReportSummary(it) }
            }

            binding.txtClockOut.setOnClickListener {

                alert(
                    getString(R.string.app_name),
                    getString(R.string.clockout_message)
                ) {
                    positiveButton(getString(android.R.string.ok)) {
                        //  viewModelClockOut.submit()

                        val bundle = Bundle()
                        bundle.putBoolean("isDashboard", true)
                        bundle.putBoolean("isSwap", false)
                        findNavController().navigate(
                            R.id.action_reportEODFragment_to_passcode,
                            bundle
                        )
                    }
                    negativeButton(R.string.tv_cancel) {
                        // Do negative stuff here
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        loadSettings()

    }

    private fun generateEODReport() {
        customerList.forEach {
            initPrinter(it)

        }
    }

    private fun initPrinter(customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters) {
        PrinterClass.closePrinter()
        if (PrinterClass.getPrinter() == null) {
            var printer: Print? = Print(requireContext())
            val enable = Print.FALSE

            try {
                printer?.openPrinter(
                    if (customerReceiptPrinters.printer_type == Constants.BLUETOOTH) {
                        Print.DEVTYPE_BLUETOOTH
                    } else {
                        Print.DEVTYPE_TCP
                    },
                    customerReceiptPrinters.ipAddress,
                    enable,
                    1000
                )


            } catch (e: Exception) {
                Log.e(TAG, "PrinterException: " + e.message)
                printer = null
                return
            }
            try {
                if (printer != null) {
                    PrinterClass.setPrinter(printer)
                    createReportFormatEOD(customerReceiptPrinters)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    }

    private fun createReportFormatEOD(customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters) {
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

            if (prefProvider?.getValue(
                    Constants.VENUE_LOGO,
                    ""
                )?.isNotEmpty() == true
            ) {
                builder.addFeedLine(1)
                builder.addTextAlign(Builder.ALIGN_CENTER)

                /* var bitmap = getBitmapFromURL(prefProvider.getValue(VENUE_LOGO, ""))*/

                val decodedString: ByteArray = android.util.Base64.decode(
                    prefProvider?.getValue(Constants.VENUE_LOGO, ""),
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
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            builder.addTextAlign(Builder.ALIGN_CENTER)
            builder.addTextSize(2, 2)

            addBuilderText(
                builder,
                prefProvider?.getValue(Constants.BUSINESS_NAME, "").toString()
            )
            builder.addFeedLine(1)
            builder.addTextFont(Builder.FONT_E)
            builder.addTextAlign(Builder.ALIGN_CENTER)
            builder.addTextLang(Builder.LANG_EN)
            addCustomerTextSize(builder, SMALL)

            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )

            addBuilderText(
                builder,
                prefProvider?.getValue(Constants.BUSINESS_ADDRESS, "")
                    .toString()
            )

            builder.addFeedLine(1)

            builder.addTextFont(Builder.FONT_E)
            builder.addTextAlign(Builder.ALIGN_CENTER)
            builder.addTextLang(Builder.LANG_EN)
            addCustomerTextSize(builder, SMALL)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )
            addBuilderText(
                builder,
                prefProvider?.getValue(Constants.BUSINESS_PHONE_NO, "").toString()
            )

            builder.addFeedLine(2)

            builder.addTextFont(Builder.FONT_E)
            builder.addTextAlign(Builder.ALIGN_CENTER)
            builder.addTextLang(Builder.LANG_EN)
            addCustomerTextSize(builder, MEDIUM)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            builder.addText("Employee End of Day Report")

            builder.addFeedLine(1)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )
            addCustomerTextSize(builder, SMALL)
            addHorizontalLine(builder)

            builder.addFeedLine(1)

            builder.addTextFont(Builder.FONT_E)
            builder.addTextAlign(Builder.ALIGN_CENTER)
            builder.addTextLang(Builder.LANG_EN)
            addCustomerTextSize(builder, SMALL)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )
            builder.addText("Employee : " + binding.spTerminals.selectedItem.toString())

            builder.addFeedLine(1)

            addHorizontalLine(builder)

            if (eodReportData?.salesSummary?.isNotEmpty() == true) {
                builder.addFeedLine(2)
                builder.addTextSize(2, 2)

                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, MEDIUM)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )
                builder.addText("ORDER SALES DETAILS")
                builder.addFeedLine(2)

                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                addCustomerTextSize(builder, SMALL)
                addHorizontalLine(builder)

                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, SMALL)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                builder.addFeedLine(1)

                eodReportData?.salesSummary?.forEach {

                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, SMALL)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )
                    builder.addText(
                        padLine(
                            it.key,
                            MethodUtils.roundOffAmount(it.value.toString().toDouble()),
                            48
                        )
                    )
                }

            }

            if (eodReportData?.salesAndTaxesSummary?.isNotEmpty() == true) {
                builder.addFeedLine(3)
                builder.addTextSize(2, 2)

                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_CENTER)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, MEDIUM)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )
                builder.addText("SALES AND TAXES SUMMARY")

                builder.addFeedLine(2)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                addCustomerTextSize(builder, SMALL)
                addHorizontalLine(builder)


                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                // builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, SMALL)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.TRUE,
                    Builder.COLOR_1
                )
                builder.addText(padLine("Category(Quantity)","Amount",48))
                builder.addFeedLine(1)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                addCustomerTextSize(builder, SMALL)
                addHorizontalLine(builder)


                eodReportData?.salesAndTaxesSummary?.forEach {
                    builder.addTextLineSpace(30)
                    builder.addFeedUnit(30)
                    builder.addTextFont(Builder.FONT_E)
                    // builder.addTextAlign(Builder.ALIGN_LEFT)
                    builder.addTextLang(Builder.LANG_EN)
                    addCustomerTextSize(builder, SMALL)
                    builder.addTextStyle(
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.FALSE,
                        Builder.COLOR_1
                    )
                    builder.addText(
                        padLine(
                            it.key,
                            MethodUtils.roundOffAmount(it.value.toString().toDouble()),
                            48
                        )
                    )
                }

            }






            builder.addFeedLine(1)
            builder.addCut(Builder.CUT_FEED)

            val status = IntArray(1)
            val battery = IntArray(1)

            try {


                PrinterClass.getPrinter()?.sendData(
                    builder,
                    if (customerReceiptPrinters.name.substring(0, 6).toString()
                            .lowercase() == "TM-m30".lowercase() || customerReceiptPrinters.name.substring(
                            0,
                            6
                        ).toString().lowercase() == "TM-m10".lowercase()
                    ) {
                        PrinterClass.BLUETOOTH_TIMEOUT
                    } else {
                        PrinterClass.BLUETOOTH_TIMEOUT

                    }, status, battery
                )

                PrinterClass.closePrinter()

                //findNavController().navigate(R.id.action_orderCompleteFragment_to_dashboardCategoryNew)
                //PrinterClass.getPrinter()?.sendData(builder, 0, status, battery)
            } catch (e: Exception) {
                PrinterClass.closePrinter()
                e.printStackTrace()
                Log.e(TAG, "PrinterError: " + e.localizedMessage)
            }

        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun customerPrinters() {
        viewModel.getCustomerPrinterList().observe(viewLifecycleOwner, {
            when (it.status) {
                Status.SUCCESS -> {
                    ProgressUtils.dismissProgressDialog()
                    if (it.data != null) {
                        customerList = it.data


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
        )
    }

    private fun loadSettings() {

        val shiftReportsSettings = prefProvider?.getValue(Constants.SHIFT_REPORT_SETTINGS, "")

        if (shiftReportsSettings != null)
            shiftReportsSettingModel =
                Gson().fromJson(shiftReportsSettings, ShiftRportConfiguration::class.java)
    }

    private fun initControls() {
        binding.nsvReport.isNestedScrollingEnabled = false
        binding.spTerminals.onItemSelectedListener = this

        setupAdapter()
        setupCalender()
    }

    private fun setupCalender() {
        startTime = TimePickerDialog.OnTimeSetListener { view, hour, minute ->
            val timecalender = Calendar.getInstance()
            timecalender.set(Calendar.HOUR_OF_DAY, hour)
            timecalender.set(Calendar.MINUTE, minute)
            viewModel.startDate.value = timeCalculateForStartEndTime(hour, minute, "isstart")
            if (differnceTrue(viewModel.startDate.value!!, viewModel.endDate.value) <= 30) {
                viewModel.getReportSummary("")
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
            if (differnceTrue(viewModel.endDate.value!!, viewModel.startDate.value) <= 30) {
                viewModel.getReportSummary("")
            } else {
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
                myCalendar2.get(Calendar.HOUR),
                myCalendar2.get(Calendar.MINUTE),
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
                myCalendar2.get(Calendar.HOUR),
                myCalendar2.get(Calendar.MINUTE),
                false
            ).show()

        }

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
        Log.e("CheckDate", "startingDate   $startDatestring $timestring")
        return "$startDatestring $timestring"
    }

    private fun setupAdapter() {

        binding.rvTerminal.adapter = terminalAdapter

        binding.rvSalesSummary.adapter = salesReportAdapter
        binding.rvRefundDetails.adapter = refundDetailsAdapter
        binding.rvPendingPayments.adapter = creditPaymentDetailsAdapter
        binding.rvTaxDetails.adapter = taxDetailsAdapter
        binding.rvDiscountDetails.adapter = discountDetailsAdapter
        binding.rvSalesTaxSummary.adapter = salesTaxSummaryAdapter
        binding.rvRefundAndVoid.adapter = cashEventSummaryAdapter
        binding.rvTotalPayments.adapter = totalPaymentsAdapter
        binding.rvCashPayments.adapter = cashPaymentsAdapter
        binding.rvPaymentDetails.adapter = paymentDetailsAdapter
        binding.rvOtherDetails.adapter = otherDetailsAdapter
        binding.rvServiceChargeDetails.adapter = serviceChargeDetailsAdapter
        binding.rvTipsDetails.adapter = tipDetailsAdapter
        binding.rvCashLog.adapter = cashLogAdapter
        binding.rvCreditCardBreakDown.adapter = creditCardBreakdownAdapter
        binding.rvSalesDetails.adapter = salesOrderDetailsAdapter
        binding.rvCreditAuditTip.adapter = creditTipAuditAdapter
        binding.rvemployeeGuestDetails.adapter = employeeGuestDetailsAdapter
        binding.rvSaleCategorySummary?.adapter = saleCategorySummaryAdapter
    }

    private fun initObservers() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)





        viewModel.startDateSelection.observe(requireActivity()) { event ->
            event.getContentIfNotHandled()?.let {

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
        viewModel.endDateSelection.observe(requireActivity()) { event ->
            event.getContentIfNotHandled()?.let {

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
        viewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }
        viewModel.data.observe(viewLifecycleOwner, EventObserver { data ->
            data.let {

                eodReportData = it


                //salesSummary
                showHide(
                    rvMedia = binding.rvSalesSummary,
                    textView = binding.txtSalesSummary,
                    headerView = null,
                    visible = it.salesSummary.isNotEmpty()
                )
                salesReportAdapter.add(it.salesSummary)

                showHide(
                    rvMedia = binding.rvCashLog,
                    textView = binding.txtCashLog,
                    headerView = null,
                    visible = it.cashLogDetails.isNotEmpty()
                )
                cashLogAdapter.add(it.cashLogDetails)

                showHide(
                    rvMedia = binding.rvSalesTaxSummary,
                    textView = binding.txtSalesTaxSummary,
                    headerView = null,
                    visible = it.salesAndTaxesSummary.isNotEmpty()
                )
                if (it.salesAndTaxesSummary.isEmpty()) {
                    binding.headerSalesTaxSummary.gone()
                }
                salesTaxSummaryAdapter.add(it.salesAndTaxesSummary)


                showHide(
                    rvMedia = binding.rvTaxDetails,
                    textView = binding.txtTaxDetails,
                    headerView = null,
                    visible = it.taxDetails.isNotEmpty()
                )
                taxDetailsAdapter.add(it.taxDetails)


                showHide(
                    rvMedia = binding.rvRefundDetails,
                    textView = binding.txtRefundDetails,
                    headerView = null,
                    visible = it.refundDetails.isNotEmpty()
                )
                refundDetailsAdapter.add(it.refundDetails)

                showHide(
                    rvMedia = binding.rvDiscountDetails,
                    textView = binding.txtDiscountDetails,
                    headerView = null,
                    visible = it.discountDetails.isNotEmpty()
                )
                discountDetailsAdapter.add(it.discountDetails)


                showHide(
                    rvMedia = binding.rvCashPayments,
                    textView = binding.txtCashPayments,
                    headerView = null,
                    visible = it.totalCashPayments.isNotEmpty()
                )
                cashPaymentsAdapter.add(it.totalCashPayments)


                showHide(
                    rvMedia = binding.rvTotalPayments,
                    textView = binding.txtTotalPayments,
                    headerView = null,
                    visible = it.totalPayments.isNotEmpty()
                )
                totalPaymentsAdapter.add(it.totalPayments)


                showHide(
                    rvMedia = binding.rvPaymentDetails,
                    textView = binding.txtPaymentDetails,
                    headerView = binding.ilPaymentDetails,
                    visible = it.paymentDetails.isNotEmpty()
                )
                paymentDetailsAdapter.add(it.paymentDetails)


                showHide(
                    rvMedia = binding.rvServiceChargeDetails,
                    textView = binding.txtServiceChargeDetails,
                    headerView = null,
                    visible = it.serviceChargeDetails.isNotEmpty()
                )
                serviceChargeDetailsAdapter.add(it.serviceChargeDetails)


                it.employeeGuestDetails?.let { it1 ->
                    showHide(
                        rvMedia = binding.rvemployeeGuestDetails,
                        textView = binding.txtemployeeGuestDetails,
                        headerView = null,
                        visible = it1.isNotEmpty()
                    )
                }
                if (it.employeeGuestDetails?.isNotEmpty() == true)
                    employeeGuestDetailsAdapter.add(it.employeeGuestDetails[0])


                showHide(
                    rvMedia = binding.rvCreditAuditTip,
                    textView = binding.txtCreditAuditTip,
                    headerView = null,
                    visible = it.creditTipAudit.isNotEmpty()
                )
                creditTipAuditAdapter.add(it.creditTipAudit)

                it.salesPerCategorySummary?.let { it1 ->
                    showHide(
                        rvMedia = binding.rvSaleCategorySummary,
                        textView = binding.txtSaleCategorySummary,
                        headerView = null,
                        visible = it1.isNotEmpty()
                    )
                }
                if (it.salesPerCategorySummary != null) {

                    var arrayListSalePerCategory: ArrayList<KeyValue> = arrayListOf()

                    it.salesPerCategorySummary.forEachIndexed { index, arrayList ->
                        if (index == 0) {
                            arrayListSalePerCategory.add(KeyValue("Cash Sales", ""))
                            arrayList.forEach {
                                arrayListSalePerCategory.add(it)
                            }
                        } else if (index == 1) {
                            arrayListSalePerCategory.add(KeyValue("Credit/Non Cash Sales", ""))
                            arrayList.forEach {
                                arrayListSalePerCategory.add(it)
                            }

                        }
                    }
                    if (arrayListSalePerCategory.isNotEmpty()) {
                        saleCategorySummaryAdapter.add(arrayListSalePerCategory)
                        saleCategorySummaryAdapter.notifyDataSetChanged()
                    }
                }



                showHide(
                    rvMedia = binding.rvTipsDetails,
                    textView = binding.txtTipsDetails,
                    headerView = binding.ilTipsDetails,
                    visible = it.tipDetails.isNotEmpty()
                )
                tipDetailsAdapter.add(it.tipDetails)

                showHide(
                    rvMedia = binding.rvPendingPayments,
                    textView = binding.txtPendingPayments,
                    headerView = null,
                    visible = it.totalCreditPaymentDetails.isNotEmpty()
                )
                creditPaymentDetailsAdapter.add(it.totalCreditPaymentDetails)


                showHide(
                    rvMedia = binding.rvRefundAndVoid,
                    textView = binding.txtCashEventSummary,
                    headerView = null,
                    visible = it.refundAndVoidDetails.isNotEmpty()
                )
                if (it.refundAndVoidDetails.isEmpty()) {
                    binding.headerRefundsVoids.gone()
                }
                cashEventSummaryAdapter.add(it.refundAndVoidDetails)


                showHide(
                    rvMedia = binding.rvOtherDetails,
                    textView = null,
                    headerView = null,
                    visible = it.otherDetails.isNotEmpty()
                )
                otherDetailsAdapter.add(it.otherDetails)


                showHide(
                    rvMedia = binding.rvCreditCardBreakDown,
                    textView = binding.txtCreditCardBreakDown,
                    headerView = null,
                    visible = it.otherDetails.isNotEmpty()
                )
                if (it.creditCardBreakdown.isEmpty()) {
                    binding.ilCreditCardBreakDown.gone()
                } else {
                    binding.ilCreditCardBreakDown.visible()
                }
                creditCardBreakdownAdapter.add(it.creditCardBreakdown)

                showHide(
                    rvMedia = binding.rvSalesDetails,
                    textView = binding.txtSalesDetails,
                    headerView = null,
                    visible = it.orderSalesDetails.data.isNotEmpty()
                )

                if (it.orderSalesDetails.data.isNotEmpty()) {
                    binding.llHeader.visible()
                    binding.llTotal.visible()
                    MethodUtils.setPriceTextView(binding.txtTotalAmount, it.orderSalesDetails.total)

                } else {
                    binding.llHeader.gone()
                    binding.llTotal.gone()
                }

                salesOrderDetailsAdapter.add(it.orderSalesDetails.data)


                binding.linReports.visible()

                shiftReportsSettingModel?.orderSalesDetails?.let { it1 ->
                    showHide(
                        binding.rvSalesDetails, binding.txtSalesDetails, null,
                        it1
                    )
                }

                if (shiftReportsSettingModel?.orderSalesDetails == false) {
                    binding.llHeader.gone()
                    binding.llTotal.gone()
                }

                shiftReportsSettingModel?.salesSummary?.let { it1 ->
                    showHide(
                        binding.rvSalesSummary, binding.txtSalesSummary, null,
                        it1
                    )
                }

                shiftReportsSettingModel?.salesAndTaxSummary?.let { it1 ->
                    showHide(
                        binding.rvSalesTaxSummary, binding.txtSalesTaxSummary, null,
                        it1
                    )
                }

                if (shiftReportsSettingModel?.salesAndTaxSummary == false) {
                    binding.headerSalesTaxSummary.gone()
                }


                shiftReportsSettingModel?.paymentDetails?.let { it1 ->
                    showHide(
                        binding.rvPaymentDetails,
                        binding.txtPaymentDetails,
                        binding.ilPaymentDetails,
                        it1
                    )
                }

                shiftReportsSettingModel?.tipsDetails?.let { it1 ->
                    showHide(
                        binding.rvTipsDetails, binding.txtTipsDetails, binding.ilTipsDetails,
                        it1
                    )
                }
                shiftReportsSettingModel?.taxDetails?.let { it1 ->
                    showHide(
                        binding.rvTaxDetails, binding.txtTaxDetails, null,
                        it1
                    )
                }
                shiftReportsSettingModel?.refundOrVoids?.let { it1 ->
                    showHide(
                        binding.rvRefundAndVoid, binding.txtCashEventSummary, null,
                        it1
                    )
                }

                if (shiftReportsSettingModel?.refundOrVoids == false) {
                    binding.headerRefundsVoids.gone()
                }

                shiftReportsSettingModel?.refundDetails?.let { it1 ->
                    showHide(
                        binding.rvRefundDetails, binding.txtRefundDetails, null,
                        it1
                    )
                }

                shiftReportsSettingModel?.discountDetails?.let { it1 ->
                    showHide(
                        binding.rvDiscountDetails, binding.txtDiscountDetails, null,
                        it1
                    )
                }
                shiftReportsSettingModel?.totalCreditPayments?.let { it1 ->
                    showHide(
                        binding.rvPendingPayments, binding.txtPendingPayments, null,
                        it1
                    )
                }

                shiftReportsSettingModel?.totalCashPayments?.let { it1 ->
                    showHide(
                        binding.rvCashPayments, binding.txtCashPayments, null,
                        it1
                    )
                }
                shiftReportsSettingModel?.totalPayments?.let { it1 ->
                    showHide(
                        binding.rvTotalPayments, binding.txtTotalPayments, null,
                        it1
                    )
                }
                shiftReportsSettingModel?.creditCardBreakdown?.let { it1 ->
                    showHide(
                        binding.rvCreditCardBreakDown, binding.txtCreditCardBreakDown, null,
                        it1
                    )
                }

                if (shiftReportsSettingModel?.creditCardBreakdown == false) {
                    binding.ilCreditCardBreakDown.gone()
                }

                shiftReportsSettingModel?.serviceChargeDetails?.let { it1 ->
                    showHide(
                        binding.rvServiceChargeDetails, binding.txtServiceChargeDetails, null,
                        it1
                    )
                }
                shiftReportsSettingModel?.cashLogDetails?.let { it1 ->
                    showHide(
                        binding.rvCashLog, binding.txtCashLog, null,
                        it1
                    )
                }
                shiftReportsSettingModel?.otherDetails?.let { it1 ->
                    showHide(
                        binding.rvOtherDetails, binding.txtOtherDetails, null,
                        it1
                    )
                }

                shiftReportsSettingModel?.creditTipAudit?.let { it1 ->
                    showHide(
                        binding.rvCreditAuditTip, binding.txtCreditAuditTip, null,
                        it1
                    )
                }
                shiftReportsSettingModel?.cashCreditPerSalesCategorySummary?.let { it1 ->
                    showHide(
                        binding.rvSaleCategorySummary, binding.txtSaleCategorySummary, null,
                        it1
                    )
                }

                shiftReportsSettingModel?.employeeGuestReport?.let { it1 ->
                    showHide(
                        binding.rvemployeeGuestDetails, binding.txtemployeeGuestDetails, null,
                        it1
                    )
                }


            }
        })
    }

    private fun showHide(
        rvMedia: RecyclerView,
        textView: TextView?,
        headerView: ViewBinding?,
        visible: Boolean
    ) {
        if (visible) {
            rvMedia.visible()
            textView?.visible()
            headerView?.root?.visible()

//            if (headerView == binding.ilPaymentDetails) {
//                binding.ilPaymentDetails.txtRefund.gone()
//            }
        } else {
            rvMedia.gone()
            textView?.gone()
            headerView?.root?.gone()
        }
    }

    private fun loadTerminals() {

        viewModel.employeeData.observe(viewLifecycleOwner) {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        resource.data?.let { employeeList ->
                            teamEmployeeListGlobal = employeeList as ArrayList<Employee>

                            Log.e("teamEmployeeListGlobal", Gson().toJson(teamEmployeeListGlobal))

                            val isPresent =
                                teamEmployeeListGlobal.any { it.name == "All Team Members" }
                            if (!isPresent) {
                                //  teamEmployeeListGlobal.removeAt(0)
                                teamEmployeeListGlobal.add(
                                    0,
                                    Employee(
                                        "",
                                        "",
                                        -1,
                                        false,
                                        "",
                                        "",
                                        -1,
                                        "All Team Members",
                                        "",
                                        "",
                                        "",
                                        false,
                                        -1,
                                        "",
                                        -1,
                                        0.0,
                                        false
                                    )
                                )
                            }

                            teamEmployeeListGlobal.forEachIndexed { index, employee ->

                                if (viewModel.employeeId() == employee.id) {
                                    defaultEmployeePos = index
                                    return@forEachIndexed
                                }

                            }

                            val roleName = teamEmployeeListGlobal.map { it.name }


                            setUpEmployeeSpinnerAdapter(
                                roleName as ArrayList<String>,
                                defaultEmployeePos
                            )



                            viewModel.setCurrentDate(myCalendar)
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

    private fun setUpEmployeeSpinnerAdapter(
        terminalList: ArrayList<String>,
        defaultEmployeePos: Int
    ) {
        val spinnerAdapter = ArrayAdapter(
            requireActivity(),
            R.layout.row_spinner,
            terminalList
        )

        try {
            spinnerAdapter.setDropDownViewResource(R.layout.row_spinner)
            binding.spTerminals.adapter = spinnerAdapter

            binding.spTerminals.setSelection(defaultEmployeePos, false);
            Log.e("defaultEmployeePos", defaultEmployeePos.toString())
            binding.spTerminals.setSelection(defaultEmployeePos)
        } catch (e: Exception) {
        }

    }


    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

        try {
            if (teamEmployeeListGlobal.size > 0 && position > 0 && position < teamEmployeeListGlobal.size) {
                viewModel.selectedTerminalId = teamEmployeeListGlobal[position].id.toString()
                Log.e("selectedEmpId", teamEmployeeListGlobal[position].id.toString())
            } else {
                viewModel.selectedTerminalId = ""
            }

            viewModel.getReportSummary("")
        } catch (e: Exception) {
            e.printStackTrace()
        }


    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }
}