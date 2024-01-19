package com.pays.pos.utils

import android.content.Context
import android.view.Gravity
import android.widget.Toast
import android.view.LayoutInflater
import android.widget.TextView


/**
 * Created by Waheed on 04,November,2019
 */

fun Context.showNormalToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}