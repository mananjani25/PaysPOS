package com.android.pos.ui.fragments.printerqueue

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.db.AppDatabase
import com.android.pos.data.repositories.PosRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrinterQueueViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val appDatabase: AppDatabase,
    private val prefProvider: PrefProvider
):ViewModel() {

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private val _snackbarText = MutableLiveData<Event<String?>>()
    val snackbarText: LiveData<Event<String?>> = _snackbarText

    fun clearPrinterQueue(){
        _showProgress.value = Event(true)
        viewModelScope.launch {
            val resource = posRepository.clearPrinterQueue()

            when(resource.status){
                Status.SUCCESS ->{
                    _showProgress.value = Event(false)
                    _snackbarText.value = Event(resource.data?.message.toString())


                }
                Status.ERROR ->{
                    _snackbarText.value = Event(resource.message.toString())
                    _showProgress.value = Event(false)

                }
                Status.LOADING ->{
                    _showProgress.value = Event(true)
                }
            }

        }

    }
}