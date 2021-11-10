package com.android.pos.ui.fragments.report

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.android.pos.R
import com.android.pos.data.model.responseModel.VenueDetailsResponse
import com.android.pos.databinding.FragmentReportSummaryBinding
import com.android.pos.ui.adapter.*
import com.android.pos.utils.EventObserver
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.extensions.visible
import com.android.pos.utils.statusUtils.Status
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class ReportSummaryFragment : Fragment(), AdapterView.OnItemSelectedListener {

    private lateinit var binding: FragmentReportSummaryBinding
    private val viewModel by viewModels<ReportViewModel>()

    private lateinit var startDate: DatePickerDialog.OnDateSetListener
    private lateinit var endDate: DatePickerDialog.OnDateSetListener
    private lateinit var terminalListGlobal: ArrayList<VenueDetailsResponse.Data.Terminal>

    private val terminalAdapter by lazy { TerminalAdapter() }
    private val salesReportAdapter by lazy { SalesReportAdapter() }
    private val refundDetailsAdapter by lazy { SalesReportAdapter() }
    private val pendingPaymentsAdapter by lazy { SalesReportAdapter() }
    private val taxDetailsAdapter by lazy { SalesReportAdapter() }
    private val discountDetailsAdapter by lazy { SalesReportAdapter() }
    private val salesTaxSummaryAdapter by lazy { SalesReportAdapter() }
    private val cashEventSummaryAdapter by lazy { SalesReportAdapter() }
    private val totalExternalPaymentsAdapter by lazy { SalesReportAdapter() }
    private val totalPaymentsAdapter by lazy { SalesReportAdapter() }
    private val cashPaymentsAdapter by lazy { SalesReportAdapter() }
    private val employeeAdapter by lazy { EmployeeAdapter() }
    private val paymentDetailsAdapter by lazy { PaymentDetailsAdapter() }
    private val employeeReportsAdapter by lazy { EmployeeReportAdapter() }
    private val otherDetailsAdapter by lazy { EmployeeReportAdapter() }
    private val serviceChargeDetailsAdapter by lazy { ServiceChargeDetailsAdapter() }
    private val tipDetailsAdapter by lazy { PaymentDetailsAdapter() }

    val myCalendar = Calendar.getInstance()
    val myCalendar1 = Calendar.getInstance()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentReportSummaryBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initControls()
        initObservers()
        loadTerminals()

        binding.txtHome.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_dashboardCategory)
        }
        binding.imgBack.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.txtSearch.setOnClickListener {
            viewModel.getReportSummary()
        }

        //call initial api
        //viewModel.getReportSummary()
    }

    private fun initControls() {
        binding.nsvReport.isNestedScrollingEnabled = false
        binding.spTerminals.onItemSelectedListener = this

        setupAdapter()
        setupCalender()
    }

    private fun setupCalender() {
        startDate = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            myCalendar.set(Calendar.YEAR, year)
            myCalendar.set(Calendar.MONTH, monthOfYear)
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            viewModel.updateLabel(myCalendar)
            viewModel.getReportSummary()
        }

        endDate = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            myCalendar1.set(Calendar.YEAR, year)
            myCalendar1.set(Calendar.MONTH, monthOfYear)
            myCalendar1.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            viewModel.updateLabel(myCalendar1)
            viewModel.getReportSummary()
        }
        viewModel.setCurrentDate(myCalendar)
    }

    private fun setupAdapter() {

        /*binding.rvTerminal.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayoutManager.HORIZONTAL
            )
        )*/
        binding.rvTerminal.adapter = terminalAdapter

        binding.rvSalesSummary.adapter = salesReportAdapter
        binding.rvRefundDetails.adapter = refundDetailsAdapter
        binding.rvPendingPayments.adapter = pendingPaymentsAdapter
        binding.rvTaxDetails.adapter = taxDetailsAdapter
        binding.rvDiscountDetails.adapter = discountDetailsAdapter
        binding.rvSalesTaxSummary.adapter = salesTaxSummaryAdapter
        binding.rvCashEventSummary.adapter = cashEventSummaryAdapter
        binding.rvTotalExternalPayments.adapter = totalExternalPaymentsAdapter
        binding.rvTotalPayments.adapter = totalPaymentsAdapter
        binding.rvCashPayments.adapter = cashPaymentsAdapter
        binding.rvEmployeeData.adapter = employeeAdapter
        binding.rvPaymentDetails.adapter = paymentDetailsAdapter
        binding.rvEmpReport.adapter = employeeReportsAdapter
        binding.rvOtherDetails.adapter = otherDetailsAdapter
        binding.rvServiceChargeDetails.adapter = serviceChargeDetailsAdapter
        binding.rvTipsDetails.adapter = tipDetailsAdapter
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
                terminalAdapter.add(it.terminals)

                showHide(
                    rvMedia = binding.rvSalesSummary,
                    textView = binding.txtSalesSummary,
                    headerView = null,
                    visible = it.salesSummary?.isNotEmpty() == true
                )
                salesReportAdapter.add(it.salesSummary)

                showHide(
                    rvMedia = binding.rvSalesSummary,
                    textView = binding.txtSalesSummary,
                    headerView = null,
                    visible = it.salesSummary?.isNotEmpty() == true
                )
                refundDetailsAdapter.add(it.refundDetails)

                showHide(
                    rvMedia = binding.rvPendingPayments,
                    textView = binding.txtPendingPayments,
                    headerView = null,
                    visible = it.pendingPayments?.isNotEmpty() == true
                )
                pendingPaymentsAdapter.add(it.pendingPayments)

                showHide(
                    rvMedia = binding.rvTaxDetails,
                    textView = binding.txtTaxDetails,
                    headerView = null,
                    visible = it.taxDetails?.isNotEmpty() == true
                )
                taxDetailsAdapter.add(it.taxDetails)

                showHide(
                    rvMedia = binding.rvDiscountDetails,
                    textView = binding.txtDiscountDetails,
                    headerView = null,
                    visible = it.discountDetails?.isNotEmpty() == true
                )
                discountDetailsAdapter.add(it.discountDetails)

                showHide(
                    rvMedia = binding.rvSalesTaxSummary,
                    textView = binding.txtSalesTaxSummary,
                    headerView = null,
                    visible = it.salesSummary?.isNotEmpty() == true
                )
                salesTaxSummaryAdapter.add(it.salesSummary)

                showHide(
                    rvMedia = binding.rvCashEventSummary,
                    textView = binding.txtCashEventSummary,
                    headerView = null,
                    visible = it.cashEventSummary?.isNotEmpty() == true
                )
                cashEventSummaryAdapter.add(it.cashEventSummary)

                showHide(
                    rvMedia = binding.rvTotalExternalPayments,
                    textView = binding.txtTotalExternalPayments,
                    headerView = null,
                    visible = it.totalExternalPayments?.isNotEmpty() == true
                )
                totalExternalPaymentsAdapter.add(it.totalExternalPayments)

                showHide(
                    rvMedia = binding.rvTotalPayments,
                    textView = binding.txtTotalPayments,
                    headerView = null,
                    visible = it.totalPayments?.isNotEmpty() == true
                )
                totalPaymentsAdapter.add(it.totalPayments)

                showHide(
                    rvMedia = binding.rvCashPayments,
                    textView = binding.txtCashPayments,
                    headerView = null,
                    visible = it.cashPayments?.isNotEmpty() == true
                )
                cashPaymentsAdapter.add(it.cashPayments)

                showHide(
                    rvMedia = binding.rvEmployeeData,
                    textView = binding.txtEmployeeData,
                    headerView = binding.ilEmployeeData,
                    visible = it.employeeData?.isNotEmpty() == true
                )
                employeeAdapter.add(it.employeeData)

                showHide(
                    rvMedia = binding.rvPaymentDetails,
                    textView = binding.txtPaymentDetails,
                    headerView = binding.ilPaymentDetails,
                    visible = it.paymentDetails?.isNotEmpty() == true
                )
                paymentDetailsAdapter.add(it.paymentDetails)

                showHide(
                    rvMedia = binding.rvEmpReport,
                    textView = null,
                    headerView = null,
                    visible = it.employeeReports?.isNotEmpty() == true
                )
                employeeReportsAdapter.addPrefixHeader(getString(R.string.employee_report))
                employeeReportsAdapter.add(it.employeeReports)

                showHide(
                    rvMedia = binding.rvOtherDetails,
                    textView = null,
                    headerView = null,
                    visible = it.otherDetails?.isNotEmpty() == true
                )
                otherDetailsAdapter.addPrefixHeader(getString(R.string.other_details))
                otherDetailsAdapter.add(it.otherDetails)

                showHide(
                    rvMedia = binding.rvServiceChargeDetails,
                    textView = binding.txtServiceChargeDetails,
                    headerView = null,
                    visible = it.serviceChargeDetails?.isNotEmpty() == true
                )
                serviceChargeDetailsAdapter.add(it.serviceChargeDetails)

                showHide(
                    rvMedia = binding.rvTipsDetails,
                    textView = binding.txtTipsDetails,
                    headerView = binding.ilTipsDetails,
                    visible = it.tipsDetails?.isNotEmpty() == true
                )
                tipDetailsAdapter.add(it.tipsDetails)

                //visible the parent view
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
        } else {
            rvMedia.gone()
            textView?.gone()
            headerView?.root?.gone()
        }
    }

    private fun loadTerminals() {

        viewModel.getTerminalListDatabse.observe(viewLifecycleOwner, {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        resource.data?.let { terminalList ->
                            terminalListGlobal =
                                terminalList as ArrayList<VenueDetailsResponse.Data.Terminal>
                            terminalListGlobal.add(
                                0, VenueDetailsResponse.Data.Terminal(
                                    createdAt = "", id = 0, locationId = 0, masterTerminal = false,
                                    name = "All Terminal", uniqId = "", updatedAt = ""
                                )
                            )

                            val roleName = terminalListGlobal.map { it.name }

                            setUpTerminalSpinnerAdapter(roleName as ArrayList<String>)

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

    private fun setUpTerminalSpinnerAdapter(terminalList: ArrayList<String>) {
        val spinnerAdapter = ArrayAdapter(
            requireActivity(),
            R.layout.row_spinner,
            terminalList
        )

        spinnerAdapter.setDropDownViewResource(R.layout.row_spinner)
        binding.spTerminals.adapter = spinnerAdapter

        binding.spTerminals.setSelection(0)

    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

        if (terminalListGlobal.size > 0 && position > 0 && position < terminalListGlobal.size) {
            viewModel.selectedTerminalId = terminalListGlobal[position].id.toString()
        } else {
            viewModel.selectedTerminalId = ""
        }
        /*viewModel.apiCallTimeSheet(
            getTerminalId(binding.spTerminals.selectedItemPosition).toString()
        )*/
        viewModel.getReportSummary()

    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }
}