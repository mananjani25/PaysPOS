package com.pays.pos.data.model.responseModel


import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
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
    @Keep
@Entity(tableName = "TbNotes")
    data class Data(
        @SerializedName("created_at")
        val createdAt: String,
        @PrimaryKey
        @SerializedName("id")
        val id: Int,
        @SerializedName("is_active")
        var isActive: Boolean = false,
        @SerializedName("location_id")
        val locationId: Int,
        @SerializedName("name")
        var name: String,
        @SerializedName("sort")
        var sort: Int,
        @SerializedName("updated_at")
        val updatedAt: String,
        var isChecked: Boolean = true,
        @SerializedName("is_deleted")
        var isDeleted: Boolean = false
    ) : Parcelable
}