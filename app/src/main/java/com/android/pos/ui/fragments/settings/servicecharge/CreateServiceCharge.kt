package com.android.pos.ui.fragments.settings.servicecharge

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.TbServiceCharge
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
    private lateinit var serviceChargeData: TbServiceCharge
    private val TAG = "CreateServiceCharge"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogAddServiceChargeBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        binding.viewModel = viewModel
        binding.createServiceChargeFragment = this

        isEdit = arguments?.getBoolean("isEdit")!!

        binding.header.txtSave.text = getString(R.string.save)
        binding.header.txtTitle.text = getString(R.string.add_service_charge)

        if (isEdit) {
            binding.header.txtSave.text = getString(R.string.update)
            binding.header.txtTitle.text = getString(R.string.update_service_charge)
            serviceChargeData = arguments?.getParcelable("serviceChargeObject")!!

            viewModel.setDiscountData(serviceChargeData)

            binding.swtEnableCharge.isChecked = serviceChargeData.isEnabled

            viewModel.isEditData(isEdit, serviceChargeData.id)
        }
        setupSnackbar()
        observeShowProgress()
        navigate()
        addTextChangeListner()
        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true /* enabled by default */) {
                override fun handleOnBackPressed() {
                    backPressManage()
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        binding.header.txtSave.setOnClickListener {
            if (binding.edtPercentageServiceCharge?.text.toString().trim().isNotEmpty()) {
                var subPer = binding.edtPercentageServiceCharge?.text.toString().split(" ")[0]
                viewModel.createServiceChargeDetails.value?.percentage = subPer.toDouble()
                viewModel.submit()
            } else {
                AlertUtils.showCustomAlert(
                    requireContext(),
                    requireContext().resources.getString(R.string.sercharge_rate_validate)
                )
            }
        }
        try {
            val inputManager =
                requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            requireActivity().currentFocus?.let {
                inputManager.showSoftInput(
                    binding.edtPercentageServiceCharge,
                    InputMethodManager.SHOW_IMPLICIT
                )
            }
        } catch (e: Exception) {
        }

        return binding.root
    }

    private fun addTextChangeListner() {
        binding.edtPercentageServiceCharge?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence, p1: Int, p2: Int, p3: Int) {
                if (binding.edtPercentageServiceCharge.text.toString()
                        .isNotEmpty() && binding.edtPercentageServiceCharge.text.toString()
                        .toDouble() > 100
                ) {
                    binding.edtPercentageServiceCharge.setText("100.00")
                    binding.edtPercentageServiceCharge.setSelection(binding.edtPercentageServiceCharge.length())
                }

            }

            override fun afterTextChanged(p0: Editable?) {

            }

        })
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.header.imgBack.setOnClickListener {
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