package com.pays.pos.data.model.responseModel


import com.google.gson.annotations.SerializedName

data class CreateRoleResponse(
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
        @SerializedName("team_roles")
        val teamRoles: List<TeamRole>
    ) {
        data class TeamRole(
            @SerializedName("employees")
            val employees: List<Employee>,
            @SerializedName("id")
            val id: Int,
            @SerializedName("name")
            val name: String
        ) {
            data class Employee(
                @SerializedName("email")
                val email: String,
                @SerializedName("first_name")
                val firstName: String,
                @SerializedName("id")
                val id: Int,
                @SerializedName("is_active")
                val isActive: Boolean,
                @SerializedName("last_name")
                val lastName: String,
                @SerializedName("location_id")
                val locationId: Int,
                @SerializedName("name")
                val name: String,
                @SerializedName("passcode")
                val passcode: String,
                @SerializedName("phone_number")
                val phoneNumber: String
            )
        }
    }
}