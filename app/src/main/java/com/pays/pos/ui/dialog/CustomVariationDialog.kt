package com.pays.pos.ui.dialog

import android.graphics.Point
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.data.entities.VariationsAttribute
import com.pays.pos.data.remote.Constants.DIALOG_KEY_ADD_VARIATION_DETAILS
import com.pays.pos.databinding.DialogCustomVariationBinding
import com.pays.pos.utils.AlertUtils
import com.pays.pos.utils.AmountTextWatcher
import com.pays.pos.utils.ItemPriceTextWatcher
import com.pays.pos.utils.extensions.setNavigationResult
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
            ItemPriceTextWatcher(
                binding.tvVariationsPrice,
                false
            )
        )
        if (variationAttribute.price != null) {
            binding.tvVariationsPrice.setText(
                activity.getString(R.string.symbole) + " " + String.format(
                    activity.getString(R.string.format),
                    variationAttribute?.price
                )
            )
        } else {
            binding.tvVariationsPrice.setText("")
        }


        setUpUnitTypeSpinnerAdapter()
        onTextChanged()
        return binding.root
    }

    private fun onTextChanged() {
        binding.tvVariationsStock.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(
                s: CharSequence, start: Int, before: Int,
                count: Int
            ) {
                binding.tvVariationsStock.setInputType(InputType.TYPE_CLASS_NUMBER )

                val enteredString = s.toString()
                val digits = TextUtils.isDigitsOnly(s)

                if (enteredString.startsWith("0")) {

                    if (enteredString.length > 0) {
                        binding.tvVariationsStock.setText(enteredString.substring(1))
                    } else {
                        binding.tvVariationsStock.setText("")
                    }
                }
                else if (digits && s.toString().trim().isNotEmpty() && s.toString().toInt() > 10000) {
                    binding.tvVariationsStock.setText("10000")
                }

            }

            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int,
                after: Int
            ) {
                binding.tvVariationsStock.setInputType(InputType.TYPE_CLASS_NUMBER )
            }

            override fun afterTextChanged(s: Editable?) {
                binding.tvVariationsStock.setInputType(InputType.TYPE_CLASS_NUMBER )

            }
        })


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

                    if (TextUtils.isEmpty(binding.tvVariationsPrice.text.toString())) {
                        variationAttribute?.price = 0.0
                        //   variationAttribute?.price = null
                        variationAttribute?.priceType = "Variable"
                    } else {
                        variationAttribute.price =
                            binding.tvVariationsPrice.text.toString().replace("$", "").toDouble()

//                        variationAttribute?.price = 0.0
                        variationAttribute?.priceType = "Fixed"
                    }


                    variationAttribute.sku = binding.tvVariationsSku.text.toString()
                    variationAttribute.stockQty = binding.tvVariationsStock.text.toString()
                    variationAttribute.isCustom = true


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