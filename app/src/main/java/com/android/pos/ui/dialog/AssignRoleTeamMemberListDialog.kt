package com.android.pos.ui.dialog

import android.graphics.Point
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.Employee
import com.android.pos.data.entities.TeamRole
import com.android.pos.databinding.DialogAssignRoleEmployeeBinding
import com.android.pos.ui.adapter.AssignRoleTeamMemberListAdapter
import com.android.pos.ui.fragments.settings.teamrole.UserAccessPermissionViewModel
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class AssignRoleTeamMemberListDialog : DialogFragment(), View.OnClickListener {
    private lateinit var adapter: AssignRoleTeamMemberListAdapter
    private lateinit var binding: DialogAssignRoleEmployeeBinding
    private val viewModel by viewModels<UserAccessPermissionViewModel>()
    var isEdit: Boolean = false
    private lateinit var employeeList: ArrayList<Employee>
    private lateinit var roleList: TeamRole

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.dialog_assign_role_employee,
            container,
            false
        )
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        isEdit = arguments?.getBoolean("isEdit")!!

        binding.imgBack.setOnClickListener(this)
        binding.txtCancel.setOnClickListener(this)
        binding.tvAssignRole.setOnClickListener(this)

        setAdapter()
        observeShowProgress()
        navigate()
        loadTeams()

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        val window: Window? = dialog!!.window
        val size = Point()
        val display: Display = window?.windowManager?.defaultDisplay!!
        display.getSize(size)
        val width: Int = size.x
        window.setLayout((width * 0.50).toInt(), WindowManager.LayoutParams.MATCH_PARENT)
        window.setGravity(Gravity.CENTER)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }


    private fun setAdapter() {
        adapter = AssignRoleTeamMemberListAdapter()
        binding.rvEmployeeList.adapter = adapter


        binding.edtSearch.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            }

            override fun afterTextChanged(s: Editable) {

                adapter.filter.filter(s.toString().trim())

            }
        })

    }

    override fun onClick(v: View?) {


        when (v?.id) {
            R.id.imgBack -> {
                dismiss()
            }
            R.id.txtCancel -> {
                findNavController().navigateUp()
            }
            R.id.tvAssignRole -> {
                viewModel.assignRole(adapter.selectedItemList(), roleList)
            }

        }
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

    private fun navigate() {

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { createRoleResponse ->
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, createRoleResponse.message
                    ) { _, _ ->
                        dismiss()
                    }
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
                        binding.rvEmployeeList.visibility = View.VISIBLE
                        resource.data?.let { employeeList ->
                            adapter.add(employeeList)
                            roleList = arguments?.getParcelable("teamRole")!!
                            adapter.selectedItemFromEdit(roleList.employees)

                        }
                    }
                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.rvEmployeeList.visibility = View.VISIBLE
                        binding.root.showAlert(resource.message)

                    }
                    Status.LOADING -> {
                        ProgressUtils.showProgressDialog(requireActivity())
                        binding.rvEmployeeList.visibility = View.GONE
                    }
                }
            }
        })
    }

}