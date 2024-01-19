package com.pays.pos.data.model

data class OrderItemVariation(
    val created_at: String,
    val id: Int,
    val name: String,
    val order_id: Any,
    val order_item_id: Int,
    val quantity: Int,
    val total_price: Any,
    val unit_price: Double,
    val updated_at: String,
    val variation_id: Int
)