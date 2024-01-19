package com.pays.pos.data.model

data class EditPrinterModel (
    val id: Int? = null,
    val printerName: String,
    var isCustomerManual: Boolean = false,
    var isKitchenManual: Boolean = false,
    var isCustomerAuto: Boolean = false,
    var isKitchenAuto: Boolean = false
)