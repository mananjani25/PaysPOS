package com.android.pos.data.model.responseModel.report

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

    fun showFormattedValue() = if (value?.isEmpty() == true) "$0.00" else "$" + String.format(
        "%.2f", value?.toDouble() ?: 0.0
    )
}
