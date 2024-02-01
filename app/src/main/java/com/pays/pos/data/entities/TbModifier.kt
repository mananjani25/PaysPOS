package com.pays.pos.data.entities

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey


@Keep
@Entity(tableName = "TbModifier")
class TbModifier {

    @PrimaryKey(autoGenerate = true)
    var id: Int? = null
    var mAltName: String = "" // RA
    var mCreatedAt: String = "" // 2020-06-23T11:30:17.253Z
    var modifierId: Int? = null// 1
    var mName: String? = "" // roasted
    var mPrice: Double? = 0.0// 10
    var mSort: Int = 0 // 0
    var mUpdatedAt: String = ""// 2020-06-23T11:30:17.253Z
    var mItemQuantity: Int? = 0
    var modifierGroupId: Int? = null
    var inventoryId: Int? = null
    var inventoryParentId: Int? = null


}