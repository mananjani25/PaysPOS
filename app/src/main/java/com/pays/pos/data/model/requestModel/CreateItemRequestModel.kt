package com.pays.pos.data.model.requestModel

import com.pays.pos.data.entities.VariationsAttribute
import com.google.gson.annotations.SerializedName


data class CreateItemRequestModel(
    @SerializedName("active")
    var active: Boolean = false,
    @SerializedName("category_id")
    var categoryId: Int? = null,
    @SerializedName("cost")
    var cost: Double = 0.0,
    @SerializedName("desc")
    var desc: String = "",
    @SerializedName("id")
    var id: Int? = null,
    @SerializedName("kitchen_name")
    var kitchenName: Int? = null,
    @SerializedName("location_id")
    var locationId: Int? = null,
    @SerializedName("modifier_set_ids")
    var modifierSetIds: List<Int>? = null,
    @SerializedName("name")
    var name: String = "",
    @SerializedName("price")
    var price: Double? = null,
    @SerializedName("image")
    var image: String? = "",
    @SerializedName("price_type")
    var priceType: String = "",
    @SerializedName("product_code")
    var productCode: String = "", //here
    @SerializedName("quantity")
    var quantity: Int? = 0,
    @SerializedName("sku")
    var sku: String = "",
    @SerializedName("tax_ids")
    var taxIds: List<String>? = null,
    @SerializedName("variations_attributes")
    var variationsAttributes: List<VariationsAttribute>? = null,
    @SerializedName("item_modifier_sets_sort")
    var itemModifierSetsSort: List<Int>? = null
)

