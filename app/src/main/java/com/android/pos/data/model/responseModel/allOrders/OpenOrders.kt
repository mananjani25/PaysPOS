package com.android.pos.data.model.responseModel.allOrders

data class OpenOrders(
    val active: Int,
    val all: Int,
    val cancelled: Int,
    val completed: Int
)