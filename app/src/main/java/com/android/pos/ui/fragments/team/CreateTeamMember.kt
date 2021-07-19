package com.android.pos.ui.fragments.team

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.databinding.FragmentCreateTeamMemberBinding
import com.android.pos.databinding.FragmentTeamDetailsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateTeamMember : Fragment() {

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    private lateinit var binding: FragmentCreateTeamMemberBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_create_team_member, container, false)
        binding.lifecycleOwner = this

        binding.imgClose.setOnClickListener {
            findNavController().navigateUp()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}