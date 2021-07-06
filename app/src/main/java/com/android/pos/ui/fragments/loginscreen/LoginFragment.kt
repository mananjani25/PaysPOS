package com.android.pos.ui.fragments.loginscreen

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.android.pos.R
import com.android.pos.databinding.FragmentLoginTestBinding
import com.android.pos.utils.extensions.hide
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.extensions.show
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment : Fragment() {

    var activity: Activity = requireActivity()
    private lateinit var binding: FragmentLoginTestBinding

    private val viewModel by viewModels<MobileNumberViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login_test, container, false)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        setupSnackbar()
        observeShowProgress()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    private fun observeShowProgress() {
        viewModel.showProgress.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    binding.progressBar.show()
                } else {
                    binding.progressBar.hide()
                }
            }
        })
    }

    private fun setupSnackbar() {

        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)


    }


}