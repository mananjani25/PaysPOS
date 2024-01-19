package com.pays.pos.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "TbSplit")
data class SplitDetailListModel(
    @PrimaryKey(autoGenerate = true)
    val id: Int=0,
    val title: String,
    val amount: Double,
    val remainingAmt: Double
)
