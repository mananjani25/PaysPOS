package com.pays.pos.ui.fragments.orders

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pays.pos.data.model.requestModel.OrderCancelRequest
import com.pays.pos.data.model.responseModel.BaseResponse
import com.pays.pos.data.model.responseModel.OpenOrderResponse
import com.pays.pos.data.model.responseModel.OrderCountsResponse
import com.pays.pos.data.model.responseModel.PrinterResponse
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.Event
import com.pays.pos.utils.statusUtils.Resource
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ActiveOrderViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val prefProvider: PrefProvider
) : ViewModel() {
    private val TAG = "ActiveOrderViewModel"
    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<BaseResponse?>>()
    val data: LiveData<Event<BaseResponse?>> = _data

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    var selectPicker1: Boolean = false
    private val _startDateSelection = MutableLiveData<Event<Unit>>()
    val startDateSelection: LiveData<Event<Unit>> = _startDateSelection

    private val _endDateSelection = MutableLiveData<Event<Unit>>()
    val endDateSelection: LiveData<Event<Unit>> = _endDateSelection

    val startDate = MutableLiveData<String>()

    val endDate = MutableLiveData<String>()

    val getcancelOrderReasonsDatabse = posRepository.getCancelOrderListDatabse()

    fun getCustomerReceiptSettings() = posRepository.getCustomerReceiptSettings()

    fun getCustomerPrinterList(): LiveData<Resource<List<PrinterResponse.Data.CustomerReceiptPrinters>>> {
        return posRepository.getCustomerPrinters()
    }

    fun getTipsList() = posRepository.getTipsList()

    fun openOrders(
        paymentStatus: String,
        startDate: String,
        endDate: String
    ): LiveData<Resource<OpenOrderResponse>> =
        posRepository.getOpenOrders(paymentStatus, startDate, endDate)


    fun phoneOrders(
        paymentStatus: String,
        startDate: String,
        endDate: String
    ): LiveData<Resource<OpenOrderResponse>> =
        posRepository.getPhoneOrders(paymentStatus, startDate, endDate)


    fun orderCounts(startDate: String?, endDate: String?, isOpenOrder: Boolean): LiveData<Resource<OrderCountsResponse>> =
        posRepository.orderCounts(startDate, endDate,isOpenOrder)

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
            var startTime = prefProvider.getValue(Constants.REPORT_START_TIME, "")
            var endTime = prefProvider.getValue(
                Constants.REPORT_END_TIME, ""
            )
            if (startTime.isNotEmpty()) {
                startDate.value = sdf.format(myCalendar.time) + " " + startTime
            } else {
                startDate.value = sdf.format(myCalendar.time) + " " + "12:00 AM"
            }
            if (endTime.isNotEmpty()) {
                endDate.value = sdf.format(myCalendar.time) + " " + endTime
            } else {
                endDate.value = sdf.format(myCalendar.time) + " " + SimpleDateFormat(
                    "hh:mm a",
                    Locale.getDefault()
                ).format(Date(System.currentTimeMillis() + 60000))
            }
        }
    }


    fun datePicker(selectPicker: Boolean) {
        selectPicker1 = selectPicker

        if (selectPicker) {
            _startDateSelection.value = Event(Unit)
        } else {
            _endDateSelection.value = Event(Unit)
        }
    }


    fun cancelOrder(orderId: Int, reason: String, reason_id: Int?) {
        _showProgress.value = Event(true)

        val request = OrderCancelRequest.OrderData(
            "Cancelled", reason, reason_id, prefProvider.getValueInt(
                Constants.EMPLOYEE_ID, 0,), "", ""
        )

        val orderCancelRequest = OrderCancelRequest(request)

        viewModelScope.launch {
            val resource = posRepository.orderCancel(orderId, orderCancelRequest)
            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)

                    resource.data.let {
                        if (it?.status == 200) {
                            resource.data?.let { createTaxResponse ->
                                _data.value = Event(createTaxResponse)
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