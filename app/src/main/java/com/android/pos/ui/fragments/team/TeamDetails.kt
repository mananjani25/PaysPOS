package com.android.pos.ui.fragments.team

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.android.pos.R
import com.android.pos.data.entities.Employee
import com.android.pos.databinding.FragmentTeamDetailsBinding
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.MethodUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TeamDetails : Fragment() {

    private var model: Employee? = null

    private val viewModel by viewModels<CreateTeamViewModel>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    private lateinit var binding: FragmentTeamDetailsBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_team_details, container, false)
        binding.lifecycleOwner = this

        setupView()
        return binding.root
    }

    private fun setupView() {

        if (model == null) {
            binding.layout.visibility = View.VISIBLE
            binding.nestedScrollView.visibility = View.GONE
        } else {
            binding.layout.visibility = View.GONE
            binding.nestedScrollView.visibility = View.VISIBLE
        }

        binding.txtFirstName.text = model?.firstName
        binding.txtLastName.text = model?.lastName
        binding.txtEmail.text = model?.email
        binding.txtPhone.text = model?.phoneNumber?.let { AlertUtils.usNumberFormat(it) }
        binding.txtPersonalPasscode.text = model?.passcode

        model?.hourlyWages?.let { MethodUtils.setPriceTextView(binding.txtHourlyRate, it) }

        binding.txtPersonalPasscode.text = model?.passcode

        model?.teamRoleId?.let { viewModel.roleNameById(it) }?.observe(viewLifecycleOwner,
            {
                binding.txtPermissionSet.text = it.data?.name
            })

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bundle = this.arguments
        model = bundle?.getParcelable("data")
        model?.name?.let { Log.e("bundle", it) }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}