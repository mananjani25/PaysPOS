package com.pays.pos.utils.extensions

import android.content.Context
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

fun Context.timeCalculateForStartEndTime(
    hour: Int,
    minute: Int,
    isStart: String,
    myCalendar: Calendar,
    myCalendar1: Calendar
): String {
    var timestring = ""
    var hoursfinal: Int = 0
    if ((hour == 12 && minute > 0) || (hour > 12 && minute > 0) || (hour > 12 && minute == 0)) {
        hoursfinal = if (hour == 12) {
            hour
        } else {
            hour - 12
        }
        timestring = if (hoursfinal < 10) {
            if (minute < 10) {
                "0$hoursfinal:0$minute PM"
            } else {
                "0$hoursfinal:$minute PM"
            }
        } else {
            if (minute < 10) {
                "$hoursfinal:0$minute PM"
            } else {
                "$hoursfinal:$minute PM"
            }
        }
    } else {
        if (hour == 0) {
            timestring = if (minute < 10) {
                "${hour.plus(12)}:0$minute AM"
            } else {
                "${hour.plus(12)}:$minute AM"
            }
        } else {
            timestring = if (hour < 10) {
                if (minute < 10) {
                    "0$hour:0$minute AM"
                } else {
                    "0$hour:$minute AM"
                }
            } else {
                if (minute < 10) {
                    "$hour:0$minute AM"
                } else {
                    "$hour:$minute AM"
                }
            }
        }

    }

    val myFormat = "MM/dd/yyyy" //In which you need put here
    val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
   val startDatestring = if (isStart == "isstart") {
        sdf.format(myCalendar.time)
    } else {
        sdf.format(myCalendar1.time)
    }
    return "$startDatestring $timestring"
}

fun Context.differenceTrue(date1: String, date2: String?): Long {
    var dateType1: Date
    var dateType2: Date
    var daydifference = "0".toLong()
//        11/30/2021 09:40 AM
    try {
        var dates = SimpleDateFormat("MM/dd/yyyy")
        dateType1 = dates.parse(date1.substringBefore(" "))
        dateType2 = dates.parse(date2?.substringBefore(" "))
        var differencedate = abs(dateType1.time - dateType2.time)
        daydifference = differencedate / (24 * 60 * 60 * 1000)

        return daydifference
    } catch (e: Exception) {
    }
    return daydifference
}