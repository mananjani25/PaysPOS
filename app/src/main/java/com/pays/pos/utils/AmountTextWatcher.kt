package com.pays.pos.utils

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import androidx.appcompat.widget.AppCompatEditText
import java.text.NumberFormat
import java.util.*

class AmountTextWatcher(
    private val editText: AppCompatEditText,
    private val isManual: Boolean,
    private val maxValue: Double? = null, // maxValue is optional and defaults to null
    var isFromGiftCard: Boolean = false
) :
    TextWatcher {
    var current = ""
    val TAG = "AmountTextWatcher"
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        editText.setInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        if (s.toString() != current && s.length < 9) {
            editText.removeTextChangedListener(this)


            val cleanString: String = s.replace("""[$,.]""".toRegex(), "")


//            val parsed = cleanString.trim().toDouble()

            // Safely parse the value
            val parsed = try {
                cleanString.trim().toDouble() / 100
            } catch (e: NumberFormatException) {
                0.0
            }

            // Clamp the value if maxValue is not null
            val clampedValue = if (maxValue != null && parsed > maxValue) maxValue else parsed

            var formatted = if (isManual) {
                NumberFormat.getCurrencyInstance(Locale.US).format(clampedValue)
            } else {
                NumberFormat.getCurrencyInstance(Locale.US).format(clampedValue)
            }

            if (isFromGiftCard) {
                formatted = formatted.replace("$", "")
                current = formatted
                editText.setText(formatted.replace("""[,]""".toRegex(), ""))
            } else {
                current = formatted
                editText.setText(formatted.replace("""[,]""".toRegex(), ""))
            }


            // To prevent setting cursor at the end of the string even if user manually changes the cursor position
            if ((start > 0 && start < editText.text.toString().length - 1)) {
                editText.setSelection(start + 1)
            } else {
//                editText.setSelection(formatted.replace("""[,]""".toRegex(), "").length)
                try {
                    editText.setSelection(formatted.replace("""[,]""".toRegex(), "").length)
                } catch (e: Exception) {
                    Log.e("Err", "Err")
                }
            }

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