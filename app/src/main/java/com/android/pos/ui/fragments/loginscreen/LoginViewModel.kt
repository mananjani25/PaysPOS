package com.android.pos.ui.fragments.loginscreen

import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.R
import com.android.pos.data.model.requestModel.LoginRequestModel
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.AUTH_TOKEN
import com.android.pos.data.remote.Constants.BASE_URL_NEW
import com.android.pos.data.remote.Constants.EMAIL
import com.android.pos.data.remote.Constants.LOCATION_ID
import com.android.pos.data.remote.Constants.TERMINAL_ID
import com.android.pos.data.remote.Constants.USERNAME
import com.android.pos.data.remote.NetworkConnectionInterceptor
import com.android.pos.data.repositories.UserRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val prefProvider: PrefProvider,
    private val networkConnectionInterceptor: NetworkConnectionInterceptor
) :
    ViewModel() {

    val loginDetails = MutableLiveData(LoginRequestModel())

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<Boolean?>>()
    val data: LiveData<Event<Boolean?>> = _data

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress


    fun submit(device_token: String) {

        if (TextUtils.isEmpty(loginDetails.value?.emailAddress?.trim())) {
            _snackbarText.value = Event(R.string.email_validate)
        } else if (!Patterns.EMAIL_ADDRESS.matcher(loginDetails.value?.emailAddress?.trim())
                .matches()
        ) {
            _snackbarText.value = Event(R.string.valid_email_validate)
        } else if (TextUtils.isEmpty(loginDetails.value?.password?.trim())) {
            _snackbarText.value = Event(R.string.password_validate)
        } else {
            _showProgress.value = Event(true)

            val data = HashMap<String, String>()
            data["email"] = loginDetails.value?.emailAddress.toString().trim()
            data["password"] = loginDetails.value?.password.toString().trim()

            viewModelScope.launch {
                val resource = userRepository.userLogIn(data)
                when (resource.status) {
                    Status.SUCCESS -> {
                        _showProgress.value = Event(false)
                        resource.data.let { logInResponse ->
                            if (logInResponse?.status == 200) {

                                resource.data?.let {
//                                    _data.value = Event(true)
                                    prefProvider.setValue(AUTH_TOKEN, it.data.authToken)
                                    prefProvider.setValue(BASE_URL_NEW, it.data.baseUrl + "/")
                                    prefProvider.setValueInt(LOCATION_ID, it.data.locationId)
                                    prefProvider.setValue(EMAIL, it.data.email)
                                    it.data.userName?.let { it1 ->
                                        prefProvider.setValue(
                                            USERNAME,
                                            it1
                                        )
                                    }
                                    //networkConnectionInterceptor.setHostBaseUrl(it.data.baseUrl + "/")

                                }

                                defaultTerminalCall(device_token)

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

    private suspend fun defaultTerminalCall(device_token: String) {

        Log.e(TERMINAL_ID, prefProvider.getValue(Constants.UNIQUE_ID, ""))
//        qwerty123
//        d219617861d4ce4b
        var unique_id = prefProvider.getValue(Constants.UNIQUE_ID, "")
        viewModelScope.launch {
            delay(1000)
            val defaultTerminal =
                userRepository.getDefaultTerminal(unique_id, device_token)
            when (defaultTerminal.status) {
                Status.SUCCESS -> {

                    defaultTerminal.data.let { terminalResponse ->
                        if (terminalResponse?.status == 200) {

                            prefProvider.setValueInt(TERMINAL_ID, terminalResponse.terminalData.id)
                            _data.value = Event(true)


                        } else {
                            _snackbarText.value = Event(defaultTerminal.message)
                        }

                    }


                }
                Status.ERROR -> {
                    _snackbarText.value = Event(defaultTerminal.message)
                    _showProgress.value = Event(false)
                }

                Status.LOADING -> {
                    _showProgress.value = Event(true)
                }
            }

        }
    }

}