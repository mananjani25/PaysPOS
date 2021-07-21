package com.android.pos.ui.fragments.team

import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.R
import com.android.pos.data.model.requestModel.CreateEmployeeRequestModel
import com.android.pos.data.model.responseModel.BaseResponse
import com.android.pos.data.model.responseModel.EmployeeListResponse
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
class CreateTeamViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val prefProvider: PrefProvider
) : ViewModel() {

    private lateinit var createEmployeeData: CreateEmployeeRequestModel

    val locationId = prefProvider.getValueInt(LOCATION_ID, 0)
    private var taxId: Int = -1

    private var isEdit: Boolean = false

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<Boolean?>>()
    val data: LiveData<Event<Boolean?>> = _data

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    val createTaxDetails = MutableLiveData(CreateEmployeeRequestModel())
    private lateinit var resource: Resource<BaseResponse>

    fun setTaxData(employeeModel: EmployeeListResponse.Data.Employee) {

        createTaxDetails.value?.firstName = employeeModel.firstName
        createTaxDetails.value?.lastName = employeeModel.lastName
        createTaxDetails.value?.email = employeeModel.email
        createTaxDetails.value?.phoneNumber = employeeModel.phoneNumber.toString()
        createTaxDetails.value?.locationId = employeeModel.locationId
        createTaxDetails.value?.passcode = employeeModel.passcode
        createTaxDetails.value?.isActive = employeeModel.isActive

    }

    fun isEditData(isEdit: Boolean, taxId: Int) {
        this.taxId = taxId
        this.isEdit = isEdit
    }


    fun submit() {


        val value = createTaxDetails.value
        if (TextUtils.isEmpty(value?.firstName?.trim())) {
            _snackbarText.value = Event(R.string.first_name_validate)
        } else if (TextUtils.isEmpty(
                value?.lastName?.trim()
            )
        ) {
            _snackbarText.value = Event(R.string.last_name_validate)
        } else {
            _showProgress.value = Event(true)


            createEmployeeData = CreateEmployeeRequestModel().apply {
                if (isEdit) id = taxId
                firstName = value!!.firstName
                lastName = value.lastName
                phoneNumber = value.phoneNumber
                email = value.email
                locationId = prefProvider.getValueInt(LOCATION_ID, -1)
                passcode = value.passcode
                isActive = true
            }


            viewModelScope.launch {

                resource = if (isEdit) {
                    posRepository.updateEmployee(taxId, createEmployeeData)
                } else {
                    posRepository.createEmployee(createEmployeeData)
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