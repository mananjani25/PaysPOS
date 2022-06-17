package com.android.pos.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.FragmentActivity
import com.android.pos.MainApplication
import com.android.pos.R
import com.android.pos.data.model.requestModel.CreateCategoryRequestModel
import com.android.pos.data.model.requestModel.CreateItemRequestModel
import com.android.pos.data.model.responseModel.GetTipReponse
import com.android.pos.data.remote.Constants
import com.android.pos.di.PrefProvider
import com.android.pos.utils.extensions.toMultiPartRequestBody
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import com.sunmi.externalprinterlibrary.api.SunmiPrinterApi
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.ceil
import kotlin.math.floor


class PrintSunmiUtils {
    companion object {
        fun printLogo(newBitmap: Bitmap?) {

            SunmiPrinterApi.getInstance().setAlignMode(1)
            if (newBitmap != null) {
                SunmiPrinterApi.getInstance().printBitmap(newBitmap, 0)
            }
            SunmiPrinterApi.getInstance().lineWrap(1)
        }

        fun printBusinessDetails(value: String, value1: String, value2: String) {

            SunmiPrinterApi.getInstance().setAlignMode(1)
            SunmiPrinterApi.getInstance().enableBold(true)
            SunmiPrinterApi.getInstance().setFontZoom(2, 2)
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)

            SunmiPrinterApi.getInstance().setAlignMode(1)
            SunmiPrinterApi.getInstance().enableBold(false)
            SunmiPrinterApi.getInstance().setFontZoom(1, 1)
            SunmiPrinterApi.getInstance().printText(value1)
            SunmiPrinterApi.getInstance().lineWrap(1)

            SunmiPrinterApi.getInstance().setAlignMode(1)
            SunmiPrinterApi.getInstance().enableBold(false)
            SunmiPrinterApi.getInstance().setFontZoom(1, 1)
            SunmiPrinterApi.getInstance().printText(value2)
            SunmiPrinterApi.getInstance().lineWrap(1)


        }

        fun venueWebsite(value: String) {
            SunmiPrinterApi.getInstance().setAlignMode(1)
            SunmiPrinterApi.getInstance().enableBold(false)
            SunmiPrinterApi.getInstance().setFontZoom(1, 1)
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)
        }

        fun paymentType(value: String) {
            SunmiPrinterApi.getInstance().setAlignMode(1)
            SunmiPrinterApi.getInstance().enableBold(false)
            SunmiPrinterApi.getInstance().setFontZoom(2, 2)
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)
        }

        fun orderId(value: String) {
            SunmiPrinterApi.getInstance().setAlignMode(0)
            SunmiPrinterApi.getInstance().enableBold(false)
            SunmiPrinterApi.getInstance().setFontZoom(1, 1)
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)
        }

        fun orderTime(value: String) {
            SunmiPrinterApi.getInstance().setAlignMode(0)
            SunmiPrinterApi.getInstance().enableBold(false)
            SunmiPrinterApi.getInstance().setFontZoom(1, 1)
            SunmiPrinterApi.getInstance().printText(
                value
            )
            SunmiPrinterApi.getInstance().lineWrap(1)

        }

        fun employee(value: String) {
            SunmiPrinterApi.getInstance().setAlignMode(0)
            SunmiPrinterApi.getInstance().enableBold(false)
            SunmiPrinterApi.getInstance().setFontZoom(1, 1)
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)
        }

        fun receiptID(value: String) {
            SunmiPrinterApi.getInstance().setAlignMode(0)
            SunmiPrinterApi.getInstance().enableBold(false)
            SunmiPrinterApi.getInstance().setFontZoom(1, 1)
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)
        }

        fun printOrderType(value: String) {

            SunmiPrinterApi.getInstance().setAlignMode(1)
            SunmiPrinterApi.getInstance().enableBold(true)
            SunmiPrinterApi.getInstance().setFontZoom(2, 2)
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(3)

        }

        fun addHorizontal() {
            val st = addHorizontalKitchenLine()
            SunmiPrinterApi.getInstance().enableUnderline(true)
            SunmiPrinterApi.getInstance().enableBold(false)
            SunmiPrinterApi.getInstance().setFontZoom(1, 1)
            SunmiPrinterApi.getInstance().printText(st)
            SunmiPrinterApi.getInstance().lineWrap(1)

        }

        fun totalDiscount(value: String) {
            SunmiPrinterApi.getInstance().enableBold(false)
            SunmiPrinterApi.getInstance().setFontZoom(1, 1)
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)

        }

        fun subTotal(value: String) {
            SunmiPrinterApi.getInstance().enableBold(false)
            SunmiPrinterApi.getInstance().setFontZoom(1, 1)
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)


        }

        fun tax(value: String) {
            SunmiPrinterApi.getInstance().enableBold(false)
            SunmiPrinterApi.getInstance().setFontZoom(1, 1)
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)

        }

        fun serviceCharge(value: String) {
            SunmiPrinterApi.getInstance().enableBold(false)
            SunmiPrinterApi.getInstance().setFontZoom(1, 1)
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)


        }

        fun tip(value: String) {
            SunmiPrinterApi.getInstance().enableBold(true)
            SunmiPrinterApi.getInstance().setFontZoom(1, 1)
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)

        }

        fun surCharge(value: String) {
            SunmiPrinterApi.getInstance().enableBold(true)
            SunmiPrinterApi.getInstance().setFontZoom(1, 1)
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)
        }

        fun cashDiscount(value: String) {
            SunmiPrinterApi.getInstance().enableBold(true)
            SunmiPrinterApi.getInstance().setFontZoom(1, 1)
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)
        }

        fun loyaltyAmount(value: String) {
            SunmiPrinterApi.getInstance().enableBold(true)
            SunmiPrinterApi.getInstance().setFontZoom(1, 1)
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)

        }

        fun loyaltyPoint(value: String) {
            SunmiPrinterApi.getInstance().enableBold(true)
            SunmiPrinterApi.getInstance().setFontZoom(1, 1)
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)
        }

        fun totalPrice(value: String) {

            SunmiPrinterApi.getInstance().enableBold(true)
            SunmiPrinterApi.getInstance().setFontZoom(1, 1)
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)

        }

        fun refundAmount(value: String) {

            SunmiPrinterApi.getInstance().enableBold(true)
            SunmiPrinterApi.getInstance().setFontZoom(1, 1)
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)

        }

        fun tips(value: String) {

            SunmiPrinterApi.getInstance().setAlignMode(0)
            SunmiPrinterApi.getInstance().enableBold(true)
            SunmiPrinterApi.getInstance().setFontZoom(1, 1)
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(2)
        }

        fun additionalTips() {

            SunmiPrinterApi.getInstance().setAlignMode(0)
            SunmiPrinterApi.getInstance().enableBold(true)
            SunmiPrinterApi.getInstance().setFontZoom(1, 1)
            SunmiPrinterApi.getInstance().printText("Additional Tips")
            SunmiPrinterApi.getInstance().lineWrap(1)
            addHorizontal()

        }

        fun addTipList(tipsList: List<GetTipReponse.Data>, totalAmount: Double) {

            addTipsList(
                tipsList,
                totalAmount
            )
        }

        fun transactionId(value: String) {

            SunmiPrinterApi.getInstance().setAlignMode(0)
            SunmiPrinterApi.getInstance().enableBold(true)
            SunmiPrinterApi.getInstance().setFontZoom(1, 1)
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)
        }

        fun transactionType(value: String) {

            SunmiPrinterApi.getInstance().enableBold(true)
            SunmiPrinterApi.getInstance().setFontZoom(1, 1)
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)

        }

        fun cardDetails(cardName: String, cardType: String, cardNumber: String) {

            for (i in 1..3) {

                SunmiPrinterApi.getInstance().setAlignMode(2)
                SunmiPrinterApi.getInstance().enableBold(true)
                SunmiPrinterApi.getInstance().setFontZoom(1, 1)
                when (i) {
                    1 -> {
                        SunmiPrinterApi.getInstance().printText(cardName)
                    }
                    2 -> {
                        SunmiPrinterApi.getInstance().printText(cardType)
                    }
                    3 -> {
                        SunmiPrinterApi.getInstance().printText(cardNumber)
                    }
                }

                SunmiPrinterApi.getInstance().lineWrap(1)
            }
        }

        fun customerDetails() {

            SunmiPrinterApi.getInstance().enableBold(true)
            SunmiPrinterApi.getInstance().setFontZoom(1, 1)
            SunmiPrinterApi.getInstance().printText("Customer Details")
            SunmiPrinterApi.getInstance().lineWrap(1)

            addHorizontal()

        }

        fun customerName(value: String) {

            SunmiPrinterApi.getInstance().setAlignMode(0)
            SunmiPrinterApi.getInstance().enableBold(false)
            SunmiPrinterApi.getInstance().setFontZoom(1, 1)
            SunmiPrinterApi.getInstance()
                .printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)
        }

        fun customerPhone(value: String) {
            SunmiPrinterApi.getInstance().setAlignMode(0)
            SunmiPrinterApi.getInstance().enableBold(false)
            SunmiPrinterApi.getInstance().setFontZoom(1, 1)
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)
        }

        fun customerAddress(value: String) {
            SunmiPrinterApi.getInstance().setAlignMode(0)
            SunmiPrinterApi.getInstance().enableBold(false)
            SunmiPrinterApi.getInstance().setFontZoom(1, 1)
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)
        }

        fun orderNote(value: String) {

            SunmiPrinterApi.getInstance().setAlignMode(1)
            SunmiPrinterApi.getInstance().enableBold(true)
            SunmiPrinterApi.getInstance().setFontZoom(1, 1)
            SunmiPrinterApi.getInstance().printText("Order Note")
            SunmiPrinterApi.getInstance().lineWrap(1)

            SunmiPrinterApi.getInstance().setAlignMode(1)
            SunmiPrinterApi.getInstance().enableBold(false)
            SunmiPrinterApi.getInstance().setFontZoom(1, 1)
            SunmiPrinterApi.getInstance().printText(value)
            SunmiPrinterApi.getInstance().lineWrap(1)
        }

        fun qrCode(value: String) {
            SunmiPrinterApi.getInstance().setAlignMode(1)
            value.let {
                SunmiPrinterApi.getInstance().printQrCode(
                    it, 6, 0
                )
            }
        }

    }


}

