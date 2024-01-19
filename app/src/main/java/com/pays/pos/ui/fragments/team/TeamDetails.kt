package com.pays.pos.ui.fragments.team

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.data.entities.Employee
import com.pays.pos.data.remote.Constants
import com.pays.pos.databinding.FragmentTeamDetailsBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TeamDetails : Fragment() {

    private var model: Employee? = null
    private var count: Int? = null

    private val viewModel by viewModels<CreateTeamViewModel>()

    @Inject
    lateinit var prefProvider: PrefProvider
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
        binding.txtNodatavallidation?.text =
            "$count Employees that you manage at " + prefProvider.getValue(
                Constants.BUSINESS_NAME,
                ""
            )

        model?.teamRoleId?.let { viewModel.roleNameById(it) }?.observe(viewLifecycleOwner
        ) {
            binding.txtPermissionSet.text = it.data?.name
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bundle = this.arguments
        model = bundle?.getParcelable("data")
        model?.name?.let { LogUtil.logE("bundle", it) }
        count = bundle?.getInt("count")

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.txtEdit?.setOnClickListener {
            val bundle = bundleOf("data" to model)
            findNavController().navigate(R.id.action_global_createTeamMember, bundle)
        }
    }
}