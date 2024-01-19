package com.pays.pos.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "TbCountryList")
data class TbCountryList(
    @SerializedName("name")
    val name: String,
    @PrimaryKey
    @SerializedName("id")
    val id: Int,
    @SerializedName("is_deleted")
    val isDeleted: Boolean
) {
}