package com.android.pos.data.model.responseModel.allOrders

data class PhoneOrders(
    val all: Int,
    val completed: Int,
    val in_progress: Int,
    val pending: Int,
    val rejected: Int,
    val upcoming: Int
)