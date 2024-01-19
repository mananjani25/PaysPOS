package com.pays.pos.data.model

data class GuestPaymentCalculationModel(
    val subTotal: Double,
    val total: Double,
    val serviceCharge: Double,
    val tax: Double,
    val cashDiscount: Double,
    val totalDiscount: Double,
    val guestId:Int?=null,
    val model:DineinCartPaymentModel?=null,
    var wholeOrderPassDiscount:Double?=null,
    var wholeOrderPassSC:Double?=null
)