package com.pays.pos.data.entities

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class TbPhones(
    @SerializedName("id") val id: Int?,
    @SerializedName("phone_number") val phone_number: String
) : Parcelable
