package com.pays.pos.data.model.requestModel


import com.google.gson.annotations.SerializedName

data class CreateTaxRequestModel(
    @SerializedName("id")
    var id: Int? = null,
    @SerializedName("location_id")
    var locationId: Int = -1,
    @SerializedName("name")
    var name: String = "",
    @SerializedName("rate")
    var rate: Double = 0.0,
    @SerializedName("tax_type")
    var taxType: String = "",
    @SerializedName("is_active")
    var isActive: Boolean = false,
    @SerializedName("is_default")
    var isDefault: Boolean = false,
    @SerializedName("is_custom_amount")
    var isCustomAmount: Boolean = false,
    @SerializedName("item_pricing")
    var itemPricing: String = "",
    @SerializedName("item_ids")
    var itemIds: ArrayList<Int>? = null
)