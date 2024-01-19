package com.pays.pos.data.model.responseModel.orderhistory


import com.google.gson.annotations.SerializedName

data class PaymentDetail(
    @SerializedName("amount")
    val amount: Double?,
    @SerializedName("cc_number")
    val ccNumber: String?,
    @SerializedName("id")
    val id: Int?,
    @SerializedName("payment_type")
    val paymentType: String?
)