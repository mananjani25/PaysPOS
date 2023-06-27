package com.android.pos.data.model.requestModel

data class OrderRequestModel(

    var completed_all_payments: Boolean = false,
    var order: OrderAttributeRequestModel,
    var send_payment_link: Boolean? = false,
    var gift_card_redeem: Boolean? = false,
    var gift_card: GiftCardRedeem? = null
) {
    class GiftCardRedeem(
        var name: String = "",
        var pin: String = ""
    )
}
