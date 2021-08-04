package com.android.pos.ui.fragments.settings.teamrole

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.model.responseModel.CreateTipResponse
import com.android.pos.data.model.responseModel.GetTipReponse
import com.android.pos.data.repositories.PosRepository
import com.android.pos.data.repositories.TaxServiceChargeRepository
import com.android.pos.data.repositories.TipDiscountRepository
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserPermissionListViewModel @Inject constructor(
    private val taxServiceChargeRepository: TaxServiceChargeRepository
) : ViewModel() {

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<CreateTipResponse?>>()
    val data: LiveData<Event<CreateTipResponse?>> = _data

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private val _notifydata = MutableLiveData<Event<Boolean?>>()
    val notifydata: LiveData<Event<Boolean?>> = _notifydata

    val getTeamRoleList = taxServiceChargeRepository.getTeamRoleList()


    /*  fun delete(id: Int) {
          _showProgress.value = Event(true)

          viewModelScope.launch {
              val resource = tipDiscountRepository.deleteTip(id)
              when (resource.status) {
                  Status.SUCCESS -> {
                      _showProgress.value = Event(false)

                      resource.data.let {
                          if (it?.status == 200) {
                              resource.data?.let { createTipResponse ->
                                  tipDiscountRepository.deleteTipDatabase(id)
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
      }*/
}