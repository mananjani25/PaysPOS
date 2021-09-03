package com.android.pos.data.entities

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class VariationsAttribute(
    @SerializedName("is_active")
    var isActive: Boolean = false,
    @SerializedName("is_custom")
    var isCustom: Boolean = false,
    @SerializedName("name")
    var name: String = "",
    @SerializedName("option_ids")
    var optionIds: List<Int>? = null,
    @SerializedName("option_set_id")
    var optionSetIds: List<Int>? = null,
    @SerializedName("price")
    var price: Double = 0.0,
    @SerializedName("sku")
    var sku: String = "",
    @SerializedName("stock_qty")
    var stockQty: String = ""
) : Parcelable

