package com.pays.pos.data.model.requestModel


import com.google.gson.annotations.SerializedName

data class CreateDiscountRequestModel(
    @SerializedName("discount")
    var discount: Discount? = null
) {
    data class Discount(
        @SerializedName("discount_type")
        var discountType: String = "",
        @SerializedName("name")
        var name: String = "",
        @SerializedName("percentage")
        var percentage: Double = 0.00,
        @SerializedName("location_id")
        var locationId: Int = -1,
    )
}