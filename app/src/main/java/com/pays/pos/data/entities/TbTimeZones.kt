package com.pays.pos.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Entity(tableName = "TbTimeZones")
data class TbTimeZones(
    @PrimaryKey
    @SerializedName("key") val name: String,
    @SerializedName("value") val value: String,
    @SerializedName("is_deleted")
    val isDeleted: Boolean = false

)
