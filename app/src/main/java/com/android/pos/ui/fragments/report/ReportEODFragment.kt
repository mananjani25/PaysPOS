package com.android.pos.ui.fragments.report

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import com.android.pos.databinding.FragmentReportEodBinding
import com.android.pos.ui.adapter.*
import com.android.pos.ui.fragments.loginscreen.ClockInOwnerViewModel
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.EventObserver
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.*
import com.android.pos.utils.statusUtils.Status
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@AndroidEntryPoint
class ReportEODFragment : Fragment(), AdapterView.OnItemSelectedListener {

    private var defaultEmployeePos: Int = 0
    private lateinit var binding: FragmentReportEodBinding
    private val viewModel by viewModels<ReportEODViewModel>()
    private val viewModelClockOut by viewModels<ClockInOwnerViewModel>()

    private lateinit var startDate: DatePickerDialog.OnDateSetListener
    private lateinit var endDate: DatePickerDialog.OnDateSetListener


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
    private val tipDetailsAdapter by lazy { PaymentDetailsAdapter(hideRefund = false) }


    private val salesOrderDetailsAdapter by lazy { SalesOrderDetailsAdapter() }

    private val creditCardBreakdownAdapter by lazy { CreditCardBreakDownAdapter(hideRefund = false) }

    private lateinit var teamEmployeeListGlobal: ArrayList<Employee>

    val myCalendar = Calendar.getInstance()
    val myCalendar1 = Calendar.getInstance()

    val myCalendar2 = Calendar.getInstance()
    val myCalendar3 = Calendar.getInstance()
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

        initControls()
        initObservers()
        loadTerminals()
        viewModel.setCurrentDate(myCalendar)

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


            viewModel.getEmployeeEmail(viewModel.selectedTerminalId.toInt()).observe(viewLifecycleOwner) {

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

        setFragmentResultListener("request_key_eod") { requestKey: String, bundle: Bundle ->


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
                    bundle.putBoolean("isSwap",false)
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
                startTime,
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
    }

    private fun initObservers() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)





        viewModel.startDateSelection.observe(requireActivity(), { event ->
            event.getContentIfNotHandled()?.let {

                DatePickerDialog(
                    requireActivity(), startDate, myCalendar
                        .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                    myCalendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
        })
        viewModel.endDateSelection.observe(requireActivity(), { event ->
            event.getContentIfNotHandled()?.let {

                DatePickerDialog(
                    requireActivity(), endDate, myCalendar1
                        .get(Calendar.YEAR), myCalendar1.get(Calendar.MONTH),
                    myCalendar1.get(Calendar.DAY_OF_MONTH)

                ).show()
            }
        })
        viewModel.showProgress.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        })
        viewModel.data.observe(viewLifecycleOwner, EventObserver { data ->
            data?.let {

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

                            setUpEmployeeSpinnerAdapter(roleName as ArrayList<String>)
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

    private fun setUpEmployeeSpinnerAdapter(terminalList: ArrayList<String>) {
        val spinnerAdapter = ArrayAdapter(
            requireActivity(),
            R.layout.row_spinner,
            terminalList
        )

        spinnerAdapter.setDropDownViewResource(R.layout.row_spinner)
        binding.spTerminals.adapter = spinnerAdapter

        binding.spTerminals.setSelection(defaultEmployeePos)

    }


    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

        if (teamEmployeeListGlobal.size > 0 && position > 0 && position < teamEmployeeListGlobal.size) {
            viewModel.selectedTerminalId = teamEmployeeListGlobal[position].id.toString()
            Log.e("selectedEmpId", teamEmployeeListGlobal[position].id.toString())
        } else {
            viewModel.selectedTerminalId = ""
        }

        viewModel.getReportSummary("")

    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }
}