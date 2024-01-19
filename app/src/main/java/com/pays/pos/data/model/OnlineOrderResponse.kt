package com.pays.pos.data.model

data class OnlineOrderResponse(
    val `data`: Data,
    val message: String,
    val status: Int,
    val type: String
)