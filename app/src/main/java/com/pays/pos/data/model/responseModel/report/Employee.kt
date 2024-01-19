package com.pays.pos.data.model.responseModel.report


import com.google.gson.annotations.SerializedName

data class Employee(
    @SerializedName("full_name")
    val fullName: String?,
    @SerializedName("items")
    val items: Int?,
    @SerializedName("paid_orders")
    val paidOrders: Int?,
    @SerializedName("refund")
    val refund: Double?,
    @SerializedName("revenue")
    val revenue: Double?
){
    fun showRevenue() = "$" + String.format(
        "%.2f", revenue
    )
    fun showRefunds() = "$" + String.format(
        "%.2f", refund
    )
}