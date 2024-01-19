package com.pays.pos.data.model.responseModel


import android.os.Parcelable
import com.pays.pos.data.entities.TbServiceCharge
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class GetServiceChargeResponse(
    @SerializedName("data")
    val `data`: List<TbServiceCharge>,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("type")
    val type: String
) : Parcelable