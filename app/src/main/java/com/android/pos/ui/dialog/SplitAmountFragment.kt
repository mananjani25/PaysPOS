package com.android.pos.ui.dialog

import android.graphics.Point
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.android.pos.R
import com.android.pos.databinding.DailogSplitAmountBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.*

@AndroidEntryPoint
class SplitAmountFragment : DialogFragment(), View.OnClickListener, TextWatcher {

    private var splitAmount: Double = 0.0
    private var isCustom: Boolean = false
    private var totalPrice: Double = 0.0
    private var splitValue: Int = -1
    private lateinit var binding: DailogSplitAmountBinding
    var current = ""

    companion object {
        fun newInstance() = SplitAmountFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DailogSplitAmountBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setupData()

        return binding.root
    }

    private fun setupData() {
        totalPrice = requireArguments().getDouble("totalPrice")
        splitValue = requireArguments().getInt("splitValue")

        binding.edtAmount.addTextChangedListener(this)
        binding.txtCustom.setOnClickListener(this)
        binding.txtContinue.setOnClickListener(this)
        binding.txtSplit2.setOnClickListener(this)
        binding.txtSplit3.setOnClickListener(this)
        binding.txtSplit4.setOnClickListener(this)
        binding.imgBack.setOnClickListener(this)

        (getString(R.string.symbole) + String.format(
            "%.2f",
            totalPrice
        )).also { binding.txtAmount.text = it }
        (getString(R.string.symbole) + String.format(
            "%.2f",
            totalPrice
        )).also { binding.edtAmount.setText(it) }

//        if (splitValue != -1) {
//
//            val splitAfterAmount = totalPrice / splitValue
//            (getString(R.string.symbole) + String.format(
//                "%.2f",
//                splitAfterAmount
//            )).also { binding.edtAmount.setText(it) }
//        }

        //   binding.txtValue.text = "\$24.00 of \$24.00 will remain after this payment."


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

    override fun onClick(v: View?) {

        when (v?.id) {
            R.id.txtContinue -> {

                if (isCustom) {
                    val ss = binding.edtSplitNo.text.toString().trim()
                    if (ss.isNotEmpty()) {
                        splitValue = ss.toInt()
                    }
                } else {

                    val stSplitAmount = binding.edtAmount.text.toString().trim()
                    if (stSplitAmount.isNotEmpty()) {
                        val cleanString: String = stSplitAmount.replace("""[$]""".toRegex(), "")
                        splitAmount = cleanString.trim().toDouble()
                    }
                }

                gotoBack()
            }
            R.id.txtCustom -> {
                isCustom = true
                binding.llCustom.visibility = View.VISIBLE
                binding.llSplit.visibility = View.GONE

            }
            R.id.imgBack -> {
                if (isCustom) {
                    isCustom = false
                    binding.llCustom.visibility = View.GONE
                    binding.llSplit.visibility = View.VISIBLE
                } else
                    dismiss()
            }
            R.id.txtSplit2 -> {

                splitValue = 2
                gotoBack()
            }
            R.id.txtSplit3 -> {
                splitValue = 3
                gotoBack()
            }
            R.id.txtSplit4 -> {
                splitValue = 4
                gotoBack()
            }
        }

    }

    private fun gotoBack() {
        val result = Bundle().apply {
            putInt("split", splitValue)
            putDouble("splitByAmount", splitAmount)
        }
        setFragmentResult("request_key_split", result)
        findNavController().navigateUp()
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        if (s.toString() != current) {
            binding.edtAmount.removeTextChangedListener(this)

            val cleanString: String = s!!.replace("""[$,.]""".toRegex(), "")

            val parsed = cleanString.trim().toDouble()
            val formatted = NumberFormat.getCurrencyInstance(Locale.US).format((parsed / 100))

            current = formatted
            binding.edtAmount.setText(formatted.replace("""[,]""".toRegex(), ""))
            binding.edtAmount.setSelection(formatted.replace("""[,]""".toRegex(), "").length)

            val enterPrice = binding.edtAmount.text!!.replace("""[$]""".toRegex(), "").toDouble()

            var remainAmount = 0.0

            if (enterPrice > totalPrice) {

                (getString(R.string.symbole) + String.format(
                    "%.2f",
                    totalPrice
                )).also { binding.edtAmount.setText(it) }

            } else {

                remainAmount = totalPrice - enterPrice
            }


            val remainAmountFormat =
                getString(R.string.symbole) + String.format("%.2f", remainAmount)
            val totalAmountFormat = getString(R.string.symbole) + String.format("%.2f", totalPrice)

            binding.txtValue.text =
                "$remainAmountFormat of $totalAmountFormat will remain after this payment."

            binding.edtAmount.addTextChangedListener(this)
        }
    }

    override fun afterTextChanged(s: Editable?) {
        val value = s.toString()
        if (value.isNotEmpty()) {

            Log.e("remainAmount", value.toString())

        }

    }
}