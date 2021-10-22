package com.android.pos.ui.fragments.report

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.pos.R
import com.android.pos.data.model.responseModel.VenueDetailsResponse
import com.android.pos.databinding.FragmentReportSummaryBinding
import com.android.pos.ui.adapter.EmployeeAdapter
import com.android.pos.ui.adapter.PaymentDetailsAdapter
import com.android.pos.ui.adapter.SalesReportAdapter
import com.android.pos.ui.adapter.TerminalAdapter
import com.android.pos.utils.EventObserver
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.extensions.visible
import com.android.pos.utils.statusUtils.Status
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
    }

    private fun initControls() {
        binding.nsvReport.isNestedScrollingEnabled = false
        binding.spTerminals.onItemSelectedListener = this

        setupAdapter()
        setupCalender()

        //call initial api
        viewModel.getReportSummary()
    }

    private fun setupCalender() {
        startDate = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            myCalendar.set(Calendar.YEAR, year)
            myCalendar.set(Calendar.MONTH, monthOfYear)
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            viewModel.updateLabel(myCalendar)
            //viewModel.apiCallTimeSheet(getTerminalId(binding.spTerminals.selectedItemPosition).toString())

        }

        endDate = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            myCalendar1.set(Calendar.YEAR, year)
            myCalendar1.set(Calendar.MONTH, monthOfYear)
            myCalendar1.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            viewModel.updateLabel(myCalendar1)
        }
        viewModel.setCurrentDate(myCalendar)
    }

    private fun setupAdapter() {

        binding.rvTerminal.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayoutManager.HORIZONTAL
            )
        )
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
    }

    private fun initObservers() {
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

                salesReportAdapter.add(it.salesSummary)
                refundDetailsAdapter.add(it.refundDetails)
                pendingPaymentsAdapter.add(it.pendingPayments)
                taxDetailsAdapter.add(it.taxDetails)
                discountDetailsAdapter.add(it.discountDetails)
                salesTaxSummaryAdapter.add(it.salesSummary)
                cashEventSummaryAdapter.add(it.cashEventSummary)
                totalExternalPaymentsAdapter.add(it.totalExternalPayments)
                totalPaymentsAdapter.add(it.totalPayments)
                cashPaymentsAdapter.add(it.cashPayments)
                employeeAdapter.add(it.employeeData)
                paymentDetailsAdapter.add(it.paymentDetails)

                /*if (it.employeeData?.isNotEmpty() == true) {
                    binding.rvEmployeeData.visible()
                    binding.txtEmployeeData.visible()
                    employeeAdapter.add(it.employeeData)
                } else {
                    binding.rvEmployeeData.gone()
                    binding.txtEmployeeData.gone()
                }*/
            }
        })
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

    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

        /*viewModel.apiCallTimeSheet(
            getTerminalId(binding.spTerminals.selectedItemPosition).toString()
        )*/

    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }
}