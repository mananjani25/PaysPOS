package com.android.pos.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.android.pos.data.typeconvert.TypeConvertorAddress
import com.android.pos.data.typeconvert.TypeConvertorPhone
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "TbCustomer")
data class TbCustomer(

    @PrimaryKey
    @SerializedName("id") val id: Int?,
    @SerializedName("first_name") val first_name: String?,
    @SerializedName("last_name") val last_name: String?,
    @SerializedName("birth_date") val birth_date: String?,
    @SerializedName("email") val email: String,
    @SerializedName("company") val company: String? = null,
    @TypeConverters(TypeConvertorPhone::class)
    @SerializedName("phones") val phones: List<TbPhones> = listOf(),
    @TypeConverters(TypeConvertorAddress::class)
    @SerializedName("addresses") val addresses: List<TbAddress> = listOf(),
    var isSelcted: Boolean = false
) : Parcelable
