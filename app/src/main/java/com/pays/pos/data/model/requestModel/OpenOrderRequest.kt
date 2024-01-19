package com.pays.pos.data.model.requestModel


import com.google.gson.annotations.SerializedName

data class OpenOrderRequest(
    @SerializedName("payment_status")
    val payment_status: String,
    @SerializedName("start_date")
    val startDate: String,
    @SerializedName("end_date")
    val endDate: String
)