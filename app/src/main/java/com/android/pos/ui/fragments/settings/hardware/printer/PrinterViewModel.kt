package com.android.pos.ui.fragments.settings.hardware.printer

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.db.AppDatabase
import com.android.pos.data.model.requestModel.CreatePrinterRequestModel
import com.android.pos.data.model.requestModel.OrderRequestModel
import com.android.pos.data.model.responseModel.CreateOrderResponse
import com.android.pos.data.model.responseModel.DeletePrinterResponseModel
import com.android.pos.data.model.responseModel.PrinterResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.repositories.PosRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.LogUtil
import com.android.pos.utils.statusUtils.Status
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class PrinterViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val appDataBase: AppDatabase,
    private val prefProvider: PrefProvider
) : ViewModel() {

    private val TAG = "PrinterViewModel"

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _printerQueueDelete = MutableLiveData<Event<String>>()
    val printerQueueDeleteScenario:LiveData<Event<String>> = _printerQueueDelete

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private var _delete = MutableLiveData<Event<String>>()
    val deletePrinter: LiveData<Event<String>> = _delete

    private var _printerCreated = MutableLiveData<Event<PrinterResponse.Data>>()
    val printerCreatedSucces:LiveData<Event<PrinterResponse.Data>> = _printerCreated

    private var _update = MutableLiveData<Event<String>>()
    val updatePrinter: LiveData<Event<String>> = _update

    val orderTypes = posRepository.getORderTypesListDatabase()


    fun printerList(): LiveData<com.android.pos.utils.statusUtils.Resource<List<PrinterResponse.Data.CustomerReceiptPrinters>>> {
        return posRepository.getPrinters()
    }

    fun getKitchenPrinters(): LiveData<com.android.pos.utils.statusUtils.Resource<List<PrinterResponse.Data.KitchenReceiptPrinters>>> {
        return posRepository.getKitchenPrinters()
    }

    suspend fun getKitchenPrintersList(): List<PrinterResponse.Data.KitchenReceiptPrinters> {
        return posRepository.getKitchenPrintersList()
    }


    fun createPrinterQueueTestOrder(orderRequest: OrderRequestModel){

        _showProgress.value = Event(true)
        viewModelScope.launch {

            val resource: com.android.pos.utils.statusUtils.Resource<CreateOrderResponse> =
                posRepository.createOrder(orderRequest)

            when (resource.status) {
                Status.LOADING -> {

                    _showProgress.value = Event(true)
                }
                Status.ERROR -> {
                    _snackbarText.value = Event(resource.message)
                    _showProgress.value = Event(false)

                }
                Status.SUCCESS -> {

                    _showProgress.value = Event(false)
                }
            }

        }

    }

    fun updatePrinterStatus(type: String, id: Int, terminal_id: Int, status: Boolean) {
        _showProgress.value = Event(true)
        LogUtil.logE(TAG, "PrinterType: ${type}")
        viewModelScope.launch {
            val resource: com.android.pos.utils.statusUtils.Resource<DeletePrinterResponseModel> =
                posRepository.updatePrinterStatus(id, terminal_id, status)

            when (resource.status) {
                Status.LOADING -> {

                    _showProgress.value = Event(true)
                }
                Status.ERROR -> {
                    _snackbarText.value = Event(resource.message)
                    _showProgress.value = Event(false)

                }
                Status.SUCCESS -> {
                    if (type == Constants.KITCHEN) {
                        posRepository.updateKitchenPrinterStatus(status, id)

                    } else if (type == Constants.CUSTOMER) {
                        posRepository.updateCustomerPrinterStatus(status, id)
                    }

                    _showProgress.value = Event(false)
                }
            }

        }


    }

    fun updatePrinter(id: Int, model: CreatePrinterRequestModel) {
        _showProgress.value = Event(true)
        viewModelScope.launch {

            val resource: com.android.pos.utils.statusUtils.Resource<DeletePrinterResponseModel> =
                posRepository.updatePrinter(id, model)
            when (resource.status) {
                Status.SUCCESS -> {
                    syncSettingModule()
                    _showProgress.value = Event(false)
                    _update.value = Event(resource.data?.message!!)


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

    fun deletePrinter(id: Int, status: String? = null) {
        _showProgress.value = Event(true)
        viewModelScope.launch {
            val resource: com.android.pos.utils.statusUtils.Resource<DeletePrinterResponseModel> =
                posRepository.deletePrinter(id, status)

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    if (status != null) {
                        if (status.lowercase() == Constants.KITCHEN.lowercase()) {
                            posRepository.deleteCustomerPrinter(id)
                        } else {
                            posRepository.deleteKitchenPrinter(id)
                        }

                    } else {

                        if (resource.data?.data?.receiptPrintType == Constants.KITCHEN) {

                            posRepository.deleteKitchenPrinter(id)
                        } else if (resource.data?.data?.receiptPrintType == Constants.CUSTOMER) {
                            posRepository.deleteCustomerPrinter(id)
                        }
                    }
                    printerList()
                    _delete.value = Event("Printer_deleted")
                }
                Status.ERROR -> {
                    _snackbarText.value = Event(resource.message)
                    Log.e("checkPrinterQueueDelete","messageResource   ${resource.message.toString()}")
                    _printerQueueDelete.value = Event(resource.message.toString())
                    _showProgress.value = Event(false)

                }
                Status.LOADING -> {
                    _showProgress.value = Event(true)


                }

            }
        }
    }

    fun createPrinter(data: CreatePrinterRequestModel) {

        _showProgress.value = Event(true)


        viewModelScope.launch {
            val resource: com.android.pos.utils.statusUtils.Resource<PrinterResponse> =
                posRepository.createPrinter(data)


            when (resource.status) {
                Status.LOADING -> {
                    _showProgress.value = Event(true)

                }
                Status.ERROR -> {
                    _snackbarText.value = Event(resource.message)
                    _showProgress.value = Event(false)

                }
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data?.data?.let {
                        _printerCreated.value = Event(it)
                    }
                    printerList()

                }

            }
        }

    }


    private fun syncSettingModule() {
        viewModelScope.launch {
            val resource = posRepository.syncVenueDetails()

            when (resource.status) {
                Status.SUCCESS -> {

                    resource.data.let { venueDetailsResponse ->
                        if (venueDetailsResponse?.status == 200) {

                            resource.data?.let {
                                posRepository.deleteCustomerPrinters()
                                posRepository.deleteKitchenPrinters()
                                posRepository.addKitchenPrinter(it.settingData.data.printers.kitchenPrinterList)
                                posRepository.addCustomerPrinter(it.settingData.data.printers.customerPrinterList)
                            }
                            _showProgress.value = Event(false)
                            prefProvider.setValueboolean(Constants.SYNC_DATA, true)
                            resource.data?.settingData?.timeStamp?.let {
                                prefProvider.setValue(
                                    Constants.SYNC_SETTING_TIME_STAMP,
                                    it
                                )
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

