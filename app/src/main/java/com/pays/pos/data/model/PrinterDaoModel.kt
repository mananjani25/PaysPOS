package com.pays.pos.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pays.pos.data.model.responseModel.PrinterResponse
import com.pays.pos.data.remote.Constants
import com.epson.epsonio.DeviceInfo
import kotlinx.parcelize.Parcelize
import java.util.*



data class PrinterDaoModel(
    @PrimaryKey
    val id: Int? = null,
    var printerName: String? = null,
    var connectionType: String? = null,
    var isActive: Boolean = false,
    var type: String = Constants.CUSTOMER,
    var dvmDeviceType: Int? = null,
    var dvmPrinterName: String? = null,
    var dvmDeviceName: String? = null,
    var dvmIpAddress: String? = null,
    var dvmMacAddress: String? = null,
    var uuid: UUID? = null,
    var printerModel: List<PrinterResponse.Data.OrderTypes>? = null
)
