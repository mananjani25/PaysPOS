package com.android.pos.data.model.requestModel


import com.google.gson.annotations.SerializedName

data class CreateServiceChargeRequestModel(
    @SerializedName("service_charge")
    var serviceCharge: ServiceCharge? = null
) {
    data class ServiceCharge(
        @SerializedName("is_enabled")
        var isEnabled: Boolean = false,
        @SerializedName("location_id")
        var locationId: Int = -1,
        @SerializedName("name")
        var name: String = "",
        @SerializedName("percentage")
        var percentage: Double = 0.0
    )
}