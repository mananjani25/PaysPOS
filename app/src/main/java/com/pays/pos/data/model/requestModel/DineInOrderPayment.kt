package com.pays.pos.data.model.requestModel

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class DineInOrderPayment(@SerializedName("order") var orderRequestModel: DineInPaymentUpdateModel?=null) :
    Parcelable {
}