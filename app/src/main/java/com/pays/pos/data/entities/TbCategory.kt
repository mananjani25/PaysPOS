package com.pays.pos.data.entities

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.pays.pos.data.typeconvert.TypeConvertersIds
import kotlinx.parcelize.Parcelize

@TypeConverters(TypeConvertersIds::class)
@Keep
@Entity(tableName = "TbCategory")
@Parcelize
class TbCategory : Parcelable {
    @PrimaryKey
    var id: Int = 0
    var locationId: Int = 0
    var active: Boolean = false
    var name: String? = ""
    var sort: Int = 0
    var createdAt: String? = ""
    var updatedAt: String? = ""
    var isSelect: Boolean = false
    var item_ids: List<Int> = emptyList()
    var thumbImgUrl: String? = ""
    var originalImgUrl: String? = ""
    var isDeleted: Boolean = false
}