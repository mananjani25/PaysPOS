package com.pays.pos.data.model

data class OrderItemTaxe(
    val amount: Any,
    val created_at: String,
    val deleted_at: Any,
    val id: Int,
    val is_default: Boolean,
    val is_tax_removed: Boolean,
    val name: String,
    val order_id: Int,
    val order_item_id: Int,
    val order_item_modifier_id: Any,
    val rate: Double,
    val tax_id: Int,
    val tax_total_amount: Double,
    val tax_type: String,
    val updated_at: String
)