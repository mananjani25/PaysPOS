package com.pays.pos.data.entities

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Keep
@Entity(tableName = "TbPrinterQueuePrintedOrders")
data class TbPrinterQueuePrintedOrders(

    @PrimaryKey
    @SerializedName("id")
    var id: Int = 0,

    @SerializedName("order_id")
    var order_id: String? = null
)

