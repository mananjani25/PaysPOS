package com.pays.pos.data.model.responseModel

import com.google.gson.annotations.SerializedName

data class MergeTableResponse(
    @SerializedName("data") val data: Data,
    @SerializedName("type") val type: String,
    @SerializedName("status") val status: Int,
    @SerializedName("message") val message: String
) {

    class Data() {

    }
}