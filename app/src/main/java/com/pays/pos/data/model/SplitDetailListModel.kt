package com.pays.pos.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "TbSplit")
data class SplitDetailListModel(
    @PrimaryKey(autoGenerate = true)
    val id: Int=0,
    val title: String,
    val amount: Double,
    val remainingAmt: Double
)
