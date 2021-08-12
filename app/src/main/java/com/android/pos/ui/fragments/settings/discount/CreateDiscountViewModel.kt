package com.android.pos.ui.fragments.settings.discount

import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.R
import com.android.pos.data.entities.TbDiscount
import com.android.pos.data.model.requestModel.CreateDiscountRequestModel
import com.android.pos.data.model.requestModel.CreateTaxRequestModel
import com.android.pos.data.model.responseModel.*
import com.android.pos.data.remote.Constants.LOCATION_ID
import com.android.pos.data.repositories.PosRepository
import com.android.pos.data.repositories.TipDiscountRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Resource
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class CreateDiscountViewModel @Inject constructor(
    private val tipDiscountRepository: TipDiscountRepository,
    private val prefProvider: PrefProvider
) : ViewModel() {

    val createDiscountDetails = MutableLiveData(CreateDiscountRequestModel.Discount())

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<CreateDiscountResponse?>>()
    val data: LiveData<Event<CreateDiscountResponse?>> = _data

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private var discountId: Int = -1
    private var discountTypeViewModel: String = "Percentage"
    private var isEdit: Boolean = false

    private lateinit var discountData: CreateDiscountRequestModel

    private lateinit var resource: Resource<CreateDiscountResponse>

    fun isEditData(isEdit: Boolean, discountId: Int) {
        this.discountId = discountId
        this.isEdit = isEdit
    }


    fun discountType(discountType: String) {
        this.discountTypeViewModel = discountType
    }

    fun setDiscountData(discountData: TbDiscount) {
        createDiscountDetails.value?.name = discountData.name
        createDiscountDetails.value?.percentage = discountData.percentage
        discountTypeViewModel = discountData.discountType
    }

    fun submit() {
        val value = createDiscountDetails.value
        if (TextUtils.isEmpty(value?.name?.trim())) {
            _snackbarText.value = Event(R.string.discount_name_validate)
        } else if (TextUtils.isEmpty(
                value?.percentage?.toString()?.trim()
            )
            || value?.percentage == 0.0
        ) {
            _snackbarText.value = Event(R.string.discount_rate_validate)
        } else {
            _showProgress.value = Event(true)

            if (isEdit) {
                discountData = CreateDiscountRequestModel().apply {
                    discount = CreateDiscountRequestModel.Discount().apply {
                        name = value!!.name
                        percentage = value.percentage
                        discountType = discountTypeViewModel    /*[Percentage Amount]*/
                        locationId = prefProvider.getValueInt(LOCATION_ID, -1)
                    }
                }
            } else {
                discountData = CreateDiscountRequestModel().apply {
                    discount = CreateDiscountRequestModel.Discount().apply {
                        name = value!!.name
                        percentage = value.percentage
                        discountType = discountTypeViewModel    /*[Percentage Amount]*/
                        locationId = prefProvider.getValueInt(LOCATION_ID, -1)
                    }


                }
            }


            viewModelScope.launch {
                resource = if (isEdit) {
                    tipDiscountRepository.updateDiscount(discountId, discountData)
                } else {
                    tipDiscountRepository.createDiscount(discountData)
                }

                when (resource.status) {
                    Status.SUCCESS -> {
                        _showProgress.value = Event(false)
                        resource.data.let { logInResponse ->
                            if (logInResponse?.status == 200) {

                                resource.data?.let { createDiscountResponse ->

                                    val discount = TbDiscount(
                                        createdAt = createDiscountResponse.data.createdAt,
                                        discountType = createDiscountResponse.data.discountType,
                                        id = createDiscountResponse.data.id,
                                        locationId = createDiscountResponse.data.locationId,
                                        name = createDiscountResponse.data.name,
                                        percentage = createDiscountResponse.data.percentage,
                                        updatedAt = createDiscountResponse.data.updatedAt,
                                        isActive = createDiscountResponse.data.isActive
                                    )
                                    tipDiscountRepository.createDiscountDatabase(discount)
                                    _data.value = Event(createDiscountResponse)
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