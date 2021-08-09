package com.android.pos.ui.fragments.createitem

import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.R
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.requestModel.CreateItemRequestModel
import com.android.pos.data.model.responseModel.BaseResponse
import com.android.pos.data.repositories.PosRepository
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Resource
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateItemViewModel @Inject constructor(
    private val posRepository: PosRepository
) :
    ViewModel() {

    private var itemId: Int? = null
    private var isEdit: Boolean = false
    var itemDetails = MutableLiveData(CreateItemRequestModel())
    private lateinit var itemData: CreateItemRequestModel

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    val modifierSet = posRepository.modifierSetsList()


    fun setData(itemObject: TbItem) {
        isEdit = true
        itemId = itemObject.itemId
        itemDetails.value?.itemName = itemObject.name
        itemDetails.value?.price = itemObject.price
        itemDetails.value?.sku = ""
        itemDetails.value?.description = itemObject.shortDescription
    }

    fun submit() {
        val value = itemDetails.value
        if (TextUtils.isEmpty(value?.itemName?.trim())) {
            _snackbarText.value = Event(R.string.item_name_validate)
        } else if (TextUtils.isEmpty(
                value?.price?.toString()?.trim()
            )
            && value?.price == 0.0
        ) {
            _snackbarText.value = Event(R.string.item_price_validate)
        } else if (TextUtils.isEmpty(value?.sku?.trim())) {
            _snackbarText.value = Event(R.string.item_sku_validate)
        } else {
            _showProgress.value = Event(true)


            if (isEdit) {
                itemData = CreateItemRequestModel().apply {
                    id = itemId
                    itemName = value!!.itemName
                    price = value.price
                    sku = ""
                    description = value.description
                }

            } else {
                itemData = CreateItemRequestModel().apply {
                    itemName = value!!.itemName
                    price = value.price
                    sku = ""
                    description = value.description

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
                                resource.data?.let {

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


}