package com.pays.pos.data.entities

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class VariationsAttribute(
    @SerializedName("id")
    var id: Int? = null,
    @SerializedName("is_active")
    var isActive: Boolean = true,
    @SerializedName("is_custom")
    var isCustom: Boolean = false,
    @SerializedName("name")
    var name: String? = "",
    @SerializedName("option_ids")
    var optionIds: List<Int>? = null,
    @SerializedName("option_set_id")
    var optionSetIds: List<Int>? = null,
    @SerializedName("price")
    var price: Double? = null,
//    @SerializedName("price")
//    var priceWithMarkup: Double? = null,
    @SerializedName("price_type")
    var priceType: String = "",
    @SerializedName("sku")
    var sku: String? = null,
    @SerializedName("stock_qty")
    var stockQty: String?=null,
    @SerializedName("_destroy")
    var _destroy: Boolean = false,
    @SerializedName("is_deleted")
    var isDeleted: Boolean = false,
    var isChecked: Boolean = false,
    var orderVariationId: Int? = null

) : Parcelable

