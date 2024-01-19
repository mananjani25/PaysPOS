package com.pays.pos.ui.fragments.settings.teamrole

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pays.pos.data.model.responseModel.BaseResponse
import com.pays.pos.data.model.responseModel.CreateRoleResponse
import com.pays.pos.data.model.responseModel.CreateTipResponse
import com.pays.pos.data.model.responseModel.GetTipReponse
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.data.repositories.TaxServiceChargeRepository
import com.pays.pos.data.repositories.TipDiscountRepository
import com.pays.pos.utils.Event
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserAccessPermissionListViewModel @Inject constructor(
    private val taxServiceChargeRepository: TaxServiceChargeRepository
) : ViewModel() {

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<BaseResponse?>>()
    val data: LiveData<Event<BaseResponse?>> = _data

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    val getTeamRoleList = taxServiceChargeRepository.getTeamRoleList()


    fun delete(id: Int) {
        _showProgress.value = Event(true)

        viewModelScope.launch {
            val resource = taxServiceChargeRepository.deleteTeamRole(id)
            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)

                    resource.data.let {
                        if (it?.status == 200) {
                            resource.data?.let { createTipResponse ->
                                taxServiceChargeRepository.deleteTeamRoleDatabase(id)
                                _data.value = Event(createTipResponse)
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