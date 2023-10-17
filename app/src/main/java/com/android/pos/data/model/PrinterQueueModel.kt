package com.android.pos.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.android.pos.data.model.responseModel.CreateOrderResponse
import com.android.pos.data.typeconvert.TCPrinterQueueSuucessModel

@TypeConverters(TCPrinterQueueSuucessModel::class)
@Entity(tableName = "PrinterQueue")
data class PrinterQueueModel(
    @PrimaryKey
    var id: Int? = null,
    var offlineId: String = "",
    var orderIdN: Int? = null,
    var orderType: String = "",
    var totalAmt: Double? = null,
    var paymentType: String = "",
    var orderID: String = "",
    var status: String = "",
    //PrinterQueueReponse.Data
    val printerQueueData: String = "",
    var orderItems: List<CreateOrderResponse.Data.Order.OrderItem> = emptyList(),
    var position: Int = 0,
    var customerName: String = "",
    var customerPhoneNo: String = "",
    var customerAddress: String = "",
    var printSuccessData: String = "",
    var employeeName: String = "",
    var dateAndTime: String = "",
    var guestAttributes: List<GuestAttrQueue> = emptyList(),
    var orderNote:String = "",
    var deliveryType:String = ""
) {
    data class PrinterReceivedSuccessModel(
        var printerId: Int,
        val queueId: Int,
        val isPrinted: Boolean = false
    )
}
