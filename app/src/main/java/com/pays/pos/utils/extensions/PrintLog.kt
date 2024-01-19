package com.pays.pos.utils.extensions

import android.content.Context
import android.util.Log

fun Context.printLog(tag:String = "POSLog",msg:String){
    Log.d(tag,msg)
}