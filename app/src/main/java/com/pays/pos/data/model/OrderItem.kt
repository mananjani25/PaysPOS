package com.pays.pos.data.model

data class OrderItem(
    val category_id: Int,
    val completed_in_kitchen: Boolean,
    val discount_amount: Double,
    val discount_id: Any,
    val discount_type: Any,
    val employee_id: Int,
    val float: Double,
    val id: Int,
    val is_fired: Boolean,
    val is_paid: Boolean,
    val is_printed: Boolean,
    val item_id: Int,
    val item_name: String,
    val note: String,
    val order_id: Int,
    val order_item_modifiers: List<Any>,
    val order_item_taxes: List<OrderItemTaxe>,
    val order_item_variation: OrderItemVariation,
    val price: Double,
    val quantity: Int,
    val refunded_amount: Double,
    val refunded_quantity: Any,
    val timestamp: Any,
    val total_price: Double
)