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
            @SerializedName("order_type_id")
            val orderTypeId: Int,
            @SerializedName("payment_status")
            val paymentStatus: String,
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
        )
    }
}