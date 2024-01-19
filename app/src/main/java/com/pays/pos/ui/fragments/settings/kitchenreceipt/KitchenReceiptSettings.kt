package com.pays.pos.ui.fragments.settings.kitchenreceipt

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
import com.pays.pos.R
import com.pays.pos.data.model.requestModel.UpdateKitchenReceiptRequestModel
import com.pays.pos.data.remote.Constants.KEY
import com.pays.pos.data.remote.Constants.LARGE
import com.pays.pos.data.remote.Constants.MEDIUM
import com.pays.pos.data.remote.Constants.ORDER_RECEIPTS
import com.pays.pos.data.remote.Constants.SETTING_KEY
import com.pays.pos.data.remote.Constants.SMALL
import com.pays.pos.databinding.FragmentKitchenReceiptSettingsBinding
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.Event
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.ProgressUtils
import com.pays.pos.utils.statusUtils.Status
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

        getKitchenSettings()
        updateDate()
        observeShowProgress()
        return binding.root
    }

    private fun getKitchenSettings() {
        viewModel.getKitchenSettings().observe(viewLifecycleOwner, { resources ->
            when (resources.status) {
                Status.SUCCESS -> {
                    viewModel._showProgress.value = Event(false)
                    resources.data.let {
                        if (it != null) {
                            viewModel.kitchenData.value = resources.data
                            viewModel.kitchenId.value = resources.data?.id
                        }

                    }

                }
                Status.ERROR -> {
                    viewModel._snackbarText.value = Event(resources.message)
                    viewModel._showProgress.value = Event(false)
                }
                Status.LOADING -> {
                    viewModel._showProgress.value = Event(true)

                }

            }

        })
    }

    private fun updateDate() {
        viewModel.data.observe(requireActivity()) {
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
        }
    }

    private fun observeData() {
        viewModel.kitchenData.observe(requireActivity()) {
            LogUtil.logE(TAG, "KitchenReceiptRespone  ${Gson().toJson(it)}")
            when (it.fonts) {
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

            setTextSize(it.fonts)
            //binding.swtShowCategory.isChecked = it.showCategory
            binding.swtSameGrpItem.isChecked = it.showItemsInGroup
            binding.swtTeamMember.isChecked = it.showTeamMember
            binding.swtOrderNote.isChecked = it.showOrderNote
            binding.swtOrderType.isChecked = it.showOrderType
            binding.swtName.isChecked = it.showCustomerName
            binding.swtPhone.isChecked = it.showCustomerPhone
            binding.swtAddress.isChecked = it.showCustomerAddress

            /*if (it.showCategory) {
                binding.txtSoup.visibility = View.VISIBLE
            } else {
                binding.txtSoup.visibility = View.GONE
            }*/
            if (it.showCustomerName) {
                binding.txtName.visibility = View.VISIBLE
            } else {
                binding.txtName.visibility = View.GONE
            }
            if (it.showCustomerPhone) {
                binding.txtPhone.visibility = View.VISIBLE
            } else {
                binding.txtPhone.visibility = View.GONE

            }
            if (it.showOrderNote) {
                binding.linearOrderNote.visibility = View.VISIBLE
            } else {
                binding.linearOrderNote.visibility = View.GONE
            }
            if (it.showOrderType) {
                binding.txtDineIn.visibility = View.VISIBLE
            } else {
                binding.txtDineIn.visibility = View.GONE
            }

            if (it.showTeamMember) {
                binding.linearTeamMember.visibility = View.VISIBLE
            } else {
                binding.linearTeamMember.visibility = View.INVISIBLE
            }
            if (it.showCustomerAddress) {
                binding.txtAddress.visibility = View.VISIBLE
            } else {
                binding.txtAddress.visibility = View.GONE
            }

        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onClick()
        onChecked()
        observeData()
        binding.rdGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                binding.radioLarge.id -> {
                    setTextSize(LARGE)
                }
                binding.radioMedium.id -> {
                    setTextSize(MEDIUM)
                }
                binding.radioSmall.id -> {
                    setTextSize(SMALL)
                }

            }

        }


    }

    private fun onChecked() {

        /*binding.swtShowCategory.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.txtSoup.visibility = View.VISIBLE
            } else {
                binding.txtSoup.visibility = View.GONE
            }
        }*/

        binding.swtOrderNote.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.linearOrderNote.visibility = View.VISIBLE
            } else {
                binding.linearOrderNote.visibility = View.GONE
            }
        }

        binding.swtOrderType.setOnCheckedChangeListener { buttonView, isChecked ->

            if (isChecked) {
                binding.txtDineIn.visibility = View.VISIBLE
            } else {
                binding.txtDineIn.visibility = View.GONE
            }
        }
        binding.swtTeamMember.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {

                binding.linearTeamMember.visibility = View.VISIBLE
            } else {
                binding.linearTeamMember.visibility = View.INVISIBLE
            }

        }

        binding.swtName.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.txtName.visibility = View.VISIBLE
            } else {
                binding.txtName.visibility = View.GONE
            }
        }

        binding.swtPhone.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.txtPhone.visibility = View.VISIBLE
            } else {
                binding.txtPhone.visibility = View.GONE
            }
        }

        binding.swtAddress.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.txtAddress.visibility = View.VISIBLE
            } else {
                binding.txtAddress.visibility = View.GONE
            }
        }


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
            //model.show_category = binding.swtShowCategory.isChecked
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

    private fun setTextSize(type: String) {

        when (type) {
            SMALL -> {
                binding.txtKitchenPreview.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.txtDineIn.textSize = requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.txtOrderId.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.txtReceiptId.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.txtEmployee.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.txtDateTime.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.txtGuestNo.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.txtName.textSize = requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.txtPhone.textSize = requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.txtAddress.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.txtDineIn.textSize = requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.txtDineIn.textSize = requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.txtDineIn.textSize = requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.txtDineIn.textSize = requireContext().resources.getDimension(R.dimen._5mdpi)

                binding.txtOrderIdLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.txtReceiptIdLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.txtEmployeeLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.txtSoup.textSize = requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.txtChicken.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.txtChickenPrice.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.txtExtra1.textSize = requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.txtExtraPrice1.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.txtExtra2.textSize = requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.txtExtraPrice2.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.txtNoteLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.txtNote.textSize = requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.txtOrderNoteLable.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.txtSugarLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.txtCustomerDetails.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)


            }
            MEDIUM -> {

                binding.txtKitchenPreview.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.txtDineIn.textSize = requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.txtOrderId.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.txtReceiptId.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.txtEmployee.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.txtDateTime.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.txtGuestNo.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.txtName.textSize = requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.txtPhone.textSize = requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.txtAddress.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.txtDineIn.textSize = requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.txtDineIn.textSize = requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.txtDineIn.textSize = requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.txtDineIn.textSize = requireContext().resources.getDimension(R.dimen._6mdpi)

                binding.txtOrderIdLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.txtReceiptIdLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.txtEmployeeLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.txtSoup.textSize = requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.txtChicken.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.txtChickenPrice.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.txtExtra1.textSize = requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.txtExtraPrice1.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.txtExtra2.textSize = requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.txtExtraPrice2.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.txtNoteLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.txtNote.textSize = requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.txtOrderNoteLable.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.txtSugarLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.txtCustomerDetails.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)


            }


            LARGE -> {
                binding.txtKitchenPreview.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.txtDineIn.textSize = requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.txtOrderId.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.txtReceiptId.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.txtEmployee.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.txtDateTime.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.txtGuestNo.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.txtName.textSize = requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.txtPhone.textSize = requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.txtAddress.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.txtOrderIdLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.txtReceiptIdLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.txtEmployeeLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.txtSoup.textSize = requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.txtChicken.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.txtChickenPrice.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.txtExtra1.textSize = requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.txtExtraPrice1.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.txtExtra2.textSize = requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.txtExtraPrice2.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.txtNoteLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.txtNote.textSize = requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.txtOrderNoteLable.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.txtSugarLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.txtCustomerDetails.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)

            }
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