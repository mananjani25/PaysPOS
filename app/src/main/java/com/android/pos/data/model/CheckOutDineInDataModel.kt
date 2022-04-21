package com.android.pos.data.model

import android.os.Parcelable
import com.android.pos.data.model.requestModel.DineInOrderPayment
import com.android.pos.data.model.requestModel.GuestPaymentRequest
import com.android.pos.data.model.responseModel.GetOrderDetailsResponse
import kotlinx.parcelize.Parcelize

@Parcelize
data class CheckOutDineInDataModel(
    val guestId: Int? = null,
    val isFromGuest: Boolean,
    val isLastPayment: Boolean,
    val guestPaymentReq: GuestPaymentRequest?,
    val orderId: Int,
    val splitModel: DineInOrderPayment,
    val dineInAdapterList:List<DineInModel>?= listOf(),
    val dineInOrderDetails:GetOrderDetailsResponse.Data?=null,
    val guestPaymentModel:GuestDataModel? = null

) : Parcelable {

}