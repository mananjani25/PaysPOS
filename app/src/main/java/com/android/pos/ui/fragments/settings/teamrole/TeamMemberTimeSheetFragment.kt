package com.android.pos.ui.fragments.settings.teamrole

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.TimeSheetListModel
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
import kotlin.collections.ArrayList

@AndroidEntryPoint
class TeamMemberTimeSheetFragment : Fragment(), AdapterView.OnItemSelectedListener {

    private lateinit var binding: FragmentTeamMemberTimeSheetBinding
    private lateinit var teamMemberTimeSheetAdapter: TeamMemberTimeSheetAdapter
    private val viewModel by viewModels<TeamMemberSheetViewModel>()
    private lateinit var date: DatePickerDialog.OnDateSetListener
    val myCalendar = Calendar.getInstance()

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


        binding.llTestClick.setOnClickListener {
            findNavController().navigate(
                R.id.action_teamMemberTimeSheetFragment_to_singleTeamMemberTimeSheetFragment
            )
        }
        binding.lifecycleOwner = this


        date =
            DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                myCalendar.set(Calendar.YEAR, year)
                myCalendar.set(Calendar.MONTH, monthOfYear)
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                viewModel.updateLabel(myCalendar)
            }

        viewModel.setCurrentDate(myCalendar)

        binding.includeView.imgClose.setOnClickListener {
            backPressManage()
        }


        datePickerObserver()
        setUpRecyclerView()
        setUpSpinnerAdapter()
        getEmployeesTimeSheetObserver()
        setupSnackbar()
        observeShowProgress()

        binding.includeView.spRoles.onItemSelectedListener = this

        return binding.root
    }

    private fun setUpSpinnerAdapter() {
        val spinnerAdapter = ArrayAdapter<String>(
            requireActivity(),
            R.layout.row_spinner,
        )

        spinnerAdapter.setDropDownViewResource(R.layout.row_spinner)
        binding.includeView.spRoles.adapter = spinnerAdapter

        /*spinTerminal.setSelection(
                getIndex(
                    spinTerminal,
                    getTerminalId()!!,
                    terminalList
                )
            )*/
    }


    private fun setUpRecyclerView() {
        teamMemberTimeSheetAdapter = TeamMemberTimeSheetAdapter(viewModel)
        binding.rvTeamTimeSheet.adapter = teamMemberTimeSheetAdapter
    }

    private fun getEmployeesTimeSheetObserver() {
        viewModel.getEmployeesTimeSheet.observe(viewLifecycleOwner, {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.rvTeamTimeSheet.visibility = View.VISIBLE
                        resource.data?.let { timeSheet ->
                            teamMemberTimeSheetAdapter.teamTimesheetList(
                                timeSheet.data
                            )
                        }
                    }
                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.rvTeamTimeSheet.visibility = View.VISIBLE
                        binding.root.showAlert(resource.message)
                    }
                    Status.LOADING -> {
                        ProgressUtils.showProgressDialog(requireActivity())
                        binding.rvTeamTimeSheet.visibility = View.GONE
                    }
                }
            }
        })
    }

    private fun datePickerObserver() {
        viewModel.dateSelection.observe(requireActivity(), { event ->
            event.getContentIfNotHandled()?.let {

                DatePickerDialog(
                    requireActivity(), date, myCalendar
                        .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                    myCalendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
        })
    }


    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {

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
    /* private fun getIndex(
         spinner: Spinner,
         myString: String,
         empBean: List<TerminalResponse.Data?>?
     ): Int {
         for (i in 0 until spinner.count) {
             if (empBean?.get(i)?.id.toString() == myString) {
                 return i
             }
         }
         return 0
     }*/

}