package com.pays.pos.utils

import android.content.Context
import android.util.Log
import com.pays.pos.data.remote.Constants.SYSTEM_TIMEZONE
import com.pays.pos.di.PrefProvider
import java.text.SimpleDateFormat
import java.util.*

object TimeFormatUtils {

    fun showCurrentTime(): String = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
    lateinit var prefProvider: PrefProvider
    fun getCurrentDate(): String {
        //2021-08-17
        val c: Date = Calendar.getInstance().time
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formattedDate: String = df.format(c)
        println("formattedDate => $formattedDate")
        return formattedDate
    }


    fun convertCurrentTime(mSelectedDate: String, context: Context?): String {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val outputFormat = SimpleDateFormat("hh:mm a")
            prefProvider = PrefProvider(context = context!!)
            outputFormat.timeZone = TimeZone.getTimeZone(prefProvider.getValue(SYSTEM_TIMEZONE, ""))
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

    fun convertCurrentDate(mSelectedDate: String, context: Context?): String {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val outputFormat = SimpleDateFormat("MMM-dd-yyyy")
            prefProvider = PrefProvider(context = context!!)
            outputFormat.timeZone = TimeZone.getTimeZone(prefProvider.getValue(SYSTEM_TIMEZONE, ""))
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

    fun convertServerTimeFromPref(mSelectedDate: String?, context: Context?): String {
        LogUtil.logE(TAG, "mSelectedDatemSelectedDate  ${mSelectedDate}")
        try {
            val inputFormat = SimpleDateFormat("MM/dd/yyyy HH:mm a")
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val outputFormat = SimpleDateFormat("MM/dd/yyyy HH:mm a")
            prefProvider = PrefProvider(context = context!!)
            outputFormat.timeZone = TimeZone.getTimeZone(prefProvider.getValue(SYSTEM_TIMEZONE, ""))
            val date = inputFormat.parse(mSelectedDate)
            val formattedDate = outputFormat.format(date)
            LogUtil.logE(TAG, "formattedDateformattedDate  ${formattedDate}")
            //  val formattedDateFinalDate = outputFormat.parse(formattedDate)
            return formattedDate
        } catch (e: Exception) {
//Thu Jul 16 05:23:26 EDT 2020
            val inputFormat = SimpleDateFormat("mm/dd/yyyy hh:mm a", Locale.US)
            val outputFormat = SimpleDateFormat("mm/dd/yyyy hh:mm a")
            val date = inputFormat.parse(mSelectedDate)
            val formattedDate = outputFormat.format(date)
            //  val formattedDateFinalDate = outputFormat.parse(formattedDate)
            return formattedDate

        }


    }

    fun convertDateFormatForOpenOrder(selectedDate: String, context: Context?): String {
        try {
            var inputFormat = SimpleDateFormat("yyyy-MM-dd")
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            var outFormat = SimpleDateFormat("MMM-dd-yyyy")
            prefProvider = PrefProvider(context = context!!)
            inputFormat.timeZone = TimeZone.getTimeZone(prefProvider.getValue(SYSTEM_TIMEZONE, ""))
            var date = inputFormat.parse(selectedDate)
            var formatedDate = outFormat.format(date)
            return formatedDate
        } catch (e: Exception) {
            var inputFormat = SimpleDateFormat("yyyy-MM-dd")
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            prefProvider = PrefProvider(context = context!!)
            var outFormat = SimpleDateFormat("MMM-dd-yyyy")
            inputFormat.timeZone = TimeZone.getTimeZone(prefProvider.getValue(SYSTEM_TIMEZONE, ""))
            var date = inputFormat.parse(selectedDate)
            var formatedDate = outFormat.format(date)
            return formatedDate
        }
    }

}