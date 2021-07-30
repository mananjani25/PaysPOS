package com.android.pos.ui.fragments.settings.teamrole

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.android.pos.R
import dagger.hilt.android.AndroidEntryPoint


/**
 * A simple [Fragment] subclass.
 * Use the [TeamMemberTimeSheetFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@AndroidEntryPoint
class TeamMemberTimeSheetFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_team_member_time_sheet, container, false)
    }

}