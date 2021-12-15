package com.android.pos.data.model

data class PrinterQueueModel(
    val id: Int,
    val offlineId: String,
    val orderId: String,
    val orderType: String,
    val totalAmt: Double,
    val paymentType: String,
    val terminalName: String,
    val status: String
)
