package com.android.pos.data.model


import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "EodShiftReport")
data class ShiftRportConfiguration(
    @PrimaryKey
    @SerializedName("id") val id: Int,
    @SerializedName("employee_account") val employeeAccount: Boolean,
    @SerializedName("tip_and_fees_earned") val tipAndFeesEarned: Boolean,
    @SerializedName("employee_guest_report") val employeeGuestReport: Boolean,
    @SerializedName("credit_tip_audit") val creditTipAudit: Boolean,
    @SerializedName("sales_and_tax_summary") val salesAndTaxSummary: Boolean,
    @SerializedName("cash_credit_per_sales_category_summary") val cashCreditPerSalesCategorySummary: Boolean,
    @SerializedName("revenue_centers") val revenueCenters: Boolean,
    @SerializedName("order_sales_details") val orderSalesDetails: Boolean,
    @SerializedName("sales_summary") val salesSummary: Boolean,
    @SerializedName("payment_details") val paymentDetails: Boolean,
    @SerializedName("tips_details") val tipsDetails: Boolean,
    @SerializedName("tax_details") val taxDetails: Boolean,
    @SerializedName("refund_or_voids") val refundOrVoids: Boolean,
    @SerializedName("refund_details") val refundDetails: Boolean,
    @SerializedName("discount_details") val discountDetails: Boolean,
    @SerializedName("total_credit_payments") val totalCreditPayments: Boolean,
    @SerializedName("total_cash_payments") val totalCashPayments: Boolean,
    @SerializedName("total_payments") val totalPayments: Boolean,
    @SerializedName("credit_card_breakdown") val creditCardBreakdown: Boolean,
    @SerializedName("service_charge_details") val serviceChargeDetails: Boolean,
    @SerializedName("cash_log_details") val cashLogDetails: Boolean,
    @SerializedName("other_details") val otherDetails: Boolean,
    @SerializedName("clock_in_out") val clockInOut: Boolean,
    @SerializedName("location_id") val locationId: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)