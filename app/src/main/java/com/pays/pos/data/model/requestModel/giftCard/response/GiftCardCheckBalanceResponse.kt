package com.pays.pos.data.model.requestModel.giftCard.response

data class GiftCardCheckBalanceResponse(
    val `data`: Data?,
    val message: String,
    val status: Int,
    val type: String
) {
    data class Data(
        val amount: Double
    )
}