package com.pays.pos.data.entities


import androidx.room.TypeConverters
import com.pays.pos.data.typeconvert.TCBusiness
import com.google.gson.annotations.SerializedName
@TypeConverters(_root_ide_package_.com.pays.pos.data.typeconvert.TCBusiness::class)
data class  BusinessAddress(
    @SerializedName("id") var bid: Int?=null,
    @SerializedName("address1") var address1: String,
    @SerializedName("address2") var address2: String,
    @SerializedName("city") var city: String,
    @SerializedName("state") var state: String,
    @SerializedName("country") var country: String,
    @SerializedName("postcode") var postcode: String,
    @SerializedName("addressable_type") val addressableType: String = "",
    @SerializedName("addressable_id") val addressableId: Int?=null,
    @SerializedName("created_at") val createdAt: String?=null,
    @SerializedName("updated_at") val updatedAt: String?=null,
    @SerializedName("latitude") val latitude: String?=null,
    @SerializedName("longitude") val longitude: String?=null,
    @SerializedName("type_of_address") val typeOfAddress: String?=null
)