package com.pays.pos.utils

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.CheckBox
import android.widget.EditText

import com.pays.pos.utils.callback.AddressTextChangeListner

class CustomerAddressTextWatcher(
    var edittext: EditText,
    var changeField: Boolean,
    var listner: AddressTextChangeListner,
    var isFromSelect: Boolean,
    var maxLength : Int? = null
) : TextWatcher {
    var pervText: String = ""
    private val TAG = this.javaClass.name
    private var counter: Int = 0
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        pervText = edittext.text.toString()
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {


    }

    override fun afterTextChanged(s: Editable?) {
        Log.e(TAG, "AfterTextChanged")

        LogUtil.logE(
            TAG,
            "oldText ${pervText}  newText ${edittext.text.toString()}  isFromSelect ${isFromSelect}  changeField ${changeField}"
        )


        if (isFromSelect==false && pervText != edittext.text.toString()){
            listner.oncheckBox(false)
        }


        if (isFromSelect) {
            Log.e(TAG, "Condition True 3")
            listner.oncheckBox(true)
            isFromSelect = false
        } else {
            if ((pervText.isNotEmpty() && !pervText.equals(edittext.text.toString())) && counter == 0) {
                Log.e(TAG, "c True 1")
                listner.onTextChanges()
                counter = 1
                isFromSelect = false
            } else if (pervText.trim().isEmpty() && edittext.text.trim().toString()
                    .isNotEmpty()
            ) {
                Log.e(TAG, "Condition True 2")
                listner.oncheckBox(false)
                isFromSelect = false

            }
        }
//        Check if the ZIP code length exceeds 10 digits
        maxLength?.let{
            if ((s?.length ?: 0) > it) {
                edittext.error = "You can't enter more than $maxLength characters"
            } else {
                edittext.error = null // Clear error if valid
            }
        }


    }

}