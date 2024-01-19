package com.pays.pos.ui.fragments.settings.discount

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pays.pos.data.entities.TbDiscount
import com.pays.pos.data.model.responseModel.CreateDiscountResponse
import com.pays.pos.data.model.responseModel.CreateTaxResponse
import com.pays.pos.data.model.responseModel.GetDiscountResponse
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.data.repositories.TipDiscountRepository
import com.pays.pos.utils.Event
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiscountListViewModel @Inject constructor(
    private val tipDiscountRepository: TipDiscountRepository
) : ViewModel() {

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<CreateDiscountResponse?>>()
    val data: LiveData<Event<CreateDiscountResponse?>> = _data

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private val _notifydata = MutableLiveData<Event<Boolean?>>()
    val notifydata: LiveData<Event<Boolean?>> = _notifydata

    val getDiscountList = tipDiscountRepository.getDiscountsList()
    val discountList = tipDiscountRepository.discountList()

    fun isDiscountActive(discountDataItem: TbDiscount) {

        // _showProgress.value = Event(true)

        viewModelScope.launch {
            discountDataItem.isActive = !discountDataItem.isActive

            val resource =
                tipDiscountRepository.discountActive(discountDataItem.id, discountDataItem.isActive)

            when (resource.status) {
                Status.SUCCESS -> {

                    //   _showProgress.value = Event(false)

                    resource.data.let {
                        if (it?.status == 200) {
                            resource.data?.let { baseResponse ->
                                tipDiscountRepository.discountActiveDatabase(
                                    discountDataItem.id,
                                    discountDataItem.isActive
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
                val resource = tipDiscountRepository.deleteDiscount(id)
                when (resource.status) {
                    Status.SUCCESS -> {
                        _showProgress.value = Event(false)

                        resource.data.let {
                            if (it?.status == 200) {
                                resource.data?.let { createTaxResponse ->
                                    tipDiscountRepository.deleteDiscountDatabase(id)
                                    _data.value = Event(createTaxResponse)
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