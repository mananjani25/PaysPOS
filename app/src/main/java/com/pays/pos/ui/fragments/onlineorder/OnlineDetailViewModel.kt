package com.pays.pos.ui.fragments.onlineorder

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pays.pos.data.model.CancelOnlineWebOrderModel
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
import kotlin.io.path.createTempDirectory

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

    private val _refresh = MutableLiveData<Boolean>()
    public val refresh: LiveData<Boolean> = _refresh


    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress
    var selectPicker1: Boolean = false
    private val _startDateSelection = MutableLiveData<Event<Unit>>()
    val startDateSelection: LiveData<Event<Unit>> = _startDateSelection

    /**
     * To cancel currently refunded order from list
     */
    val cancelOnlineWebOrderLiveData = MutableLiveData<CancelOnlineWebOrderModel>()


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

    fun toggleRefresh(value:Boolean){
        _refresh.value=value
    }
    fun onLineorderCounts(
        startDate: String?,
        endDate: String?
    ): LiveData<Resource<OnlineOrderCountResponse>> =
        posRepository.onlineOrderCounts(startDate, endDate)

    fun allOrderCounts(
        startDate: String?,
        endDate: String?
    ): LiveData<Resource<AllOrdersCountResponse>> =
        posRepository.allOrderCounts(startDate, endDate)


    fun onlineOrders(
        startDate: String,
        endDate: String,
        order_status: String
    ): LiveData<Resource<OnlineOrderResponseModel>> =
        posRepository.getOnlineOrders(startDate, endDate, order_status)

    fun getAllOrders(
        startDate: String,
        endDate: String,
        order_status: String,
        payment_status: String,
        order_type_id: String
    ): LiveData<Resource<OnlineOrderResponseModel>> =
        posRepository.getAllOrders(startDate, endDate, order_status, payment_status, order_type_id)

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
        refundData.paymentRefund?.refunded_amount = refundAmount

        _showProgress.value = Event(true)

        viewModelScope.launch {

            val resource = posRepository.refundPaymentOnline(refundData)
            when (resource.status) {
                Status.SUCCESS -> {

                    cancelOnlineWebOrderLiveData.postValue(refundData.paymentRefund?.orderId?.let {
                        CancelOnlineWebOrderModel(
                            0,
                            it, false, isRefunded = true
                        )
                    })

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
                if (status == "4") {
                    temp_calender.add(Calendar.DATE, 7)
                    endDate.value = sdf.format(temp_calender.time) + " " + endTime
                } else {
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
}