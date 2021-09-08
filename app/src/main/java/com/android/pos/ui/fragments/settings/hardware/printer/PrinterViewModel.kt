package com.android.pos.ui.fragments.settings.hardware.printer

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.db.AppDatabase
import com.android.pos.data.model.requestModel.CreatePrinterRequestModel
import com.android.pos.data.model.responseModel.DeletePrinterResponseModel
import com.android.pos.data.model.responseModel.PrinterResponse
import com.android.pos.data.repositories.PosRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Status
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.util.Util
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrinterViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val appDataBase: AppDatabase,
    private val prefProvider: PrefProvider
) : ViewModel() {

    private val TAG = "PrinterViewModel"

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private var _delete = MutableLiveData<Event<String>>()
    val deletePrinter: LiveData<Event<String>> = _delete

    private var _update = MutableLiveData<Event<String>>()
    val updatePrinter: LiveData<Event<String>> = _update


    fun printerList() = posRepository.getPrinters()

    fun updatePrinterStatus(id: Int, terminal_id: Int, status: Boolean) {
        _showProgress.value = Event(true)
        viewModelScope.launch {
            val resource: com.android.pos.utils.statusUtils.Resource<DeletePrinterResponseModel> =
                posRepository.updatePrinterStatus(id, terminal_id, status)

            when (resource.status) {
                Status.LOADING -> {
                    _showProgress.value = Event(true)
                }
                Status.ERROR -> {
                    _snackbarText.value = Event(resource.message)
                    _showProgress.value = Event(false)

                }
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                }
            }

        }


    }

    fun updatePrinter(id: Int, model: CreatePrinterRequestModel) {
        _showProgress.value = Event(true)
        viewModelScope.launch {

            val resource: com.android.pos.utils.statusUtils.Resource<DeletePrinterResponseModel> =
                posRepository.updatePrinter(id, model)
            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    _update.value = Event(resource.data?.message!!)


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

    fun deletePrinter(id: Int) {
        _showProgress.value = Event(true)
        viewModelScope.launch {
            val resource: com.android.pos.utils.statusUtils.Resource<DeletePrinterResponseModel> =
                posRepository.deletePrinter(id)

            when (resource.status) {
                Status.SUCCESS -> {
                    Log.e("PrinterViewModel", "PrinterDeleted")
                    _showProgress.value = Event(false)
                    printerList()
                    _delete.value = Event("Printer_deleted")
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

    fun createPrinter(data: CreatePrinterRequestModel) {
        _showProgress.value = Event(true)

        viewModelScope.launch {
            val resource: com.android.pos.utils.statusUtils.Resource<PrinterResponse> =
                posRepository.createPrinter(data)


            when (resource.status) {
                Status.LOADING -> {
                    _showProgress.value = Event(true)

                }
                Status.ERROR -> {
                    _snackbarText.value = Event(resource.message)
                    _showProgress.value = Event(false)

                }
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    printerList()

                    Log.e(TAG, "resourceData:  ${Gson().toJson(resource.data)}")

                }

            }
        }

    }

}