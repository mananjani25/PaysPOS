package com.pays.pos.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "TbEmployee")
data class Employee(
    @SerializedName("email")
    val email: String?,
    @SerializedName("first_name")
    val firstName: String?,
    @PrimaryKey
    @SerializedName("id")
    val id: Int,
    @SerializedName("is_active")
    val isActive: Boolean,
    @SerializedName("last_name")
    val lastName: String?,
    @SerializedName("phone_country")
    val phone_country: String?,
    @SerializedName("location_id")
    val locationId: Int,
    @SerializedName("name")
    val name: String?,
    @SerializedName("passcode")
    val passcode: String?,
    @SerializedName("phone_number")
    val phoneNumber: String?,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("is_clocked_in")
    val isClockedIn: Boolean,
    @SerializedName("loggedin_terminal_id")
    val loggedinTerminalId: Int,
    @SerializedName("updated_at")
    val updatedAt: String?,
    @SerializedName("team_role_id")
    val teamRoleId: Int?,
    @SerializedName("hourly_wages")
    val hourlyWages: Double = 0.0,
    var isChecked: Boolean = false,
    @SerializedName("is_deleted")
    var isDeleted: Boolean = false
) : Parcelable