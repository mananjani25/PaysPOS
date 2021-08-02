package com.android.pos.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.android.pos.data.model.responseModel.BaseResponse
import com.android.pos.data.typeconvert.TypeConvertersIds
import com.android.pos.data.typeconvert.TypeConvertorAddress
import com.android.pos.data.typeconvert.TypeConvertorPhone
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class CustomerListResponse(
    @SerializedName("data")
    val data: List<Data>,
) : BaseResponse(), Parcelable {

    @Parcelize
    @Entity(tableName = "TbCustomer")
    data class Data(
        @PrimaryKey
        @SerializedName("id") val id: Int,
        @SerializedName("first_name") val first_name: String,
        @SerializedName("last_name") val last_name: String,
        @SerializedName("birth_date") val birth_date: String,
        @SerializedName("email") val email: String,
        @TypeConverters(TypeConvertorPhone::class)
        @SerializedName("phones") val phones: List<Phones> = listOf(),
        @TypeConverters(TypeConvertorAddress::class)
        @SerializedName("addresses") val addresses: List<Addresses> = listOf(),
        var isSelcted: Boolean = false
    ) : Parcelable {
        @Parcelize
        data class Phones(
            @SerializedName("id") val id: Int,
            @SerializedName("phone_number") val phone_number: String
        ) : Parcelable


        @Parcelize
        data class Addresses(
            @SerializedName("id") val id: Int,
            @SerializedName("address1") val address1: String,
            @SerializedName("address2") val address2: String,
            @SerializedName("city") val city: String,
            @SerializedName("state") val state: String,
            @SerializedName("country") val country: String,
            @SerializedName("postcode") val postcode: Int,
            @SerializedName("address_type") val address_type: String,
            @SerializedName("latitude") val latitude: String,
            @SerializedName("longitude") val longitude: String,
            @SerializedName("type_of_address") val type_of_address: Int,
            @SerializedName("full_address") val full_address: String,
            @SerializedName("street") val street: String
        ) : Parcelable

    }
}
