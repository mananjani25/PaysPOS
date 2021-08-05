package com.android.pos.ui.fragments.settings.teamrole

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.android.pos.R
import com.android.pos.data.entities.TeamRole
import com.android.pos.data.model.responseModel.GetUserPermissionListResponse
import com.android.pos.databinding.FragmentUserAccessPermissionListBinding
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

@AndroidEntryPoint
class UserAccessPermissionListFragment : Fragment() {

    private var position: Int = -1
    private lateinit var binding: FragmentUserAccessPermissionListBinding
    private val viewModel by viewModels<UserAccessPermissionListViewModel>()
    private lateinit var userPermissionListAdapter: UserPermissionListAdapter
    private lateinit var userPermissionObject: TeamRole
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
        getUserPermissionListObserver()
        setupSnackbar()
        deleteTip()

        binding.txtAddNewRole.setOnClickListener {
            findNavController().navigate(
                R.id.action_userAccessPermissionListFragment_to_userAccessPermissionFragment
            )
        }
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

    private fun getUserPermissionListObserver() {
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

    private fun deleteTip() {

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

    private fun setupSnackbar() =
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

}