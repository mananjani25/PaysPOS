package com.android.pos.ui.fragments.onlineorder

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.pos.data.model.responseModel.*
import com.android.pos.data.repositories.PosRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
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

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress
    var selectPicker1: Boolean = false
    private val _startDateSelection = MutableLiveData<Event<Unit>>()
    val startDateSelection: LiveData<Event<Unit>> = _startDateSelection

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
    fun onLineorderCounts(): LiveData<Resource<OnlineOrderCountResponse>> =
        posRepository.onlineOrderCounts()


    fun onlineOrders(
        startDate: String,
        endDate: String,
        order_status:String
    ): LiveData<Resource<OnlineOrderResponseModel>> =
        posRepository.getOnlineOrders(startDate, endDate,order_status)

    fun acceptedAndDeclineOrder(
        time: Int,
        order_id: Int,
        isaccepted: Boolean
    ): LiveData<Resource<BaseResponse>> =
        posRepository.acceptedAndDeclineOrders(time, order_id,isaccepted)


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