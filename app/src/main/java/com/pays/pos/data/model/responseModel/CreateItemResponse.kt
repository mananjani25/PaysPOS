package com.pays.pos.data.model.responseModel


import com.google.gson.annotations.SerializedName

data class CreateItemResponse(
    @SerializedName("data")
    val `data`: List<Data>,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("type")
    val type: String
) {
    data class Data(
        @SerializedName("active")
        val active: Boolean,
        @SerializedName("cost")
        val cost: Double,
        @SerializedName("id")
        val id: Int,
        @SerializedName("kitchen_name")
        val kitchenName: String,
        @SerializedName("modifier_set_ids")
        val modifierSetIds: List<Int>,
        @SerializedName("name")
        val name: String,
        @SerializedName("original_image_url")
        val originalImageUrl: Any,
        @SerializedName("price")
        val price: Double,
        @SerializedName("price_type")
        val priceType: String,
        @SerializedName("product_code")
        val productCode: String,
        @SerializedName("quantity")
        val quantity: Int,
        @SerializedName("sku")
        val sku: String,
        @SerializedName("sort")
        val sort: Int,
        @SerializedName("thumb_image_url")
        val thumbImageUrl: Any
    )
}