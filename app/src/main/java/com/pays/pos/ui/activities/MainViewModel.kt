package com.pays.pos.ui.activities

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.pays.pos.data.model.responseModel.CashLogResponse
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.Event
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val posRepository: PosRepository,
    val prefProvider: PrefProvider

) : ViewModel() {


    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<CashLogResponse?>>()
    val data: LiveData<Event<CashLogResponse?>> = _data

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private val _logout = MutableLiveData<Event<Boolean>>()
    val logout: LiveData<Event<Boolean>> = _logout

    fun logoutAPI() {

        _showProgress.value = Event(true)
        viewModelScope.launch {
            val dataClockout = HashMap<String, String>()
            dataClockout["passcode"] = prefProvider.getValue(Constants.PASSCODE, "").toString()
            dataClockout["terminal_id"] =
                prefProvider.getValueInt(Constants.TERMINAL_ID, -1).toString()

            val resourceClockout = posRepository.employeeClockOut(dataClockout)
            when (resourceClockout.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    prefProvider.setValueboolean(Constants.IS_CLOCKOUT, true)
                    resourceClockout.data.let {
                        if (it?.status == 200) {
                            resourceClockout.data?.let {
                                callLogoutApi()
                            }
                        }

                    }

                }

                Status.ERROR -> {
                    _snackbarText.value = Event(resourceClockout.message)
                    _showProgress.value = Event(false)
                }

                Status.LOADING -> {
                    _showProgress.value = Event(true)
                }
            }


        }
    }

    private suspend fun callLogoutApi() {
        _showProgress.value = Event(true)
        val data = HashMap<String, String>()
        data["email"] =
            prefProvider.getValue(Constants.EMAIL, "").toString()
        val resource = posRepository.logout(data)
        when (resource.status) {
            Status.SUCCESS -> {
                _showProgress.value = Event(false)
                resource.data?.let { it ->

                    _logout.value = Event(true)


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

    fun clearTable() {

        viewModelScope.launch {
            posRepository.clearTable()
        }
    }

    fun updatePrintersData() {


        viewModelScope.launch {
            val resource = posRepository.syncVenueDetails()

            when (resource.status) {
                Status.SUCCESS -> {
                    Log.e("PrinterRefreshWorker", "call syncVenueDetails success")
                    resource.data.let { venueDetailsResponse ->
                        if (venueDetailsResponse?.status == 200) {

                            resource.data?.let {
                                posRepository.deleteCustomerPrinters()
                                posRepository.deleteKitchenPrinters()
                                posRepository.addKitchenPrinter(it.settingData.data.printers.kitchenPrinterList)
                                posRepository.addCustomerPrinter(it.settingData.data.printers.customerPrinterList)

                            }
                        }
                    }
                }

                Status.ERROR -> {
                    Log.e("PrinterRefreshWorker", "call syncVenueDetails error")
                }

                Status.LOADING -> {
                    Log.e("PrinterRefreshWorker", "call syncVenueDetails loading")
                }
            }
        }
    }

}