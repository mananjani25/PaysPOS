package com.android.pos.data.model

import com.android.pos.data.model.responseModel.CreateOrderResponse
import com.android.pos.data.model.responseModel.PrinterQueueReponse

data class PrinterQueueModel(
    var id: Int? = null,
    var offlineId: String = "",
    var orderId: Int? = null,
    var orderType: String = "",
    var totalAmt: Double? = null,
    var paymentType: String = "",
    var terminalName: String = "",
    var status: String = "",
    val data: PrinterQueueReponse.Data? = null,
    var orderItems: ArrayList<CreateOrderResponse.Data.Order.OrderItem>? = null
)
