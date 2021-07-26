package com.android.pos.ui.dialog

import android.graphics.Point
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.android.pos.R
import com.android.pos.data.model.OptionListModel
import com.android.pos.data.remote.Constants.DIALOG_KEY
import com.android.pos.data.remote.Constants.DIALOG_KEY_TAX
import com.android.pos.databinding.DialogEditItemTitleBinding
import com.android.pos.databinding.DialogItemPricingBinding
import com.android.pos.ui.adapter.ChooseColorsAdapter
import com.android.pos.utils.extensions.setNavigationResult
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



        if (isEdit) {
            itemPricing = arguments?.getString("itemPricing")!!
            if (itemPricing == "Add Tax To Item Price") {
                binding.ivAddTax.setImageResource(R.drawable.ic_outline_radio_button_checked)
                binding.ivIncludeTax.setImageResource(R.drawable.ic_uncheck_circle)

            } else if (itemPricing == "Include Tax in Item Price") {
                binding.ivIncludeTax.setImageResource(R.drawable.ic_outline_radio_button_checked)
                binding.ivAddTax.setImageResource(R.drawable.ic_uncheck_circle)
            }
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
                setNavigationResult(DIALOG_KEY_TAX, ADD_INCLUDE_TAX)
                findNavController().popBackStack()
            }
            R.id.llAddTax -> {
                ADD_INCLUDE_TAX = "Add Tax To Item Price"
                binding.ivAddTax.setImageResource(R.drawable.ic_outline_radio_button_checked)
                binding.ivIncludeTax.setImageResource(R.drawable.ic_uncheck_circle)
            }
            R.id.llIncludeTax -> {
                ADD_INCLUDE_TAX = "Include Tax in Item Price"
                binding.ivIncludeTax.setImageResource(R.drawable.ic_outline_radio_button_checked)
                binding.ivAddTax.setImageResource(R.drawable.ic_uncheck_circle)
            }

        }
    }


}