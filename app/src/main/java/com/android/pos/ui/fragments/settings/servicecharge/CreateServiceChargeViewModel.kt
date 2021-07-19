package com.android.pos.ui.fragments.settings.servicecharge

import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.R
import com.android.pos.data.model.requestModel.CreateServiceChargeRequestModel
import com.android.pos.data.model.responseModel.*
import com.android.pos.data.remote.Constants.LOCATION_ID
import com.android.pos.data.repositories.PosRepository
import com.android.pos.data.repositories.TaxServiceChargeRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Resource
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class CreateServiceChargeViewModel @Inject constructor(
    private val taxServiceChargeRepository: TaxServiceChargeRepository,
    private val prefProvider: PrefProvider
) : ViewModel() {

    val createServiceChargeDetails =
        MutableLiveData(CreateServiceChargeRequestModel.ServiceCharge())

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<Boolean?>>()
    val data: LiveData<Event<Boolean?>> = _data

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private var serviceChargeId: Int = -1

    private var enableSerChargeViewModel: Boolean = false
    private var isEdit: Boolean = false

    private lateinit var serviceChargeData: CreateServiceChargeRequestModel

    private lateinit var resource: Resource<CreateServiceChargeResponse>

    fun isEditData(isEdit: Boolean, serviceChargeId: Int) {
        this.serviceChargeId = serviceChargeId
        this.isEdit = isEdit
    }


    fun enableSerCharge(enableSerCharge: Boolean) {
        this.enableSerChargeViewModel = enableSerCharge
    }

    fun setDiscountData(discountData: GetServiceChargeResponse.Data) {
        createServiceChargeDetails.value?.name = discountData.name
        createServiceChargeDetails.value?.percentage = discountData.percentage
        enableSerChargeViewModel = discountData.isEnabled
    }

    fun submit() {
        val value = createServiceChargeDetails.value
        if (TextUtils.isEmpty(value?.name?.trim())) {
            _snackbarText.value = Event(R.string.discount_name_validate)
        } else if (TextUtils.isEmpty(
                value?.percentage?.toString()?.trim()
            )
            && value?.percentage == 0.0
        ) {
            _snackbarText.value = Event(R.string.discount_rate_validate)
        } else {
            _showProgress.value = Event(true)

            if (isEdit) {
                serviceChargeData = CreateServiceChargeRequestModel().apply {
                    serviceCharge = CreateServiceChargeRequestModel.ServiceCharge().apply {
                        name = value!!.name
                        percentage = value.percentage
                        isEnabled = enableSerChargeViewModel
                        locationId = prefProvider.getValueInt(LOCATION_ID, -1)
                    }
                }
            } else {
                serviceChargeData = CreateServiceChargeRequestModel().apply {
                    serviceCharge = CreateServiceChargeRequestModel.ServiceCharge().apply {
                        name = value!!.name
                        percentage = value.percentage
                        isEnabled = enableSerChargeViewModel
                        locationId = prefProvider.getValueInt(LOCATION_ID, -1)
                    }
                }
            }


            viewModelScope.launch {
                if (isEdit) {
                    resource = taxServiceChargeRepository.updateServiceCharge(serviceChargeId, serviceChargeData)
                } else {
                    resource = taxServiceChargeRepository.createServiceCharge(serviceChargeData)
                }

                when (resource.status) {
                    Status.SUCCESS -> {
                        _showProgress.value = Event(false)
                        resource.data.let { logInResponse ->
                            if (logInResponse?.status == 200) {

                                resource.data?.let {
                                    _data.value = Event(true)
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