package com.android.pos.data.entities

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "TbCategory")
class TbCategory() : Parcelable {


    var createdAt: String = ""

    @PrimaryKey
    var id: Int = 0
    var isHide: Boolean = false
    var name: String = ""
    var sort: Int = 0
    var updatedAt: String = ""
    var isSelect: Boolean = false

    constructor(parcel: Parcel) : this() {
        createdAt = parcel.readString().toString()
        id = parcel.readInt()
        isHide = parcel.readByte() != 0.toByte()
        name = parcel.readString().toString()
        sort = parcel.readInt()
        updatedAt = parcel.readString().toString()
        isSelect = parcel.readByte() != 0.toByte()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(createdAt)
        parcel.writeInt(id)
        parcel.writeByte(if (isHide) 1 else 0)
        parcel.writeString(name)
        parcel.writeInt(sort)
        parcel.writeString(updatedAt)
        parcel.writeByte(if (isSelect) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TbCategory> {
        override fun createFromParcel(parcel: Parcel): TbCategory {
            return TbCategory(parcel)
        }

        override fun newArray(size: Int): Array<TbCategory?> {
            return arrayOfNulls(size)
        }
    }


}