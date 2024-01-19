package com.pays.pos.data.model.responseModel.orderhistory


import com.google.gson.annotations.SerializedName

data class Phone(
    @SerializedName("id")
    val id: Int?,
    @SerializedName("phone_number")
    val phoneNumber: String?
)