package com.android.pos.data.model

import com.android.pos.data.remote.Constants.CUSTOMER
import com.epson.epsonio.DeviceInfo
import java.util.*

data class PrinterListModel(
    val id: Int? = null,
    var printerName: String? = null,
    var connectionType: String? = null,
    var isActive: Boolean = false,
    var type: String = CUSTOMER,
    var deviceModel: DeviceInfo,
    var uuid: UUID? = null
)
