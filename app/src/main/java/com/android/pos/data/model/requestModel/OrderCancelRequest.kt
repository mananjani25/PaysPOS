package com.android.pos.data.model.requestModel


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

data class OrderCancelRequest(

    @SerializedName("order")
    val order: OrderData

) {
    @Parcelize
    class OrderData(

        @SerializedName("payment_status")
        val payment_status: String,
        @SerializedName("custom_cancel_order_reason")
        val custom_cancel_order_reason: String,
        @SerializedName("cancel_order_reason_id")
        val cancel_order_reason_id: Int?,
        @SerializedName("cancel_by_employee_id")
        val cancel_by_employee_id: Int?,
        @SerializedName("future_delivery_date")
        val futureDeliveryDate: String = "",
        @SerializedName("future_delivery_time")
        val futureDeliveryTime: String = ""

    ) : Parcelable
}