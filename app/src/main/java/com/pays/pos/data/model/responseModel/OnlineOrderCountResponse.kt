package com.pays.pos.data.model.responseModel

import com.google.gson.annotations.SerializedName

data class OnlineOrderCountResponse (
    @SerializedName("data")
    val `data`: Data
    ) : BaseResponse() {
        data class Data(
            @SerializedName("online_pending_orders")
            val online_pending_orders: Int,
            @SerializedName("online_in_progress_orders")
            val online_in_progress_orders: Int,
            @SerializedName("online_complete_orders")
            val online_complete_orders: Int,
            @SerializedName("online_rejected_orders")
            val online_rejected_orders: Int,
            @SerializedName("upcoming_orders")
            val upcoming_orders: Int
        )
}