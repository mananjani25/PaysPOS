package com.pays.pos.ui.fragments.settings.teamrole

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.data.remote.Constants.TIME_TRACKER_ENABLED
import com.pays.pos.databinding.FragmentEmployeeBinding
import com.pays.pos.di.PrefProvider

import com.pays.pos.di.RolePermission
import com.pays.pos.utils.Pref
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TeamMemberSettings : Fragment() {
    private lateinit var binding: FragmentEmployeeBinding
    lateinit var prefProvider: PrefProvider

    @Inject
    lateinit var rolePermission: RolePermission

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
        prefProvider = PrefProvider(requireContext())


     //Hidden By Zeeshan
/*
        if (prefProvider.getValueboolean(TIME_TRACKER_ENABLED, false)) {
            binding.timeTracker.isChecked = true
        }
*/
        binding.timeTracker.isChecked = true //added by zeeshan

        binding.timeTracker.setOnClickListener {
            if (binding.timeTracker.isChecked) {
                prefProvider.setValueboolean(TIME_TRACKER_ENABLED, true)
            } else {
                prefProvider.setValueboolean(TIME_TRACKER_ENABLED, false)
            }
        }

        binding.llTimeTracking.setOnClickListener {
            if (rolePermission.hasEmployeeTimesheetPermission(binding.root)) {
                findNavController().navigate(R.id.action_settings_to_teamMemberTimeSheetFragment)
            }
        }

        binding.llUserAccessPermission.setOnClickListener {
            if (rolePermission.hasUserAccessPermission(binding.root)) {
                findNavController().navigate(R.id.action_settings_to_userAccessPermissionListFragment)
            }
        }
    }
}