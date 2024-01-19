package com.pays.pos.data.model.responseModel.report

import com.google.gson.annotations.SerializedName

data class Terminal(
    @SerializedName("name")
    val name: String?,
    @SerializedName("total")
    val total: Double?
) {
    fun showFormattedValue(): String {
        return if (total == -9.9) {
            "Amount Collected"
        } else {
            "$" + String.format(

                "%.2f", total
            )
        }
    }
}
