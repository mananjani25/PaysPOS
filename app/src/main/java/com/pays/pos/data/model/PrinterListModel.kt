package com.pays.pos.data.model

import android.os.Parcel
import android.os.Parcelable
import com.pays.pos.data.model.responseModel.PrinterResponse
import com.pays.pos.data.remote.Constants.CUSTOMER
import com.epson.epsonio.DeviceInfo
import java.util.UUID


data class PrinterListModel(
    val id: Int? = null,
    var printerName: String? = null,
    var connectionType: String? = null,
    var modelName:String?= null,
    var isActive: Boolean = false,
    var isCustomerActive: Boolean = false,
    var isKitchenActive: Boolean = false,
    var type: String = CUSTOMER,
    var deviceModel: DeviceInfo? = null,
    var uuid: UUID? = null,
    var printerModel: List<PrinterResponse.Data.OrderTypes>? = null,
    var currentPrinterType: String? = null,
    var printerCategories: List<PrinterResponse.Data.PrinterCategories>? = null,
    var portNo:Int?=null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readString()!!,
        TODO("deviceModel"),
        TODO("uuid"),
        TODO("printerModel"),
        TODO("printerCategories")

    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(id)
        parcel.writeString(printerName)
        parcel.writeString(connectionType)
        parcel.writeByte(if (isActive) 1 else 0)
        parcel.writeString(type)

    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<PrinterListModel> {
        override fun createFromParcel(parcel: Parcel): PrinterListModel {
            return PrinterListModel(parcel)
        }

        override fun newArray(size: Int): Array<PrinterListModel?> {
            return arrayOfNulls(size)
        }
    }

}
