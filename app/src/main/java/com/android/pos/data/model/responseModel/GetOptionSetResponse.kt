package com.android.pos.data.model.responseModel


import com.android.pos.data.entities.OptionSet
import com.google.gson.annotations.SerializedName

data class GetOptionSetResponse(
    @SerializedName("data")
    val `data`: List<OptionSet>,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("type")
    val type: String
)