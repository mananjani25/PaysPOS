package com.android.pos.data.model.responseModel


import com.android.pos.data.model.responseModel.report.KeyValue
import com.google.gson.annotations.SerializedName

data class EodReportResponse(
    @SerializedName("data")
    val `data`: Data,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("type")
    val type: String
) {
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
        val orderSalesDetails: OrderSalesDetails,
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
        @SerializedName("item_wise_sales")
        val itemWiseSales: ArrayList<ItemWiseSalesData>,
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
        val wastageDetails: ArrayList<ArrayList<KeyValue>>,
        @SerializedName("employee_guest_details")
        val employeeGuestDetails: ArrayList<ArrayList<KeyValue>>?,
        @SerializedName("sales_per_category_summary")
        val salesPerCategorySummary: ArrayList<ArrayList<KeyValue>>?

    ) {

        data class CreditCardBreakdown(
            @SerializedName("key")
            val key: String,
            @SerializedName("tips")
            val tips: Double,
            @SerializedName("value")
            val value: Double
        ) {

            fun showData() =

                when {

                    key.trim() == "Refund" -> {
                        showFormattedValueMinus()
                    }

                    else -> {
                        showFormattedValue()
                    }


                }

            fun showDataTip() =

                when {

                    key.trim() == "Refund" -> {
                        showFormattedValueMinusTip()
                    }

                    else -> {
                        showFormattedValueTips()
                    }


                }

            private fun showFormattedValueMinus() =
                if (value == 0.0 || value == 0.00) "$0.00" else "-$" + String.format(
                    "%.2f", value
                )


            private fun showFormattedValueMinusTip() =
                if (tips == 0.0 || tips == 0.00) "$0.00" else "-$" + String.format(
                    "%.2f", tips
                )

            fun showFormattedValue() = "$" + String.format(
                "%.2f", value ?: 0.0
            )

            fun showFormattedValueTips() = "$" + String.format(
                "%.2f", tips ?: 0.0
            )


        }

        data class ClockInClockOut(
            @SerializedName("key")
            val key: String,
            @SerializedName("value")
            val value: String
        )

        data class OrderSalesDetails(
            @SerializedName("data")
            var `data`: List<Details>,
            @SerializedName("Total")
            var total: Double
        ) {
            data class Details(
                @SerializedName("Amount")
                var amount: Double,
                @SerializedName("Order Id")
                var orderId: String,
                @SerializedName("Pay Type")
                var payType: String,
                @SerializedName("Service Charge")
                var serviceCharge: Double,
                @SerializedName("Tip")
                var tip: Double,
                @SerializedName("Terminal")
                var terminal: String
            )
        }

        data class ItemWiseSalesData(
            @SerializedName("item_name")
            var itemName: String,
            @SerializedName("quantity")
            var quantity: String,
            @SerializedName("amount")
            var amount: Double,
        )


    }
}