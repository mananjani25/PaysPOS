package com.pays.pos.utils

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import androidx.appcompat.widget.AppCompatEditText
import java.lang.String.format
import java.text.NumberFormat
import java.util.*
import java.util.regex.Pattern

//By Dharmesh Basapati
//Created this new Class same as AmountTextWatcher.kt for resolving BIS-294 and 295 issue(s)
class ItemPriceTextWatcher(private val editText: AppCompatEditText, private val isManual: Boolean) :
    TextWatcher {
    var current = ""
    val TAG = "ItemPriceTextWatcher"
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        editText.setInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        if (s.toString() != current && s.length < 12) { // Increased 9 to 12 in length check for accepting this - 9999999.99
            editText.removeTextChangedListener(this)


            val cleanString: String = s.replace("""[$,.]""".toRegex(), "")


            val parsed = cleanString.trim().toDouble()

            val formatted =
                if (isManual) {
                    NumberFormat.getCurrencyInstance(Locale.US).format((parsed / 100))
                } else {
                    NumberFormat.getCurrencyInstance(Locale.US).format((parsed / 100))
                }

            current = formatted
            editText.setText(formatted.replace("""[,]""".toRegex(), ""))
            editText.setSelection(formatted.replace("""[,]""".toRegex(), "").length)

            editText.addTextChangedListener(this)
        }
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        editText.setInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
    }

    override fun afterTextChanged(s: Editable) {
        editText.setInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
    }
}