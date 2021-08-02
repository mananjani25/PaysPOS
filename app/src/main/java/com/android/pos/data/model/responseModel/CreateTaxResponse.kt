package com.android.pos.data.model.responseModel


import com.google.gson.annotations.SerializedName

data class CreateTaxResponse(
    @SerializedName("data")
    val `data`: Data,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("type")
    val type: String
) {
    data class Data(
        @SerializedName("created_at")
        val createdAt: String,
        @SerializedName("id")
        val id: Int,
        @SerializedName("location_id")
        val locationId: Int,
        @SerializedName("name")
        val name: String,
        @SerializedName("rate")
        val rate: Double,
        @SerializedName("tax_type")
        val taxType: String,
        @SerializedName("updated_at")
        val updatedAt: String,
        @SerializedName("is_active")
        var isActive: Boolean = false,
        @SerializedName("is_default")
        val isDefault: Boolean,
        @SerializedName("is_custom_amount")
        val isCustomAmount: Boolean,
        @SerializedName("item_pricing")
        var itemPricing: String?,
        @SerializedName("item_ids")
        val itemIds: List<Int>
    )
}