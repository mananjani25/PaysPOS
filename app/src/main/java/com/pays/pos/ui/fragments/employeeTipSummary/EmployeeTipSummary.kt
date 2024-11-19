package com.pays.pos.ui.fragments.employeeTipSummary

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.epson.eposprint.Builder
import com.epson.eposprint.Print
import com.google.gson.Gson
import com.pays.pos.R
import com.pays.pos.data.model.responseModel.PrinterResponse
import com.pays.pos.data.model.responseModel.employeeTipSummary.EmployeeTipSummaryResponse
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.EMAIL
import com.pays.pos.data.remote.Constants.END_DATE
import com.pays.pos.data.remote.Constants.START_DATE
import com.pays.pos.databinding.FragmentEmployeeTipSammaryBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.logger.MessageEvent
import com.pays.pos.ui.adapter.EmployeeTipSummaryAdapter
import com.pays.pos.ui.fragments.payment.OrderCompleteFragment
import com.pays.pos.ui.fragments.settings.hardware.printer.BluetoothUtil
import com.pays.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper
import com.pays.pos.utils.*
import com.pays.pos.utils.extensions.differenceTrue
import com.pays.pos.utils.extensions.timeCalculateForStartEndTime
import com.pays.pos.utils.landi.LPrint
import com.pays.pos.utils.printer.PrinterClass
import com.pays.pos.utils.statusUtils.Status
import com.sunmi.externalprinterlibrary.api.ConnectCallback
import com.sunmi.externalprinterlibrary.api.SunmiPrinter
import com.sunmi.externalprinterlibrary.api.SunmiPrinterApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class EmployeeTipSummary : Fragment() {


    private var ETSdataList: ArrayList<EmployeeTipSummaryResponse.Data>? = null

    @set:Inject
    internal var prefProvider: PrefProvider? = null

    private val TAG = "EmployeeTipSummary"

    private var customerList: List<PrinterResponse.Data.CustomerReceiptPrinters> = listOf()

    private val viewModel by viewModels<EmployeeTipSummaryViewModel>()
    private lateinit var binding: FragmentEmployeeTipSammaryBinding
    private val employeeTipSummaryAdapter by lazy { EmployeeTipSummaryAdapter() }

    private lateinit var startDate: DatePickerDialog.OnDateSetListener
    private lateinit var endDate: DatePickerDialog.OnDateSetListener

    private lateinit var startTime: TimePickerDialog.OnTimeSetListener
    private lateinit var endTime: TimePickerDialog.OnTimeSetListener

    val myCalendar = Calendar.getInstance()
    val myCalendar1 = Calendar.getInstance()

    val myCalendar2 = Calendar.getInstance()
    val myCalendar3 = Calendar.getInstance()

    private var sunmiFrameworkVersion: Array<String>? =
        null //Fetching Sunmi OS version to format printing.

    var onBluetoothPermissionGranted: OrderCompleteFragment.OnBluetoothPermissionGranted? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEmployeeTipSammaryBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        //        3.3.39
        sunmiFrameworkVersion =
            prefProvider?.getValue(Constants.SUNMI_FRAMEWORK_VERSION, "").toString().split(".")
                .toTypedArray()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        setupCalender()
        initAdapter()
        setInitDate()
        loadData()
        initObserver()
        customerPrinters()
    }


    private fun loadData() {

        viewModel.getEmployeeTipSummary()
    }

    private fun initObserver() {

        viewModel.startDateSelection.observe(requireActivity()) { event ->
            event.getContentIfNotHandled()?.let {

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
        viewModel.endDateSelection.observe(requireActivity()) { event ->
            event.getContentIfNotHandled()?.let {

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


        viewModel.data.observe(viewLifecycleOwner, EventObserver { data ->
            data.let {

                ETSdataList = it.data
                ETSdataList?.let { it1 ->
                    employeeTipSummaryAdapter.add(it1)

                }

            }

        })

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

    private fun customerPrinters() {
        viewModel.getCustomerPrinterList().observe(
            viewLifecycleOwner
        ) {
            when (it.status) {
                Status.SUCCESS -> {
                    // ProgressUtils.dismissProgressDialog()
                    if (it.data != null) {
                        customerList = it.data
                    }

                }

                Status.ERROR -> {
                    // ProgressUtils.dismissProgressDialog()
                }

                Status.LOADING -> {
                    //  ProgressUtils.showProgressDialog(requireActivity())
                }

            }

        }
    }

    private fun setInitDate() {

        viewModel.setCurrentDate(myCalendar)

    }

    private fun initAdapter() {

        binding.apply {

            rvEmployeeTipSummary.adapter = employeeTipSummaryAdapter

        }
    }

    private fun setupCalender() {
        startTime = TimePickerDialog.OnTimeSetListener { view, hour, minute ->
            val timecalender = Calendar.getInstance()
            timecalender.set(Calendar.HOUR_OF_DAY, hour)
            timecalender.set(Calendar.MINUTE, minute)
            viewModel.startDate.value = requireContext().timeCalculateForStartEndTime(
                hour,
                minute,
                "isstart",
                myCalendar,
                myCalendar1
            )
            if (requireContext().differenceTrue(
                    viewModel.startDate.value!!,
                    viewModel.endDate.value
                ) <= 30
            ) {
                // Call API here
                viewModel.getEmployeeTipSummary()
                Log.e(
                    "setupCalender",
                    "1 start date = ${viewModel.startDate.value}, End date = ${viewModel.endDate.value}"
                )

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
            viewModel.endDate.value = requireContext().timeCalculateForStartEndTime(
                hour,
                minute,
                "isend",
                myCalendar,
                myCalendar1
            )
            if (requireContext().differenceTrue(
                    viewModel.endDate.value!!,
                    viewModel.startDate.value
                ) <= 30
            ) {
                // Call API here
                viewModel.getEmployeeTipSummary()
                Log.e(
                    "setupCalender",
                    "2 start date = ${viewModel.startDate.value}, End date = ${viewModel.endDate.value}"
                )

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
                myCalendar1.get(2),
                myCalendar1.get(2),
                false
            ).show()

        }
    }

    private fun sendEmail() {

        if (viewModel.terminalId != -1) {
            viewModel.getEmployeeEmail(viewModel.terminalId)
                .observe(viewLifecycleOwner) {

                    if (it.status == Status.SUCCESS) {
                        val bundle = Bundle()
                        bundle.putBoolean("EOD", true)
                        bundle.putBoolean("ETS", true)
                        bundle.putInt("type", 2)
                        bundle.putString(EMAIL, it.data?.email)
                        bundle.putString(START_DATE, viewModel.startDate.value)
                        bundle.putString(END_DATE, viewModel.endDate.value)
                        findNavController().navigate(
                            R.id.action_reports_to_sendReceiptFragment,
                            bundle
                        )
                    }
                }
        } else {
            val bundle = Bundle()
            bundle.putBoolean("EOD", true)
            bundle.putInt("type", 2)
            bundle.putString(EMAIL, "")
            bundle.putString(START_DATE, viewModel.startDate.value)
            bundle.putString(END_DATE, viewModel.endDate.value)
            findNavController().navigate(
                R.id.action_reports_to_sendReceiptFragment,
                bundle
            )

        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: String?) {
        // Do something
        if (event.equals("1")) {
            sendEmail()
        } else if (event.equals("2")) {
            generateEmployeeTipSummaryReceiptPrint()
        } else {
            if (event != null) {
                viewModel.getEmployeeTipSummary()
            }
        }
    }

    private fun generateEmployeeTipSummaryReceiptPrint() {

        customerList.forEach {
            if (it.status) {
                initPrinter(it)
            }
        }
    }

    private fun initPrinter(customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters) {
        Log.d("BIS-685", "initPrinter: Called")

        if (customerReceiptPrinters.name.startsWith(Constants.SUNMI_PRINTER, true)) {

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
                            createReportFormatETSsunmi()

                        }

                        override fun onDisconnect() {
                            println("onDisconnect")
                        }

                    })
            } else {
                createReportFormatETSsunmi()
            }

        } else if (customerReceiptPrinters.name.startsWith(Constants.SUNMI_INNER_PRINTER, true)) {

            SunmiPrintHelper.getInstance().initSunmiPrinterService(requireContext())
            viewLifecycleOwner.lifecycleScope.launch {
                delay(100)
                setService(customerReceiptPrinters)
            }


        } else if (customerReceiptPrinters.name.startsWith(Constants.LANDI_INNER_PRINTER, true)) {

            viewLifecycleOwner.lifecycleScope.launch {
                delay(100)
                createReportFormatETSLandi(customerReceiptPrinters)
            }


        } else {


            viewLifecycleOwner.lifecycleScope.launch {
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
                        LogUtil.logE(TAG, "PrinterException: " + e.message)
                        printer = null
                        return@launch
                    }
                    try {
                        if (printer != null) {
                            PrinterClass.setPrinter(printer)
                            createReportFormatETSM30(customerReceiptPrinters)
                        }

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun checkBluetoothPermissions(onBluetoothPermissionGranted: OrderCompleteFragment.OnBluetoothPermissionGranted) {
        this.onBluetoothPermissionGranted = onBluetoothPermissionGranted
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf<String>(Manifest.permission.BLUETOOTH),
                Constants.PERMISSION_BLUETOOTH
            )
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.BLUETOOTH_ADMIN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf<String>(Manifest.permission.BLUETOOTH_ADMIN),
                Constants.PERMISSION_BLUETOOTH_ADMIN
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf<String>(Manifest.permission.BLUETOOTH_CONNECT),
                Constants.PERMISSION_BLUETOOTH_CONNECT
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf<String>(Manifest.permission.BLUETOOTH_SCAN),
                Constants.PERMISSION_BLUETOOTH_SCAN
            )
        } else {
            onBluetoothPermissionGranted.onPermissionsGranted()
        }
    }


    private fun createReportFormatETSLandi(customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters) {
        this.checkBluetoothPermissions(object : OrderCompleteFragment.OnBluetoothPermissionGranted {
            override fun onPermissionsGranted() {
                GlobalScope.launch {
                    LPrint.connectLandiInnerPrinter(customerReceiptPrinters.macAddress)
                        ?.let { outputStream ->
                            LPrint.apply {
                                setOutputStream(outputStream)

                                try {
                                    printCenter(
                                        prefProvider?.getValue(
                                            Constants.BUSINESS_NAME,
                                            ""
                                        ) ?: "",
                                        fontSize = FONT_SIZE_5X,
                                        isBold = true,
                                        printOnNewLine = true
                                    )
                                    lineBreak()
                                    var venueAddress =
                                        prefProvider?.getValue(Constants.BUSINESS_ADDRESS, "")

                                    var businessPhoneNumber = prefProvider?.getValue(
                                        Constants.BUSINESS_PHONE_NO,
                                        ""
                                    )

                                    printCenter(venueAddress ?: "", fontSize = SMALL_SIZE)
                                    lineBreak()

                                    businessPhoneNumber?.let {
                                        if (it.isNotEmpty()) {
                                            printCenter(it, fontSize = SMALL_SIZE)
                                            lineBreak()
                                        }
                                    }

                                    printCenter(
                                        getString(R.string.employee_tip_summary),
                                        fontSize = FONT_SIZE_5X
                                    )
                                    lineBreak()
                                    printCenter("Employee : " + prefProvider?.employeeName())
                                    lineBreak()
                                    printDashedLineAndBreak()

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        val current = LocalDateTime.now()
                                        val formatter =
                                            DateTimeFormatter.ofPattern("MMM-dd-yyyy hh:mm:a")
                                        val formatted = current.format(formatter)
                                        printLeft(
                                            "Print Time: " + formatted,
                                            fontSize = RESET_FONT_SIZE
                                        )
                                    }

                                    lineBreak()

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        printLeft(
                                            "Report Time: ${viewModel.startDate.value} To \n${viewModel.endDate.value}"
                                        )
                                    }

                                    lineBreak()
                                    lineBreak()

/*-------------------The table should be printed here---------------*/
                                    val headerWidths = intArrayOf(20, 10, 10, 10, 10) // Set the column widths
                                    val widths = intArrayOf(13, 9, 9, 9, 8) // Set the column widths
                                    printTableRow(outputStream, arrayOf("Employee      Cash    Card   External    Total"), headerWidths)
                                    lineBreak()
                                    printDashedLineAndBreak()

                                    ETSdataList?.forEach {
                                        printTableRow(
                                            outputStream, arrayOf(
                                                "${MethodUtils.ellipsize(it.employee_name, 8)}",
                                                "$${standardAmount(it.total_cash_tips)}",
                                                "$${standardAmount(it.total_card_tips)}",
                                                "$${standardAmount(it.total_external_tips)}",
                                                "$${standardAmount(it.total_tips)}",
                                            ), widths
                                        )
                                        lineBreak()
                                    }

/*-------------------The table should be printed here---------------*/

/*                                    printCenter("Employee   Cash    Card    External    Total", fontSize = RESET_FONT_SIZE)
                                    lineBreak()
                                    ETSdataList?.forEach {
                                        printLeft("${MethodUtils.ellipsize(it.employee_name,6)}   $${standardAmount(it.total_cash_tips)}    $${standardAmount(it.total_card_tips)}    $${standardAmount(it.total_external_tips)}     $${standardAmount(it.total_tips)}", fontSize = RESET_FONT_SIZE)
                                        lineBreak()
                                    }*/
                                    lineBreak()

                                    printLeft("${getString(R.string.tv_employee).uppercase()} x _______________________________")
                                    lineBreak()
                                    lineBreak()
                                    printLeft("${getString(R.string.cash_received_by).uppercase()} x __________________________")
                                    lineBreak()

                                } catch (e: Exception) {

                                    EventBus.getDefault()
                                        .post(
                                            MessageEvent(
                                                "${Constants.LINE_BREAK_TAB} EmployeeTipSummary.kt _createReportFormatETSLandi() -> ${
                                                    Gson().toJson(
                                                        e.printStackTrace()
                                                    )
                                                }"
                                            )
                                        )

                                    e.printStackTrace()
                                }
                                lineBreak()
                                paperCut()
                                disconnectLandiPrinter()
                            }
                        }
                }
            }

            private fun standardAmount(value: Double): String {
                if (value == 0.0) {
                    return "0.00"
                } else
                    return value.toString()
            }
        })
    }

    private fun createReportFormatETSM30(customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters) {

        try {

            var builder: Builder? = null
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
            addCustomerTextSize(builder, Constants.SMALL)

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
            addCustomerTextSize(builder, Constants.SMALL)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )
            addBuilderText(
                builder,
                MethodUtils.getUSFormatNumber(
                    prefProvider?.getValue(
                        Constants.BUSINESS_PHONE_NO,
                        ""
                    ).toString()
                )
            )

            builder.addFeedLine(2)

            builder.addTextFont(Builder.FONT_E)
            builder.addTextAlign(Builder.ALIGN_CENTER)
            builder.addTextLang(Builder.LANG_EN)
            addCustomerTextSize(builder, Constants.MEDIUM)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )


            builder.addText("Employee Tips Summary")

            builder.addFeedLine(1)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )
            addCustomerTextSize(builder, Constants.SMALL)
            addHorizontalLine(builder)

            builder.addFeedLine(1)

            builder.addTextFont(Builder.FONT_E)
            builder.addTextAlign(Builder.ALIGN_CENTER)
            builder.addTextLang(Builder.LANG_EN)
            addCustomerTextSize(builder, Constants.SMALL)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )


            builder.addText("Employee : " + prefProvider?.employeeName())

            builder.addFeedLine(1)

            addHorizontalLine(builder)

            builder.addFeedLine(1)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val current = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy hh:mm:a")
                val formatted = current.format(formatter)

                builder.addTextLineSpace(30)
                builder.addFeedUnit(30)
                builder.addTextFont(Builder.FONT_E)
                builder.addTextAlign(Builder.ALIGN_LEFT)
                builder.addTextLang(Builder.LANG_EN)
                addCustomerTextSize(builder, Constants.SMALL)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                builder.addText("Print Time:" + formatted)

                builder.addText("Report Time:" + viewModel.startDate.value + "To\n" + viewModel.endDate.value)
            }


            builder.addFeedLine(2)
            addCustomerTextSize(builder, Constants.SMALL)
            addHorizontalLine(builder)

            addSixHeaderForEmployeeTipSummary(builder)

            addCustomerTextSize(builder, Constants.SMALL)
            addHorizontalLine(builder)

            ETSdataList?.forEach {
                addItemsInEmployeeTipsSummaryM30(it, builder)
            }


            builder.addFeedLine(3)


            builder.addTextFont(Builder.FONT_E)
            builder.addTextAlign(Builder.ALIGN_CENTER)
            builder.addTextLang(Builder.LANG_EN)
            addCustomerTextSize(builder, Constants.SMALL)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            builder.addText("EMPLOYEE x " + com.pays.pos.utils.repeat("_", 37))

            builder.addFeedLine(2)
            builder.addTextFont(Builder.FONT_E)
            builder.addTextAlign(Builder.ALIGN_CENTER)
            builder.addTextLang(Builder.LANG_EN)
            addCustomerTextSize(builder, Constants.SMALL)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            builder.addText("CASH RECEIVED BY" + com.pays.pos.utils.repeat("_", 32))




            builder.addFeedLine(2)
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
                        10 * 10000
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
                LogUtil.logE(TAG, "PrinterError: " + e.localizedMessage)
            }
        } catch (e: Exception) {
            LogUtil.logE(TAG, "PrinterError: 2" + e.localizedMessage)
        }


    }

    private fun setService(customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters) {
        if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.FoundSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper", "FoundSunmiPrinter")

            if (!BluetoothUtil.isBlueToothPrinter) {
                createReportFormatETSsunmiInnerPrinter()
            }

        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.CheckSunmiPrinter) {
            Handler(Looper.getMainLooper()).postDelayed(
                { setService(customerReceiptPrinters) },
                2000
            )
            LogUtil.logE("SunmiPrintHelper", "CheckSunmiPrinter")
        } else if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.LostSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper", "LostSunmiPrinter")
        } else {
            LogUtil.logE("SunmiPrintHelper", "ELSE")
        }
    }

    private fun createReportFormatETSsunmiInnerPrinter() {

        if (prefProvider?.getValue(
                Constants.VENUE_LOGO,
                ""
            )?.isNotEmpty() == true
        ) {
            PrintSunmiUtils.printLogoInner(
                prefProvider?.getValue(
                    Constants.VENUE_LOGO,
                    ""
                )!!
            )
        }

        PrintSunmiUtils.printBusinessDetailsInner(
            prefProvider?.getValue(Constants.BUSINESS_NAME, "").toString(),
            prefProvider?.getValue(Constants.BUSINESS_ADDRESS, "").toString(),
            prefProvider?.getValue(Constants.BUSINESS_PHONE_NO, "").toString()
        )
        SunmiPrintHelper.getInstance().lineWrap(1)

        PrintSunmiUtils.headerText("Employee Tips Summary")
        if (prefProvider?.employeeName().toString().isNotEmpty()) {
            PrintSunmiUtils.normalTextCenter("Employee : " + prefProvider?.employeeName())
        }

        PrintSunmiUtils.addHorizontalInner()
        SunmiPrintHelper.getInstance().lineWrap(1)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PrintSunmiUtils.normalText("Print Time:${MethodUtils.formatted()}")
        }
//        SunmiPrintHelper.getInstance().lineWrap(1)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PrintSunmiUtils.normalText("Report Time:${viewModel.startDate.value} To \n${viewModel.endDate.value}")
        }
        SunmiPrintHelper.getInstance().lineWrap(1)

        //Main part start
        if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)
                ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
        ) {
            employeeTipSummaryHeaderNew()
            ETSdataList?.forEach {
                addItemsInEmployeeTipsSummaryInnerPrinterNew(it)
            }
        } else {
            employeeTipSummaryHeader()
            ETSdataList?.forEach {
                addItemsInEmployeeTipsSummaryInnerPrinter(it)
            }
        }
        // Main part end

        SunmiPrintHelper.getInstance().lineWrap(2)


        PrintSunmiUtils.normalText("EMPLOYEE x " + com.pays.pos.utils.repeat("_", 36))
        SunmiPrintHelper.getInstance().lineWrap(1)

        PrintSunmiUtils.normalText("CASH RECEIVED BY" + com.pays.pos.utils.repeat("_", 31))

        PrintSunmiUtils.cutPaperInner()

    }

    private fun createReportFormatETSsunmi() {


        if (prefProvider?.getValue(
                Constants.VENUE_LOGO,
                ""
            )?.isNotEmpty() == true
        ) {
            printBusinessLogo()
        }

        PrintSunmiUtils.printBusinessDetails(
            prefProvider?.getValue(Constants.BUSINESS_NAME, "").toString(),
            prefProvider?.getValue(Constants.BUSINESS_ADDRESS, "").toString(),
            prefProvider?.getValue(Constants.BUSINESS_PHONE_NO, "").toString()
        )

        SunmiPrinterApi.getInstance().lineWrap(1)

        PrintSunmiUtils.addLable("Employee Tips Summary")

        SunmiPrinterApi.getInstance().setAlignMode(1)
        SunmiPrinterApi.getInstance().enableBold(false)
        SunmiPrinterApi.getInstance().setFontZoom(1, 1)
        SunmiPrinterApi.getInstance()
            .printText("Employee : " + prefProvider?.employeeName())
        SunmiPrinterApi.getInstance().lineWrap(1)

        PrintSunmiUtils.addHorizontal()
        SunmiPrinterApi.getInstance().lineWrap(1)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val current = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy hh:mm:a")
            val formatted = current.format(formatter)

            PrintSunmiUtils.orderTime("Print Time:$formatted")
        }
        SunmiPrinterApi.getInstance().lineWrap(1)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PrintSunmiUtils.normalText("Report Time:${viewModel.startDate.value} To \n${viewModel.endDate.value}")
        }

        if (ETSdataList?.isNotEmpty() == true) {
            PrintSunmiUtils.addHorizontal()
            addSixHeaderForEmployeeTipSummarySunmi()
            PrintSunmiUtils.addHorizontal()

            ETSdataList?.forEach {
                addItemsInEmployeeTipsSummary(it)
            }

        }

        SunmiPrinterApi.getInstance().lineWrap(2)



        PrintSunmiUtils.orderTime("EMPLOYEE x " + com.pays.pos.utils.repeat("_", 36))
        SunmiPrinterApi.getInstance().lineWrap(1)

        PrintSunmiUtils.orderTime("CASH RECEIVED BY" + com.pays.pos.utils.repeat("_", 31))

        SunmiPrinterApi.getInstance().lineWrap(5)
        SunmiPrinterApi.getInstance().cutPaper(1, 1)

    }

    private fun printBusinessLogo() {
        val decodedString: ByteArray = Base64.decode(
            prefProvider?.getValue(Constants.VENUE_LOGO, "") ?: "",
            Base64.DEFAULT
        )
        val bitmap: Bitmap =
            BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

        val newBitmap = Bitmap.createScaledBitmap(bitmap, 210, 210, true)

        PrintSunmiUtils.printLogo(newBitmap)

    }

    override fun onStop() {
        super.onStop()
        org.greenrobot.eventbus.EventBus.getDefault().unregister(this)
    }

    override fun onStart() {
        super.onStart()
        org.greenrobot.eventbus.EventBus.getDefault().register(this)
    }
}