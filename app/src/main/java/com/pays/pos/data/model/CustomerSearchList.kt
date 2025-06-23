package com.pays.pos.data.model


import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import com.pays.pos.data.entities.TbAddress
import com.pays.pos.data.entities.TbPhones
import com.pays.pos.data.typeconvert.TypeConvertorAddress
import com.pays.pos.data.typeconvert.TypeConvertorPhone

data class CustomerSearchList(
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
        @SerializedName("id") val id: Int?,
        @SerializedName("first_name") val first_name: String?,
        @SerializedName("last_name") val last_name: String?,
        @SerializedName("birth_date") val birth_date: String?,
        @SerializedName("email") val email: String?,
        @SerializedName("enroll_to_loyalty") var enroll_to_loyalty: Boolean? = false,
        @SerializedName("same_as_billing_address") var same_as_billing_address: Boolean? = false,
        @SerializedName("final_reward") var final_reward: Int? = 0,
        @SerializedName("company") val company: String? = null,
        @TypeConverters(TypeConvertorPhone::class)
        @SerializedName("phones") val phones: List<TbPhones> = listOf(),
        @TypeConverters(TypeConvertorAddress::class)
        @SerializedName("addresses") val addresses: List<TbAddress> = listOf(),
        @SerializedName("is_tokenized") val isTokenized: Boolean = false,
        @SerializedName("card_token") val cardToken: String = "",
    ) {

    }
}


