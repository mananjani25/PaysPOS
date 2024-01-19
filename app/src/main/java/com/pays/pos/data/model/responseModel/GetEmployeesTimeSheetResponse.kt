package com.pays.pos.data.model.responseModel


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class GetEmployeesTimeSheetResponse(
    @SerializedName("data")
    val `data`: List<Data>,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("all_employees_total_hours")
    val employeTotalHours: String,
    @SerializedName("all_employees_total_wages")
    val employeeTotalWage: String,
    @SerializedName("type")
    val type: String

) : Parcelable {
    @Parcelize
    data class Data(
        @SerializedName("data_of_week")
        val dataOfWeek: DataOfWeek,
        @SerializedName("team_id")
        val teamId: Int,
        @SerializedName("team_name")
        val teamName: String,
        @SerializedName("team_role_name")
        val teamRoleName: String,
        @SerializedName("team_role")
        val teamRole: Int,
        @SerializedName("total_hours")
        val totalHours: String,
        @SerializedName("total_wage")
        val totalWage: String
    ) : Parcelable {
        @Parcelize
        data class DataOfWeek(
            @SerializedName("Fri")
            val fri: String,
            @SerializedName("Mon")
            val mon: String,
            @SerializedName("Sat")
            val sat: String,
            @SerializedName("Sun")
            val sun: String,
            @SerializedName("Thu")
            val thu: String,
            @SerializedName("Tue")
            val tue: String,
            @SerializedName("Wed")
            val wed: String
        ) : Parcelable
    }
}