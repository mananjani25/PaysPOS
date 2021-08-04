package com.android.pos.ui.fragments.settings.servicecharge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.responseModel.GetServiceChargeResponse
import com.android.pos.data.remote.Constants.ADD_SERVICE_CHARGE
import com.android.pos.data.remote.Constants.KEY
import com.android.pos.databinding.DialogAddServiceChargeBinding
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.liveSnackBar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateServiceCharge : Fragment() {
    private lateinit var binding: DialogAddServiceChargeBinding

    private val viewModel by viewModels<CreateServiceChargeViewModel>()

    var isEdit: Boolean = false
    private lateinit var serviceChargeData: GetServiceChargeResponse.Data

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogAddServiceChargeBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        binding.viewModel = viewModel
        binding.createServiceChargeFragment = this

        isEdit = arguments?.getBoolean("isEdit")!!

        if (isEdit) {
            binding.txtSave.text = getString(R.string.update)
            serviceChargeData = arguments?.getParcelable("serviceChargeObject")!!

            viewModel.setDiscountData(serviceChargeData)

            binding.swtEnableCharge.isChecked = serviceChargeData.isEnabled

            viewModel.isEditData(isEdit, serviceChargeData.id)
        }

        setupSnackbar()
        observeShowProgress()
        navigate()

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true /* enabled by default */) {
                override fun handleOnBackPressed() {
                    backPressManage()
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.imgBack.setOnClickListener {
            backPressManage()
        }

    }

    fun enableSerCharge(isChecked: Boolean) {
        if (isChecked) {
            viewModel.enableSerCharge(isChecked)
        } else {
            viewModel.enableSerCharge(isChecked)
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
            event.getContentIfNotHandled()?.let { createServiceChargeResponse ->
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, createServiceChargeResponse.message
                    ) { _, _ ->
                        backPressManage()
                    }
                }
            }
        })
    }

    private fun backPressManage() {
        val navController = findNavController()
        navController.previousBackStackEntry?.savedStateHandle?.set(KEY, ADD_SERVICE_CHARGE)
        navController.popBackStack()
    }

    private fun setupSnackbar() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    }
}