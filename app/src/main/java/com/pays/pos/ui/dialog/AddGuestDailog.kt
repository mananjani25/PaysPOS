package com.pays.pos.ui.dialog

import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.databinding.AddGuestLayoutBinding
import com.pays.pos.databinding.AddOnlineTimeDiialogBinding
import com.pays.pos.utils.AlertUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddGuestDailog : DialogFragment() {


    lateinit var binding: AddGuestLayoutBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = AddGuestLayoutBinding.inflate(inflater, container, false)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        val back = ColorDrawable(ContextCompat.getColor(binding.root.context,R.color.bg_color))
        val inset = InsetDrawable(back, 150, 100, 150, 130)
        dialog?.window?.setBackgroundDrawable(inset);
        setupData()
        setKeyPad()
    }

    private fun setKeyPad() {
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


    }


    private fun setupData() {

        binding.txtClear.setOnClickListener {
            binding.edtAmount.text?.clear()
        }
        binding.imgBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.txtSave.setOnClickListener {
            var finalstring = binding.edtAmount.text.toString()
            if (finalstring.isEmpty()) {
                AlertUtils.showCustomAlert(requireContext(), "Please enter Guest count.")
            } else {
                val result = Bundle().apply {
                    putInt("count", finalstring.toInt())
                }
                requireActivity().supportFragmentManager.setFragmentResult("request_for_guestcount", result)

                findNavController().navigateUp()
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
    private fun removeLastCharacter(str: String): String {
        return str.substring(0, str.length - 1)
    }
    private fun calculateValue(number: String, delete: Boolean) {
        if (binding.edtAmount?.text?.length!! > 0 && delete) {
            binding.edtAmount?.setText(removeLastCharacter(binding?.edtAmount!!.text.toString()))
        } else {
            binding.edtAmount.append(number)
            if (binding.edtAmount.text!!.length >= 2) {
                var value = binding.edtAmount.text.toString().toInt()
                if (value > 15) {
                    binding.edtAmount.text!!.clear()
                    binding.edtAmount.append("15")
                }
            }
        }
    }
}