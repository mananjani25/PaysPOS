package com.android.pos.ui.fragments.settings.teamrole

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.entities.Employee
import com.android.pos.data.model.responseModel.BaseResponse
import com.android.pos.data.model.responseModel.GetEmployeeTimeSheetDetailsResponse
import com.android.pos.data.model.responseModel.GetEmployeesTimeSheetResponse
import com.android.pos.data.repositories.PosRepository
import com.android.pos.data.repositories.TaxServiceChargeRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.LogUtil
import com.android.pos.utils.statusUtils.Resource
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class TeamMemberSheetViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val taxServiceChargeRepository: TaxServiceChargeRepository
) : ViewModel() {

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<GetEmployeesTimeSheetResponse?>>()
    val data: LiveData<Event<GetEmployeesTimeSheetResponse?>> = _data

    private val _sendTimeSheet = MutableLiveData<Event<String>>()
    val sendTimeSheet: LiveData<Event<String>> = _sendTimeSheet

    private val _timeSheetDetails = MutableLiveData<Event<GetEmployeeTimeSheetDetailsResponse?>>()
    val timeSheetDetails: LiveData<Event<GetEmployeeTimeSheetDetailsResponse?>> = _timeSheetDetails

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    val startDate = MutableLiveData<String>()

    val endDate = MutableLiveData<String>()

    var selectPicker1: Boolean = false
    var roleIdViewMOdel: String = ""

    private val _startDateSelection = MutableLiveData<Event<Unit>>()
    val startDateSelection: LiveData<Event<Unit>> = _startDateSelection

    private val _endDateSelection = MutableLiveData<Event<Unit>>()
    val endDateSelection: LiveData<Event<Unit>> = _endDateSelection

    private val _employeeIdViewModel = MutableLiveData<Event<GetEmployeesTimeSheetResponse.Data>>()
    val employeeIdViewModel: LiveData<Event<GetEmployeesTimeSheetResponse.Data>> =
        _employeeIdViewModel

    /* val getEmployeesTimeSheet =
         posRepository.employeesTimeSheet(startDate.value.toString(), endDate.value.toString(),roleId)*/
    val getTeamRoleList = taxServiceChargeRepository.getTeamRoleList()


    fun setCurrentDate(myCalendar: Calendar) {
        val myFormat = "MM/dd/yyyy" //In which you need put here
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        /* startDate.value = sdf.format(myCalendar.time) + " " + SimpleDateFormat(
             "hh:mm a",
             Locale.getDefault()
         ).format(Date(System.currentTimeMillis() - 60000 * 30))*/

        startDate.value = sdf.format(myCalendar.time) + " " + "12:00 AM"

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

    fun singleMemberTimeSheet(employeeId: GetEmployeesTimeSheetResponse.Data) {
        _employeeIdViewModel.value = Event(employeeId)
    }

    fun apiCallTimeSheet(roleId: String) {
        roleIdViewMOdel = roleId

        if (roleId == "-1") {
            roleIdViewMOdel = ""
        }
        _showProgress.value = Event(true)
        viewModelScope.launch {
            val resource = posRepository.employeesTimeSheet(
                startDate.value.toString(),
                endDate.value.toString(),
                roleIdViewMOdel
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

    fun apiCallTimeSheetDetails(teamId: String) {
        _showProgress.value = Event(true)

        viewModelScope.launch {
            val resource = posRepository.employeesTimeSheetDetails(
                startDate.value.toString(),
                endDate.value.toString(),
                teamId
            )

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { logInResponse ->
                        if (logInResponse?.status == 200) {

                            resource.data?.let { timeSheetResponse ->
                                _timeSheetDetails.value = Event(timeSheetResponse)

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

    fun sendEmailTimeSheet(emailId: String, teamId: String) {

        _showProgress.value = Event(true)
        viewModelScope.launch {

            LogUtil.logE("startDate", startDate.value ?: "")
            LogUtil.logE("endDate", endDate.value ?: "")
            var resource: Resource<BaseResponse>? = null
            if (teamId.isNotEmpty()) {
                resource =
                    posRepository.sendEmailReportSummary(
                        startDate = startDate.value.toString(),
                        endDate = endDate.value.toString(),
                        employee_id = teamId,
                        email = emailId
                    )
            } else {
                resource =
                    posRepository.sendEmailReportSummary(
                        startDate = startDate.value.toString(),
                        endDate = endDate.value.toString(),
                        employee_id = "",
                        email = emailId
                    )
            }

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let {
                        if (it?.status == 200) {
                            Log.d("responseMessage", "sendEmailTimeSheet: "+it.message)
                            if (emailId.isEmpty()) {
                                _sendTimeSheet.postValue(Event(it.message))
                            } else {
                                _snackbarText.value = Event(it.message)
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

    fun getEmployeeEmail(emp_id: Int): LiveData<Resource<Employee>> {
        return posRepository.getEmployeeEmail(emp_id)
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

