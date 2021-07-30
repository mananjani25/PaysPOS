package com.android.pos.data.model.responseModel


import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class EmployeeListResponse(
    @SerializedName("data")
    val `data`: Data,
) : BaseResponse(), Parcelable {
    @Parcelize
    data class Data(
        @SerializedName("employees")
        val employees: List<Employee>
    ) : Parcelable {
        @Parcelize
        @Entity(tableName = "TbEmployee")
        data class Employee(
            @SerializedName("created_at")
            val createdAt: String,
            @SerializedName("email")
            val email: String,
            @SerializedName("first_name")
            val firstName: String?,
            @PrimaryKey
            @SerializedName("id")
            val id: Int,
            @SerializedName("is_active")
            val isActive: Boolean,
            @SerializedName("is_clocked_in")
            val isClockedIn: Boolean,
            @SerializedName("last_name")
            val lastName: String?,
            @SerializedName("location_id")
            val locationId: Int,
            @SerializedName("loggedin_terminal_id")
            val loggedinTerminalId: Int,
            @SerializedName("name")
            val name: String?,
            @SerializedName("passcode")
            val passcode: String,
            @SerializedName("phone_number")
            val phoneNumber: String?,
            @SerializedName("updated_at")
            val updatedAt: String
        ) : Parcelable
    }
}