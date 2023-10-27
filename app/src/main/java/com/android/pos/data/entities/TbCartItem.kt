package com.android.pos.data.entities

import android.os.Parcelable
import android.util.Log
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.android.pos.data.model.responseModel.category.Category
import com.android.pos.data.model.responseModel.item.Item
import com.android.pos.data.typeconvert.TCModifier
import com.android.pos.data.typeconvert.TypeConvertersIds
import com.android.pos.data.typeconvert.TypeConvertersTax
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.util.*
import kotlin.collections.ArrayList

@TypeConverters(TypeConvertersTax::class, TypeConvertersIds::class, TCModifier::class)
@Entity(tableName = "TbCartItem")
@Parcelize
class TbCartItem : Parcelable {

    @PrimaryKey(autoGenerate = true)
    var cartItemId: Int = 0
    var itemId: Int = 0
    var name: String = ""
    var id: Int = 0
    var price: Double = 0.0
    var quantity: Int = 0
    var sku: String = ""
    var isHide: Boolean = false
    var sort: Int = 0
    var dineInSort: Int = 0
    var hide_status: String? = null
    var website_hide_status: String? = null
    var taxes: List<TaxData>? = null
    var imageUrl: String? = null
    var thumbImageUrl: String? = null
    var createdAt: String = ""
    var updatedAt: String = ""
    var customItemID: Int = 0
    var categoryId: Int = 0
    var categoryName: String = ""
    var shortDescription: String = ""
    var note: String = ""

    var itemQuantity: Int = 0
    var isManualSales: Boolean = false
    var isChecked: Boolean = false
    var modifier_set_ids: List<Int> = emptyList()
    var modifiers: List<Modifier> = emptyList()

    var customItemCount: Int = 0
    var discountPrice: Double = 0.0
    var singleItemPrice: Double = 0.0
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
    var headerPositionDinein = 0
    var itemOriginalModifiersList: List<Modifier>? = arrayListOf()

    fun convertToItem(item: Item, category: Category?): TbCartItem {

        itemId = item.id
        name = item.name ?: ""
        price = item.price
        quantity = item.quantity
        sku = item.sku ?: ""
        website_hide_status = item.website_hide_status ?: ""
        hide_status = item.hide_status ?: ""
        isHide = item.active
        sort = item.sort
        imageUrl = item.originalImageUrl
        thumbImageUrl = item.thumbImageUrl
        categoryId = category?.id ?: item.categoryId ?: 0
        categoryName = category?.name ?: item.categoryName ?: ""
        taxes = item.taxes
        modifier_set_ids = item.itemModifierSetsSort
        variationsAttributes = item.variations
        shortDescription = item.desc ?: ""
        isDeleted = item.isDeleted
        return this
    }

    fun convertToCartItem(item: TbItem, model: TbItem): TbCartItem {
        Log.e("GetItemForCheck", "item1  ${Gson().toJson(item)}")
        Log.e("GetItemForCheck", "model1  ${Gson().toJson(model)}")

        val modeTb = TbCartItem()

        val itemList = mutableListOf<TaxData>()
        val itemTaxIds: ArrayList<Int> = arrayListOf()


        item.taxes?.forEach {
            itemTaxIds.add(it.id)
        }
        model.taxes?.let {

            itemList.addAll(it)
        }
        val removeItems: ArrayList<TaxData> = arrayListOf()

        val itemListIds: ArrayList<Int> = arrayListOf()

        for (m in 0 until itemList.size) {

            itemListIds.add(itemList[m].id)
        }

        item.taxes?.forEachIndexed { index, it ->

            for (i in 0 until itemList.size) {

                if (itemList.get(i).id == it.id) {
                    if (it.isDeleted || !it.isActive) {
                        removeItems.add(itemList.get(i))

                    }

                }
            }


            if (!itemList.contains(it) && !it.isDeleted && it.isActive && !itemListIds.contains(it.id)) {
                itemList.add(it)
            }

            //for update the tax
            else if (itemListIds.contains(it.id) && !it.isDeleted && it.isActive) {

                for (m in 0 until itemList.size) {
                    if (itemList.get(m).id == it.id) {
                        itemList.set(m, it)
                    }
                }
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
        modeTb.price = item.price
        modeTb.quantity = item.quantity
        modeTb.sku = item.sku ?: ""
        modeTb.isHide = item.isHide
        modeTb.sort = item.sort
        modeTb.imageUrl = item.imageUrl
        modeTb.website_hide_status = item.website_hide_status ?: ""
        modeTb.hide_status = item.hide_status ?: ""
        modeTb.thumbImageUrl = item.thumbImageUrl
        modeTb.categoryId = item.categoryId
        modeTb.categoryName = item.categoryName


        if (item.name.trim().equals("Veg slice",true)) {
            Log.e("price_without_markup_1", Gson().toJson(modeTb.modifier_set_ids))
            Log.e("price_without_markup_2", Gson().toJson(item.modifier_set_ids))
        }
        /*  if (item.itemModifierSetsSort?.isNotEmpty() == true) {
              modeTb.itemModifierSetsSort = item.itemModifierSetsSort
          } else {
              modeTb.itemModifierSetsSort = model.itemModifierSetsSort
          }*/
        if (item.modifier_set_ids.isEmpty() && model.modifier_set_ids.isEmpty()) {

            val listMod: ArrayList<Int> = arrayListOf()
            listMod.addAll(item.modifier_set_ids)
            listMod.addAll(modeTb.modifier_set_ids)

            modeTb.modifier_set_ids = LinkedHashSet(listMod).toMutableList()

        } else if (item.modifier_set_ids.isNotEmpty()) {
            modeTb.modifier_set_ids = item.modifier_set_ids

        } else {

            modeTb.modifier_set_ids = item.modifier_set_ids

        }

        if (item.variationsAttributes.isNotEmpty() && model.variationsAttributes.isNotEmpty()) {
            val variationList: ArrayList<VariationsAttribute> = arrayListOf()
            variationList.addAll(model.variationsAttributes)
            val removeVar: ArrayList<VariationsAttribute> = arrayListOf()
            val listIdsVariation: ArrayList<Int> = arrayListOf()
            model.variationsAttributes.forEach {
                it.id?.let { it1 -> listIdsVariation.add(it1) }
            }

            item.variationsAttributes.forEach {
                if (listIdsVariation.contains(it.id)) {
                    Log.e("ModYEs", "Content")
                    model.variationsAttributes.forEach { it1 ->

                        if (it1.id == it.id && it.isDeleted) {
                            variationList.forEach { varI ->
                                if (varI.id == it.id) {
                                    varI.isDeleted = it.isDeleted
                                }
                            }
                            removeVar.add(it)


                        } else if (it1.id == it.id && !it.isActive) {
                            variationList.forEach { varI ->
                                if (varI.id == it.id) {
                                    varI.isActive = it.isActive
                                }
                            }
                            removeVar.add(it)
                        } else {
                            variationList.forEach { varI ->
                                if (varI.id == it.id) {
                                    varI.name = it.name
                                    varI.priceType = it.priceType
                                    varI.price = it.price
                                    varI.optionIds = it.optionIds
                                    varI.optionSetIds = it.optionSetIds
                                    varI.orderVariationId = it.orderVariationId
                                    varI.stockQty = it.stockQty
                                    varI.sku = it.sku
                                }

                            }
                        }

                    }


                } else {
                    variationList.add(it)
                }


            }

            variationList.removeAll(removeVar)

            Log.e("CheckVarRemove", "removeVar  ${Gson().toJson(removeVar)}")
            Log.e("CheckVarRemove", "variation  ${Gson().toJson(variationList)}")



            Log.e("GetVaroatom", "${variationList.size}")

            modeTb.variationsAttributes = variationList

        } else if (item.variationsAttributes.isNotEmpty()) {
            modeTb.variationsAttributes = item.variationsAttributes

        } else {
            modeTb.variationsAttributes = model.variationsAttributes

        }

        if (item.modifiers.isNotEmpty() && model.modifiers.isNotEmpty()) {

            var modifierList: ArrayList<Modifier> = arrayListOf()
            modifierList.addAll(model.modifiers)
            var removeVar: ArrayList<Modifier> = arrayListOf()
            var listIdsVariation: ArrayList<Int> = arrayListOf()
            model.modifiers.forEach {
                it.id?.let { it1 -> listIdsVariation.add(it1) }
            }

            item.modifiers.forEach {
                if (listIdsVariation.contains(it.id)) {
                    Log.e("ModYEs", "Content")
                    model.variationsAttributes.forEach { it1 ->

                        if (it1.id == it.id && it.isDeleted) {
                            modifierList.forEach { varI ->
                                if (varI.id == it.id) {
                                    varI.isDeleted = it.isDeleted
                                }
                            }
                            removeVar.add(it)


                        } else if (it1.id == it.id && !it.isChecked) {
                            modifierList.forEach { varI ->
                                if (varI.id == it.id) {
                                    varI.isChecked = it.isChecked
                                }
                            }
                            removeVar.add(it)
                        } else {
                            modifierList.forEach { varI ->
                                if (varI.id == it.id) {
                                    varI.name = it.name
                                    varI.sort = it.sort
                                    varI.price = it.price
                                    varI._destroy = it._destroy
                                    varI.isChecked = it.isChecked
                                    varI.isDeleted = it.isDeleted
                                    varI.modifierSetId = it.modifierSetId
                                    varI.orderItemTaxes = it.orderItemTaxes
                                }

                            }
                        }

                    }


                } else {
                    modifierList.add(it)
                }


            }

            modifierList.removeAll(removeVar)


            Log.e("GetVaroatommodifierList", "${modifierList.size}")

            modeTb.modifiers = modifierList


        } else if (item.modifiers.isNotEmpty()) {
            modeTb.modifiers = item.modifiers

        } else {
            modeTb.modifiers = model.modifiers

        }


        modeTb.shortDescription = item.shortDescription ?: ""
        modeTb.isDeleted = item.isDeleted
      //  modeTb.price_without_markup = item.price_without_markup
        return modeTb
    }

    fun convertToModifier(modifierSetOld: ModifierSet, model: ModifierSet): ModifierSet {

        var modeModifierSet = ModifierSet()

        val itemList = mutableListOf<Modifier>()
        val itemTaxIds: ArrayList<Int> = arrayListOf()
        modifierSetOld.modifiers.forEach {
            it.id?.let { it1 -> itemTaxIds.add(it1) }
        }


        model.modifiers.let {

            itemList.addAll(it)
        }
        val removeItems: ArrayList<Modifier> = arrayListOf()

        modifierSetOld.modifiers.forEachIndexed { index, it ->

            for (i in 0 until itemList.size) {

                if (itemList.get(i).id == it.id) {
                    if (it.isDeleted) {
                        removeItems.add(itemList[i])
                    }

                }
            }
            if (!itemList.contains(it) && !it.isDeleted) {
                var content = false
                for (i in 0 until itemList.size) {
                    if (it.id == itemList.get(i).id) {
                        content = true
                        break
                    }
                }
                if (!content) {
                    itemList.add(it)
                }
            }
        }
        //remove items from list
        itemList.removeAll(removeItems)

        Log.e("modeModifierSet", Gson().toJson(itemList))


        if (itemList.isEmpty()) {
            modeModifierSet.modifiers = emptyList()
        } else {
            modeModifierSet.modifiers = itemList
        }

        modeModifierSet.id = modifierSetOld.id
        modeModifierSet.itemIds = modifierSetOld.itemIds
        modeModifierSet.name = modifierSetOld.name
        modeModifierSet.updatedAt = modifierSetOld.updatedAt
        modeModifierSet.locationId = modifierSetOld.locationId
        modeModifierSet.isChecked = modifierSetOld.isChecked
        modeModifierSet.min_required = modifierSetOld.min_required
        modeModifierSet.max_allowed = modifierSetOld.max_allowed
        modeModifierSet.sort = modifierSetOld.sort
        modeModifierSet.isDeleted = modifierSetOld.isDeleted

        Log.e("modeModifierSet1", Gson().toJson(modeModifierSet))


        return modeModifierSet
    }

}