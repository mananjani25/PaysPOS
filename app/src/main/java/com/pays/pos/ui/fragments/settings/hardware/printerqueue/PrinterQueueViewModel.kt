package com.pays.pos.ui.fragments.settings.hardware.printerqueue

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pays.pos.data.db.AppDatabase
import com.pays.pos.data.model.responseModel.PrinterResponse
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.Event
import com.pays.pos.utils.statusUtils.Resource
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrinterQueueViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val appDatabase: AppDatabase,
    private val prefProvider: PrefProvider,
) : ViewModel() {
    private val TAG = "PrinterQueueViewModel"

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    val _deleteQueue = MutableLiveData<Event<Int>>()
    val deleteQueue: LiveData<Event<Int>> = _deleteQueue

    val _deleteAllQueue = MutableLiveData<Event<String>>()
    val deleteAllQueue: LiveData<Event<String>> = _deleteAllQueue


    fun getKitchenPrinterList(): LiveData<Resource<List<PrinterResponse.Data.KitchenReceiptPrinters>>> {
        return posRepository.getKitchenPrinters()
    }

    fun getKitchenReceiptSettings() = posRepository.getKitchenReceiptSettings()
    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText


    fun deleteAllQueuePrinter(id: Array<Int>) {
        _showProgress.value = Event(true)
        viewModelScope.launch {
            val resource = posRepository.deleteAllQueuePrinter(id)
            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    _deleteAllQueue.value = Event(resource.data?.message.toString())

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

    fun deleteQueuePrinter(id: Int, pos: Int) {
        //_showProgress.value = Event(true)
        viewModelScope.launch {
            val resource = posRepository.deleteQueuePrinter(id)

            when (resource.status) {
                Status.SUCCESS -> {
                    // _showProgress.value = Event(false)
                    _deleteQueue.value = Event(pos)

                }
                Status.LOADING -> {
                    //  _showProgress.value = Event(true)

                }
                Status.ERROR -> {
                    _snackbarText.value = Event(resource.message)
                    // _showProgress.value = Event(false)

                }
            }

        }
    }

}