package com.android.pos.ui.fragments.transactions

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.pos.R
import com.android.pos.data.entities.Employee
import com.android.pos.data.entities.TbOrderType
import com.android.pos.data.entities.TeamRole
import com.android.pos.data.model.responseModel.VenueDetailsResponse
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.FragmentTransactionBinding
import com.android.pos.ui.activities.MainActivity
import com.android.pos.ui.adapter.TransactionAdapter
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.statusUtils.Status
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import kotlin.collections.ArrayList

@AndroidEntryPoint
class TransactionFragment : Fragment(), AdapterView.OnItemSelectedListener {

    private lateinit var binding: FragmentTransactionBinding
    private lateinit var transactionAdapter: TransactionAdapter
    private val viewModel by viewModels<TransactionViewModel>()
    private lateinit var startDate: DatePickerDialog.OnDateSetListener
    private lateinit var endDate: DatePickerDialog.OnDateSetListener
    private lateinit var terminalListGlobal: ArrayList<VenueDetailsResponse.Data.Terminal>
    private lateinit var orderTypeListGlobal: ArrayList<TbOrderType>
    private lateinit var teamRoleListGlobal: ArrayList<TeamRole>
    private lateinit var teamEmployeeListGlobal: ArrayList<Employee>
    val myCalendar = Calendar.getInstance()
    val myCalendar1 = Calendar.getInstance()

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

        //remove afterwards
        //  viewModel.apiCallTimeSheet("")
        //  navigate()

        binding.includeView.spTerminals.onItemSelectedListener = this
        binding.includeView.spRoles.onItemSelectedListener = this
        binding.includeView.spEmployees.onItemSelectedListener = this
        binding.includeView.spOrders.onItemSelectedListener = this


        startDate = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            myCalendar.set(Calendar.YEAR, year)
            myCalendar.set(Calendar.MONTH, monthOfYear)
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            viewModel.updateLabel(myCalendar)
            viewModel.apiCallTimeSheet(
                getTerminalId(binding.includeView.spTerminals.selectedItemPosition).toString(),
                getRoleId(binding.includeView.spRoles.selectedItemPosition).toString(),
                getEmployeeId(binding.includeView.spEmployees.selectedItemPosition).toString(),
                getOrderId(binding.includeView.spOrders.selectedItemPosition).toString()
            )

        }

        endDate = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            myCalendar1.set(Calendar.YEAR, year)
            myCalendar1.set(Calendar.MONTH, monthOfYear)
            myCalendar1.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            viewModel.updateLabel(myCalendar1)
            viewModel.apiCallTimeSheet(
                getTerminalId(binding.includeView.spTerminals.selectedItemPosition).toString(),
                getRoleId(binding.includeView.spRoles.selectedItemPosition).toString(),
                getEmployeeId(binding.includeView.spEmployees.selectedItemPosition).toString(),
                getOrderId(binding.includeView.spOrders.selectedItemPosition).toString()
            )
        }


        viewModel.setCurrentDate(myCalendar)

        binding.includeView.imgDrawer.setOnClickListener {
            (requireActivity() as MainActivity).enableDrawer()
        }

        binding.includeView.txtTitle.text = getString(R.string.transactions)

        binding.includeView.txtHome.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_dashboardCategory)
        }
        return binding.root
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

    private fun getTerminalId(position: Int): Int? {
        return terminalListGlobal?.get(position)?.id
    }

    private fun getRoleId(position: Int): Int? {
        return teamRoleListGlobal?.get(position)?.id
    }

    private fun getEmployeeId(position: Int): Int? {
        return teamEmployeeListGlobal?.get(position)?.id
    }

    private fun getOrderId(position: Int): Int? {
        return orderTypeListGlobal?.get(position)?.id
    }


    private fun setUpRecyclerView() {
        binding.rvTeamTimeSheet.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayoutManager.VERTICAL
            )
        )
        transactionAdapter = TransactionAdapter(viewModel)
        binding.rvTeamTimeSheet.adapter = transactionAdapter
    }

    private fun getEmployeesTimeSheetObserver() {
        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { timeSheet ->
                binding.rvTeamTimeSheet.visibility = View.VISIBLE
                transactionAdapter.teamTimesheetList(
                    timeSheet.data.payments
                )

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
                            teamRoleListGlobal.add(0, TeamRole(-1, "All Roles", null, null))
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
                                )
                            )
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
                            orderTypeListGlobal.add(
                                0,
                                TbOrderType("", -1, false, -1, "All Orders", "", -1, "")
                            )
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
        viewModel.apiCallTimeSheet(
            getTerminalId(binding.includeView.spTerminals.selectedItemPosition).toString(),
            getRoleId(binding.includeView.spRoles.selectedItemPosition).toString(),
            getEmployeeId(binding.includeView.spEmployees.selectedItemPosition).toString(),
            getOrderId(binding.includeView.spOrders.selectedItemPosition).toString()
        )

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
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        })

    }


    /*private fun navigate() {

        viewModel.employeeIdViewModel.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { employeeModel ->
                val bundle = Bundle().apply {
                    putParcelable("employeeModel", employeeModel)
                }
                findNavController().navigate(
                    R.id.action_teamMemberTimeSheetFragment_to_singleTeamMemberTimeSheetFragment,
                    bundle
                )
            }
        })

    }*/
}