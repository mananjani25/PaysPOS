package com.pays.pos.ui.fragments.settings.teamrole

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pays.pos.data.model.responseModel.BaseResponse
import com.pays.pos.data.repositories.TaxServiceChargeRepository
import com.pays.pos.utils.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SingleTeamMemberSheetViewModel @Inject constructor(
    private val taxServiceChargeRepository: TaxServiceChargeRepository
) : ViewModel() {

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<BaseResponse?>>()
    val data: LiveData<Event<BaseResponse?>> = _data

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    val startDate = MutableLiveData<String>()

    val endDate = MutableLiveData<String>()

    var selectPicker1: Boolean = false

    private val _dateSelection = MutableLiveData<Event<Unit>>()
    val dateSelection: LiveData<Event<Unit>> = _dateSelection

    /*init {
        val myFormat = "dd/MM/yyyy" //In which you need put here
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        startDate.value = sdf.format(myCalendar.time)
        endDate.value = sdf.format(myCalendar.time)
    }*/

    fun setCurrentDate(myCalendar: Calendar) {
        val myFormat = "MM/dd/yyyy" //In which you need put here
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        startDate.value = sdf.format(myCalendar.time)
        endDate.value = sdf.format(myCalendar.time)
    }

    fun datePicker(selectPicker: Boolean) {
        selectPicker1 = selectPicker
        _dateSelection.value = Event(Unit)
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


}