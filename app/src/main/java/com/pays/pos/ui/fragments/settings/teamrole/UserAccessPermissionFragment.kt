package com.pays.pos.ui.fragments.settings.teamrole

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.data.entities.Employee
import com.pays.pos.data.entities.ModulePermission
import com.pays.pos.data.entities.TeamRole
import com.pays.pos.databinding.FragmentUserAccessPermissionBinding

import com.pays.pos.di.RolePermission
import com.pays.pos.ui.adapter.*
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.extensions.liveSnackBar
import com.pays.pos.utils.extensions.showAlert
import com.pays.pos.utils.statusUtils.Status
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class UserAccessPermissionFragment : Fragment() {

    private lateinit var binding: FragmentUserAccessPermissionBinding
    private val viewModel by viewModels<UserAccessPermissionViewModel>()
    private lateinit var allSelectedTeamMemberAdapter: AllSelectedTeamMemberAdapter
    private lateinit var selectedTeamMemberAdapter: SelectedTeamMemberAdapter
    private lateinit var allPermissionModuleAdapter: AllPermissionModuleAdapter
    private lateinit var selectedPermissionModuleAdapter: SelectedPermissionModuleAdapter
    private lateinit var assignTeamListAdapter: AssignTeamListAdapter
    private lateinit var userPermissionObject: TeamRole
    var isEdit: Boolean = false
    private lateinit var employeeListGlobal: List<Employee>
    private var deliverModuleResponse: Boolean = false
    private var deliverRolesResponse: Boolean = false
    private var deliverEmployeesResponse: Boolean = false
    private val TAG = "UserAccessPermission"

    @Inject
    lateinit var rolePermission: RolePermission

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
        binding.viewModel = viewModel

        isEdit = arguments?.getBoolean("isEdit")!!

        setHeader()

        var roleLabel = getString(R.string.tv_add_new_role_name)
        if (isEdit) {
            binding.addRole.text = getString(R.string.update_role)
            userPermissionObject = arguments?.getParcelable("userPermissionObject")!!
            viewModel.setUserPermissionData(userPermissionObject)
            // viewModel.setModuleData(userPermissionObject)
            viewModel.isEditData(isEdit, userPermissionObject.id)

            //role
            if(rolePermission.isDefaultUserRoleWithoutAlert(userPermissionObject.name)){
                roleLabel = getString(R.string.role_name)
                binding.tvRoleName.isEnabled = false
            }
        }

        binding.tvRoleNameLabel.text = roleLabel

        setUpRecyclerView()
        setupSnackbar()
        loadTeams()
        setAllUser()
        setSelctedUser()
        setAllModule()
        setSelectedModule()
        navigate()
        observeShowProgress()
        showEmployeeListDialog()
        getUserPermissionListObserver()
        getModulesObserver()

        binding.header.imgBack.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.txtCancel.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.header.txtSave.setOnClickListener {
            findNavController().navigate(R.id.action_userAccessPermissionFragment_to_dashboardCategoryNew)
        }

        return binding.root
    }


    private fun setHeader() {
        binding.header.txtTitle.text=getString(R.string.tv_user_access_permission)
        binding.header.txtSave.text=getString(R.string.tv_home)
    }


    private fun setAllUser() {
        viewModel.allEmployeeList.observe(viewLifecycleOwner, {
            it as ArrayList<Employee>
            setEmployeeData(it)
        })
    }

    private fun setSelctedUser() {
        viewModel.selectedEmployeeList.observe(viewLifecycleOwner, {
            it as ArrayList<Employee>
            selectedTeamMemberAdapter.addEmployee(it)
        })
    }


    private fun setAllModule() {
        viewModel.allModuleList.observe(viewLifecycleOwner, {
            it as ArrayList<ModulePermission>
            allPermissionModuleAdapter.addPermissionModule(it)
        })
    }

    private fun setSelectedModule() {
        viewModel.selectedModuleList.observe(viewLifecycleOwner, {
            it as ArrayList<ModulePermission>
            selectedPermissionModuleAdapter.addPermissionModule(it)
        })
    }


    private fun setUpRecyclerView() {
        allSelectedTeamMemberAdapter = AllSelectedTeamMemberAdapter(viewModel)
        binding.rvAllMember.adapter = allSelectedTeamMemberAdapter

        selectedTeamMemberAdapter = SelectedTeamMemberAdapter(viewModel)
        binding.rvSelectedMember.adapter = selectedTeamMemberAdapter

        assignTeamListAdapter = AssignTeamListAdapter(viewModel)
        binding.rvAssignRole.adapter = assignTeamListAdapter

        allPermissionModuleAdapter = AllPermissionModuleAdapter(viewModel)
        binding.rvAllModule.adapter = allPermissionModuleAdapter


        selectedPermissionModuleAdapter = SelectedPermissionModuleAdapter(viewModel)
        binding.rvSelectedModule.adapter = selectedPermissionModuleAdapter
    }

    private fun loadTeams() {

        viewModel.employeeData.observe(viewLifecycleOwner) {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        LogUtil.logE(TAG, "deliverEmployeesResponse: $deliverEmployeesResponse")
                        deliverEmployeesResponse = true
                        manageProgress(false)
                        binding.rvAllMember.visibility = View.VISIBLE
                        resource.data?.let { employeeList ->
                            employeeListGlobal = employeeList
                            setEmployeeData(employeeListGlobal)
                            viewModel.setEmployeeList(employeeListGlobal)

                            setEditData()
                        }
                    }
                    Status.ERROR -> {
                        deliverEmployeesResponse = true
                        manageProgress(false)
                        binding.rvAllMember.visibility = View.VISIBLE
                        binding.root.showAlert(resource.message)

                    }
                    Status.LOADING -> {
                        manageProgress(true)
                        binding.rvAllMember.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun getModulesObserver() {
        viewModel.getTeamModules.observe(viewLifecycleOwner, {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        LogUtil.logE(TAG,"deliverModuleResponse: $deliverModuleResponse")
                        deliverModuleResponse = true
                        manageProgress(false)
                        binding.rvAllMember.visibility = View.VISIBLE
                        resource.data?.let { moduleList ->
                            allPermissionModuleAdapter.addPermissionModule(moduleList)
                            viewModel.setModuleList(moduleList)

                        }

                        setEditData()
                    }
                    Status.ERROR -> {
                        deliverModuleResponse = true
                        manageProgress(false)
                        binding.rvAllMember.visibility = View.VISIBLE
                        binding.root.showAlert(resource.message)

                    }
                    Status.LOADING -> {
                        manageProgress(true)
                        ProgressUtils.showProgressDialog(requireActivity())
                        binding.rvAllMember.visibility = View.GONE
                    }
                }
            }
        })
    }


    private fun getUserPermissionListObserver() {
        viewModel.getTeamRoleList.observe(viewLifecycleOwner, {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        LogUtil.logE(TAG,"deliverRolesResponse: $deliverRolesResponse")
                        deliverRolesResponse = true
                        manageProgress(false)
                        binding.rvAssignRole.visibility = View.VISIBLE
                        resource.data?.let { tipList ->
                            assignTeamListAdapter.addPermissionList(
                                tipList
                            )
                        }

                        setEditData()
                    }
                    Status.ERROR -> {
                        deliverRolesResponse = true
                        manageProgress(false)
                        binding.rvAssignRole.visibility = View.VISIBLE
                        binding.root.showAlert(resource.message)
                    }
                    Status.LOADING -> {
                        manageProgress(true)
                        binding.rvAssignRole.visibility = View.GONE
                    }
                }
            }
        })
    }

    private fun setEmployeeData(employeeList: List<Employee>) {
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
                        if (!isEdit) {
                            binding.tvRoleName.setText("")
                        }
                        findNavController().navigateUp()
                        //getUserPermissionListObserver()
                    }
                }

            }
        })
    }

    private fun observeShowProgress() {

        viewModel.showProgress.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                manageProgress(it)
            }
        })
    }

    private fun showEmployeeListDialog() {

        viewModel.showEmployeeListDialog.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { teamRole ->
                if (MethodUtils.isDoubleClick()) return@observe
                val bundle = Bundle()
                bundle.putBoolean("isEdit", true)
                bundle.putParcelable("teamRole", teamRole)
                findNavController().navigate(
                    R.id.action_userAccessPermissionFragment_to_assignTeamMemberListDialog,
                    bundle
                )

            }
        }
    }

    private fun manageProgress(show: Boolean) {
        if (show) {
            ProgressUtils.showProgressDialog(requireActivity())
        } else if (deliverModuleResponse && deliverRolesResponse && deliverEmployeesResponse) {
            //close progress after getting all responses
            ProgressUtils.dismissProgressDialog()
        }
    }

    private fun setEditData() {
        //load result after getting all responses
        if (isEdit && deliverEmployeesResponse && deliverModuleResponse && deliverModuleResponse) {
            userPermissionObject =
                arguments?.getParcelable("userPermissionObject")!!
            viewModel.setUserPermissionData(userPermissionObject)
            viewModel.setModuleData(userPermissionObject)
        }
    }

    private fun setupSnackbar() =
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

}