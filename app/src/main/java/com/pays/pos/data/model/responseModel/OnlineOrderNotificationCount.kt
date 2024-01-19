package com.pays.pos.data.model.responseModel


import com.google.gson.annotations.SerializedName


data class OnlineOrderNotificationCount(
    @SerializedName("data")
    val `data`: Data,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("type")
    val type: String
) {
    data class Data(
        @SerializedName("count")
        val count: Int
    )
}
