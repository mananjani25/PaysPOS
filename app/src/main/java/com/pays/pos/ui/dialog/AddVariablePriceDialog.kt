package com.pays.pos.ui.dialog

import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Display
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.data.entities.VariationsAttribute
import com.pays.pos.data.remote.Constants.DIALOG_KEY_VARIATION_DETAILS
import com.pays.pos.databinding.DailogAddVariablePriceBinding
import com.pays.pos.utils.AmountTextWatcher
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.extensions.setNavigationResult
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddVariablePriceDialog : DialogFragment() {

    private lateinit var binding: DailogAddVariablePriceBinding
    private var variationAttribute: VariationsAttribute? = null

    companion object {
        fun newInstance() = AddVariablePriceDialog()

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        val back = ColorDrawable(ContextCompat.getColor(binding.root.context,R.color.bg_color))
        val inset = InsetDrawable(back, 150, 100, 150, 100)
        dialog?.window?.setBackgroundDrawable(inset);

        variationAttribute =
            arguments?.getParcelable("variationAttribute")

        if (variationAttribute?.price != null) {
            binding.edtAmount.setText(MethodUtils.roundOffAmountString(variationAttribute?.price!!))

        } else {
            binding.edtAmount.setText("")
        }
        binding.edtAmount.addTextChangedListener(AmountTextWatcher(binding.edtAmount, false))
        binding.txtSave.setOnClickListener {
            if (TextUtils.isEmpty(binding.edtAmount.text.toString())) {
                //   variationAttribute?.price = null
//                variationAttribute?.priceType = "Variable"
            } else {

                variationAttribute?.price =
                    binding.edtAmount.text.toString().replace("$", "").toDouble()

//                variationAttribute?.priceType = "Fixed"
            }

            var bundle: Bundle = Bundle()
            bundle.putParcelable("data", variationAttribute)
            setNavigationResult(DIALOG_KEY_VARIATION_DETAILS, variationAttribute)
            findNavController().popBackStack()
        }

        setKeyPad()

        binding.imgBack.setOnClickListener {
            dismiss()
        }

        binding.llKeypad.txtClear.setOnClickListener {

            binding.edtAmount.setText("0.00")

        }

        binding.llKeypad.txt10.text = "$10"
        binding.llKeypad.txt20.text = "$20"
        binding.llKeypad.txt30.text = "$30"

        binding.llKeypad.txt10.setOnClickListener {
            val price = binding.llKeypad.txt10.text.toString().trim()
                .substring(1, binding.llKeypad.txt10.text.toString().length).toDouble()
            binding.edtAmount.setText(MethodUtils.roundOffAmountString(price))
        }
        binding.llKeypad.txt20.setOnClickListener {
            val price = binding.llKeypad.txt20.text.toString().trim()
                .substring(1, binding.llKeypad.txt20.text.toString().length ).toDouble()
            binding.edtAmount.setText(MethodUtils.roundOffAmountString(price))
        }
        binding.llKeypad.txt30.setOnClickListener {
            val price = binding.llKeypad.txt30.text.toString().trim()
                .substring(1, binding.llKeypad.txt30.text.toString().length ).toDouble()
            binding.edtAmount.setText(MethodUtils.roundOffAmountString(price))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DailogAddVariablePriceBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

    private fun setKeyPad() {
        binding.llKeypad.tvOne.setOnClickListener {
            calculateValue("1", false)

        }

        binding.llKeypad.tvTwo.setOnClickListener {
            calculateValue("2", false)

        }

        binding.llKeypad.tvThree.setOnClickListener {
            calculateValue("3", false)

        }

        binding.llKeypad.tvFour.setOnClickListener {

            calculateValue("4", false)
        }

        binding.llKeypad.tvFive.setOnClickListener {
            calculateValue("5", false)
        }

        binding.llKeypad.tvSix.setOnClickListener {
            calculateValue("6", false)
        }

        binding.llKeypad.tvSeven.setOnClickListener {
            calculateValue("7", false)
        }

        binding.llKeypad.tvEight.setOnClickListener {
            calculateValue("8", false)
        }

        binding.llKeypad.tvNine.setOnClickListener {
            calculateValue("9", false)
        }

        binding.llKeypad.tvZero.setOnClickListener {
            calculateValue("0", false)
        }

        binding.llKeypad.tvClear.setOnClickListener {
            calculateValue("", true)
        }
        binding.llKeypad.tvDZero.setOnClickListener {
            calculateValue("00", false)
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

    fun calculateValue(number: String, delete: Boolean) {

        if (binding.edtAmount.text?.length!! > 1 && delete) {
            binding.edtAmount.setText(removeLastCharacter(binding.edtAmount.text.toString()))

        } else {
            binding.edtAmount.append(number)


        }
    }

    private fun removeLastCharacter(str: String): String {
        return str.substring(0, str.length - 1)
    }

}