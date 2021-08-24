package com.android.pos.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import com.android.pos.MainApplication
import com.android.pos.R
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


class MethodUtils {
    companion object {

        @SuppressLint("SetTextI18n")
        fun setPriceEditText(appCompatEditText: AppCompatEditText, price: Double) {

            appCompatEditText.setText(
                MainApplication.getInstance()!!.getText(R.string.symbole)
                    .toString() + String.format(
                    "%.2f", price
                )
            )

        }

        @SuppressLint("SetTextI18n")
        fun setPriceTextView(appCompatTextView: AppCompatTextView, price: Double) {
            appCompatTextView.text = MainApplication.getInstance()!!.getText(R.string.symbole)
                .toString() + String.format(
                "%.2f", price
            )

        }

        fun roundOffAmount(price: Double): String {
            return MainApplication.getInstance()!!.getText(R.string.symbole)
                .toString() + String.format("%.2f", price)
        }

        fun roundOffAmountDouble(price: Double): Double {
            return String.format("%.2f", price).toDouble()
        }

        fun roundOffAmountString(price: Double): String {
            return String.format("%.2f", price)
        }

        fun hideKeyboard(activity: Activity) {
            try {
                val inputManager =
                    activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                activity.currentFocus?.let {
                    inputManager.hideSoftInputFromWindow(
                        activity.currentFocus!!.windowToken,
                        InputMethodManager.HIDE_NOT_ALWAYS
                    )
                }
            } catch (e: Exception) {
            }
        }

        fun getTime(hour: Int, minute: Int): String {
            val time = "$hour:$minute"
            val fmt = SimpleDateFormat("HH:mm", Locale.US)
            var date: Date? = null
            try {
                date = fmt.parse(time)
            } catch (e: ParseException) {
                e.printStackTrace()
            }
            val fmtOut = SimpleDateFormat("hh:mm aa", Locale.US)
            return fmtOut.format(date)
        }

        fun getText(edtFirstName: AppCompatEditText): String {

            return edtFirstName.text.toString().trim()
        }
    }
}