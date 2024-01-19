package com.pays.pos.data.model.responseModel.allOrders

data class ThirdPartyOnlineOrders(
    val all: Int,
    val completed: Int,
    val in_progress: Int,
    val pending: Int,
    val rejected: Int,
    val upcoming: Int
)