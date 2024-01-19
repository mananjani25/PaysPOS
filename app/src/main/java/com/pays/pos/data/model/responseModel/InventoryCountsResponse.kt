package com.pays.pos.data.model.responseModel

import com.google.gson.annotations.SerializedName

class InventoryCountsResponse(
    @SerializedName("data")
    val `data`: Data
) : BaseResponse() {
    data class Data(
        @SerializedName("active_items")
        val activeItems: Int,
        @SerializedName("categories")
        val categories: Int,
        @SerializedName("modifier_sets")
        val modifierSets: Int,
        @SerializedName("option_sets")
        val optionSets: Int,
        @SerializedName("hidden_categories")
        val hiddenCategories: Int,
        @SerializedName("hidden_items")
        val hiddenItems: Int,
        @SerializedName("hidden_items_website")
        val hidden_items_website: Int
    )
}