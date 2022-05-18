package com.android.pos.ui.dialog

import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Bundle
import android.text.InputFilter
import android.text.Spanned
import android.view.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.databinding.AddOnlineTimeDiialogBinding
import com.android.pos.di.PrefProvider
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class AddOnlineTimeDialog : DialogFragment() {


    @Inject
    lateinit var prefProvider: PrefProvider
    private var isFromDetails = false
    private var isFromTransaction = false
    lateinit var binding: AddOnlineTimeDiialogBinding
    var doneOnce = false
    var timeFilter: InputFilter? = null

    companion object {
        fun newInstance() = AddOnlineTimeDialog()
    }

    var string_final: StringBuffer = StringBuffer()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        val back = ColorDrawable(Color.WHITE)
        val inset = InsetDrawable(back, 150, 100, 150, 100)
        dialog?.window?.setBackgroundDrawable(inset);

        binding.txtTitle?.text = getString(R.string.add_time_online)
        setupData()
        setKeyPad()

    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = AddOnlineTimeDiialogBinding.inflate(inflater, container, false)
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        return binding.root
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
//
//            val result = Bundle().apply {
//                setFragmentResult("request_key_time", result)

            findNavController().navigateUp()
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


    private fun calculateValue(number: String, delete: Boolean) {
        if (binding.edtAmount?.text?.length!! > 0 && delete) {
            binding.edtAmount?.setText(removeLastCharacter(binding?.edtAmount!!.text.toString()))
        } else {
            binding.edtAmount.append(number)
//            if (binding.edtAmount.text?.length == 2) {
//                binding.edtAmount.append(":")
//                binding.edtAmount.append(number)
//            } else {
//                binding.edtAmount.append(number)
//            }
        }
    }

    private fun removeLastCharacter(str: String): String {
        return str.substring(0, str.length - 1)
    }


}