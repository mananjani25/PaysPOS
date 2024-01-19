package com.pays.pos.utils

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import androidx.appcompat.widget.AppCompatEditText
import java.lang.String.format
import java.text.NumberFormat
import java.util.*

class AmountWatcher(private val editText: AppCompatEditText) :
    TextWatcher {
    var current = ""
    val TAG = "AmountTextWatcher"
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        if (s.toString() != current && s.length<9) {
            editText.removeTextChangedListener(this)


            val cleanString: String = s.replace("""[$,.]""".toRegex(), "")


            val parsed = cleanString.trim().toDouble()

            val formatted = NumberFormat.getCurrencyInstance(Locale.US).format((parsed / 100))


            current = formatted
            editText.setText(formatted.replace("""[,]""".toRegex(), ""))
            editText.setSelection(formatted.replace("""[,]""".toRegex(), "").length)

            editText.addTextChangedListener(this)
        }
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun afterTextChanged(s: Editable) {}
}