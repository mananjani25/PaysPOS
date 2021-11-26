package com.android.pos.ui.fragments.dinein

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.model.responseModel.BaseResponse
import com.android.pos.data.model.responseModel.CreateNoteResponse
import com.android.pos.data.model.responseModel.NoteResponse
import com.android.pos.data.remote.Constants.EMPLOYEE_ID
import com.android.pos.data.remote.Constants.LOCATION_ID
import com.android.pos.data.remote.Constants.TERMINAL_ID
import com.android.pos.data.repositories.PosRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Resource
import com.android.pos.utils.statusUtils.Status
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DineInViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val prefProvider: PrefProvider
) : ViewModel() {

    private val TAG = "DineInViewModel"

    private val _snackbarText = MutableLiveData<Event<String?>>()
    val snackbarText: LiveData<Event<String?>> = _snackbarText

    private val _data = MutableLiveData<Event<CreateNoteResponse?>>()
    val data: LiveData<Event<CreateNoteResponse?>> = _data

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    val _tableStatus = MutableLiveData<Event<String>>()
    val tableCheck: LiveData<Event<String>> = _tableStatus

    val _tableStatusSuccess = MutableLiveData<Event<Int>>()
    val tableCheckSuccess: LiveData<Event<Int>> = _tableStatusSuccess

    val _mergeStatus = MutableLiveData<Event<String>>()
    val mergeStatusChange: LiveData<Event<String>> = _mergeStatus

    val _unMergeStatus = MutableLiveData<Event<String>>()
    val unMergeStatusUpdate: LiveData<Event<String>> = _unMergeStatus

    val getFloorPlan = posRepository.getFloorPlan(prefProvider.getValueInt(LOCATION_ID, 0))

    val getFloorPlanDetails = posRepository.getFloorPlanTableDetails()

    fun mergeTable(parentTableId: Int, childIds: String) {
        _showProgress.value = Event(true)
        viewModelScope.launch {
            val resource = posRepository.mergeFloorTable(parentTableId, childIds)

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    (resource.data?.message?.let {
                        _mergeStatus.value = Event(it)
                    })
                }
                Status.LOADING -> {
                    _showProgress.value = Event(true)

                }
                Status.ERROR -> {
                    _snackbarText.value = Event(resource.message.toString())
                    _showProgress.value = Event(false)

                }

            }

        }
    }

    fun unMergeTable(id: Int) {
        _showProgress.value = Event(true)
        viewModelScope.launch {
            val resource = posRepository.unMergeTable(id)
            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    _mergeStatus.value = Event(resource.data?.message.toString())

                }
                Status.ERROR -> {
                    _snackbarText.value = Event(resource.message.toString())
                    _showProgress.value = Event(false)


                }
                Status.LOADING -> {
                    _showProgress.value = Event(true)

                }

            }

        }


    }

    fun getTableStatus(tableId: Int, status: String) {
        _showProgress.value = Event(true)
        viewModelScope.launch {
            val resource = posRepository.getTableStatus(
                tableId, prefProvider.getValueInt(
                    EMPLOYEE_ID, 0
                ), prefProvider.getValueInt(TERMINAL_ID, 0), status
            )

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { response ->
                        Log.e(TAG, "getTableStatusResponse:  ${Gson().toJson(response)}")
                        if (response?.status == 200) {
                            _tableStatusSuccess.value = Event(response.status)

                        } else {
                            _tableStatus.value = response?.let { Event(it.message) }
                        }

                    }
                }

                Status.ERROR -> {
                    _snackbarText.value = Event(resource.message.toString())
                    _showProgress.value = Event(false)
                }

                Status.LOADING -> {
                    _showProgress.value = Event(true)
                }
            }

        }

    }

}