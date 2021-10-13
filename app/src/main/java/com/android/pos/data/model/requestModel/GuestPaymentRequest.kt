package com.android.pos.data.model.requestModel

import com.google.gson.annotations.SerializedName

data class GuestPaymentRequest(
    @SerializedName("guest") var paymentAttributes: PaymentAttributes
) {

}
