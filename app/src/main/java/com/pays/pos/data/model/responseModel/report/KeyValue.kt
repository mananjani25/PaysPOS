package com.pays.pos.data.model.responseModel.report

import android.util.Log
import android.view.View
import com.google.gson.annotations.SerializedName

data class KeyValue(
    @SerializedName("key")
    val key: String?,
    @SerializedName("value")
    val value: String?
) {
//    fun showFormattedValue() = "$" + String.format(
//        "%.2f", value ?: 0.0
//    )

    fun showData() =

        when {

            key?.trim().equals("Refunded Tax & Fees") -> {
                showFormattedValueMinus()
            }
            key?.trim().equals("Refunded Tips") -> {
                showFormattedValueMinus()
            }
            key?.trim().equals("Service Charges Refunded") -> {
                showFormattedValueMinus()
            }
            key?.trim().equals("Cash Refund") -> {
                showFormattedValueMinus()
            }
            key?.trim().equals("Card Refund") -> {
                showFormattedValueMinus()
            }
            key?.trim().equals("Refund by Card") -> {
                showFormattedValueMinus()
            }
            key?.trim().equals("Refund by Cash") -> {
                showFormattedValueMinus()
            }
            key?.trim().equals("Refund Tax by Card") -> {
                showFormattedValueMinus()
            }
            key?.trim().equals("Refund Tax by Cash") -> {
                showFormattedValueMinus()
            }

            key?.trim().equals("Tips Refunded") -> {
                showFormattedValueMinus()
            }

            key?.trim().equals("Refunds") -> {
                showFormattedValueMinus()
            }
            key?.trim().equals("Refund") -> {
                showFormattedValueMinus()
            }
            key?.trim().equals("Refund SC by Card") -> {
                showFormattedValueMinus()
            }
            key?.trim().equals("Refund SC by Cash") -> {
                showFormattedValueMinus()
            }
            key?.trim().equals("Total Refunds") -> {
                showFormattedValueMinus()
            }
            key?.trim().equals("Name") -> {
                showName()
            }
            else -> {
                showFormattedValue()
            }
        }


    fun showFormattedValue() = if (value?.isEmpty() == true) "$0.00" else "$" + String.format(
        "%.2f", value?.toDouble() ?: 0.0
    )

    fun showName() =  value.toString()

    fun showDividerLine() = if(key?.contains("name",true)==true) View.VISIBLE else View.GONE

    private fun showFormattedValueMinus() =
        if (value?.isEmpty() == true || (value=="0.0" || value=="0.00")) "$0.00" else "-$" + String.format(
            "%.2f", value?.toDouble() ?: 0.0
        )
}
