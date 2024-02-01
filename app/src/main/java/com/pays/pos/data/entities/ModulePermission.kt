package com.pays.pos.data.entities

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
@Keep
@Entity(tableName = "TbModule")
data class ModulePermission(
    @SerializedName("created_at")
    val createdAt: String,
    @PrimaryKey
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("team_role_id")
    val teamRoleId: Int,
    @SerializedName("updated_at")
    val updatedAt: String
) : Parcelable