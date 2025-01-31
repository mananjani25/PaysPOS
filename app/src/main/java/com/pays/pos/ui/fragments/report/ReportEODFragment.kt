package com.pays.pos.ui.fragments.report

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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.pays.pos.R
import com.pays.pos.databinding.FragmentReportEodBinding
import com.pays.pos.data.entities.Employee
import com.pays.pos.data.model.ClockinOutReportModel
import com.pays.pos.data.model.ShiftRportConfiguration
import com.pays.pos.data.model.responseModel.EodReportResponse
import com.pays.pos.data.model.responseModel.PrinterResponse
import com.pays.pos.data.model.responseModel.report.KeyValue
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.MEDIUM
import com.pays.pos.data.remote.Constants.SMALL
import com.pays.pos.data.remote.Constants.SUNMI_INNER_PRINTER
import com.pays.pos.data.remote.Constants.LANDI_INNER_PRINTER
import com.pays.pos.data.remote.Constants.SUNMI_PRINTER
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.adapter.ClockInClockOutAdapter
import com.pays.pos.ui.adapter.CreditCardBreakDownAdapter
import com.pays.pos.ui.adapter.CreditTipAuditAdapter
import com.pays.pos.ui.adapter.EmployeeGuestDetailsAdapter
import com.pays.pos.ui.adapter.ItemWiseSalesAdapter
import com.pays.pos.ui.adapter.PaymentDetailsAdapter
import com.pays.pos.ui.adapter.SalesOrderDetailsAdapter
import com.pays.pos.ui.adapter.SalesReportAdapter
import com.pays.pos.ui.adapter.ServiceChargeDetailsAdapter
import com.pays.pos.ui.adapter.TerminalAdapter
import com.pays.pos.ui.adapter.boldpos.SalesPerCategorySummary
import com.pays.pos.ui.fragments.loginscreen.PasscodeViewModel
import com.pays.pos.ui.fragments.settings.hardware.printer.BluetoothUtil
import com.pays.pos.ui.fragments.settings.hardware.printer.SunmiPrintHelper
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.EventObserver
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.PrintSunmiUtils
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.addBuilderText
import com.pays.pos.utils.addCreditCardBreakDown
import com.pays.pos.utils.addCreditCardBreakDownData
import com.pays.pos.utils.addCreditCardBreakDownDataInner
import com.pays.pos.utils.addCreditCardBreakDownDataLandiInner
import com.pays.pos.utils.addCreditCardBreakDownInner
import com.pays.pos.utils.addCreditCardBreakDownLandiInner
import com.pays.pos.utils.addCreditTipAuditData
import com.pays.pos.utils.addCreditTipAuditDataInner
import com.pays.pos.utils.addCreditTipAuditDataLandiInner
import com.pays.pos.utils.addCreditTipAuditHeader
import com.pays.pos.utils.addCreditTipAuditHeaderInner
import com.pays.pos.utils.addCreditTipAuditHeaderLandiInner
import com.pays.pos.utils.addCustomerTextSize
import com.pays.pos.utils.addHorizontalLine
import com.pays.pos.utils.addItemWiseSales
import com.pays.pos.utils.addItemWiseSalesHeader
import com.pays.pos.utils.addItemWiseSalesHeaderSunmiInner
import com.pays.pos.utils.addItemWiseSalesSunmiInnerPrinter
import com.pays.pos.utils.addItemWiseSalesLandiInnerPrinter
import com.pays.pos.utils.addItemsInOrderSalesDetails
import com.pays.pos.utils.addItemsInOrderSalesDetailsInner
import com.pays.pos.utils.addPaymentDetailsHeader
import com.pays.pos.utils.addPaymentDetailsHeaderInner
import com.pays.pos.utils.addPaymentDetailsHeaderLandiInnerNew
import com.pays.pos.utils.addPaymentDetailsThreeData
import com.pays.pos.utils.addPaymentDetailsThreeDataInner
import com.pays.pos.utils.addPaymentDetailsThreeDataLandiInner
import com.pays.pos.utils.addPaymentDetailsTwoData
import com.pays.pos.utils.addPaymentDetailsTwoDataInner
import com.pays.pos.utils.addPaymentDetailsTwoDataLandiInner
import com.pays.pos.utils.addRefundVoidsMultiple
import com.pays.pos.utils.addRefundVoidsMultipleInner
import com.pays.pos.utils.addRefundVoidsMultipleLandiInner
import com.pays.pos.utils.addSixHeaderForOrderSaleDetails
import com.pays.pos.utils.addSixHeaderForOrderSaleDetailsSunmi
import com.pays.pos.utils.addSixHeaderForOrderSaleDetailsSunmiInner
import com.pays.pos.utils.employeeGuestDetailsData
import com.pays.pos.utils.employeeGuestDetailsDataInner
import com.pays.pos.utils.employeeGuestDetailsDataLandiInner
import com.pays.pos.utils.extensions.alert
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.liveSnackBar
import com.pays.pos.utils.extensions.printLog
import com.pays.pos.utils.extensions.runOnUiThread
import com.pays.pos.utils.extensions.showAlert
import com.pays.pos.utils.extensions.visible
import com.pays.pos.utils.itemWiseSalesM30Print
import com.pays.pos.utils.padLine
import com.pays.pos.utils.printer.PrinterClass
import com.pays.pos.utils.repeat
import com.pays.pos.utils.statusUtils.Status
import com.epson.eposprint.Builder
import com.epson.eposprint.Print
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.pays.pos.data.remote.Constants.BUSINESS_ADDRESS
import com.pays.pos.data.remote.Constants.BUSINESS_PHONE_NO
import com.pays.pos.data.remote.Constants.getCurrentTimeFromTimeZone
import com.pays.pos.ui.adapter.ExternalPaymentDetailsAdapter
import com.pays.pos.ui.fragments.payment.OrderCompleteFragment.OnBluetoothPermissionGranted
import com.pays.pos.utils.addCreditCardBreakDownDataInnerNew
import com.pays.pos.utils.addCreditCardBreakDownInnerNew
import com.pays.pos.utils.addCreditTipAuditDataInnerNew
import com.pays.pos.utils.addCreditTipAuditHeaderInnerNew
import com.pays.pos.utils.addItemWiseSalesHeaderSunmiInnerNew
import com.pays.pos.utils.addItemWiseSalesHeaderLandiInner
import com.pays.pos.utils.addItemWiseSalesSunmiInnerPrinterNew
import com.pays.pos.utils.addItemsInOrderSalesDetailsInnerNew
import com.pays.pos.utils.addItemsInOrderSalesDetailsLandiInner
import com.pays.pos.utils.addPaymentDetailsHeaderLandiInner
import com.pays.pos.utils.addPaymentDetailsHeaderInnerNew
import com.pays.pos.utils.addPaymentDetailsThreeDataInnerNew
import com.pays.pos.utils.addPaymentDetailsTwoDataInnerNew
import com.pays.pos.utils.addRefundVoidsMultipleInnerNew
import com.pays.pos.utils.addSixHeaderForOrderSaleDetailsSunmiInnerNew
import com.pays.pos.utils.addSixHeaderForOrderSaleDetailsLandiInner
import com.pays.pos.utils.employeeGuestDetailsDataInnerNew
import com.pays.pos.utils.landi.LPrint
import com.sunmi.externalprinterlibrary.api.ConnectCallback
import com.sunmi.externalprinterlibrary.api.SunmiPrinter
import com.sunmi.externalprinterlibrary.api.SunmiPrinterApi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class ReportEODFragment(var showHeader: Boolean = true) : Fragment(),
    AdapterView.OnItemSelectedListener {

    private var eodReportConfiguration: ShiftRportConfiguration? = null
    var eodReportData: EodReportResponse.Data? = null
    private var shiftReportsSettingModel: ShiftRportConfiguration? = null
    private var defaultEmployeePos: Int = 0
    private lateinit var binding: FragmentReportEodBinding
    private val viewModel by viewModels<ReportEODViewModel>()
    private val viewModelClockOut by viewModels<PasscodeViewModel>()
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
    private val itemWiseSalesAdapter by lazy { ItemWiseSalesAdapter() }
    private val saleCategorySummaryAdapter by lazy { SalesPerCategorySummary() }
    private val externalPaymentsAdapter by lazy { ExternalPaymentDetailsAdapter() }


    private val salesOrderDetailsAdapter by lazy { SalesOrderDetailsAdapter() }

    private val clockInClockOutAdapter by lazy { ClockInClockOutAdapter() }

    private val creditCardBreakdownAdapter by lazy { CreditCardBreakDownAdapter(hideRefund = false) }

    private var teamEmployeeListGlobal: ArrayList<Employee> = arrayListOf()

    val myCalendar = Calendar.getInstance()
    val myCalendar1 = Calendar.getInstance()

    val myCalendar2 = Calendar.getInstance()
    val myCalendar3 = Calendar.getInstance()

    private var sunmiFrameworkVersion: Array<String>? = null //Fetching Sunmi OS version to format printing.

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
        //        3.3.39
        sunmiFrameworkVersion =
            prefProvider?.getValue(Constants.SUNMI_FRAMEWORK_VERSION, "").toString().split(".")
                .toTypedArray()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (showHeader) {
            binding.rlHeader.visibility = View.VISIBLE
        } else {
            binding.rlHeader.visibility = View.GONE
        }
        try {
            eodReportSettings()
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


                sendEmail()


                //  viewModel.getReportSummary("")
            }

            setFragmentResultListener("request_key_eod") { _: String, bundle: Bundle ->


                bundle.getString("email")?.let { viewModel.getReportSummary(it) }
            }

            binding.txtClockOut.setOnClickListener {

                if (MethodUtils.isDoubleClick()) return@setOnClickListener
                alert(
                    getString(R.string.app_name),
                    getString(R.string.clockout_message)
                ) {
                    positiveButton(getString(android.R.string.ok)) {
                        //  viewModelClockOut.submit()

//                       viewModel.clockOut()
                        val bundle = Bundle()
                        bundle.putBoolean("isDashboard", true)
                        bundle.putBoolean("isSwap", false)
                        if (findNavController().currentDestination?.id == R.id.reportEODFragment) {
                            findNavController().navigate(
                                R.id.action_reportEODFragment_to_passcode,
                                bundle
                            )
                        }


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

    private fun sendEmail() {
        if (viewModel.selectedTerminalId.isNotEmpty()) {
            viewModel.getEmployeeEmail(viewModel.selectedTerminalId.toInt())
                .observe(viewLifecycleOwner) {

                    if (it.status == Status.SUCCESS) {
                        val bundle = Bundle()
                        bundle.putBoolean("EOD", true)
                        bundle.putInt("type", 2)
                        bundle.putString("email", it.data?.email)
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
            bundle.putString("email", "")
            findNavController().navigate(
                R.id.action_reports_to_sendReceiptFragment,
                bundle
            )

        }
    }

    private fun eodReportSettings() {
        viewModel.getEODReportSettings().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    eodReportConfiguration = it.data

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

    private fun generateEODReport() {
        customerList.forEach {
            if (it.status) {
                initPrinter(it)
            }
        }
    }

    private fun generateLANDIEODReport() {
        customerList.forEach {
            if (it.status) {
                printFromLandiInnerPrinter(true, it)
            }
        }
    }

    private fun initPrinter(customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters) {
        Log.d("BIS-685", "initPrinter: Called")

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
                            createReportFormatEODSunmi(customerReceiptPrinters)

                        }

                        override fun onDisconnect() {
                            println("onDisconnect")
                        }

                    })
            } else {
                createReportFormatEODSunmi(customerReceiptPrinters)
            }

        } else if (customerReceiptPrinters.name.startsWith(SUNMI_INNER_PRINTER, true)) {

            SunmiPrintHelper.getInstance().initSunmiPrinterService(requireContext())
            viewLifecycleOwner.lifecycleScope.launch {
                delay(100)
                setService(customerReceiptPrinters)
            }


        } else if (customerReceiptPrinters.name.startsWith(LANDI_INNER_PRINTER, true)) {


            viewLifecycleOwner.lifecycleScope.launch {
                delay(100)
                printFromLandiInnerPrinter(true, customerReceiptPrinters)
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
                        LogUtil.logE(TAG, "customerReceiptPrinters.ipAddress: " + customerReceiptPrinters.ipAddress)
                        LogUtil.logE(TAG, "PrinterException: " + e)
                        printer = null
                        return@launch
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
        }
    }

    interface OnBluetoothPermissionGranted {
        fun onPermissionsGranted()
    }

    var onBluetoothPermissionGranted: OnBluetoothPermissionGranted? = null

    fun checkBluetoothPermissions(onBluetoothPermissionGranted: OnBluetoothPermissionGranted) {
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

    private fun printFromLandiInnerPrinter(
        isAutoPrint: Boolean,
        customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters
    ) {

        this.checkBluetoothPermissions(object : OnBluetoothPermissionGranted {
            override fun onPermissionsGranted() {
                GlobalScope.launch {
                    LPrint.connectLandiInnerPrinter(customerReceiptPrinters.macAddress)
                        ?.let { outputStream ->
                            LPrint.apply {
                                setOutputStream(outputStream)

                                try {
                                    Log.d("BIS-685", "createReportFormatEODLandiInner: Called")

                                    prefProvider?.getValue(Constants.VENUE_LOGO,"")
                                        ?.let { printLogoLandiInner(it) }

                                    printCenter(
                                        prefProvider?.getValue(
                                            Constants.BUSINESS_NAME,
                                            ""
                                        )!!,
                                        fontSize = FONT_SIZE_5X,
                                        isBold = true,
                                        printOnNewLine = true
                                    )

                                    printCenter(
                                        prefProvider?.getValue(
                                            Constants.BUSINESS_ADDRESS,
                                            ""
                                        )!!,
                                        fontSize = NORMAL_SIZE,
                                        isBold = true,
                                        printOnNewLine = true
                                    )

                                     printCenter(
                                        MethodUtils.getUSFormatNumber(
                                            prefProvider?.getValue(
                                                Constants.BUSINESS_PHONE_NO,
                                                ""
                                            )!!
                                        ).toString(),
                                            fontSize = NORMAL_SIZE,
                                            isBold = true,
                                            printOnNewLine = true
                                    )

                                    printCenter(
                                        "Employee End of Day Report",
                                        fontSize = MEDIUM_SIZE,
                                        isBold = true,
                                        printOnNewLine = true
                                    )

                                    if (binding.spTerminals.selectedItem.toString().isNotEmpty()) {
                                        printCenter(
                                            "Employee : " + binding.spTerminals.selectedItem.toString(),
                                            fontSize = FONT_SIZE_4X,
                                            isBold = true,
                                            printOnNewLine = true
                                        )
                                        lineBreak()
                                    } else {

                                        printCenter(
                                            "Employee : " + prefProvider?.getValue(
                                                Constants.EMPLOYEE_NAME,
                                                ""
                                            )!!,
                                            fontSize = NORMAL_SIZE,
                                            isBold = true,
                                            printOnNewLine = true
                                        )
                                        lineBreak()
                                    }

                                    printDashedLineAndBreak()

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                                        lineBreak()

                                        printLeft(
                                            "Print Time : ${
                                                getCurrentTimeFromTimeZone(
                                                    requireContext(),
                                                    MethodUtils.formatted()
                                                )
                                            }"
                                        )
                                    }
                                    lineBreak()

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        printLeft(
                                            "Employee Report:" + eodReportData?.reportTime

                                        )
                                    }
                                    lineBreak()

                                    if (eodReportData?.orderSalesDetails?.data?.isNotEmpty() == true && eodReportConfiguration?.orderSalesDetails == true) {

                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            printCenter(
                                                "ORDER SALES DETAILS",
                                                fontSize = FONT_SIZE_4X,
                                                isBold = true,
                                                printOnNewLine = true
                                            )

                                            lineBreak()

                                            addSixHeaderForOrderSaleDetailsLandiInner()
                                            lineBreak()

                                            printDashedLineAndBreak()
                                            eodReportData?.orderSalesDetails?.data?.forEach {
                                                addItemsInOrderSalesDetailsLandiInner(it)
                                                lineBreak()
                                            }

                                            var totalAmount = 0.0

                                            eodReportData?.orderSalesDetails?.data?.forEach {

                                                totalAmount += it.amount
                                            }
                                            printDashedLineAndBreak()
                                            LPrint.print("Total                                  ${MethodUtils.roundOffAmount(totalAmount)}")
                                        }
                                        lineBreak()
                                    }

                                    if (eodReportData?.salesSummary?.isNotEmpty() == true && eodReportConfiguration?.salesSummary == true) {

                                        printCenter(
                                            "SALES SUMMARY",
                                            fontSize = FONT_SIZE_4X,
                                            isBold = true,
                                            printOnNewLine = true
                                        )
                                        printDashedLineAndBreak()
                                        lineBreak()

                                        eodReportData?.salesSummary?.forEach {


                                        print(
                                            padLine(
                                                it.key,
                                                it.showData(),
                                                48
                                            ).toString()
                                        )

                                    }
                                        lineBreak()

                                    if (eodReportData?.salesAndTaxesSummary?.isNotEmpty() == true && eodReportConfiguration?.salesAndTaxSummary == true) {

                                        printCenter(
                                            "SALES AND TAXES SUMMARY",
                                            fontSize = FONT_SIZE_4X,
                                            isBold = true,
                                            printOnNewLine = true
                                        )
                                        lineBreak()

                                        print(
                                            padLine(
                                                "Category(Quantity)",
                                                "Amount",
                                                48
                                            ).toString()
                                        )

                                        printDashedLineAndBreak()

                                        eodReportData?.salesAndTaxesSummary?.forEach {

                                            print(
                                                padLine(
                                                    it.key,
                                                    it.showData(),
                                                    48
                                                ).toString()
                                            )
                                        }

                                        lineBreak()
                                    }

                                    if (eodReportData?.itemWiseSales?.isNotEmpty() == true && eodReportConfiguration?.isItemWiseSales == true){

                                        printCenter(
                                            "ITEM WISE SALES",
                                            fontSize = FONT_SIZE_4X,
                                            isBold = true,
                                            printOnNewLine = true
                                        )
                                        lineBreak()

                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            addItemWiseSalesHeaderLandiInner()
                                            printDashedLineAndBreak()
                                            eodReportData?.itemWiseSales?.forEach {
                                                addItemWiseSalesLandiInnerPrinter(it)
                                            }


                                        }
                                        lineBreak()
                                    }

                                    if (eodReportData?.paymentDetails?.isNotEmpty() == true && eodReportConfiguration?.paymentDetails == true) {

                                        printCenter(
                                            "PAYMENT DETAILS",
                                            fontSize = FONT_SIZE_4X,
                                            isBold = true,
                                            printOnNewLine = true
                                        )
                                        lineBreak()

                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                                            addPaymentDetailsHeaderLandiInner()
                                            printDashedLineAndBreak()


                                            eodReportData?.paymentDetails?.forEach {

                                                if (it.size == 2) {


                                                    addPaymentDetailsThreeDataLandiInner(it)

                                                } else if (it.size == 1) {
                                                    it.forEach {
                                                        addPaymentDetailsTwoDataLandiInner(it)
                                                    }
                                                }
                                            }
                                        }

                                        lineBreak()
                                    }

                                    if (eodReportData?.tipDetails?.isNotEmpty() == true && eodReportConfiguration?.tipsDetails == true) {

                                        printCenter(
                                            "TIPS DETAILS",
                                            fontSize = FONT_SIZE_4X,
                                            isBold = true,
                                            printOnNewLine = true
                                        )
                                        lineBreak()

                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                                            addPaymentDetailsHeaderLandiInnerNew()
                                            printDashedLineAndBreak()

                                            eodReportData?.tipDetails?.forEach {

                                                if (it.size == 2) {
                                                    addPaymentDetailsThreeDataLandiInner(it)

                                                } else if (it.size == 1) {
                                                    it.forEach {
                                                        addPaymentDetailsTwoDataLandiInner(it)
                                                    }
                                                }
                                            }
                                        }
                                        lineBreak()
                                    }

                                    if (eodReportData?.taxDetails?.isNotEmpty() == true && eodReportConfiguration?.taxDetails == true) {

                                        printCenter(
                                            "TAX DETAILS",
                                            fontSize = FONT_SIZE_4X,
                                            isBold = true,
                                            printOnNewLine = true
                                        )
                                        lineBreak()

                                        eodReportData?.taxDetails?.forEach {

                                            printLeft(
                                                padLine(
                                                    it.key,
                                                    it.showData(),
                                                    48
                                                ).toString()
                                            )
                                        }

                                        lineBreak()
                                    }

                                    if (eodReportData?.refundAndVoidDetails?.isNotEmpty() == true && eodReportConfiguration?.refundOrVoids == true) {

                                        printCenter(
                                            "REFUNDS/VOIDS",
                                            fontSize = FONT_SIZE_4X,
                                            isBold = true,
                                            printOnNewLine = true
                                        )
                                        lineBreak()

                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            print(
                                                padLine(
                                                    "Order Id(Employee Name)",
                                                    "Amount",
                                                    48
                                                ).toString()
                                            )

                                            printDashedLineAndBreak()

                                            eodReportData?.refundAndVoidDetails?.forEach {

                                                if (it.size > 1) {
                                                    addRefundVoidsMultipleLandiInner(it)
                                                } else if (it.size == 1) {
                                                    it.forEach {
                                                        addPaymentDetailsTwoDataLandiInner(it)
                                                    }
                                                }
                                            }
                                        }
                                        lineBreak()
                                    }

                                    if (eodReportData?.refundDetails?.isNotEmpty() == true && eodReportConfiguration?.refundDetails == true) {

                                        printCenter(
                                            "REFUND DETAILS",
                                            fontSize = FONT_SIZE_4X,
                                            isBold = true,
                                            printOnNewLine = true
                                        )
                                        lineBreak()

                                        eodReportData?.refundDetails?.forEach {

                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                print(
                                                    padLine(
                                                        it.key,
                                                        it.showData(),
                                                        48
                                                    ).toString()
                                                )
                                            }
                                        }
                                        lineBreak()
                                    }

                                    if (eodReportData?.discountDetails?.isNotEmpty() == true && eodReportConfiguration?.discountDetails == true) {

                                        printCenter(
                                            "DISCOUNT DETAILS",
                                            fontSize = FONT_SIZE_4X,
                                            isBold = true,
                                            printOnNewLine = true
                                        )
                                        lineBreak()

                                        eodReportData?.discountDetails?.forEach {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                print(
                                                    padLine(
                                                        it.key,
                                                        it.showData(),
                                                        48
                                                    ).toString()
                                                )
                                            }
                                        }
                                        lineBreak()
                                    }

                                    if (eodReportData?.totalCreditPaymentDetails?.isNotEmpty() == true && eodReportConfiguration?.totalCreditPayments == true) {

                                        printCenter(
                                            "TOTAL CREDIT PAYMENT",
                                            fontSize = FONT_SIZE_4X,
                                            isBold = true,
                                            printOnNewLine = true
                                        )
                                        lineBreak()

                                        eodReportData?.totalCreditPaymentDetails?.forEach {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                print(
                                                    padLine(
                                                        it.key,
                                                        it.showData(),
                                                        48
                                                    ).toString()
                                                )
                                            }
                                        }
                                        lineBreak()
                                    }

                                    if (eodReportData?.totalCashPayments?.isNotEmpty() == true && eodReportConfiguration?.totalCashPayments == true) {

                                        printCenter(
                                            "TOTAL CASH PAYMENT",
                                            fontSize = FONT_SIZE_4X,
                                            isBold = true,
                                            printOnNewLine = true
                                        )
                                        lineBreak()

                                        eodReportData?.totalCashPayments?.forEach {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                print(
                                                    padLine(
                                                        it.key,
                                                        it.showData(),
                                                        48
                                                    ).toString()
                                                )
                                            }
                                        }
                                        lineBreak()
                                    }

                                    if (eodReportData?.externalPayments?.isNotEmpty() == true && eodReportConfiguration?.totalCashPayments == true) {

                                        printCenter(
                                            "TOTAL EXTERNAL PAYMENT",
                                            fontSize = FONT_SIZE_4X,
                                            isBold = true,
                                            printOnNewLine = true
                                        )
                                        lineBreak()

                                        eodReportData?.externalPayments?.forEach { report ->
                                            report.forEach {
                                                if(it.key?.contains("Name", true) == true){
                                                    printDashedLineAndBreak()
                                                }

                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                    print(
                                                        padLine(
                                                            it.key,
                                                            it.showData(),
                                                            48
                                                        ).toString()
                                                    )
                                                    if(it.key?.contains("Name", true) == true){
                                                        printDashedLineAndBreak()
                                                    }

                                                }
                                            }
                                        }
                                        lineBreak()
                                    }

                                    if (eodReportData?.totalPayments?.isNotEmpty() == true && eodReportConfiguration?.totalPayments == true) {

                                        printCenter(
                                            "TOTAL PAYMENTS",
                                            fontSize = FONT_SIZE_4X,
                                            isBold = true,
                                            printOnNewLine = true
                                        )
                                        lineBreak()

                                        eodReportData?.totalPayments?.forEach {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                print(
                                                    padLine(
                                                        it.key,
                                                        it.showData(),
                                                        48
                                                    ).toString()
                                                )
                                            }
                                        }
                                        lineBreak()
                                    }

                                    if (eodReportData?.creditCardBreakdown?.isNotEmpty() == true && eodReportConfiguration?.creditCardBreakdown == true) {

                                        printCenter(
                                            "CREDIT CARD BREAKDOWN",
                                            fontSize = FONT_SIZE_4X,
                                            isBold = true,
                                            printOnNewLine = true
                                        )
                                        lineBreak()

                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            addCreditCardBreakDownLandiInner()

                                            printDashedLineAndBreak()

                                            eodReportData?.creditCardBreakdown?.forEach {

                                                addCreditCardBreakDownDataLandiInner(it)
                                            }
                                        }
                                        lineBreak()
                                    }

                                    if (eodReportData?.serviceChargeDetails?.isNotEmpty() == true && eodReportConfiguration?.serviceChargeDetails == true) {

                                        printCenter(
                                            "SERVICE CHARGE DETAILS",
                                            fontSize = FONT_SIZE_4X,
                                            isBold = true,
                                            printOnNewLine = true
                                        )
                                        lineBreak()

                                        eodReportData?.serviceChargeDetails?.forEach {

                                            it.forEach {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                    addPaymentDetailsTwoDataLandiInner(it)
                                                }
                                            }
                                        }
                                        lineBreak()
                                    }
                                    if (eodReportData?.creditTipAudit?.isNotEmpty() == true && eodReportConfiguration?.creditTipAudit == true) {

                                        printCenter(
                                            "CREDIT TIP AUDIT",
                                            fontSize = FONT_SIZE_4X,
                                            isBold = true,
                                            printOnNewLine = true
                                        )
                                        lineBreak()

                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            addCreditTipAuditHeaderLandiInner()
                                            lineBreak()
                                            printDashedLineAndBreak()


                                            eodReportData?.creditTipAudit?.forEach { it ->
                                                var fPArt = ""
                                                var sPart = ""
                                                var lPart = ""
                                                var tPArt = ""

                                                it.forEach {

                                                    if (it.key?.contains(
                                                            "Subtotal",
                                                            true
                                                        ) == true
                                                    ) {
                                                        fPArt = MethodUtils.roundOffAmount(
                                                            it.value?.toDouble() ?: 0.0
                                                        )
                                                    } else if (it.key?.contains(
                                                            "Tip",
                                                            true
                                                        ) == true
                                                    ) {
                                                        sPart = MethodUtils.roundOffAmount(
                                                            it.value?.toDouble() ?: 0.0
                                                        )
                                                    } else if (it.key?.contains(
                                                            "Total",
                                                            true
                                                        ) == true
                                                    ) {
                                                        lPart = MethodUtils.roundOffAmount(
                                                            it.value?.toDouble() ?: 0.0
                                                        )
                                                    } else if (it.key?.contains(
                                                            "Payment Id",
                                                            true
                                                        ) == true
                                                    ) {
                                                        tPArt = it.value.toString()
                                                    }

                                                }

                                                addCreditTipAuditDataLandiInner(
                                                    fPArt,
                                                    sPart,
                                                    tPArt,
                                                    lPart
                                                )
                                                lineBreak()

                                            }
                                        }
                                        lineBreak()
                                    }

                                    if (eodReportData?.employeeGuestDetails?.isNotEmpty() == true && eodReportConfiguration?.employeeGuestReport == true) {

                                        printCenter(
                                            "EMPLOYEE GUEST DETAILS",
                                            fontSize = FONT_SIZE_4X,
                                            isBold = true,
                                            printOnNewLine = true
                                        )
                                        lineBreak()

                                        eodReportData?.employeeGuestDetails?.forEach {
                                            it.forEach {

                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                    employeeGuestDetailsDataLandiInner(it)
                                                }
                                            }
                                        }
                                        lineBreak()
                                    }

                                    if (eodReportData?.salesPerCategorySummary?.isNotEmpty() == true && eodReportConfiguration?.cashCreditPerSalesCategorySummary == true) {

                                        printCenter(
                                            "SALES PER CATEGORY SUMMARY\n(MIXED-PAYMENT ORDER ITEMS NOT INCLUDED)",
                                            fontSize = FONT_SIZE_4X,
                                            isBold = true,
                                            printOnNewLine = true
                                        )
                                        lineBreak()

                                        eodReportData?.salesPerCategorySummary?.forEachIndexed { index, arrayList ->
                                            if (index == 0) {
                                                printCenter("Cash Sales")
                                                lineBreak()
                                                printDashedLineAndBreak()
                                                lineBreak()
                                                arrayList.forEach {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                        addPaymentDetailsTwoDataLandiInner(it)
                                                    }
                                                }
                                                lineBreak()

                                                printDashedLineAndBreak()
                                                lineBreak()
                                                print("\n")

                                            } else if (index == 1) {
                                                printCenter("Credit/Non Cash Sales")
                                                lineBreak()
                                                printDashedLineAndBreak()
                                                lineBreak()

                                                arrayList.forEach {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                        addPaymentDetailsTwoDataLandiInner(it)
                                                    }
                                                }
                                            }
                                        }
                                        lineBreak()
                                    }

                                    // Clock in-out report
                                    if (eodReportData?.clockInClockOut?.isNotEmpty() == true && eodReportConfiguration?.clockInOut == true) {

                                        printCenter(
                                            "CLOCK IN-CLOCK OUT",
                                            fontSize = FONT_SIZE_4X,
                                            isBold = true,
                                            printOnNewLine = true
                                        )
                                        lineBreak()

                                        eodReportData?.clockInClockOut?.forEach {
                                            it.forEach { data ->
                                                if (data.key != "Total") {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                        print(
                                                            padLine(
                                                                if (data.key == "Total Working Hour") {
                                                                    "Total"
                                                                } else {
                                                                    data.key
                                                                },
                                                                data.value.toString(),
                                                                48
                                                            ).toString()
                                                        )
                                                    }
                                                }
                                            }
                                            lineBreak()
                                        }
                                    }

                                    if (eodReportData?.cashLogDetails?.isNotEmpty() == true && eodReportConfiguration?.cashLogDetails == true) {

                                        printCenter(
                                            "CASH LOG DETAILS",
                                            fontSize = FONT_SIZE_4X,
                                            isBold = true,
                                            printOnNewLine = true
                                        )
                                        lineBreak()

                                        eodReportData?.cashLogDetails?.forEach {

                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                addPaymentDetailsTwoDataLandiInner(it)
                                            }
                                        }
                                        lineBreak()
                                    }

                                    if (eodReportData?.otherDetails?.isNotEmpty() == true && eodReportConfiguration?.otherDetails == true) {

                                        printCenter(
                                            "OTHER DETAILS",
                                            fontSize = FONT_SIZE_4X,
                                            isBold = true,
                                            printOnNewLine = true
                                        )
                                        lineBreak()

                                        eodReportData?.otherDetails?.forEach {

                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                addPaymentDetailsTwoDataLandiInner(it)
                                            }

                                        }

                                    }
                                        lineBreak()
                                        lineBreak()

                                        print("EMPLOYEE x " + repeat("_", 36))
                                        lineBreak()

                                        print("CASH RECEIVED BY" + repeat("_", 31))

                                        lineBreak()
                                        lineBreak()
                                        lineBreak()
                                        lineBreak()

                                        paperCut()
                                }
                                    }catch (e: java.lang.Exception) {
                                    e.printStackTrace()
                                    }
                                }
                            }
                    }
                }
            })
    }


    private fun setService(customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters) {
        if (SunmiPrintHelper.getInstance().sunmiPrinter == SunmiPrintHelper.FoundSunmiPrinter) {

            LogUtil.logE("SunmiPrintHelper", "FoundSunmiPrinter")

            if (!BluetoothUtil.isBlueToothPrinter) {
                createReportFormatEODSunmiInner(customerReceiptPrinters)
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

    private fun printBusinessLogo() {
        val decodedString: ByteArray = Base64.decode(
            prefProvider?.getValue(Constants.VENUE_LOGO, "") ?: "",
            Base64.DEFAULT
        )
        val bitmap: Bitmap =
            BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

        val newBitmap = Bitmap.createScaledBitmap(bitmap!!, 210, 210, true)

        PrintSunmiUtils.printLogo(newBitmap)

    }


    override fun onStart() {
        super.onStart()

        org.greenrobot.eventbus.EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        org.greenrobot.eventbus.EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: String?) {
        // Do something
        if (event.equals("1")) {
            sendEmail()
        } else if (event.equals("2")) {
            generateEODReport()
        } else {
            if (event != null) {
                viewModel.getReportSummary(event)
            }
        }

    }

    private fun createReportFormatEOD(customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters) {
        Log.d("BIS-685", "createReportFormatEOD: Called")
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
                addCustomerTextSize(builder, SMALL)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                builder.addText("Print Time:" + formatted)
            }

            builder.addFeedLine(1)
            builder.addTextLineSpace(30)
            builder.addFeedUnit(30)
            builder.addTextFont(Builder.FONT_E)
            builder.addTextAlign(Builder.ALIGN_LEFT)
            builder.addTextLang(Builder.LANG_EN)
            addCustomerTextSize(builder, SMALL)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.FALSE,
                Builder.COLOR_1
            )
            builder.addText("Employee Report:" + eodReportData?.reportTime)


            if (eodReportData?.orderSalesDetails?.data?.isNotEmpty() == true && eodReportConfiguration?.orderSalesDetails == true) {
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

                addSixHeaderForOrderSaleDetails(builder)
                builder.addFeedLine(1)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                addCustomerTextSize(builder, SMALL)
                addHorizontalLine(builder)

                eodReportData?.orderSalesDetails?.data?.forEach {
                    addItemsInOrderSalesDetails(builder, it)
                }


            }

            if (eodReportData?.salesSummary?.isNotEmpty() == true && eodReportConfiguration?.salesSummary == true) {
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
                builder.addText("SALES SUMMARY")
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
                            it.showData(),
                            48
                        )
                    )
                }

            }

            if (eodReportData?.salesAndTaxesSummary?.isNotEmpty() == true && eodReportConfiguration?.salesAndTaxSummary == true) {
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
                builder.addText(padLine("Category(Quantity)", "Amount", 48))
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
                            if (it.value?.isNotEmpty() == true) MethodUtils.roundOffAmount(
                                it.value.toString().toDouble()
                            ) else "$0.00",
                            48
                        )
                    )
                }

            }

            if (eodReportData?.paymentDetails?.isNotEmpty() == true && eodReportConfiguration?.paymentDetails == true) {
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
                builder.addText("PAYMENT DETAILS")

                builder.addFeedLine(2)
                addCustomerTextSize(builder, SMALL)
                addHorizontalLine(builder)

                addPaymentDetailsHeader(builder)
                builder.addFeedLine(1)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                addCustomerTextSize(builder, SMALL)
                addHorizontalLine(builder)

                eodReportData?.paymentDetails?.forEach {

                    if (it.size == 2) {


                        addPaymentDetailsThreeData(builder, it)

                    } else if (it.size == 1) {
                        it.forEach {
                            addPaymentDetailsTwoData(builder, it)
                        }
                    }
                }


            }

            if (eodReportData?.itemWiseSales?.isNotEmpty() == true && eodReportConfiguration?.isItemWiseSales == true) {
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
                builder.addText("ITEM WISE SALES")

                builder.addFeedLine(2)
                addCustomerTextSize(builder, SMALL)
                addHorizontalLine(builder)

                addItemWiseSalesHeader(builder)

                builder.addFeedLine(1)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                addCustomerTextSize(builder, SMALL)
                addHorizontalLine(builder)

                eodReportData?.itemWiseSales?.forEach {
                    itemWiseSalesM30Print(it,builder)

                }

            }

            if (eodReportData?.tipDetails?.isNotEmpty() == true && eodReportConfiguration?.tipsDetails == true) {
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
                builder.addText("TIPS DETAILS")

                builder.addFeedLine(2)
                addCustomerTextSize(builder, SMALL)
                addHorizontalLine(builder)

                addPaymentDetailsHeader(builder)
                builder.addFeedLine(1)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                addCustomerTextSize(builder, SMALL)
                addHorizontalLine(builder)

                eodReportData?.tipDetails?.forEach {

                    if (it.size == 2) {
                        addPaymentDetailsThreeData(builder, it)

                    } else if (it.size == 1) {
                        it.forEach {
                            addPaymentDetailsTwoData(builder, it)
                        }
                    }
                }


            }

            if (eodReportData?.taxDetails?.isNotEmpty() == true && eodReportConfiguration?.taxDetails == true) {
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
                builder.addText("TAX DETAILS")
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

                eodReportData?.taxDetails?.forEach {

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
                            it.showData(),
                            48
                        )
                    )
                }

            }

            if (eodReportData?.refundAndVoidDetails?.isNotEmpty() == true && eodReportConfiguration?.refundOrVoids == true) {
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
                builder.addText("REFUNDS/VOIDS")
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
                builder.addText(padLine("Order Id(Employee Name)", "Amount", 48))
                builder.addFeedLine(1)
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

                eodReportData?.refundAndVoidDetails?.forEach {

                    if (it.size > 1) {
                        addRefundVoidsMultiple(builder, it)
                    } else if (it.size == 1) {
                        it.forEach {
                            addPaymentDetailsTwoData(builder, it)
                        }

                    }
                }

            }


            if (eodReportData?.refundDetails?.isNotEmpty() == true && eodReportConfiguration?.refundDetails == true) {
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
                builder.addText("REFUND DETAILS")
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

                eodReportData?.refundDetails?.forEach {

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
                            it.showData(),
                            48
                        )
                    )
                }

            }

            if (eodReportData?.discountDetails?.isNotEmpty() == true && eodReportConfiguration?.discountDetails == true) {
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
                builder.addText("DISCOUNT DETAILS")
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

                eodReportData?.discountDetails?.forEach {

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
                            it.showData(),
                            48
                        )
                    )
                }

            }

            if (eodReportData?.totalCreditPaymentDetails?.isNotEmpty() == true && eodReportConfiguration?.totalCreditPayments == true) {
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
                builder.addText("TOTAL CREDIT PAYMENT")
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

                eodReportData?.totalCreditPaymentDetails?.forEach {

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
                            it.showData(),
                            48
                        )
                    )
                }

            }
            if (eodReportData?.totalCashPayments?.isNotEmpty() == true && eodReportConfiguration?.totalCashPayments == true) {
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
                builder.addText("TOTAL CASH PAYMENT")
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

                eodReportData?.totalCashPayments?.forEach {

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
                            it.showData(),
                            48
                        )
                    )
                }

            }
            if (eodReportData?.totalPayments?.isNotEmpty() == true && eodReportConfiguration?.totalPayments == true) {
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
                builder.addText("TOTAL PAYMENTS")
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

                eodReportData?.totalPayments?.forEach {

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
                            it.showData(),
                            48
                        )
                    )
                }

            }

            if (eodReportData?.creditCardBreakdown?.isNotEmpty() == true && eodReportConfiguration?.creditCardBreakdown == true) {
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
                builder.addText("CREDIT CARD BREAKDOWN")

                builder.addFeedLine(2)
                addCustomerTextSize(builder, SMALL)
                addHorizontalLine(builder)

                addCreditCardBreakDown(builder)
                builder.addFeedLine(1)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                addCustomerTextSize(builder, SMALL)
                addHorizontalLine(builder)

                eodReportData?.creditCardBreakdown?.forEach {

                    addCreditCardBreakDownData(builder, it)
                }


            }

            if (eodReportData?.serviceChargeDetails?.isNotEmpty() == true && eodReportConfiguration?.serviceChargeDetails == true) {
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
                builder.addText("SERVICE CHARGE DETAILS")

                builder.addFeedLine(2)
                addCustomerTextSize(builder, SMALL)
                addHorizontalLine(builder)


                eodReportData?.serviceChargeDetails?.forEach {

                    it.forEach {
                        addPaymentDetailsTwoData(builder, it)
                    }
                }


            }
            if (eodReportData?.creditTipAudit?.isNotEmpty() == true && eodReportConfiguration?.creditTipAudit == true) {
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
                builder.addText("CREDIT TIP AUDIT")

                builder.addFeedLine(2)
                addCustomerTextSize(builder, SMALL)
                addHorizontalLine(builder)

                addCreditTipAuditHeader(builder)
                builder.addFeedLine(1)
                builder.addTextStyle(
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.FALSE,
                    Builder.COLOR_1
                )
                addCustomerTextSize(builder, SMALL)
                addHorizontalLine(builder)

                eodReportData?.creditTipAudit?.forEach {
                    var FPArt = ""
                    var SPart = ""
                    var LPart = ""
                    var TPArt = ""

                    it.forEach {


                        if (it.key?.contains("Subtotal", true) == true) {
                            FPArt = MethodUtils.roundOffAmount(it.value?.toDouble() ?: 0.0)
                        } else if (it.key?.contains("Tip", true) == true) {
                            SPart = MethodUtils.roundOffAmount(it.value?.toDouble() ?: 0.0)
                        } else if (it.key?.contains("Total", true) == true) {
                            LPart = MethodUtils.roundOffAmount(it.value?.toDouble() ?: 0.0)
                        } else if (it.key?.contains("Payment Id", true) == true) {
                            TPArt = it.value.toString()
                        }

                    }
                    addCreditTipAuditData(builder, FPArt, SPart, TPArt, LPart)
                }


            }
            if (eodReportData?.employeeGuestDetails?.isNotEmpty() == true && eodReportConfiguration?.employeeGuestReport == true) {
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
                builder.addText("EMPLOYEE GUEST DETAILS")

                builder.addFeedLine(2)
                addCustomerTextSize(builder, SMALL)
                addHorizontalLine(builder)


                eodReportData?.employeeGuestDetails?.forEach {
                    it.forEach {
                        employeeGuestDetailsData(builder, it)
                    }

                }


            }
            if (eodReportData?.salesPerCategorySummary?.isNotEmpty() == true && eodReportConfiguration?.cashCreditPerSalesCategorySummary == true) {
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
                builder.addText("SALES PER CATEGORY SUMMARY\n(MIXED-PAYMENT ORDER ITEMS NOT INCLUDED)")

                builder.addFeedLine(2)
                addCustomerTextSize(builder, SMALL)
                addHorizontalLine(builder)


                eodReportData?.salesPerCategorySummary?.forEachIndexed { index, arrayList ->
                    if (index == 0) {

                        builder.addTextLineSpace(30)
                        builder.addFeedUnit(30)
                        builder.addTextFont(Builder.FONT_E)
                        // builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        addCustomerTextSize(builder, Constants.SMALL)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )
                        builder.addText("Cash Sales")
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
                        arrayList.forEach {
                            addPaymentDetailsTwoData(builder, it)
                        }


                    } else if (index == 1) {
                        builder.addFeedLine(1)
                        builder.addTextLineSpace(30)
                        builder.addFeedUnit(30)
                        builder.addTextFont(Builder.FONT_E)
                        // builder.addTextAlign(Builder.ALIGN_LEFT)
                        builder.addTextLang(Builder.LANG_EN)
                        addCustomerTextSize(builder, Constants.SMALL)
                        builder.addTextStyle(
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.FALSE,
                            Builder.COLOR_1
                        )
                        addHorizontalLine(builder)
                        builder.addText("Credit/Non Cash Sales")
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
                        arrayList.forEach {
                            addPaymentDetailsTwoData(builder, it)
                        }

                    }


                }


            }

            // Clock in-out report
            if (eodReportData?.clockInClockOut?.isNotEmpty() == true && eodReportConfiguration?.clockInOut == true) {
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
                builder.addText("CLOCK IN-CLOCK OUT")
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

                eodReportData?.clockInClockOut?.forEach {
                    it.forEach { data ->
                        if (data.key != "Total") {
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
                                    if (data.key == "Total Working Hour") {
                                        "Total"
                                    } else {
                                        data.key
                                    },
                                    data.value.toString(),
                                    48
                                )
                            )
                        }
                    }
                    builder.addFeedLine(1)
                }
            }
            if (eodReportData?.cashLogDetails?.isNotEmpty() == true && eodReportConfiguration?.cashLogDetails == true) {
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
                builder.addText("CASH LOG DETAILS")

                builder.addFeedLine(2)
                addCustomerTextSize(builder, SMALL)
                addHorizontalLine(builder)


                eodReportData?.cashLogDetails?.forEach {

                    addPaymentDetailsTwoData(builder, it)


                }


            }
            if (eodReportData?.otherDetails?.isNotEmpty() == true && eodReportConfiguration?.otherDetails == true) {
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
                builder.addText("OTHER DETAILS")

                builder.addFeedLine(2)
                addCustomerTextSize(builder, SMALL)
                addHorizontalLine(builder)


                eodReportData?.otherDetails?.forEach {

                    addPaymentDetailsTwoData(builder, it)


                }


            }


            builder.addFeedLine(3)


            builder.addTextFont(Builder.FONT_E)
            builder.addTextAlign(Builder.ALIGN_CENTER)
            builder.addTextLang(Builder.LANG_EN)
            addCustomerTextSize(builder, SMALL)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            builder.addText("EMPLOYEE x " + repeat("_", 37))

            builder.addFeedLine(2)
            builder.addTextFont(Builder.FONT_E)
            builder.addTextAlign(Builder.ALIGN_CENTER)
            builder.addTextLang(Builder.LANG_EN)
            addCustomerTextSize(builder, SMALL)
            builder.addTextStyle(
                Builder.FALSE,
                Builder.FALSE,
                Builder.TRUE,
                Builder.COLOR_1
            )
            builder.addText("CASH RECEIVED BY" + repeat("_", 32))




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

        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun createReportFormatEODSunmi(customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters) {
        Log.d("BIS-685", "createReportFormatEODSunmi: Called")
        try {


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

            PrintSunmiUtils.addLable("Employee End of Day Report")

            SunmiPrinterApi.getInstance().setAlignMode(1)
            SunmiPrinterApi.getInstance().enableBold(false)
            SunmiPrinterApi.getInstance().setFontZoom(1, 1)
            SunmiPrinterApi.getInstance()
                .printText("Employee : " + binding.spTerminals.selectedItem.toString())
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

            PrintSunmiUtils.orderTime("Employee Report:" + eodReportData?.reportTime)
            SunmiPrinterApi.getInstance().lineWrap(1)

            if (eodReportData?.orderSalesDetails?.data?.isNotEmpty() == true && eodReportConfiguration?.orderSalesDetails == true) {

                PrintSunmiUtils.addLable("ORDER SALES DETAILS")

                addSixHeaderForOrderSaleDetailsSunmi()

                PrintSunmiUtils.addHorizontal()


                eodReportData?.orderSalesDetails?.data?.forEach {
                    addItemsInOrderSalesDetails(it)
                }
                var totalAmount = 0.0
                eodReportData?.orderSalesDetails?.data?.forEach {

                    totalAmount += it.amount
                }
                PrintSunmiUtils.orderTime("Total                                ${MethodUtils.roundOffAmount(totalAmount)}")

                SunmiPrinterApi.getInstance().lineWrap(1)
            }

            if (eodReportData?.salesSummary?.isNotEmpty() == true && eodReportConfiguration?.salesSummary == true) {

                PrintSunmiUtils.addLable("SALES SUMMARY")
                SunmiPrinterApi.getInstance().lineWrap(1)


                eodReportData?.salesSummary?.forEach {

                    PrintSunmiUtils.orderTime(
                        padLine(
                            it.key,
                            it.showData(),
                            48
                        ).toString()
                    )
                }
                SunmiPrinterApi.getInstance().lineWrap(1)

            }

            if (eodReportData?.salesAndTaxesSummary?.isNotEmpty() == true && eodReportConfiguration?.salesAndTaxSummary == true) {

                PrintSunmiUtils.addLable("SALES AND TAXES SUMMARY")

                PrintSunmiUtils.orderTime(padLine("Category(Quantity)", "Amount", 48).toString())

                PrintSunmiUtils.addHorizontal()


                eodReportData?.salesAndTaxesSummary?.forEach {

                    PrintSunmiUtils.orderTime(
                        padLine(
                            it.key,
                            it.showData(),
                            48
                        ).toString()
                    )
                }
                SunmiPrinterApi.getInstance().lineWrap(1)
            }

            if (eodReportData?.itemWiseSales?.isNotEmpty() == true && eodReportConfiguration?.isItemWiseSales == true){

                PrintSunmiUtils.addLable("ITEM WISE SALES")
                addItemWiseSalesHeader()

                PrintSunmiUtils.addHorizontal()
                eodReportData?.itemWiseSales?.forEach {
                    addItemWiseSales(it)
                }

                SunmiPrinterApi.getInstance().lineWrap(1)

            }

            if (eodReportData?.paymentDetails?.isNotEmpty() == true && eodReportConfiguration?.paymentDetails == true) {

                PrintSunmiUtils.addLable("PAYMENT DETAILS")

                addPaymentDetailsHeader()

                PrintSunmiUtils.addHorizontal()


                eodReportData?.paymentDetails?.forEach {

                    if (it.size == 2) {


                        addPaymentDetailsThreeData(it)

                    } else if (it.size == 1) {
                        it.forEach {
                            addPaymentDetailsTwoData(it)
                        }
                    }
                }

                SunmiPrinterApi.getInstance().lineWrap(1)
            }

            if (eodReportData?.tipDetails?.isNotEmpty() == true && eodReportConfiguration?.tipsDetails == true) {

                PrintSunmiUtils.addLable("TIPS DETAILS")


                addPaymentDetailsHeader()

                PrintSunmiUtils.addHorizontal()

                eodReportData?.tipDetails?.forEach {

                    if (it.size == 2) {
                        addPaymentDetailsThreeData(it)

                    } else if (it.size == 1) {
                        it.forEach {
                            addPaymentDetailsTwoData(it)
                        }
                    }
                }
                SunmiPrinterApi.getInstance().lineWrap(1)

            }

            if (eodReportData?.taxDetails?.isNotEmpty() == true && eodReportConfiguration?.taxDetails == true) {

                PrintSunmiUtils.addLable("TAX DETAILS")


                eodReportData?.taxDetails?.forEach {


                    PrintSunmiUtils.orderTime(
                        padLine(
                            it.key,
                            it.showData(),
                            48
                        ).toString()
                    )
                }
                SunmiPrinterApi.getInstance().lineWrap(1)
            }

            if (eodReportData?.refundAndVoidDetails?.isNotEmpty() == true && eodReportConfiguration?.refundOrVoids == true) {

                PrintSunmiUtils.addLable("REFUNDS/VOIDS")


                PrintSunmiUtils.orderTime(
                    padLine(
                        "Order Id(Employee Name)",
                        "Amount",
                        48
                    ).toString()
                )

                PrintSunmiUtils.addHorizontal()

                eodReportData?.refundAndVoidDetails?.forEach {

                    if (it.size > 1) {
                        addRefundVoidsMultiple(it)
                    } else if (it.size == 1) {
                        it.forEach {
                            addPaymentDetailsTwoData(it)
                        }

                    }
                }
                SunmiPrinterApi.getInstance().lineWrap(1)
            }


            if (eodReportData?.refundDetails?.isNotEmpty() == true && eodReportConfiguration?.refundDetails == true) {

                PrintSunmiUtils.addLable("REFUND DETAILS")


                eodReportData?.refundDetails?.forEach {


                    PrintSunmiUtils.orderTime(
                        padLine(
                            it.key,
                            it.showData(),
                            48
                        ).toString()
                    )
                }
                SunmiPrinterApi.getInstance().lineWrap(1)
            }

            if (eodReportData?.discountDetails?.isNotEmpty() == true && eodReportConfiguration?.discountDetails == true) {

                PrintSunmiUtils.addLable("DISCOUNT DETAILS")


                eodReportData?.discountDetails?.forEach {

                    PrintSunmiUtils.orderTime(
                        padLine(
                            it.key,
                            it.showData(),
                            48
                        ).toString()
                    )
                }
                SunmiPrinterApi.getInstance().lineWrap(1)
            }

            if (eodReportData?.totalCreditPaymentDetails?.isNotEmpty() == true && eodReportConfiguration?.totalCreditPayments == true) {

                PrintSunmiUtils.addLable("TOTAL CREDIT PAYMENT")


                eodReportData?.totalCreditPaymentDetails?.forEach {


                    PrintSunmiUtils.orderTime(
                        padLine(
                            it.key,
                            it.showData(),
                            48
                        ).toString()
                    )
                }
                SunmiPrinterApi.getInstance().lineWrap(1)

            }
            if (eodReportData?.totalCashPayments?.isNotEmpty() == true && eodReportConfiguration?.totalCashPayments == true) {

                PrintSunmiUtils.addLable("TOTAL CASH PAYMENT")


                eodReportData?.totalCashPayments?.forEach {

                    PrintSunmiUtils.orderTime(
                        padLine(
                            it.key,
                            it.showData(),
                            48
                        ).toString()
                    )
                }
                SunmiPrinterApi.getInstance().lineWrap(1)
            }
            if (eodReportData?.totalPayments?.isNotEmpty() == true && eodReportConfiguration?.totalPayments == true) {

                PrintSunmiUtils.addLable("TOTAL PAYMENTS")

                eodReportData?.totalPayments?.forEach {


                    PrintSunmiUtils.orderTime(
                        padLine(
                            it.key,
                            it.showData(),
                            48
                        ).toString()
                    )
                }
                SunmiPrinterApi.getInstance().lineWrap(1)
            }

            if (eodReportData?.creditCardBreakdown?.isNotEmpty() == true && eodReportConfiguration?.creditCardBreakdown == true) {

                PrintSunmiUtils.addLable("CREDIT CARD BREAKDOWN")


                addCreditCardBreakDown()

                PrintSunmiUtils.addHorizontal()

                eodReportData?.creditCardBreakdown?.forEach {

                    addCreditCardBreakDownData(it)
                }
                SunmiPrinterApi.getInstance().lineWrap(1)

            }

            if (eodReportData?.serviceChargeDetails?.isNotEmpty() == true && eodReportConfiguration?.serviceChargeDetails == true) {

                PrintSunmiUtils.addLable("SERVICE CHARGE DETAILS")

                eodReportData?.serviceChargeDetails?.forEach {

                    it.forEach {
                        addPaymentDetailsTwoData(it)
                    }
                }

                SunmiPrinterApi.getInstance().lineWrap(1)
            }
            if (eodReportData?.creditTipAudit?.isNotEmpty() == true && eodReportConfiguration?.creditTipAudit == true) {

                PrintSunmiUtils.addLable("CREDIT TIP AUDIT")
                addCreditTipAuditHeader()
                PrintSunmiUtils.addHorizontal()

                eodReportData?.creditTipAudit?.forEach {
                    var FPArt = ""
                    var SPart = ""
                    var LPart = ""
                    var TPArt = ""

                    it.forEach {


                        if (it.key?.contains("Subtotal", true) == true) {
                            FPArt = MethodUtils.roundOffAmount(it.value?.toDouble() ?: 0.0)
                        } else if (it.key?.contains("Tip", true) == true) {
                            SPart = MethodUtils.roundOffAmount(it.value?.toDouble() ?: 0.0)
                        } else if (it.key?.contains("Total", true) == true) {
                            LPart = MethodUtils.roundOffAmount(it.value?.toDouble() ?: 0.0)
                        } else if (it.key?.contains("Payment Id", true) == true) {
                            TPArt = it.value.toString()
                        }

                    }
                    addCreditTipAuditData(FPArt, SPart, TPArt, LPart)
                }

                SunmiPrinterApi.getInstance().lineWrap(1)
            }
            if (eodReportData?.employeeGuestDetails?.isNotEmpty() == true && eodReportConfiguration?.employeeGuestReport == true) {

                PrintSunmiUtils.addLable("EMPLOYEE GUEST DETAILS")

                eodReportData?.employeeGuestDetails?.forEach {
                    it.forEach {
                        employeeGuestDetailsData(it)
                    }

                }

                SunmiPrinterApi.getInstance().lineWrap(1)
            }
            if (eodReportData?.salesPerCategorySummary?.isNotEmpty() == true && eodReportConfiguration?.cashCreditPerSalesCategorySummary == true) {

                PrintSunmiUtils.addLable("SALES PER CATEGORY SUMMARY\n(MIXED-PAYMENT ORDER ITEMS NOT INCLUDED)")

                eodReportData?.salesPerCategorySummary?.forEachIndexed { index, arrayList ->
                    if (index == 0) {

                        PrintSunmiUtils.addValue("Cash Sales")
                        PrintSunmiUtils.addHorizontal()
                        SunmiPrinterApi.getInstance().lineWrap(1)
                        arrayList.forEach {
                            addPaymentDetailsTwoData(it)
                        }
                        SunmiPrinterApi.getInstance().lineWrap(1)

                        PrintSunmiUtils.addHorizontal()

                    } else if (index == 1) {

                        PrintSunmiUtils.addValue("Credit/Non Cash Sales")
                        PrintSunmiUtils.addHorizontal()
                        SunmiPrinterApi.getInstance().lineWrap(1)

                        arrayList.forEach {
                            addPaymentDetailsTwoData(it)
                        }

                    }


                }
                SunmiPrinterApi.getInstance().lineWrap(1)

            }

            // Clock in-out report
            if (eodReportData?.clockInClockOut?.isNotEmpty() == true && eodReportConfiguration?.clockInOut == true) {
                PrintSunmiUtils.addLable("CLOCK IN-CLOCK OUT")
                eodReportData?.clockInClockOut?.forEach { it ->
                    it.forEach {
                        if (it.key != "Total") {
                            PrintSunmiUtils.orderTime(
                                padLine(
                                    if (it.key == "Total Working Hour") { "Total" } else { it.key },
                                    it.value.toString(),
                                    48
                                ).toString()
                            )
                        }
                    }
                }
            }

            if (eodReportData?.cashLogDetails?.isNotEmpty() == true && eodReportConfiguration?.cashLogDetails == true) {

                PrintSunmiUtils.addLable("CASH LOG DETAILS")

                eodReportData?.cashLogDetails?.forEach {

                    addPaymentDetailsTwoData(it)


                }

                SunmiPrinterApi.getInstance().lineWrap(1)
            }
            if (eodReportData?.otherDetails?.isNotEmpty() == true && eodReportConfiguration?.otherDetails == true) {

                PrintSunmiUtils.addLable("OTHER DETAILS")

                eodReportData?.otherDetails?.forEach {

                    addPaymentDetailsTwoData(it)


                }

            }


            SunmiPrinterApi.getInstance().lineWrap(2)



            PrintSunmiUtils.orderTime("EMPLOYEE x " + repeat("_", 36))
            SunmiPrinterApi.getInstance().lineWrap(1)

            PrintSunmiUtils.orderTime("CASH RECEIVED BY" + repeat("_", 31))

            SunmiPrinterApi.getInstance().lineWrap(5)
            SunmiPrinterApi.getInstance().cutPaper(1, 1)

        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun createReportFormatEODSunmiInner(customerReceiptPrinters: PrinterResponse.Data.CustomerReceiptPrinters) {
        try {
            Log.d("BIS-685", "createReportFormatEODSunmiInner: Called")

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

            PrintSunmiUtils.headerText("Employee End of Day Report")
            if (binding.spTerminals.selectedItem.toString().isNotEmpty()) {
                PrintSunmiUtils.normalTextCenter("Employee : " + binding.spTerminals.selectedItem.toString())
            } else {
                PrintSunmiUtils.normalTextCenter(
                    "Employee : " + prefProvider?.getValue(
                        Constants.EMPLOYEE_NAME,
                        ""
                    )
                )
            }
            PrintSunmiUtils.addHorizontalInner()
            SunmiPrintHelper.getInstance().lineWrap(1)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PrintSunmiUtils.normalText("Print Time:${MethodUtils.formatted()}")
            }
            SunmiPrintHelper.getInstance().lineWrap(1)

            PrintSunmiUtils.normalText("Employee Report:" + eodReportData?.reportTime)
            SunmiPrintHelper.getInstance().lineWrap(1)

            if (eodReportData?.orderSalesDetails?.data?.isNotEmpty() == true && eodReportConfiguration?.orderSalesDetails == true) {

                PrintSunmiUtils.headerText("ORDER SALES DETAILS")

                if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt()!=39){
                    addSixHeaderForOrderSaleDetailsSunmiInnerNew()
                    PrintSunmiUtils.addHorizontalInnerNew()
                    PrintSunmiUtils.normalTextNew("\n")
                    eodReportData?.orderSalesDetails?.data?.forEach {
                        addItemsInOrderSalesDetailsInnerNew(it)
                    }

                    var totalAmount = 0.0

                    eodReportData?.orderSalesDetails?.data?.forEach {

                        totalAmount += it.amount
                    }
                    PrintSunmiUtils.addHorizontalInnerNew()
//                    PrintSunmiUtils.normalTextNew("\n")
                    SunmiPrintHelper.getInstance().lineWrap(1)
                    PrintSunmiUtils.normalTextNew("Total                                  ${MethodUtils.roundOffAmount(totalAmount)}")

                } else {
                    addSixHeaderForOrderSaleDetailsSunmiInner()
                    PrintSunmiUtils.addHorizontalInner()
                    eodReportData?.orderSalesDetails?.data?.forEach {
                        addItemsInOrderSalesDetailsInner(it)
                    }

                    var totalAmount = 0.0

                    eodReportData?.orderSalesDetails?.data?.forEach {

                        totalAmount += it.amount
                    }
//                    PrintSunmiUtils.normalText("\n")
                    SunmiPrintHelper.getInstance().lineWrap(1)
                    PrintSunmiUtils.normalText("Total                                  ${MethodUtils.roundOffAmount(totalAmount)}")
                }

                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            if (eodReportData?.salesSummary?.isNotEmpty() == true && eodReportConfiguration?.salesSummary == true) {

                PrintSunmiUtils.headerText("SALES SUMMARY")
                SunmiPrintHelper.getInstance().lineWrap(1)


                eodReportData?.salesSummary?.forEach {

                    if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt()!=39) {
                        PrintSunmiUtils.normalTextNew(
                            padLine(
                                it.key,
                                it.showData(),
                                48
                            ).toString()
                        )
                    } else {
                        PrintSunmiUtils.normalText(
                            padLine(
                                it.key,
                                it.showData(),
                                48
                            ).toString()
                        )
                    }
                }
                SunmiPrintHelper.getInstance().lineWrap(1)

            }

            if (eodReportData?.salesAndTaxesSummary?.isNotEmpty() == true && eodReportConfiguration?.salesAndTaxSummary == true) {

                PrintSunmiUtils.headerText("SALES AND TAXES SUMMARY")

                if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt()!=39) {

                    PrintSunmiUtils.normalTextNew(
                        padLine(
                            "Category(Quantity)",
                            "Amount",
                            48
                        ).toString()
                    )

                    PrintSunmiUtils.addHorizontalInnerNew()
                    PrintSunmiUtils.normalTextNew("\n")

                    eodReportData?.salesAndTaxesSummary?.forEach {

                        PrintSunmiUtils.normalTextNew(
                            padLine(
                                it.key,
                                it.showData(),
                                48
                            ).toString()
                        )
                    }
                } else {
                    PrintSunmiUtils.normalText(
                        padLine(
                            "Category(Quantity)",
                            "Amount",
                            48
                        ).toString()
                    )

                    PrintSunmiUtils.addHorizontalInner()


                    eodReportData?.salesAndTaxesSummary?.forEach {

                        PrintSunmiUtils.normalText(
                            padLine(
                                it.key,
                                it.showData(),
                                48
                            ).toString()
                        )
                    }
                }
                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            if (eodReportData?.itemWiseSales?.isNotEmpty() == true && eodReportConfiguration?.isItemWiseSales == true){

                PrintSunmiUtils.headerText("ITEM WISE SALES")
                if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt()!=39) {
                    addItemWiseSalesHeaderSunmiInnerNew()

                    PrintSunmiUtils.addHorizontalInnerNew()
                    PrintSunmiUtils.normalTextNew("\n")

                    eodReportData?.itemWiseSales?.forEach {
                        addItemWiseSalesSunmiInnerPrinterNew(it)
                    }
                } else {
                    addItemWiseSalesHeaderSunmiInner()

                    PrintSunmiUtils.addHorizontalInner()
                    eodReportData?.itemWiseSales?.forEach {
                        addItemWiseSalesSunmiInnerPrinter(it)
                    }
                }

                SunmiPrintHelper.getInstance().lineWrap(1)

            }

            if (eodReportData?.paymentDetails?.isNotEmpty() == true && eodReportConfiguration?.paymentDetails == true) {

                PrintSunmiUtils.headerText("PAYMENT DETAILS")

                if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt()!=39) {
                    addPaymentDetailsHeaderInnerNew()

                    PrintSunmiUtils.addHorizontalInnerNew()
                    PrintSunmiUtils.normalTextNew("\n")

                    eodReportData?.paymentDetails?.forEach {

                        if (it.size == 2) {


                            addPaymentDetailsThreeDataInnerNew(it)

                        } else if (it.size == 1) {
                            it.forEach {
                                addPaymentDetailsTwoDataInnerNew(it)
                            }
                        }
                    }
                } else {
                    addPaymentDetailsHeaderInner()

                    PrintSunmiUtils.addHorizontalInner()


                    eodReportData?.paymentDetails?.forEach {

                        if (it.size == 2) {


                            addPaymentDetailsThreeDataInner(it)

                        } else if (it.size == 1) {
                            it.forEach {
                                addPaymentDetailsTwoDataInner(it)
                            }
                        }
                    }
                }

                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            if (eodReportData?.tipDetails?.isNotEmpty() == true && eodReportConfiguration?.tipsDetails == true) {

                PrintSunmiUtils.headerText("TIPS DETAILS")

                if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt()!=39){
                    addPaymentDetailsHeaderInnerNew()

                    PrintSunmiUtils.addHorizontalInnerNew()
                    PrintSunmiUtils.normalTextNew("\n")

                    eodReportData?.tipDetails?.forEach {

                        if (it.size == 2) {
                            addPaymentDetailsThreeDataInnerNew(it)

                        } else if (it.size == 1) {
                            it.forEach {
                                addPaymentDetailsTwoDataInnerNew(it)
                            }
                        }
                    }
                } else {

                    addPaymentDetailsHeaderInner()

                    PrintSunmiUtils.addHorizontalInner()

                    eodReportData?.tipDetails?.forEach {

                        if (it.size == 2) {
                            addPaymentDetailsThreeDataInner(it)

                        } else if (it.size == 1) {
                            it.forEach {
                                addPaymentDetailsTwoDataInner(it)
                            }
                        }
                    }
                }
                SunmiPrintHelper.getInstance().lineWrap(1)

            }

            if (eodReportData?.taxDetails?.isNotEmpty() == true && eodReportConfiguration?.taxDetails == true) {

                PrintSunmiUtils.headerText("TAX DETAILS")


                eodReportData?.taxDetails?.forEach {

                    if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt()!=39){
                        PrintSunmiUtils.normalTextNew(
                            padLine(
                                it.key,
                                it.showData(),
                                48
                            ).toString()
                        )
                    } else {
                        PrintSunmiUtils.normalText(
                            padLine(
                                it.key,
                                it.showData(),
                                48
                            ).toString()
                        )
                    }
                }
                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            if (eodReportData?.refundAndVoidDetails?.isNotEmpty() == true && eodReportConfiguration?.refundOrVoids == true) {

                PrintSunmiUtils.headerText("REFUNDS/VOIDS")

                if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt()!=39){
                    PrintSunmiUtils.normalTextNew(
                        padLine(
                            "Order Id(Employee Name)",
                            "Amount",
                            48
                        ).toString()
                    )

                    PrintSunmiUtils.addHorizontalInnerNew()
                    PrintSunmiUtils.normalTextNew("\n")

                    eodReportData?.refundAndVoidDetails?.forEach {

                        if (it.size > 1) {
                            addRefundVoidsMultipleInnerNew(it)
                        } else if (it.size == 1) {
                            it.forEach {
                                addPaymentDetailsTwoDataInnerNew(it)
                            }

                        }
                    }
                } else {

                    PrintSunmiUtils.normalText(
                        padLine(
                            "Order Id(Employee Name)",
                            "Amount",
                            48
                        ).toString()
                    )

                    PrintSunmiUtils.addHorizontalInner()

                    eodReportData?.refundAndVoidDetails?.forEach {

                        if (it.size > 1) {
                            addRefundVoidsMultipleInner(it)
                        } else if (it.size == 1) {
                            it.forEach {
                                addPaymentDetailsTwoDataInner(it)
                            }

                        }
                    }
                }
                SunmiPrintHelper.getInstance().lineWrap(1)
            }


            if (eodReportData?.refundDetails?.isNotEmpty() == true && eodReportConfiguration?.refundDetails == true) {

                PrintSunmiUtils.headerText("REFUND DETAILS")


                eodReportData?.refundDetails?.forEach {

                    if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt()!=39){
                        PrintSunmiUtils.normalTextNew(
                            padLine(
                                it.key,
                                it.showData(),
                                48
                            ).toString()
                        )
                    }else {
                        PrintSunmiUtils.normalText(
                            padLine(
                                it.key,
                                it.showData(),
                                48
                            ).toString()
                        )
                    }
                }
                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            if (eodReportData?.discountDetails?.isNotEmpty() == true && eodReportConfiguration?.discountDetails == true) {

                PrintSunmiUtils.headerText("DISCOUNT DETAILS")


                eodReportData?.discountDetails?.forEach {
                    if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt()!=39) {
                        PrintSunmiUtils.normalTextNew(
                            padLine(
                                it.key,
                                it.showData(),
                                48
                            ).toString()
                        )
                    }else {
                        PrintSunmiUtils.normalText(
                            padLine(
                                it.key,
                                it.showData(),
                                48
                            ).toString()
                        )
                    }
                }
                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            if (eodReportData?.totalCreditPaymentDetails?.isNotEmpty() == true && eodReportConfiguration?.totalCreditPayments == true) {

                PrintSunmiUtils.headerText("TOTAL CREDIT PAYMENT")


                eodReportData?.totalCreditPaymentDetails?.forEach {

                    if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt()!=39) {
                        PrintSunmiUtils.normalTextNew(
                            padLine(
                                it.key,
                                it.showData(),
                                48
                            ).toString()
                        )
                    }else{
                        PrintSunmiUtils.normalText(
                            padLine(
                                it.key,
                                it.showData(),
                                48
                            ).toString()
                        )
                    }
                }
                SunmiPrintHelper.getInstance().lineWrap(1)

            }
            if (eodReportData?.totalCashPayments?.isNotEmpty() == true && eodReportConfiguration?.totalCashPayments == true) {

                PrintSunmiUtils.headerText("TOTAL CASH PAYMENT")


                eodReportData?.totalCashPayments?.forEach {

                    if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt()!=39) {
                        PrintSunmiUtils.normalTextNew(
                            padLine(
                                it.key,
                                it.showData(),
                                48
                            ).toString()
                        )
                    }else {
                        PrintSunmiUtils.normalText(
                            padLine(
                                it.key,
                                it.showData(),
                                48
                            ).toString()
                        )
                    }
                }
                SunmiPrintHelper.getInstance().lineWrap(1)
            }
            if (eodReportData?.externalPayments?.isNotEmpty() == true && eodReportConfiguration?.totalCashPayments == true) {

                PrintSunmiUtils.headerText("TOTAL EXTERNAL PAYMENT")


                eodReportData?.externalPayments?.forEach { report ->
                    report.forEach {
                        if(it.key?.contains("Name", true) == true){
                            if (sunmiFrameworkVersion?.get(0)
                                    ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)
                                    ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
                            ) {
                                PrintSunmiUtils.addHorizontalInnerNew()
                            } else {
                                PrintSunmiUtils.addHorizontalInner()
                            }
                            SunmiPrintHelper.getInstance().lineWrap(1)
                        }
                        if (sunmiFrameworkVersion?.get(0)
                                ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)
                                ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
                        ) {
                            PrintSunmiUtils.normalTextNew(
                                padLine(
                                    it.key,
                                    it.showData(),
                                    48
                                ).toString()
                            )
                        } else {
                            PrintSunmiUtils.normalText(
                                padLine(
                                    it.key,
                                    it.showData(),
                                    48
                                ).toString()
                            )
                        }
                        if(it.key?.contains("Name", true) == true){
                            if (sunmiFrameworkVersion?.get(0)
                                    ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)
                                    ?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt() != 39
                            ) {
                                PrintSunmiUtils.addHorizontalInnerNew()
                            } else {
                                PrintSunmiUtils.addHorizontalInner()
                            }
                            SunmiPrintHelper.getInstance().lineWrap(1)
                        }
                    }
                }
                SunmiPrintHelper.getInstance().lineWrap(1)
            }
            if (eodReportData?.totalPayments?.isNotEmpty() == true && eodReportConfiguration?.totalPayments == true) {

                PrintSunmiUtils.headerText("TOTAL PAYMENTS")

                eodReportData?.totalPayments?.forEach {

                    if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt()!=39) {
                        PrintSunmiUtils.normalTextNew(
                            padLine(
                                it.key,
                                it.showData(),
                                48
                            ).toString()
                        )
                    }else {
                        PrintSunmiUtils.normalText(
                            padLine(
                                it.key,
                                it.showData(),
                                48
                            ).toString()
                        )
                    }
                }
                SunmiPrintHelper.getInstance().lineWrap(1)
            }

            if (eodReportData?.creditCardBreakdown?.isNotEmpty() == true && eodReportConfiguration?.creditCardBreakdown == true) {

                PrintSunmiUtils.headerText("CREDIT CARD BREAKDOWN")

                if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt()!=39) {

                    addCreditCardBreakDownInnerNew()

                    PrintSunmiUtils.addHorizontalInnerNew()
                    PrintSunmiUtils.normalTextNew("\n")

                    eodReportData?.creditCardBreakdown?.forEach {

                        addCreditCardBreakDownDataInnerNew(it)
                    }
                } else {
                    addCreditCardBreakDownInner()

                    PrintSunmiUtils.addHorizontalInner()

                    eodReportData?.creditCardBreakdown?.forEach {

                        addCreditCardBreakDownDataInner(it)
                    }
                }
                SunmiPrintHelper.getInstance().lineWrap(1)

            }

            if (eodReportData?.serviceChargeDetails?.isNotEmpty() == true && eodReportConfiguration?.serviceChargeDetails == true) {

                PrintSunmiUtils.headerText("SERVICE CHARGE DETAILS")

                eodReportData?.serviceChargeDetails?.forEach {

                    it.forEach {
                        if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt()!=39)
                            addPaymentDetailsTwoDataInnerNew(it)
                        else
                            addPaymentDetailsTwoDataInner(it)
                    }
                }

                SunmiPrintHelper.getInstance().lineWrap(1)
            }
            if (eodReportData?.creditTipAudit?.isNotEmpty() == true && eodReportConfiguration?.creditTipAudit == true) {

                PrintSunmiUtils.headerText("CREDIT TIP AUDIT")
                if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt()!=39) {
                    addCreditTipAuditHeaderInnerNew()
                    PrintSunmiUtils.addHorizontalInnerNew()
                    PrintSunmiUtils.normalTextNew("\n")
                }else {
                    addCreditTipAuditHeaderInner()
                    PrintSunmiUtils.addHorizontalInner()
                }

                eodReportData?.creditTipAudit?.forEach { it ->
                    var FPArt = ""
                    var SPart = ""
                    var LPart = ""
                    var TPArt = ""

                    it.forEach {


                        when {
                            it.key?.contains("Subtotal", true) == true -> {
                                FPArt = MethodUtils.roundOffAmount(it.value?.toDouble() ?: 0.0)
                            }

                            it.key?.contains("Tip", true) == true -> {
                                SPart = MethodUtils.roundOffAmount(it.value?.toDouble() ?: 0.0)
                            }

                            it.key?.contains("Total", true) == true -> {
                                LPart = MethodUtils.roundOffAmount(it.value?.toDouble() ?: 0.0)
                            }

                            it.key?.contains("Payment Id", true) == true -> {
                                TPArt = it.value.toString()
                            }
                        }

                    }
                    if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt()!=39) {
                        addCreditTipAuditDataInnerNew(FPArt, SPart, TPArt, LPart)
                    }else{
                        addCreditTipAuditDataInner(FPArt, SPart, TPArt, LPart)
                    }
                }

                SunmiPrintHelper.getInstance().lineWrap(1)
            }
            if (eodReportData?.employeeGuestDetails?.isNotEmpty() == true && eodReportConfiguration?.employeeGuestReport == true) {

                PrintSunmiUtils.headerText("EMPLOYEE GUEST DETAILS")

                eodReportData?.employeeGuestDetails?.forEach {
                    it.forEach {
                        if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt()!=39) {
                            employeeGuestDetailsDataInnerNew(it)
                        }else{
                            employeeGuestDetailsDataInner(it)
                        }
                    }

                }

                SunmiPrintHelper.getInstance().lineWrap(1)
            }
            if (eodReportData?.salesPerCategorySummary?.isNotEmpty() == true && eodReportConfiguration?.cashCreditPerSalesCategorySummary == true) {

                PrintSunmiUtils.headerText("SALES PER CATEGORY SUMMARY\n(MIXED-PAYMENT ORDER ITEMS NOT INCLUDED)")

                eodReportData?.salesPerCategorySummary?.forEachIndexed { index, arrayList ->
                    if (index == 0) {

                        PrintSunmiUtils.normalTextCenter("Cash Sales")
                        PrintSunmiUtils.addHorizontalInner()
                        SunmiPrintHelper.getInstance().lineWrap(1)
                        arrayList.forEach {
                            if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt()!=39) {
                                addPaymentDetailsTwoDataInnerNew(it)
                            }else{
                                addPaymentDetailsTwoDataInner(it)
                            }
                        }
                        SunmiPrintHelper.getInstance().lineWrap(1)

                        PrintSunmiUtils.addHorizontalInnerNew()
                        PrintSunmiUtils.normalTextNew("\n")

                    } else if (index == 1) {

                        PrintSunmiUtils.normalTextCenter("Credit/Non Cash Sales")
                        PrintSunmiUtils.addHorizontalInner()
                        SunmiPrintHelper.getInstance().lineWrap(1)

                        arrayList.forEach {
                            if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt()!=39) {
                                addPaymentDetailsTwoDataInnerNew(it)
                            }else{
                                addPaymentDetailsTwoDataInner(it)
                            }
                        }

                    }


                }
                SunmiPrintHelper.getInstance().lineWrap(1)

            }

            // Clock in-out report
            if (eodReportData?.clockInClockOut?.isNotEmpty() == true && eodReportConfiguration?.clockInOut == true) {

                PrintSunmiUtils.headerText("CLOCK IN-CLOCK OUT")
                SunmiPrintHelper.getInstance().lineWrap(1)

                eodReportData?.clockInClockOut?.forEach {
                    it.forEach { data ->
                        if (data.key != "Total") {
                            if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt()!=39) {
                                PrintSunmiUtils.normalTextNew(
                                    padLine(
                                        if (data.key == "Total Working Hour") {
                                            "Total"
                                        } else {
                                            data.key
                                        },
                                        data.value.toString(),
                                        48
                                    ).toString()
                                )
                            }else {
                                PrintSunmiUtils.normalText(
                                    padLine(
                                        if (data.key == "Total Working Hour") {
                                            "Total"
                                        } else {
                                            data.key
                                        },
                                        data.value.toString(),
                                        48
                                    ).toString()
                                )
                            }
                        }
                    }
                    SunmiPrintHelper.getInstance().lineWrap(1)
                }
            }

            if (eodReportData?.cashLogDetails?.isNotEmpty() == true && eodReportConfiguration?.cashLogDetails == true) {

                PrintSunmiUtils.headerText("CASH LOG DETAILS")

                eodReportData?.cashLogDetails?.forEach {
                    if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt()!=39) {
                        addPaymentDetailsTwoDataInnerNew(it)
                    }else{
                        addPaymentDetailsTwoDataInner(it)
                    }
                }

                SunmiPrintHelper.getInstance().lineWrap(1)
            }
            if (eodReportData?.otherDetails?.isNotEmpty() == true && eodReportConfiguration?.otherDetails == true) {

                PrintSunmiUtils.headerText("OTHER DETAILS")

                eodReportData?.otherDetails?.forEach {

                    if (sunmiFrameworkVersion?.get(0)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(1)?.toInt()!! >= 3 && sunmiFrameworkVersion?.get(2)?.toInt()!=39) {
                        addPaymentDetailsTwoDataInnerNew(it)
                    }else{
                        addPaymentDetailsTwoDataInner(it)
                    }

                }

            }


            SunmiPrintHelper.getInstance().lineWrap(2)



            PrintSunmiUtils.normalText("EMPLOYEE x " + repeat("_", 36))
            SunmiPrintHelper.getInstance().lineWrap(1)

            PrintSunmiUtils.normalText("CASH RECEIVED BY" + repeat("_", 31))

            PrintSunmiUtils.cutPaperInner()

        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun customerPrinters() {
        viewModel.getCustomerPrinterList().observe(
            viewLifecycleOwner
        ) {
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
                System.gc()
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

            var fromDate = SimpleDateFormat("dd/MM/yyyy hh:mm a").parse(viewModel.startDate.value).getTime() / 1000
            var endDate = SimpleDateFormat("dd/MM/yyyy hh:mm a").parse(timeCalculateForStartEndTime(hour, minute, "isend")).getTime() / 1000
            if (fromDate<=endDate){
                val timecalender = Calendar.getInstance()
                timecalender.set(Calendar.HOUR_OF_DAY, hour)
                timecalender.set(Calendar.MINUTE, minute)

                viewModel.endDate.value = timeCalculateForStartEndTime(hour, minute, "isend")
                if (differnceTrue(viewModel.endDate.value!!, viewModel.startDate.value) <= 30) {
                    System.gc()
                    viewModel.getReportSummary("")
                } else {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        requireActivity(),
                        "Please Select date in 30 Days."
                    ) { _, _ ->
                    }
                }
            }else{
                AlertUtils.showCustomAlertWithListenerWithOK(
                    requireActivity(),
                    "The end date cannot be earlier than the start date. Please select a valid date range."
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
        LogUtil.logE("CheckDate", "startingDate   $startDatestring $timestring")
        return "$startDatestring $timestring"
    }

    private fun setupAdapter() {

        binding.apply {

            binding.rvTerminal.adapter = terminalAdapter

            rvSalesSummary.adapter = salesReportAdapter
            rvRefundDetails.adapter = refundDetailsAdapter
            rvPendingPayments.adapter = creditPaymentDetailsAdapter
            rvTaxDetails.adapter = taxDetailsAdapter
            rvDiscountDetails.adapter = discountDetailsAdapter
            rvSalesTaxSummary.adapter = salesTaxSummaryAdapter
            rvRefundAndVoid.adapter = cashEventSummaryAdapter
            rvTotalPayments.adapter = totalPaymentsAdapter
            rvCashPayments.adapter = cashPaymentsAdapter
            rvPaymentDetails.adapter = paymentDetailsAdapter
            rvOtherDetails.adapter = otherDetailsAdapter
            rvServiceChargeDetails.adapter = serviceChargeDetailsAdapter
            rvTipsDetails.adapter = tipDetailsAdapter
            rvCashLog.adapter = cashLogAdapter
            rvCreditCardBreakDown.adapter = creditCardBreakdownAdapter
            rvSalesDetails.adapter = salesOrderDetailsAdapter
            //rvSalesDetails.isNestedScrollingEnabled = false
            rvCreditAuditTip.adapter = creditTipAuditAdapter
            rvemployeeGuestDetails.adapter = employeeGuestDetailsAdapter
            rvSaleCategorySummary?.adapter = saleCategorySummaryAdapter
            rvClockInClockOut?.adapter = clockInClockOutAdapter
            rvExternalPayments?.adapter = externalPaymentsAdapter
            rvItemWiseSales.adapter = itemWiseSalesAdapter
        }


    }

    private fun initObservers() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

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

                Log.d(TAG, "BEFORE JOB: CALLED")
                runOnUiThread(Runnable {
                ProgressUtils.showProgressDialog(requireActivity())
                })

                updateData(it)
                Handler(Looper.getMainLooper()).postDelayed(Runnable {

                    runOnUiThread(Runnable {
                    ProgressUtils.dismissProgressDialog()
                    })


                }, 4000)

                Log.d(TAG, "AFTER JOB: CALLED")
            }


        })
    }

    fun updateData(it: EodReportResponse.Data) {


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
                rvMedia = binding.rvExternalPayments,
                textView = binding.txtExternalPayments,
                headerView = null,
                visible = it.externalPayments.isNotEmpty()
            )
            Log.e("ExternalPaymentDetails", "Details ${Gson().toJson(it.externalPayments)}")

            val newList = ArrayList<KeyValue>()

        it.externalPayments.forEach {
            it.forEach{it1->
                newList.add(it1)
            }
        }

            externalPaymentsAdapter.add(newList)


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

            LogUtil.logE(TAG, "clock in out Data:  ${Gson().toJson(it.clockInClockOut)}")
            it.clockInClockOut?.let {
                if (it.isNotEmpty()) {
                    var list: ArrayList<ClockinOutReportModel> = arrayListOf()
                    it.forEach {
                        if (it.size == 5) {
                            val model = ClockinOutReportModel()
                            it.forEach {


                                if (it.key == "Employee") {
                                    model.empName = it.value


                                } else if (it.key == "Clock In") {
                                    model.clockIn = it.value
                                } else if (it.key == "Clock Out") {
                                    model.clockOutval = it.value

                                } else if (it.key == "Total Working Hour") {
                                    model.totalTime = it.value
                                } else if (it.key == "Actual In Time") {
                                    model.actualTime = it.value
                                }


                            }
                            list.add(model)

                        }
                    }
                    LogUtil.logE(TAG, "clockinData ${list.size}")

                    clockInClockOutAdapter.setList(list)
                } else {
                    binding.rvClockInClockOut?.gone()
                    binding.txtClockInClockOut?.gone()
                    binding.linearClockInOut?.gone()
                }


            }

            if (it.salesPerCategorySummary != null) {

                var arrayListSalePerCategory: ArrayList<KeyValue> = arrayListOf()

                it.salesPerCategorySummary.forEachIndexed { index, arrayList ->
                    if (index == 0) {
                        arrayListSalePerCategory.add(KeyValue("FULL_LINE_DIVIDER", ""))
                        arrayListSalePerCategory.add(KeyValue("Cash Sales", ""))
                        arrayListSalePerCategory.add(KeyValue("FULL_LINE_DIVIDER", ""))
                        arrayList.forEach {
                            arrayListSalePerCategory.add(it)
                        }
                    } else if (index == 1) {
                        arrayListSalePerCategory.add(KeyValue("FULL_LINE_DIVIDER", ""))
                        arrayListSalePerCategory.add(KeyValue("Credit/Non Cash Sales", ""))
                        arrayListSalePerCategory.add(KeyValue("FULL_LINE_DIVIDER", ""))
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


        itemWiseSalesAdapter.add(it.itemWiseSales)

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


        showHide(
            rvMedia = binding.rvItemWiseSales,
            textView = binding.txtItemWiseSales,
            headerView = binding.itemWiseHeader,
            visible = it.itemWiseSales.isEmpty()
        )


        shiftReportsSettingModel?.isItemWiseSales?.let { it1 ->
            binding.apply {
                showHide(
                    rvItemWiseSales,
                    txtItemWiseSales,
                    itemWiseHeader,
                    it1
                )
            }
        }
        requireContext().printLog("updateData","is boolean = ${shiftReportsSettingModel?.isItemWiseSales}")
        requireContext().printLog("updateData","is data = ${it.itemWiseSales.isEmpty()}")


        itemWiseSalesAdapter.add(it.itemWiseSales)



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

            binding.txtClockOut.isClickable = true
            binding.txtClockOut.isFocusable = true


    }

    private fun showHide(
        rvMedia: RecyclerView,
        textView: TextView?,
        headerView: ViewBinding?,
        visible: Boolean
    ) {
        runOnUiThread(Runnable {
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
        })
    }

    private fun loadTerminals() {

        viewModel.employeeData.observe(viewLifecycleOwner) {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        resource.data?.let { employeeList ->
                            teamEmployeeListGlobal.clear()
                            teamEmployeeListGlobal.addAll(employeeList)


                            val isPresent =
                                teamEmployeeListGlobal.any { it.name == "All Employees" }

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
                                    "All Employees",
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


                            teamEmployeeListGlobal.forEachIndexed { index, employee ->
                                Log.d("viewLifecycleOwner","index = $index")
                                if (viewModel.employeeId() == employee.id) {
                                    Log.d("teamEmployeeListGlobal","index = $index")
                                    defaultEmployeePos = index
                                    return@forEachIndexed
                                }

                            }

                            val roleName = teamEmployeeListGlobal.map { it.name }



                            if (defaultEmployeePos != -1 && roleName.isNotEmpty())
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
        LogUtil.logE(TAG, "terminalListSize  mm ${terminalList.size}")
        val spinnerAdapter = ArrayAdapter(
            requireActivity(),
            R.layout.row_spinner,
            terminalList
        )

        try {
            spinnerAdapter.setDropDownViewResource(R.layout.row_spinner)
            binding.spTerminals.adapter = spinnerAdapter

            if (defaultEmployeePos != -1) {
                // binding.spTerminals.setSelection(defaultEmployeePos, false);
                LogUtil.logE("defaultEmployeePos", defaultEmployeePos.toString())

                viewModel.viewModelScope.launch {
                    try {
                        binding.spTerminals.setSelection(defaultEmployeePos, false)
                        LogUtil.logE(TAG, "terminalListSize  1")
                    }catch (e:Exception){

                        if (defaultEmployeePos <=0 ){
                            LogUtil.logE(TAG, "terminalListSize  2")
                            binding.spTerminals.setSelection(0, false)
                        }else {
                            try {
                                LogUtil.logE(TAG, "terminalListSize  3")
                                val selection = defaultEmployeePos - 1
                                binding.spTerminals.setSelection(selection, false)
                            }catch (e:Exception) {
                                Log.e("ClearDataLogoutCrash", e.toString())
                            }
                        }


                    }

                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }


    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

        try {
            if (teamEmployeeListGlobal.size > 0 && position > 0 && position < teamEmployeeListGlobal.size) {
                viewModel.selectedTerminalId = teamEmployeeListGlobal[position].id.toString()
                LogUtil.logE("selectedEmpId", teamEmployeeListGlobal[position].id.toString())
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