package com.android.pos.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.android.pos.data.model.responseModel.GetTaxResponse
import com.android.pos.data.typeconvert.TypeConvertersTax
import kotlinx.parcelize.Parcelize


@TypeConverters(TypeConvertersTax::class)
@Entity(tableName = "TbItem")
@Parcelize
class TbItem : Parcelable {

    @PrimaryKey
    var itemId: Int = 0
    var name: String = ""
    var cost: Double = 0.0
    var price: Double = 0.0
    var priceType: String = ""
    var quantity: Int = 0
    var kitchenName: String = ""
    var productCode: String = ""
    var sku: String = ""
    var isHide: Boolean = false
    var sort: Int = 0
    var taxes: List<GetTaxResponse.TaxData>? = null
    var imageUrl: String? = null
    var thumbImageUrl: String? = null
    var createdAt: String = ""
    var updatedAt: String = ""

    var categoryId: Int = 0
    var categoryName: String = ""

    var modifierGroupIds: String = ""
    var shortDescription: String = ""

    var itemQuantity: Int = 0
    var isManualSales: Boolean = false
    var isChecked: Boolean = false

}