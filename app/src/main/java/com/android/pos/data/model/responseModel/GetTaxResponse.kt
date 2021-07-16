package com.android.pos.data.model.responseModel


import com.google.gson.annotations.SerializedName

data class GetTaxResponse(
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
        @SerializedName("created_at")
        val createdAt: String,
        @SerializedName("id")
        val id: Int,
        @SerializedName("location_id")
        val locationId: Int,
        @SerializedName("name")
        val name: String,
        @SerializedName("rate")
        val rate: Int,
        @SerializedName("tax_type")
        val taxType: Any,
        @SerializedName("updated_at")
        val updatedAt: String
    )
}