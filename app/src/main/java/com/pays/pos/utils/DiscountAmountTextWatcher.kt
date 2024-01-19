package com.pays.pos.utils

import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.widget.AppCompatEditText
import com.pays.pos.data.entities.TbItem
import java.text.NumberFormat
import java.util.*

class DiscountAmountTextWatcher(
    private val editText: AppCompatEditText,
    private val isAmount: Boolean,
    private val item: TbItem
) :
    TextWatcher {
    var current = ""
    val TAG = "AmountTextWatcher"
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        if (s.toString() != current) {
            editText.removeTextChangedListener(this)


            val cleanString: String = s.replace("""[$,.%]""".toRegex(), "")


            val parsed = cleanString.toDouble()

            val formatted = NumberFormat.getCurrencyInstance(Locale.US).format((parsed / 100))


            current = formatted
            editText.setText(formatted.replace("""[$,%]""".toRegex(), ""))
            editText.setSelection(formatted.replace("""[$,%]""".toRegex(), "").length)

            if (isAmount) {
                // amount
                if (editText.text.toString().toDouble() > item.price) {
                    editText.setText(MethodUtils.roundOffAmountString(item.price))
                }

            } else {

                // percentage

                if (editText.text.toString().toDouble() > 100) {
                    editText.setText("100.00")
                }
            }

            editText.addTextChangedListener(this)
        }
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun afterTextChanged(s: Editable) {}
}