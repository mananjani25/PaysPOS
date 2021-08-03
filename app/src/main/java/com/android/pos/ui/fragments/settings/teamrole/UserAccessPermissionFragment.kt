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
import com.android.pos.data.model.responseModel.EmployeeListResponse
import com.android.pos.databinding.FragmentTeamMemberTimeSheetBinding
import com.android.pos.databinding.FragmentUserAccessPermissionBinding
import com.android.pos.ui.adapter.SelectedTeamMemberAdapter
import com.android.pos.ui.adapter.SelectedTeamMemberAdapter1
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
    private lateinit var selectedTeamMemberAdapter: SelectedTeamMemberAdapter
    private lateinit var selectedTeamMemberAdapter1: SelectedTeamMemberAdapter1
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

        binding.lifecycleOwner = this

        setUpRecyclerView()
        setupSnackbar()
        loadTeams()

        return binding.root
    }


    private fun setUpRecyclerView() {
        selectedTeamMemberAdapter = SelectedTeamMemberAdapter()
        binding.rvSelectedMember.adapter = selectedTeamMemberAdapter

        /*selectedTeamMemberAdapter1 = SelectedTeamMemberAdapter1()
        binding.rvSelectedMember1.adapter = selectedTeamMemberAdapter1*/
    }

    private fun loadTeams() {

        viewModel.employeeData().observe(viewLifecycleOwner, {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.rvSelectedMember.visibility = View.VISIBLE
                        resource.data?.let { employeeList -> setEmployeeData(employeeList) }

                    }
                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.rvSelectedMember.visibility = View.VISIBLE
                        binding.root.showAlert(resource.message)

                    }
                    Status.LOADING -> {
                        ProgressUtils.showProgressDialog(requireActivity())
                        binding.rvSelectedMember.visibility = View.GONE
                    }
                }
            }
        })
    }

    private fun setEmployeeData(employeeList: List<EmployeeListResponse.Data.Employee>) {
        selectedTeamMemberAdapter.apply {
            addEmployee(employeeList)
            notifyDataSetChanged()
        }
    }


    private fun setupSnackbar() =
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

}