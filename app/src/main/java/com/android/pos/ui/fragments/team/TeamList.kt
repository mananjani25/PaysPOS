package com.android.pos.ui.fragments.team

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.responseModel.EmployeeListResponse
import com.android.pos.databinding.FragmentTeamListBinding
import com.android.pos.ui.activities.MainActivity
import com.android.pos.ui.adapter.TeamsAdapter
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.callback.CustomCallback
import com.android.pos.utils.callback.OperationCallback
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.statusUtils.Status
import com.android.pos.utils.sticky_recycler.StickyHeaderLayoutManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TeamList : Fragment(), CustomCallback, OperationCallback {
    private lateinit var binding: FragmentTeamListBinding
    private val viewModel by viewModels<TeamListViewModel>()
    var adapter: TeamsAdapter = TeamsAdapter()
    private lateinit var empObject: EmployeeListResponse.Data.Employee

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
        observeShowProgress()
        loadTeamDetails(null)
        loadTeams()
        deleteEmployee()

        binding.layoutTool.imgOptionMenu.setOnClickListener {

            findNavController().navigate(R.id.action_global_createTeamMember)
        }


    }

    private fun deleteEmployee() {

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {

                AlertUtils.showAlert(requireActivity(), it.message)

                adapter.removeItem(empObject)


            }
        })
    }

    private fun setupStickyLayout() {
        val stickyHeaderLayoutManager = StickyHeaderLayoutManager()
        binding.rvEmployeeList.layoutManager = stickyHeaderLayoutManager
        adapter.setCallback(this)
        adapter.setOperationCallback(this)
        binding.rvEmployeeList.adapter = adapter

    }

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

    private fun loadTeams() {

        viewModel.employeeData.observe(viewLifecycleOwner, {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()

                        if (it.data != null && it.data.data.employees.isNotEmpty())
                            adapter.setPeople(it.data.data.employees as MutableList<EmployeeListResponse.Data.Employee>)
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


    private fun configureToolbar() {
        binding.layoutTool.txtTitle.text = "Team"
        binding.layoutTool.txtSubTitle.text = ""
        binding.layoutTool.txtEdit.visibility = View.GONE
        binding.layoutTool.imgDrawer.setOnClickListener {
            (requireActivity() as MainActivity).enableDrawer()
        }
        binding.layoutTool.imgOptionMenu.setImageDrawable(requireContext().resources.getDrawable(R.drawable.ic_add))
        binding.layoutTool.imgOptionMenuContainer.visibility = View.GONE

        binding.layoutTool.txtEdit.setOnClickListener {

            val bundle = bundleOf("data" to empObject)
            findNavController().navigate(R.id.action_global_createTeamMember, bundle)
        }
    }

    private fun loadTeamDetails(data: EmployeeListResponse.Data.Employee?) {

        val teamDetails = TeamDetails()

        val args = Bundle()
        args.putParcelable("data", data)
        teamDetails.arguments = args

        val fm: FragmentManager = requireActivity().supportFragmentManager
        fm.beginTransaction().replace(binding.frameContainer.id, teamDetails).commit()
    }

    override fun onItemClickListener(view: View?, data: EmployeeListResponse.Data.Employee) {

        Log.e("onItemClickListener", ">>>>")
        binding.layoutTool.txtEdit.visibility = View.VISIBLE

        if (data.firstName != null && data.lastName != null) {
            binding.layoutTool.txtSubTitle.text = data.firstName + " " + data.lastName.toString()
        } else {
            binding.layoutTool.txtSubTitle.text = data.firstName

        }
        empObject = data
        loadTeamDetails(data)
    }

    override fun onItemClickListener(employee: EmployeeListResponse.Data.Employee) {
        Log.e("onItem ", ">>>> ${employee.firstName}")

        empObject = employee
        viewModel.delete(employee.id)

    }

}