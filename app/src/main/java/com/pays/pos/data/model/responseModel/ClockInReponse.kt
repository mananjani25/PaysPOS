package com.pays.pos.data.model.responseModel


import com.google.gson.annotations.SerializedName

data class ClockInReponse(
    @SerializedName("data")
    val `data`: Data
) : BaseResponse() {
    data class Data(
        @SerializedName("employee_id")
        val employeeId: Int,
        @SerializedName("employee_name")
        val employee_name: String,
        @SerializedName("employee_role")
        val employee_role: String?,
        @SerializedName("team_role_id")
        val team_role_id: Int?,
        @SerializedName("module_records")
        val moduleRecords: List<ModuleRecord>
    ) {
        data class ModuleRecord(
            @SerializedName("created_at")
            val createdAt: String,
            @SerializedName("id")
            val id: Int,
            @SerializedName("name")
            val name: String,
            @SerializedName("updated_at")
            val updatedAt: String
        )
    }
}