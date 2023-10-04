package com.android.pos.ui.fragments.team

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.Employee
import com.android.pos.databinding.FragmentTeamListBinding
import com.android.pos.di.RolePermission
import com.android.pos.ui.adapter.TeamsAdapter
import com.android.pos.utils.*
import com.android.pos.utils.callback.CustomCallback
import com.android.pos.utils.callback.OperationCallback
import com.android.pos.utils.extensions.alert
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.extensions.visible
import com.android.pos.utils.statusUtils.Status
import com.android.pos.utils.sticky_recycler.StickyHeaderLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class TeamList : Fragment(), CustomCallback, OperationCallback {
    private var deteleempObject: Employee? = null
    private var selectedPos: Int = -1
    private lateinit var binding: FragmentTeamListBinding
    private val viewModel by viewModels<TeamListViewModel>()
    var adapter: TeamsAdapter = TeamsAdapter()
    private var empObject: Employee? = null
    private var isEmptyString = true
    var count = 0
    var isFromSearch: Boolean = false
    @Inject
    lateinit var rolePermission: RolePermission
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_team_list, container, false)
        binding.lifecycleOwner = this

        setupStickyLayout()
        configureToolbar()
        observeShowProgress()

        loadTeams()
        deleteEmployee()

        LogUtil.logE("Calling", "onCreateView")

        setUpHeader()

        return binding.root
    }

    private fun setUpHeader() {
        binding.layoutTool.txtTimeSheet.visible()

        binding.layoutTool.txtTimeSheet.setOnClickListener {
            try {
                if (rolePermission.hasEmployeeTimesheetPermission(binding.root)) {
                    findNavController().navigate(R.id.action_teamList_to_teamMemberTimeSheetFragment)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        LogUtil.logE("Calling", "onViewCreated")

        binding.layoutTool.imgOptionMenu.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_global_createTeamMember)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        binding.layoutTool.txtHome.setOnClickListener {

            try {
                findNavController().navigate(R.id.action_teamList_to_dashboardCategoryNew)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        searchQuery()

    }

    override fun onResume() {
        super.onResume()
        LogUtil.logE("Calling", "onResume")
    }

    private fun deleteEmployee() {

        viewModel.data.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {

                AlertUtils.showCustomAlert(requireActivity(), it.message)

                empObject?.let { it1 -> adapter.removeItem(it1, requireActivity()) }

                if (adapter.getPeople()?.isNotEmpty() == true) {
                    empObject = adapter.getPeople()?.get(0)
                }
                loadTeamDetails (empObject)


            }
        }
    }

    private fun setupStickyLayout() {

        binding.rvEmployeeList.itemAnimator = null
        val stickyHeaderLayoutManager = StickyHeaderLayoutManager()
        binding.rvEmployeeList.layoutManager = stickyHeaderLayoutManager
        adapter.setCallback(this)
        adapter.setOperationCallback(this)
        binding.rvEmployeeList.adapter = adapter

    }

    private fun observeShowProgress() {

        viewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }

    }

    private fun loadTeams() {

        viewModel.employeeData().observe(viewLifecycleOwner) {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        if (!isFromSearch) {
                            ProgressUtils.dismissProgressDialog()
                        }

                        if (resource.data != null && resource.data.isNotEmpty())
                            binding.noEmployeeData?.visibility = View.GONE
                            adapter.setSelected(selectedPos)


                        count = resource.data?.size!!
                        adapter.setPeople(
                            resource.data as MutableList<Employee>,
                            requireActivity()
                        )

                        //  initially show first employee selected
                        selectedPos = adapter.getPeople()?.get(0)?.id!!
                        adapter.setSelected(selectedPos)

                        if (selectedPos != -1)
                            resource.data.forEach {
                                if (it.id == selectedPos) {
                                    loadTeamDetails(it)
                                    return@forEach
                                }

                            }


                        if (empObject != null) {
                            loadTeamDetails(empObject)
                        } else {
                            loadTeamDetails(null)
                        }
                    }
                    Status.ERROR -> {
                        if(!isFromSearch) {
                            ProgressUtils.dismissProgressDialog()
                        }
                        binding.root.showAlert(resource.message)

                    }
                    Status.LOADING -> {
                        if(!isFromSearch) {
                            ProgressUtils.showProgressDialog(requireActivity())
                        }
                    }
                }
            }
        }
    }


    private fun configureToolbar() {
        binding.layoutTool.txtTitle.text = "Employees"
        binding.layoutTool.txtSubTitle.text = ""
        binding.layoutTool.imgDrawer.setOnClickListener {
            findNavController().navigate(R.id.action_teamList_to_menupos)
        }
        binding.layoutTool.imgOptionMenu.setImageResource(R.drawable.ic_add)
        binding.layoutTool.imgOptionMenuContainer.visibility = View.GONE

    }

    private fun loadTeamDetails(data: Employee?) {


        if (data != null) {
            if (data.lastName != null && data.lastName.isNotEmpty() && !data.lastName.equals(
                    "null",
                    ignoreCase = true
                )
            ) { var final_string =
                data.firstName.toString().substring(0, 1)
                    .uppercase(Locale.getDefault()) + data.firstName.toString()
                    .substring(1, data.firstName.toString().length) + " " +
                        data.lastName.toString().substring(0, 1)
                            .uppercase(Locale.getDefault()) + data.lastName.toString()
                    .substring(1, data.lastName.toString().length)
                binding.layoutTool.txtSubTitle.setText(final_string)
            } else {
                var final_string =
                    data.firstName.toString().substring(0,1).uppercase(Locale.getDefault()) + data.firstName.toString()
                        .substring(1, data.firstName.toString().length)
                binding.layoutTool.txtSubTitle.setText(final_string)
            }

            empObject = data
        } else {
            binding.layoutTool.txtSubTitle.text = ""
        }

        val teamDetails = TeamDetails()

        val args = Bundle()
        args.putParcelable("data", data)
        args.putInt("count", count)
        teamDetails.arguments = args

        val fm: FragmentManager = requireActivity().supportFragmentManager
        fm.beginTransaction().replace(binding.frameContainer.id, teamDetails).commit()
    }

    override fun onItemClickListener(view: View?, data: Employee) {

        LogUtil.logE("onItemClickListener", ">>>>")
        MethodUtils.hideKeyboard(requireActivity())
        selectedPos = data.id
        empObject = data
        loadTeamDetails(empObject)
    }

    override fun onOptionClickListener(view: View?, viewHolder: TeamsAdapter.ItemViewHolder) {
        val popupMenu = view?.let { PopupMenu(requireContext(), it) }
        popupMenu?.menuInflater?.inflate(R.menu.edit_delete__hide_menu, popupMenu.menu)
        popupMenu?.menu?.findItem(R.id.menu_edit)?.isVisible = false
        popupMenu?.menu?.findItem(R.id.menu_hide)?.isVisible = false
        popupMenu?.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_delete -> {
                    alert(
                        getString(R.string.app_name),
                        getString(R.string.delete_employee_message)
                    ) {
                        positiveButton(getString(R.string.tv_delete)) {

                            deteleempObject =
                                viewHolder?.menuOption?.getTag(R.string.tv_order_id) as Employee
                            // Do positive stuff here
                            empObject =
                                viewHolder?.menuOption?.getTag(R.string.tv_order_id) as Employee

                            LogUtil.logE("Edit", empObject!!.id.toString())
                            viewModel.delete(empObject!!.id)
                        }
                        negativeButton(R.string.tv_cancel) {
                            // Do negative stuff here
                        }
                    }

                }
            }
            true
        }
        popupMenu?.show()
    }

    override fun onItemClickListener(employee: Employee) {
        LogUtil.logE("onItem ", ">>>> ${employee.firstName}")

        empObject = employee
        viewModel.delete(employee.id)

    }

    private fun searchQuery() {
        binding.autoSearch?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString() == " ") {
                    binding.autoSearch?.setText("")
                    isEmptyString = true
                }
            }

            override fun afterTextChanged(s: Editable?) {
                try {
                    if (s?.trim()?.isNotEmpty() == true) {
                        isEmptyString = false
                        searchByText(s.trim().toString())
                    } else {
                        if (!isEmptyString) {
                            isEmptyString = true
                            isFromSearch = true
                            loadTeams()
                        }
                 }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
    }

    fun searchByText(query: String) {
        viewModel.searchEmployees(query).observe(viewLifecycleOwner) {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        if (resource.data != null && resource.data.isNotEmpty()) {
                            binding.noEmployeeData?.visibility = View.GONE
                            adapter.setSelected(selectedPos)
                            count = resource.data.size
                            adapter.getList().clear()
                            adapter.setPeople(
                                resource.data as MutableList<Employee>,
                                requireActivity()
                            )
                            if (selectedPos != -1)
                                resource.data.forEach {
                                    if (it.id == selectedPos) {
                                        loadTeamDetails(it)
                                        return@forEach
                                    }
                                }
                            if (empObject != null) {
                                loadTeamDetails(empObject)
                            } else {
                                loadTeamDetails(null)
                            }
                        } else {
                            binding.noEmployeeData?.visibility = View.VISIBLE
                        }
                    }
                    Status.ERROR -> {
                        binding.root.showAlert(resource.message)
                    }
                    Status.LOADING -> {
                    }
                }
            }
        }
    }
}