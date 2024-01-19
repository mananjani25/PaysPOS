package com.pays.pos.data.model.responseModel


import com.google.gson.annotations.SerializedName

data class GetEmployeeTimeSheetDetailsResponse(
    @SerializedName("data")
    val `data`: List<Data>,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("type")
    val type: String,
    @SerializedName("employee_total_hours")
    val employeTotalHours: String,
    @SerializedName("employee_total_wages")
    val employeeTotalWage: String,
) {
    data class Data(
        @SerializedName("actual_in_time")
        val actualInTime: String,
        @SerializedName("clock_in_time")
        val clockInTime: String,
        @SerializedName("clock_out_date")
        val clockOutDate: String,
        @SerializedName("clock_out_time")
        val clockOutTime: String,
        @SerializedName("date")
        val date: String,
        @SerializedName("hourly_wages")
        val hourlyWages: String,
        @SerializedName("is_log_present")
        val isLogPresent: Boolean,
        @SerializedName("total_in_time")
        val totalInTime: String,
        @SerializedName("total_wage")
        val totalWage: String
    )
}