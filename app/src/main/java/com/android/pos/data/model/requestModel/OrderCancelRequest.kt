package com.android.pos.data.model.requestModel


import com.google.gson.annotations.SerializedName

data class OrderCancelRequest(
    @SerializedName("payment_status")
    val payment_status: String,
    @SerializedName("cancel_order_reason")
    val cancel_order_reason: String,
    @SerializedName("cancel_order_reason_id")
    val cancel_order_reason_id: Int?,
)