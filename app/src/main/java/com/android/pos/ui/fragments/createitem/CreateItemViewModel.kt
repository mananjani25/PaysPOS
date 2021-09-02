package com.android.pos.ui.fragments.createitem

import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.R
import com.android.pos.data.entities.TbItem
import com.android.pos.data.entities.VariationsAttribute
import com.android.pos.data.model.requestModel.CreateItemRequestModel
import com.android.pos.data.model.responseModel.BaseResponse
import com.android.pos.data.model.responseModel.CreateItemResponse
import com.android.pos.data.remote.Constants.LOCATION_ID
import com.android.pos.data.repositories.PosRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Resource
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateItemViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val prefProvider: PrefProvider
) :
    ViewModel() {

    private var itemId: Int? = null
    private var categoryId: Int? = null
    private var isEdit: Boolean = false
    var itemDetails = MutableLiveData(CreateItemRequestModel())
    private lateinit var itemData: CreateItemRequestModel

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private val _data = MutableLiveData<Event<BaseResponse?>>()
    val data: LiveData<Event<BaseResponse?>> = _data

    private val _variationListLiveData = MutableLiveData<Event<ArrayList<VariationsAttribute>>>()
    val variationListLiveData: LiveData<Event<ArrayList<VariationsAttribute>>> =
        _variationListLiveData

    val modifierSet = posRepository.modifierSetsList()


    fun setData(itemObject: TbItem) {
        isEdit = true
        itemId = itemObject.itemId
        itemDetails.value?.name = itemObject.name
        itemDetails.value?.price = itemObject.price
        itemDetails.value?.sku = ""
        itemDetails.value?.desc = itemObject.shortDescription
    }

    fun editVariation(variationList: ArrayList<VariationsAttribute>) {
        _variationListLiveData.value = Event(variationList)
    }

    fun submit() {
        val value = itemDetails.value
        if (TextUtils.isEmpty(value?.name?.trim())) {
            _snackbarText.value = Event(R.string.item_name_validate)
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


            if (isEdit) {
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
                    locationId = prefProvider.getValueInt(LOCATION_ID, -1)

                }
            }

            viewModelScope.launch {
                val resource: Resource<BaseResponse> = if (isEdit) {
                    posRepository.updateItem(itemId!!, itemData)
                } else {
                    posRepository.createItem(itemData)
                }
                when (resource.status) {
                    Status.SUCCESS -> {
                        _showProgress.value = Event(false)

                        resource.data.let {
                            if (it?.status == 200) {
                                resource.data?.let { createItemResponse ->
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

    fun setCategoryId(categoryId: Int) {
        this.categoryId = categoryId
    }


}