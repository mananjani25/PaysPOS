package com.pays.pos.data.model.responseModel


import com.google.gson.annotations.SerializedName

data class ServiceChargeListResponse(
    @SerializedName("data")
    val data: Data,
) : BaseResponse() {
    data class Data(
        @SerializedName("service_charges") val serviceCharges: List<ServiceCharge>,
        @SerializedName("service_charge_enable") val serviceChargeEnable: Boolean,
        @SerializedName("enable_dine_in_service_charge") val enableDineInServiceCharge: Boolean
    ) {
        data class ServiceCharge(
            @SerializedName("id") val id: Int,
            @SerializedName("name") val name: String,
            @SerializedName("is_enabled") val isEnabled: Boolean,
            @SerializedName("percentage") val percentage: Double,
            @SerializedName("location_id") val locationId: Int,
            @SerializedName("created_at") val createdAt: String,
            @SerializedName("updated_at") val updatedAt: String,
            @SerializedName("min_guest_count") val minGuestCount: Int,
            @SerializedName("max_guest_count") val maxGuestCount: Int,
            @SerializedName("order_type") val orderType: String,
            @SerializedName("is_deleted")
            var isDeleted: Boolean = false
        )
    }
}