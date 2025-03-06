package com.pays.pos.ui.fragments.transactions

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pays.pos.data.model.requestModel.CashInOutModel
import com.pays.pos.data.model.requestModel.CashInOutPaymentModel
import com.pays.pos.data.model.requestModel.CashLogRequest
import com.pays.pos.data.model.responseModel.BaseResponse
import com.pays.pos.data.model.responseModel.GetTransactionListResponse
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.LOCATION_ID
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.data.repositories.TaxServiceChargeRepository
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.Event
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val taxServiceChargeRepository: TaxServiceChargeRepository,
    private val prefProvider: PrefProvider,
    private val posRepository: PosRepository
) : ViewModel() {

    val locationId = prefProvider.getValueInt(LOCATION_ID, 0)
    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<GetTransactionListResponse?>>()
    val data: LiveData<Event<GetTransactionListResponse?>> = _data

    private val _data1 = MutableLiveData<Event<BaseResponse?>>()
    val data1: LiveData<Event<BaseResponse?>> = _data1

    private val _data2 = MutableLiveData<Event<Double>>()
    val data2: LiveData<Event<Double>> = _data2

    private val _cashLogUpdated=MutableLiveData<Event<Boolean>>()
    val cashLogUpdate get() = _cashLogUpdated


    private val _transactionDetails =
        MutableLiveData<Event<GetTransactionListResponse.Data.Payment>>()
    val transactionDetails: LiveData<Event<GetTransactionListResponse.Data.Payment>> =
        _transactionDetails

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    val getTerminalListDatabse = posRepository.getTerminalListDatabse()

    val startDate = MutableLiveData<String>()

    val endDate = MutableLiveData<String>()

    var selectPicker1: Boolean = false
    var terminalIdViewModel: String = ""
    var orderTypeIdViewModel: String = ""
    var roleIdViewModel: String = ""
    var employeeIdViewModel: String = ""

    private val _startDateSelection = MutableLiveData<Event<Unit>>()
    val startDateSelection: LiveData<Event<Unit>> = _startDateSelection

    private val _endDateSelection = MutableLiveData<Event<Unit>>()
    val endDateSelection: LiveData<Event<Unit>> = _endDateSelection


    /* val getEmployeesTimeSheet =
         posRepository.employeesTimeSheet(startDate.value.toString(), endDate.value.toString(),roleId)*/
    val getTeamRoleList = taxServiceChargeRepository.getTeamRoleListFromDatabase()
    val employeeData = posRepository.getEmployeeListLocationWiseDatabse(locationId)
    val orderTypes = posRepository.orderTypesfromDatabase()

    fun setCurrentDate(myCalendar: Calendar) {
        val myFormat = "MM/dd/yyyy" //In which you need put here
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        /* startDate.value = sdf.format(myCalendar.time) + " " + SimpleDateFormat(
             "hh:mm a",
             Locale.getDefault()
         ).format(Date(System.currentTimeMillis() - 60000 * 30))*/

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
            ).format(Date(System.currentTimeMillis() + 300000))
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

    fun transactionId(transactionId: GetTransactionListResponse.Data.Payment) {

        if (!transactionId.payableType.equals(
                "GiftCard",
                true
            ) || !transactionId.payableType.equals("Invoice", true)
            || !transactionId.payableType.equals("GiftCardAmountTab", true)
        ) {
            _transactionDetails.value = Event(transactionId)
        }
    }

    fun updateLabel(myCalendar: Calendar) {
        val myFormat = "MM/dd/yyyy" //In which you need put here
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())

        if (selectPicker1) {
            startDate.value = sdf.format(myCalendar.time)
        } else {
            endDate.value = sdf.format(myCalendar.time)
        }
    }


    fun apiCallTimeSheet(
        currentPage: Int,
        terminalId: String,
        roleId: String,
        employeeId: String,
        orderTypeId: String,
        tipType: String,
        paymentType: String
    ) {
        terminalIdViewModel = terminalId
        roleIdViewModel = roleId
        employeeIdViewModel = employeeId
        orderTypeIdViewModel = orderTypeId

        if (terminalId == "-1") {
            terminalIdViewModel = ""
        }

        if (roleId == "-1") {
            roleIdViewModel = ""
        }

        if (employeeId == "-1") {
            employeeIdViewModel = ""
        }

        if (orderTypeId == "-1") {
            orderTypeIdViewModel = ""
        }

        Log.e("startDate", startDate.value.toString())
        Log.e("endDate", endDate.value.toString())

        _showProgress.value = Event(true)

        viewModelScope.launch {
            val data = LinkedHashMap<String, String>()
            data["page"] = currentPage.toString()
            data["per_page"] = 10.toString()
            data["start_date"] = startDate.value.toString()
            data["end_date"] = endDate.value.toString()
            data["terminal_id"] = terminalIdViewModel
            data["employee_role_id"] = roleIdViewModel
            data["employee_id"] = employeeIdViewModel
            data["order_type_id"] = orderTypeIdViewModel
            when (tipType) {
                "All Tips Type" -> {
                    data["tips_adjusted"] = ""
                }

                "Adjusted" -> {
                    data["tips_adjusted"] = true.toString()
                }

                "Unadjusted" -> {
                    data["tips_adjusted"] = false.toString()
                }
            }
            when (paymentType) {
                "All Transaction Types" -> {
                    data["payment_type"] = ""
                }

                "Cash" -> {
                    data["payment_type"] = "Cash"
                }

                "Card" -> {
                    data["payment_type"] = "Card"
                }

                "Gift Card","External" -> {
                    data["payment_type"] = "External"
                }

            }
            LogUtil.logE("TransactionViewModel", "filteredData $data")

            val resource = taxServiceChargeRepository.getTransactionList(data)

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { logInResponse ->
                        if (logInResponse?.status == 200) {

                            resource.data?.let { timeSheetResponse ->
                                _data.value = Event(timeSheetResponse)
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

    private fun validateDates(startDate: String?, endDate: String?): Boolean {
        var b = false
        try {
            val myFormat = "MM/dd/yyyy" //In which you need put here
            val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
            b = if (sdf.parse(startDate).before(sdf.parse(endDate))) {
                true //If start date is before end date
            } else sdf.parse(startDate).equals(sdf.parse(endDate))
        } catch (e: ParseException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
        return b
    }

    fun getCashEventDetails(
        tippedAmount: Double,
        orderId: Int,
        id: Int,
        event: String,
        isChange: Byte,
        fromOrderComplete: Byte = 0,
    ) {
        viewModelScope.launch {
            val resource = posRepository.getEventDetailsByOrderId(orderId.toString())

            when (resource.status) {
                Status.SUCCESS -> {
                    resource.data?.let { response ->
                        val lastEvent = response.data.lastOrNull()
                        val lastReason = lastEvent?.reason?.lowercase() ?: ""

                        val isPaymentReceived = lastReason.contains("payment received")
                        val isGiftCard = lastReason.contains("gift card")
                        val isChangeReturned = lastReason.contains("change returned")

                        val shouldCreateCashLog = response.data.isEmpty() ||
                                (response.data.size == 1 && lastEvent?.event.equals("in", ignoreCase = true) && isPaymentReceived && fromOrderComplete == 1.toByte()) ||
                                (fromOrderComplete == 1.toByte() && (isPaymentReceived || isGiftCard) && isChange == 1.toByte())

                        if (shouldCreateCashLog) {
                            val cashLogRequest = CashLogRequest(
                                tippedAmount,
                                prefProvider.getValueInt(Constants.EMPLOYEE_ID, -1),
                                event,
                                orderId,
                                id,
                                if (isChange == 1.toByte()) "Change returned after order's payment" else "Tip added to the order",
                                prefProvider.getValueInt(Constants.TERMINAL_ID, -1),
                                null,
                                null
                            )
                            makeCashLogCreateRequest(cashLogRequest)
                        } else if (isChange == 0.toByte() && isChangeReturned) {
                            val cashLogRequest = CashLogRequest(
                                tippedAmount,
                                prefProvider.getValueInt(Constants.EMPLOYEE_ID, -1),
                                event,
                                orderId,
                                id,
                                "Tip added to the order",
                                prefProvider.getValueInt(Constants.TERMINAL_ID, -1),
                                null,
                                null
                            )
                            makeCashLogCreateRequest(cashLogRequest)
                        } else {
                            var updatedTippedAmount: Double = tippedAmount
                            var reason: String = if (isChange == 1.toByte()) "Change returned after order's payment" else "Tip updated for order"
                            if (response.data.size == 1 && lastEvent?.event.equals("in", ignoreCase = true) && isPaymentReceived && fromOrderComplete == 0.toByte()) {
                                lastEvent?.let { cashEvent ->
                                    updatedTippedAmount = tippedAmount + cashEvent.amount!!.toDouble() - cashEvent.totalTips!!.toDouble()
                                    reason = "Payment received for order"
                                }
                            }
                            val updateCashLogRequest = CashLogRequest(
                                updatedTippedAmount,
                                prefProvider.getValueInt(Constants.EMPLOYEE_ID, -1),
                                event,
                                lastEvent?.orderId ?: orderId,
                                lastEvent?.id ?: id,
                                reason,
                                prefProvider.getValueInt(Constants.TERMINAL_ID, -1),
                                null,
                                tippedAmount
                            )

                            lastEvent?.id?.let { updateCashLog(it, updateCashLogRequest) }
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


    /*
        fun getCashEventDetails(tippedAmount: Double, orderId: Int, id: Int, event:String, isChange:Byte, fromOrderComplete:Byte=0){
            viewModelScope.launch {
                val resource=posRepository.getEventDetailsByOrderId(orderId.toString())

                when(resource.status){
                    Status.SUCCESS->{
                        resource.data?.let {

                            if ((it.data.isEmpty() || (it.data.size==1 && it.data.last().event.equals("in",ignoreCase = true) && (it.data.last().reason?.contains("Payment received", ignoreCase = true)?:false))) || (fromOrderComplete.toInt() == 1 && ((it.data.last().reason?.contains("Payment received", ignoreCase = true)?:false) || (it.data.last().reason?.contains("Gift card", ignoreCase = true)?:false)) && isChange.toInt() == 1)){

                                val cashLogRequest = CashLogRequest(
                                    tippedAmount,
                                    prefProvider.getValueInt(Constants.EMPLOYEE_ID, -1),
                                    event,
                                    orderId,
                                    id,
                                    if (isChange.toInt()==1) "Change returned after order's payment" else "Tip added to the order",
                                    prefProvider.getValueInt(Constants.TERMINAL_ID, -1),
                                    null,
                                    null
                                )
                                makeCashLogCreateRequest(cashLogRequest)

                            }else if (isChange.toInt()==0 && (it.data.last().reason?.contains("Change returned", ignoreCase = true)?:false)){
                                val cashLogRequest = CashLogRequest(
                                    tippedAmount,
                                    prefProvider.getValueInt(Constants.EMPLOYEE_ID, -1),
                                    event,
                                    orderId,
                                    id,
                                    "Tip added to the order",
                                    prefProvider.getValueInt(Constants.TERMINAL_ID, -1),
                                    null,
                                    null
                                )
                                makeCashLogCreateRequest(cashLogRequest)
                            } else{
                                */
/*{
                                "payment_id": 0,
                                "order_id": 0,
                                "amount": 0,
                                "total_tips": 0,
                                "tip_setting_id": 0,
                                "employee_id": 0,
                                "terminal_id": 0,
                                "reason": "string",
                                "event": "string"
                            }*//*


                            val updateCashLogRequest = CashLogRequest(
                                tippedAmount,
                                prefProvider.getValueInt(Constants.EMPLOYEE_ID, -1),
                                event,
                                it.data.last().orderId?:orderId,
                                it.data.last().id?:id,
                                if (isChange.toInt()==1) "Change returned after order's payment" else "Tip updated for order",
                                prefProvider.getValueInt(Constants.TERMINAL_ID, -1),
                                null,
                                null
                            )
//1536
                            it.data.last().id?.let {
                                updateCashLog(it,updateCashLogRequest)
                            }
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
*/

    private fun updateCashLog(cashEventId: Int, cashLogRequest: CashLogRequest) {
        viewModelScope.launch {
            val result=posRepository.updateCashEventsByOrderId(cashEventId, cashLogRequest)
            when(result.status){
                Status.SUCCESS->{
                    _showProgress.value = Event(false)
                    _cashLogUpdated.value=Event(true)
                }

                Status.LOADING->{
                }

                Status.ERROR->{
                    _cashLogUpdated.value=Event(false)
                    _snackbarText.value = Event(result.message)
                    _showProgress.value = Event(false)
                }

            }
        }

    }

    fun makeCashLogCreateRequest(cashLogRequest: CashLogRequest) {
        viewModelScope.launch {
            val resource = posRepository.cashInOut(cashLogRequest)

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { response ->
                        if (response?.status == 200) {

                            resource.data?.let {

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

    fun orderUpdateTip(orderID: Int, tipAmount: Double, is_captured: Boolean) {

        val paymentModel = CashInOutPaymentModel()
        paymentModel.id = orderID
        paymentModel.isCaptured = is_captured
        val data = CashInOutModel()
        data.paymentAttributes = paymentModel

        viewModelScope.launch {

            val resource = posRepository.orderUpdateTip(orderID, tipAmount, is_captured, data)

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { baseResponse ->
                        if (baseResponse?.status == 200) {

                            resource.data?.let { response ->

                                _data1.value = Event(response)
                                _data2.value = Event(tipAmount)

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

    private val _updateTipData = MutableLiveData<Event<BaseResponse?>>()
    val updateTipData: LiveData<Event<BaseResponse?>> = _updateTipData

    fun updateTipWithSignature(orderId: Int, signatureInBase64: String, tip: Double) {

        viewModelScope.launch {
            _showProgress.value = Event(true)
            val option = HashMap<String, Any>()
            option["id"] = orderId
            option["signature"] = signatureInBase64
            option["tips"] = tip

            val resource = posRepository.updateTipWithSignatureFM(option)

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { baseResponse ->
                        if (baseResponse?.status == 200) {

                            resource.data?.let { response ->

                                Log.d("TAG", "updateTipWithSignature: $response")
                                _updateTipData.value = Event(response)

                            }
                        } else {
                            _snackbarText.value = Event(resource.message)
                        }
                    }
                }

                Status.ERROR -> {
                    Log.d("TAG", "updateTipWithSignature: ${resource.message}")
                    _snackbarText.value = Event(resource.message)
                    _showProgress.value = Event(false)
                }

                Status.LOADING -> {
                    Log.d("TAG", "updateTipWithSignature: LOADING...")
                    _showProgress.value = Event(true)

                }
            }
        }
    }
}

