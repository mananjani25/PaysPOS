package com.pays.pos.ui.dialog

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pays.pos.data.db.AppDatabase
import com.pays.pos.data.model.responseModel.BaseResponse
import com.pays.pos.data.model.responseModel.PasscodeManagerModel
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.data.repositories.UserRepository
import com.pays.pos.di.PrefProvider
import com.pays.pos.di.RolePermission
import com.pays.pos.utils.Event
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PasscodeDialogForManagerViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val prefProvider: PrefProvider
) :
    ViewModel() {

    private val _snackbarText = MutableLiveData<Event<String>>()
    val snackbarText: LiveData<Event<String>> = _snackbarText

    private val _data = MutableLiveData<Event<Boolean>>()
    val data: LiveData<Event<Boolean>> = _data

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress
    fun submit(passcode: String) {
        _showProgress.value = Event(true)
        viewModelScope.launch {

            val passcodeResorcse =
                posRepository.checkPermissionRole(passcode)
            when (passcodeResorcse.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    passcodeResorcse.data.let { responseData ->
                        if (responseData?.data == false){
                            _snackbarText.value = Event(responseData.message.toString())
                        }else{
                            _data.value = Event(true)
                        }
                    }


                }
                Status.ERROR -> {
                    _snackbarText.value = Event(passcodeResorcse.message.toString())
                    _showProgress.value = Event(false)
                }

                Status.LOADING -> {
                    _showProgress.value = Event(true)
                }
            }

        }
    }


}