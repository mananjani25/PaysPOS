package com.android.pos.data.model.responseModel


import com.android.pos.data.model.responseModel.report.KeyValue
import com.android.pos.data.model.responseModel.report.KeyValueWithString
import com.google.gson.annotations.SerializedName

data class EodReportResponse(
    @SerializedName("data")
    val `data`: Data
) : BaseResponse() {
    data class Data(
        @SerializedName("cash_log_details")
        val cashLogDetails: List<KeyValue>,
        @SerializedName("clock_in_clock_out")
        val clockInClockOut: ArrayList<ArrayList<ClockInClockOut>>,
        @SerializedName("credit_card_breakdown")
        val creditCardBreakdown: List<CreditCardBreakdown>,
        @SerializedName("credit_tip_audit")
        val creditTipAudit: ArrayList<ArrayList<KeyValue>>,
        @SerializedName("discount_details")
        val discountDetails: List<KeyValue>,
        @SerializedName("order_sales_details")
        val orderSalesDetails: ArrayList<ArrayList<KeyValueWithString>>,
        @SerializedName("other_details")
        val otherDetails: List<KeyValue>,
        @SerializedName("payment_details")
        val paymentDetails: ArrayList<ArrayList<KeyValue>>,
        @SerializedName("refund_and_void_details")
        val refundAndVoidDetails: ArrayList<ArrayList<KeyValue>>,
        @SerializedName("refund_details")
        val refundDetails: List<KeyValue>,
        @SerializedName("report_time")
        val reportTime: String,
        @SerializedName("sales_and_taxes_summary")
        val salesAndTaxesSummary: List<KeyValue>,
        @SerializedName("sales_summary")
        val salesSummary: List<KeyValue>,
        @SerializedName("service_charge_details")
        val serviceChargeDetails: ArrayList<ArrayList<KeyValue>>,
        @SerializedName("tax_details")
        val taxDetails: List<KeyValue>,
        @SerializedName("tip_details")
        val tipDetails: ArrayList<ArrayList<KeyValue>>,
        @SerializedName("total_cash_payments")
        val totalCashPayments: List<KeyValue>,
        @SerializedName("total_credit_payment_details")
        val totalCreditPaymentDetails: List<KeyValue>,
        @SerializedName("total_payments")
        val totalPayments: List<KeyValue>,
        @SerializedName("wastage_details")
        val wastageDetails: ArrayList<ArrayList<KeyValue>>
    ) {

        data class CreditCardBreakdown(
            @SerializedName("key")
            val key: String,
            @SerializedName("tips")
            val tips: Double,
            @SerializedName("value")
            val value: Double
        )

        data class ClockInClockOut(
            @SerializedName("key")
            val key: String,
            @SerializedName("value")
            val value: String
        )

        data class OrderSalesDetails(
            @SerializedName("key")
            val key: String,
            @SerializedName("value")
            val value: String
        )


    }
}