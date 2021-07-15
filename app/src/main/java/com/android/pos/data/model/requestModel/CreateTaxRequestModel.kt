package com.android.pos.data.model.requestModel


import com.google.gson.annotations.SerializedName

data class CreateTaxRequestModel(
    @SerializedName("id")
    var id: Int? = null,
    @SerializedName("location_id")
    var locationId: Int=-1,
    @SerializedName("name")
    var name: String="",
    @SerializedName("rate")
    var rate: Double=0.0,
    @SerializedName("tax_type")
    var taxType: String=""
)