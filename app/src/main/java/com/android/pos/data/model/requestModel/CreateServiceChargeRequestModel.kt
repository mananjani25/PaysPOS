package com.android.pos.data.model.requestModel


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

data class CreateServiceChargeRequestModel(
    @SerializedName("service_charge")
    var serviceCharge: ServiceCharge? = null
) {
    @Parcelize
    data class ServiceCharge(
        @SerializedName("is_enabled")
        var isEnabled: Boolean = false,
        @SerializedName("location_id")
        var locationId: Int = -1,
        @SerializedName("name")
        var name: String = "",
        @SerializedName("percentage")
        var percentage: Double = 0.0
    ):Parcelable
}