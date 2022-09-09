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
    var isDestroy: Boolean = false
    var reorder: Boolean = false
    var manualSaleId: String = UUID.randomUUID().toString()
    var isDeleted: Boolean = false

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
        shortDescription = item.desc ?: ""
        isDeleted = item.isDeleted
        return this
    }


    fun convertToItem1(item: TbItem, model: TbItem): TbItem {

        var modeTb = TbItem()

        var itemList = mutableListOf<TaxData>()
        var itemTaxIds: ArrayList<Int> = arrayListOf()
        item.taxes?.forEach {
            itemTaxIds.add(it.id)
        }


        model.taxes?.let {

            itemList.addAll(it)
        }
        var removeItems: ArrayList<TaxData> = arrayListOf()

        item.taxes?.forEachIndexed { index, it ->

            for (i in 0 until itemList.size) {

                if (itemList.get(i).id == it.id) {
                    if (it.isDeleted || !it.isActive) {
                        removeItems.add(itemList.get(i))

                    }

                }
            }
            if (!itemList.contains(it) && !it.isDeleted && it.isActive) {
                itemList.add(it)
            }
        }
        //remove items from list
        itemList.removeAll(removeItems)


        if (itemList.isEmpty()) {
            modeTb.taxes = emptyList()
        } else {
            modeTb.taxes = itemList
        }
        modeTb.itemId = item.itemId
        modeTb.name = item.name ?: ""
        modeTb.cost = item.cost
        modeTb.price = item.price
        modeTb.priceType = item.priceType ?: ""
        modeTb.quantity = item.quantity
        modeTb.kitchenName = item.kitchenName ?: ""
        modeTb.productCode = item.productCode ?: ""
        modeTb.sku = item.sku ?: ""
        modeTb.isHide = item.isHide
        modeTb.sort = item.sort
        modeTb.imageUrl = item.imageUrl
        modeTb.thumbImageUrl = item.thumbImageUrl
        modeTb.categoryId = item.categoryId
        modeTb.categoryName = item.categoryName
        modeTb.modifier_set_ids = item.modifier_set_ids
        modeTb.variationsAttributes = item.variationsAttributes
        modeTb.shortDescription = item.shortDescription ?: ""
        modeTb.isDeleted = item.isDeleted
        modeTb.modifiers = item.modifiers
        return modeTb
    }

}