package com.pays.pos.data.entities

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
@Entity(tableName = "TbServiceCharge")
data class  TbServiceCharge(
    @SerializedName("created_at")
    val createdAt: String?=null,
    @PrimaryKey
    @SerializedName("id")
    val id: Int,
    @SerializedName("is_enabled")
    var isEnabled: Boolean = false,
    @SerializedName("location_id")
    val locationId: Int,
    @SerializedName("min_guest_count")
    val min_guest_count: Int? = null,
    @SerializedName("max_guest_count")
    val max_guest_count: Int? = null,
    @SerializedName("name")
    val name: String,
    @SerializedName("order_type")
    val order_type: String? = null,
    @SerializedName("percentage")
    val percentage: Double,
    @SerializedName("updated_at")
    val updatedAt: String?=null,
    @SerializedName("is_active")
    var isActive: Boolean = false,
    var isChecked: Boolean = true,
    @SerializedName("order_service_charge_id")
    val order_service_charge_id: Int? = null,
    @SerializedName("is_deleted")
    var isDeleted: Boolean = false

    ) : Parcelable