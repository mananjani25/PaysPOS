package com.pays.pos.data.model.requestModel

import com.google.gson.annotations.SerializedName

data class CreateEmployeeRequestModel(
    @SerializedName("id")
    var id: Int? = null,
    @SerializedName("first_name")
    var firstName: String? = "",
    @SerializedName("last_name")
    var lastName: String? = "",
    @SerializedName("phone_number")
    var phoneNumber: String = "",
    @SerializedName("email")
    var email: String = "",
    @SerializedName("location_id")
    var locationId: Int = 0,
    @SerializedName("phone_country")
    var phone_country: Int?=null,
    @SerializedName("passcode")
    var passcode: String = "",
    @SerializedName("is_active")
    var isActive: Boolean = false,
    @SerializedName("hourly_wages")
    var hourly_wages: Double = 0.0,
    @SerializedName("team_role_id")
    var team_role_id: Int? = null
)