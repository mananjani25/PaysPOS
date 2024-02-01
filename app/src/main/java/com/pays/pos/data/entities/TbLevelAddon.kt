package com.pays.pos.data.entities

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.pays.pos.data.typeconvert.TypeConvertersDBLevelAddon

@TypeConverters(TypeConvertersDBLevelAddon::class)
@Keep
@Entity(tableName = "TbLevelAddon")
class TbLevelAddon {

    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
    var inventoryId: Int? = null
    var categoryId: Int? = null
    var cost: Double = 0.0
    var createdAt: String = ""
    var imageUrl: String = ""
    var isHide: Boolean = false
    var kitchenName: String = ""
    var modifierGroupIds: String = ""
    var name: String = ""
    var price: Double = 0.0
    var productCode: String = ""
    var quantity: Int = 0
    var shortDescription: String = ""
    var sort: Int = 0
    var updatedAt: String = ""
    var itemQuantity: Int = 0
    var isManualSales: Boolean = false
    var levelAddonId: Int? = null
    var inventoryParentId: Int? = null


}