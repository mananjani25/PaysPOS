package com.pays.pos.utils

import android.util.Log

fun String.subTotalToDouble():Double{
    try {
        return this.substring(1, this.length).toDouble()
    }catch (exception:Exception){
        Log.e("Total Price Convert crash",exception.printStackTrace().toString())
        return 0.0
    }

}