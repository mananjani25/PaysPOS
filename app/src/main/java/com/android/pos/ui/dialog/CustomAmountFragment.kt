package com.android.pos.ui.dialog

import android.annotation.SuppressLint
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
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.AmountTextWatcher
import com.android.pos.utils.MethodUtils
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.*


@AndroidEntryPoint
class CustomAmountFragment : DialogFragment() {

    private lateinit var binding: DailogCustomAmountBinding

    companion object {
        fun newInstance() = CustomAmountFragment()
    }

    var totalprice: Double = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dailog_custom_amount, container, false)
        binding.lifecycleOwner = this

        binding.edtAmount.addTextChangedListener(AmountTextWatcher(binding.edtAmount, false))
        totalprice = requireArguments().getDouble("totalprice")
        binding.txtAmount.setText(MethodUtils.roundOffAmount(totalprice) + " Cash")
        binding.imgBack.setOnClickListener {
            findNavController().navigateUp()
        }
        binding.txtSend.setOnClickListener {
            val amount = binding.edtAmount.text.toString().replace("$","").toDouble()
            if (amount <totalprice) {
                AlertUtils.showCustomAlert(
                    requireActivity(),
                    "You can't enter les than Total Amount"
                )

            } else {
                goBack(amount)
            }
        }

        return binding.root
    }

    fun goBack(amount: Double) {
        val result = Bundle().apply {
            putDouble("amount", amount)
        }
        setFragmentResult("request_for_customAmount", result)
        findNavController().navigateUp()
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