package com.pays.pos.ui.fragments.loginscreen

import android.text.TextUtils
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pays.pos.R
import com.pays.pos.data.entities.TbCustomer
import com.pays.pos.data.model.requestModel.CreateCustomerRequestModel
import com.pays.pos.data.model.requestModel.LoginRequestModel
import com.pays.pos.data.model.responseModel.LogInResponse
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.AUTH_TOKEN
import com.pays.pos.data.remote.Constants.BASE_URL_NEW
import com.pays.pos.data.remote.Constants.EMAIL
import com.pays.pos.data.remote.Constants.LOCATION_ID
import com.pays.pos.data.remote.Constants.LOCATION_NAME
import com.pays.pos.data.remote.Constants.TERMINAL_ID
import com.pays.pos.data.remote.Constants.TERMINAL_NAME
import com.pays.pos.data.remote.Constants.USERNAME
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.data.repositories.UserRepository
import com.pays.pos.di.HostSelectionInterceptor
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.Event
import com.pays.pos.utils.statusUtils.Status
import com.testfairy.TestFairy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val posRepository: PosRepository,
    private val prefProvider: PrefProvider,
    private val hostSelectionInterceptor: HostSelectionInterceptor
) :
    ViewModel() {

    val loginDetails = MutableLiveData(LoginRequestModel())

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<Boolean?>>()
    val data: LiveData<Event<Boolean?>> = _data

    private val _dataCustomer = MutableLiveData<Event<TbCustomer>>()
    val dataCustomer: LiveData<Event<TbCustomer>> = _dataCustomer

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
                                    prefProvider.setValue(BASE_URL_NEW, it.data.baseUrl + "/")
                                    hostSelectionInterceptor.setHostBaseUrl()

                                    TestFairy.setUserId(
                                        loginDetails.value?.emailAddress.toString().trim()
                                    );

                                    defaultTerminalCall(device_token, it.data)
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

    suspend fun defaultTerminalCall(device_token: String, data: LogInResponse.Data) {
        _showProgress.value = Event(true)

        val unique_id = prefProvider.getUniqueId()
        viewModelScope.launch {

            val defaultTerminal =
                userRepository.getDefaultTerminal(unique_id, device_token)
            when (defaultTerminal.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    defaultTerminal.data.let { terminalResponse ->
                        if (terminalResponse?.status == 200) {

                            prefProvider.setValue(AUTH_TOKEN, data.authToken)


                            prefProvider.setValueInt(LOCATION_ID, data.locationId)
                            prefProvider.setValue(EMAIL, data.email)
                            data.userName?.let { it1 ->
                                prefProvider.setValue(
                                    USERNAME,
                                    it1
                                )
                            }
                            prefProvider.setValueboolean(
                                Constants.ONLINE_ORDER_ENABLE,
                                terminalResponse.terminalData.enabled_for_receiving_web_order!!
                            )
                            prefProvider.setValueInt(TERMINAL_ID, terminalResponse.terminalData.id)
                            prefProvider.setValue(TERMINAL_NAME, terminalResponse.terminalData.name)
                            _data.value = Event(true)


                        } else {
                            _snackbarText.value = Event(defaultTerminal.message)
                            prefProvider.setValue(AUTH_TOKEN, "")
                            _data.value = Event(false)
                        }

                    }


                }

                Status.ERROR -> {
                    _snackbarText.value = Event(defaultTerminal.message)
                    _showProgress.value = Event(false)
                    prefProvider.setValue(AUTH_TOKEN, "")
                    _data.value = Event(false)
                }

                Status.LOADING -> {
                    _showProgress.value = Event(true)
                }
            }

        }
    }

    fun createCustomer(addCustomerData: CreateCustomerRequestModel, customerID: Int = -1, isEdit: Boolean = false) {

        _showProgress.value = Event(true)

        viewModelScope.launch {
            val resource = if (isEdit) {
                posRepository.updateCustomer(customerID, addCustomerData)
            } else {

                posRepository.createCustomer(addCustomerData)
            }
            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)

                    resource.data.let {
                        if (it?.status == 200) {

                            resource.data?.let { customerListReposne ->

                                val model = TbCustomer(
                                    id = customerListReposne.data.id,
                                    first_name = customerListReposne.data.first_name,
                                    last_name = customerListReposne.data.last_name,
                                    birth_date = customerListReposne.data.birth_date,
                                    email = customerListReposne.data.email,
                                    phones = customerListReposne.data.phones,
                                    addresses = customerListReposne.data.addresses,
                                    enroll_to_loyalty = customerListReposne.data.enroll_to_loyalty,
                                    same_as_billing_address = customerListReposne.data.same_as_billing_address,
                                    final_reward = customerListReposne.data.final_reward,
                                    company = customerListReposne.data.company,
                                    isSelcted = true,
                                )


                                posRepository.addCustomer(model)

                                customerListReposne.data.id?.let { it1 ->
                                    prefProvider.setValueInt(Constants.CUSTOMER_ID,
                                        it1
                                    )
                                }

                                _dataCustomer.value = Event(model)

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
