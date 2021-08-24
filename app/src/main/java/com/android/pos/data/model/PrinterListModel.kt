package com.android.pos.data.model

import com.android.pos.data.remote.Constants.CUSTOMER
import com.epson.epsonio.DeviceInfo

data class PrinterListModel(
    val id: Int? = null,
    var printerName: String? = null,
    var connectionType: String? =null,
    var isActive: Boolean = false,
    val type: String = CUSTOMER,
    var deviceModel: DeviceInfo
)
