package com.android.pos.data.model

data class GuestPaymentCalculationModel(
    val subTotal: Double,
    val total: Double,
    val serviceCharge: Double,
    val tax: Double,
    val cashDiscount: Double,
    val totalDiscount: Double,
    val guestId:Int?=null,
    val model:DineinCartPaymentModel?=null
)