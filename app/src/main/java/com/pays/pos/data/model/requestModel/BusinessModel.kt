package com.pays.pos.data.model.requestModel


import androidx.room.PrimaryKey
import com.pays.pos.data.entities.BusinessAddress
import com.pays.pos.data.entities.TbBusinessDetails
import com.google.gson.annotations.SerializedName

 class BusinessModel{


     @SerializedName("id")
     var id: Int = 0

     @SerializedName("business_name")
     var business_name: String = ""

     @SerializedName("business_website")
     var business_website: String = ""

     @SerializedName("phone_number")
     var phone_number: String = ""

     @SerializedName("phone_number_2_country")
     var phone_number_2_country: String = ""

     @SerializedName("phone_number_1_country")
     var phone_number_1_country: String = ""

     @SerializedName("phone_number_2")
     var phone_number_2: String = ""


     @SerializedName("time_zone")
     var time_zone: String = ""

     @SerializedName("customer_contact_email")
     var customer_contact_email: String = ""

    @SerializedName("address_attributes")
    var addressAttributes: List<BusinessAddress> = emptyList()
 }
