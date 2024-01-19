package com.pays.pos.ui.fragments.settings.Security

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.pays.pos.data.remote.Constants
import com.pays.pos.databinding.FragmentSecurityBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.ProgressUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class Security : Fragment() {
    private lateinit var binding: FragmentSecurityBinding
    private val viewModel by viewModels<SecurityViewModel>()

    @Inject
    lateinit var prefProvider: PrefProvider
    var checked = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSecurityBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefProvider = PrefProvider(requireContext())


        observeShowProgress()
        if (prefProvider.getValueboolean(Constants.LOCK_SCREEN_TRANSACTION, false)) {
            checked = true
            binding.switchLockscreen.isChecked = true
        } else {
            checked = false
            binding.switchLockscreen.isChecked = false
        }

        binding.switchLockscreen.setOnClickListener {
            viewModel.updateTransactionLock(binding.switchLockscreen.isChecked)
        }
    }

    private fun observeShowProgress() {

        viewModel.showProgress.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(requireActivity())
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }

        viewModel.data.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    prefProvider.setValueboolean(
                        Constants.LOCK_SCREEN_TRANSACTION,
                        binding.switchLockscreen.isChecked
                    )
                }
            }
        }

    }
}