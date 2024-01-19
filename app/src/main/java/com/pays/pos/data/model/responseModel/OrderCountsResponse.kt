package com.pays.pos.data.model.responseModel


import com.google.gson.annotations.SerializedName

data class OrderCountsResponse(
    @SerializedName("data")
    val `data`: Data
) : BaseResponse() {
    data class Data(
        @SerializedName("active_orders")
        val activeOrders: Int,
        @SerializedName("cancelled_orders")
        val cancelledOrders: Int,
        @SerializedName("completed_orders")
        val completedOrders: Int,
        @SerializedName("upcoming_orders")
        val upcomingOrders: Int
    )
}