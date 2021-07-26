package com.android.pos.ui.fragments.settings.tax

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.responseModel.GetTaxResponse
import com.android.pos.data.remote.Constants.CREATE_TAX
import com.android.pos.data.remote.Constants.DIALOG_KEY
import com.android.pos.data.remote.Constants.DIALOG_KEY_TAX
import com.android.pos.data.remote.Constants.SETTING_KEY
import com.android.pos.databinding.DialogCreateNewTaxBinding
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.getNavigationResultLiveData
import com.android.pos.utils.extensions.liveSnackBar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateTax : Fragment() {

    private lateinit var binding: DialogCreateNewTaxBinding

    private val viewModel by viewModels<CreateTaxViewModel>()
    private var itemIds = ArrayList<Int>()

    var isEdit: Boolean = false
    private lateinit var taxData: GetTaxResponse.TaxData

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.dialog_create_new_tax, container, false)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.createTaxFragment = this


        isEdit = arguments?.getBoolean("isEdit")!!

        if (isEdit) {
            taxData = arguments?.getParcelable("taxObject")!!
            binding.txtSave.text = getString(R.string.update)
            viewModel.setTaxData(taxData)

            /*if (taxData.itemPricing == 0) {
                binding.tvItemPricing.text =
            }*/

            binding.itemsCount.setText("" + taxData.itemIds.size + " Items")

            binding.swtEnableTax.isChecked = taxData.isActive
            viewModel.isEditData(isEdit, taxData.id)
        }

        setupSnackbar()
        observeShowProgress()
        navigate()

        binding.llAllItemsDialog.setOnClickListener {
            findNavController().navigate(R.id.action_newTax_to_itemDialog)
        }

        binding.llItemPricing.setOnClickListener {
            if (isEdit) {
                //bundle have to sent for item ids
                findNavController().navigate(R.id.action_newTax_to_itemPricingDialog)
            } else {
                findNavController().navigate(R.id.action_newTax_to_itemPricingDialog)
            }

        }

        val resultDialogKey = getNavigationResultLiveData<ArrayList<TbItem>>(DIALOG_KEY)

        resultDialogKey?.observe(viewLifecycleOwner) {
            if (it.size > 0) {
                binding.itemsCount.text = "" + it.size + " Items"
            } else {
                binding.itemsCount.text = "No Items"
            }

            it.forEach {
                itemIds.add(it.itemId)
            }
        }

        val resultDialogKeyTax = getNavigationResultLiveData<Int>(DIALOG_KEY_TAX)

        resultDialogKeyTax?.observe(viewLifecycleOwner) { itemPricing ->

            if (itemPricing == 0) {
                binding.tvItemPricing.text = getString(R.string.tv_add_tax_to_item_price)
            } else if (itemPricing == 1) {
                binding.tvItemPricing.text = getString(R.string.tv_include_tax_in_item_price)
            }

            viewModel.setItemIds(itemIds, itemPricing)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<AppCompatImageView>(R.id.imgBack).setOnClickListener {
            val navController = findNavController()
            navController.previousBackStackEntry?.savedStateHandle?.set(
                SETTING_KEY,
                CREATE_TAX
            )
            navController.popBackStack()
            //findNavController().navigateUp()
        }
    }

    fun enableTax(isChecked: Boolean) {
        if (isChecked) {
            viewModel.enableTax(isChecked)
        } else {
            viewModel.enableTax(isChecked)
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
                if (it) {
                    findNavController().navigateUp()
                }
            }
        })
    }

    private fun setupSnackbar() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    }
}