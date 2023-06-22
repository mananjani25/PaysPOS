package com.android.pos.data.model.requestModel.giftCard.response

data class GiftCard(
    val id: Int,
    val amount: String,
    val customer_id: Int,
    val location_id: Int,
    val name: String,
    val password: String,
    val payments: List<Payment>
)