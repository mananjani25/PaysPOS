package com.android.pos.ui.fragments.settings.tax

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.AppCompatImageView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.TaxData
import com.android.pos.data.entities.TbItem
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.ADD_TAX
import com.android.pos.data.remote.Constants.CREATE_TAX
import com.android.pos.data.remote.Constants.DIALOG_KEY
import com.android.pos.data.remote.Constants.DIALOG_KEY_TAX
import com.android.pos.data.remote.Constants.INCLUDE_TAX
import com.android.pos.data.remote.Constants.KEY
import com.android.pos.databinding.DialogCreateNewTaxBinding
import com.android.pos.di.PrefProvider
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.LogUtil
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.getNavigationResultLiveData
import com.android.pos.utils.extensions.liveSnackBar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class CreateTax : Fragment() {

    private var previousValue: String = ""
    private lateinit var binding: DialogCreateNewTaxBinding

    private val viewModel by viewModels<CreateTaxViewModel>()
    private var itemIds = ArrayList<Int>()
    private var itemPricing: String = ""

    var isEdit: Boolean = false
    private lateinit var taxData: TaxData
    private lateinit var taxDataTmp: TaxData

    @set:Inject
    internal var prefProvider: PrefProvider? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.dialog_create_new_tax, container, false)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel
        binding.createTaxFragment = this


        isEdit = arguments?.getBoolean("isEdit")!!


        binding.header.txtSave.text = getString(R.string.save)
        binding.header.txtTitle.text = getString(R.string.tv_new_tax_add)

        if (isEdit) {
            taxDataTmp = arguments?.getParcelable("taxObject")!!
            taxData = arguments?.getParcelable("taxObject")!!
            binding.header.txtSave.text = getString(R.string.update)
            binding.header.txtTitle.text = getString(R.string.tv_update_tax)
            viewModel.setTaxData(taxData)

            binding.itemsCount.text = "" + taxData.itemIds.size + " Items"
            binding.tvItemPricing.text = taxData.itemPricing
            binding.edtAmount.setText(String.format("%.2f", viewModel.createTaxDetails.value?.rate))

            binding.swtEnableTax.isChecked = taxData.isActive
            binding.swtCustomAmount.isChecked = taxData.isCustomAmount
            viewModel.isEditData(isEdit, taxData.id)

            itemPricing = taxData.itemPricing.toString()
            itemIds = taxData.itemIds as ArrayList<Int>
            viewModel.setItemPricing(itemPricing)
            viewModel.setItemIds(itemIds)

            if (taxData.taxType == getString(R.string.disc_percentage)) {
                binding.swtTaxType.isChecked = true
                binding.swtTaxType.text = getString(R.string.disc_percentage)
                binding.edtAmount.hint = resources.getString(R.string.add_tax__percentage)
            } else {
                binding.swtTaxType.isChecked = false
                binding.swtTaxType.text = getString(R.string.dollar_amount)
                binding.edtAmount.hint = resources.getString(R.string.add_tax__dollor)
            }
        }

        binding.edtAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                Log.d("addTextChangedListener","editable = ${s.toString()}")

                try {
                 if (binding.swtTaxType.text == "Percentage") {

                     if(s?.length == 1 && s[0] == '.'){
                         binding.edtAmount.setText("")
                         return
                     }

                     if (previousValue.contains(".")){
                         Log.d("addTextChangedListener","1 dot is already exist")
                         val str = s.toString()
                         val strold = str.substring(0, str.length - 1)
                         val lastchar = str.substring(str.length - 1)
                         if (strold.contains(".") && lastchar == ".") {
                             val length: Int? = binding.edtAmount.text?.length
                             if (length != null) {
                                 if (length > 0) {
                                     binding.edtAmount.text?.delete(length - 1, length)
                                 }
                             }
                         }

                         return
                     }


                     val temp_rate = s.toString()
                     previousValue = temp_rate
                     if (temp_rate.isNotEmpty()) {
                         if (temp_rate.toFloat() > 100) {
                             AlertUtils.showCustomAlertWithListenerWithOK(
                                 requireContext(),
                                 "Please enter percentage less than or equal to 100"
                             ) { _, _ ->
                                 binding.edtAmount.setText("")
                             }
                         }
                     }

                 }
             }catch (e:Exception){
                 Log.d("addTextChangedListener","exception = $e")
             }
            }

        })
        setupSnackbar()
        observeShowProgress()
        navigate()

        binding.header.txtSave.setOnClickListener {

            val rate = binding.edtAmount.text.toString()
            var rate_double = 0.0
            if (rate.isNotEmpty()) {
                rate_double = MethodUtils.roundOffAmountDouble(rate.toDouble())
            }
            viewModel.submit(rate_double)
        }

        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true /* enabled by default */) {
                override fun handleOnBackPressed() {
                    backPressManage()
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        binding.llAllItemsDialog.setOnClickListener {
            val bundle = Bundle()
            if (isEdit) {
                bundle.putBoolean("isEdit", true)
                bundle.putIntegerArrayList("itemIds", itemIds)
            } else {
                bundle.putIntegerArrayList("itemIds", itemIds)
            }
            bundle.putString("where", "tax")
            findNavController().navigate(R.id.action_newTax_to_itemDialog, bundle)
        }

        binding.llItemPricing.setOnClickListener {
            val bundle = Bundle()
            if (isEdit) {
                //bundle have to sent for item ids

                bundle.putBoolean("isEdit", true)
                LogUtil.logE("itemPricing", itemPricing.toString())
                bundle.putString("itemPricing", itemPricing)
                findNavController().navigate(R.id.action_newTax_to_itemPricingDialog, bundle)
            } else {
                bundle.putString("itemPricing", itemPricing)
                findNavController().navigate(R.id.action_newTax_to_itemPricingDialog, bundle)
            }

        }

        val resultDialogKey = getNavigationResultLiveData<ArrayList<TbItem>>(DIALOG_KEY)
        resultDialogKey?.observe(viewLifecycleOwner) {

            itemIds.clear()
            if (it.size > 0) {
                binding.itemsCount.text = "" + it.size + " Items"
            } else {
                binding.itemsCount.text = "No Items"
            }

            it.forEach {
                itemIds.add(it.itemId)
            }

            viewModel.setItemIds(itemIds)
        }

        val resultDialogKeyTax = getNavigationResultLiveData<String>(DIALOG_KEY_TAX)

        resultDialogKeyTax?.observe(viewLifecycleOwner) { itemPricing1 ->
            itemPricing = itemPricing1
            if (itemPricing == ADD_TAX) {
                binding.tvItemPricing.text = getString(R.string.tv_add_tax_to_item_price)
            } else if (itemPricing == INCLUDE_TAX) {
                binding.tvItemPricing.text = getString(R.string.tv_include_tax_in_item_price)
            }
            viewModel.setItemPricing(itemPricing)

        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<AppCompatImageView>(R.id.imgBack).setOnClickListener {
            backPressManage()
            //findNavController().navigateUp()
        }

    }

    private fun backPressManage() {

        /*   Log.e("itemIdsSizeFrag","itemIdsSize ${taxDataTmp.itemIds.size}")
           viewModel.setItemIds(taxDataTmp.itemIds.toCollection(arrayListOf()))
           viewModel.setTaxData(taxDataTmp)
   */

        val navController = findNavController()
        navController.previousBackStackEntry?.savedStateHandle?.set(
            KEY,
            CREATE_TAX
        )
        navController.popBackStack()
    }

    fun enableTax(isChecked: Boolean) {
        if (isChecked) {
            viewModel.enableTax(isChecked)
        } else {
            viewModel.enableTax(isChecked)
        }
    }

    fun customAmount(isChecked: Boolean) {
        if (isChecked) {
            viewModel.customAmount(isChecked)
        } else {
            viewModel.customAmount(isChecked)
        }
    }

    fun taxType(isChecked: Boolean) {
        if (isChecked) {
            binding.swtTaxType.text = getString(R.string.disc_percentage)
            binding.edtAmount.setText("")
            binding.edtAmount.hint = resources.getString(R.string.add_tax__percentage)
            viewModel.discountType(getString(R.string.disc_percentage))
        } else {
            binding.swtTaxType.text = getString(R.string.dollar_amount)
            binding.edtAmount.hint = resources.getString(R.string.add_tax__dollor)
            binding.edtAmount.setText("")
            viewModel.discountType(getString(R.string.dollar_amount))
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
    }

    private fun navigate() {

        viewModel.data.observe(viewLifecycleOwner, { event ->
            event.getContentIfNotHandled()?.let { createTaxResponse ->
                activity?.let {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        it, createTaxResponse.message
                    ) { _, _ ->
                        if (prefProvider?.getValue("device_token", "")?.trim()?.isEmpty() == true) {
                            val intent = Intent()
                            intent.action = Constants.SYNC_SETTING_NOTIFICATION
                            requireContext().sendBroadcast(intent)

                            val intent2 = Intent()
                            intent2.action = Constants.SYNC_NOTIFICATION
                            requireContext().sendBroadcast(intent2)
                        }
                        backPressManage()
                    }
                }

            }
        })
    }

    private fun setupSnackbar() {
        binding.root.liveSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_SHORT)

    }

}