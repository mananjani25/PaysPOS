package com.android.pos.ui.fragments.onlineorder

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.model.requestModel.RefundRequestModelOnlineOrder
import com.android.pos.data.model.responseModel.*
import com.android.pos.data.repositories.PosRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Resource
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class OnlineDetailViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val prefProvider: PrefProvider
) : ViewModel() {
    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<BaseResponse?>>()
    val data: LiveData<Event<BaseResponse?>> = _data

    private val _dataRefundDone = MutableLiveData<Event<BaseResponse?>>()
    val dataRefundDone: LiveData<Event<BaseResponse?>> = _dataRefundDone


    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress
    var selectPicker1: Boolean = false
    private val _startDateSelection = MutableLiveData<Event<Unit>>()
    val startDateSelection: LiveData<Event<Unit>> = _startDateSelection

    fun getKitchenReceiptSettings() = posRepository.getKitchenReceiptSettings()

    fun getKitchenPrinterList(): LiveData<Resource<List<PrinterResponse.Data.KitchenReceiptPrinters>>> {
        return posRepository.getKitchenPrinters()
    }


    private val _endDateSelection = MutableLiveData<Event<Unit>>()
    val endDateSelection: LiveData<Event<Unit>> = _endDateSelection

    val startDate = MutableLiveData<String>()

    val endDate = MutableLiveData<String>()

    fun datePicker(selectPicker: Boolean) {
        selectPicker1 = selectPicker

        if (selectPicker) {
            _startDateSelection.value = Event(Unit)
        } else {
            _endDateSelection.value = Event(Unit)
        }
    }
    fun onLineorderCounts(startDate: String?, endDate: String?): LiveData<Resource<OnlineOrderCountResponse>> =
        posRepository.onlineOrderCounts(startDate,endDate)


    fun onlineOrders(
        startDate: String,
        endDate: String,
        order_status:String
    ): LiveData<Resource<OnlineOrderResponseModel>> =
        posRepository.getOnlineOrders(startDate, endDate,order_status)

    fun acceptedAndDeclineOrder(
        time: Int,
        order_id: Int,
        isaccepted: Boolean,
        employee_id:Int,
        terminalid:Int
    ): LiveData<Resource<OnlineOrderStatusUpdateResponse>> =
        posRepository.acceptedAndDeclineOrders(time, order_id,isaccepted,employee_id,terminalid)

    fun refundPaymentApiCall(
        refundAmount: Double,
        refundData: RefundRequestModelOnlineOrder,
        refundReason: String,
        paymentType: String
    ) {

        refundData.paymentRefund?.reasonForRefund = refundReason
        refundData.paymentRefund?.amount = refundAmount

        _showProgress.value = Event(true)

        viewModelScope.launch {

            val resource = posRepository.refundPaymentOnline(refundData)
            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { logInResponse ->
                        if (logInResponse?.status == 200) {

                            resource.data?.let { createTaxResponse ->
                                _dataRefundDone.value = Event(createTaxResponse)
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
    fun updateOnlineOrder(
        order_id: Int,
        order_status: String
    ): LiveData<Resource<BaseResponse>> =
        posRepository.updateOnlineOrders( order_id,order_status)


    fun setCurrentDate(myCalendar: Calendar, paramStartDate: String?, paramEndDate: String?) {
        val myFormat = "MM/dd/yyyy" //In which you need put here
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        /* startDate.value = sdf.format(myCalendar.time) + " " + SimpleDateFormat(
             "hh:mm a",
             Locale.getDefault()
         ).format(Date(System.currentTimeMillis() - 60000 * 30))*/

        if (paramStartDate != null && paramEndDate != null) {
            startDate.value = paramStartDate.toString()
            endDate.value = paramEndDate.toString()
        } else {
            startDate.value = sdf.format(myCalendar.time) + " " + "12:00 AM"
            endDate.value = sdf.format(myCalendar.time) + " " + SimpleDateFormat(
                "hh:mm a",
                Locale.getDefault()
            ).format(Date(System.currentTimeMillis() + 60000))
        }
    }
}