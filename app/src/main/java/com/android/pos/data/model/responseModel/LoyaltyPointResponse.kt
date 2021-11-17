package com.android.pos.data.model.responseModel


import com.android.pos.data.entities.LoyaltyProgramsModel
import com.google.gson.annotations.SerializedName

data class LoyaltyPointResponse(
    @SerializedName("data")
    val `data`: List<LoyaltyProgramsModel>,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("type")
    val type: String
)