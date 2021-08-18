package com.android.pos.data.model.requestModel

import com.google.gson.annotations.SerializedName


data class CreateItemRequestModel(
    @SerializedName("active")
    var active: Boolean = false,
    @SerializedName("category_id")
    var categoryId: Int = -1,
    @SerializedName("cost")
    var cost: Double = 0.0,
    @SerializedName("desc")
    var desc: String = "",
    /*@SerializedName("id")
    var id: Int = -1,
    @SerializedName("kitchen_name")
    var kitchenName: Int = -1,*/
    @SerializedName("location_id")
    var locationId: Int = -1,
    @SerializedName("modifier_set_ids")
    var modifierSetIds: List<Int>? = null,
    @SerializedName("name")
    var name: String = "",
    @SerializedName("price")
    var price: Double = 0.0,
    @SerializedName("price_type")
    var priceType: String = "",
    @SerializedName("product_code")
    var productCode: String = "",
    @SerializedName("quantity")
    var quantity: Int = -1,
    @SerializedName("sku")
    var sku: String = "",
    @SerializedName("tax_ids")
    var taxIds: List<Int>? = null,
    @SerializedName("variations_attributes")
    var variationsAttributes: List<VariationsAttribute>? = null
) {
    data class VariationsAttribute(
        @SerializedName("is_active")
        var isActive: Boolean = false,
        @SerializedName("is_custom")
        var isCustom: Boolean = false,
        @SerializedName("name")
        var name: String = "",
        @SerializedName("option_ids")
        var optionIds: String = "",
        @SerializedName("option_set_id")
        var optionSetId: String = "",
        @SerializedName("price")
        var price: Double = 0.0,
        @SerializedName("sku")
        var sku: String = "",
        @SerializedName("stock_qty")
        var stockQty: String = ""
    )
}

