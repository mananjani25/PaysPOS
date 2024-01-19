package com.pays.pos.data.entities

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Entity(tableName = "TbBusinessDetails")
class TbBusinessDetails {


    @PrimaryKey
    @SerializedName("id")
    var id: Int = 0

    @SerializedName("business_name")
    var business_name: String? = null

    @SerializedName("business_website")
    var business_website: String? = null

    @SerializedName("phone_number")
    var phone_number: String? = null

    @SerializedName("phone_number_2_country")
    var phone_number_2_country: String? = null

    @SerializedName("phone_number_1_country")
    var phone_number_1_country: String? = null

    @SerializedName("phone_number_2")
    var phone_number_2: String? = null


    @SerializedName("time_zone")
    var time_zone: String? = null

    @SerializedName("customer_contact_email")
    var customer_contact_email: String? = null


    @SerializedName("address_attributes")
    var businessAddress : List<BusinessAddress> = emptyList()

    @SerializedName("is_deleted")
    var isDeleted: Boolean = false
}

