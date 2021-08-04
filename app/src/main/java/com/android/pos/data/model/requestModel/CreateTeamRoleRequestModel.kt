package com.android.pos.data.model.requestModel


import com.google.gson.annotations.SerializedName

data class CreateTeamRoleRequestModel(
    @SerializedName("id")
    var id: Int? = null,
    @SerializedName("employee_ids")
    var employeeIds: List<Int>? = null,
    @SerializedName("name")
    var name: String = ""
)