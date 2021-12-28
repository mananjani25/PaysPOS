package com.android.pos.ui.dialog

import android.graphics.Point
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.EditText
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.databinding.DailogCustomAmountBinding
import com.android.pos.utils.AmountTextWatcher
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat


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
        totalprice = requireArguments().getDouble("totalprice")
        binding.txtAmount.text = "$ " + String.format(
            "%.2f",
            totalprice
        ) + " Cash"

        binding.imgBack.setOnClickListener {
            dismiss()
        }
        binding.txtSend.setOnClickListener {
            val result = Bundle().apply {
                putDouble("amount", binding.edtAmount.text.toString().replace("$", "").toDouble())
            }
            setFragmentResult("request_for_customAmount", result)
            findNavController().navigateUp()
        }
        return binding.root
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