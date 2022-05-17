package com.android.pos.data.entities


import com.google.gson.annotations.SerializedName

data class  BusinessAddress(
    @SerializedName("id") val bid: Int,
    @SerializedName("address1") val address1: String,
    @SerializedName("address2") val address2: String,
    @SerializedName("city") val city: String,
    @SerializedName("state") val state: String,
    @SerializedName("country") val country: String,
    @SerializedName("postcode") val postcode: String,
    @SerializedName("addressable_type") val addressableType: String,
    @SerializedName("addressable_id") val addressableId: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("latitude") val latitude: String,
    @SerializedName("longitude") val longitude: String,
    @SerializedName("type_of_address") val typeOfAddress: String
)