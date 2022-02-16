package com.android.pos.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize


@Parcelize
@Entity(tableName = "UserSwap")
class UserSwapModel(
    @PrimaryKey
    @SerializedName("employee_id")
    val employee_id: Int,
    @SerializedName("employee_name")
    val employee_name: String,
    @SerializedName("employee_role")
    val employee_role: String,
    @SerializedName("team_role_id")
    val team_role_id: Int,
    @SerializedName("date_time")
    val date_time: String,
    @SerializedName("passcode")
    val passcode: String,
) : Parcelable