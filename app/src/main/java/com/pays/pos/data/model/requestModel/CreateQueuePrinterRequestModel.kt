package com.pays.pos.data.model.requestModel

import com.google.gson.annotations.SerializedName

data class CreateQueuePrinterRequestModel(
    @SerializedName("location_id") val location_id: Int,
    @SerializedName("order_type") val order_type: String,
    @SerializedName("printer_id") val printer_id: List<Int>,
    //@SerializedName("printer_list") val printer_list: List<Int>,
    @SerializedName("order_item_attributes") val order_item_attributes: List<OrderItemsAttribute>,
    @SerializedName("order_data") val order_data: OrderAttributeRequestModel,
    //@SerializedName("printer_failed_ids") val printer_failed_ids: List<Int>,
    @SerializedName("terminal_id") val terminal_id: Int
)
