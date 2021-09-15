package com.android.pos.ui.dialog

import android.graphics.Point
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.EditText
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.android.pos.R
import com.android.pos.databinding.DailogCustomAmountBinding
import com.android.pos.databinding.DialogIssueRefundBinding
import com.android.pos.utils.AmountTextWatcher
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat


@AndroidEntryPoint
class IssueRefundFragment : DialogFragment() {

    private lateinit var binding: DialogIssueRefundBinding

    companion object {
        fun newInstance() = IssueRefundFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.dialog_issue_refund, container, false)
        binding.lifecycleOwner = this


        binding.edtAmount.addTextChangedListener(AmountTextWatcher(binding.edtAmount, false))

        binding.rgRefundType.setOnCheckedChangeListener { group, checkedId ->

            if (checkedId == R.id.rbItems) {
                binding.llItemList.visibility = View.VISIBLE
                binding.llRefundAmount.visibility = View.GONE
            } else if (checkedId == R.id.rbAmount) {
                binding.llItemList.visibility = View.GONE
                binding.llRefundAmount.visibility = View.VISIBLE
            }
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