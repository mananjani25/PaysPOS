package com.pays.pos.data.model.requestModel

import android.os.Parcelable
import com.pays.pos.data.model.responseModel.GuestPaymentAttributes
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class GuestPaymentRequest(
    @SerializedName("guest") var paymentAttributes: GuestPaymentAttributes,
    @SerializedName("order") var orderReq: DineInPaymentUpdateModel,
    @SerializedName("gift_card") var gift_card: GuestPaymentRequest.GiftCardRedeem? = null,
):Parcelable {
    @Parcelize
    class GiftCardRedeem(
        var name: String = "",
        var pin: String = ""
    ):Parcelable

}
