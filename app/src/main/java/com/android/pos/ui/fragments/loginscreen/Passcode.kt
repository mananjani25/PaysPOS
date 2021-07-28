package com.android.pos.ui.fragments.loginscreen

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.MainApplication
import com.android.pos.R
import com.android.pos.databinding.FragmentPasscodeBinding
import com.android.pos.ui.activities.MainActivity
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.liveSnackBar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class Passcode : Fragment() {

    private lateinit var binding: FragmentPasscodeBinding
    private val viewModel by viewModels<PasscodeViewModel>()
    var isDashboard: Boolean = false
    var isClockOut: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true /* enabled by default */) {
                override fun handleOnBackPressed() {
                    if (isClockOut) {
                        (requireActivity() as MainActivity).finish()

                    } else {
                        findNavController().navigateUp()

                    }
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_passcode, container, false)
        binding.lifecycleOwner = this
        binding.passcodeViewModel = viewModel
        isDashboard = arguments?.getBoolean("isDashboard")!!

        if (isDashboard) {
            binding.tvWelcomeTag.text = getString(R.string.tv_clock_out)
            viewModel.isDashboardData(isDashboard)
        }

        setupSnackbar()
        observeShowProgress()
        navigate()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.passCodeView.setKeyTextColor(resources.getColor(R.color.white))

        val typeface: Typeface? =
            ResourcesCompat.getFont(requireActivity(), R.font.sf_pro_display_regular)
        binding.passCodeView.setTypeFace(typeface)

        binding.passCodeView.setOnTextChangeListener {
            if (it.length == 4) {

                Log.e("passCodeView", it.toString())
                viewModel.submit(it.toString())

            }
        }

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
                if (isDashboard) {
                    isDashboard = false
                    isClockOut = true
                    viewModel.isDashboardData(isDashboard)
                    binding.passCodeView.setPassCode("")
                    binding.tvWelcomeTag.text = getString(R.string.tv_clock_in)
                    AlertUtils.showCustomAlert(requireActivity(), it.message)
                } else {
                    findNavController().navigate(R.id.action_passcode_to_clockInOwner)
                }
            }
        })

    }

    private fun setupSnackbar() {


        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)


    }
}