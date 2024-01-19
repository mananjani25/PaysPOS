package com.pays.pos.data.model.responseModel


import com.google.gson.annotations.SerializedName

open class BaseResponse {

    @SerializedName("message")
    val message: String = "" // You will receive an email with reset password instructions.

    @SerializedName("status")
    var status: Int = 0 // 200

    @SerializedName("type")
    val type: String = "" // Success
}

