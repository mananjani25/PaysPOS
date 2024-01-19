package com.pays.pos.data.model.responseModel

import com.google.gson.annotations.SerializedName

data class OnlineOrderStatusUpdateResponse(
    @SerializedName("data")
    val `data`: OnlineOrderResponseModel.Data,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("type")
    val type: String
)