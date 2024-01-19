package com.pays.pos.data.model.responseModel


import com.google.gson.annotations.SerializedName

data class LogInResponse(
    @SerializedName("data")
    val `data`: Data,
) : BaseResponse() {
    data class Data(
        @SerializedName("auth_token")
        val authToken: String,
        @SerializedName("base_url")
        val baseUrl: String,
        @SerializedName("created_at")
        val createdAt: String,
        @SerializedName("email")
        val email: String,
        @SerializedName("id")
        val id: Int,
        @SerializedName("location_id")
        val locationId: Int,
        @SerializedName("location_name")
        val locationName: String,
        @SerializedName("updated_at")
        val updatedAt: String,
        @SerializedName("user_name")
        val userName: String?
    )
}