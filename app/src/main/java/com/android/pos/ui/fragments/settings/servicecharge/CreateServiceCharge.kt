package com.android.pos.ui.fragments.settings.servicecharge

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.android.pos.utils.DecimalDigitsCountFilter
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.gone
import com.android.pos.utils.extensions.liveSnackBar
import com.android.pos.utils.extensions.visible
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateServiceCharge : Fragment() {
    private lateinit var binding: DialogAddServiceChargeBinding

    private val viewModel by viewModels<CreateServiceChargeViewModel>()

    var isEdit: Boolean = false
    private lateinit var serviceChargeData: TbServiceCharge
    var isfrom = "takeout"
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DialogAddServiceChargeBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        binding.viewModel = viewModel
        binding.createServiceChargeFragment = this
        isfrom = arguments?.getString("isFrom", "takeout").toString()
        isEdit = arguments?.getBoolean("isEdit")!!

        if (isfrom == "dinein") {
            binding.linearGuest?.visible()
        } else {
            binding.linearGuest?.gone()
        }
        binding.header.txtSave.text = getString(R.string.save)
        binding.header.txtTitle.text = getString(R.string.add_service_charge)

        if (isEdit) {
            binding.header.txtSave.text = getString(R.string.update)
            binding.header.txtTitle.text = getString(R.string.update_service_charge)
            serviceChargeData = arguments?.getParcelable("serviceChargeObject")!!
            binding.editPercentage.setText(String.format("%.2f", serviceChargeData.percentage))
            if (isfrom == "dinein") {
                binding.editMinguest.setText(serviceChargeData.min_guest_count.toString())
                binding.editMaxguest.setText(serviceChargeData.max_guest_count.toString())
            }
            viewModel.setDiscountData(serviceChargeData)

            binding.swtEnableCharge.isChecked = serviceChargeData.isEnabled

            viewModel.isEditData(isEdit, serviceChargeData.id, isfrom)
        } else {
            viewModel.isEditData(false, -1, isfrom)
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

        //binding.editPercentage.filters = arrayOf(DecimalDigitsCountFilter(2))

        binding.editPercentage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                Log.e("Text Changed", "Service tax" + s.toString())
            }

            override fun afterTextChanged(s: Editable?) {
                try {
                    if (s.toString().isNotEmpty()) {
                        if (s.toString().toDouble() > 100) {
                            binding.editPercentage?.setText("100")
                            binding.editPercentage.setSelection(binding.editPercentage.length())
                        }
                    }
                } catch (_: Exception) {
                }
            }

        })
        binding.header.txtSave.setOnClickListener {
            var subPer = "0.0"

            try{
            if (binding.editPercentage.text?.isNotEmpty() == true) {
                subPer = binding.editPercentage?.text.toString().split(" ")[0]
            }
            viewModel.createServiceChargeDetails.value?.percentage = subPer.toDouble()
            if (isfrom == "dinein") {
                if (binding.editMinguest.text?.isNotEmpty() == true) {
                    viewModel.createServiceChargeDetails.value?.min_guest_count =
                        binding.editMinguest?.text.toString().toInt()
                } else {
                    viewModel.createServiceChargeDetails.value?.min_guest_count = 0
                }
                if (binding.editMaxguest?.text?.isNotEmpty() == true) {
                    viewModel.createServiceChargeDetails.value?.max_guest_count =
                        binding.editMaxguest?.text.toString().toInt()
                } else {
                    viewModel.createServiceChargeDetails.value?.max_guest_count = 0
                }
                viewModel.createServiceChargeDetails.value?.order_type = "DineIn"
            } else {
                viewModel.createServiceChargeDetails.value?.order_type = "TakeOutAndParkOrder"
            }
            MethodUtils.hideKeyboard(requireActivity())
            viewModel.submit()
        }catch (_:Exception){


            AlertUtils.showCustomAlertWithListenerWithOK(requireContext(),"Please input valid value"){_,_->
                binding.editPercentage.setText("")
            }
        }
        }

        return binding.root
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