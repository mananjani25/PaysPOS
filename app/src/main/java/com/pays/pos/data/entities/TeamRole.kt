package com.pays.pos.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "TbTeamRole")
data class TeamRole(
    @PrimaryKey
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("employees")
    val employees: List<Employee>?,
    @SerializedName("module_permission")
    val modulePermission: List<ModulePermission>?,
    @SerializedName("is_deleted")
    var isDeleted: Boolean = false
) : Parcelable