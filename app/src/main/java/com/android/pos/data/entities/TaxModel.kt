package com.android.pos.data.entities

import android.os.Parcel
import android.os.Parcelable
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.android.pos.data.typeconvert.TypeConvertersTax


@TypeConverters(TypeConvertersTax::class)
data class TaxModel(
    var createdAt: String? = "",
    var flateRate: String? = "",
    @PrimaryKey
    var id: Int = 0,
    var inventoryIds: String? = "",
    var isDefault: Boolean = false,
    var name: String? = "",
    var rate: String? = "",
    var taxType: String? = "",
    var updatedAt: String? = "",
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readInt(),
        parcel.readString(),
        parcel.readByte() != 0.toByte(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(createdAt)
        parcel.writeString(flateRate)
        parcel.writeInt(id)
        parcel.writeString(inventoryIds)
        parcel.writeByte(if (isDefault) 1 else 0)
        parcel.writeString(name)
        parcel.writeString(rate)
        parcel.writeString(taxType)
        parcel.writeString(updatedAt)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TaxModel> {
        override fun createFromParcel(parcel: Parcel): TaxModel {
            return TaxModel(parcel)
        }

        override fun newArray(size: Int): Array<TaxModel?> {
            return arrayOfNulls(size)
        }
    }
}
