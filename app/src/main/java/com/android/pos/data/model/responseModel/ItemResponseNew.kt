package com.android.pos.data.model.responseModel


import com.google.gson.annotations.SerializedName

data class ItemResponseNew(
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
        @SerializedName("active")
        val active: Boolean,
        @SerializedName("category_id")
        val categoryId: Int,
        @SerializedName("category_name")
        val categoryName: String,
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
        @SerializedName("option_sets")
        val optionSets: List<Int>,
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
        @SerializedName("selected_option_sets")
        val selectedOptionSets: List<SelectedOptionSet>,
        @SerializedName("sku")
        val sku: String,
        @SerializedName("sort")
        val sort: Int,
        @SerializedName("thumb_image_url")
        val thumbImageUrl: Any,
        @SerializedName("variations")
        val variations: List<Variation>
    ) {
        data class SelectedOptionSet(
            @SerializedName("display_name")
            val displayName: String,
            @SerializedName("id")
            val id: Int,
            @SerializedName("location_id")
            val locationId: Int,
            @SerializedName("name")
            val name: String,
            @SerializedName("option_type")
            val optionType: String,
            @SerializedName("options")
            val options: List<Option>,
            @SerializedName("sort")
            val sort: Int
        ) {
            data class Option(
                @SerializedName("id")
                val id: Int,
                @SerializedName("name")
                val name: String,
                @SerializedName("sort")
                val sort: Int
            )
        }

        data class Variation(
            @SerializedName("id")
            val id: Int,
            @SerializedName("is_active")
            val isActive: Boolean,
            @SerializedName("is_custom")
            val isCustom: Boolean,
            @SerializedName("item_id")
            val itemId: Int,
            @SerializedName("name")
            val name: String,
            @SerializedName("option_ids")
            val optionIds: List<Int>,
            @SerializedName("option_set_id")
            val optionSetId: List<Int>,
            @SerializedName("price")
            val price: Double,
            @SerializedName("price_type")
            val priceType: String,
            @SerializedName("sku")
            val sku: String,
            @SerializedName("stock_qty")
            val stockQty: Any
        )
    }
}