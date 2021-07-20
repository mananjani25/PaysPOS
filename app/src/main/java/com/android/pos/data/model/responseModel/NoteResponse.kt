package com.android.pos.data.model.responseModel


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Parcelize
data class NoteResponse(
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
        var name: String,
        @SerializedName("sort")
        val sort: Int,
        @SerializedName("updated_at")
        val updatedAt: String,
        var isChecked: Boolean = true
    ) : Parcelable
}