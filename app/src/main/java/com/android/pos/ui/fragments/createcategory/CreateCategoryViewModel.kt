package com.android.pos.ui.fragments.createcategory

import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.R
import com.android.pos.data.entities.TbCategory
import com.android.pos.data.model.requestModel.CreateCategoryRequestModel
import com.android.pos.data.remote.Constants
import com.android.pos.data.repositories.PosRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Status.*
import dagger.hilt.android.lifecycle.HiltViewModel
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
        categoryDetails.value?.name = categoryData.name?:""
    }

    //  val getInventory = catId.value?.let { posRepository.getInventory(it) }

    fun submit(ids: ArrayList<Int>, imagePath: String?, categoryData: TbCategory) {

        if (TextUtils.isEmpty(categoryDetails.value?.name?.trim())) {
            _snackbarText.value = Event(R.string.category_name_validate)
        }
        else if (categoryData.name.equals(categoryDetails.value?.name))
            _snackbarText.value = Event(R.string.same_category_name)
        else {
            _showProgress.value = Event(true)

            if (isEdit) {
                createCategoryRequestModel = CreateCategoryRequestModel().apply {
                    id = catId
                    name = categoryDetails.value?.name.toString()
                    active = true
                    location_id = prefProvider.getValueInt(Constants.LOCATION_ID, -1)
                    item_ids = ids
                    image = imagePath
                }
            } else {
                createCategoryRequestModel = CreateCategoryRequestModel().apply {
                    name = categoryDetails.value?.name.toString()
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

                                    val category = TbCategory().apply {
                                        name = it.data.name
                                        id = it.data.id
                                        locationId = it.data.locationId
                                        active = it.data.active
                                        sort = it.data.sort
                                        createdAt = it.data.createdAt
                                        updatedAt = it.data.updatedAt
                                        thumbImgUrl = it.data.thumbImgUrl
                                        originalImgUrl = it.data.originalImgUrl

                                    }
                                    posRepository.createCategory(category)


                                    if (isEdit) {
                                        val oldIds = posRepository.getItemsByCategory(category.id)
                                        oldIds?.forEach { old ->
                                            posRepository.updateItemCategory(
                                                category.id,
                                                category.name?:"",
                                                null
                                            )
                                        }
                                    }

                                    ids.forEach { itemId ->
                                        posRepository.updateItemCategory(
                                            category.id,
                                            category.name?:"",
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