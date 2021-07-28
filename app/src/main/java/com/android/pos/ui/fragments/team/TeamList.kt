package com.android.pos.ui.fragments.team

import android.graphics.Color
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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.model.responseModel.EmployeeListResponse
import com.android.pos.databinding.FragmentTeamListBinding
import com.android.pos.ui.activities.MainActivity
import com.android.pos.ui.adapter.TeamsAdapter
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.SwipeHelperNew
import com.android.pos.utils.callback.CustomCallback
import com.android.pos.utils.callback.OperationCallback
import com.android.pos.utils.extensions.alert
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.statusUtils.Status
import com.android.pos.utils.sticky_recycler.StickyHeaderLayoutManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TeamList : Fragment(), CustomCallback, OperationCallback {
    private var selectedPos: Int = -1
    private lateinit var binding: FragmentTeamListBinding
    private val viewModel by viewModels<TeamListViewModel>()
    var adapter: TeamsAdapter = TeamsAdapter()
    private var empObject: EmployeeListResponse.Data.Employee? = null

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

        if (empObject != null) {
            loadTeamDetails(empObject)
        } else {
            loadTeamDetails(null)
        }
        loadTeams()
        deleteEmployee()

        Log.e("Calling", "onCreateView")

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        Log.e("Calling", "onViewCreated")

        binding.layoutTool.imgOptionMenu.setOnClickListener {

            findNavController().navigate(R.id.action_global_createTeamMember)
        }


    }

    override fun onResume() {
        super.onResume()
        Log.e("Calling", "onResume")
    }

    private fun deleteEmployee() {

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {

                AlertUtils.showCustomAlert(requireActivity(), it.message)

                empObject?.let { it1 -> adapter.removeItem(it1, requireActivity()) }


            }
        })
    }

    private fun setupStickyLayout() {

        binding.rvEmployeeList.itemAnimator = null
        val stickyHeaderLayoutManager = StickyHeaderLayoutManager()
        binding.rvEmployeeList.layoutManager = stickyHeaderLayoutManager
        adapter.setCallback(this)
        adapter.setOperationCallback(this)
        binding.rvEmployeeList.adapter = adapter


        object : SwipeHelperNew(activity, binding.rvEmployeeList) {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {

                Log.e("makeMovementFlags", viewHolder.itemView.tag.toString())
                if (viewHolder.itemView.tag.toString() == "header") {
                    return 0
                }
                return makeMovementFlags(0, ItemTouchHelper.LEFT)

            }

            override fun instantiateUnderlayButton(
                viewHolder: RecyclerView.ViewHolder?,
                underlayButtons: MutableList<UnderlayButton?>
            ) {

                underlayButtons.add(UnderlayButton(
                    "Delete",
                    0,
                    Color.parseColor("#FF3C30")
                ) { pos ->

                    alert(
                        getString(R.string.app_name),
                        getString(R.string.delete_employee_message)
                    ) {
                        positiveButton(getString(R.string.tv_delete)) {
                            // Do positive stuff here
                            empObject =
                                viewHolder?.itemView?.getTag(R.string.tv_order_id) as EmployeeListResponse.Data.Employee

                            Log.e("Edit", empObject!!.id.toString())
                            viewModel.delete(empObject!!.id)
                        }
                        negativeButton(R.string.tv_cancel) {
                            // Do negative stuff here
                        }
                    }


                })

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

    private fun loadTeams() {

        viewModel.employeeData().observe(viewLifecycleOwner, { it ->
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()

                        if (resource.data != null && resource.data.data.employees.isNotEmpty())

                            adapter.setSelected(selectedPos)
                        adapter.setPeople(
                            resource.data?.data?.employees as MutableList<EmployeeListResponse.Data.Employee>,
                            requireActivity()
                        )

                        if (selectedPos != -1)
                            resource.data.data.employees.forEach {
                                if (it.id == selectedPos) {
                                    loadTeamDetails(it)
                                    return@forEach
                                }

                            }

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
        binding.layoutTool.imgOptionMenu.setImageResource(R.drawable.ic_add)
        binding.layoutTool.imgOptionMenuContainer.visibility = View.GONE

        binding.layoutTool.txtEdit.setOnClickListener {

            val bundle = bundleOf("data" to empObject)
            findNavController().navigate(R.id.action_global_createTeamMember, bundle)
        }
    }

    private fun loadTeamDetails(data: EmployeeListResponse.Data.Employee?) {


        if (data != null) {
            binding.layoutTool.txtEdit.visibility = View.VISIBLE
            if (data.firstName != null && data.lastName != null) {
                binding.layoutTool.txtSubTitle.text =
                    data.firstName + " " + data.lastName.toString()
            } else {
                binding.layoutTool.txtSubTitle.text = data.firstName

            }
        }

        val teamDetails = TeamDetails()

        val args = Bundle()
        args.putParcelable("data", data)
        teamDetails.arguments = args

        val fm: FragmentManager = requireActivity().supportFragmentManager
        fm.beginTransaction().replace(binding.frameContainer.id, teamDetails).commit()
    }

    override fun onItemClickListener(view: View?, data: EmployeeListResponse.Data.Employee) {

        Log.e("onItemClickListener", ">>>>")
        selectedPos = data.id
        empObject = data
        loadTeamDetails(empObject)
    }

    override fun onItemClickListener(employee: EmployeeListResponse.Data.Employee) {
        Log.e("onItem ", ">>>> ${employee.firstName}")

        empObject = employee
        viewModel.delete(employee.id)

    }

}