package com.android.pos.data.entities

class RedeemLoyaltyInfo() {
    var loyaltyProgramsModel: LoyaltyProgramsModel? = null
    var total : Double = 0.0
    var usedLoyaltyPoints: Int = 0
    var remainingLoyaltyPoints: Int = 0
    var remainingLoyaltyAmount: Double = 0.0
    var usedLoyaltyAmount: Double = 0.0

    fun showFormattedValue(value : Double) = "$" + String.format(
        "%.2f",
        value
    )
}