package com.android.pos.data.model.responseModel


import com.google.gson.annotations.SerializedName

data class VenueDataResponse(
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
        @SerializedName("categories")
        val categories: List<Category>
    ) {
        data class Category(
            @SerializedName("id")
            val id: Int,
            @SerializedName("items")
            val items: List<Item>,
            @SerializedName("name")
            val name: String,
            @SerializedName("sort")
            val sort: Int
        ) {
            data class Item(
                @SerializedName("cost")
                val cost: Int,
                @SerializedName("id")
                val id: Int,
                @SerializedName("kitchen_name")
                val kitchenName: String,
                @SerializedName("name")
                val name: String,
                @SerializedName("price")
                val price: Int,
                @SerializedName("price_type")
                val priceType: String,
                @SerializedName("product_code")
                val productCode: String,
                @SerializedName("quantity")
                val quantity: Int,
                @SerializedName("sku")
                val sku: String
            )
        }
    }
}