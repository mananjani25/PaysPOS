package com.android.pos.data.model.requestModel

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SpitByOrderRequestModel(

    var id: Int? = null,
    var completed_all_payments: Boolean = false,
    var amount_tab: SpitByOrderPaymentModel
    /*var order: SpitByOrderPaymentModel*/
): Parcelable
