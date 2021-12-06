package com.android.pos.ui.fragments.settings.teamrole

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.databinding.FragmentEmployeeBinding
import com.android.pos.di.RolePermission
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TeamMemberSettings : Fragment() {
    private lateinit var binding: FragmentEmployeeBinding

    @Inject
    lateinit var rolePermission : RolePermission

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEmployeeBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.llTimeTracking.setOnClickListener {
            if (rolePermission.hasEmployeeTimesheetPermission(binding.root)) {
                findNavController().navigate(R.id.action_settings_to_teamMemberTimeSheetFragment)
            }
        }

        binding.llUserAccessPermission.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_userAccessPermissionListFragment)
        }
    }
}