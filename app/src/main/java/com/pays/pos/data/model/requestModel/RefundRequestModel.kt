package com.pays.pos.data.model.requestModel


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class RefundRequestModel(
    @SerializedName("payment_refund")
    var paymentRefund: PaymentRefund? = null
) : Parcelable {
    @Parcelize
    data class PaymentRefund(
        @SerializedName("amount")
        var amount: Double = 0.0,
        @SerializedName("employee_id")
        var employeeId: Int? = null,
        @SerializedName("id")
        var id: Int? = null,
        @SerializedName("order_id")
        var orderId: Int? = null,
        @SerializedName("order_item_refunds_attributes")
        var orderItemRefundsAttributes: List<OrderItemRefundsAttribute>? = null,
        @SerializedName("payment_id")
        var paymentId: Int? = null,
        @SerializedName("reason_for_refund")
        var reasonForRefund: String = "",
        @SerializedName("service_charge_refunded")
        var serviceChargeRefunded: Double = 0.0,
        @SerializedName("subtotal_refunded")
        var subtotal_refunded: Double = 0.0,
        @SerializedName("tax_refunded")
        var taxRefunded: Double = 0.0,
        @SerializedName("terminal_id")
        var terminalId: Int? = null,
        @SerializedName("tips_refunded")
        var tipsRefunded: Double = 0.0,
        @SerializedName("cash_discount_or_surcharge_refunded")
        var cash_discount_or_surcharge_refunded: Double = 0.0,

    ) : Parcelable {
        @Parcelize
        data class OrderItemRefundsAttribute(
            @SerializedName("amount")
            var amount: Double = 0.0,
            @SerializedName("employee_id")
            var employeeId: Int? = null,
            @SerializedName("id")
            var id: Int? = null,
            @SerializedName("order_id")
            var orderId: Int? = null,
            @SerializedName("order_item_id")
            var orderItemId: Int? = null,
            @SerializedName("payment_id")
            var paymentId: Int? = null,
            @SerializedName("payment_refund_id")
            var paymentRefundId: Int? = null,
            @SerializedName("quantity")
            var quantity: Int? = null,
            @SerializedName("refund_type")
            var refundType: Int? = null,
            @SerializedName("wastage_id")
            var wastageId: Int? = null,
            @SerializedName("refunded_amount")
        var refunded_amount: Double = 0.0
        ) : Parcelable
    }
}