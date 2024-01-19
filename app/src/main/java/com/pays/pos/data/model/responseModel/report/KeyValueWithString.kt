package com.pays.pos.data.model.responseModel.report

import com.google.gson.annotations.SerializedName

data class KeyValueWithString(
    @SerializedName("key")
    val key: String?,
    @SerializedName("value")
    val value: String?
) {
    fun showFormattedValue(): String {
        val formatted = try {
            "$" + String.format(
                "%.2f", value?.toDoubleOrNull()?:0
            )
        } catch (e: Exception) {
            value ?: ""
        }
        return formatted
    }
}
