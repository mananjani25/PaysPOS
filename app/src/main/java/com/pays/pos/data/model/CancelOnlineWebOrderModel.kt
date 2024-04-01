package com.pays.pos.data.model

data class CancelOnlineWebOrderModel(
    val time: Int,
    val orderId: Int,
    val is_accepted: Boolean,
    var isRefunded: Boolean)