package com.android.pos.data.model.requestModel.giftCard.request

data class GiftCardAddValueRequest(
    val gift_card: GiftCard,
    val gift_card_amount_tab: GiftCardAmountTab
) {
    data class GiftCard(
        val added_amount: Double,
        val name: String,
        val gift_card_type: String
    )

    data class GiftCardAmountTab(
        val payment_attributes: PaymentAttributes?
    ) {
        data class PaymentAttributes(
            val amount: Double? = 0.0,
            val card_name: String? = "",
            val card_number: String? = "",
            val card_type: Int? = 0,
            val cash_discount_fee: Double? = 0.0,
            val cash_discount_or_surcharge: Int? = 0,
            val cash_discount_type: String? = "",
            val employee_id: Int? = 0,
            val is_loyalty_applied: Boolean? = false,
            val loyalty_amount: Double? = 0.0,
            val loyalty_program_id: String? = "",
            val magensa_response: String? = "",
            val offline_id: String? = "",
            val payable_type: String? = "",
            val payment_type: String? = "",
            val service_charge_amount: Int? = 0,
            val sub_total: Double? = 0.0,
            val tax_amount: Int? = 0,
            val terminal_id: Int? = 0,
            val tip_setting_id: Int? = 0,
            val tips: Double? = 0.0,
            val tips_adjusted: Boolean? = false,
            val total_cash_discount: Int? = 0,
            val total_discount: Double? = 0.0,
            val transaction_id: String? = "",
            val used_reward_points: Int? = 0
        )
    }
}