package com.android.pos.data.model.requestModel.giftCard.request

data class GiftCard(
    val amount: String,
    val customer_id: Int,
    val location_id: Int,
    val name: String,
    val password: String,
    val payment_attributes: PaymentAttributes?
)