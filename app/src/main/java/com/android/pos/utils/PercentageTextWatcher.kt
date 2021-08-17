package com.android.pos.utils

import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.widget.AppCompatEditText
import java.text.NumberFormat
import java.util.*

class PercentageTextWatcher(private val editText: AppCompatEditText) : TextWatcher {
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

            editText.addTextChangedListener(this)
        }
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun afterTextChanged(s: Editable) {}
}