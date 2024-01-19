package com.pays.pos.data.model.responseModel


import com.pays.pos.data.entities.Employee
import com.google.gson.annotations.SerializedName

data class CreateEmployeeResponse(
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
        @SerializedName("employee")
        val employee: Employee
    )
}