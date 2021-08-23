package com.android.pos.data.model

import com.android.pos.data.remote.Constants.CUSTOMER

data class PrinterListModel(
    val id: Int? = null,
    var printerName: String? = null,
    var connectionType: String? =null,
    val isActive: Boolean = false,
    val type: String = CUSTOMER
)
