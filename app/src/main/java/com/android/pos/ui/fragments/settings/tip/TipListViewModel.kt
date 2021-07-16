package com.android.pos.ui.fragments.settings.tip

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.model.responseModel.CreateTipResponse
import com.android.pos.data.repositories.PosRepository
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TipListViewModel @Inject constructor(
    private val posRepository: PosRepository
) : ViewModel() {

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<CreateTipResponse?>>()
    val data: LiveData<Event<CreateTipResponse?>> = _data

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    val getTipList = posRepository.getTipList()


    fun delete(id: Int) {
        _showProgress.value = Event(true)

        viewModelScope.launch {
            val resource = posRepository.deleteTip(id)
            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)

                    resource.data.let {
                        if (it?.status == 200) {
                            resource.data?.let { createTipResponse ->
                                _data.value = Event(createTipResponse)
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