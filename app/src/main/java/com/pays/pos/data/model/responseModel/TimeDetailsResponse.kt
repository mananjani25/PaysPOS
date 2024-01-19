package com.pays.pos.data.model.responseModel


import com.google.gson.annotations.SerializedName

class TimeDetailsResponse(
    @SerializedName("data")
    val `data`: Data
) : BaseResponse() {
    data class Data(
        @SerializedName("time")
        val time: String,
        @SerializedName("date")
        val date: String,
        @SerializedName("location_name")
        val locationName: String,
        @SerializedName("terminal_name")
        val terminalName: String
    )
}