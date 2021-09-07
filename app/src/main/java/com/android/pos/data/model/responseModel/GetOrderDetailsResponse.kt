package com.android.pos.data.model.responseModel

import com.google.gson.annotations.SerializedName


data class GetOrderDetailsResponse(
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
        @SerializedName("created_at")
        val createdAt: String,
        @SerializedName("creation_time_on_terminal")
        val creationTimeOnTerminal: Any,
        @SerializedName("customer")
        val customer: Customer,
        @SerializedName("customer_id")
        val customerId: Int,
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
        val employee: Employee,
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
        val updatedAt: String,
        @SerializedName("order_type")
        val orderType: String
    ) {
        data class Customer(
            @SerializedName("birth_date")
            val birthDate: String,
            @SerializedName("company")
            val company: String,
            @SerializedName("created_at")
            val createdAt: String,
            @SerializedName("email")
            val email: String,
            @SerializedName("first_name")
            val firstName: String,
            @SerializedName("id")
            val id: Int,
            @SerializedName("last_name")
            val lastName: String,
            @SerializedName("location_id")
            val locationId: Int,
            @SerializedName("note")
            val note: Any,
            @SerializedName("updated_at")
            val updatedAt: String
        )

        data class Employee(
            @SerializedName("created_at")
            val createdAt: String,
            @SerializedName("email")
            val email: String,
            @SerializedName("first_name")
            val firstName: String,
            @SerializedName("hourly_wages")
            val hourlyWages: Double,
            @SerializedName("id")
            val id: Int,
            @SerializedName("is_active")
            val isActive: Boolean,
            @SerializedName("is_clocked_in")
            val isClockedIn: Boolean,
            @SerializedName("last_name")
            val lastName: String,
            @SerializedName("location_id")
            val locationId: Int,
            @SerializedName("loggedin_terminal_id")
            val loggedinTerminalId: Int,
            @SerializedName("name")
            val name: String,
            @SerializedName("passcode")
            val passcode: String,
            @SerializedName("phone_number")
            val phoneNumber: String,
            @SerializedName("team_role_id")
            val teamRoleId: Int,
            @SerializedName("updated_at")
            val updatedAt: String
        )

        data class OrderItem(
            @SerializedName("category_id")
            val categoryId: Int,
            @SerializedName("completed_in_kitchen")
            val completedInKitchen: Boolean,
            @SerializedName("created_at")
            val createdAt: String,
            @SerializedName("discount_amount")
            val discountAmount: Double,
            @SerializedName("discount_id")
            val discountId: Any,
            @SerializedName("discount_type")
            val discountType: String,
            @SerializedName("edit_count")
            val editCount: Any,
            @SerializedName("edit_timestamp")
            val editTimestamp: Any,
            @SerializedName("employee_id")
            val employeeId: Int,
            @SerializedName("float")
            val float: Double,
            @SerializedName("id")
            val id: Int,
            @SerializedName("is_edited")
            val isEdited: Boolean,
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
            @SerializedName("price")
            val price: Double,
            @SerializedName("quantity")
            val quantity: Int,
            @SerializedName("timestamp")
            val timestamp: String,
            @SerializedName("total_price")
            val totalPrice: Double,
            @SerializedName("updated_at")
            val updatedAt: String
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
            val cardType: String,
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
            val orderId: Any,
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