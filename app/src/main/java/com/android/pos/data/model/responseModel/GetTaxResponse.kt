package com.android.pos.data.model.responseModel


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class GetTaxResponse(
    @SerializedName("data")
    val `data`: List<TaxData>,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("type")
    val type: String
) : Parcelable {
    @Parcelize
    data class TaxData(
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
        val isActive: Boolean,
        @SerializedName("is_default")
        val isDefault: String,
        @SerializedName("is_custom_amount")
        val isCustomAmount: String,
        @SerializedName("item_pricing")
        val itemPricing: String,
        @SerializedName("item_ids")
        val itemIds: List<Int>
    ) : Parcelable
}