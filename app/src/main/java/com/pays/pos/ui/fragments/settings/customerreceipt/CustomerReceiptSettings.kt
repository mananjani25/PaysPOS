package com.pays.pos.ui.fragments.settings.customerreceipt

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.data.model.requestModel.UpdateCustomerReceiptRequestModel
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.LARGE
import com.pays.pos.data.remote.Constants.MEDIUM
import com.pays.pos.data.remote.Constants.SMALL
import com.pays.pos.databinding.FragmentCustomerReceiptSettingsBinding
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.Event
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.extensions.gone
import com.pays.pos.utils.extensions.visible
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CustomerReceiptSettings : Fragment() {

    private val TAG = "CustomerReceiptSettings"
    private val viewModel by viewModels<CustomerReceiptViewModel>()
    private lateinit var binding: FragmentCustomerReceiptSettingsBinding
    private lateinit var pd: Dialog
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
        progressDialog()
        observeShowProgress()

        binding.ivBack.setOnClickListener {
            val navController = findNavController()
            navController.previousBackStackEntry?.savedStateHandle?.set(
                Constants.KEY,
                Constants.ORDER_RECEIPTS
            )
            navController.popBackStack()
        }


        getCustomerSettings()

        updateDate()


        return binding.root
    }

    private fun getCustomerSettings() {
        viewModel.getCustomerSettings().observe(viewLifecycleOwner, { resource ->

            when (resource.status) {
                Status.ERROR -> {
                    viewModel._snackbarText.value = Event(resource.message)
                    viewModel._showProgress.value = Event(false)
                }
                Status.SUCCESS -> {
                    viewModel._showProgress.value = Event(false)
                    resource.data.let {
                        if (it != null) {
                            viewModel.customerData.value = it
                            viewModel.customerId.value = it?.id
                        }

                    }


                }
                Status.LOADING -> {
                    viewModel._showProgress.value = Event(true)

                }
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeData()
        onClick()
        onChecked()

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
            model.showOrderNote = binding.swtOrderNote.isChecked
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
            model.showTeam = binding.swtEmployee.isChecked
            model.showCdAndScCustomerReceipt = binding.customerReciptPart2.swtSurCash.isChecked


            viewModel.updateCustomer(model)

        }
    }

    private fun onChecked() {

        binding.customerReciptPart2.swtTipSuggestion.setOnCheckedChangeListener { buttonView, isChecked ->

            if (isChecked) {
                binding.layoutCustomerReceipt.txtAdditionalTip.visibility = View.VISIBLE
                binding.layoutCustomerReceipt.viewLineTip.visibility = View.VISIBLE
                binding.layoutCustomerReceipt.linearTip1.visibility = View.VISIBLE
                binding.layoutCustomerReceipt.linearTip2.visibility = View.VISIBLE

            } else {
                binding.layoutCustomerReceipt.txtAdditionalTip.visibility = View.GONE
                binding.layoutCustomerReceipt.viewLineTip.visibility = View.GONE
                binding.layoutCustomerReceipt.linearTip1.visibility = View.GONE
                binding.layoutCustomerReceipt.linearTip2.visibility = View.GONE
            }
        }

        binding.swtOrderType.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.layoutCustomerReceipt.txtOrderType.visibility = View.VISIBLE
            } else {
                binding.layoutCustomerReceipt.txtOrderType.visibility = View.GONE

            }
        }

        binding.customerReciptPart2.swtOrderTime.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.layoutCustomerReceipt.txtDateTime.visibility = View.VISIBLE
            } else {
                binding.layoutCustomerReceipt.txtDateTime.visibility = View.GONE
            }
        }
        binding.customerReciptPart2.swtPrintTime.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.layoutCustomerReceipt.txtPrintDateTime.visibility = View.VISIBLE
            } else {
                binding.layoutCustomerReceipt.txtPrintDateTime.visibility = View.GONE
            }
        }

        binding.customerReciptPart2.swtQrCode.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.layoutCustomerReceipt.imgQrCode.visibility = View.VISIBLE
            } else {
                binding.layoutCustomerReceipt.imgQrCode.visibility = View.GONE
            }
        }

        binding.customerReciptPart2.swtSurCash.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked){
                binding.layoutCustomerReceipt.linearCashDisSurCharge?.visibility = View.VISIBLE
            }
            else{
                binding.layoutCustomerReceipt.linearCashDisSurCharge?.visibility = View.GONE
            }

        }

        binding.swtEmployee.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.layoutCustomerReceipt.txtEmployee.visibility = View.VISIBLE
            } else {
                binding.layoutCustomerReceipt.txtEmployee.visibility = View.GONE
            }
        }

        binding.customerReciptPart2.swtRfundAmt.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.layoutCustomerReceipt.linearRefundAmt.visibility = View.VISIBLE
            } else {
                binding.layoutCustomerReceipt.linearRefundAmt.visibility = View.GONE
            }

        }

        binding.customerReciptPart2.swtCustAddress.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.layoutCustomerReceipt.txtCusAddress.visibility = View.VISIBLE
            } else {
                binding.layoutCustomerReceipt.txtCusAddress.visibility = View.GONE
            }

        }

        binding.customerReciptPart2.swtName.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {

                binding.layoutCustomerReceipt.txtCustomerName.visibility = View.VISIBLE
            } else {
                binding.layoutCustomerReceipt.txtCustomerName.visibility = View.GONE
            }

        }


        binding.customerReciptPart2.swtWebAddress.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.layoutCustomerReceipt.txtWebSite.visibility = View.VISIBLE

            } else {
                binding.layoutCustomerReceipt.txtWebSite.visibility = View.GONE
            }

        }

        binding.customerReciptPart2.swtAddress.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.layoutCustomerReceipt.txtAddress.visibility = View.VISIBLE
            } else {
                binding.layoutCustomerReceipt.txtAddress.visibility = View.GONE
            }

        }



        binding.customerReciptPart2.swtTipCash.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.layoutCustomerReceipt.linearTips?.visibility = View.VISIBLE
            } else {
                binding.layoutCustomerReceipt.linearTips?.visibility = View.GONE
            }

        }

        binding.customerReciptPart2.swtCustomNote.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.layoutCustomerReceipt.txtSugarLabel.visibility = View.VISIBLE
            } else {
                binding.layoutCustomerReceipt.txtSugarLabel.visibility = View.GONE

            }

        }


        binding.swtOrderNote.setOnCheckedChangeListener { buttonView, isChecked ->
            LogUtil.logE(TAG, "ORderNoteChecked ${isChecked}")
            if (isChecked) {
                binding.layoutCustomerReceipt.txtOrderNoteLable.visibility = View.VISIBLE
                binding.layoutCustomerReceipt.txtSugarLabel.visibility = View.VISIBLE

            } else {
                binding.layoutCustomerReceipt.txtOrderNoteLable.visibility = View.GONE
                binding.layoutCustomerReceipt.txtSugarLabel.visibility = View.GONE
            }
        }

        binding.swtSplitAmount.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {

            } else {

            }

        }



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

        binding.swtAddons.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.layoutCustomerReceipt.linearModifier.visibility = View.VISIBLE
                binding.layoutCustomerReceipt.linearModifier1.visibility = View.VISIBLE
                binding.layoutCustomerReceipt.linearModifier2.visibility = View.VISIBLE

            } else {
                binding.layoutCustomerReceipt.linearModifier.visibility = View.GONE
                binding.layoutCustomerReceipt.linearModifier1.visibility = View.GONE
                binding.layoutCustomerReceipt.linearModifier2.visibility = View.GONE
            }

        }
        binding.customerReciptPart2.swtLogo.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.layoutCustomerReceipt.imgIcon.visibility = View.VISIBLE
            } else {
                binding.layoutCustomerReceipt.imgIcon.visibility = View.GONE
            }
        }

        binding.customerReciptPart2.swtAddress.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {

                binding.layoutCustomerReceipt.txtAddress.visibility = View.VISIBLE
            } else {
                binding.layoutCustomerReceipt.txtAddress.visibility = View.GONE
            }

        }


        binding.customerReciptPart2.swtPhone.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.layoutCustomerReceipt.txtCustomerPhone.visibility = View.VISIBLE
            } else {
                binding.layoutCustomerReceipt.txtCustomerPhone.visibility = View.GONE
            }
        }
        binding.swtOrderId.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                binding.layoutCustomerReceipt.txtOrderId.visibility = View.VISIBLE
            } else {
                binding.layoutCustomerReceipt.txtOrderId.visibility = View.GONE

            }

        }


    }

    private fun observeData() {
        viewModel.customerData.observe(requireActivity(), {
            if (it != null) {
                val model = it
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

                setTextSize(model.fonts)
                binding.swtOrderId.isChecked = model.showOrderIdTop
                binding.swtAddons.isChecked = model.showModifiers
                binding.swtOrderNote.isChecked = model.showOrderNote
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
                binding.customerReciptPart2.swtSurCash.isChecked = model.showCashDisSurCharg
                binding.customerReciptPart2.swtCustomNote.isChecked = model.showCustomNote
                binding.swtEmployee.isChecked = model.showTeam



                /*if (model.emp){
                    binding.layoutCustomerReceipt.txtEmployee.visibility = View.VISIBLE
                }
                else{
                    binding.layoutCustomerReceipt.txtEmployee.visibility = View.GONE
                }*/

                if (model.showCashDisSurCharg){
                    binding.layoutCustomerReceipt.linearCashDisSurCharge?.visible()
                }
                else{
                    binding.layoutCustomerReceipt.linearCashDisSurCharge?.gone()
                }

                if (model.showTipSuggestion) {
                    binding.layoutCustomerReceipt.txtAdditionalTip.visibility = View.VISIBLE
                    binding.layoutCustomerReceipt.viewLineTip.visibility = View.VISIBLE
                    binding.layoutCustomerReceipt.linearTip1.visibility = View.VISIBLE
                    binding.layoutCustomerReceipt.linearTip2.visibility = View.VISIBLE

                } else {
                    binding.layoutCustomerReceipt.txtAdditionalTip.visibility = View.GONE
                    binding.layoutCustomerReceipt.viewLineTip.visibility = View.GONE
                    binding.layoutCustomerReceipt.linearTip1.visibility = View.GONE
                    binding.layoutCustomerReceipt.linearTip2.visibility = View.GONE
                }
                if (model.showOrderType) {
                    binding.layoutCustomerReceipt.txtOrderType.visibility = View.VISIBLE
                } else {
                    binding.layoutCustomerReceipt.txtOrderType.visibility = View.GONE
                }

                if (model.showOrderTime) {
                    binding.layoutCustomerReceipt.txtDateTime.visibility = View.VISIBLE
                } else {
                    binding.layoutCustomerReceipt.txtDateTime.visibility = View.GONE
                }
                if (model.showPrintTime) {
                    binding.layoutCustomerReceipt.txtPrintDateTime.visibility = View.VISIBLE
                } else {
                    binding.layoutCustomerReceipt.txtPrintDateTime.visibility = View.GONE

                }

                if (model.showModifiers) {
                    binding.layoutCustomerReceipt.linearModifier.visibility = View.VISIBLE
                    binding.layoutCustomerReceipt.linearModifier1.visibility = View.VISIBLE
                    binding.layoutCustomerReceipt.linearModifier2.visibility = View.VISIBLE

                } else {
                    binding.layoutCustomerReceipt.linearModifier.visibility = View.GONE
                    binding.layoutCustomerReceipt.linearModifier1.visibility = View.GONE
                    binding.layoutCustomerReceipt.linearModifier2.visibility = View.GONE

                }
                if (model.showTeam) {
                    binding.layoutCustomerReceipt.txtEmployee.visibility = View.VISIBLE
                } else {
                    binding.layoutCustomerReceipt.txtEmployee.visibility = View.INVISIBLE
                }

                if (model.showQrCode) {
                    binding.layoutCustomerReceipt.imgQrCode.visibility = View.VISIBLE

                } else {
                    binding.layoutCustomerReceipt.imgQrCode.visibility = View.GONE
                }

                if (model.showOrderIdTop) {
                    binding.layoutCustomerReceipt.txtOrderId.visibility = View.VISIBLE
                } else {
                    binding.layoutCustomerReceipt.txtOrderId.visibility = View.GONE
                }

                if (model.showVenueLogo) {
                    binding.layoutCustomerReceipt.imgIcon.visibility = View.VISIBLE
                } else {
                    binding.layoutCustomerReceipt.imgIcon.visibility = View.GONE
                }

                if (model.showVenueAddress) {
                    binding.layoutCustomerReceipt.txtAddress.visibility = View.VISIBLE

                } else {
                    binding.layoutCustomerReceipt.txtAddress.visibility = View.GONE
                }
                if (model.showCustomerPhone) {
                    binding.layoutCustomerReceipt.txtCustomerPhone.visibility = View.VISIBLE

                } else {
                    binding.layoutCustomerReceipt.txtCustomerPhone.visibility = View.GONE
                }

                if (model.showCustomerAddress) {
                    binding.layoutCustomerReceipt.txtCusAddress.visibility = View.VISIBLE

                } else {
                    binding.layoutCustomerReceipt.txtCusAddress.visibility = View.GONE
                }

                if (model.showCustomerName){
                    binding.layoutCustomerReceipt.txtCustomerName.visibility = View.VISIBLE
                }
                else{
                    binding.layoutCustomerReceipt.txtCustomerName.visibility = View.GONE
                }
                /*if (model.showCustomNote) {
                    binding.layoutCustomerReceipt.txtSugarLabel.visibility = View.VISIBLE
                } else {
                    binding.layoutCustomerReceipt.txtSugarLabel.visibility = View.GONE
                }*/
                if (model.showTipLineForCash) {
                    binding.layoutCustomerReceipt.linearTips?.visibility = View.VISIBLE
                } else {
                    binding.layoutCustomerReceipt.linearTips?.visibility = View.GONE
                }

                if (model.showRefundAmount) {
                    binding.layoutCustomerReceipt.linearRefundAmt.visibility = View.VISIBLE
                } else {
                    binding.layoutCustomerReceipt.linearRefundAmt.visibility = View.GONE
                }


            }

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
                    if (pd != null && !pd.isShowing) {
                        pd.show()
                    }
                } else {
                    if (pd != null && pd.isShowing) {
                        pd.dismiss()
                    }
                }
            }
        })
    }

    private fun setTextSize(type: String) {
        when (type) {
            SMALL -> {
                binding.layoutCustomerReceipt.txtTitle.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtFood.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtAddress.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtNumber.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtWebSite.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtOrderId.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtReceiptId.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtEmployee.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtDateTime.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtPrintDateTime.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtChicken.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtChickerPrice.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtExtraSp.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtExtraPrice.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtExtraSp2.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtExtraPrice2.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtExtraSp3.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtExtraPrice3.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtSubTotalLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtSubTotal.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtRefundLAbel.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtRefundAmount.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtServiceLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtServiceChargeAmt.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtDiscountLable.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtDiscountAmount.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtCashDiscounLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtCashDiscountAmt.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtTotlPriceLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtTotalPriceAmt.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtChrgAmountLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtChargeAmount.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtAdditionalTip.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtEnterLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtEntertainAmt.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtName.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtPrice.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtTip.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtTipAmt.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtTotalLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtTotalAmt.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtTransactionIDLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtTransactionIDAmt.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtTransactionTypeLable.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtTransactionTypeAmt.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtCustDetailsLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtCustomerName.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtCustomerPhone.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtCusAddress.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtOrderNoteLable.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtSugarLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtWebSiteName.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)
                binding.layoutCustomerReceipt.txtOrderType.textSize =
                    requireContext().resources.getDimension(R.dimen._5mdpi)

            }

            MEDIUM -> {
                binding.layoutCustomerReceipt.txtTitle.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtFood.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtAddress.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtNumber.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtWebSite.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtOrderId.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtReceiptId.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtEmployee.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtDateTime.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtPrintDateTime.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtChicken.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtChickerPrice.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtExtraSp.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtExtraPrice.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtExtraSp2.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtExtraPrice2.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtExtraSp3.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtExtraPrice3.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtSubTotalLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtSubTotal.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtRefundLAbel.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtRefundAmount.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtServiceLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtServiceChargeAmt.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtDiscountLable.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtDiscountAmount.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtCashDiscounLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtCashDiscountAmt.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtTotlPriceLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtTotalPriceAmt.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtChrgAmountLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtChargeAmount.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtAdditionalTip.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtEnterLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtEntertainAmt.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtName.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtPrice.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtTip.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtTipAmt.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtTotalLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtTotalAmt.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtTransactionIDLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtTransactionIDAmt.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtTransactionTypeLable.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtTransactionTypeAmt.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtCustDetailsLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtCustomerName.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtCustomerPhone.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtCusAddress.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtOrderNoteLable.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtSugarLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtWebSiteName.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)
                binding.layoutCustomerReceipt.txtOrderType.textSize =
                    requireContext().resources.getDimension(R.dimen._6mdpi)

            }

            LARGE -> {
                binding.layoutCustomerReceipt.txtTitle.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtFood.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtAddress.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtNumber.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtWebSite.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtOrderId.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtReceiptId.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtEmployee.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtDateTime.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtPrintDateTime.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtChicken.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtChickerPrice.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtExtraSp.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtExtraPrice.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtExtraSp2.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtExtraPrice2.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtExtraSp3.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtExtraPrice3.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtSubTotalLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtSubTotal.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtRefundLAbel.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtRefundAmount.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtServiceLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtServiceChargeAmt.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtDiscountLable.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtDiscountAmount.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtCashDiscounLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtCashDiscountAmt.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtTotlPriceLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtTotalPriceAmt.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtChrgAmountLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtChargeAmount.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtAdditionalTip.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtEnterLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtEntertainAmt.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtName.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtPrice.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtTip.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtTipAmt.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtTotalLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtTotalAmt.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtTransactionIDLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtTransactionIDAmt.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtTransactionTypeLable.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtTransactionTypeAmt.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtCustDetailsLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtCustomerName.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtCustomerPhone.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtCusAddress.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtOrderNoteLable.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtSugarLabel.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtWebSiteName.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)
                binding.layoutCustomerReceipt.txtOrderType.textSize =
                    requireContext().resources.getDimension(R.dimen._7mdpi)


            }
        }

    }

    fun progressDialog() {

        pd = Dialog(requireActivity())
        pd.setContentView(R.layout.view_loading)
        // pd.setProgressStyle(ProgressDialog.BUTTON_NEUTRAL)
//        pd.setMessage("Please Wait..")
        pd.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        pd.window?.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT)
        )
        pd.setCanceledOnTouchOutside(false)
        pd.setCancelable(false)
        pd.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )


    }
}