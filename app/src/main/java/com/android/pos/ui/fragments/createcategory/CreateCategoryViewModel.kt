package com.android.pos.ui.fragments.createcategory

import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.R
import com.android.pos.data.entities.TbCategory
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.requestModel.CreateCategoryRequestModel
import com.android.pos.data.remote.Constants
import com.android.pos.data.repositories.PosRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Status.*
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateCategoryViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val prefProvider: PrefProvider
) :
    ViewModel() {

    val categoryDetails = MutableLiveData(CreateCategoryRequestModel())
    private lateinit var createCategoryRequestModel: CreateCategoryRequestModel

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private val _data = MutableLiveData<Event<String?>>()
    val data: LiveData<Event<String?>> = _data

    // private var catId: Int = -1
    private var isEdit: Boolean = false


    val items = posRepository.getItemsList()

    var catId: Int = 0

    fun isEditData(isEdit: Boolean, catId: Int) {
        this.isEdit = isEdit
        this.catId = catId
    }


    fun categoryData(categoryData: TbCategory) {
        categoryDetails.value?.name = categoryData.name ?: ""
    }

    //  val getInventory = catId.value?.let { posRepository.getInventory(it) }

    private fun removeIdsFromOldCategories(
        ids: ArrayList<Int>,
        tbItemsList: ArrayList<TbItem>,
        defaultCatData: TbCategory?
    ) {
        viewModelScope.launch {
            val oldIds = posRepository.getItemsByCategory(catId)

            // If Item removed form current category
            oldIds?.forEach {
                if (!ids.contains(it)) {
                    if (defaultCatData != null) {
                        // assigned removed ids to Default Category
                        posRepository.updateItemCategory(
                            defaultCatData.id,
                            defaultCatData.name ?: "",
                            it
                        )
                        // Updated items ids list of default category
                        val itemIds: ArrayList<Int?>? =
                            posRepository.getItemsByCategory(defaultCatData.id) as ArrayList<Int?>?
                        itemIds?.add(it)
                        posRepository.updateCategoryItems(defaultCatData.id, itemIds as List<Int>)

                        // Updated items ids list of Current category
                        val currentCatItemIds: ArrayList<Int?>? =
                            posRepository.getItemsByCategory(catId) as ArrayList<Int?>?
                        currentCatItemIds?.remove(it)
                        posRepository.updateCategoryItems(catId, currentCatItemIds as List<Int>)
                    }
                }
            }

            // If Item added to current category
            ids.forEach { id ->
                tbItemsList.filter { item ->
                    item.itemId == id && !oldIds?.contains(id)!!
                }.forEach {
                    // Updated items ids list of old category
                    val itemIds: ArrayList<Int?>? =
                        posRepository.getItemsByCategory(it.categoryId) as ArrayList<Int?>?
                    if (itemIds != null && itemIds.size > 0) {
                        itemIds.remove(id)
                        posRepository.updateCategoryItems(it.categoryId, itemIds as List<Int>)
                    }

                    // Updated items ids list of current category
                    val currentCatItemIds: ArrayList<Int?>? =
                        posRepository.getItemsByCategory(catId) as ArrayList<Int?>?
                    if (!currentCatItemIds?.contains(id)!!)
                        currentCatItemIds.add(id)
                    posRepository.updateCategoryItems(catId, currentCatItemIds as List<Int>)
                }
            }
        }
    }

    fun submit(
        ids: ArrayList<Int>,
        imagePath: String?,
        tbItemsList: ArrayList<TbItem>,
        defaultCatData: TbCategory?
    ) {

//        if (isEdit) {
//            if (categoryDetails.value?.name?.trim()?.isEmpty() == true) {
//                _snackbarText.value = Event(R.string.category_name_validate)
//            } else {
//                _showProgress.value = Event(true)
//            }
//        } else

        if (TextUtils.isEmpty(categoryDetails.value?.name?.trim())) {
            _snackbarText.value = Event(R.string.category_name_validate)
        } else {
            _showProgress.value = Event(true)


            if (isEdit) {
                createCategoryRequestModel = CreateCategoryRequestModel().apply {
                    id = catId
                    name = categoryDetails.value?.name!!.trim().replace("\\s+".toRegex(), " ")
                    active = true
                    location_id = prefProvider.getValueInt(Constants.LOCATION_ID, -1)
                    item_ids = ids
                    image = imagePath
                }
            } else {
                createCategoryRequestModel = CreateCategoryRequestModel().apply {
                    name = categoryDetails.value?.name!!.trim().replace("\\s+".toRegex(), " ")
                    active = true
                    location_id = prefProvider.getValueInt(Constants.LOCATION_ID, -1)
                    item_ids = ids
                    image = imagePath
                }
            }

            viewModelScope.launch {

                val resource = if (isEdit) {
                    posRepository.updateCategoryCall(catId, createCategoryRequestModel)
                } else {
                    posRepository.createCategoryCall(createCategoryRequestModel)
                }

                when (resource.status) {
                    SUCCESS -> {
                        _showProgress.value = Event(false)
                        resource.data.let { categoryResponse ->
                            if (categoryResponse?.status == 200) {
                                resource.data?.let {
                                    Log.d("TAG", "responseCreateCategory submit: "+ Gson().toJson(it))
                                    removeIdsFromOldCategories(ids, tbItemsList, defaultCatData)
                                    val category = TbCategory().apply {
                                        name = it.data.name.trim()
                                        id = it.data.id
                                        locationId = it.data.locationId
                                        active = it.data.active
                                        sort = it.data.sort
                                        createdAt = it.data.createdAt
                                        updatedAt = it.data.updatedAt
                                        thumbImgUrl = it.data.thumbImgUrl
                                        item_ids = ids
                                        originalImgUrl = it.data.originalImgUrl
                                    }
                                    posRepository.createCategory(category)
                                    Log.d("TAG", "responseCreateCategory sort number of gift card: "+prefProvider.getValueInt(Constants.GIFT_CARD_SORT,0))
                                    Log.d("TAG", "responseCreateCategory sort number of default : "+prefProvider.getValueInt(Constants.DEFAULT_CATEGORY_SORT,0))
                                    Log.d("TAG", "responseCreateCategory id of gift card at first position: "+prefProvider.getValueboolean(Constants.GIFT_CARD_AT_FIRST,false))
                                    if (prefProvider.getValueboolean(Constants.GIFT_CARD_AT_FIRST,false)){
                                        posRepository.updateSorting(Constants.DEFAULT_CATEGORY,prefProvider.getValueInt(Constants.DEFAULT_CATEGORY_SORT,0)+1)
                                    }else{
                                        posRepository.updateSorting(Constants.GIFT_CARD_CATEGORY,prefProvider.getValueInt(Constants.GIFT_CARD_SORT,0)+1)
                                        posRepository.updateSorting(Constants.DEFAULT_CATEGORY,prefProvider.getValueInt(Constants.DEFAULT_CATEGORY_SORT,0)+2)
                                    }

                                   /* if (isEdit) {
                                        val oldIds = posRepository.getItemsByCategory(category.id)
                                        oldIds?.forEach { old ->
                                            posRepository.updateItemCategory(
                                                category.id,
                                                category.name ?: "",
                                                null
                                            )
                                        }
                                    }*/

                                    ids.forEach { itemId ->
                                        posRepository.updateItemCategory(
                                            category.id,
                                            category.name ?: "",
                                            itemId
                                        )
                                    }

                                    _data.value = Event(it.message)


                                }
                            } else {
                                _snackbarText.value = Event(resource.message)
                            }

                        }
                    }

                    ERROR -> {
                        _snackbarText.value = Event(resource.message)
                        _showProgress.value = Event(false)
                    }

                    LOADING -> {
                        _showProgress.value = Event(true)
                    }
                }
            }

        }
    }


}