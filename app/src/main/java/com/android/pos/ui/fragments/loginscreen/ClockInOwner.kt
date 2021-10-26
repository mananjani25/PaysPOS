package com.android.pos.ui.fragments.loginscreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.USERNAME
import com.android.pos.databinding.FragmentClockInOwnerBinding
import com.android.pos.di.PrefProvider
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.TimeFormatUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ClockInOwner : Fragment() {
    private lateinit var binding: FragmentClockInOwnerBinding
    private val viewModel by viewModels<ClockInOwnerViewModel>()

    @Inject
    lateinit var prefProvider: PrefProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_clock_in_owner, container, false)
        binding.lifecycleOwner = this
        binding.clockInViewModel = viewModel

        observeShowProgress()
        navigate()

        binding.txtTitle.text = prefProvider.getValue(USERNAME, "")
        binding.txtClockInLabel.text =
            "Clocked in as " + prefProvider.getValue(Constants.EMPLOYEE_ROLE, "")
        binding.txtTime.text = TimeFormatUtils.showCurrentTime()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onClick()
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

    private fun navigate() {

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    findNavController().navigate(R.id.action_clockInOwner_to_scheduledShifts)
                }
            }
        })

    }

    private fun onClick() {

        binding.txtContinuePOS.setOnClickListener {
            findNavController().navigate(R.id.action_clockInOwner_to_dashboardCategory)
        }

    }
}