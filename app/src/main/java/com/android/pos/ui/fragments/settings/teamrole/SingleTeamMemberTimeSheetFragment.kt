package com.android.pos.ui.fragments.settings.teamrole

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import com.android.pos.R
import com.android.pos.data.model.SingleMemberTimeSheetListModel
import com.android.pos.databinding.FragmentSingleTeamMemberTimeSheetBinding
import com.android.pos.ui.adapter.SingleTeamMemberTimeSheetAdapter
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import kotlin.collections.ArrayList


@AndroidEntryPoint
class SingleTeamMemberTimeSheetFragment : Fragment() {

    private lateinit var binding: FragmentSingleTeamMemberTimeSheetBinding
    private lateinit var teamMemberTimeSheetAdapter: SingleTeamMemberTimeSheetAdapter
    val timeSheet = ArrayList<SingleMemberTimeSheetListModel>()
    private val viewModel by viewModels<TeamMemberSheetViewModel>()
    private lateinit var date: DatePickerDialog.OnDateSetListener
    val myCalendar = Calendar.getInstance()

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

        for (i in 1..5) {
            timeSheet.add(
                SingleMemberTimeSheetListModel(
                    "John Smith",
                    "08:30",
                    "$10.00",
                    "08:30",
                    "08:30",
                    "08:30"
                )
            )
        }

        teamMemberTimeSheetAdapter = SingleTeamMemberTimeSheetAdapter(timeSheet)
        binding.rvSingleTimeSheet.adapter = teamMemberTimeSheetAdapter

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        date =
            DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                myCalendar.set(Calendar.YEAR, year)
                myCalendar.set(Calendar.MONTH, monthOfYear)
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                viewModel.updateLabel(myCalendar)

            }

        viewModel.setCurrentDate(myCalendar)


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

}