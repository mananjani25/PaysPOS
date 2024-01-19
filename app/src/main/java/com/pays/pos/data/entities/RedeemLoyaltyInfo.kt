package com.pays.pos.data.entities

import android.os.Parcelable
import android.util.Log
import kotlinx.parcelize.Parcelize

@Parcelize
class RedeemLoyaltyInfo() : Parcelable {
    var loyaltyProgramsModel: LoyaltyProgramsModel? = null
    var total: Double = 0.0
    var usedLoyaltyPoints: Int = 0
    var remainingLoyaltyPoints: Int = 0
    var remainingAmount: Double = 0.0
    var usedLoyaltyAmount: Double = 0.0
    var isLoyaltyApplied: Boolean? = false
    var amountToBePaid: Double = 0.0
    var needToApplyLoyalty: Boolean = false
    var availablePoints: Int = 0

    fun getAmountToBePaid(): Double? {
        return if (needToApplyLoyalty) {
            remainingAmount
        } else {
            total
        }
    }

    fun showFormattedValue(value: Double) = "$" + String.format(
        "%.2f",
        value
    )
}