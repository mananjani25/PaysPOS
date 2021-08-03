package com.android.pos.ui.fragments.settings.teamrole

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.android.pos.R
import com.android.pos.data.model.SingleMemberTimeSheetListModel
import com.android.pos.databinding.FragmentSingleTeamMemberTimeSheetBinding
import com.android.pos.ui.adapter.SingleTeamMemberTimeSheetAdapter
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class SingleTeamMemberTimeSheetFragment : Fragment() {

    private lateinit var binding: FragmentSingleTeamMemberTimeSheetBinding
    private lateinit var teamMemberTimeSheetAdapter: SingleTeamMemberTimeSheetAdapter
    val timeSheet = ArrayList<SingleMemberTimeSheetListModel>()
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
        binding.lifecycleOwner = this
        return binding.root
    }

}