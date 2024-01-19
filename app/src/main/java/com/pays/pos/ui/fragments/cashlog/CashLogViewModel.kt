package com.pays.pos.ui.fragments.cashlog

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pays.pos.data.model.responseModel.CashLogResponse
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.Event
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CashLogViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val prefProvider: PrefProvider
) : ViewModel() {

    val getTerminalListDatabse = posRepository.getTerminalListDatabse()

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<CashLogResponse?>>()
    val data: LiveData<Event<CashLogResponse?>> = _data

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private val _startDateSelection = MutableLiveData<Event<Unit>>()
    val startDateSelection: LiveData<Event<Unit>> = _startDateSelection

    private val _endDateSelection = MutableLiveData<Event<Unit>>()
    val endDateSelection: LiveData<Event<Unit>> = _endDateSelection

    var selectPicker1: Boolean = false
    val startDate = MutableLiveData<String>()

    val endDate = MutableLiveData<String>()

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

    fun setNumerFormat() {

    }

    fun datePicker(selectPicker: Boolean) {
        selectPicker1 = selectPicker

        if (selectPicker) {
            _startDateSelection.value = Event(Unit)
        } else {
            _endDateSelection.value = Event(Unit)
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


    fun apiCallTimeSheet(terminalId: String, currentPage: Int) {

        val tId = if (terminalId == "-1") "" else terminalId

        _showProgress.value = Event(true)
        viewModelScope.launch {
            val resource = posRepository.getCashLog(
                startDate.value.toString(),
                endDate.value.toString(),
                tId,
                currentPage.toString(),
                "10"
            )

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
}