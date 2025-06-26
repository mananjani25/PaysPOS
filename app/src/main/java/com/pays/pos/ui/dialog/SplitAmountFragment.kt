package com.pays.pos.ui.dialog

import android.R.attr.maxLength
import android.graphics.Point
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.*
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.pays.pos.R
import com.pays.pos.data.remote.Constants
import com.pays.pos.databinding.DailogSplitAmountBinding
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.LogUtil
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.*
import javax.inject.Inject


@AndroidEntryPoint
class SplitAmountFragment : DialogFragment(), View.OnClickListener, TextWatcher {

    private var splitAmount: Double = 0.0
    private var isCustom: Boolean = false
    private var totalPrice: Double = 0.0
    private var splitValue: Int = 1
    private var paymentAmount : Double = 0.0
    private var isCashDiscount = ""
//    private var isAmountWiseSplit: Boolean = false
    private lateinit var binding: DailogSplitAmountBinding
    var current = ""

    @Inject
    lateinit var prefProvider: PrefProvider

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
        setupInputFilter()
        return binding.root
    }

    /*override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isAmountWiseSplit = requireArguments().getBoolean("amountWiseSplit", false)

        if (isAmountWiseSplit) {
            binding.splitDialogTextView.text = "Amount to split"
        }
    }*/

    private fun setupInputFilter() {

        binding.edtSplitNo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                p0?.let {
                    if (it.toString().isNotEmpty()) {
                        if (it.toString().toInt() > 20) {
                            binding.edtSplitNo.setText("20")
                            binding.edtSplitNo.setSelection(2)
                        } else if (it.toString().toInt() == 0) {
                            binding.edtSplitNo.setText("")
                            binding.edtSplitNo.setSelection(0)
                        }
                    }
                }
            }

            override fun afterTextChanged(p0: Editable?) {

            }
        })
    }

    private fun setupData() {
        totalPrice = prefProvider.getValue(Constants.WHOLE_AMOUNT, "0.0").toDouble()
        paymentAmount = arguments?.getDouble("paymentAmount",0.0) ?: 0.0
        isCashDiscount = arguments?.getString("isCashDiscount","") ?: ""
//        splitValue = requireArguments().getInt("splitValue")

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
        )).also {
            if(isCashDiscount == "CashDiscount"){

                binding.txtAmount.text = getString(R.string.symbole) + String.format("%.2f",paymentAmount)
            }
            else binding.txtAmount.text = it
        }
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
                val ss = binding.edtSplitNo.text.toString().trim()
                if (ss.isNotEmpty()) {
                    splitValue = ss.toInt()
                }
//                if (isCustom) {
//
//                } else {
//
//                    val stSplitAmount = binding.edtAmount.text.toString().trim()
//                    if (stSplitAmount.isNotEmpty()) {
//                        val cleanString: String = stSplitAmount.replace("""[$]""".toRegex(), "")
//                        splitAmount = cleanString.trim().toDouble()
//                    }
//                }

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
        requireActivity().supportFragmentManager.setFragmentResult("request_key_split", result)
        findNavController().navigateUp()
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        if (s.toString() != current) {
            binding.edtAmount.removeTextChangedListener(this)

            try {
                val cleanString: String = s!!.replace("""[$,.]""".toRegex(), "")
                val parsed = cleanString.trim().toDouble()
                val formatted = NumberFormat.getCurrencyInstance(Locale.US).format((parsed / 100))

                current = formatted
                binding.edtAmount.setText(formatted.replace("""[,]""".toRegex(), ""))
                binding.edtAmount.setSelection(formatted.replace("""[,]""".toRegex(), "").length)

                val enterPrice =
                    binding.edtAmount.text!!.replace("""[$]""".toRegex(), "").toDouble()

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
                val totalAmountFormat =
                    getString(R.string.symbole) + String.format("%.2f", totalPrice)

                binding.txtValue.text =
                    "$remainAmountFormat of $totalAmountFormat will remain after this payment."
            } catch (e: Exception) {
            }
            binding.edtAmount.addTextChangedListener(this)
        }
    }

    override fun afterTextChanged(s: Editable?) {
        val value = s.toString()
        if (value.isNotEmpty()) {

            LogUtil.logE("remainAmount", value.toString())

        }

    }
}