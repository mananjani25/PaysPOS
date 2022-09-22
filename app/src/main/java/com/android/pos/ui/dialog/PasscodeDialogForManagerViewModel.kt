package com.android.pos.ui.dialog

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.db.AppDatabase
import com.android.pos.data.model.responseModel.BaseResponse
import com.android.pos.data.model.responseModel.PasscodeManagerModel
import com.android.pos.data.remote.Constants
import com.android.pos.data.repositories.PosRepository
import com.android.pos.data.repositories.UserRepository
import com.android.pos.di.PrefProvider
import com.android.pos.di.RolePermission
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Status
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
                }

                Status.LOADING -> {
                    _showProgress.value = Event(true)
                }
            }

        }
    }


}