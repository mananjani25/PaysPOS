package com.android.pos.data.model.responseModel


import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.android.pos.data.entities.ModulePermission
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class GetTeamRoleModule(
    @SerializedName("data")
    val `data`: List<ModulePermission>,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("type")
    val type: String
) : Parcelable