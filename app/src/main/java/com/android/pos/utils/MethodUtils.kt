package com.android.pos.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import com.android.pos.MainApplication
import com.android.pos.R
import com.android.pos.data.model.requestModel.CreateCategoryRequestModel
import com.android.pos.data.model.requestModel.CreateItemRequestModel
import com.android.pos.utils.extensions.toMultiPartRequestBody
import okhttp3.RequestBody
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

        fun getTextTextView(edtFirstName: AppCompatTextView): String {

            return edtFirstName.text.toString().trim()
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
            createItemRequestMap["productCode"] =
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
    }

    /*fun addItemsForDineIn(list: ArrayList<DineInModel>): ArrayList<DineInModel> {
        for (i in 0 until list.size) {
            if (list.get(i).items.isNotEmpty()) {
                list[i].items.forEach {

                }
            }

        }


    }*/
}