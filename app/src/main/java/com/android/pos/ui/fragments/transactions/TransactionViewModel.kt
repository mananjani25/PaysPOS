package com.android.pos.ui.fragments.transactions

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.model.responseModel.BaseResponse
import com.android.pos.data.model.responseModel.GetTransactionListResponse
import com.android.pos.data.remote.Constants.LOCATION_ID
import com.android.pos.data.repositories.PosRepository
import com.android.pos.data.repositories.TaxServiceChargeRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.collections.LinkedHashMap

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
        startDate.value = sdf.format(myCalendar.time) + " " + SimpleDateFormat(
            "hh:mm a",
            Locale.getDefault()
        ).format(Date())
        endDate.value = sdf.format(myCalendar.time) + " " + SimpleDateFormat(
            "hh:mm a",
            Locale.getDefault()
        ).format(Date())

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
        _transactionDetails.value = Event(transactionId)
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
                "External" -> {
                    data["payment_type"] = "External"
                }
            }

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

    fun orderUpdateTip(orderID: Int, tipAmount: Double) {

        viewModelScope.launch {

            val resource = posRepository.orderUpdateTip(orderID, tipAmount)

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
}

