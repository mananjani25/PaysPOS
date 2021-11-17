package com.android.pos.data.model.responseModel.orderhistory


import com.google.gson.annotations.SerializedName

data class Orders(
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("date")
    val date: String?,
    @SerializedName("id")
    val id: Int?,
    @SerializedName("item_details")
    val itemDetails: String?,
    @SerializedName("payment_details")
    val paymentDetails: List<PaymentDetail>?,
    @SerializedName("payment_status")
    val paymentStatus: String?,
    @SerializedName("short_receipt_url")
    val shortReceiptUrl: String?,
    @SerializedName("total")
    val total: Double?,
    @SerializedName("order_loyalty_points")
    val order_loyalty_points: Double?
)