package com.pays.pos.data.model

data class PrinterJSONElementData(
    var printerName: String,
    var macAddress: String,
    var ipAddress: String,
    var modelName:String,
    var portNo:Int,
    var printerQueueModelList: ArrayList<PrinterQueueModel>
)