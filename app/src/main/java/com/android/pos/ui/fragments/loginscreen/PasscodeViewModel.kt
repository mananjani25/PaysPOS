package com.android.pos.ui.fragments.loginscreen

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.db.AppDatabase
import com.android.pos.data.model.responseModel.BaseResponse
import com.android.pos.data.model.responseModel.TimeDetailsResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.EMPLOYEE_ID
import com.android.pos.data.remote.Constants.EMPLOYEE_NAME
import com.android.pos.data.remote.Constants.EMPLOYEE_ROLE
import com.android.pos.data.remote.Constants.EMPLOYEE_ROLE_ID
import com.android.pos.data.remote.Constants.IS_CLOCKOUT
import com.android.pos.data.remote.Constants.PASSCODE
import com.android.pos.data.remote.Constants.TERMINAL_ID
import com.android.pos.data.repositories.PosRepository
import com.android.pos.data.repositories.UserRepository
import com.android.pos.di.PrefProvider
import com.android.pos.di.RolePermission
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Status
import com.android.pos.utils.workmanager.ThreadPoolManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PasscodeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val posRepository: PosRepository,
    private val prefProvider: PrefProvider,
    private val appDatabase: AppDatabase,
    private val rolePermission: RolePermission
) :
    ViewModel() {

    private val _snackbarText = MutableLiveData<Event<String>>()
    val snackbarText: LiveData<Event<String>> = _snackbarText

    private val _data = MutableLiveData<Event<BaseResponse>>()
    val data: LiveData<Event<BaseResponse>> = _data

    private val _timeData = MutableLiveData<Event<TimeDetailsResponse>>()
    val timeData: LiveData<Event<TimeDetailsResponse>> = _timeData

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private var isDashboard: Boolean = false


    private val _data1 = MutableLiveData<Event<Boolean>>()
    val data1: LiveData<Event<Boolean>> = _data1

    fun isDashboardData(isDashboard: Boolean) {
        this.isDashboard = isDashboard
    }


    fun deleteCart() {
        viewModelScope.launch {
            posRepository.deleteAllCart()
        }
    }

    fun getTimeDetails(terminalId: Int){
        _showProgress.value = Event(true)
        viewModelScope.launch {
            val timeDetails =
                posRepository.timeDetails(terminalId)
            when (timeDetails.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    timeDetails.data.let { timeResponse ->
                        if (timeResponse?.status == 200) {
                            _timeData.value = Event(timeResponse)
                            prefProvider.setValue(Constants.TERMINAL_NAME,timeResponse.data.terminalName)
                        } else {
                            _data1.value = Event(false)
                            _snackbarText.value = Event(timeDetails.message.toString())
                        }
                    }
                }
                Status.ERROR -> {
                    _snackbarText.value = Event(timeDetails.message.toString())
                    _showProgress.value = Event(false)
                }
                Status.LOADING -> {
                    _showProgress.value = Event(true)
                }
            }

        }
    }

    fun defaultTerminalCall(device_token: String, deviceId: String) {
        _showProgress.value = Event(true)
        viewModelScope.launch {

            val defaultTerminal =
                userRepository.getDefaultTerminal(deviceId, device_token)
            when (defaultTerminal.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    defaultTerminal.data.let { terminalResponse ->
                        if (terminalResponse?.status == 200) {

                            prefProvider.setValueInt(TERMINAL_ID, terminalResponse.terminalData.id)

                        } else {
                            _data1.value = Event(false)
                            _snackbarText.value = Event(defaultTerminal.message.toString())
                        }

                    }


                }
                Status.ERROR -> {
                    prefProvider.setValue(Constants.AUTH_TOKEN, "")
                    _data1.value = Event(false)
                    _snackbarText.value = Event(defaultTerminal.message.toString())
                    _showProgress.value = Event(false)

                }

                Status.LOADING -> {
                    _showProgress.value = Event(true)
                }
            }

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
                                    _data.value = Event(it)
                                }
                            } else {
                                _snackbarText.value = Event(resource.message.toString())
                            }

                        }

                    }

                    Status.ERROR -> {
                        _snackbarText.value = Event(resource.message.toString())
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

                                    if (prefProvider.getValueInt(
                                            EMPLOYEE_ID,
                                            -1
                                        ) != it.data.employeeId
                                    ) {


                                        prefProvider.setValueboolean(
                                            Constants.IS_UPDATE_ORDER,
                                            false
                                        )
                                        prefProvider.setValueInt(Constants.IS_UPDATE_ORDER_ID, -1)
                                        prefProvider.setValueInt(
                                            Constants.IS_UPDATE_ORDER_PAYMENT_ID,
                                            -1
                                        )
                                        prefProvider.setValue(
                                            Constants.IS_UPDATE_ORDER_PAY_OFFLINE_ID,
                                            ""
                                        )
                                        prefProvider.setValue(
                                            Constants.IS_UPDATE_ORDER_OFFLINE_ID,
                                            ""
                                        )
                                        prefProvider.setValueboolean(
                                            Constants.IS_UPDATE_ORDER_FROM_ACTIVE_ORDER,
                                            false
                                        )
                                        prefProvider.setValueboolean(
                                            Constants.IS_UPDATE_ORDER_LOYALTY_APPLIED,
                                            false
                                        )

                                        prefProvider.setValue(Constants.CUSTOMER_NAME, "")
                                        prefProvider.setValue(Constants.PREF_CUSTOMER, "")
                                        prefProvider.setValueInt(Constants.CUSTOMER_ID, -1)
                                        deleteCart()

                                    }

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
                                    prefProvider.setValueboolean("clockOutFromNoti", false)
                                    prefProvider.setValue(PASSCODE, passcode)
                                    ThreadPoolManager.instance.executeTask(Runnable {

                                        rolePermission.findCurrentUserRoleAndSave(
                                            appDatabase.teamRoleDao().allRoleList()
                                        )


                                    })


                                    //prefProvider.setValueboolean(Constants.SYNC_DATA, false)
                                    employeeLogin(data)

                                }
                            } else {
                                _snackbarText.value = Event(resource.message.toString())
                            }

                        }


                    }

                    Status.ERROR -> {
                        _snackbarText.value = Event(resource.message.toString())
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
                        _snackbarText.value = Event(employeeLogin.message.toString())
                    }

                }
            }

            Status.ERROR -> {
                _snackbarText.value = Event(employeeLogin.message.toString())
                _showProgress.value = Event(false)
            }

            Status.LOADING -> {
                _showProgress.value = Event(true)
            }
        }
    }
}
