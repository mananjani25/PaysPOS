package com.android.pos.utils

import android.text.Editable
import android.text.TextWatcher
import android.widget.CheckBox
import android.widget.EditText

import com.android.pos.utils.callback.AddressTextChangeListner

class CustomerAddressTextWatcher(
    var checkBox: CheckBox,
    var edittext: EditText,
   var changeField: Boolean,
    var listner: AddressTextChangeListner,
    var isFromSelect:Boolean = false
) : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        if (edittext.text.trim().toString().isNotEmpty()) {
            listner.onTextChanges(true)
        }
            }

    override fun afterTextChanged(s: Editable?) {
        if (changeField && !isFromSelect){
            listner.onTextChanges(true)
        }
        checkBox.isChecked = changeField
    }
}