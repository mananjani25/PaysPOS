package com.pays.pos.data.entities

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize


@Parcelize
@Keep
@Entity(tableName = "TbDiscount")
data class TbDiscount(
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("discount_type")
    var discountType: String,
    @PrimaryKey
    @SerializedName("id")
    val id: Int,
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
    var isChecked: Boolean = true,
    @SerializedName("is_deleted")
    var isDeleted: Boolean = false
) : Parcelable