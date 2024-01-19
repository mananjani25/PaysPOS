package com.pays.pos.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class GuestDataModel(
    val subTotal: Double,
    val totalTax: Double,
    val totalAmount: Double,
    val totalDiscount: Double,
    val cashDiscount: Double,
    val totalServiceCharge: Double
) :Parcelable{

}