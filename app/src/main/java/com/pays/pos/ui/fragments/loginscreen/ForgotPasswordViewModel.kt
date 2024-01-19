package com.pays.pos.ui.fragments.loginscreen

import android.text.TextUtils
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pays.pos.R
import com.pays.pos.data.model.requestModel.LoginRequestModel
import com.pays.pos.data.remote.Constants.AUTH_TOKEN
import com.pays.pos.data.remote.Constants.BASE_URL_NEW
import com.pays.pos.data.remote.Constants.EMAIL
import com.pays.pos.data.remote.Constants.LOCATION_ID
import com.pays.pos.data.remote.Constants.TERMINAL_ID
import com.pays.pos.data.remote.Constants.USERNAME
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.data.repositories.UserRepository
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.Event
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val prefProvider: PrefProvider
) :
    ViewModel() {

    val loginDetails = MutableLiveData(LoginRequestModel())

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<String?>>()
    val data: LiveData<Event<String?>> = _data

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress


    fun submit() {

        if (TextUtils.isEmpty(loginDetails.value?.emailAddress?.trim())) {
            _snackbarText.value = Event(R.string.email_validate)
        } else if (!Patterns.EMAIL_ADDRESS.matcher(loginDetails.value?.emailAddress?.trim())
                .matches()
        ) {
            _snackbarText.value = Event(R.string.valid_email_validate)
        } else {
            _showProgress.value = Event(true)

            val data = HashMap<String, String>()
            data["email"] = loginDetails.value?.emailAddress.toString()

            viewModelScope.launch {
                val resource = userRepository.forgotPassword(data)
                when (resource.status) {
                    Status.SUCCESS -> {
                        _showProgress.value = Event(false)
                        resource.data.let { logInResponse ->
                            if (logInResponse?.status == 200) {

                                resource.data?.let {
                                    _data.value = Event(logInResponse.message)

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