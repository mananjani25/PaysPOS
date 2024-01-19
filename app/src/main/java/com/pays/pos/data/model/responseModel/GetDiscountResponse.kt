package com.pays.pos.data.model.responseModel


import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pays.pos.data.entities.TbDiscount
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class GetDiscountResponse(
    @SerializedName("data")
    val `data`: List<TbDiscount>,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("type")
    val type: String
) : Parcelable