package com.pays.pos.ui.fragments.settings.tip

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pays.pos.data.db.AppDatabase
import com.pays.pos.data.model.responseModel.BaseResponse
import com.pays.pos.data.model.responseModel.CreateTipResponse
import com.pays.pos.data.model.responseModel.GetTipReponse
import com.pays.pos.data.model.responseModel.NoteResponse
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.data.repositories.TipDiscountRepository
import com.pays.pos.utils.Event
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TipListViewModel @Inject constructor(
    private val tipDiscountRepository: TipDiscountRepository,
    private val appDatabase: AppDatabase
) : ViewModel() {

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<CreateTipResponse?>>()
    val data: LiveData<Event<CreateTipResponse?>> = _data

    private val _data1 = MutableLiveData<Event<BaseResponse?>>()
    val data1: LiveData<Event<BaseResponse?>> = _data1


    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private val _notifydata = MutableLiveData<Event<Boolean?>>()
    val notifydata: LiveData<Event<Boolean?>> = _notifydata

    val getTipList = tipDiscountRepository.getTipList()

    val getTipActiveList = tipDiscountRepository.getTipActiveList()

    fun
            isTipActive(tipDataItem: GetTipReponse.Data) {

        // _showProgress.value = Event(true)

        viewModelScope.launch {
            tipDataItem.isActive = !tipDataItem.isActive

            val resource =
                tipDiscountRepository.tipActive(tipDataItem.id, tipDataItem.isActive)

            when (resource.status) {
                Status.SUCCESS -> {

                    //   _showProgress.value = Event(false)

                    resource.data.let {
                        if (it?.status == 200) {
                            resource.data?.let { baseResponse ->
                                tipDiscountRepository.tipActiveDatabase(
                                    tipDataItem.id,
                                    tipDataItem.isActive
                                )
                                _notifydata.value = Event(true)

                            }
                        } else {
                            _snackbarText.value = Event(resource.message)
                        }

                    }

                }

                Status.ERROR -> {
                    _snackbarText.value = Event(resource.message)
                    //_showProgress.value = Event(false)
                }

                Status.LOADING -> {
                    // _showProgress.value = Event(true)
                }
            }
        }


    }

    fun delete(id: Int) {
        _showProgress.value = Event(true)

        viewModelScope.launch {
            val resource = tipDiscountRepository.deleteTip(id)
            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)

                    resource.data.let {
                        if (it?.status == 200) {
                            resource.data?.let { createTipResponse ->
                                tipDiscountRepository.deleteTipDatabase(id)
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

    fun reOrderItem(itemId: Int, oldPos: Int, newPos: Int) {
        _showProgress.value = Event(true)

        viewModelScope.launch {

            val resource = tipDiscountRepository.reOrderTip(itemId, oldPos, newPos)
            when (resource.status) {
                Status.SUCCESS -> {

                    _showProgress.value = Event(false)

                    resource.data.let {
                        if (it?.status == 200) {
                            resource.data?.let { baseResponse ->
                                _data1.value = Event(baseResponse)
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

    fun reOrder(allItems: ArrayList<GetTipReponse.Data>) {

        if (allItems.isNotEmpty()) {
            viewModelScope.launch {
                appDatabase.tipDao().addAllTips(allItems)
            }
        }
    }
}