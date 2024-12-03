package com.pays.pos.utils

import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.widget.AppCompatEditText
import com.pays.pos.data.entities.Modifier
import java.text.NumberFormat
import java.util.*

class PriceTextWatcher(private val editText: AppCompatEditText, private val data: Modifier) :
    TextWatcher {
    var current = ""
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        if (s.toString() != current && s.length < 9) {
            editText.removeTextChangedListener(this)

            val cleanString: String = s.replace("""[$,.]""".toRegex(), "")

//            val parsed = cleanString.trim().toDouble()
            val parsed = cleanString.trim().toDoubleOrNull() ?: 0.0
            val formatted = NumberFormat.getCurrencyInstance(Locale.US).format((parsed / 100))

            current = formatted
            editText.setText(formatted.replace("""[,]""".toRegex(), ""))
            editText.setSelection(formatted.replace("""[,]""".toRegex(), "").length)

            data.price = formatted.replace("""[$,]""".toRegex(), "").toDouble()

            editText.addTextChangedListener(this)
        }
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun afterTextChanged(s: Editable) {}
}