package com.android.pos.data.model.responseModel.item

import com.android.pos.data.entities.ModifierSet
import com.android.pos.data.entities.OptionSet
import com.android.pos.data.entities.TaxData
import com.android.pos.data.entities.VariationsAttribute
import com.google.gson.annotations.SerializedName

data class Item(
    @SerializedName("active")
    val active: Boolean,
    @SerializedName("category_id")
    val categoryId: Int,
    @SerializedName("category_name")
    val categoryName: String?,
    @SerializedName("cost")
    val cost: Double,
    @SerializedName("id")
    val id: Int,
    @SerializedName("kitchen_name")
    val kitchenName: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("price")
    val price: Double,
    @SerializedName("price_type")
    val priceType: String?,
    @SerializedName("product_code")
    val productCode: String?,
    @SerializedName("quantity")
    val quantity: Int,
    @SerializedName("sku")
    val sku: String?,
    @SerializedName("desc")
    val desc: String?,
    @SerializedName("hide_status")
    val hide_status: String?,
    @SerializedName("website_hide_status")
    val website_hide_status: String?,
    @SerializedName("sort")
    val sort: Int,
    @SerializedName("original_image_url")
    val originalImageUrl: String?,
    @SerializedName("thumb_image_url")
    val thumbImageUrl: String?,
    @SerializedName("option_sets")
    val optionSets: List<Int>,
    @SerializedName("selected_option_sets")
    val selectedOptionSets: List<OptionSet>,
    @SerializedName("modifier_set_ids")
    val modifierSetIds: List<Int>,
    @SerializedName("modifier_sets")
    val modifierSets: List<ModifierSet> = emptyList(),
    @SerializedName("variations")
    val variations: List<VariationsAttribute>,
    var taxes: List<TaxData>? = null,
    @SerializedName("is_deleted")
    val isDeleted: Boolean
)