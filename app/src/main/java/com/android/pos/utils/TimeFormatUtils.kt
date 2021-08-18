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

    fun convertCurrentTime(mSelectedDate: String): String {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("hh:mm a")
            val date = inputFormat.parse(mSelectedDate)
            val formattedDate = outputFormat.format(date)
            //  val formattedDateFinalDate = outputFormat.parse(formattedDate)
            return formattedDate
        } catch (e: Exception) {
//Thu Jul 16 05:23:26 EDT 2020
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            val outputFormat = SimpleDateFormat("hh:mm a")
            val date = inputFormat.parse(mSelectedDate)
            val formattedDate = outputFormat.format(date)
            //  val formattedDateFinalDate = outputFormat.parse(formattedDate)
            return formattedDate

        }


    }

    fun convertCurrentDate(mSelectedDate: String): String {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM-dd-yyyy")
            val date = inputFormat.parse(mSelectedDate)
            val formattedDate = outputFormat.format(date)
            //  val formattedDateFinalDate = outputFormat.parse(formattedDate)
            return formattedDate
        } catch (e: Exception) {
//Thu Jul 16 05:23:26 EDT 2020
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            val outputFormat = SimpleDateFormat("MMM-dd-yyyy")
            val date = inputFormat.parse(mSelectedDate)
            val formattedDate = outputFormat.format(date)
            //  val formattedDateFinalDate = outputFormat.parse(formattedDate)
            return formattedDate

        }


    }
}