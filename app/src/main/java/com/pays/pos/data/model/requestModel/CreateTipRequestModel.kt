package com.pays.pos.data.model.requestModel


import com.google.gson.annotations.SerializedName

data class CreateTipRequestModel(
    @SerializedName("id")
    var id: Int? = null,
    @SerializedName("is_active")
    var isActive: Boolean = false,
    @SerializedName("location_id")
    var locationId: Int = -1,
    @SerializedName("name")
    var name: String = "",
    @SerializedName("rate")
    var rate: Double = 0.0
)