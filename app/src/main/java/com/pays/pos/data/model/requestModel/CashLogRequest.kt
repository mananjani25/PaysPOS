package com.pays.pos.data.model.requestModel


import com.google.gson.annotations.SerializedName

data class CashLogRequest(
    @SerializedName("amount")
    val amount: Double,
    @SerializedName("employee_id")
    val employeeId: Int,
    @SerializedName("event")
    val event: String,
    @SerializedName("order_id")
    val orderId: String,
    @SerializedName("payment_id")
    val paymentId: Int,
    @SerializedName("reason")
    val reason: String,
    @SerializedName("terminal_id")
    val terminalId: Int,
    @SerializedName("tip_setting_id")
    val tipSettingId: Int?,
    @SerializedName("total_tips")
    val totalTips: Double?
)