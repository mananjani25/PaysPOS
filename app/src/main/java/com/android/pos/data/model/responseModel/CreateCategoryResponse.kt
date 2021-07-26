package com.android.pos.data.model.responseModel


import com.google.gson.annotations.SerializedName

data class CreateCategoryResponse(
    @SerializedName("data")
    val `data`: Data,
) : BaseResponse() {
    data class Data(
        @SerializedName("active")
        val active: Boolean,
        @SerializedName("created_at")
        val createdAt: String,
        @SerializedName("id")
        val id: Int,
        @SerializedName("location_id")
        val locationId: Int,
        @SerializedName("name")
        val name: String,
        @SerializedName("sort")
        val sort: Int = -1,
        @SerializedName("updated_at")
        val updatedAt: String
    )
}