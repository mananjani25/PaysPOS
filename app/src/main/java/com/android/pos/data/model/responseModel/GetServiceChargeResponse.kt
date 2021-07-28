package com.android.pos.data.model.responseModel


import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class GetServiceChargeResponse(
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
    @Entity(tableName = "TbServiceCharge")
    data class Data(
        @SerializedName("created_at")
        val createdAt: String,
        @PrimaryKey
        @SerializedName("id")
        val id: Int,
        @SerializedName("is_enabled")
        var isEnabled: Boolean = false,
        @SerializedName("location_id")
        val locationId: Int,
        @SerializedName("name")
        val name: String,
        @SerializedName("percentage")
        val percentage: Double,
        @SerializedName("updated_at")
        val updatedAt: String,
        @SerializedName("is_active")
        var isActive: Boolean = false,
        var isChecked: Boolean = true
    ) : Parcelable
}