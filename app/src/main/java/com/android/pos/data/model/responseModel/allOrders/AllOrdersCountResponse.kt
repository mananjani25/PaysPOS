package com.android.pos.data.model.responseModel.allOrders

data class AllOrdersCountResponse(
    val `data`: Data,
    val message: String,
    val status: Int,
    val type: String
)