package com.pays.pos.data.model.responseModel.orderhistory


import com.google.gson.annotations.SerializedName

data class Addresse(
    @SerializedName("address1")
    val address1: String?,
    @SerializedName("address2")
    val address2: String?,
    @SerializedName("city")
    val city: String?,
    @SerializedName("country")
    val country: Any?,
    @SerializedName("full_address")
    val fullAddress: String?,
    @SerializedName("id")
    val id: Int?,
    @SerializedName("latitude")
    val latitude: Any?,
    @SerializedName("longitude")
    val longitude: Any?,
    @SerializedName("postcode")
    val postcode: String?,
    @SerializedName("state")
    val state: String?,
    @SerializedName("street")
    val street: String?,
    @SerializedName("type_of_address")
    val typeOfAddress: String?
)