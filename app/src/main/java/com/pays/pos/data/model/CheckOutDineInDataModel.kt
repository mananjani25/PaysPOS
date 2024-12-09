package com.pays.pos.data.model

import android.os.Parcelable
import com.pays.pos.data.model.requestModel.DineInOrderPayment
import com.pays.pos.data.model.requestModel.GuestPaymentRequest
import com.pays.pos.data.model.requestModel.OrderServiceChargesAttribute
import com.pays.pos.data.model.responseModel.GetOrderDetailsResponse
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
    var dineInOrderDetails:GetOrderDetailsResponse.Data?=null,
    val guestPaymentModel:GuestDataModel? = null,
    val guestPosition:Int?=null,
    val servicChargeAppliedlist:ArrayList<OrderServiceChargesAttribute>?=null

) : Parcelable {

}