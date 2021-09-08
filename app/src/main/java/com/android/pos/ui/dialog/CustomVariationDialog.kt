package com.android.pos.ui.dialog

import android.app.Activity
import android.graphics.Point
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.android.pos.R
import com.android.pos.data.entities.VariationsAttribute
import com.android.pos.data.model.OptionListModel
import com.android.pos.data.model.VariationListModel
import com.android.pos.data.remote.Constants.ADD_TAX
import com.android.pos.data.remote.Constants.DIALOG_KEY
import com.android.pos.data.remote.Constants.DIALOG_KEY_ADD_VARIATION_DETAILS
import com.android.pos.data.remote.Constants.DIALOG_KEY_TAX
import com.android.pos.data.remote.Constants.DIALOG_KEY_VARIATION_DETAILS
import com.android.pos.data.remote.Constants.DIALOG_KEY_VARIATION_DETAILS_POSITION
import com.android.pos.data.remote.Constants.DIALOG_KEY_VARIATION_DETAILS_REMOVE
import com.android.pos.data.remote.Constants.INCLUDE_TAX
import com.android.pos.databinding.DialogCustomVariationBinding
import com.android.pos.databinding.DialogEditItemTitleBinding
import com.android.pos.databinding.DialogEditVariationBinding
import com.android.pos.databinding.DialogItemPricingBinding
import com.android.pos.ui.adapter.ChooseColorsAdapter
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.AmountTextWatcher
import com.android.pos.utils.extensions.alert
import com.android.pos.utils.extensions.setNavigationResult
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class CustomVariationDialog : DialogFragment(), View.OnClickListener,
    AdapterView.OnItemSelectedListener {
    private lateinit var binding: DialogCustomVariationBinding
    private var variationAttribute = VariationsAttribute()
    val builder = StringBuilder()
    private var unitTypeList = ArrayList<String>()

    var activity: CustomVariationDialog = this

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.dialog_custom_variation, container, false)
        binding.lifecycleOwner = this

        binding.imgBack.setOnClickListener(this)
        binding.txtDone.setOnClickListener(this)
        binding.spUnit.onItemSelectedListener = this


        binding.tvVariationsPrice.addTextChangedListener(
            AmountTextWatcher(
                binding.tvVariationsPrice,
                false
            )
        )
        binding.tvVariationsPrice.setText(
            activity.getString(R.string.symbole) + " " + String.format(
                activity.getString(R.string.format),
                variationAttribute?.price
            )
        )

        setUpUnitTypeSpinnerAdapter()

        return binding.root
    }

    private fun setUpUnitTypeSpinnerAdapter() {
        unitTypeList.clear()
        unitTypeList.add(getString(R.string.tv_unit_fixed))
        unitTypeList.add(getString(R.string.tv_unit_variable))

        val spinnerAdapter = ArrayAdapter(
            requireActivity(),
            R.layout.row_spinner_variation,
            unitTypeList
        )

        spinnerAdapter.setDropDownViewResource(R.layout.row_spinner)
        binding.spUnit.adapter = spinnerAdapter
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onClick(v: View?) {


        when (v?.id) {
            R.id.imgBack -> {
                dismiss()
            }
            R.id.txtDone -> {

                if (TextUtils.isEmpty(binding.etVariationsName.text.toString())) {
                    activity.let {
                        AlertUtils.showCustomAlertWithListenerWithOK(
                            requireContext(), getString(R.string.variation_name_validate)
                        ) { _, _ ->

                        }
                    }

                } else {
                    variationAttribute.name = binding.etVariationsName.text.toString()
                    variationAttribute.price =
                        binding.tvVariationsPrice.text.toString().replace("$", "").toDouble()
                    variationAttribute.sku = binding.tvVariationsSku.text.toString()
                    variationAttribute.stockQty = binding.tvVariationsStock.text.toString()


                    setNavigationResult(DIALOG_KEY_ADD_VARIATION_DETAILS, variationAttribute)
                    findNavController().popBackStack()
                }
            }

        }
    }

    override fun onResume() {
        super.onResume()

        val window: Window? = dialog!!.window
        val size = Point()
        val display: Display = window?.windowManager?.defaultDisplay!!
        display.getSize(size)
        val width: Int = size.x
        window.setLayout((width * 0.50).toInt(), WindowManager.LayoutParams.MATCH_PARENT)
        window.setGravity(Gravity.CENTER)
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (binding.spUnit.selectedItem == getString(R.string.tv_unit_fixed)) {
            binding.llPrice.visibility = View.VISIBLE

        } else if (binding.spUnit.selectedItem == getString(R.string.tv_unit_variable)) {
            binding.llPrice.visibility = View.GONE

        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {

    }


}