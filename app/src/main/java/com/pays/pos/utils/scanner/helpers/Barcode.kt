package com.pays.pos.utils.scanner.helpers

import android.os.Parcel
import android.os.Parcelable
import com.pays.pos.data.model.PrinterListModel

/**
 * Created by qvfr34 on 9/19/2014.
 */
class Barcode : Parcelable {
    var barcodeData: ByteArray
    var barcodeType: Int
    var fromScannerID: Int

    constructor(barcodeData: ByteArray, barcodeType: Int, fromScannerID: Int) {
        this.barcodeData = barcodeData
        this.barcodeType = barcodeType
        this.fromScannerID = fromScannerID
    }

    constructor(`in`: Parcel) {
        barcodeData = `in`.readString()!!.toByteArray()
        barcodeType = `in`.readInt()
        fromScannerID = `in`.readInt()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeString(String(barcodeData))
        parcel.writeInt(barcodeType)
        parcel.writeInt(fromScannerID)
    }

    companion object CREATOR : Parcelable.Creator<Barcode> {
        override fun createFromParcel(source: Parcel): Barcode {
            return Barcode(source)
        }

        override fun newArray(size: Int): Array<Barcode?> {
            return arrayOfNulls(size)
        }
    }
}