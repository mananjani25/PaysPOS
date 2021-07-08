package com.android.pos.ui.fragments.loginscreen

import android.text.TextUtils
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.R
import com.android.pos.data.model.requestModel.LoginRequestModel
import com.android.pos.data.repositories.PosRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class LoginViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val prefProvider: PrefProvider
) :
    ViewModel() {

    val loginDetails = MutableLiveData(LoginRequestModel())

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    fun submit() {

//        if (TextUtils.isEmpty(loginDetails.value?.emailAddress?.trim())) {
//            _snackbarText.value = Event(R.string.email_validate)
//        } else if (!Patterns.EMAIL_ADDRESS.matcher(loginDetails.value?.emailAddress?.trim())
//                .matches()
//        ) {
//            _snackbarText.value = Event(R.string.valid_email_validate)
//        } else if (TextUtils.isEmpty(loginDetails.value?.password?.trim())) {
//            _snackbarText.value = Event(R.string.password_validate)
//        } else {
//            _showProgress.value = Event(true)
//
//            val data = HashMap<String, String>()
//             data["phone_number"] = loginDetails.value?.emailAddress.toString()
//             data["country_code"] = "+91"
//
//            viewModelScope.launch {
//                val resource = posRepository.sendOtp(data)
//                when (resource.status) {
//                    Status.SUCCESS -> {
//                        _showProgress.value = Event(false)
//                        resource.data?.let {
//                            _snackbarText.value = Event(it.message)
//                        }
//                    }
//
//                    Status.ERROR -> {
//                        _snackbarText.value = Event(resource.message)
//                        _showProgress.value = Event(false)
//                    }
//
//                    Status.LOADING -> {
//                        _showProgress.value = Event(true)
//                    }
//                }
//            }
//
//        }

    }
}