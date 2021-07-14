package com.android.pos.data.model.responseModel


import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

data class EmployeeResponse(
    @SerializedName("data")
    val `data`: List<Data>,
) : BaseResponse() {
    data class Data(
        @SerializedName("created_at")
        val createdAt: String,
        @SerializedName("email")
        val email: String,
        @SerializedName("first_name")
        val firstName: String,
        @SerializedName("id")
        val id: Int,
        @SerializedName("is_active")
        val isActive: Boolean,
        @SerializedName("is_clocked_in")
        val isClockedIn: Boolean,
        @SerializedName("last_name")
        val lastName: String,
        @SerializedName("location_id")
        val locationId: Int,
        @SerializedName("loggedin_terminal_id")
        val loggedinTerminalId: Int,
        @SerializedName("name")
        val name: String,
        @SerializedName("passcode")
        val passcode: String,
        @SerializedName("phone_number")
        val phoneNumber: String,
        @SerializedName("updated_at")
        val updatedAt: String
    ) : Parcelable {
        constructor(parcel: Parcel) : this(
            parcel.readString().toString(),
            parcel.readString().toString(),
            parcel.readString().toString(),
            parcel.readInt(),
            parcel.readByte() != 0.toByte(),
            parcel.readByte() != 0.toByte(),
            parcel.readString().toString(),
            parcel.readInt(),
            parcel.readInt(),
            parcel.readString().toString(),
            parcel.readString().toString(),
            parcel.readString().toString(),
            parcel.readString().toString()
        ) {
        }

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(createdAt)
            parcel.writeString(email)
            parcel.writeString(firstName)
            parcel.writeInt(id)
            parcel.writeByte(if (isActive) 1 else 0)
            parcel.writeByte(if (isClockedIn) 1 else 0)
            parcel.writeString(lastName)
            parcel.writeInt(locationId)
            parcel.writeInt(loggedinTerminalId)
            parcel.writeString(name)
            parcel.writeString(passcode)
            parcel.writeString(phoneNumber)
            parcel.writeString(updatedAt)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<Data> {
            override fun createFromParcel(parcel: Parcel): Data {
                return Data(parcel)
            }

            override fun newArray(size: Int): Array<Data?> {
                return arrayOfNulls(size)
            }
        }
    }
}