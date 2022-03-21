package com.android.pos.ui.dialog

import android.graphics.Point
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.databinding.DailogCustomAmountBinding
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.AmountTextWatcher
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class CustomAmountFragment : DialogFragment() {

    private lateinit var binding: DailogCustomAmountBinding
    var totalprice: Double = 0.0

    companion object {
        fun newInstance() = CustomAmountFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dailog_custom_amount, container, false)
        binding.lifecycleOwner = this


        binding.edtAmount.addTextChangedListener(AmountTextWatcher(binding.edtAmount, false))
        if (arguments != null)
            totalprice = requireArguments().getDouble("totalprice")
/*
        binding.txtAmount.text = "$ " + String.format(
            "%.2f",
            totalprice
        ) + " Cash"
*/

        binding.imgBack.setOnClickListener {
            dismiss()
        }
        binding.txtSend.setOnClickListener {
            if (binding.edtAmount.text.toString().isNotEmpty()){
                var custom_amount = binding.edtAmount.text.toString().replace("$", "").toDouble()
                if (custom_amount > totalprice) {
                    val result = Bundle().apply {
                        putDouble("amount", custom_amount)
                        putDouble("totalAmount", totalprice)
                    }
                    setFragmentResult("request_for_customAmount", result)
                    findNavController().navigateUp()
                } else {
                    AlertUtils.showCustomAlertWithListenerWithOK(
                        requireContext(),
                        "Please enter amount greater than actual amount"
                    ) { _, _ ->
                    }
                }
            }

        }
        setKeyPad()
        return binding.root
    }

    private fun setKeyPad() {
        binding.txt10.text = "$10.00"
        binding.txt20.text = "$20.00"
        binding.txt30.text = "$30.00"

        binding.txt10.setOnClickListener {
            binding.edtAmount.setText("$10.00")
        }
        binding.txt20.setOnClickListener {
            binding.edtAmount.setText("$20.00")
        }
        binding.txt30.setOnClickListener {
            binding.edtAmount.setText("$30.00")
        }

        binding.tvOne.setOnClickListener {
            calculateValue("1", false)

        }

        binding.tvTwo.setOnClickListener {
            calculateValue("2", false)

        }

        binding.tvThree.setOnClickListener {
            calculateValue("3", false)

        }

        binding.tvFour.setOnClickListener {

            calculateValue("4", false)
        }

        binding.tvFive.setOnClickListener {
            calculateValue("5", false)
        }

        binding.tvSix.setOnClickListener {
            calculateValue("6", false)
        }

        binding.tvSeven.setOnClickListener {
            calculateValue("7", false)
        }

        binding.tvEight.setOnClickListener {
            calculateValue("8", false)
        }

        binding.tvNine.setOnClickListener {
            calculateValue("9", false)
        }

        binding.tvZero.setOnClickListener {
            calculateValue("0", false)
        }

        binding.tvClear.setOnClickListener {
            calculateValue("", true)
        }
        binding.tvDZero.setOnClickListener {
            calculateValue("00", false)
        }

    }

    private fun calculateValue(number: String, delete: Boolean) {
        /*discountAdapter.clearSelectedItem()
        selectedListPos = -1*/
        if (binding.edtAmount.text?.length!! > 1 && delete) {
            binding.edtAmount.setText(removeLastCharacter(binding.edtAmount.text.toString()))

        } else {
            binding.edtAmount.append(number)
        }
    }
    private fun removeLastCharacter(str: String): String {
        return str.substring(0, str.length - 1)
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