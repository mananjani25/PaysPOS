package com.android.pos.data.model.responseModel


import com.android.pos.data.entities.TaxData
import com.google.gson.annotations.SerializedName

data class CreateTaxResponse(
    @SerializedName("data")
    val `data`: TaxData,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("type")
    val type: String
)