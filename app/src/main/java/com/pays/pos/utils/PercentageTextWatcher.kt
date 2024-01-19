package com.pays.pos.utils

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.appcompat.widget.AppCompatEditText
import com.pays.pos.data.entities.TbItem
import java.text.NumberFormat
import java.util.*

class PercentageTextWatcher(private val editText: AppCompatEditText) : TextWatcher {
    var current = ""
    val TAG = "AmountTextWatcher"
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun afterTextChanged(s: Editable) {


    }
}