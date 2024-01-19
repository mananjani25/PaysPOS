package com.pays.pos.ui.fragments.settings.hardware.scangun

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pays.pos.data.db.AppDatabase
import com.pays.pos.data.model.responseModel.report.Data
import com.pays.pos.data.model.responseModel.report.Terminal
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.data.repositories.TaxServiceChargeRepository
import com.pays.pos.data.repositories.TipDiscountRepository
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.Event
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ScanGunViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val appDatabase: AppDatabase,
    private val prefProvider: PrefProvider,
    private val taxServiceChargeRepository: TaxServiceChargeRepository,
    private val tipDiscountRepository: TipDiscountRepository
) : ViewModel() {

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _startDateSelection = MutableLiveData<Event<Unit>>()
    val startDateSelection: LiveData<Event<Unit>> = _startDateSelection

    private val _endDateSelection = MutableLiveData<Event<Unit>>()
    val endDateSelection: LiveData<Event<Unit>> = _endDateSelection

    private val _data = MutableLiveData<Event<Data?>>()
    val data: LiveData<Event<Data?>> = _data

    val getTerminalListDatabse = posRepository.getTerminalListDatabse()

    var selectPicker1: Boolean = false
    val startDate = MutableLiveData<String>()

    val endDate = MutableLiveData<String>()
    var selectedTerminalId = ""
    val terminalTitle = Terminal("Terminal", -9.9)


    fun setCurrentDate(myCalendar: Calendar) {
        val myFormat = "MM/dd/yyyy" //In which you need put here
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())


        startDate.value = sdf.format(myCalendar.time) + " " + SimpleDateFormat(
            "hh:mm a",
            Locale.getDefault()
        ).format(Date(System.currentTimeMillis() - 60000 * 30))
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

    fun updateLabel(myCalendar: Calendar) {
        val myFormat = "MM/dd/yyyy" //In which you need put here
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())

        if (selectPicker1) {
            startDate.value = sdf.format(myCalendar.time)
        } else {
            endDate.value = sdf.format(myCalendar.time)
        }
    }


    fun getReportSummary() {

        _showProgress.value = Event(true)
        viewModelScope.launch {

            val resourceReport =
                posRepository.getReportSummary(
                    startDate = startDate.value ?: "",
                    endDate = endDate.value ?: "",
                    terminalId = selectedTerminalId
                )
            when (resourceReport.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resourceReport.data.let {
                        if (it?.status == 200) {
                            _data.postValue(Event(resourceReport.data?.data))
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
