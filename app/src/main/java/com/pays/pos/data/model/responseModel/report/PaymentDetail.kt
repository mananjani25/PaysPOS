package com.pays.pos.data.model.responseModel.report


import com.google.gson.annotations.SerializedName

data class PaymentDetail(
    @SerializedName("payment_details")
    val paymentDetails: List<List<KeyValue>>?
)