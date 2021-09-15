package com.android.pos.ui.dialog

import android.graphics.Point
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.EditText
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.android.pos.R
import com.android.pos.data.model.responseModel.GetOrderDetailsResponse
import com.android.pos.databinding.DailogCustomAmountBinding
import com.android.pos.databinding.DialogIssueRefundBinding
import com.android.pos.ui.fragments.transactions.TransactionDetailsViewModel
import com.android.pos.utils.AmountTextWatcher
import com.android.pos.utils.MethodUtils
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.*


@AndroidEntryPoint
class IssueRefundFragment : DialogFragment(), TextWatcher {

    private lateinit var binding: DialogIssueRefundBinding
    private lateinit var orderDetailsResponse: GetOrderDetailsResponse
    private val viewModel by viewModels<TransactionDetailsViewModel>()

    companion object {
        fun newInstance() = IssueRefundFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_issue_refund, container, false)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel


       // binding.edtAmount.addTextChangedListener(AmountTextWatcher(binding.edtAmount, false))

        binding.rgRefundType.setOnCheckedChangeListener { group, checkedId ->

            if (checkedId == R.id.rbItems) {
                binding.llItemList.visibility = View.VISIBLE
                binding.llRefundAmount.visibility = View.GONE
            } else if (checkedId == R.id.rbAmount) {
                binding.llItemList.visibility = View.GONE
                binding.llRefundAmount.visibility = View.VISIBLE
            }
        }

        orderDetailsResponse = arguments?.getParcelable("orderDetailsResponse")!!
        binding.orderDetails = orderDetailsResponse
        binding.edtAmount.addTextChangedListener(this)

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




            if (binding.edtAmount.text.toString()
                    .toDouble() > orderDetailsResponse.data.totalAmount
            ) {
                binding.edtAmount.setText(MethodUtils.roundOffAmountString(orderDetailsResponse.data.totalAmount))
            }

            binding.edtAmount.addTextChangedListener(this)
        }
    }

    override fun afterTextChanged(s: Editable?) {

    }


}