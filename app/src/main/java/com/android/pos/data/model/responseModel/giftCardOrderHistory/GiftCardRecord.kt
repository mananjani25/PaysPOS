package com.android.pos.data.model.responseModel.giftCardOrderHistory

data class GiftCardRecord(
    val amount: String,
    val created_at: String,
    val event: String,
    val id: Int,
    val item_details: String,
    val new_balance: String,
    val old_balance: String
)