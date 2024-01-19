package com.pays.pos.data.model.requestModel

data class SpitByOrderRequestModel(

    var id: Int? = null,
    var completed_all_payments: Boolean = false,
    var amount_tab: SpitByOrderPaymentModel,
    var gift_card_redeem: Boolean? = false,
    var gift_card: GiftCardRedeem? = null
    /*var order: SpitByOrderPaymentModel*/
) {
    class GiftCardRedeem(
        var name: String = "",
        var pin: String = ""
    )
}
