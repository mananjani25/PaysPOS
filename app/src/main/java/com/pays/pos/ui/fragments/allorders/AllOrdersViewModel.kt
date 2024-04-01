package com.pays.pos.ui.fragments.allorders

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pays.pos.data.model.requestModel.OrderCancelRequest
import com.pays.pos.data.model.requestModel.RefundRequestModelOnlineOrder
import com.pays.pos.data.model.responseModel.*
import com.pays.pos.data.model.responseModel.allOrders.AllOrdersCountResponse
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
class AllOrdersViewModel @Inject constructor(
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

    /**
     * To change tab
     * */
    var changeTabPosition = MutableLiveData<Int>(-1)


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

    fun allOrderCounts(startDate: String?, endDate: String?): LiveData<Resource<AllOrdersCountResponse>> =
        posRepository.allOrderCounts(startDate, endDate)

    fun getAllOrders(
        startDate: String,
        endDate: String,
        order_status: String,
        payment_status: String,
        order_type_id: String
    ): LiveData<Resource<OnlineOrderResponseModel>> =
        posRepository.getAllOrders(startDate, endDate, order_status, payment_status, order_type_id)

    // To accept/decline order
    fun acceptedAndDeclineOrder(
        time: Int,
        order_id: Int,
        isaccepted: Boolean,
        employee_id: Int,
        terminalid: Int
    ): LiveData<Resource<OnlineOrderStatusUpdateResponse>> =
        posRepository.acceptedAndDeclineOrders(time, order_id, isaccepted, employee_id, terminalid)

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
    // To update online order
    fun updateOnlineOrder(
        order_id: Int,
        order_status: String
    ): LiveData<Resource<BaseResponse>> =
        posRepository.updateOnlineOrders(order_id, order_status)


    fun setCurrentDate(
        myCalendar: Calendar,
        paramStartDate: String?,
        paramEndDate: String?,
        status: String
    ) {
        val myFormat = "MM/dd/yyyy" //In which you need put here
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        /* startDate.value = sdf.format(myCalendar.time) + " " + SimpleDateFormat(
             "hh:mm a",
             Locale.getDefault()
         ).format(Date(System.currentTimeMillis() - 60000 * 30))*/


        if (paramStartDate?.isNotEmpty() == true && paramEndDate?.isNotEmpty() == true) {
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
                var temp_calender = Calendar.getInstance()
                if (status=="4"){
                    temp_calender.add(Calendar.DATE,7)
                    endDate.value = sdf.format(temp_calender.time) + " " + endTime
                }else{
                    endDate.value = sdf.format(temp_calender.time) + " " + endTime
                }
            } else {
                if (status == "4") {
                    endDate.value = sdf.format(myCalendar.time) + " " + SimpleDateFormat(
                        "hh:mm a",
                        Locale.getDefault()
                    ).format(Date(System.currentTimeMillis() + 604800000))
                } else {
                    endDate.value = sdf.format(myCalendar.time) + " " + SimpleDateFormat(
                        "hh:mm a",
                        Locale.getDefault()
                    ).format(Date(System.currentTimeMillis() + 60000))
                }

            }
        }
    }

    val getcancelOrderReasonsDatabse = posRepository.getCancelOrderListDatabse()

    // To cancel order
    fun cancelOrder(orderId: Int, reason: String, reason_id: Int?) {
        _showProgress.value = Event(true)
        val formatterDate = SimpleDateFormat("yyyy-MM-dd")
        val formatterTime = SimpleDateFormat("hh:mm a")
        val date = Date()
        val request = OrderCancelRequest.OrderData(
            "Cancelled", reason, reason_id, prefProvider.getValueInt(
                Constants.EMPLOYEE_ID, 0,
            ), formatterDate.format(date), formatterTime.format(date)
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
                                Log.d("08JUNE23", "Set Data Value From Cancel API")
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