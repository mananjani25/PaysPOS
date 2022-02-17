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
import com.android.pos.R
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.FragmentPasscodeBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.activities.MainActivity
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.liveSnackBar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class Passcode : Fragment() {

    private lateinit var binding: FragmentPasscodeBinding
    private val viewModel by viewModels<PasscodeViewModel>()
    var isDashboard: Boolean = false
    var isClockOut: Boolean = false
    var isSwap: Boolean = false
    var validationmsg: String = ""

    @Inject
    lateinit var prefProvider: PrefProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true /* enabled by default */) {
                override fun handleOnBackPressed() {
                    if(isSwap || isDashboard){
                        findNavController().navigateUp()
                    }else{
                        (requireActivity() as MainActivity).finish()
                    }
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_passcode, container, false)
        binding.lifecycleOwner = this
        binding.passcodeViewModel = viewModel
        isDashboard = arguments?.getBoolean("isDashboard")!!
        isSwap = arguments?.getBoolean("isSwap")!!

        if (isSwap) {
            binding.tvWelcomeTag.text = getString(R.string.tv_clock_in)
        } else {
            if (isDashboard) {
                binding.tvWelcomeTag.text = getString(R.string.tv_clock_out)
                viewModel.isDashboardData(isDashboard)
            }
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

                if (isDashboard) {
                    if (prefProvider.getValue(Constants.PASSCODE, "").toString() == it.toString()
                    ) {
                        viewModel.submit(it.toString())
                    } else {
                        binding.passCodeView.setPassCode("")
                        AlertUtils.showCustomAlert(
                            requireActivity(),
                            "You have entered wrong Passcode."
                        )

                    }
                } else {
                    viewModel.submit(it.toString())
                }

            }
        }

        binding.Cancel.setOnClickListener {
            if (isClockOut) {
                (requireActivity() as MainActivity).finish()

            } else {
                findNavController().navigateUp()

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
                validationmsg = it.message
                if (isDashboard) {
                    isDashboard = false
                    isClockOut = true
                    viewModel.isDashboardData(isDashboard)
                    binding.passCodeView.setPassCode("")
                    binding.tvWelcomeTag.text = getString(R.string.tv_clock_in)
                    AlertUtils.showCustomAlert(requireContext(), validationmsg)
                } else {
                    findNavController().navigate(R.id.action_passcode_to_dashboard)
                }
            }
        })

    }

    private fun setupSnackbar() {


        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)


    }
}