package com.android.pos.data.model.responseModel.employeeTipSummary

import com.google.gson.annotations.SerializedName

data class EmployeeTipSummaryResponse(
    @SerializedName("data")
    val `data`: ArrayList<Data>,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("type")
    val type: String
) {
    data class Data(
        @SerializedName("employee_name")
        val employee_name: String,
        @SerializedName("total_card_tips")
        val total_card_tips: Double,
        @SerializedName("total_cash_tips")
        val total_cash_tips: Double,
        @SerializedName("total_tips")
        val total_tips: Double
    )

}

