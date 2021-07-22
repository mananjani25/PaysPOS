package com.android.pos.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.android.pos.data.typeconvert.TypeConvertersTax
import kotlinx.parcelize.Parcelize

@TypeConverters(TypeConvertersTax::class)
@Entity(tableName = "TbItem", primaryKeys = ["itemId", "categoryId"])
@Parcelize
class TbItem : Parcelable {

    var itemId: Int = 0
    var categoryId: Int = 0
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
    var taxes: List<TaxModel>? = null


}