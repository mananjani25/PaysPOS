package com.android.pos.ui.fragments.settings.teamrole

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.TimeSheetListModel
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.FragmentTeamMemberTimeSheetBinding
import com.android.pos.ui.adapter.TeamMemberTimeSheetAdapter
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import kotlin.collections.ArrayList

@AndroidEntryPoint
class TeamMemberTimeSheetFragment : Fragment() {

    private lateinit var binding: FragmentTeamMemberTimeSheetBinding
    private lateinit var teamMemberTimeSheetAdapter: TeamMemberTimeSheetAdapter
    val timeSheet = ArrayList<TimeSheetListModel>()
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

        timeSheet.clear()
        for (i in 1..5) {
            timeSheet.add(
                TimeSheetListModel(
                    "John Smith",
                    "08:30",
                    "$10.00",
                    "08:30",
                    "08:30",
                    "08:30",
                    "08:30",
                    "08:30",
                    "08:30",
                    "08:30"
                )
            )
        }
        teamMemberTimeSheetAdapter = TeamMemberTimeSheetAdapter(timeSheet)
        binding.rvTeamTimeSheet.adapter = teamMemberTimeSheetAdapter
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

        return binding.root
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

    private fun backPressManage() {
        val navController = findNavController()
        navController.previousBackStackEntry?.savedStateHandle?.set(
            Constants.KEY,
            Constants.TEAM_MEMBER
        )
        navController.popBackStack()
    }

}