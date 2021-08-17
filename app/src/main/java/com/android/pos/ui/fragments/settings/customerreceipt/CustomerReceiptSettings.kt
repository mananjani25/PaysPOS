package com.android.pos.ui.fragments.settings.customerreceipt

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.requestModel.UpdateCustomerReceiptRequestModel
import com.android.pos.data.remote.Constants
import com.android.pos.databinding.FragmentCustomerReceiptSettingsBinding
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CustomerReceiptSettings : Fragment() {

    private val TAG = "CustomerReceiptSettings"
    private val viewModel by viewModels<CustomerReceiptViewModel>()
    private lateinit var binding: FragmentCustomerReceiptSettingsBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_customer_receipt_settings,
                container,
                false
            )
        binding.lifecycleOwner = this

        binding.ivBack.setOnClickListener {
            val navController = findNavController()
            navController.previousBackStackEntry?.savedStateHandle?.set(
                Constants.KEY,
                Constants.ORDER_RECEIPTS
            )
            navController.popBackStack()
        }


        observeData()
        observeShowProgress()
        updateDate()
        onClick()
        return binding.root
    }

    private fun onClick() {
        binding.txtSave.setOnClickListener {
            var model: UpdateCustomerReceiptRequestModel = UpdateCustomerReceiptRequestModel()
            when (binding.rdGroup.checkedRadioButtonId) {
                binding.radioSmall.id -> {
                    model.fonts = Constants.SMALL
                }
                binding.radioMedium.id -> {
                    model.fonts = Constants.MEDIUM
                }
                binding.radioLarge.id -> {
                    model.fonts = Constants.LARGE
                }
            }

            model.showOrderIdTop = binding.swtOrderId.isChecked
            model.showModifiers = binding.swtAddons.isChecked
            model.showSplitAmount = binding.swtOrderNote.isChecked
            model.showRolledOver = binding.swtAutoRollOvr.isChecked
            model.showOrderType = binding.swtOrderType.isChecked
            model.showOrderTime = binding.customerReciptPart2.swtOrderTime.isChecked
            model.showPrintTime = binding.customerReciptPart2.swtPrintTime.isChecked
            model.showRefundAmount = binding.customerReciptPart2.swtRfundAmt.isChecked
            model.showVenueLogo = binding.customerReciptPart2.swtLogo.isChecked
            model.showWebsiteAddress = binding.customerReciptPart2.swtWebAddress.isChecked
            model.showVenueAddress = binding.customerReciptPart2.swtAddress.isChecked
            model.showCustomerName = binding.customerReciptPart2.swtName.isChecked
            model.showCustomerPhone = binding.customerReciptPart2.swtPhone.isChecked
            model.showCustomerAddress = binding.customerReciptPart2.swtCustAddress.isChecked
            model.showTipSuggestion = binding.customerReciptPart2.swtTipSuggestion.isChecked
            model.showTipLineForCash = binding.customerReciptPart2.swtTipCash.isChecked
            model.showQrCode = binding.customerReciptPart2.swtQrCode.isChecked
            model.showCustomNote = binding.customerReciptPart2.swtCustomNote.isChecked

            viewModel.updateCustomer(model)

        }
    }

    private fun observeData() {
        viewModel.customerData.observe(requireActivity(), {
            val model = it.data
            when (model.fonts) {
                Constants.SMALL -> {
                    binding.rdGroup.check(binding.radioSmall.id)
                }
                Constants.LARGE -> {
                    binding.rdGroup.check(binding.radioLarge.id)

                }
                Constants.MEDIUM -> {
                    binding.rdGroup.check(binding.radioMedium.id)
                }
            }

            binding.swtOrderId.isChecked = model.showOrderIdTop
            binding.swtAddons.isChecked = model.showModifiers
            binding.swtOrderNote.isChecked = model.showSplitAmount
            binding.swtAutoRollOvr.isChecked = model.showRolledOver
            binding.swtOrderType.isChecked = model.showOrderType
            binding.customerReciptPart2.swtOrderTime.isChecked = model.showOrderTime
            binding.customerReciptPart2.swtPrintTime.isChecked = model.showPrintTime
            binding.customerReciptPart2.swtRfundAmt.isChecked = model.showRefundAmount
            binding.customerReciptPart2.swtLogo.isChecked = model.showVenueLogo
            binding.customerReciptPart2.swtWebAddress.isChecked = model.showWebsiteAddress
            binding.customerReciptPart2.swtAddress.isChecked = model.showVenueAddress
            binding.customerReciptPart2.swtName.isChecked = model.showCustomerName
            binding.customerReciptPart2.swtPhone.isChecked = model.showCustomerPhone
            binding.customerReciptPart2.swtCustAddress.isChecked = model.showCustomerAddress
            binding.customerReciptPart2.swtTipSuggestion.isChecked = model.showTipSuggestion
            binding.customerReciptPart2.swtTipCash.isChecked = model.showTipLineForCash
            binding.customerReciptPart2.swtQrCode.isChecked = model.showQrCode
            binding.customerReciptPart2.swtCustomNote.isChecked = model.showCustomNote

        })

    }

    private fun updateDate() {
        viewModel.data.observe(requireActivity(), {
            it.getContentIfNotHandled()?.let { data ->
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, data.toString()
                    ) { _, _ ->
                        val navController = findNavController()
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            Constants.KEY,
                            Constants.ORDER_RECEIPTS
                        )
                        navController.popBackStack()
                    }
                }

            }
        })
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
}