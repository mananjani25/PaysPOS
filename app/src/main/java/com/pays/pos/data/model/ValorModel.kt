package com.pays.pos.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "valor")
data class ValorModel(
    @PrimaryKey
    val id: Int = 1,
    val appid: String = "",
    val appkey: String = "",
    val epi: String = "",
    val channel_id: String = "",
    val enabled: Boolean = true,
)