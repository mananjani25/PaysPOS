package com.android.pos.data.model.requestModel

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


data class OrderRequestModel(

    var completed_all_payments: Boolean = false,
    var order: OrderAttributeRequestModel
)
