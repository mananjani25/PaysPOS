package com.pays.pos.data.model.responseModel


import com.google.gson.annotations.SerializedName

data class CreateDiscountResponse(
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
        @SerializedName("discount_type")
        val discountType: String,
        @SerializedName("id")
        val id: Int,
        @SerializedName("location_id")
        val locationId: Int,
        @SerializedName("name")
        val name: String,
        @SerializedName("percentage")
        val percentage: Double,
        @SerializedName("updated_at")
        val updatedAt: String,
        @SerializedName("is_active")
        var isActive: Boolean = false,
    )
}