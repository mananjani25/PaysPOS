package com.android.pos.ui.fragments.settings.teamrole

import android.app.DatePickerDialog
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
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.pos.R
import com.android.pos.data.entities.TeamRole
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.FragmentTeamMemberTimeSheetBinding
import com.android.pos.ui.adapter.TeamMemberTimeSheetAdapter
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.statusUtils.Status
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class TeamMemberTimeSheetFragment : Fragment(), AdapterView.OnItemSelectedListener {

    private lateinit var binding: FragmentTeamMemberTimeSheetBinding
    private lateinit var teamMemberTimeSheetAdapter: TeamMemberTimeSheetAdapter
    private val viewModel by viewModels<TeamMemberSheetViewModel>()
    private lateinit var startDate: DatePickerDialog.OnDateSetListener
    private lateinit var endDate: DatePickerDialog.OnDateSetListener
    private var teamRoleListGlobal: ArrayList<TeamRole> = arrayListOf()
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
                R.layout.fragment_team_member_time_sheet,
                container,
                false
            )

        binding.viewModel = viewModel


        binding.lifecycleOwner = this


        binding.includeView.imgClose.setOnClickListener {
            backPressManage()
        }


        startDatePickerObserver()
        endDatePickerObserver()
        setUpRecyclerView()
        getEmployeesTimeSheetObserver()
        getRoleListObserver()
        setupSnackbar()
        observeShowProgress()
        navigate()

        binding.includeView.spRoles.onItemSelectedListener = this

        binding.includeView.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun afterTextChanged(s: Editable) {

                teamMemberTimeSheetAdapter.filter.filter(s.toString().trim())

            }
        })

        startDate = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            myCalendar.set(Calendar.YEAR, year)
            myCalendar.set(Calendar.MONTH, monthOfYear)
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            viewModel.updateLabel(myCalendar)
            viewModel.apiCallTimeSheet(getRoleId(binding.includeView.spRoles.selectedItemPosition).toString())

        }

        endDate = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            myCalendar1.set(Calendar.YEAR, year)
            myCalendar1.set(Calendar.MONTH, monthOfYear)
            myCalendar1.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            viewModel.updateLabel(myCalendar1)
            viewModel.apiCallTimeSheet(getRoleId(binding.includeView.spRoles.selectedItemPosition).toString())

        }

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
        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { timeSheet ->
                binding.rvTeamTimeSheet.visibility = View.VISIBLE
                teamMemberTimeSheetAdapter.teamTimesheetList(
                    timeSheet.data
                )

                binding.tvEmployeeTotalHours.text =
                    "All Employees Total Hours:- " + timeSheet.employeTotalHours
                binding.tvEmployeeTotalWages.text =
                    "All Employees Total Wages:- " + timeSheet.employeeTotalWage

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
        })
    }


    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

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

    private fun navigate() {

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

    }
}