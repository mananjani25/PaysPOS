package com.pays.pos.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "characters")
data class CharacterModel(
    val created: String = "",
    val gender: String = "",
    @PrimaryKey
    val id: Int = 1,
    val image: String = "",
    val name: String = "",
    val species: String = "",
    val status: String = "",
    val type: String = "",
    val url: String = ""
)