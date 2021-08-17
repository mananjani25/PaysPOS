package com.android.pos.utils

import java.text.SimpleDateFormat
import java.util.*

object TimeFormatUtils {

    fun showCurrentTime(): String = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())

    fun getCurrentDate(): String {
        //2021-08-17
        val c: Date = Calendar.getInstance().time
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formattedDate: String = df.format(c)
        println("formattedDate => $formattedDate")
        return formattedDate
    }
}