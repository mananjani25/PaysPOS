package com.pays.pos.data.model.responseModel


import com.pays.pos.data.entities.BusinessAddress
import com.pays.pos.data.entities.TbTimeZones
import com.google.gson.annotations.SerializedName

data class BusinessResponse(
    @SerializedName("data") val `data`: Data,
) : BaseResponse() {
    data class Data(
        @SerializedName("id") val id: Int,
        @SerializedName("business_name") val businessName: String,
        @SerializedName("phone_number") val phoneNumber: String,
        @SerializedName("phone_number_2") val phoneNumber2: String,
        @SerializedName("phone_number_1_country") val phoneNumber1Country: String,
        @SerializedName("phone_number_2_country") val phoneNumber2Country: String,
        @SerializedName("customer_contact_email") val customerContactEmail: String,
        @SerializedName("business_website") val businessWebsite: String,
        @SerializedName("address") val address: String?,
        @SerializedName("user_id") val userId: Int,
        @SerializedName("subdomain") val subdomain: String,
        @SerializedName("location_category_id") val locationCategoryId: Int,
        @SerializedName("time_zone") val timeZone: String,
        @SerializedName("created_at") val createdAt: String,
        @SerializedName("updated_at") val updatedAt: String,
        @SerializedName("logo") val logo: Logo,
        @SerializedName("time_zone_options") val timeZoneOptions: List<TbTimeZones>,
        @SerializedName("address_attributes") val addressAttributes: BusinessAddress
    ) {
        data class Logo(
            @SerializedName("url") val url: String,
            @SerializedName("thumb") val thumb: Thumb
        ) {
            data class Thumb(
                @SerializedName("url") val url: String
            )
        }

    }
}