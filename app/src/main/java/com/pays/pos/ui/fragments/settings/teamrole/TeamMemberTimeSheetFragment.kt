package com.pays.pos.ui.fragments.settings.teamrole

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.pays.pos.R
import com.pays.pos.data.entities.TeamRole
import com.pays.pos.data.remote.Constants
import com.pays.pos.databinding.FragmentTeamMemberTimeSheetBinding
import com.pays.pos.ui.adapter.TeamMemberTimeSheetAdapter
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.extensions.liveSnackBar
import com.pays.pos.utils.extensions.showAlert
import com.pays.pos.utils.statusUtils.Status
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@AndroidEntryPoint
class TeamMemberTimeSheetFragment : Fragment(), AdapterView.OnItemSelectedListener {

    private lateinit var binding: FragmentTeamMemberTimeSheetBinding
    private lateinit var teamMemberTimeSheetAdapter: TeamMemberTimeSheetAdapter
    private val viewModel by viewModels<TeamMemberSheetViewModel>()
    private lateinit var startDate: DatePickerDialog.OnDateSetListener
    private lateinit var endDate: DatePickerDialog.OnDateSetListener
    private lateinit var startTime: TimePickerDialog.OnTimeSetListener
    private lateinit var endTime: TimePickerDialog.OnTimeSetListener
    private var teamRoleListGlobal: ArrayList<TeamRole> = arrayListOf()
    val myCalendar = Calendar.getInstance()
    val myCalendar1 = Calendar.getInstance()
    val myCalendar2 = Calendar.getInstance()
    val myCalendar3 = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_team_member_time_sheet,
                container,
                false
            )

        binding.viewModel = viewModel


        binding.lifecycleOwner = this


        binding.includeView.imgClose.setOnClickListener {
            backPressManage()
        }

        binding.includeView.txtHome.setOnClickListener {
            try {
                findNavController().navigate(
                    R.id.action_teamMemberTimeSheetFragment_to_dashboardCategoryNew
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


        startDatePickerObserver()
        endDatePickerObserver()
        setUpRecyclerView()
        getEmployeesTimeSheetObserver()
        getRoleListObserver()
        setupSnackbar()
        observeShowProgress()
        navigate()

        binding.includeView.txtEmail.setOnClickListener {
            try {
                if (MethodUtils.isDoubleClick()) return@setOnClickListener
                val bundle = Bundle()
                bundle.putBoolean("isFromTimeSheet", true)
                bundle.putString("email", "")
                bundle.putInt("type", 2)
                findNavController().navigate(
                    R.id.action_teamMemberTimeSheetFragment_to_sendReceiptFragment,
                    bundle
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        setFragmentResultListener("request_key_timesheet") { _: String, bundle: Bundle ->
            viewModel.sendEmailTimeSheet( bundle.getString("email").toString(),"")

        }
        binding.includeView.spRoles.onItemSelectedListener = this


        binding.includeView.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.toString() == " ") {
                    binding.includeView.edtSearch.setText("")
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun afterTextChanged(s: Editable) {

                teamMemberTimeSheetAdapter.filter.filter(s.toString().lowercase().trim())

            }
        })

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
        startTime = TimePickerDialog.OnTimeSetListener { view, hour, minute ->
            val timecalender = Calendar.getInstance()
            timecalender.set(Calendar.HOUR_OF_DAY, hour)
            timecalender.set(Calendar.MINUTE, minute)
            viewModel.startDate.value = timeCalculateForStartEndTime(hour, minute, "isstart")
            if (differnceTrue(viewModel.startDate.value!!, viewModel.endDate.value) <= 30) {
                viewModel.apiCallTimeSheet(getRoleId(binding.includeView.spRoles.selectedItemPosition).toString())
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
            if (fromDate<=endDate) {
                val timecalender = Calendar.getInstance()
                timecalender.set(Calendar.HOUR_OF_DAY, hour)
                timecalender.set(Calendar.MINUTE, minute)
                viewModel.endDate.value = timeCalculateForStartEndTime(hour, minute, "isend")
                if (differnceTrue(viewModel.endDate.value!!, viewModel.startDate.value) <= 30) {
                    viewModel.apiCallTimeSheet(getRoleId(binding.includeView.spRoles.selectedItemPosition).toString())
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


        endDate = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            myCalendar1.set(Calendar.YEAR, year)
            myCalendar1.set(Calendar.MONTH, monthOfYear)
            myCalendar1.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            TimePickerDialog(
                requireActivity(),
                android.R.style.Theme_Material_Light_Dialog,
                endTime,
                myCalendar3.get(Calendar.HOUR),
                myCalendar3.get(Calendar.MINUTE),
                false
            ).show()
        }

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setCurrentDate(myCalendar)
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

    private fun startDatePickerObserver() {
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
    }

    private fun endDatePickerObserver() {
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
    }


    private fun setUpSpinnerAdapter(teamRoleList: ArrayList<String>) {
        val spinnerAdapter = ArrayAdapter(
            requireActivity(),
            R.layout.row_spinner,
            teamRoleList
        )

        spinnerAdapter.setDropDownViewResource(R.layout.row_spinner)
        binding.includeView.spRoles.adapter = spinnerAdapter

    }

    private fun getRoleId(position: Int): Int? {
        return teamRoleListGlobal?.get(position)?.id
    }


    private fun setUpRecyclerView() {

        binding.rvTeamTimeSheet.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayoutManager.VERTICAL
            )
        )

        teamMemberTimeSheetAdapter = TeamMemberTimeSheetAdapter(viewModel)
        binding.rvTeamTimeSheet.adapter = teamMemberTimeSheetAdapter
    }

    private fun getEmployeesTimeSheetObserver() {
        viewModel.data.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { timeSheet ->
                binding.rvTeamTimeSheet.visibility = View.VISIBLE
                teamMemberTimeSheetAdapter.teamTimesheetList(
                    timeSheet.data
                )

                binding.tvEmployeeTotalHours.text =
                    "All Employees Total Hours : " + timeSheet.employeTotalHours
                binding.tvEmployeeTotalWages.text =
                    "All Employees Total Wages : " + timeSheet.employeeTotalWage

            }
        }

    }

    private fun getRoleListObserver() {
        viewModel.getTeamRoleList.observe(viewLifecycleOwner) {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        resource.data?.let { roleList ->

                            teamRoleListGlobal.clear()
                            teamRoleListGlobal.add(0, TeamRole(-1, "All Roles", null, null))
                            teamRoleListGlobal.addAll(roleList as ArrayList<TeamRole>)
                            val roleName = teamRoleListGlobal.map { it.name }

                            setUpSpinnerAdapter(roleName as ArrayList<String>)

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
        }
    }


    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

        binding.includeView.edtSearch.setText("")
        viewModel.apiCallTimeSheet(teamRoleListGlobal[position].id.toString())
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

    private fun navigate() {

        viewModel.employeeIdViewModel.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { employeeModel ->
                try {
                    val bundle = Bundle().apply {
                        putParcelable("employeeModel", employeeModel)
                    }
                    activity?.let { MethodUtils.hideSoftKeyboard(it) }
                    findNavController().navigate(
                        R.id.action_teamMemberTimeSheetFragment_to_singleTeamMemberTimeSheetFragment,
                        bundle
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

    }
}