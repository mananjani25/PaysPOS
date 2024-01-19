package com.pays.pos.data.model.responseModel

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.pays.pos.data.entities.TbAddress
import com.pays.pos.data.entities.TbCustomer
import com.pays.pos.data.entities.TbPhones
import com.pays.pos.data.model.CustomerListResponse
import com.pays.pos.data.typeconvert.TypeConvertorAddress
import com.pays.pos.data.typeconvert.TypeConvertorPhone
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class CreateCustomerReponse(
    @SerializedName("data")
    val data: TbCustomer,
) : BaseResponse(), Parcelable {

    /*@Parcelize
    @Entity(tableName = "TbCustomer")
    data class Data(
        @PrimaryKey
        @SerializedName("id") val id: Int,
        @SerializedName("first_name") val first_name: String,
        @SerializedName("last_name") val last_name: String,
        @SerializedName("birth_date") val birth_date: String?,
        @SerializedName("email") val email: String,
        @TypeConverters(TypeConvertorPhone::class)
        @SerializedName("phones") val phones: List<TbPhones> = listOf(),
        @TypeConverters(TypeConvertorAddress::class)
        @SerializedName("addresses") val addresses: List<TbAddress> = listOf(),
        var isSelcted: Boolean = false
    ) : Parcelable {
    }*/


}