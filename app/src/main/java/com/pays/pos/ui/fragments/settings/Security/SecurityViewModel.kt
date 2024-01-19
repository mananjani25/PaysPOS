package com.pays.pos.ui.fragments.settings.Security


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.Event

import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val prefProvider: PrefProvider
) : ViewModel() {


    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText


    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress


    private val _data = MutableLiveData<Event<Boolean>>()
    val data: LiveData<Event<Boolean>> = _data

    fun updateTransactionLock(status: Boolean) {
        _showProgress.value = Event(true)
        viewModelScope.launch {
            var resource = posRepository.updateTransactionLockScreen(status)

            when (resource.status) {
                Status.SUCCESS -> {
                    resource.data.let { response ->
                        if (response?.status == 200) {
                            _showProgress.value = Event(false)
                            resource.data?.let { transactionres ->
                                _data.value = Event(true)

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


