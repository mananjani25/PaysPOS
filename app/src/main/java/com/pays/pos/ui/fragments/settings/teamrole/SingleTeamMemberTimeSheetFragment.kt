package com.pays.pos.ui.fragments.settings.teamrole

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.pays.pos.R
import com.pays.pos.data.model.responseModel.GetEmployeesTimeSheetResponse
import com.pays.pos.databinding.FragmentSingleTeamMemberTimeSheetBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.ui.adapter.SingleTeamMemberTimeSheetAdapter
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.TimeFormatUtils.prefProvider
import com.pays.pos.utils.extensions.liveSnackBar
import com.pays.pos.utils.statusUtils.Status
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.lang.Math.abs
import java.text.SimpleDateFormat
import java.util.*


@AndroidEntryPoint
class SingleTeamMemberTimeSheetFragment : Fragment() {

    private lateinit var employeeModel: GetEmployeesTimeSheetResponse.Data
    private lateinit var binding: FragmentSingleTeamMemberTimeSheetBinding
    private lateinit var teamMemberTimeSheetAdapter: SingleTeamMemberTimeSheetAdapter
    private val viewModel by viewModels<TeamMemberSheetViewModel>()
    private lateinit var startDate: DatePickerDialog.OnDateSetListener
    private lateinit var endDate: DatePickerDialog.OnDateSetListener
    private lateinit var startTime: TimePickerDialog.OnTimeSetListener
    private lateinit var endTime: TimePickerDialog.OnTimeSetListener
    val myCalendar = Calendar.getInstance()
    val myCalendar1 = Calendar.getInstance()
    val myCalendar2 = Calendar.getInstance()
    val myCalendar3 = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_single_team_member_time_sheet,
                container,
                false
            )
        employeeModel = arguments?.getParcelable("employeeModel")!!

        binding.viewModel = viewModel
        binding.lifecycleOwner = this


        startDatePickerObserver()
        endDatePickerObserver()

        setUpRecyclerView()
        getEmployeesTimeSheetDetailsObserver()
        setupSnackbar()

        binding.tvEmployeeName.text = employeeModel.teamName + " Time Sheet"
        binding.tvEmployeeId.text =
            "Employee ID: #" + employeeModel.teamId + " | " + "Employee Role: #" + employeeModel.teamRoleName
        binding.includeView.spinnerLayoutTimesheet.visibility = View.GONE
        binding.includeView.edtSearch.visibility = View.GONE
        binding.includeView.txtPrint.visibility = View.GONE

        binding.includeView.txtEmail.setOnClickListener {
            try {
                if (MethodUtils.isDoubleClick()) return@setOnClickListener
                viewModel.getEmployeeEmail(employeeModel.teamId)
                    .observe(viewLifecycleOwner) {

                        if (it.status == Status.SUCCESS) {
                            val bundle = Bundle()
                            bundle.putBoolean("isFromTimeSheet", true)
                            bundle.putInt("type", 2)
                            bundle.putString("email", it.data?.email)
                            findNavController().navigate(
                                R.id.action_singleteamMemberTimeSheetFragment_to_sendReceiptFragment,
                                bundle
                            )
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
        setFragmentResultListener("request_key_timesheet") { _: String, bundle: Bundle ->
            bundle.getString("email")
                ?.let { viewModel.sendEmailTimeSheet(it, employeeModel.teamId.toString()) }
        }

        binding.includeView.txtHome.setOnClickListener {
            try {
                findNavController().navigate(
                    R.id.action_singleTeamMemberTimeSheetFragment_to_dashboardCategoryNew
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }




        startDate =
            DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
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


//                viewModel.updateLabel(myCalendar)
//                viewModel.apiCallTimeSheetDetails(employeeModel.teamId.toString())
            }

        endDate =
            DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                myCalendar1.set(Calendar.YEAR, year)
                myCalendar1.set(Calendar.MONTH, monthOfYear)
                myCalendar1.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                TimePickerDialog(
                    requireActivity(),
                    android.R.style.Theme_Material_Light_Dialog,
                    endTime,
                    myCalendar1.get(Calendar.HOUR),
                    myCalendar1.get(Calendar.MINUTE),
                    false
                ).show()
//                viewModel.updateLabel(myCalendar1)
//                viewModel.apiCallTimeSheetDetails(employeeModel.teamId.toString())
            }


        startTime = TimePickerDialog.OnTimeSetListener { view, hour, minute ->
            val timecalender = Calendar.getInstance()
            timecalender.set(Calendar.HOUR_OF_DAY, hour)
            timecalender.set(Calendar.MINUTE, minute)
            viewModel.startDate.value = timeCalculateForStartEndTime(hour, minute, "isstart")
            if (differnceTrue(viewModel.startDate.value!!, viewModel.endDate.value) <= 30) {
                viewModel.apiCallTimeSheetDetails(employeeModel.teamId.toString())
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
                    viewModel.apiCallTimeSheetDetails(employeeModel.teamId.toString())
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

        viewModel.apiCallTimeSheetDetails(employeeModel.teamId.toString())

        binding.includeView.imgClose.setOnClickListener {
            findNavController().navigateUp()
        }

        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setCurrentDate(
            myCalendar
        )
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

    private fun setupSnackbar() =
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    private fun startDatePickerObserver() {
        viewModel.startDateSelection.observe(requireActivity(), { event ->
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

        })
    }

    private fun endDatePickerObserver() {
        viewModel.endDateSelection.observe(requireActivity(), { event ->
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
        })
    }


    private fun setUpRecyclerView() {
        binding.rvSingleTimeSheet.addItemDecoration(
            DividerItemDecoration(
                context,
                LinearLayoutManager.VERTICAL
            )
        )
        teamMemberTimeSheetAdapter = SingleTeamMemberTimeSheetAdapter()
        binding.rvSingleTimeSheet.adapter = teamMemberTimeSheetAdapter
    }

    private fun getEmployeesTimeSheetDetailsObserver() {
        viewModel.timeSheetDetails.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { timeSheet ->
                binding.rvSingleTimeSheet.visibility = View.VISIBLE
                binding.includeView.spinnerLayoutTimesheet.visibility = View.GONE
                teamMemberTimeSheetAdapter.teamTimesheetDetailsList(
                    timeSheet.data,
                    employeeModel.teamId
                )
                binding.tvEmployeeTotalHours.text =
                    "Employees Total Hours : " + timeSheet.employeTotalHours
                binding.tvEmployeeTotalWages.text =
                    "Employees Total Wages : " + timeSheet.employeeTotalWage
            }
        })

    }


}