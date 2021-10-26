package com.android.pos.data.model.responseModel.report

import com.google.gson.annotations.SerializedName

data class KeyValue(
    @SerializedName("key")
    val key: String?,
    @SerializedName("value")
    val value: Double?
) {
    fun showFormattedValue() = "$" + String.format(
        "%.2f", value ?: 0.0
    )
}
