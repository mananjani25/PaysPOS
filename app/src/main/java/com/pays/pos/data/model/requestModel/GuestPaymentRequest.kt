package com.pays.pos.data.model.requestModel

import android.os.Parcelable
import com.pays.pos.data.model.responseModel.GuestPaymentAttributes
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class GuestPaymentRequest(
    @SerializedName("guest") var paymentAttributes: GuestPaymentAttributes,
    @SerializedName("order") var orderReq: DineInPaymentUpdateModel
):Parcelable {

}
