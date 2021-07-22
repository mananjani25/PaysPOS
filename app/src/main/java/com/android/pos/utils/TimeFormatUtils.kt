package com.android.pos.utils

import java.text.SimpleDateFormat
import java.util.*

object TimeFormatUtils {
    fun showCurrentTime() = SimpleDateFormat("HH:mm a", Locale.getDefault()).format(Date())

}