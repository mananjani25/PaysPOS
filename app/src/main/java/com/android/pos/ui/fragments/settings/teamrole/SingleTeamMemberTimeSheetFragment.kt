package com.android.pos.ui.fragments.settings.teamrole

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.pos.R
import com.android.pos.data.model.responseModel.GetEmployeesTimeSheetResponse
import com.android.pos.databinding.FragmentSingleTeamMemberTimeSheetBinding
import com.android.pos.ui.adapter.SingleTeamMemberTimeSheetAdapter
import dagger.hilt.android.AndroidEntryPoint
import java.util.*


@AndroidEntryPoint
class SingleTeamMemberTimeSheetFragment : Fragment() {

    private lateinit var employeeModel: GetEmployeesTimeSheetResponse.Data
    private lateinit var binding: FragmentSingleTeamMemberTimeSheetBinding
    private lateinit var teamMemberTimeSheetAdapter: SingleTeamMemberTimeSheetAdapter
    private val viewModel by viewModels<TeamMemberSheetViewModel>()
    private lateinit var startDate: DatePickerDialog.OnDateSetListener
    private lateinit var endDate: DatePickerDialog.OnDateSetListener
    val myCalendar = Calendar.getInstance()
    val myCalendar1 = Calendar.getInstance()

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

        binding.tvEmployeeName.text = employeeModel.teamName + " Time Sheet"
        binding.tvEmployeeId.text = "Employee ID: #" + employeeModel.teamId
        binding.includeView.spRoles.visibility = View.GONE
        binding.includeView.edtSearch.visibility = View.GONE
        binding.includeView.txtPrint.visibility = View.GONE


        binding.includeView.txtHome.setOnClickListener {

            findNavController().navigate(
                R.id.action_singleTeamMemberTimeSheetFragment_to_dashboardCategoryNew
            )
        }




        startDate =
            DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                myCalendar.set(Calendar.YEAR, year)
                myCalendar.set(Calendar.MONTH, monthOfYear)
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                viewModel.updateLabel(myCalendar)
                viewModel.apiCallTimeSheetDetails(employeeModel.teamId.toString())
            }

        endDate =
            DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                myCalendar1.set(Calendar.YEAR, year)
                myCalendar1.set(Calendar.MONTH, monthOfYear)
                myCalendar1.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                viewModel.updateLabel(myCalendar1)
                viewModel.apiCallTimeSheetDetails(employeeModel.teamId.toString())
            }

        viewModel.setCurrentDate(myCalendar)

        viewModel.apiCallTimeSheetDetails(employeeModel.teamId.toString())

        binding.includeView.imgClose.setOnClickListener {
            findNavController().navigateUp()
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
                    timeSheet.data
                )
                binding.tvEmployeeTotalHours.text =
                    "Employees Total Hours : " + timeSheet.employeTotalHours
                binding.tvEmployeeTotalWages.text =
                    "Employees Total Wages : " + timeSheet.employeeTotalWage
            }
        })

    }


}