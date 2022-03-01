package com.android.pos.ui.fragments.report

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.entities.Employee
import com.android.pos.data.model.responseModel.EodReportResponse
import com.android.pos.data.model.responseModel.report.Terminal
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.TERMINAL_ID
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
class ReportEODViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val prefProvider: PrefProvider
) : ViewModel() {

    val locationId = prefProvider.getValueInt(Constants.LOCATION_ID, 0)
    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _startDateSelection = MutableLiveData<Event<Unit>>()
    val startDateSelection: LiveData<Event<Unit>> = _startDateSelection

    private val _endDateSelection = MutableLiveData<Event<Unit>>()
    val endDateSelection: LiveData<Event<Unit>> = _endDateSelection

    private val _data = MutableLiveData<Event<EodReportResponse.Data>>()
    val data: LiveData<Event<EodReportResponse.Data>> = _data

    val getTerminalListDatabse = posRepository.getTerminalListDatabse()

    fun getEmployeeEmail(emp_id:Int): LiveData<Resource<Employee>> {
        return posRepository.getEmployeeEmail(emp_id)
    }


    val employeeData = posRepository.getEmployeeListLocationWiseDatabse(locationId)

    var selectPicker1: Boolean = false
    val startDate = MutableLiveData<String>()

    val endDate = MutableLiveData<String>()
    var selectedTerminalId = "0"
    val terminalTitle = Terminal("Terminal", -9.9)

    fun employeeId(): Int {
        return prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
    }

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
        ).format(Date(System.currentTimeMillis() + 300000))


    }

    fun datePicker(selectPicker: Boolean) {
        selectPicker1 = selectPicker

        if (selectPicker) {
            _startDateSelection.value = Event(Unit)
        } else {
            _endDateSelection.value = Event(Unit)
        }
    }


    fun getReportSummary(emailId: String) {

        _showProgress.value = Event(true)
        viewModelScope.launch {

            Log.e("startDate", startDate.value ?: "")
            Log.e("endDate", endDate.value ?: "")

            val resourceReport =
                posRepository.getReportEOD(
                    startDate = startDate.value ?: "",
                    endDate = endDate.value ?: "",
                    terminalId = prefProvider.getValueInt(TERMINAL_ID, 0).toString(),
                    employee_id = selectedTerminalId,
                    email = emailId

                )
            when (resourceReport.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resourceReport.data.let {
                        if (it?.status == 200) {

                            if (emailId.isEmpty()) {
                                _data.postValue(Event(resourceReport.data?.data!!))
                            } else {
                                _snackbarText.value = Event(it.message)
                            }
                        }
                    }
                }

                Status.ERROR -> {
                    _snackbarText.value = Event(resourceReport.message)
                    _showProgress.value = Event(false)
                }

                Status.LOADING -> {
                    _showProgress.value = Event(true)
                }
            }
        }
    }
}
