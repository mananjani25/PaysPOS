package com.pays.pos.data.model.responseModel


import com.pays.pos.data.entities.LoyaltyProgramsModel
import com.google.gson.annotations.SerializedName

data class CreateLoyaltyPointResponse(
    @SerializedName("data")
    val `data`: LoyaltyProgramsModel,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("type")
    val type: String
)