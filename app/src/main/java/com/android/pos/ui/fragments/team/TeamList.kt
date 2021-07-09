package com.android.pos.ui.fragments.team

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.android.pos.R
import com.android.pos.data.model.TeamListModel
import com.android.pos.databinding.FragmentTeamListBinding
import com.android.pos.ui.activities.MainActivity
import com.android.pos.ui.adapter.TeamListAdapter

class TeamList : Fragment() {
    private lateinit var binding: FragmentTeamListBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_team_list, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setAdapter()

        binding.layoutTool.imgDrawer.setOnClickListener {
            (requireActivity() as MainActivity).openDrawer()
        }


    }

    private fun setAdapter() {
        var list: ArrayList<TeamListModel> = arrayListOf()
        list.add(TeamListModel(0, "DM", "David Miller", "davidmiller@gmail.com"))
        list.add(TeamListModel(0, "DD", "Devin Doe", "devindoe@gmail.com"))
        list.add(TeamListModel(0, "KM", "Krisha Miller", "krishamiller@gmail.com"))
        list.add(TeamListModel(0, "RD", "Robert Doe", "robertdoe@gmail.com"))

        binding.rvEmployeeList.adapter = TeamListAdapter(requireContext(), list)
    }

}