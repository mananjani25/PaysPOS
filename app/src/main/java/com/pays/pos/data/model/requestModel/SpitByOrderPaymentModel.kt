package com.pays.pos.data.model.requestModel

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SpitByOrderPaymentModel(

    var payments_attributes: List<PaymentAttributes>? = emptyList()
): Parcelable
