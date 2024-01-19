package com.pays.pos.ui.fragments.team

import android.text.TextUtils
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pays.pos.R
import com.pays.pos.data.entities.Employee
import com.pays.pos.data.model.requestModel.CreateEmployeeRequestModel
import com.pays.pos.data.model.responseModel.CreateEmployeeResponse
import com.pays.pos.data.remote.Constants.LOCATION_ID
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.data.repositories.TaxServiceChargeRepository
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.Event
import com.pays.pos.utils.statusUtils.Resource
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateTeamViewModel @Inject constructor(
    private val taxServiceChargeRepository: TaxServiceChargeRepository,
    private val posRepository: PosRepository,
    private val prefProvider: PrefProvider
) : ViewModel() {

    private lateinit var createEmployeeData: CreateEmployeeRequestModel

    var locationId = prefProvider.getValueInt(LOCATION_ID, -1)
    var phone_country_temp = -1
    private var taxId: Int = -1
    private var roleId: Int = -1

    private var isEdit: Boolean = false

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<CreateEmployeeResponse?>>()
    val data: LiveData<Event<CreateEmployeeResponse?>> = _data

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    val createTaxDetails = MutableLiveData(CreateEmployeeRequestModel())
    private lateinit var resource: Resource<CreateEmployeeResponse>

    val roleList = taxServiceChargeRepository.getTeamRoleListFromDatabase()

    fun roleNameById(taxId: Int) = taxServiceChargeRepository.getCurrentUserTeamRoleFromDb(taxId)

    val coutrylist = posRepository.getAllCountryList()
    fun setTaxData(employeeModel: Employee) {

        createTaxDetails.value?.firstName = employeeModel.firstName
        createTaxDetails.value?.lastName = employeeModel.lastName
        createTaxDetails.value?.email = employeeModel.email ?: ""
        createTaxDetails.value?.phoneNumber =
            employeeModel.phoneNumber.toString()
        createTaxDetails.value?.locationId = prefProvider.getValueInt(LOCATION_ID, -1)
        createTaxDetails.value?.passcode = employeeModel.passcode ?: ""
        createTaxDetails.value?.isActive = employeeModel.isActive
        createTaxDetails.value?.hourly_wages = employeeModel.hourlyWages
        if (employeeModel.teamRoleId != null) {
            roleId = employeeModel.teamRoleId
        }

    }

    fun isEditData(isEdit: Boolean, taxId: Int) {
        this.taxId = taxId
        this.isEdit = isEdit
    }

    fun isCountryChanged(position: Int) {
        this.phone_country_temp = position
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
        } else if (value?.phoneNumber?.length != 0 && value?.phoneNumber?.length!! < 14) {
            _snackbarText.value = Event(R.string.valid_phone_no_validate)
        } else if (!TextUtils.isEmpty(value?.email?.trim()) && !Patterns.EMAIL_ADDRESS.matcher(value?.email?.trim())
                .matches()
        ) {
            _snackbarText.value = Event(R.string.valid_email_validate)
        } else if (roleId == -1 || roleId == 0) {
            _snackbarText.value = Event(R.string.please_choos_a_role)
        } else if (TextUtils.isEmpty(value.passcode.trim())) {
            _snackbarText.value = Event(R.string.please_enter_passcode)
        }else if (value.passcode.trim().length < 4) {
            _snackbarText.value = Event(R.string.please_enter_passcode_length)
        } else {
            _showProgress.value = Event(true)


            createEmployeeData = CreateEmployeeRequestModel().apply {
                if (isEdit) id = taxId
                firstName = value?.firstName
                lastName = value?.lastName
                phoneNumber = value?.phoneNumber?.replace(("[\\D]").toRegex(), "")!!
                email = value?.email!!
                locationId = prefProvider.getValueInt(LOCATION_ID, -1)
                phone_country = phone_country_temp
                passcode = value.passcode.toString()
                isActive = true
                team_role_id = roleId
                hourly_wages = value.hourly_wages
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

                                resource.data?.let { createEmployeeResponse ->

                                    posRepository.createEmployeeDatabase(createEmployeeResponse.data.employee)
                                    _data.value = Event(createEmployeeResponse)

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

    fun setRoleId(roleId: Int) {
        this.roleId = roleId
    }


}