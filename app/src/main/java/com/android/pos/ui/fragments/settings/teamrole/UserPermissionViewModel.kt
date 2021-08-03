package com.android.pos.ui.fragments.settings.teamrole

import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.R
import com.android.pos.data.model.requestModel.CreateServiceChargeRequestModel
import com.android.pos.data.model.requestModel.CreateTeamRoleRequestModel
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
class UserPermissionViewModel @Inject constructor(
    private val taxServiceChargeRepository: TaxServiceChargeRepository,
    private val prefProvider: PrefProvider,
    private val posRepository: PosRepository
) : ViewModel() {

    val locationId = prefProvider.getValueInt(LOCATION_ID, 0)

    val createUserPermission =
        MutableLiveData(CreateTeamRoleRequestModel())

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<CreateServiceChargeResponse?>>()
    val data: LiveData<Event<CreateServiceChargeResponse?>> = _data

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private var roleId: Int = -1

    private var enableSerChargeViewModel: Boolean = false
    private var isEdit: Boolean = false


    private lateinit var createTeamRoleRequestModel: CreateTeamRoleRequestModel

    private lateinit var resource: Resource<CreateServiceChargeResponse>


    fun employeeData() = posRepository.employeesList(locationId)

    fun isEditData(isEdit: Boolean, taxId: Int) {
        this.roleId = taxId
        this.isEdit = isEdit
    }


    fun submit() {
        val value = createUserPermission.value
        if (TextUtils.isEmpty(value?.name?.trim())) {
            _snackbarText.value = Event(R.string.role_name_validate)
        } else {
            _showProgress.value = Event(true)

            createTeamRoleRequestModel = CreateTeamRoleRequestModel().apply {
                if (isEdit) id = roleId
                name = value!!.name
            }


            viewModelScope.launch {
                if (isEdit) {
                    resource = taxServiceChargeRepository.updateTeamRole(
                        roleId,
                        createTeamRoleRequestModel
                    )
                } else {
                    resource = taxServiceChargeRepository.createTeamRole(createTeamRoleRequestModel)
                }

                when (resource.status) {
                    Status.SUCCESS -> {
                        _showProgress.value = Event(false)
                        resource.data.let { logInResponse ->
                            if (logInResponse?.status == 200) {

                                resource.data?.let { createTeamRole ->

                                    /* val serviceCharge = GetServiceChargeResponse.Data(
                                         createdAt = createServiceChargeResponse.data.createdAt,
                                         id = createServiceChargeResponse.data.id,
                                         isEnabled = createServiceChargeResponse.data.isEnabled,
                                         locationId = createServiceChargeResponse.data.locationId,
                                         name = createServiceChargeResponse.data.name,
                                         percentage = createServiceChargeResponse.data.percentage,
                                         updatedAt = createServiceChargeResponse.data.updatedAt,
                                         isActive = createServiceChargeResponse.data.isActive
                                     )

                                     taxServiceChargeRepository.createServiceChargeDatabase(
                                         serviceCharge
                                     )*/
                                    _data.value = Event(createTeamRole)
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