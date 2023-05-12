package com.android.pos.data.model

data class PrinterJSONElementData(
    var printerName: String,
    var macAddress: String,
    var ipAddress: String,
    var modelName:String,
    var printerQueueModelList: ArrayList<PrinterQueueModel>
)