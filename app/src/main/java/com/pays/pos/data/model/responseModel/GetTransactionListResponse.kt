package com.pays.pos.data.model.responseModel


import com.google.gson.annotations.SerializedName

data class GetTransactionListResponse(
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
        @SerializedName("payments")
        val payments: List<Payment>,

        @SerializedName("pagination")
        val pagination: Pagination

    ) {
        data class Payment(
            @SerializedName("amount")
            val amount: Double,
            @SerializedName("card_name")
            val cardName: String,
            @SerializedName("card_number")
            val cardNumber: String,
            @SerializedName("card_type")
            val cardType: String,
            @SerializedName("cash_discount_or_surcharge")
            val cash_discount_or_surcharge: Double,
            @SerializedName("created_at")
            val createdAt: String,
            @SerializedName("customer")
            val customer: Customer,
            @SerializedName("employee_id")
            val employeeId: Int,
            @SerializedName("employee_name")
            val employeeName: String,
            @SerializedName("id")
            val id: Int,
            @SerializedName("offline_id")
            val offlineId: String,
            @SerializedName("order_details")
            val orderDetails: OrderDetails,
            @SerializedName("order_id")
            val orderId: Int,
            @SerializedName("custom_order_id")
            val custom_order_id: Int,
            @SerializedName("payable_id")
            val payableId: Int,
            @SerializedName("payable_type")
            val payableType: String,
            @SerializedName("gift_card_id")
            val giftCardId: Int,
            @SerializedName("payment_id")
            val paymentId: Any,
            @SerializedName("payment_type")
            val paymentType: String,
            @SerializedName("service_charge_amount")
            val serviceChargeAmount: Double,
            @SerializedName("tax_amount")
            val taxAmount: Double,
            @SerializedName("terminal_id")
            val terminalId: Int,
            @SerializedName("tips")
            var tips: Double,
            @SerializedName("total_amount")
            var totalAmount: Double,
            @SerializedName("transaction_id")
            val transactionId: String,
            @SerializedName("terminal_name")
            val terminalName: String,
            @SerializedName("employee_role_name")
            val employeeRoleName: String,
            @SerializedName("refunded_amount")
            val refundedAmount: Double,
            @SerializedName("magensa_response")
            val magensaResponse: String?,
            @SerializedName("global_uniq_id")
            var global_uniq_id: String = "",
            @SerializedName("ref_num")
            var ref_num: String = "",
            @SerializedName("ext_data")
            var ext_data: String = ""

        ) {
            data class Customer(
                @SerializedName("first_name")
                val firstName: String?,
                @SerializedName("last_name")
                val lastName: String?
            )

            data class OrderDetails(
                @SerializedName("delivery_type")
                val deliveryType: String,
                @SerializedName("id")
                val id: Int,
                @SerializedName("open_order_type")
                val openOrderType: Any,
                @SerializedName("open_order_type_name")
                val openOrderTypeName: Any,
                @SerializedName("order_type")
                val orderType: String,
                @SerializedName("order_type_name")
                val orderTypeName: String,
                @SerializedName("payment_status")
                val paymentStatus: String,
                @SerializedName("receipt_id")
                val receiptId: String,
                @SerializedName("short_receipt_url")
                val shortReceiptUrl: String,
                @SerializedName("refunded_quantity")
                val refundedQuantity: String,
                @SerializedName("refunded_amount")
                val refundedAmount: Double,
                @SerializedName("magensa_response")
                val magensaResponse: String?

            )
        }

        data class Pagination(
            @SerializedName("max_page_size")
            val maxPageSize: String,
            @SerializedName("per_page")
            val perPage: Int
        )
    }
}