package com.android.pos.ui.dialog

import android.annotation.SuppressLint
import android.graphics.Point
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.data.entities.TbDiscount
import com.android.pos.data.entities.TbItem
import com.android.pos.data.remote.Constants.AMOUNT
import com.android.pos.data.remote.Constants.PERCENTAGE
import com.android.pos.databinding.DailogAddDiscountBinding
import com.android.pos.databinding.DailogAddVariablePriceBinding
import com.android.pos.ui.adapter.DialogDiscountListAdapter
import com.android.pos.ui.fragments.settings.discount.DiscountListViewModel
import com.android.pos.utils.MethodUtils
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.*

@AndroidEntryPoint
class AddVariablePriceDialog : DialogFragment(), TextWatcher {

    private lateinit var binding: DailogAddVariablePriceBinding

    companion object {
        fun newInstance() = AddVariablePriceDialog()

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.edtAmount.addTextChangedListener(this)

        setKeyPad()

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

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    var current = ""
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

        if (s.toString() != current) {
            binding.edtAmount.removeTextChangedListener(this)


            val cleanString: String = s!!.replace("""[$,.%]""".toRegex(), "")


            val parsed = cleanString.toDouble()

            val formatted = NumberFormat.getCurrencyInstance(Locale.US).format((parsed / 100))


            current = formatted

            binding.edtAmount.setText(formatted.replace("""[$,%]""".toRegex(), ""))
            binding.edtAmount.setSelection(formatted.replace("""[$,%]""".toRegex(), "").length)

            binding.edtAmount.addTextChangedListener(this)
        }
    }

    override fun afterTextChanged(s: Editable?) {

    }

}