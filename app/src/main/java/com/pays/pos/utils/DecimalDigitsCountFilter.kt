package com.pays.pos.utils

import android.text.InputFilter
import android.text.Spanned

class DecimalDigitsCountFilter(private val maxDigitsAfterDecimalPoint: Int? = 2) : InputFilter {
    override fun filter(
        source: CharSequence, start: Int, end: Int,
        dest: Spanned, dstart: Int, dend: Int
    ): CharSequence? {
        val builder: StringBuilder = StringBuilder(dest)
        builder.replace(
            dstart, dend, source
                .subSequence(start, end).toString()
        )
        return if (!builder.toString().matches(
                Regex("(([1-9]{1})([0-9]{0,})?)?(\\.[0-9]{0," + maxDigitsAfterDecimalPoint + "})?")

            )
        ) {
            if (source.isEmpty()) dest.subSequence(dstart, dend) else ""
        } else null
    }
}