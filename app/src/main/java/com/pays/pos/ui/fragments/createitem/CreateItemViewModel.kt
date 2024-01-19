package com.pays.pos.ui.fragments.createitem

import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pays.pos.R
import com.pays.pos.data.entities.ModifierSet
import com.pays.pos.data.entities.TbItem
import com.pays.pos.data.entities.VariationsAttribute
import com.pays.pos.data.model.requestModel.CreateItemRequestModel
import com.pays.pos.data.model.responseModel.item.ItemResponseNew
import com.pays.pos.data.remote.Constants.LOCATION_ID
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.Event
import com.pays.pos.utils.statusUtils.Resource
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Exception
import javax.inject.Inject

@HiltViewModel
class CreateItemViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val prefProvider: PrefProvider
) :
    ViewModel() {

    private var itemId: Int? = null
    private var categoryIdViewModel: Int = 0
    private var isEdit: Boolean = false
    var itemDetails = MutableLiveData(CreateItemRequestModel())
    private lateinit var itemData: CreateItemRequestModel
    private lateinit var modifierSetIdsViewModel: ArrayList<Int>
    private lateinit var itemModifierSetsSortList: ArrayList<Int>
    private var selectedTaxList: ArrayList<String> = ArrayList()
    var taxNameToDisplay: String = ""
    private var itemPriceViewModel: Double? = 0.0
    private var descViewModel: String = ""
    private var skuViewModel: String = ""
    private var stockViewModel: Int = 0
    private var imageViewModel: String? = ""
    private var body2ViewModel: String? = ""
    private var productCodeModel: String? = ""
    private lateinit var variationAttributeModel: ArrayList<VariationsAttribute>

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private val _data = MutableLiveData<Event<ItemResponseNew?>>()
    val data: LiveData<Event<ItemResponseNew?>> = _data

    private val _variationListLiveData = MutableLiveData<Event<ArrayList<VariationsAttribute>>>()
    val variationListLiveData: LiveData<Event<ArrayList<VariationsAttribute>>> =
        _variationListLiveData

    val modifierSet = posRepository.modifierSetsList()

    fun updateMod(mod: ModifierSet) = posRepository.updateModSet(mod)


    fun setData(itemObject: TbItem) {
        isEdit = true
        itemId = itemObject.itemId
        itemDetails.value?.name = itemObject.name
        itemDetails.value?.price = itemObject.price
        itemDetails.value?.sku = ""
        itemDetails.value?.desc = itemObject.shortDescription
    }


    fun submit() {
        val value = itemDetails.value


        if (TextUtils.isEmpty(value?.name?.trim())) {
            _snackbarText.value = Event(R.string.item_name_validate)
        } else if (categoryIdViewModel == 0) {
            _snackbarText.value = Event(R.string.category_select_validate)
        } /*else if (TextUtils.isEmpty(
                value?.price?.toString()?.trim()
            )
            && value?.price == 0.0
        ) {
            _snackbarText.value = Event(R.string.item_price_validate)
        } else if (TextUtils.isEmpty(value?.sku?.trim())) {
            _snackbarText.value = Event(R.string.item_sku_validate)
        }*/ else {
            _showProgress.value = Event(true)


            /*if (isEdit) {
                itemData = CreateItemRequestModel().apply {
                    //id = itemId!!
                    name = value!!.name
                    //   price = value.price
                    //   sku = value.sku
                    //   desc = value.desc
                    categoryId = categoryId
                    locationId = prefProvider.getValueInt(LOCATION_ID, -1)
                }

            } else {
                itemData = CreateItemRequestModel().apply {
                    name = value!!.name
                    //   price = value.price
                    //   sku = value.sku
                    //   desc = value.desc
                    //  quantity = 10
                    categoryId = categoryId
                    modifierSetIds = modifierSetIdsViewModel
                    locationId = prefProvider.getValueInt(LOCATION_ID, -1)

                }
            }*/

            itemData = CreateItemRequestModel().apply {
                if (isEdit) id = itemId
                active = true
                name = value!!.name.trim().replace("\\s+".toRegex(), " ")
                priceType = if (itemPriceViewModel != null) {
                    "Fixed"
                } else {
                    "Variable"
                }
                price = itemPriceViewModel
                image = imageViewModel
                sku = skuViewModel
                quantity = stockViewModel
                desc = descViewModel
                categoryId = categoryIdViewModel
                modifierSetIds = modifierSetIdsViewModel
                variationsAttributes = variationAttributeModel
                locationId = prefProvider.getValueInt(LOCATION_ID, -1)
                taxIds = selectedTaxList
                productCode = productCodeModel ?: ""
                itemModifierSetsSort = itemModifierSetsSortList

            }

            viewModelScope.launch {
                val resource: Resource<ItemResponseNew> = if (isEdit) {
                    posRepository.updateItemApiCall(itemId!!, itemData)
                } else {
                    posRepository.createItemApiCall(itemData)
                }
                when (resource.status) {
                    Status.SUCCESS -> {
                        _showProgress.value = Event(false)

                        resource.data.let {
                            if (it?.status == 200) {
                                resource.data?.let { createItemResponse ->

                                    //save data in db


                                    val item = TbItem().convertToItem(createItemResponse.data, null)
                                    updateItemsDataInModifiers(createItemResponse.data.modifierSetIds, item.itemId)
                                    posRepository.createItem(item)

                                    _data.value = Event(createItemResponse)
                                }
                            } else {
                                _snackbarText.value = Event(resource.message)
                            }

                        }
                    }

                    Status.ERROR -> {
                        _snackbarText.value = Event(resource.message)
                        _showProgress.value = Event(false)
                    }

                    Status.LOADING -> {
                        _showProgress.value = Event(true)
                    }
                }
            }

        }

    }

    // To update items ids array for modifiers in DB
    private suspend fun updateItemsDataInModifiers(modifierSetIds: List<Int>, itemId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var oldModifierSetIds: ArrayList<Int> = arrayListOf()
                posRepository.getSingleItem(itemId).let { item ->
                    if(item?.modifier_set_ids != null) {
                        oldModifierSetIds = item.modifier_set_ids as ArrayList
                    }
                }
                // Remove item ids from modifier (if item's new modifier set doesn't contain ids of existing modifier set)
                oldModifierSetIds.forEach { oldModifierId ->
                    if (!modifierSetIds.contains(oldModifierId)) {
                        val modifier: ModifierSet? = posRepository.getSingleModifier(oldModifierId)
                        if (modifier != null) {
                            val itemsIdsPresent: ArrayList<Int> = modifier.itemIds as ArrayList
                            if (itemsIdsPresent.isNotEmpty()) {
                                itemsIdsPresent.remove(itemId)
                                posRepository.updateItemIdsForModifier(
                                    oldModifierId,
                                    itemsIdsPresent
                                )
                            }
                        }
                    }
                }

                // Add Item id to modifier  (if item's existing modifier set doesn't contain ids of new modifier set)
                modifierSetIds.forEach { newModId ->
                    if (!oldModifierSetIds.contains(newModId)) {
                        val modifier: ModifierSet? = posRepository.getSingleModifier(newModId)
                        if (modifier != null) {
                            val itemsIdsPresent: ArrayList<Int> = modifier.itemIds as ArrayList
                            if (itemsIdsPresent.isNotEmpty()) {
                                if (!itemsIdsPresent.contains(itemId)) {
                                    itemsIdsPresent.add(itemId)
                                }
                            } else {
                                itemsIdsPresent.add(itemId)
                            }
                            posRepository.updateItemIdsForModifier(newModId, itemsIdsPresent)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setCategoryId(categoryId: Int) {
        this.categoryIdViewModel = categoryId
    }

    fun selectedTaxList(taxIds: ArrayList<String>) {
        this.selectedTaxList.clear()
        this.selectedTaxList.addAll(taxIds)
    }

    fun getSelectedTaxList(): ArrayList<String> {
        return selectedTaxList
    }

    fun selectedModifierList(modifierSetIds: ArrayList<Int>) {
        this.modifierSetIdsViewModel = modifierSetIds
    }

    fun selectedModifierSortList(list: ArrayList<Int>) {
        this.itemModifierSetsSortList = list
    }

    fun variationAttribute(variationAttribute: ArrayList<VariationsAttribute>) {
        this.variationAttributeModel = variationAttribute
    }

    fun itemDetails(
        filePath: String?,
        itemPrice: Double?,
        desc: String,
        sku: String,
        stock: Int,
        productCode: String
    ) {
        this.imageViewModel = filePath
        this.itemPriceViewModel = itemPrice
        this.descViewModel = desc
        this.skuViewModel = sku
        this.stockViewModel = stock
        this.productCodeModel = productCode
    }


}