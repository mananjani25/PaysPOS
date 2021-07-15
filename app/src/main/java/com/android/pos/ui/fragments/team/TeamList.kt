package com.android.pos.ui.fragments.team

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.TeamListModel
import com.android.pos.data.model.responseModel.EmployeeResponse
import com.android.pos.databinding.FragmentTeamListBinding
import com.android.pos.ui.activities.MainActivity
import com.android.pos.ui.adapter.TeamListAdapter
import com.android.pos.ui.adapter.TeamsAdapter
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.callback.CustomCallback
import com.android.pos.utils.statusUtils.Status
import com.android.pos.utils.sticky_recycler.StickyHeaderLayoutManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TeamList : Fragment(), CustomCallback {
    private lateinit var binding: FragmentTeamListBinding
    private val viewModel by viewModels<TeamListViewModel>()
    var adapter: TeamsAdapter = TeamsAdapter()

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


        setupStickyLayout()

        configureToolbar()
//        setAdapter()
        loadTeamDetails()

        loadTeams()

        binding.layoutTool.imgOptionMenu.setOnClickListener {
            findNavController().navigate(R.id.action_global_createTeamMember)
        }


    }

    private fun setupStickyLayout() {
        val stickyHeaderLayoutManager = StickyHeaderLayoutManager()
        binding.rvEmployeeList.layoutManager = stickyHeaderLayoutManager
        binding.rvEmployeeList.adapter = adapter
        adapter.setCallback(this)
    }

    private fun loadTeams() {

        viewModel.employeeData.observe(viewLifecycleOwner, {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        it.data?.data?.get(0)?.let { it1 -> Log.e("SUCCESS", it1.name) }

                        if (it.data != null && it.data.data.isNotEmpty())
                            adapter.setPeople(it.data.data as MutableList<EmployeeResponse.Data>)
                    }
                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()

                    }
                    Status.LOADING -> {
                        ProgressUtils.showProgressDialog(requireActivity())
                    }
                }
            }
        })
    }


    private fun configureToolbar() {
        binding.layoutTool.txtTitle.text = "Team"
        binding.layoutTool.imgDrawer.setOnClickListener {
            (requireActivity() as MainActivity).enableDrawer()
        }
        binding.layoutTool.imgOptionMenu.setImageDrawable(requireContext().resources.getDrawable(R.drawable.ic_add))
        binding.layoutTool.imgOptionMenuContainer.visibility = View.GONE
    }

    private fun loadTeamDetails(data: EmployeeResponse.Data) {

        val teamDetails = TeamDetails()

        val args = Bundle()
        args.putParcelable("data", data)
        teamDetails.arguments = args

        val fm: FragmentManager = requireActivity().supportFragmentManager
        fm.beginTransaction().replace(binding.frameContainer.id, teamDetails).commit()
    }

    private fun loadTeamDetails() {

        val teamDetails = TeamDetails()
        val fm: FragmentManager = requireActivity().supportFragmentManager
        fm.beginTransaction().replace(binding.frameContainer.id, teamDetails).commit()
    }

    private fun setAdapter() {


        val list: ArrayList<TeamListModel> = arrayListOf()
        list.add(TeamListModel(0, "D", "", "", true))
        list.add(TeamListModel(0, "DM", "David Miller", "davidmiller@gmail.com"))
        list.add(TeamListModel(0, "DD", "Devin Doe", "devindoe@gmail.com"))
        list.add(TeamListModel(0, "K", "", "", true))
        list.add(TeamListModel(0, "KM", "Krisha Miller", "krishamiller@gmail.com"))
        list.add(TeamListModel(0, "R", "", "", true))
        list.add(TeamListModel(0, "RD", "Robert Doe", "robertdoe@gmail.com"))

        binding.rvEmployeeList.adapter = TeamListAdapter(requireContext(), list)
    }

    override fun onItemClickListener(view: View?, data: EmployeeResponse.Data) {

        Log.e("onItemClickListener", ">>>>")

        loadTeamDetails(data)
    }

}