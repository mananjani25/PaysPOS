package com.pays.pos.ui.fragments.loginscreen

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pays.pos.data.remote.Constants.IS_CLOCKOUT
import com.pays.pos.data.remote.Constants.PASSCODE
import com.pays.pos.data.remote.Constants.TERMINAL_ID
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.data.repositories.UserRepository
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.Event
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClockInOwnerViewModel @Inject constructor(
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

    fun submit() {
        _showProgress.value = Event(true)

        val data = HashMap<String, String>()
        data["passcode"] = prefProvider.getValue(PASSCODE, "").toString()
        data["terminal_id"] = prefProvider.getValueInt(TERMINAL_ID, -1).toString()

        viewModelScope.launch {
            val resource = userRepository.employeeClockOut(data)
            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)

                    resource.data.let {
                        if (it?.status == 200) {
                            resource.data?.let {
                                prefProvider.setValueboolean(IS_CLOCKOUT, false)
                                _data.value = Event(true)

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