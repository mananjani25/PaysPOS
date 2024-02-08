package com.pays.pos.data.entities


import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
@Entity(tableName = "LoyaltyPrograms")
data class LoyaltyProgramsModel(
    @SerializedName("amount")
    val amount: Double,
    @SerializedName("created_at")
    val createdAt: String,
    @PrimaryKey
    @SerializedName("id")
    val id: Int,
    @SerializedName("is_enable")
    var isEnable: Boolean = false,
    @SerializedName("location_id")
    val locationId: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("reward_point")
    var rewardPoint: Int,
    @SerializedName("reward_type")
    val rewardType: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("is_deleted")
    var isDeleted: Boolean = false
): Parcelable