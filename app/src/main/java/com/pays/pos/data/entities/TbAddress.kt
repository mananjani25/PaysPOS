package com.pays.pos.data.entities

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class TbAddress(
    @SerializedName("id") val id: Int?,
    @SerializedName("address1") val address1: String,
    @SerializedName("address2") val address2: String,
    @SerializedName("city") val city: String,
    @SerializedName("state") val state: String,
    @SerializedName("country") val country: String,
    @SerializedName("postcode") val postcode: String,
    @SerializedName("address_type") val address_type: String = "",
    @SerializedName("latitude") val latitude: String="",
    @SerializedName("longitude") val longitude: String="",
    @SerializedName("type_of_address") val type_of_address: String,
    @SerializedName("full_address") val full_address: String,
    @SerializedName("street") val street: String
) : Parcelable
