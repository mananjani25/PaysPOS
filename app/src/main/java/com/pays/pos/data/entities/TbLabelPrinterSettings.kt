package com.pays.pos.data.entities

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Keep
@Entity(tableName = "TbLabelPrinterSettings")
data class TbLabelPrinterSettings(
    @PrimaryKey
    @SerializedName("id") val id: Int,

    @SerializedName("one_item_per_reciept")
    val oneItemPerReciept: Boolean = false
)
