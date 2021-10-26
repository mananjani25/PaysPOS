package com.android.pos.data.model.requestModel

data class SpitByOrderRequestModel(

    var id: Int? = null,
    var completed_all_payments: Boolean = false,
    var amount_tab: PaymentAttributes,
    var order: SpitByOrderPaymentModel
)
