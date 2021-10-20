package com.android.pos.ui.fragments.transactions

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.pos.R
import com.android.pos.data.entities.Employee
import com.android.pos.data.entities.TbOrderType
import com.android.pos.data.entities.TeamRole
import com.android.pos.data.model.responseModel.GetTransactionListResponse
import com.android.pos.data.model.responseModel.VenueDetailsResponse
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.FragmentTransactionBinding
import com.android.pos.ui.activities.MainActivity
import com.android.pos.ui.adapter.TransactionAdapter
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.callback.ItemCallback
import com.android.pos.utils.callback.PaginationScrollListener
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.statusUtils.Status
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import kotlin.collections.ArrayList

@AndroidEntryPoint
class TransactionFragment : Fragment(), AdapterView.OnItemSelectedListener, ItemCallback {

    private var selectedPos: Int = 0
    private var checkFilter: Boolean = false
    private var singleTransaction: GetTransactionListResponse.Data.Payment? = null
    private var tipAmount: Double = 0.0
    private lateinit var binding: FragmentTransactionBinding
    private lateinit var transactionAdapter: TransactionAdapter
    private val viewModel by viewModels<TransactionViewModel>()
    private lateinit var startDate: DatePickerDialog.OnDateSetListener
    private lateinit var endDate: DatePickerDialog.OnDateSetListener
    private lateinit var terminalListGlobal: ArrayList<VenueDetailsResponse.Data.Terminal>
    private lateinit var orderTypeListGlobal: ArrayList<TbOrderType>
    private lateinit var teamRoleListGlobal: ArrayList<TeamRole>
    private lateinit var teamEmployeeListGlobal: ArrayList<Employee>
    private var tipTypeList = ArrayList<String>()
    private var paymentTypeList = ArrayList<String>()
    val myCalendar = Calendar.getInstance()
    val myCalendar1 = Calendar.getInstance()
    private var spinnerTouched = false

    private var employeeTimeSheet = ArrayList<GetTransactionListResponse.Data.Payment>()

    private var TOTAL_PAGES = 0
    var PAGE_START = 1
    private var isLoading = false
    private var currentPage = PAGE_START
    private var isLastPage = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_transaction,
                container,
                false
            )

        binding.viewModel = viewModel


        binding.lifecycleOwner = this


        startDatePickerObserver()
        endDatePickerObserver()
        setUpRecyclerView()
        getEmployeesTimeSheetObserver()
        getRoleListObserver()
        setupSnackbar()
        observeShowProgress()
        loadTeams()
        loadTerminals()
        getOrderType()
        navigate()
        orderUpdateTips()

        binding.includeView.spTerminals.onItemSelectedListener = this
        binding.includeView.spRoles.onItemSelectedListener = this
        binding.includeView.spEmployees.onItemSelectedListener = this
        binding.includeView.spOrders.onItemSelectedListener = this
        binding.includeView.spTipTypes.onItemSelectedListener = this
        binding.includeView.spTransactionTypes.onItemSelectedListener = this
        setUpTipTypeSpinnerAdapter()
        setUpPaymentTypeSpinnerAdapter()


        startDate = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            myCalendar.set(Calendar.YEAR, year)
            myCalendar.set(Calendar.MONTH, monthOfYear)
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            viewModel.updateLabel(myCalendar)
            viewModel.apiCallTimeSheet(
                currentPage,
                getTerminalId(binding.includeView.spTerminals.selectedItemPosition).toString(),
                getRoleId(binding.includeView.spRoles.selectedItemPosition).toString(),
                getEmployeeId(binding.includeView.spEmployees.selectedItemPosition).toString(),
                getOrderId(binding.includeView.spOrders.selectedItemPosition).toString(),
                getTipType(binding.includeView.spTipTypes.selectedItemPosition),
                getPaymentType(binding.includeView.spTransactionTypes.selectedItemPosition)
            )

        }

        endDate = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            myCalendar1.set(Calendar.YEAR, year)
            myCalendar1.set(Calendar.MONTH, monthOfYear)
            myCalendar1.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            viewModel.updateLabel(myCalendar1)
            viewModel.apiCallTimeSheet(
                currentPage,
                getTerminalId(binding.includeView.spTerminals.selectedItemPosition).toString(),
                getRoleId(binding.includeView.spRoles.selectedItemPosition).toString(),
                getEmployeeId(binding.includeView.spEmployees.selectedItemPosition).toString(),
                getOrderId(binding.includeView.spOrders.selectedItemPosition).toString(),
                getTipType(binding.includeView.spTipTypes.selectedItemPosition),
                getPaymentType(binding.includeView.spTransactionTypes.selectedItemPosition)

            )
        }


        //   viewModel.setCurrentDate(myCalendar)

        binding.includeView.imgDrawer.setOnClickListener {
            (requireActivity() as MainActivity).enableDrawer()
        }

        binding.includeView.txtTitle.text = getString(R.string.transactions)

        binding.includeView.txtHome.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_dashboardCategory)
        }


        setFragmentResultListener("request_key_tips") { requestKey: String, bundle: Bundle ->
            tipAmount = bundle.getDouble("tipAmount")

            singleTransaction?.let { viewModel.orderUpdateTip(it.orderId, tipAmount) }

        }

        binding.includeView.spTerminals.setOnTouchListener { v, event ->
            spinnerTouched = true
            false
        }

        binding.includeView.spRoles.setOnTouchListener { v, event ->
            spinnerTouched = true
            false
        }

        binding.includeView.spEmployees.setOnTouchListener { v, event ->
            spinnerTouched = true
            false
        }

        binding.includeView.spOrders.setOnTouchListener { v, event ->
            spinnerTouched = true
            false
        }

        binding.includeView.spTipTypes.setOnTouchListener { v, event ->
            spinnerTouched = true
            false
        }
        binding.includeView.spTransactionTypes.setOnTouchListener { v, event ->
            spinnerTouched = true
            false
        }

        val layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvTeamTimeSheet.layoutManager = layoutManager

        binding.rvTeamTimeSheet.addOnScrollListener(object :
            PaginationScrollListener(layoutManager) {


            override fun isLastPage(): Boolean {
                return isLastPage
            }

            override fun isLoading(): Boolean {
                return isLoading
            }

            override fun getTotalPageCount(): Int {

                return TOTAL_PAGES
            }


            override fun loadMoreItems() {
                isLoading = true
                currentPage += 1

                viewModel.apiCallTimeSheet(
                    currentPage,
                    getTerminalId(binding.includeView.spTerminals.selectedItemPosition).toString(),
                    getRoleId(binding.includeView.spRoles.selectedItemPosition).toString(),
                    getEmployeeId(binding.includeView.spEmployees.selectedItemPosition).toString(),
                    getOrderId(binding.includeView.spOrders.selectedItemPosition).toString(),
                    getTipType(binding.includeView.spTipTypes.selectedItemPosition),
                    getPaymentType(binding.includeView.spTransactionTypes.selectedItemPosition)
                )
            }

        })


        viewModel.apiCallTimeSheet(
            currentPage,
            getTerminalId(binding.includeView.spTerminals.selectedItemPosition).toString(),
            getRoleId(binding.includeView.spRoles.selectedItemPosition).toString(),
            getEmployeeId(binding.includeView.spEmployees.selectedItemPosition).toString(),
            getOrderId(binding.includeView.spOrders.selectedItemPosition).toString(),
            getTipType(binding.includeView.spTipTypes.selectedItemPosition),
            getPaymentType(binding.includeView.spTransactionTypes.selectedItemPosition)

        )


        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setCurrentDate(myCalendar)
    }

    private fun startDatePickerObserver() {
        viewModel.startDateSelection.observe(requireActivity(), { event ->
            event.getContentIfNotHandled()?.let {

                DatePickerDialog(
                    requireActivity(), startDate, myCalendar
                        .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                    myCalendar.get(Calendar.DAY_OF_MONTH)

                ).show()
            }

        })
    }

    private fun endDatePickerObserver() {
        viewModel.endDateSelection.observe(requireActivity(), { event ->
            event.getContentIfNotHandled()?.let {

                DatePickerDialog(
                    requireActivity(), endDate, myCalendar1
                        .get(Calendar.YEAR), myCalendar1.get(Calendar.MONTH),
                    myCalendar1.get(Calendar.DAY_OF_MONTH)

                ).show()
            }
        })
    }


    private fun setUpRoleSpinnerAdapter(teamRoleList: ArrayList<String>) {
        val spinnerAdapter = ArrayAdapter(
            requireActivity(),
            R.layout.row_spinner,
            teamRoleList
        )

        spinnerAdapter.setDropDownViewResource(R.layout.row_spinner)
        binding.includeView.spRoles.adapter = spinnerAdapter

    }

    private fun setUpEmployeeSpinnerAdapter(employeeList: ArrayList<String>) {
        val spinnerAdapter = ArrayAdapter(
            requireActivity(),
            R.layout.row_spinner,
            employeeList
        )

        spinnerAdapter.setDropDownViewResource(R.layout.row_spinner)
        binding.includeView.spEmployees.adapter = spinnerAdapter

    }

    private fun setUpTerminalSpinnerAdapter(terminalList: ArrayList<String>) {
        val spinnerAdapter = ArrayAdapter(
            requireActivity(),
            R.layout.row_spinner,
            terminalList
        )

        spinnerAdapter.setDropDownViewResource(R.layout.row_spinner)
        binding.includeView.spTerminals.adapter = spinnerAdapter

    }

    private fun setUpOrderTypeSpinnerAdapter(orderTypeList: ArrayList<String>) {
        val spinnerAdapter = ArrayAdapter(
            requireActivity(),
            R.layout.row_spinner,
            orderTypeList
        )

        spinnerAdapter.setDropDownViewResource(R.layout.row_spinner)
        binding.includeView.spOrders.adapter = spinnerAdapter

    }

    private fun setUpTipTypeSpinnerAdapter() {

        tipTypeList.clear()
        tipTypeList.add(getString(R.string.tv_all_tip_types))
        tipTypeList.add(getString(R.string.tv_adjusted))
        tipTypeList.add(getString(R.string.tv_unadjusted))

        val spinnerAdapter = ArrayAdapter(
            requireActivity(),
            R.layout.row_spinner,
            tipTypeList
        )

        spinnerAdapter.setDropDownViewResource(R.layout.row_spinner)
        binding.includeView.spTipTypes.adapter = spinnerAdapter

    }

    private fun setUpPaymentTypeSpinnerAdapter() {

        paymentTypeList.clear()
        paymentTypeList.add(getString(R.string.tv_all_payment_types))
        paymentTypeList.add(getString(R.string.tv_cash_payment))
        paymentTypeList.add(getString(R.string.tv_external))

        val spinnerAdapter = ArrayAdapter(
            requireActivity(),
            R.layout.row_spinner,
            paymentTypeList
        )

        spinnerAdapter.setDropDownViewResource(R.layout.row_spinner)
        binding.includeView.spTransactionTypes.adapter = spinnerAdapter

    }

    private fun getTerminalId(position: Int): Int? {
        return if (this::terminalListGlobal.isInitialized) {

            if (position == -1) {
                terminalListGlobal?.get(0)?.id
            } else {
                terminalListGlobal?.get(position)?.id
            }

        } else {
            -1
        }
    }

    private fun getRoleId(position: Int): Int? {
        return if (this::teamRoleListGlobal.isInitialized) {

            if (position == -1) {
                teamRoleListGlobal?.get(0)?.id
            } else {
                teamRoleListGlobal?.get(position)?.id
            }

        } else {
            -1
        }
    }

    private fun getEmployeeId(position: Int): Int? {
        return if (this::teamEmployeeListGlobal.isInitialized) {

            if (position == -1) {
                teamEmployeeListGlobal?.get(0)?.id
            } else {
                teamEmployeeListGlobal?.get(position)?.id
            }
        } else {
            -1
        }
    }

    private fun getOrderId(position: Int): Int? {
        return if (this::orderTypeListGlobal.isInitialized) {

            if (position == -1) {
                orderTypeListGlobal?.get(0)?.id
            } else {
                orderTypeListGlobal?.get(position)?.id
            }

        } else {
            -1
        }
    }

    private fun getTipType(position: Int): String {
        return tipTypeList[position]
    }

    private fun getPaymentType(position: Int): String {
        return paymentTypeList[position]
    }


    private fun setUpRecyclerView() {
        binding.rvTeamTimeSheet.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayoutManager.VERTICAL
            )
        )

        transactionAdapter = TransactionAdapter(viewModel)
        transactionAdapter.setCallback(this)
        binding.rvTeamTimeSheet.adapter = transactionAdapter


    }

    private fun getEmployeesTimeSheetObserver() {
        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { timeSheet ->
                binding.rvTeamTimeSheet.visibility = View.VISIBLE
                // employeeTimeSheet.addAll(timeSheet.data.payments)
                TOTAL_PAGES = timeSheet.data.pagination.maxPageSize.toInt()

                isLoading = false
                transactionAdapter.showLoading(false)

                if (checkFilter) {
                    checkFilter = false
                    transactionAdapter.clear()
                }
                transactionAdapter.addAll(timeSheet.data.payments)


                if (currentPage != TOTAL_PAGES) {
                    transactionAdapter.showLoading(true)
                } else {
                    isLastPage = true
                }


            }
        })

    }

    private fun getRoleListObserver() {
        viewModel.getTeamRoleList.observe(viewLifecycleOwner, {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        resource.data?.let { roleList ->

                            teamRoleListGlobal = roleList as ArrayList<TeamRole>
                            val isPresent = teamRoleListGlobal.any { it.name == "All Roles" }

                            if (!isPresent) {
                                // teamRoleListGlobal.removeAt(0)
                                teamRoleListGlobal.add(0, TeamRole(-1, "All Roles", null, null))
                            }
                            val roleName = teamRoleListGlobal.map { it.name }

                            setUpRoleSpinnerAdapter(roleName as ArrayList<String>)

                            Log.d("callapi", "::callapi")

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

    private fun loadTeams() {

        viewModel.employeeData.observe(viewLifecycleOwner, {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        resource.data?.let { employeeList ->
                            teamEmployeeListGlobal = employeeList as ArrayList<Employee>

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
                                        -1,
                                        "All Team Members",
                                        "",
                                        "",
                                        "",
                                        false,
                                        -1,
                                        "",
                                        "",
                                        null
                                    )
                                )
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

                            val isPresent = terminalListGlobal.any { it.name == "All Terminals" }

                            if (!isPresent) {
                                //    terminalListGlobal.removeAt(0)
                                terminalListGlobal.add(
                                    0,
                                    VenueDetailsResponse.Data.Terminal(
                                        "",
                                        -1,
                                        -1,
                                        false,
                                        "All Terminals",
                                        "",
                                        ""
                                    )
                                )
                            }

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

    private fun getOrderType() {

        viewModel.orderTypes.observe(viewLifecycleOwner, {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        resource.data?.let { terminalList ->
                            orderTypeListGlobal =
                                terminalList as ArrayList<TbOrderType>

                            val isPresent = orderTypeListGlobal.any { it.name == "All Orders" }

                            if (!isPresent) {
                                //   orderTypeListGlobal.removeAt(0)
                                orderTypeListGlobal.add(
                                    0,
                                    TbOrderType("", -1, false, -1, "All Orders", "", -1, "")
                                )
                            }
                            val roleName = orderTypeListGlobal.map { it.name }

                            setUpOrderTypeSpinnerAdapter(roleName as ArrayList<String>)

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


    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

        if (spinnerTouched) {

            checkFilter = true
            viewModel.apiCallTimeSheet(
                currentPage,
                getTerminalId(binding.includeView.spTerminals.selectedItemPosition).toString(),
                getRoleId(binding.includeView.spRoles.selectedItemPosition).toString(),
                getEmployeeId(binding.includeView.spEmployees.selectedItemPosition).toString(),
                getOrderId(binding.includeView.spOrders.selectedItemPosition).toString(),
                getTipType(binding.includeView.spTipTypes.selectedItemPosition),
                getPaymentType(binding.includeView.spTransactionTypes.selectedItemPosition)
            )

        }
        spinnerTouched = false
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
    }

    private fun backPressManage() {
        val navController = findNavController()
        navController.previousBackStackEntry?.savedStateHandle?.set(
            Constants.KEY,
            Constants.TEAM_MEMBER
        )
        navController.popBackStack()
    }

    private fun setupSnackbar() =
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    private fun observeShowProgress() {

        viewModel.showProgress.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                if (currentPage == 1) {
                    if (it) {
                        ProgressUtils.showProgressDialog(requireActivity())
                    } else {
                        ProgressUtils.dismissProgressDialog()
                    }
                }
            }
        })

    }


    private fun navigate() {
        viewModel.transactionDetails.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                val bundle = Bundle().apply {
                    putInt("orderId", it.orderDetails.id)
                    putString("orderType", it.orderDetails.orderType)
                }
                findNavController().navigate(
                    R.id.action_transactionFragment_to_transactionDetailsFragment,
                    bundle
                )
            }
        })

    }

    private fun orderUpdateTips() {
        viewModel.data1.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {

                binding.root.showAlert(it.message)

            }
        })

        viewModel.data2.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {

                transactionAdapter.updateTip(selectedPos, it)
            }
        })

    }

    override fun onItemClickListener(view: View?, pos: Int) {

        selectedPos = pos
        singleTransaction = transactionAdapter.getItem(pos)

        val bundle = Bundle()
        bundle.putDouble("totalTip", singleTransaction!!.tips)
        singleTransaction?.amount?.let { bundle.putDouble("totalPrice", it) }
        findNavController().navigate(
            R.id.action_transactionFragment_to_addTipsDialog,
            bundle
        )
    }
}