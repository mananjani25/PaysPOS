package com.pays.pos.utils

import android.util.Log


/**
 * Created by Waheed on 04,November,2019
 */

object LogUtil {

    fun logE(key: String, message: String) {
        Log.e(key, message)
        println("$key :: $message")
    }
    fun logEN(key: String, message: String) {
        Log.e(key, message)
    }
}

object LongLog {

    fun log(TAG: String?, message: String) {
        val maxLogSize = 2000
        for (i in 0..message.length / maxLogSize) {
            val start = i * maxLogSize
            var end = (i + 1) * maxLogSize
            end = if (end > message.length) message.length else end
            Log.d(TAG, message.substring(start, end))
        }
    }

}