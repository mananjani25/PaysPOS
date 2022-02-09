package com.android.pos.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.text.TextUtils
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import com.android.pos.MainApplication
import com.android.pos.R
import com.android.pos.data.model.requestModel.CreateCategoryRequestModel
import com.android.pos.data.model.requestModel.CreateItemRequestModel
import com.android.pos.data.remote.Constants
import com.android.pos.di.PrefProvider
import com.android.pos.utils.extensions.toMultiPartRequestBody
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
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

        @SuppressLint("SetTextI18n")
        fun setPriceTextView(appCompatTextView: TextView, price: Double) {
            appCompatTextView.text = MainApplication.getInstance()!!.getText(R.string.symbole)
                .toString() + String.format(
                "%.2f", price
            )

        }


        @SuppressLint("SetTextI18n")
        fun setRefundPriceTextView(appCompatTextView: AppCompatTextView, price: Double) {

            appCompatTextView.text = "Total Refundable Amount is " + MainApplication.getInstance()!!
                .getText(R.string.symbole)
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

        fun clearString(s: String): Double {

            val cleanString: String = s.replace("""[$]""".toRegex(), "")

            return cleanString.trim().toDouble()
        }

        fun getTextTextView(edtFirstName: AppCompatTextView): String {

            return edtFirstName.text.toString().trim()
        }

        fun getUSFormatNumber(inputNumber: String): String {


            val pnu: PhoneNumberUtil = PhoneNumberUtil.getInstance()
            var outPutNumber = inputNumber
            try {
                val pn: Phonenumber.PhoneNumber = pnu.parse(inputNumber, "US")
                outPutNumber =
                    pnu.format(pn, PhoneNumberUtil.PhoneNumberFormat.NATIONAL)

            } catch (e: NumberFormatException) {
                return inputNumber
            }
            return outPutNumber
        }

        fun generateItemRequest(data: CreateItemRequestModel): HashMap<String, RequestBody> {
            val createItemRequestMap = HashMap<String, RequestBody>()
            createItemRequestMap["active"] = data.active.toString().toMultiPartRequestBody()
            createItemRequestMap["category_id"] =
                data.categoryId.toString().toMultiPartRequestBody()
            createItemRequestMap["cost"] = data.cost.toString().toMultiPartRequestBody()
            createItemRequestMap["desc"] = data.desc.toString().toMultiPartRequestBody()
            createItemRequestMap["id"] = data.id.toString().toMultiPartRequestBody()
            createItemRequestMap["kitchen_name"] =
                data.locationId.toString().toMultiPartRequestBody()
            createItemRequestMap["location_id"] =
                data.locationId.toString().toMultiPartRequestBody()
            createItemRequestMap["name"] = data.name.toString().toMultiPartRequestBody()
            createItemRequestMap["price"] = data.price.toString().toMultiPartRequestBody()
            createItemRequestMap["priceType"] = data.priceType.toString().toMultiPartRequestBody()
            createItemRequestMap["product_code"] =
                data.productCode.toString().toMultiPartRequestBody()
            createItemRequestMap["quantity"] = data.quantity.toString().toMultiPartRequestBody()
            createItemRequestMap["sku"] = data.sku.toString().toMultiPartRequestBody()

            return createItemRequestMap
        }

        fun generateCategoryRequest(data: CreateCategoryRequestModel): HashMap<String, RequestBody> {
            val createCategoryRequestMap = HashMap<String, RequestBody>()
            createCategoryRequestMap["id"] = data.id.toString().toMultiPartRequestBody()
            createCategoryRequestMap["name"] = data.name.toString().toMultiPartRequestBody()
            createCategoryRequestMap["active"] = data.active.toString().toMultiPartRequestBody()
            createCategoryRequestMap["location_id"] =
                data.location_id.toString().toMultiPartRequestBody()
            return createCategoryRequestMap
        }

        fun makeMultiPartBody(
            fileUrl: String?,
            contentType: String,
            fileKeyName: String
        ): MultipartBody.Part? {
            var filePart: MultipartBody.Part? = null
            if (fileUrl?.isNotEmpty() == true) {
                val file = File(fileUrl)
                val fileBody = ProgressRequestBody(File(fileUrl), contentType, null)
                filePart = MultipartBody.Part.createFormData(fileKeyName, file.name, fileBody)
            }
            return filePart
        }

        private fun changeDateFormat(
            inputFormat: String,
            inputDate: String,
            outputFormat: String
        ): String {

            try {
                val inputDateFormat = SimpleDateFormat(inputFormat, Locale.getDefault())
                inputDateFormat.timeZone = TimeZone.getTimeZone("GMT")
                val date: Date? = inputDateFormat.parse(inputDate)
                date ?: return inputDate
                val outputDateFormat = SimpleDateFormat(outputFormat, Locale.getDefault())
                return outputDateFormat.format(date)
            } catch (e: Exception) {
                e.printStackTrace()
                return inputDate
            }
        }

        private fun changeDateFormatWithoutTimeZone(
            inputFormat: String,
            inputDate: String,
            outputFormat: String
        ): String {

            try {
                val inputDateFormat = SimpleDateFormat(inputFormat, Locale.getDefault())
                val date: Date? = inputDateFormat.parse(inputDate)
                date ?: return inputDate
                val outputDateFormat = SimpleDateFormat(outputFormat, Locale.getDefault())
                return outputDateFormat.format(date)
            } catch (e: Exception) {
                e.printStackTrace()
                return inputDate
            }
        }

        fun getFormattedDateTime(
            inputFormat: String,
            date: String,
            outputFormat: String,
            timeZoneApplied: Boolean
        ): String {
            return if (!TextUtils.isEmpty(date)) {
                if (timeZoneApplied) {
                    changeDateFormat(
                        inputFormat,
                        date,
                        outputFormat
                    )
                } else {
                    changeDateFormatWithoutTimeZone(
                        inputFormat,
                        date,
                        outputFormat
                    )
                }
            } else {
                ""
            }
        }

        var mLastClickTime = 0L

        fun isDoubleClick(): Boolean {
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                return true
            }
            mLastClickTime = SystemClock.elapsedRealtime()
            return false
        }

        fun isEnableCashDiscount(context: Context): Boolean {
            val prefProvider: PrefProvider = PrefProvider(context)
            return prefProvider.getValueboolean(Constants.CASHDIS_SURCHARGEENABLE, false)
        }

        fun calculateCashDiscount(
            finalAmount: Double,
            prefProvider: PrefProvider,
            context: Context
        ): Double {
            var amountType = prefProvider.getValue(Constants.AMOUNT_TYPE, "")
            var rateorAmount = prefProvider.getValue(Constants.RATE_OR_AMOUNT, "0")
            if (amountType == "Dollar") {
                if (finalAmount.toDouble() > 0) {
                    if (finalAmount <= rateorAmount.toDouble()) {
                        return finalAmount
                    } else {
                        if (rateorAmount.toDouble() < 0.toDouble()) {
                            return 0.0
                        } else {
                            return rateorAmount.toDouble()
                        }
                    }
                } else {
                    return 0.00
                }
            } else if (amountType == "Percentage") {
                if (finalAmount.toDouble() > 0) {
                    if (rateorAmount.toDouble() >= 100) {
                        return finalAmount
                    } else {
                        return (finalAmount * rateorAmount.toDouble() / 100)
                    }
                }
            }
            return 0.00
        }

        fun errorLog(tag: String, message: String) {
            Log.e(tag, message)
        }
    }


}

/*fun addItemsForDineIn(list: ArrayList<DineInModel>): ArrayList<DineInModel> {
    for (i in 0 until list.size) {
        if (list.get(i).items.isNotEmpty()) {
            list[i].items.forEach {

            }
        }

    }


}*/
