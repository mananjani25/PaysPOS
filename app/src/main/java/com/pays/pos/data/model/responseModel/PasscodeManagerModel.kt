package com.pays.pos.data.model.responseModel


import com.google.gson.annotations.SerializedName

data class PasscodeManagerModel(
    @SerializedName("data") val `data`: Boolean,
    @SerializedName("type") val type: String,
    @SerializedName("status") val status: Int,
    @SerializedName("message") val message: String
)