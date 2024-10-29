package com.pays.pos.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.FragmentActivity
import androidx.work.WorkManager
import com.google.gson.Gson
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import com.pays.pos.MainApplication
import com.pays.pos.R
import com.pays.pos.data.entities.ModifierSet
import com.pays.pos.data.model.requestModel.CreateCategoryRequestModel
import com.pays.pos.data.model.requestModel.CreateItemRequestModel
import com.pays.pos.data.remote.Constants
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.extensions.toMultiPartRequestBody
import com.pays.pos.utils.workmanager.UploadWorker2
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.text.DecimalFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.*


class MethodUtils {
    companion object {

        private var fourthValue: Double = 0.0
        private var thirdValue: Double = 0.0
        private var secondValue: Int = 0

        fun generalizeAmount(amount:String): String {
            if (!amount.contains('.')){
                return amount+".00"
            }
            if ((amount.length - 1) - amount.indexOf('.') < 2) {
                return amount.toString() + "0"
            } else {
                return amount.toString()
            }
        }

        fun ellipsize(text: String, maxLength: Int = 4): String {
            return if (text.length > maxLength) {
                text.substring(0, maxLength) + "..."
            } else {
                text
            }
        }


        fun getCardType(xml: String): String {
            if (xml != null && !xml.isNullOrEmpty()) {

                var applabStartIndex = xml.indexOf("<APPLAB>")
                var applabEndIndex = xml.indexOf("</APPLAB>")
                return xml.substring(
                    applabStartIndex + "<APPLAB>".length,
                    applabEndIndex
                )
            }
            return ""
        }

        @SuppressLint("HardwareIds")
        fun getDeviceId(requireActivity: FragmentActivity): String {
            return Settings.Secure.getString(
                requireActivity.contentResolver,
                Settings.Secure.ANDROID_ID
            )
        }

        fun hideSoftKeyboard(activity: Activity) {
            if (activity.getCurrentFocus() == null) {
                return
            }
            val inputMethodManager =
                activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(activity.currentFocus!!.windowToken, 0)
        }

        fun convertSortListForModifierSet(
            seqIds: List<Int>,
            modList: ArrayList<ModifierSet>
        ): ArrayList<ModifierSet> {
            var newListMod: ArrayList<ModifierSet> = arrayListOf()
            Log.e(TAG, "getseqIds:  ${Gson().toJson(seqIds)}")

            for (i in 0 until seqIds.size) {
                modList.find { it.id == seqIds.get(i) }?.let { newListMod.add(it) }
            }

            Log.e(TAG, "newListMod:  ${newListMod.size}")


            return newListMod
        }


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
        fun setPriceTextViewDown(appCompatTextView: TextView, price: Double) {
            appCompatTextView.text = MainApplication.getInstance()!!.getText(R.string.symbole)
                .toString() + getTwoDecimal(price).toPrecision(2)

        }

        @SuppressLint("SetTextI18n")
        fun setPriceTextViewUP(appCompatTextView: TextView, price: Double) {
            appCompatTextView.text = MainApplication.getInstance()!!.getText(R.string.symbole)
                .toString() + roundOffAmountUp(price)

        }


        @SuppressLint("SetTextI18n")
        fun setRefundPriceTextView(appCompatTextView: AppCompatTextView, price: Double) {

            appCompatTextView.text = "Total Refundable Amount is " + MainApplication.getInstance()!!
                .getText(R.string.symbole)
                .toString() + String.format(
                "%.2f", price
            )

        }

        /*   fun roundOffAmount(price: Double): String {
               return MainApplication.getInstance()!!.getText(R.string.symbole)
                   .toString() + String.format("%.2f", price)
           }
   */
        fun roundOffAmount(price: Double): String {
            return MainApplication.getInstance()!!.getText(R.string.symbole)
                .toString() + getTwoDecimal(price).toPrecision(2)
        }

        fun roundOffTwoDec(price: Double): String {
            return MainApplication.getInstance()!!.getText(R.string.symbole)
                .toString() + getTwoDecimal(price)
        }

        fun roundOffAmountDown(price: Double): Double {
            var valueFormat = DecimalFormat("##.##")
            //  valueFormat.roundingMode = RoundingMode.UNNECESSARY
            return valueFormat.format(price).toDouble()


        }

        fun roundOffAmountUp(price: Double): Double {
            var valueFormat = DecimalFormat("##.##")
//            valueFormat.roundingMode = RoundingMode.CEILING
            return valueFormat.format(price).toDouble()

        }

        /*     fun roundOffAmountDouble(price: Double?): Double {
                 return String.format("%.2f", price).toDouble()
             }*/

        fun roundOffAmountDouble(price: Double?): Double {
            return if (price != null)
                getTwoDecimal(price)
            else
                String.format("%.2f", price).toDouble()
        }

        /* fun roundOffAmountString(price: Double): String {
             return String.format("%.2f", price)
         }*/

        fun roundOffAmountString(price: Double): String {
            return getTwoDecimal(price).toDouble().toPrecision(2)
        }

        fun roundOffAmountStringToDouble(price: String): String {

            if (price.isEmpty() || price == "0.0" || price == "0.00") {
                return "0.00"
            }

            return String.format("%.2f", price.toDouble())
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

        fun showKeyboard(activity: Activity) {

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

        /**
         * This is the new method created for formatting a
         * 10-digit number into US phone number format i.e (XXX) XXX-XXXX
         * By Dharmesh Basapati
         * */
        fun formatPhoneNumber(phoneNumber: String): String {
            if (phoneNumber.length != 10) {
                // Handle invalid input (must be 10 digits)
                return "Invalid phone number"
            }

            val areaCode = phoneNumber.substring(0, 3)
            val firstPart = phoneNumber.substring(3, 6)
            val secondPart = phoneNumber.substring(6)

            return "($areaCode) $firstPart-$secondPart"
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
            createItemRequestMap["price"] =
                data.price.toString().toMultiPartRequestBody()
//            createItemRequestMap["priceType"] = data.priceType.toString().toMultiPartRequestBody()
            createItemRequestMap["price_type"] = data.priceType.toString().toMultiPartRequestBody()
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
                val fileBody = ProgressRequestBody(
                    File(fileUrl),
                    contentType,
                    null
                )
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

        fun isDoubleClickCategory(): Boolean {
            if (SystemClock.elapsedRealtime() - mLastClickTime < 500) {
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
            LogUtil.logE(tag, message)
        }

        fun percentageCalculation(price: Double, rate: Double): Double {

            Log.e("TOTAL TIP Check","Price $price")

            return (price * rate) / 100
        }

        fun calculatePercentageFromAmount(amount: Double, total: Double): Double {
            return if (((amount / total) * 100).isNaN()) {
                0.0
            } else {
                (amount / total) * 100
            }
        }

        @SuppressLint("SetTextI18n")
        fun getCashPaymentOptionList(
            totalPrice: Double,
            tvCash1: AppCompatTextView,
            tvCash2: AppCompatTextView,
            tvCash3: AppCompatTextView
        ) {
            LogUtil.logE(TAG, "totalPrice  $totalPrice")
            secondValue = floor(totalPrice + 2).toInt()
            LogUtil.logE(TAG, "secondValue  $secondValue")
            val newVal = totalPrice + 2
            thirdValue = calculateCashOption(newVal)
            LogUtil.logE(TAG, "thirdValuethirdValue:   ${thirdValue}")
            if (secondValue.toDouble() == thirdValue) {
                if (secondValue > 1000) {
                    thirdValue += 100
                } else {
                    thirdValue += 50
                }

            }
            fourthValue = calculateCashOption(thirdValue)
            if (thirdValue == fourthValue) {
                fourthValue += 100
            } else {
                fourthValue += 50
            }

            setPriceTextView(tvCash1, secondValue.toDouble())
            setPriceTextView(tvCash2, thirdValue)
            setPriceTextView(tvCash3, fourthValue)


        }

        private fun calculateCashOption(value: Double): Double {
            when {
                value > 1000 -> {
                    return ceil(value / 100) * 100

                }
                value > 500 -> {
                    return ceil(value / 50) * 50
                }
                else -> {
                    val arrAmount = arrayOf(
                        5,
                        10,
                        20,
                        50,
                        100,
                        110,
                        120,
                        150,
                        200,
                        210,
                        220,
                        250,
                        300,
                        310,
                        320,
                        350,
                        400,
                        410,
                        420,
                        450,
                        500
                    )
                    val myValue = value.toInt()
                    LogUtil.logE(TAG, "myValue:  ${myValue}")
                    var searchIndex: Int = -1
                    val filterValue = arrAmount.filter {
                        it >= value
                    }.first()
                    searchIndex = arrAmount.indexOf(filterValue)
                    LogUtil.logE(TAG, "filterValue:  ${filterValue}")
                    LogUtil.logE(TAG, "searchIndex:  ${searchIndex}")


                    if (arrAmount.contains(myValue)) {
                        searchIndex += 1
                    }

                    return if (searchIndex >= arrAmount.size) {
                        550.0
                    } else {
                        val lastAmount = arrAmount[searchIndex]
                        lastAmount.toDouble()
                    }

                }
            }
        }


        fun randomOfflineId(locationId: String): String {

            val timestamp = System.currentTimeMillis().toString()
            val ss = locationId + timestamp.takeLast(4)
            val reqLent = 12 - ss.length
            val Alphabet = getSaltString(reqLent)
            val timeStampFinal = Alphabet + ss
            LogUtil.logE("timeStampFinal", timeStampFinal)

            return timeStampFinal
        }

        open fun getSaltString(reqLent: Int): String? {
            val SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
            val salt = StringBuilder()
            val rnd = Random()
            while (salt.length < reqLent) { // length of the random string.
                val index = (rnd.nextFloat() * SALTCHARS.length).toInt()
                salt.append(SALTCHARS[index])
            }
            return salt.toString()
        }

        fun isValidCVVNumber(str: String?): Boolean {
            // Regex to check valid CVV number.
            val regex = "^[0-9]{3,4}$"
            val p: Pattern = Pattern.compile(regex)
            if (str == null) {
                return false
            }
            val m: Matcher = p.matcher(str)
            return m.matches()
        }

        fun isValidCardExpNumber(str: String?): Boolean {
            // Regex to check valid CVV number.
            val regex = "/^(0[1-9]|1[0-2])\\/?([0-9]{2})\$/"
            val p: Pattern = Pattern.compile(regex)
            if (str == null) {
                return false
            }
            val m: Matcher = p.matcher(str)
            return m.matches()
        }

        fun clearAppData() {
            try {


                val runtime = Runtime.getRuntime()
                runtime.exec("pm clear com.pays.pos")

            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun formatted(): String {

            val current = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("MMM-dd-yyyy hh:mm:a")
            return current.format(formatter)

        }

        fun getTwoDecimal(value: Double): Double {

            try {
                val tmp = value.toString()
                val tmpIndex = tmp.indexOf(".", 0, true)
                return if (tmp.length > tmpIndex + 3) {
                    String.format("%.2f", value).toDouble()
                } else {
                    String.format("%.2f", value).toDouble()
                }
            } catch (e: java.lang.Exception) {
                Log.e("CheckDecCrash", "checkData ${e.message}")
                return value
            }


        }

        fun getTwoDecimalWithZero(value: Double): Double {

            Log.e("getTwoDecimalWithZero", "" + value)
            try {
                val tmp = value.toString()
                val tmpIndex = tmp.indexOf(".", 0, true)
                Log.e("getTwoDecimalWithZero", "" + tmpIndex)
                return if (tmp.length == tmpIndex + 2) {
                    Log.e("getTwoDecimalWithZero", "" + value.toPrecision(2) as Double)
                    value.toPrecision(2) as Double
                    //String.format("%.2f", value).toDouble()
                } else {
                    String.format("%.2f", value).toDouble()
                }
            } catch (e: java.lang.Exception) {
                Log.e("CheckDecCrash", "checkData ${e.message}")
                return value
            }


        }

        fun Double.toPrecision(precision: Int) =
            if (precision < 1) {
                "${this.roundToInt()}"
            } else {
                val p = 10.0.pow(precision)
                val v = (abs(this) * p).roundToInt()
                val i = floor(v / p)
                var f = "${floor(v - (i * p)).toInt()}"
                while (f.length < precision) f = "0$f"
                val s = if (this < 0) "-" else ""
                "$s${i.toInt()}.$f"
            }

        fun String.toDoubleWithPrecision(precision: Int): Double {
            return this.toDouble().toPrecision(precision).toDouble()
        }

        /**
         * An extension function to generate N-digit random numbers
         * Example: 6.generateRandomNumbers() or 12.generateRandomNumbers()
         * */
        fun Int.generateRandomNumbers(): Long {
            val randomNumber = StringBuilder()

            repeat(this) {
                randomNumber.append(kotlin.random.Random.nextInt(1, 10))
            }

            return randomNumber.toString().toLong()
        }

        fun getLatestCashDiscountOrSurCharge(
            totalAmount: Double,
            prefProvider: PrefProvider,
            context: Context
        ): Double {
            return if (isEnableCashDiscount(context)) {
                calculateCashDiscount(
                    totalAmount,
                    prefProvider,
                    context
                )
            } else {
                0.0
            }
        }

        fun removeChars(str: String?, numberOfCharactersToRemove: Int): String? {
            return if (str != null && !str.trim { it <= ' ' }.isEmpty()) {
                str.substring(0, str.length - numberOfCharactersToRemove)
            } else ""
        }
    }


}

fun Context.disconnectSocket() {
    try {
        UploadWorker2.workerDisconnect()
        WorkManager.getInstance(this).cancelAllWork()
    } catch (e: Exception) {
        //WorkManager.getInstance(this).cancelAllWork()
        Log.d("MainActivityOnPause", "onPause exception")
    }
}

