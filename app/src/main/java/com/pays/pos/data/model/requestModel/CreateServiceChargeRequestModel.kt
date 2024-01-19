package com.pays.pos.data.model.requestModel


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
        var percentage: Double = 0.00,
        @SerializedName("min_guest_count")
        var min_guest_count: Int? = null,
        @SerializedName("max_guest_count")
        var max_guest_count: Int? = null,
        @SerializedName("order_type")
        var order_type: String = ""

    ) : Parcelable
}