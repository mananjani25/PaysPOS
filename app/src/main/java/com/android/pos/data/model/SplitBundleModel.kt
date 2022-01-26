package com.android.pos.data.model

import com.android.pos.data.entities.CartModel
import com.android.pos.data.model.responseModel.GetOrderDetailsResponse

data class SplitBundleModel(
    var isNextPayment: Boolean,
    var splitValue: Int,
    var orderId: Int,
    var paymentAmount: Double,
    var remainingAmt: Double,
    var isSplitByNo: Boolean,
    var isSplitByAmount: Boolean,
    var isCustomCash: Boolean,
    var getDineInOrderDetails: GetOrderDetailsResponse.Data?=null,
    var dineInList: ArrayList<DineInModel>,
    var subTotalWT: Double,
    var serviceCharge: Double,
    var totalDiscount: Double,
    var totalTaxAmount: Double,
    var tip : Double,
    var cashDiscountSurcharge:Double,
    var cartlist: List<CartModel>
)
