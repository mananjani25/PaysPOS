package com.pays.pos.ui.dialog

import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.pays.pos.R
import com.pays.pos.data.model.OptionListModel
import com.pays.pos.data.remote.Constants.ADD_TAX
import com.pays.pos.data.remote.Constants.DIALOG_KEY
import com.pays.pos.data.remote.Constants.DIALOG_KEY_TAX
import com.pays.pos.data.remote.Constants.INCLUDE_TAX
import com.pays.pos.databinding.DialogEditItemTitleBinding
import com.pays.pos.databinding.DialogItemPricingBinding
import com.pays.pos.ui.adapter.ChooseColorsAdapter
import com.pays.pos.utils.extensions.setNavigationResult
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class ItemPricingDialog : DialogFragment(), View.OnClickListener {
    private lateinit var binding: DialogItemPricingBinding
    private var ADD_INCLUDE_TAX: String = ""
    var isEdit: Boolean = false
    var itemPricing: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.dialog_item_pricing, container, false)
        binding.lifecycleOwner = this

        binding.imgBack.setOnClickListener(this)
        binding.txtDone.setOnClickListener(this)
        binding.llAddTax.setOnClickListener(this)
        binding.llIncludeTax.setOnClickListener(this)

        isEdit = arguments?.getBoolean("isEdit")!!


        // if (isEdit) {
        itemPricing = arguments?.getString("itemPricing").toString()
        if (itemPricing == ADD_TAX) {
            binding.ivAddTax.setImageResource(R.drawable.ic_outline_radio_button_checked)
            binding.ivIncludeTax.setImageResource(R.drawable.ic_uncheck_circle)

        } else if (itemPricing == INCLUDE_TAX) {
            binding.ivIncludeTax.setImageResource(R.drawable.ic_outline_radio_button_checked)
            binding.ivAddTax.setImageResource(R.drawable.ic_uncheck_circle)
        }
        //  }

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
                setNavigationResult(DIALOG_KEY_TAX, ADD_INCLUDE_TAX)
                findNavController().popBackStack()
            }
            R.id.llAddTax -> {
                itemPricing = ADD_TAX
                ADD_INCLUDE_TAX = ADD_TAX
                binding.ivAddTax.setImageResource(R.drawable.ic_outline_radio_button_checked)
                binding.ivIncludeTax.setImageResource(R.drawable.ic_uncheck_circle)
            }
            R.id.llIncludeTax -> {
                itemPricing = INCLUDE_TAX
                ADD_INCLUDE_TAX = INCLUDE_TAX
                binding.ivIncludeTax.setImageResource(R.drawable.ic_outline_radio_button_checked)
                binding.ivAddTax.setImageResource(R.drawable.ic_uncheck_circle)
            }

        }
    }


}