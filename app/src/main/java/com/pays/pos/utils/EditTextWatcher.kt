package com.pays.pos.utils

import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.widget.AppCompatEditText
import com.pays.pos.data.entities.Modifier

class EditTextWatcher(private val editText: AppCompatEditText, private val data: Modifier) :
    TextWatcher {
    override fun afterTextChanged(s: Editable) {
        editText.removeTextChangedListener(this)
        data.name = s.toString()
        editText.addTextChangedListener(this)
    }

    override fun beforeTextChanged(
        arg0: CharSequence, arg1: Int, arg2: Int,
        arg3: Int
    ) {
    }

    override fun onTextChanged(s: CharSequence, arg1: Int, arg2: Int, arg3: Int) {


    }
}