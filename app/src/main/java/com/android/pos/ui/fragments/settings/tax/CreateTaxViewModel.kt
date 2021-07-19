package com.android.pos.ui.fragments.settings.tax

import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.R
import com.android.pos.data.model.requestModel.CreateTaxRequestModel
import com.android.pos.data.model.responseModel.CreateTaxResponse
import com.android.pos.data.model.responseModel.GetTaxResponse
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
class CreateTaxViewModel @Inject constructor(
    private val taxServiceChargeRepository: TaxServiceChargeRepository,
    private val prefProvider: PrefProvider
) : ViewModel() {

    val createTaxDetails = MutableLiveData(CreateTaxRequestModel())

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<Boolean?>>()
    val data: LiveData<Event<Boolean?>> = _data

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private var taxId: Int = -1

    private var isEdit: Boolean = false

    private lateinit var taxData: CreateTaxRequestModel

    private lateinit var resource: Resource<CreateTaxResponse>

    fun isEditData(isEdit: Boolean, taxId: Int) {
        this.taxId = taxId
        this.isEdit = isEdit
    }

    fun setTaxData(taxData: GetTaxResponse.TaxData) {
        createTaxDetails.value?.name = taxData.name
        createTaxDetails.value?.rate = taxData.rate
    }

    fun submit() {
        val value = createTaxDetails.value
        if (TextUtils.isEmpty(value?.name?.trim())) {
            _snackbarText.value = Event(R.string.tax_name_validate)
        } else if (TextUtils.isEmpty(
                value?.rate?.toString()?.trim()
            )
            && value?.rate == 0.0
        ) {
            _snackbarText.value = Event(R.string.tax_rate_validate)
        } else {
            _showProgress.value = Event(true)

            if (isEdit) {
                taxData = CreateTaxRequestModel().apply {
                    id = taxId
                    name = value!!.name
                    rate = value.rate
                    locationId = prefProvider.getValueInt(LOCATION_ID, -1)
                }
            } else {
                taxData = CreateTaxRequestModel().apply {
                    name = value!!.name
                    rate = value.rate
                    locationId = prefProvider.getValueInt(LOCATION_ID, -1)
                }
            }


            viewModelScope.launch {
                if (isEdit) {
                    resource = taxServiceChargeRepository.updateTax(taxId, taxData)
                } else {
                    resource = taxServiceChargeRepository.createTax(taxData)
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