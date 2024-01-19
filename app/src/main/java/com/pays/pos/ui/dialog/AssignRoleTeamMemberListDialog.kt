package com.pays.pos.ui.dialog

import android.graphics.Point
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.data.entities.Employee
import com.pays.pos.data.entities.TeamRole
import com.pays.pos.databinding.DialogAssignRoleEmployeeBinding
import com.pays.pos.ui.adapter.AssignRoleTeamMemberListAdapter
import com.pays.pos.ui.fragments.settings.teamrole.UserAccessPermissionViewModel
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.extensions.showAlert
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class AssignRoleTeamMemberListDialog : DialogFragment(), View.OnClickListener {
    private lateinit var adapter: AssignRoleTeamMemberListAdapter
    private lateinit var binding: DialogAssignRoleEmployeeBinding
    private val viewModel by viewModels<UserAccessPermissionViewModel>()
    var isEdit: Boolean = false
    private lateinit var employeeList: ArrayList<Employee>
    private lateinit var roleList: TeamRole
    var isSelectedAll = false
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
        binding.imgSelectall.setOnClickListener {
            if (!isSelectedAll) {
                for (i in adapter.filterList.indices) {
                    if (!adapter.filterList[i].isChecked) {
                        adapter.filterList[i].isChecked = true
                        adapter.selectedItemList.add(adapter.filterList[i])
                    }
                }
                binding.imgSelectall.setImageResource(R.drawable.ic_check_circle)
                isSelectedAll = true
            } else {
                for (i in adapter.filterList.indices) {
                    if (adapter.filterList[i].isChecked) {
                        adapter.filterList[i].isChecked = false
                        adapter.selectedItemList.remove(adapter.filterList[i])
                    }

                }
                binding.imgSelectall.setImageResource(R.drawable.ic_uncheck_circle)
                isSelectedAll = false
            }

            adapter.notifyDataSetChanged()
        }
        binding.searchEditEmployee.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                adapter.filter.filter(s.toString().trim())
            }

        })

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

        viewModel.getEmployeeListDatabse.observe(viewLifecycleOwner, {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.rvEmployeeList.visibility = View.VISIBLE
                        resource.data?.let { employeeList ->
                            if (employeeList.size > 0) {
                                binding.imgSelectall.visibility = View.VISIBLE
                            } else {
                                binding.imgSelectall.visibility = View.GONE
                            }
                            adapter.add(employeeList)
                            roleList = arguments?.getParcelable("teamRole")!!

                            adapter.selectedItemFromEdit(roleList.employees!!)

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