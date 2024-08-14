package com.pays.pos.data.entities

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Keep
@Entity(tableName = "TbDynamicPaymentRecords")
class TbDynamicPaymentRecords {
    @PrimaryKey
    @SerializedName("id")
    var id: Int = 0

    @SerializedName("name")
    var name: String? = null

    @SerializedName("sort")
    var sort: Int? = null

    @SerializedName("is_active")
    var isActive: Boolean? = null

    @SerializedName("location_id")
    var locationId: Int? = null

    @SerializedName("created_at")
    var createdAt: String? = null

    @SerializedName("updated_at")
    var updatedAt: String? = null
}

