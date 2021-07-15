package com.android.pos.data.model.responseModel


import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class EmployeeResponse(
    @SerializedName("data")
    val `data`: List<Data>,
) : BaseResponse(), Parcelable {
    @Parcelize
    data class Data(
        @SerializedName("created_at")
        val createdAt: String,
        @SerializedName("email")
        val email: String,
        @SerializedName("first_name")
        val firstName: String?,
        @SerializedName("id")
        val id: Int,
        @SerializedName("is_active")
        val isActive: Boolean,
        @SerializedName("is_clocked_in")
        val isClockedIn: Boolean,
        @SerializedName("last_name")
        val lastName: String? = "",
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
    ) : Parcelable
}