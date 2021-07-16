package com.android.pos.data.model.responseModel


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class GetTipReponse(
    @SerializedName("data")
    val `data`: List<Data>,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("type")
    val type: String
) : Parcelable {
    @Parcelize
    data class Data(
        @SerializedName("created_at")
        val createdAt: String,
        @SerializedName("id")
        val id: Int,
        @SerializedName("is_active")
        val isActive: Boolean,
        @SerializedName("location_id")
        val locationId: Int,
        @SerializedName("name")
        val name: String,
        @SerializedName("rate")
        val rate: Double,
        @SerializedName("sort")
        val sort: Int,
        @SerializedName("updated_at")
        val updatedAt: String
    ) : Parcelable
}