package com.android.pos.ui.fragments.settings.teamrole

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.TeamRole
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.FragmentUserAccessPermissionListBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.adapter.UserPermissionListAdapter
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.SwipeHelper
import com.android.pos.utils.extensions.alert
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.extensions.showAlert
import com.android.pos.utils.statusUtils.Status
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class UserAccessPermissionListFragment : Fragment() {

    private var position: Int = -1
    private lateinit var binding: FragmentUserAccessPermissionListBinding
    private val viewModel by viewModels<UserAccessPermissionListViewModel>()
    private lateinit var userPermissionListAdapter: UserPermissionListAdapter
    private lateinit var userPermissionObject: TeamRole

    @Inject
    lateinit var prefProvider: PrefProvider

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_user_access_permission_list,
            container,
            false
        )

        binding.lifecycleOwner = this

        setUpRecyclerView()
        getUserRoleListObserver()
        setupSnackbar()
        deleteEmployeeRole()
        observeShowProgress()

        binding.txtAddNewRole.setOnClickListener {
            //TODO user permission
            if (prefProvider.isManager()) {
                findNavController().navigate(
                    R.id.action_userAccessPermissionListFragment_to_userAccessPermissionFragment
                )
            }else{
                binding.root.showAlert("")
            }
        }

        binding.imgClose.setOnClickListener {
            backPressManage()
        }

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true /* enabled by default */) {
                override fun handleOnBackPressed() {
                    backPressManage()
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        return binding.root
    }


    private fun setUpRecyclerView() {
        userPermissionListAdapter = UserPermissionListAdapter()
        binding.rvUserPermissionList.adapter = userPermissionListAdapter

        object : SwipeHelper(activity, binding.rvUserPermissionList) {
            override fun instantiateUnderlayButton(
                viewHolder: RecyclerView.ViewHolder?,
                underlayButtons: MutableList<UnderlayButton?>
            ) {

                underlayButtons.add(UnderlayButton(
                    "Edit",
                    0,
                    Color.parseColor("#2997cc")
                ) { pos ->

                    userPermissionObject = userPermissionListAdapter.getItem(pos)
                    val bundle = Bundle()
                    bundle.putBoolean("isEdit", true)
                    bundle.putParcelable("userPermissionObject", userPermissionObject)

                    //     var bundle= bundleOf()
                    findNavController().navigate(
                        R.id.action_userAccessPermissionListFragment_to_userAccessPermissionFragment,
                        bundle
                    )

                })

                underlayButtons.add(UnderlayButton(
                    "Delete",
                    0,
                    Color.parseColor("#FF3C30")
                ) { pos ->

                    position = pos

                    alert(
                        getString(R.string.app_name),
                        getString(R.string.delete_employee_role_message)
                    ) {
                        positiveButton(getString(R.string.tv_delete)) {
                            // Do positive stuff here
                            userPermissionObject = userPermissionListAdapter.getItem(pos)
                            viewModel.delete(userPermissionListAdapter.getItem(pos).id)
                        }
                        negativeButton(R.string.tv_cancel) {
                            // Do negative stuff here
                        }
                    }
                })
            }
        }
    }

    private fun getUserRoleListObserver() {
        viewModel.getTeamRoleList.observe(viewLifecycleOwner, {
            it?.let { resource ->
                when (resource.status) {
                    Status.SUCCESS -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.rvUserPermissionList.visibility = View.VISIBLE
                        resource.data?.let { tipList -> setUserPermissionData(tipList) }
                    }
                    Status.ERROR -> {
                        ProgressUtils.dismissProgressDialog()
                        binding.rvUserPermissionList.visibility = View.VISIBLE
                        binding.root.showAlert(resource.message)
                    }
                    Status.LOADING -> {
                        ProgressUtils.showProgressDialog(requireActivity())
                        binding.rvUserPermissionList.visibility = View.GONE
                    }
                }
            }
        })
    }

    private fun deleteEmployeeRole() {

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                /* AlertUtils.showAlert(requireActivity(), it.message)
                 var adapter = binding.rvTaxList.adapter as TaxListAdapter
                 var list = adapter.taxList
                 list.remove(taxObject)
                 adapter.taxList = list
                 adapter.notifyDataSetChanged()*/

                AlertUtils.showCustomAlert(requireActivity(), it.message)
                /*tipListUpdateDelete.remove(tipObject)
                tipListadapter.addTips(tipListUpdateDelete)
                tipListadapter.notifyItemRemoved(position)
                tipListadapter.notifyItemRangeChanged(position, tipListUpdateDelete.size)*/

            }
        })

    }

    private fun setUserPermissionData(permissionList: List<TeamRole>) {
        userPermissionListAdapter.apply {
            addPermissionList(permissionList)
            notifyDataSetChanged()
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

    private fun backPressManage() {
        val navController = findNavController()
        navController.previousBackStackEntry?.savedStateHandle?.set(
            Constants.KEY,
            Constants.TEAM_MEMBER
        )
        navController.popBackStack()
    }

    private fun setupSnackbar() =
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

}