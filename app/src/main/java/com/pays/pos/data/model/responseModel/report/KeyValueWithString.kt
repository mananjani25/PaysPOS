package com.pays.pos.data.model.responseModel.report

import com.google.gson.annotations.SerializedName
import kotlin.math.absoluteValue

data class KeyValueWithString(
    @SerializedName("key")
    val key: String?,
    @SerializedName("value")
    val value: String?
) {
    fun showFormattedValue(): String {
        val formatted = try {
            if (value?.toDouble()?:0.0 < 0.0){
                var data=String.format(
                    "%.2f", value?.toDouble()?.absoluteValue ?: 0
                )
                if (data.toDouble()==0.0){
                    return "$" + data
                }else {
                    return "-$" + String.format(
                        "%.2f", value?.toDouble()?.absoluteValue ?: 0
                    )
                }
            }else {
               return  "$" + String.format(
                    "%.2f", value?.toDoubleOrNull() ?: 0
                )
            }
        } catch (e: Exception) {
            value ?: ""
        }

        /* val formatted = try {
            if (value?.toDouble()?:0.0 < 0.0){
                return  "-$" + String.format(
                    "%.2f", value?.toDouble()?.absoluteValue ?: 0
                )
            }else {
               return  "$" + String.format(
                    "%.2f", value?.toDoubleOrNull() ?: 0
                )
            }
        } catch (e: Exception) {
            value ?: ""
        }*/
        return formatted
    }
}
