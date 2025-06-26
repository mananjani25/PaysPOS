package com.pays.pos.data.model.responseModel


import com.google.gson.annotations.SerializedName

data class CashLogResponse(
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
        @SerializedName("cashes")
        val cashes: List<Cashe>,
        @SerializedName("total_cash_in")
        val totalCashIn: Double,
        @SerializedName("total_cash_out")
        val totalCashOut: Double,
        @SerializedName("total_left_amount_in_drawer")
        val totalLeftAmountInDrawer: Double,
        @SerializedName("pagination")
        val pagination: GetTransactionListResponse.Data.Pagination
    ) {
        data class Cashe(
            @SerializedName("amount")
            val amount: Double,
            @SerializedName("created_at")
            val createdAt: String,
            @SerializedName("employee_id")
            val employeeId: Int,
            @SerializedName("employee_name")
            val employeeName: String,
            @SerializedName("event")
            val event: String,
            @SerializedName("id")
            val id: Int,
            @SerializedName("order_id")
            val orderId: Int,
            @SerializedName("custom_order_id")
            val custom_order_id: Int,
            @SerializedName("order_type_name")
            private val _orderTypeName: String?,
            @SerializedName("payment_id")
            val paymentId: Int,
            @SerializedName("reason")
            val reason: String,
            @SerializedName("terminal_id")
            val terminalId: Int,
            @SerializedName("terminal_name")
            val terminalName: String,
            @SerializedName("tip_setting_id")
            val tipSettingId: Any,
            @SerializedName("total_tips")
            val totalTips: Any
        ) {
            val orderTypeName: String
                get() = _orderTypeName ?: "-"
        }
    }
}