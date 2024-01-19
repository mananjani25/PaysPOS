package com.pays.pos.data.model

import android.os.Parcel
import android.os.Parcelable
import com.pays.pos.data.entities.CartModel
import com.pays.pos.data.model.requestModel.DineInOrderPayment
import com.pays.pos.data.model.requestModel.GuestPaymentRequest
import com.pays.pos.data.model.responseModel.GetFloorPlanResponse
import com.pays.pos.data.model.responseModel.GetOrderDetailsResponse
import kotlinx.parcelize.Parcelize

@Parcelize
data class DineinCartPaymentModel(
    var isTotalPayment: Boolean? = false,
    var isLastPayment: Boolean? = false,
    var totalPrice: Double = 0.0,
    var subTotalPrice: Double = 0.0,
    var isGuestPay: Boolean = false,
    var splitModel: DineInOrderPayment? = null,
    var totalServiceCharge: Double = 0.0,
    var divideCashDiscount: Double = 0.0,
    var totalDiscount: Double = 0.0,
    var totalTax: Double = 0.0,
    var getOrderDetailsResponse: GetOrderDetailsResponse.Data? = null,
    var dineInAdapterList: ArrayList<DineInModel>? = null,
    var guestRequestModel: GuestPaymentRequest? = null,
    var totalGuestCount: Int = 0,
    var cartList: CartModel? = null,
    var guestId: Int? = null,
    var paidGuestCount: Int = 0,
    var guestSelectedPos: Int = 0,
    var floorPlanModel: GetFloorPlanResponse.Data.FloorPlanTable? = null
):Parcelable {
}