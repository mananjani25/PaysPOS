package com.android.pos.ui.fragments.loginscreen

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.remote.Constants.PASSCODE
import com.android.pos.data.remote.Constants.TERMINAL_ID
import com.android.pos.data.repositories.PosRepository
import com.android.pos.data.repositories.UserRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PasscodeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val prefProvider: PrefProvider
) :
    ViewModel() {

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<Boolean?>>()
    val data: LiveData<Event<Boolean?>> = _data

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    fun submit(passcode: String) {

        _showProgress.value = Event(true)

        val data = HashMap<String, String>()
        data["passcode"] = passcode
        data["terminal_id"] = prefProvider.getValueInt(TERMINAL_ID, -1).toString()

        viewModelScope.launch {
            val resource = userRepository.employeeClockIn(data)
            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)

                    resource.data.let {
                        if (it?.status == 200) {
                            resource.data?.let {

                                prefProvider.setValue(PASSCODE, passcode)
                                employeeLogin(data)

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

    private suspend fun employeeLogin(clockinData: HashMap<String, String>) {
        val employeeLogin = userRepository.employeeLogIn(clockinData)

        when (employeeLogin.status) {
            Status.SUCCESS -> {
                _showProgress.value = Event(false)

                employeeLogin.data.let {
                    if (it?.status == 200) {
                        employeeLogin.data?.let {
                            _data.value = Event(true)

                        }
                    } else {
                        _snackbarText.value = Event(employeeLogin.message)
                    }

                }
            }

            Status.ERROR -> {
                _snackbarText.value = Event(employeeLogin.message)
                _showProgress.value = Event(false)
            }

            Status.LOADING -> {
                _showProgress.value = Event(true)
            }
        }
    }
}
