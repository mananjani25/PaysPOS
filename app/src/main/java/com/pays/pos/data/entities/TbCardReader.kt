package com.pays.pos.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName


@Entity(tableName = "TbCardReader")
data class TbCardReader(

    @SerializedName("name")
    var name: String = "",

    @PrimaryKey
    @SerializedName("mcAddress")
    var mcAddress: String = "",

    @SerializedName("type")
    var type: Int = -1,

    @SerializedName("status")
    var status: Int = 0


)