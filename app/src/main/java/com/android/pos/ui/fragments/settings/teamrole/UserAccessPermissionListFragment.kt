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
import com.android.pos.databinding.FragmentTeamMemberTimeSheetBinding
import com.android.pos.databinding.FragmentUserAccessPermissionListBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UserAccessPermissionListFragment : Fragment() {

    private lateinit var binding: FragmentUserAccessPermissionListBinding
    private val viewModel by viewModels<UserPermissionListViewModel>()
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

        binding.txtAddNewRole.setOnClickListener {
            findNavController().navigate(
                R.id.action_userAccessPermissionListFragment_to_userAccessPermissionFragment
            )
        }
        return binding.root
    }

}