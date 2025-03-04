package com.pays.pos.ui.fragments.settings.hardware.printer

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.epson.epsonio.DevType
import com.epson.epsonio.DeviceInfo
import com.google.gson.Gson
import com.pays.pos.data.db.AppDatabase
import com.pays.pos.data.model.PrinterListModel
import com.pays.pos.data.model.requestModel.CreatePrinterRequestModel
import com.pays.pos.data.model.requestModel.OrderRequestModel
import com.pays.pos.data.model.responseModel.CreateOrderResponse
import com.pays.pos.data.model.responseModel.DeletePrinterResponseModel
import com.pays.pos.data.model.responseModel.PrinterResponse
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.BLUETOOTH
import com.pays.pos.data.remote.Constants.CUSTOMER
import com.pays.pos.data.remote.Constants.KITCHEN
import com.pays.pos.data.remote.Constants.WIFI
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.di.PrefProvider
import com.pays.pos.logger.MessageEvent
import com.pays.pos.utils.Event
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.statusUtils.Resource
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject


@HiltViewModel
class PrinterViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val appDataBase: AppDatabase,
    private val prefProvider: PrefProvider
) : ViewModel() {

    private val TAG = "PrinterViewModel"

    private val _snackbarText = MutableLiveData<Event<String?>>()
    val snackbarText: LiveData<Event<String?>> = _snackbarText

    private val _printerQueueDelete = MutableLiveData<Event<String>>()
    val printerQueueDeleteScenario: LiveData<Event<String>> = _printerQueueDelete

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private var _delete = MutableLiveData<Event<String>>()
    val deletePrinter: LiveData<Event<String>> = _delete

    private var _deleteKitchen = MutableLiveData<Event<Int>>()
    val deleteKitchenPrinter: LiveData<Event<Int>> = _deleteKitchen


    private var _printerCreated = MutableLiveData<Event<PrinterResponse.Data>>()
    val printerCreatedSucces: LiveData<Event<PrinterResponse.Data>> = _printerCreated

    private var _update = MutableLiveData<Event<String>>()
    val updatePrinter: LiveData<Event<String>> = _update

    private var _popBackStack=MutableLiveData<Event<Boolean>>()
    val popBackStack:LiveData<Event<Boolean>> = _popBackStack

    private var _localUpdatePrinter = MutableLiveData<Event<PrinterListModel>>()
    val localUpdatePrinter:LiveData<Event<PrinterListModel>> = _localUpdatePrinter


    private var _disableCompleteTouch = MutableLiveData<Event<Boolean>>()
    val disableCompleteTouch:LiveData<Event<Boolean>> = _disableCompleteTouch


    val orderTypes = posRepository.getORderTypesListDatabase()


    suspend fun deleteAllKitchenPrinters() {
        posRepository.deleteKitchenPrinters()
    }

    fun printerList(): LiveData<com.pays.pos.utils.statusUtils.Resource<List<PrinterResponse.Data.CustomerReceiptPrinters>>> {
        return posRepository.getPrinters()
    }

    fun getKitchenPrinters(): LiveData<com.pays.pos.utils.statusUtils.Resource<List<PrinterResponse.Data.KitchenReceiptPrinters>>> {
        return posRepository.getKitchenPrinters()
    }

     fun getKitchenPrintersList(): LiveData<Resource<List<PrinterResponse.Data.KitchenReceiptPrinters>>> {
        return posRepository.getKitchenPrinters()
    }

   suspend fun getKitchenPrinterForPrint(): List<PrinterResponse.Data.KitchenReceiptPrinters> {
        return posRepository.getKitchenPrinterForPrint()
    }


    fun createPrinterQueueTestOrder(orderRequest: OrderRequestModel) {
        _showProgress.value = Event(true)

        if (orderRequest.order.deliveryType.equals("null")){
            orderRequest.order.deliveryType=""
        }

        viewModelScope.launch {
            EventBus.getDefault()
                .post(MessageEvent("${Constants.LINE_BREAK_TAB} PrinterViewModel.kt_createPrinterQueueTestOrder ${Gson().toJson(orderRequest)}"))

            val resource: com.pays.pos.utils.statusUtils.Resource<CreateOrderResponse> =
                posRepository.createOrder(orderRequest)

            when (resource.status) {
                Status.LOADING -> {

                    _showProgress.value = Event(true)
                }
                Status.ERROR -> {
                    EventBus.getDefault()
                        .post(MessageEvent("${Constants.LINE_BREAK_TAB} PrinterViewModel.kt_createPrinterQueueTestOrder_ERROR"))

                    _snackbarText.value = Event(resource.message)
                    _showProgress.value = Event(false)

                }
                Status.SUCCESS -> {

                    _showProgress.value = Event(false)
                    EventBus.getDefault()
                        .post(MessageEvent("${Constants.LINE_BREAK_TAB} PrinterViewModel.kt_createPrinterQueueTestOrder_SUCCESS"))

                }
            }

        }

    }

    fun updatePrinterStatus(type: String, id: Int, terminal_id: Int, status: Boolean) {
        _showProgress.value = Event(true)
        LogUtil.logE(TAG, "PrinterType: ${type}")
        viewModelScope.launch {
            val resource: com.pays.pos.utils.statusUtils.Resource<DeletePrinterResponseModel> =
                when (type) {
                    KITCHEN -> posRepository.updatePrinterStatusKitchen(id, terminal_id, status)

                    CUSTOMER -> posRepository.updatePrinterStatusCustomer(id, terminal_id, status)

                    else -> posRepository.updatePrinterStatus(id, terminal_id, status)
                }

            when (resource.status) {
                Status.LOADING -> {

                    _showProgress.value = Event(true)
                }

                Status.ERROR -> {
                    _snackbarText.value = Event(resource.message.toString())
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

    fun updatePrinter(id: Int, model: CreatePrinterRequestModel,printerListModel: PrinterListModel) {
        _showProgress.value = Event(true)
        viewModelScope.launch {

            val resource: com.pays.pos.utils.statusUtils.Resource<DeletePrinterResponseModel> =
                posRepository.updatePrinter(id, model)
            when (resource.status) {
                Status.SUCCESS -> {
                    //syncSettingModule()
                    _showProgress.value = Event(false)

                   // _localUpdatePrinter.value = Event(printerListModel)
                    if (printerListModel.currentPrinterType == KITCHEN){

                        var kitchenPrinter = PrinterResponse.Data.KitchenReceiptPrinters(
                            id = printerListModel.id ?: 0,
                            name = printerListModel.printerName?:"",
                            modalName = printerListModel?.modelName ?:"",
                            printer_type = printerListModel.connectionType,
                            status = printerListModel.isActive,
                            receiptPrintType = printerListModel.type,
                            isCashDrawerOpen = true,
                            locationId = prefProvider.getLocationId(),
                            createdAt = "",
                            updatedAt = "",
                            ipAddress = printerListModel.deviceModel?.ipAddress,
                            unpaidReceiptAutoPrinting =true,
                            isReportPrintEnable = true,
                            isAutomaticTwoCustomerReceipt = false,
                            printerCategories = printerListModel.printerCategories?: arrayListOf(),
                            terminalIds = listOf(),
                            unpaidReceiptAutoPrintTerminalIds = "",
                            orderTypes = printerListModel.printerModel?: arrayListOf(),
                            isDeleted = false,
                            macAddress = printerListModel.deviceModel?.macAddress?:"",
                            kitchenStatus = printerListModel.isKitchenActive,
                            customerStatus = printerListModel.isCustomerActive


                        )



                        appDataBase.printerDao().updateKitchenPrinter(kitchenPrinter)
                    }
                    else{

                        var customerPrinter = PrinterResponse.Data.CustomerReceiptPrinters(
                            id = printerListModel.id ?: 0,
                            name = printerListModel.printerName?:"",
                            modalName = printerListModel?.modelName ?:"",
                            printer_type = printerListModel.connectionType,
                            status = printerListModel.isActive,
                            receiptPrintType = printerListModel.type,
                            isCashDrawerOpen = true,
                            locationId = prefProvider.getLocationId(),
                            createdAt = "",
                            updatedAt = "",
                            ipAddress = printerListModel.deviceModel?.ipAddress,
                            unpaidReceiptAutoPrinting =true,
                            isReportPrintEnable = true,
                            isAutomaticTwoCustomerReceipt = false,
                            printerCategories = printerListModel.printerCategories?: arrayListOf(),
                            terminalIds = listOf(),
                            unpaidReceiptAutoPrintTerminalIds = "",
                            orderTypes = printerListModel.printerModel?: arrayListOf(),
                            isDeleted = false,
                            macAddress = printerListModel.deviceModel?.macAddress?:"",
                            kitchenStatus = printerListModel.isKitchenActive,
                            customerStatus = printerListModel.isCustomerActive



                        )

                        appDataBase.printerDao().updateCustomerPrinter(customerPrinter)
                    }

                    _update.value = Event(resource.data?.message!!)

                }
                Status.ERROR -> {
                    if (resource.message?.contains("Couldn't find PrinterSetting with")?:false){
                        _popBackStack.value = Event(true)
                    }
                    _snackbarText.value = Event(resource.message)
                    _showProgress.value = Event(false)

                }
                Status.LOADING -> {
                    _showProgress.value = Event(true)

                }

            }
        }

    }

    fun deletePrinter(printerListModel: PrinterListModel, status: String? = null) {
        /*  Log.d("innerPrinterKitchen", "6th stage")
          Log.d("innerPrinterKitchen", status!!)
  */

        _showProgress.value = Event(true)
        viewModelScope.launch {
            val resource: com.pays.pos.utils.statusUtils.Resource<DeletePrinterResponseModel> =
                posRepository.deletePrinter(printerListModel.id!!, status)

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    if (status != null) {
                        Log.e(TAG,"checkStatus:  ${status}")
                        if (status.lowercase() == Constants.KITCHEN.lowercase()) {
                            Log.e("PrinterDelete", "Printer Inner ID: ${printerListModel.id}")
                            posRepository.deleteCustomerPrinter(printerListModel.id)
                        } else if (status.lowercase() == Constants.CUSTOMER.lowercase()) {
                            Log.e("PrinterDelete", "Printer ID: ${printerListModel.id}")

                            posRepository.deleteKitchenPrinter(printerListModel.id)
                        } else {
                            Log.e("PrinterDeleteElse", "Printer Else ID: ${printerListModel.id}")
                           // posRepository.deleteCustomerPrinter(printerListModel.id)
                        }

                    } else {

                        if (resource.data?.data?.receiptPrintType == Constants.KITCHEN) {
                            posRepository.deleteKitchenPrinter(printerListModel.id)
                        } else if (resource.data?.data?.receiptPrintType == Constants.CUSTOMER) {
                            posRepository.deleteCustomerPrinter(printerListModel.id)
                        }
                    }
                    printerList()
                    _delete.value = Event("Printer_deleted")
                }
                Status.ERROR -> {
                    _snackbarText.value = Event(resource.message)

                    posRepository.deleteKitchenPrinter(printerListModel.id)

                    Log.e(
                        "checkPrinterQueueDelete",
                        "messageResource   ${resource.message.toString()}"
                    )
                    _printerQueueDelete.value = Event(resource.message.toString())
                    _showProgress.value = Event(false)

                }
                Status.LOADING -> {
                    _showProgress.value = Event(true)


                }

            }
        }
    }

    fun createPrinter(data: CreatePrinterRequestModel, showLoader: Boolean = true) {

        if (showLoader) {
            _showProgress.value = Event(true)
        }

        _disableCompleteTouch.postValue(Event(false))
        viewModelScope.launch {
            val resource: com.pays.pos.utils.statusUtils.Resource<PrinterResponse> =
                posRepository.createPrinter(data)


            when (resource.status) {
                Status.LOADING -> {
                    if (showLoader) {
                        _showProgress.value = Event(true)
                    }

                }
                Status.ERROR -> {
                    _snackbarText.value = Event(resource.message)
                    _showProgress.value = Event(false)
                    _disableCompleteTouch.postValue(Event(true))
                }
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data?.data?.let {
                        _printerCreated.value = Event(it)
                    }
                    printerList()
                    _disableCompleteTouch.postValue(Event(true))
                }

            }
        }

    }


    private fun syncSettingModule(isFromUpdate: Boolean = false) {
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

                                try {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        runBlocking {
                                            var printOrderId=posRepository.getLabelPrinterSettingsData().printOrderId
                                            posRepository.insertOrUpdateLabelPrinter(it.settingData.data.oneItemPerReciept,printOrderId)
                                        }
                                    }
                                } catch (e: Exception) {

                                }


                                if (isFromUpdate) {
                                    Printer.updatePrinter?.reloadAdapter()
                                }

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

    fun deleteKitchenPrinter(id: Int) {
        viewModelScope.launch {
            posRepository.deleteKitchenPrinter(id)

        }

    }

    fun updatePrinter() {
        syncSettingModule(isFromUpdate = true)
    }

    fun deleteAllCustomerPrinters() {
        viewModelScope.launch {
            posRepository.deleteCustomerPrinters()
        }
    }


}

