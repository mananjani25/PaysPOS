package com.android.pos.ui.dialog

import android.app.Activity
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.*
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
import com.android.pos.data.remote.Constants.DIALOG_KEY_TAX
import com.android.pos.data.remote.Constants.DIALOG_KEY_VARIATION_DETAILS
import com.android.pos.data.remote.Constants.DIALOG_KEY_VARIATION_DETAILS_POSITION
import com.android.pos.data.remote.Constants.DIALOG_KEY_VARIATION_DETAILS_REMOVE
import com.android.pos.data.remote.Constants.INCLUDE_TAX
import com.android.pos.databinding.DialogEditItemTitleBinding
import com.android.pos.databinding.DialogEditVariationBinding
import com.android.pos.databinding.DialogItemPricingBinding
import com.android.pos.ui.adapter.ChooseColorsAdapter
import com.android.pos.utils.AmountTextWatcher
import com.android.pos.utils.extensions.setNavigationResult
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class CustomVariationDialog : DialogFragment(), View.OnClickListener {
    private lateinit var binding: DialogEditVariationBinding
    private var variationAttribute: VariationsAttribute? = null
    private var variationAttributeCopy = ArrayList<VariationsAttribute>()
    val builder = StringBuilder()

    var activity: CustomVariationDialog = this

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.dialog_edit_variation, container, false)
        binding.lifecycleOwner = this

        binding.imgBack.setOnClickListener(this)
        binding.txtDone.setOnClickListener(this)

        variationAttribute =
            arguments?.getParcelable("variationAttributeList")


        binding.tvVariationsName.setText(variationAttribute?.name)

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
        binding.tvVariationsSku.setText(variationAttribute?.sku)
        binding.tvVariationsStock.setText(variationAttribute?.stockQty)

        binding.tvRemoveVariation.setOnClickListener {
            setNavigationResult(DIALOG_KEY_VARIATION_DETAILS_REMOVE, variationAttribute)
            findNavController().popBackStack()
        }


        return binding.root
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

                variationAttribute?.name = binding.tvVariationsName.text.toString()

                variationAttribute?.price =
                    binding.tvVariationsPrice.text.toString().replace("$", "").toDouble()
                variationAttribute?.sku = binding.tvVariationsSku.text.toString()
                variationAttribute?.stockQty = binding.tvVariationsStock.text.toString()


                setNavigationResult(DIALOG_KEY_VARIATION_DETAILS, variationAttribute)
                findNavController().popBackStack()
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


}