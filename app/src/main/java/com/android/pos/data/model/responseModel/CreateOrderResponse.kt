package com.android.pos.data.model.responseModel


import com.google.gson.annotations.SerializedName

data class CreateOrderResponse(
    @SerializedName("data")
    val `data`: Data
) : BaseResponse() {
    data class Data(
        @SerializedName("order")
        val order: Order
    ) {
        data class Order(
            @SerializedName("created_at")
            val createdAt: String,
            @SerializedName("creation_time_on_terminal")
            val creationTimeOnTerminal: Any,
            @SerializedName("customer")
            val customer: Any,
            @SerializedName("customer_id")
            val customerId: Any,
            @SerializedName("date")
            val date: String,
            @SerializedName("delivery_employee_id")
            val deliveryEmployeeId: Any,
            @SerializedName("delivery_type")
            val deliveryType: String,
            @SerializedName("discount_type_id")
            val discountTypeId: Any,
            @SerializedName("dynamic_discount_id")
            val dynamicDiscountId: Any,
            @SerializedName("edit_order_count")
            val editOrderCount: Any,
            @SerializedName("edited_order_timestamp")
            val editedOrderTimestamp: Any,
            @SerializedName("employee")
            val employee: Any,
            @SerializedName("employee_id")
            val employeeId: Int,
            @SerializedName("future_delivery_date")
            val futureDeliveryDate: Any,
            @SerializedName("future_delivery_time")
            val futureDeliveryTime: String,
            @SerializedName("id")
            val id: Int,
            @SerializedName("index_of_date")
            val indexOfDate: Any,
            @SerializedName("integer")
            val integer: Any,
            @SerializedName("is_edited")
            val isEdited: Boolean,
            @SerializedName("location_id")
            val locationId: Int,
            @SerializedName("note")
            val note: String,
            @SerializedName("offline_id")
            val offlineId: String,
            @SerializedName("open_order_type")
            val openOrderType: Any,
            @SerializedName("open_order_type_id")
            val openOrderTypeId: Any,
            @SerializedName("order_items")
            val orderItems: List<OrderItem>,
            @SerializedName("order_service_charges")
            val orderServiceCharges: List<OrderServiceCharge>,
            @SerializedName("order_type")
            val orderType: String,
            @SerializedName("order_type_id")
            val orderTypeId: Int,
            @SerializedName("payment_status")
            val paymentStatus: String,
            @SerializedName("payments")
            val payments: List<Payment>,
            @SerializedName("service_charge_enabled")
            val serviceChargeEnabled: Boolean,
            @SerializedName("sub_total")
            val subTotal: Double,
            @SerializedName("tax_enabled")
            val taxEnabled: Boolean,
            @SerializedName("terminal_id")
            val terminalId: Int,
            @SerializedName("total_amount")
            val totalAmount: Double,
            @SerializedName("total_cash_discount_fee")
            val totalCashDiscountFee: Double,
            @SerializedName("total_discount")
            val totalDiscount: Double,
            @SerializedName("total_service_charges")
            val totalServiceCharges: Double,
            @SerializedName("total_tax_amount")
            val totalTaxAmount: Double,
            @SerializedName("total_tips")
            val totalTips: Double,
            @SerializedName("updated_at")
            val updatedAt: String
        ) {
            data class OrderItem(
                @SerializedName("category_id")
                val categoryId: Int,
                @SerializedName("completed_in_kitchen")
                val completedInKitchen: Boolean,
                @SerializedName("discount_amount")
                val discountAmount: Double,
                @SerializedName("discount_id")
                val discountId: Any,
                @SerializedName("discount_type")
                val discountType: String,
                @SerializedName("employee_id")
                val employeeId: Int,
                @SerializedName("float")
                val float: Double,
                @SerializedName("id")
                val id: Int,
                @SerializedName("is_paid")
                val isPaid: Boolean,
                @SerializedName("is_printed")
                val isPrinted: Boolean,
                @SerializedName("item_id")
                val itemId: Int,
                @SerializedName("item_name")
                val itemName: String,
                @SerializedName("note")
                val note: String,
                @SerializedName("order_id")
                val orderId: Int,
                @SerializedName("order_item_modifiers")
                val orderItemModifiers: List<Any>,
                @SerializedName("price")
                val price: Double,
                @SerializedName("quantity")
                val quantity: Int,
                @SerializedName("timestamp")
                val timestamp: String,
                @SerializedName("total_price")
                val totalPrice: Double
            )

            data class OrderServiceCharge(
                @SerializedName("amount")
                val amount: Double,
                @SerializedName("created_at")
                val createdAt: String,
                @SerializedName("id")
                val id: Int,
                @SerializedName("name")
                val name: String,
                @SerializedName("order_id")
                val orderId: Int,
                @SerializedName("rate")
                val rate: Double,
                @SerializedName("service_charge_id")
                val serviceChargeId: Int,
                @SerializedName("updated_at")
                val updatedAt: String
            )

            data class Payment(
                @SerializedName("amount")
                val amount: Double,
                @SerializedName("card_name")
                val cardName: String,
                @SerializedName("card_number")
                val cardNumber: String,
                @SerializedName("card_type")
                val cardType: Int,
                @SerializedName("cash_discount")
                val cashDiscount: Double,
                @SerializedName("created_at")
                val createdAt: String,
                @SerializedName("employee_id")
                val employeeId: Int,
                @SerializedName("id")
                val id: Int,
                @SerializedName("offline_id")
                val offlineId: String,
                @SerializedName("order_id")
                val orderId: Int,
                @SerializedName("payable_id")
                val payableId: Int,
                @SerializedName("payable_type")
                val payableType: String,
                @SerializedName("payment_type")
                val paymentType: String,
                @SerializedName("service_charge_amount")
                val serviceChargeAmount: Double,
                @SerializedName("sub_total")
                val subTotal: Double,
                @SerializedName("tax_amount")
                val taxAmount: Double,
                @SerializedName("terminal_id")
                val terminalId: Int,
                @SerializedName("tips")
                val tips: Double,
                @SerializedName("tips_adjusted")
                val tipsAdjusted: Boolean,
                @SerializedName("total_discount")
                val totalDiscount: Double,
                @SerializedName("transaction_id")
                val transactionId: String,
                @SerializedName("updated_at")
                val updatedAt: String
            )
        }
    }
}