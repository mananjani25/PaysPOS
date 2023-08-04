package com.android.pos.data.model.requestModel.giftCard.response

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class GiftCardAddValueResponse(
    @SerializedName("data")
    val `data`: Data? = null,
    val message: String,
    val status: Int,
    val type: String
) {
    data class Data(
        val gift_card: GiftCard
    ): Parcelable {
        data class GiftCard(
            val id: Int,
            val amount: String,
            val customer_id: Int,
            val location_id: Int,
            val name: String,
            val password: String,
            val payments: List<Payment>
        ) {
            data class Payment(
                val id: Int,
                val amount: Double,
                val card_name: String,
                val card_number: String,
                val cash_discount_fee: Double,
                val cash_discount_or_surcharge: Double,
                val cash_discount_type: String,
                val employee_id: Int,
                val is_loyalty_applied: Boolean,
                val loyalty_amount: Double,
                val loyalty_program_id: String,
                val magensa_response: String,
                val offline_id: String,
                val payable_type: String,
                val payment_type: String,
                val service_charge_amount: Double,
                val sub_total: Double,
                val tax_amount: Double,
                val terminal_id: Int,
                val tip_setting_id: Int,
                val tips: Double,
                val tips_adjusted: Boolean,
                val total_cash_discount: Double,
                val total_discount: Double,
                val transaction_id: String,
                val used_reward_points: Int
            )
        }

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel?, flags: Int) {

        }

        companion object CREATOR : Parcelable.Creator<Data> {
            override fun createFromParcel(parcel: Parcel): Data {
                return Data(TODO("Order"))
            }

            override fun newArray(size: Int): Array<Data?> {
                return arrayOfNulls(size)
            }
        }
    }
}