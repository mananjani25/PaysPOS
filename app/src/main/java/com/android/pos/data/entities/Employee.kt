package com.android.pos.data.entities

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Employee(
    @SerializedName("email")
    val email: String,
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("id")
    val id: Int,
    @SerializedName("is_active")
    val isActive: Boolean,
    @SerializedName("last_name")
    val lastName: String,
    @SerializedName("location_id")
    val locationId: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("passcode")
    val passcode: String,
    @SerializedName("phone_number")
    val phoneNumber: String
) : Parcelable