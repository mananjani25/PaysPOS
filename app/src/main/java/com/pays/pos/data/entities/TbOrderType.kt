package com.pays.pos.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "TbOrderType")
data class TbOrderType(
    @SerializedName("created_at")
    val createdAt: String,
    @PrimaryKey
    @SerializedName("id")
    val id: Int,
    @SerializedName("is_active")
    val isActive: Boolean,
    @SerializedName("location_id")
    val locationId: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("order_type")
    val orderType: String,
    @SerializedName("sort")
    val sort: Int,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("is_deleted")
    var isDeleted: Boolean = false,
     @SerializedName("primary_order_type")
    var primaryOrderType: Boolean = false,
    @SerializedName("is_default")
    var isDefault: Boolean = false,
) : Parcelable
