package com.pays.pos.data.model.responseModel

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
class GuestPaymentAttributes : Parcelable {
    @SerializedName("amount")
    var amount: Double = 0.0

    @SerializedName("card_name")
    var cardName: String = ""

    @SerializedName("card_number")
    var cardNumber: String = ""

    @SerializedName("card_type")
    var cardType: String = ""

    @SerializedName("cash_discount_or_surcharge")
    var cash_discount_or_surcharge: Double = 0.0

    @SerializedName("cash_discount_fee")
    var cashDiscountFee: Double = 0.0

    @SerializedName("cash_discount_type")
    var cash_discount_type: String = ""

    @SerializedName("total_cash_discount")
    var total_cash_discount: Double = 0.0


    @SerializedName("employee_id")
    var employeeId: Int = 0

    @SerializedName("id")
    var id: Int? = null

    @SerializedName("offline_id")
    var offlineId: String = ""

    @SerializedName("payable_type")
    var payableType: String = ""

    @SerializedName("payment_type")
    var paymentType: String = ""

    @SerializedName("service_charge_amount")
    var serviceChargeAmount: Double = 0.0

    @SerializedName("sub_total")
    var subTotal: Double = 0.0

    @SerializedName("tax_amount")
    var taxAmount: Double = 0.0

    @SerializedName("terminal_id")
    var terminalId: Int = 0

    @SerializedName("order_id")
    var order_id: Int? = null

    @SerializedName("tips")
    var tips: Double = 0.0

    @SerializedName("tips_adjusted")
    var tipsAdjusted: Boolean = false

    @SerializedName("total_discount")
    var totalDiscount: Double = 0.0

    @SerializedName("transaction_id")
    var transactionId: String = ""

    @SerializedName("payments_attributes")
    var paymentAttributes: List<GuestPaymentAttributes>? = null

    @SerializedName("is_paid")
    var isPaid: Boolean = true

    @SerializedName("magensa_response")
    var magensaResponse: String = ""

    @SerializedName("global_uniq_id")
    var global_uniq_id: String = ""

    @SerializedName("ext_data")
    var ext_data: String = ""

    @SerializedName("ecr_ref_num")
    var ecr_ref_num: String = ""

    @SerializedName("pax_transaction_token")
    var pax_transaction_token: String = ""

    @SerializedName("ref_num")
    var ref_num: String = ""

}