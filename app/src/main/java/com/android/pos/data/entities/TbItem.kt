package com.android.pos.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.android.pos.data.model.responseModel.category.Category
import com.android.pos.data.model.responseModel.item.Item
import com.android.pos.data.typeconvert.TypeConvertersTax
import kotlinx.parcelize.Parcelize
import java.util.*


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
    var taxes: List<TaxData>? = null
    var imageUrl: String? = null
    var thumbImageUrl: String? = null
    var createdAt: String = ""
    var updatedAt: String = ""
    var customItemID: Int = 0
    var categoryId: Int = 0
    var categoryName: String = ""

    var modifierGroupIds: String = ""
    var shortDescription: String = ""
    var note: String = ""

    var itemQuantity: Int = 0
    var isManualSales: Boolean = false
    var isChecked: Boolean = false
    var modifier_set_ids: List<Int> = emptyList()
    var option_set_ids: List<Int> = emptyList()
    var isTax: Boolean = false
    var modifiers: List<Modifier> = emptyList()

    var customItemCount: Int = 0
    var discountPrice: Double = 0.0
    var isDiscountDefault = false
    var discountId: Int? = null
    var discountType: String = ""
    var variationsAttributes: List<VariationsAttribute> = emptyList()
    var optionSets: List<OptionSet>? = null

    var orderItemId: Int? = null
    var isFired: Boolean = false
    var timeStamp: String? = null
    var isPaid: Boolean = false
    var isEdited: Boolean = false
    var guestItemId: Int? = null
    var isDestroy:Boolean = false
    var reorder:Boolean = false
    var isSelectedItem:Boolean = false
    var manualSaleId: String = UUID.randomUUID().toString()

    fun convertToItem(item: Item, category: Category?): TbItem {
        itemId = item.id
        name = item.name ?: ""
        cost = item.cost
        price = item.price
        priceType = item.priceType ?: ""
        quantity = item.quantity
        kitchenName = item.kitchenName ?: ""
        productCode = item.productCode ?: ""
        sku = item.sku ?: ""
        isHide = item.active
        sort = item.sort
        imageUrl = item.originalImageUrl
        thumbImageUrl = item.thumbImageUrl
        categoryId = category?.id ?: item.categoryId ?: 0
        categoryName = category?.name ?: item.categoryName ?: ""
        taxes = item.taxes
        modifier_set_ids = item.modifierSetIds
        variationsAttributes = item.variations
        shortDescription = item.desc?: ""
        return this
    }
}