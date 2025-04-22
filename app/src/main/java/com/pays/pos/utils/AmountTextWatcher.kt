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
    private val maxValue: Double? = null,
    var isFromGiftCard: Boolean = false
) : TextWatcher {

    private var current = ""
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
    private val TAG = "AmountTextWatcher"

    companion object {
        private val NON_DIGIT_REGEX = """[^\d]""".toRegex()
        private val COMMA_REGEX = ",".toRegex()
    }

    init {
        editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        if (s.toString() == current || s.length >= 9) return

        editText.removeTextChangedListener(this)

        val cleanString = s.replace(NON_DIGIT_REGEX, "")

        val parsed = cleanString.toDoubleOrNull()?.div(100) ?: 0.0
        val clampedValue = maxValue?.let { minOf(it, parsed) } ?: parsed

        var formatted = currencyFormatter.format(clampedValue)
        if (isFromGiftCard) {
            formatted = formatted.replace("$", "")
        }

        current = formatted
        val cleanFormatted = formatted.replace(COMMA_REGEX, "")
        editText.setText(cleanFormatted)

        try {
            val cursorPosition = cleanFormatted.length.coerceAtMost(editText.text?.length ?: 0)
            editText.setSelection(cursorPosition)
        } catch (e: Exception) {
            Log.e(TAG, "Cursor position error: ${e.message}")
        }

        editText.addTextChangedListener(this)
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        // No need to repeatedly set inputType unless dynamic switching is needed
    }

    override fun afterTextChanged(s: Editable) {
        // Likewise, no repeated setting needed
    }
}
