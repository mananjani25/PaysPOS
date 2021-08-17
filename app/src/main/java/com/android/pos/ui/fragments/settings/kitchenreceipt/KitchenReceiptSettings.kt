package com.android.pos.ui.fragments.settings.kitchenreceipt

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.model.requestModel.UpdateKitchenReceiptRequestModel
import com.android.pos.data.remote.Constants.KEY
import com.android.pos.data.remote.Constants.LARGE
import com.android.pos.data.remote.Constants.MEDIUM
import com.android.pos.data.remote.Constants.ORDER_RECEIPTS
import com.android.pos.data.remote.Constants.SETTING_KEY
import com.android.pos.data.remote.Constants.SMALL
import com.android.pos.databinding.FragmentKitchenReceiptSettingsBinding
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.ProgressUtils
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class KitchenReceiptSettings : Fragment(), CompoundButton.OnCheckedChangeListener {

    private val viewModel by viewModels<KitchenReceiptViewModel>()
    private lateinit var binding: FragmentKitchenReceiptSettingsBinding
    private val TAG = "KitchenReceiptSettings"
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_kitchen_receipt_settings,
                container,
                false
            )
        binding.lifecycleOwner = this

        binding.ivBack.setOnClickListener {
            val navController = findNavController()
            navController.previousBackStackEntry?.savedStateHandle?.set(KEY, ORDER_RECEIPTS)
            navController.popBackStack()
        }
        observeData()
        updateDate()
        observeShowProgress()
        return binding.root
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
                            KEY,
                            ORDER_RECEIPTS
                        )
                        navController.popBackStack()
                    }
                }

            }
        })
    }

    private fun observeData() {
        viewModel.kitchenData.observe(requireActivity(), {
            Log.e(TAG, "KitchenReceiptRespone  ${Gson().toJson(it)}")
            when (it.data.fonts) {
                SMALL -> {
                    binding.rdGroup.check(binding.radioSmall.id)
                }
                LARGE -> {
                    binding.rdGroup.check(binding.radioLarge.id)

                }
                MEDIUM -> {
                    binding.rdGroup.check(binding.radioMedium.id)
                }

            }

            binding.swtShowCategory.isChecked = it.data.showCategory
            binding.swtSameGrpItem.isChecked = it.data.showItemsInGroup
            binding.swtTeamMember.isChecked = it.data.showTeamMember
            binding.swtOrderNote.isChecked = it.data.showOrderNote
            binding.swtOrderType.isChecked = it.data.showOrderType
            binding.swtName.isChecked = it.data.showCustomerName
            binding.swtPhone.isChecked = it.data.showCustomerPhone
            binding.swtAddress.isChecked = it.data.showCustomerAddress

        })

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onClick()


    }

    private fun onClick() {
        binding.txtSave.setOnClickListener {
            var model: UpdateKitchenReceiptRequestModel = UpdateKitchenReceiptRequestModel()
            when (binding.rdGroup.checkedRadioButtonId) {
                binding.radioSmall.id -> {
                    model.font = SMALL
                }
                binding.radioMedium.id -> {
                    model.font = MEDIUM
                }
                binding.radioLarge.id -> {
                    model.font = LARGE
                }
            }
            model.show_category = binding.swtShowCategory.isChecked
            model.show_items_in_group = binding.swtSameGrpItem.isChecked
            model.show_team_member = binding.swtTeamMember.isChecked
            model.show_order_note = binding.swtOrderNote.isChecked
            model.show_order_type = binding.swtOrderType.isChecked
            model.show_customer_name = binding.swtName.isChecked
            model.show_customer_phone = binding.swtPhone.isChecked
            model.show_customer_address = binding.swtAddress.isChecked

            viewModel.updateKitchen(model)


        }
    }


    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        // if(buttonView?.id==)
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