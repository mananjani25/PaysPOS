package com.android.pos.ui.fragments.loginscreen

import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.entities.UserSwapModel
import com.android.pos.data.model.responseModel.BaseResponse
import com.android.pos.data.remote.Constants.EMPLOYEE_ID
import com.android.pos.data.remote.Constants.EMPLOYEE_NAME
import com.android.pos.data.remote.Constants.EMPLOYEE_ROLE
import com.android.pos.data.remote.Constants.EMPLOYEE_ROLE_ID
import com.android.pos.data.remote.Constants.IS_CLOCKOUT
import com.android.pos.data.remote.Constants.PASSCODE
import com.android.pos.data.remote.Constants.TERMINAL_ID
import com.android.pos.data.repositories.UserRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.sql.Timestamp
import javax.inject.Inject

@HiltViewModel
class PasscodeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val prefProvider: PrefProvider
) :
    ViewModel() {

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<BaseResponse>>()
    val data: LiveData<Event<BaseResponse>> = _data


    public val _userList = MutableLiveData<Event<List<UserSwapModel>>>()
    val userList: LiveData<Event<List<UserSwapModel>>> = _userList

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private var isDashboard: Boolean = false

    fun isDashboardData(isDashboard: Boolean) {
        this.isDashboard = isDashboard
    }

    fun getAllUserClockInData() {
        viewModelScope.launch {
            var userListtemp: List<UserSwapModel> = arrayListOf()
            userListtemp =
                userRepository.userClockInData()
            _userList.value = Event(userListtemp)
        }
    }

    fun submit(passcode: String) {
        _showProgress.value = Event(true)
        if (isDashboard) {
            val data = HashMap<String, String>()
            data["passcode"] = passcode
            data["terminal_id"] = prefProvider.getValueInt(TERMINAL_ID, -1).toString()

            viewModelScope.launch {
                val resource = userRepository.employeeClockOut(data)
                when (resource.status) {
                    Status.SUCCESS -> {
                        prefProvider.setValueboolean(IS_CLOCKOUT, false)
                        _showProgress.value = Event(false)

                        resource.data.let {
                            if (it?.status == 200) {
                                resource.data?.let {
                                    if (prefProvider.getValueInt(EMPLOYEE_ID, 0) != 0) {
                                        var employeeId = prefProvider.getValueInt(EMPLOYEE_ID, 0)
                                        userRepository.removeUserClockInData(employeeId)
                                    }
                                    _data.value = Event(it)
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
        } else {
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
                                    var date_time = ""
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        date_time = Timestamp(System.currentTimeMillis()).toString()
                                    }

                                    userRepository.addUserClockInData(

                                        UserSwapModel(
                                            it.data.employeeId,
                                            it.data.employee_name,
                                            it.data.employee_role.toString(),
                                            it.data.team_role_id!!,
                                            date_time,
                                            passcode
                                        )
                                    )

                                    prefProvider.setValueboolean(IS_CLOCKOUT, true)
                                    prefProvider.setValueInt(EMPLOYEE_ID, it.data.employeeId)
                                    prefProvider.setValue(EMPLOYEE_NAME, it.data.employee_name)
                                    it.data.employee_role?.let { it1 ->
                                        prefProvider.setValue(
                                            EMPLOYEE_ROLE,
                                            it1
                                        )
                                    }
                                    prefProvider.setValueInt(
                                        EMPLOYEE_ROLE_ID,
                                        it.data.team_role_id ?: 0
                                    )
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

    }

    private suspend fun employeeLogin(clockinData: HashMap<String, String>) {
        val employeeLogin = userRepository.employeeLogIn(clockinData)

        when (employeeLogin.status) {
            Status.SUCCESS -> {
                _showProgress.value = Event(false)

                employeeLogin.data.let {
                    if (it?.status == 200) {
                        employeeLogin.data?.let {
                            _data.value = Event(it)

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
