package com.pays.pos.ui.fragments.settings.teamrole

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.pays.pos.R
import com.pays.pos.data.entities.TeamRole
import com.pays.pos.data.remote.Constants
import com.pays.pos.databinding.FragmentUserAccessPermissionListBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.di.RolePermission
import com.pays.pos.ui.adapter.UserPermissionListAdapter
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.SwipeHelper
import com.pays.pos.utils.callback.ItemCallback
import com.pays.pos.utils.extensions.alert
import com.pays.pos.utils.extensions.liveSnackBar
import com.pays.pos.utils.extensions.showAlert
import com.pays.pos.utils.statusUtils.Status
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class UserAccessPermissionListFragment : Fragment(), ItemCallback {

    private var position: Int = -1
    private lateinit var binding: FragmentUserAccessPermissionListBinding
    private val viewModel by viewModels<UserAccessPermissionListViewModel>()
    private lateinit var userPermissionListAdapter: UserPermissionListAdapter
    private lateinit var userPermissionObject: TeamRole

    @Inject
    lateinit var prefProvider: PrefProvider

    @Inject
    lateinit var rolePermission: RolePermission

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
        setHeader()

        setUpRecyclerView()
        getUserRoleListObserver()
        setupSnackbar()
        deleteEmployeeRole()
        observeShowProgress()

        binding.txtAddNewRole.setOnClickListener {
            findNavController().navigate(
                R.id.action_userAccessPermissionListFragment_to_userAccessPermissionFragment
            )
        }

        binding.header.imgBack.setOnClickListener {
            backPressManage()
        }
        binding.header.txtSave.setOnClickListener {
            findNavController().navigate(R.id.action_userAccessPermissionListFragment_to_dashboardCategory)
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

    private fun setHeader() {
        binding.header.txtTitle.text = getString(R.string.tv_user_access_permission)
        binding.header.txtSave.text = getString(R.string.tv_home)
    }


    private fun setUpRecyclerView() {
        userPermissionListAdapter = UserPermissionListAdapter()
        binding.rvUserPermissionList.adapter = userPermissionListAdapter

        userPermissionListAdapter.setCallback(this)
/*
        object : SwipeHelper(activity, binding.rvUserPermissionList) {
            override fun instantiateUnderlayButton(
                viewHolder: RecyclerView.ViewHolder?,
                underlayButtons: MutableList<UnderlayButton?>
            ) {

                underlayButtons.add(UnderlayButton(
                    "Edit",
                    ContextCompat.getColor(context, R.color.swipe_text_color),
                    ContextCompat.getColor(context, R.color.swipe_bg_edit)
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
                    ContextCompat.getColor(context, R.color.swipe_text_color),
                    ContextCompat.getColor(context, R.color.swipe_bg_delete)
                ) { pos ->

                    position = pos
                    if (rolePermission.isDefaultUserRole(userPermissionListAdapter.getItem(pos).name.trim(), binding.root)) {
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
                    }
                })
            }
        }
*/
    }

    private fun getUserRoleListObserver() {
        viewModel.getTeamRoleList.observe(viewLifecycleOwner) {
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
        }
    }

    private fun deleteEmployeeRole() {

        viewModel.data.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                AlertUtils.showCustomAlert(requireActivity(), it.message)
            }
        }

    }

    private fun setUserPermissionData(permissionList: List<TeamRole>) {
        userPermissionListAdapter.apply {
            addPermissionList(permissionList)
            notifyDataSetChanged()
        }
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

    override fun onItemClickListener(view: View?, pos: Int) {
        val popupMenu = view?.let { PopupMenu(requireContext(), it) }
        popupMenu?.menuInflater?.inflate(R.menu.edit_delete_menu, popupMenu.menu)
        popupMenu?.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_edit -> {
                    userPermissionObject = userPermissionListAdapter.getItem(pos)
                    val bundle = Bundle()
                    bundle.putBoolean("isEdit", true)
                    bundle.putParcelable("userPermissionObject", userPermissionObject)

                    //     var bundle= bundleOf()
                    findNavController().navigate(
                        R.id.action_userAccessPermissionListFragment_to_userAccessPermissionFragment,
                        bundle
                    )
                }
                R.id.menu_delete -> {
                    activity?.let {
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
                    }
                }
            }
            true
        }
        popupMenu?.show()
    }

}