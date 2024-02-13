package com.pays.pos.ui.fragments.payment

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pays.pos.R
import com.pays.pos.data.db.AppDatabase
import com.pays.pos.data.model.PrinterQueueModel
import com.pays.pos.data.model.SplitDetailListModel
import com.pays.pos.data.model.requestModel.CreateNoteRequest
import com.pays.pos.data.model.responseModel.BaseResponse
import com.pays.pos.data.model.responseModel.CustomerAssignedResponse
import com.pays.pos.data.model.responseModel.PrinterResponse
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.EMAIL
import com.pays.pos.data.remote.Constants.END_DATE
import com.pays.pos.data.remote.Constants.START_DATE
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.Event
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.statusUtils.Resource
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class OrderCompleteViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val prefProvider: PrefProvider,
    private val appDatabase: AppDatabase
) : ViewModel() {

    val createNoteDetails = MutableLiveData(CreateNoteRequest())

    var itsFromETP = false

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<BaseResponse?>>()
    val data: LiveData<Event<BaseResponse?>> = _data

    private val _data1 = MutableLiveData<Event<CustomerAssignedResponse?>>()
    val data1: LiveData<Event<CustomerAssignedResponse?>> = _data1

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private lateinit var resource: Resource<BaseResponse>

    fun getTipsList() = posRepository.getTipsList()

    fun getCustomerReceiptSettings() = posRepository.getCustomerReceiptSettings()

    fun getKitchenReceiptSettings() = posRepository.getKitchenReceiptSettings()

    fun getPrinterQueueData() = posRepository.getPrinterQueueData()

    suspend fun addPrinterQueueData(queueData: PrinterQueueModel) =
        posRepository.addPrinterQueueData(queueData)

    fun checkQueueExist(id: Int) = posRepository.checkQueueExist(id)


    fun sendMailForETS(email: String, startDate: String, endDate: String){

        // for listing -> reports/employee_tip_summary"

        val data = HashMap<String, String>()
        data[EMAIL] = email
        data[START_DATE] = startDate
        data[END_DATE] = endDate

        if (email.isEmpty()) {
            _snackbarText.value = Event(R.string.email_validate)
        } else {

            _showProgress.value = Event(true)

            viewModelScope.launch {

                resource =  posRepository.emailReceiptForETS(data)

                when (resource.status) {
                    Status.SUCCESS -> {
                        itsFromETP = true
                        _showProgress.value = Event(false)
                        resource.data.let { baseResponse ->
                            if (baseResponse?.status == 200) {

                                resource.data?.let { response ->

                                    _data.postValue(Event(response))

                                }
                            } else {
                                _snackbarText.postValue(Event(resource.message))
                            }
                        }
                    }

                    Status.ERROR -> {
                        itsFromETP = true
                        _snackbarText.postValue(Event(resource.message))
                        _showProgress.postValue(Event(false))
                    }

                    Status.LOADING -> {
                        _showProgress.postValue(Event(true))
                    }
                }

            }
        }


    }

    fun submit(
        type: String,
        email: String,
        phoneNumber: String,
        orderID: Int,
        isGiftCardType: Boolean = false
    ) {

        if (type == "Email" && email.isEmpty()) {
            _snackbarText.value = Event(R.string.email_validate)
        } else if (type == "Email" && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _snackbarText.value = Event(R.string.valid_email_validate)
        } else if (type == "Message" && phoneNumber.isEmpty()) {
            _snackbarText.value = Event(R.string.phone_validate)
        } else if (type == "Message" && phoneNumber.replace(("[\\D]").toRegex(), "").length < 10) {
            _snackbarText.value = Event(R.string.valid_phone_validate)
        } else {
            _showProgress.value = Event(true)

            val data = HashMap<String, String>()
            if (type == "Email") {
                data["email"] = email
            } else {
                // data["phone_no"] = phoneNumber.replace(("[\\D]").toRegex(), "")
                data["phone_no"] = phoneNumber
            }

            data["id"] = orderID.toString()

            viewModelScope.launch {

                resource = if(isGiftCardType){
                    if (type == "Email") {
                        posRepository.giftCardEmailReceipt(data)
                    } else
                        posRepository.giftCardPhoneReceipt(data)
                } else {
                    if (type == "Email") {
                        posRepository.emailReceipt(data)
                    } else
                        posRepository.phoneReceipt(data)
                }

                when (resource.status) {
                    Status.SUCCESS -> {
                        itsFromETP = false
                        _showProgress.value = Event(true)
                        resource.data.let { baseResponse ->
                            if (baseResponse?.status == 200) {

                                resource.data?.let { response ->

                                    _data.postValue(Event(response))

                                }
                            } else {
                                _snackbarText.postValue(Event(resource.message))
                            }
                        }
                    }

                    Status.ERROR -> {
                        itsFromETP = false
                        _snackbarText.postValue(Event(resource.message))
                        _showProgress.postValue(Event(false))
                    }

                    Status.LOADING -> {
                        _showProgress.postValue(Event(true))
                    }
                }
            }

        }

    }

    fun getCustomerPrinterList(): LiveData<Resource<List<PrinterResponse.Data.CustomerReceiptPrinters>>> {
        return posRepository.getCustomerPrinters()
    }

    fun getKitchenPrinterList(): LiveData<Resource<List<PrinterResponse.Data.KitchenReceiptPrinters>>> {
        return posRepository.getKitchenPrinters()
    }

    fun assignCustomer(orderID: Int, custId: Int, payment_id: Int, final_Reward: Int) {

        viewModelScope.launch {

            val resource: Resource<CustomerAssignedResponse> =
                posRepository.assignCustomerOrder(orderID, custId, 0, payment_id, final_Reward)
                        as Resource<CustomerAssignedResponse>

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { response ->
                        if (response?.status == 200) {
                            resource.data.let {
                                if (it?.data?.order?.customer != null) {
                                    posRepository.updateFinalRewards(
                                        it.data.order.customer.final_reward.toInt(),
                                        it.data.order.customer.id
                                    )
                                }
                            }
                            prefProvider.setValueInt(Constants.PAYMENT_ID, 0)
                            _data1.value = Event(resource.data)
                        } else {
                            _snackbarText.value = Event(resource.data)
                        }
                    }
                }

                Status.ERROR -> {
                    _snackbarText.value = Event(resource.data)
                    _showProgress.value = Event(false)
                }

                Status.LOADING -> {
                    _showProgress.value = Event(true)
                }
            }
        }
    }

    fun deleteCart() {
        viewModelScope.launch {
            posRepository.deleteCart(prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0))
        }
    }

    fun deleteSplitDb() {
        viewModelScope.launch {
            posRepository.deleteSplitDb()
        }
    }

    val allSplitList = appDatabase.splitDao().allSplitList


    fun addSplitToDatabase(title: String, amount: Double, remainingAmt: Double) {

        val model =
            SplitDetailListModel(title = title, amount = amount, remainingAmt =  remainingAmt)
        viewModelScope.launch {
            posRepository.addSplitAmount(model)
        }
    }

    suspend fun updateStatusPrinterQueue(listIds: List<Int>, id: Int) {
        appDatabase.printerQueueDao().updatePrinterQueue(listIds, id)

    }

    fun checkDataisExistOrNot(printerQueueModel: PrinterQueueModel): Boolean {
        var printerQueue: PrinterQueueModel? = null
        viewModelScope.launch {
            printerQueue = printerQueueModel.id?.let {
                posRepository.getPrinterQueueQueryData(it)
            }
        }

        if (printerQueue != null) {
            if (printerQueueModel.printSuccessData.isNotEmpty()) {

              /*  for (i in 0 until printerQueue!!.printSuccessData?.size) {
                    if (printerQueue!!.printSuccessData.contains(printerQueue!!.printSuccessData[i])) {
                        return true
                        break
                    } else {
                        return false
                    }
                }*/
                printerQueueModel.printSuccessData.forEach {
                    if (printerQueue!!.printSuccessData.contains(it)) {

                        return false

                    }

                }
            } else {

                return false
            }


        } else {
            return false
        }
        return false
    }


}