package com.android.pos.ui.fragments.settings.teamrole

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.PermissionModuleListModel
import com.android.pos.data.model.responseModel.EmployeeListResponse
import com.android.pos.databinding.FragmentUserAccessPermissionBinding
import com.android.pos.ui.adapter.AllPermissionModuleAdapter
import com.android.pos.ui.adapter.AllSelectedTeamMemberAdapter
import com.android.pos.ui.adapter.SelectedPermissionModuleAdapter
import com.android.pos.ui.adapter.SelectedTeamMemberAdapter
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.statusUtils.Status
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UserAccessPermissionFragment : Fragment() {

    private lateinit var binding: FragmentUserAccessPermissionBinding
    private val viewModel by viewModels<UserPermissionViewModel>()
    private lateinit var allSelectedTeamMemberAdapter: AllSelectedTeamMemberAdapter
    private lateinit var selectedTeamMemberAdapter: SelectedTeamMemberAdapter
    private lateinit var allPermissionModuleAdapter: AllPermissionModuleAdapter
    private lateinit var selectedPermissionModuleAdapter: SelectedPermissionModuleAdapter

    val timeSheet = ArrayList<PermissionModuleListModel>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_user_access_permission,
                container,
                false
            )

        binding.viewModel = viewModel
        binding.lifecycleOwner = this


        setUpRecyclerView()
        setupSnackbar()
        loadTeams()
        setAllUser()
        setSelctedUser()
        setAllModule()
        setSelectedModule()
        navigate()
        observeShowProgress()

        return binding.root
    }


    private fun setAllUser() {
        viewModel.allEmployeeList.observe(viewLifecycleOwner, {
            it as ArrayList<EmployeeListResponse.Data.Employee>
            setEmployeeData(it)
        })
    }

    private fun setSelctedUser() {
        viewModel.selectedEmployeeList.observe(viewLifecycleOwner, {
            it as ArrayList<EmployeeListResponse.Data.Employee>
            selectedTeamMemberAdapter.addEmployee(it)
        })
    }


    private fun setAllModule() {
        viewModel.allModuleList.observe(viewLifecycleOwner, {
            it as ArrayList<PermissionModuleListModel>
            allPermissionModuleAdapter.addPermissionModule(timeSheet)
        })
    }

    private fun setSelectedModule() {
        viewModel.selectedModuleList.observe(viewLifecycleOwner, {
            it as ArrayList<PermissionModuleListModel>
            selectedPermissionModuleAdapter.addPermissionModule(timeSheet)
        })
    }


    private fun setUpRecyclerView() {
        allSelectedTeamMemberAdapter = AllSelectedTeamMemberAdapter(viewModel)
        binding.rvAllMember.adapter = allSelectedTeamMemberAdapter

        selectedTeamMemberAdapter = SelectedTeamMemberAdapter(viewModel)
        binding.rvSelectedMember.adapter = selectedTeamMemberAdapter


        for (i in 1..5) {
            timeSheet.add(
                PermissionModuleListModel(
                    "Module$i"
                )
            )
        }

        viewModel.setModuleList(timeSheet)

        allPermissionModuleAdapter = AllPermissionModuleAdapter(viewModel)
        binding.rvAllModule.adapter = allPermissionModuleAdapter

        allPermissionModuleAdapter.addPermissionModule(timeSheet)


        selectedPermissionModuleAdapter = SelectedPermissionModuleAdapter(viewModel)
        binding.rvSelectedModule.adapter = selectedPermissionModuleAdapter
    }

    private fun loadTeams() {

        viewModel.employeeData().observe(viewLifecycleOwner, {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.rvAllMember.visibility = View.VISIBLE
                        resource.data?.let { employeeList ->
                            setEmployeeData(employeeList)
                            viewModel.setEmployeeList(employeeList)
                        }
                    }
                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.rvAllMember.visibility = View.VISIBLE
                        binding.root.showAlert(resource.message)

                    }
                    Status.LOADING -> {
                        ProgressUtils.showProgressDialog(requireActivity())
                        binding.rvAllMember.visibility = View.GONE
                    }
                }
            }
        })
    }

    private fun setEmployeeData(employeeList: List<EmployeeListResponse.Data.Employee>) {
        allSelectedTeamMemberAdapter.apply {
            addEmployee(employeeList)
            notifyDataSetChanged()
        }
    }

    private fun navigate() {

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { createRoleResponse ->
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, createRoleResponse.message
                    ) { _, _ ->
                        findNavController().navigateUp()
                    }
                }

            }
        })
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

    private fun setupSnackbar() =
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

}