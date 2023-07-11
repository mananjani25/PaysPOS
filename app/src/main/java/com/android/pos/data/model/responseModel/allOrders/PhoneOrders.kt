package com.android.pos.data.model.responseModel.allOrders

data class PhoneOrders(
    val active: Int,
    val all: Int,
    val cancelled: Int,
    val completed: Int
)