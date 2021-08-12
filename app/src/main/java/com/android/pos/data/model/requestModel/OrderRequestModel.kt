package com.android.pos.data.model.requestModel

data class OrderRequestModel(

    var completed_all_payments: Boolean = false,
    var order: OrderAttributeRequestModel
)
