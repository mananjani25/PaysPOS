package com.android.pos.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "TbCategory")
@Parcelize
class TbCategory : Parcelable {
    @PrimaryKey
    var id: Int = 0
    var locationId: Int = 0
    var active: Boolean = false
    var name: String = ""
    var sort: Int = 0
    var createdAt: String = ""
    var updatedAt: String = ""
    var isSelect: Boolean = false
}