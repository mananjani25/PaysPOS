package com.android.pos.ui.fragments.settings.teamrole

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.databinding.FragmentTeamMemberTimeSheetBinding
import com.android.pos.databinding.FragmentUserAccessPermissionBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UserAccessPermissionFragment : Fragment() {

    private lateinit var binding: FragmentUserAccessPermissionBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_user_access_permission,
                container,
                false
            )

        binding.lifecycleOwner = this

        return binding.root
    }

}