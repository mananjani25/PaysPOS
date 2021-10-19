package com.android.pos.ui.fragments.dinein

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.db.AppDatabase
import com.android.pos.data.model.DineInModel
import com.android.pos.data.model.requestModel.GuestPaymentRequest
import com.android.pos.data.model.responseModel.BaseResponse
import com.android.pos.data.model.responseModel.CreateOrderResponse
import com.android.pos.data.model.responseModel.GetOrderDetailsResponse
import com.android.pos.data.repositories.PosRepository
import com.android.pos.data.repositories.TaxServiceChargeRepository
import com.android.pos.data.repositories.TipDiscountRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Resource
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DineInOrderTableViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val appDatabase: AppDatabase,
    private val prefProvider: PrefProvider,
    private val taxServiceChargeRepository: TaxServiceChargeRepository,
    private val tipDiscountRepository: TipDiscountRepository
) : ViewModel() {
    private val TAG = "DineInOrderTableViewModel"

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    val _Basedata = MutableLiveData<Event<GetOrderDetailsResponse.Data?>>()


    private val _msgText = MutableLiveData<Event<String>>()
    val msgText: LiveData<Event<String>> = _msgText


    fun payByGuest(id: Int, model: GuestPaymentRequest) {
        _showProgress.value = Event(true)
        viewModelScope.launch {
            val resource: Resource<BaseResponse> =
                posRepository.payByGuest(id, false, model)

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { response ->
                        if (response?.status == 200) {
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

    fun fireItemToKitchen(id: Int, status: Boolean, itemIds: String) {

        _showProgress.value = Event(true)

        viewModelScope.launch {
            val resource: Resource<BaseResponse> =
                posRepository.updateKitchenFireStatus(id, status, itemIds)
            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { response ->
                        if (response?.status == 200) {
                            _snackbarText.value = Event(resource.message)
                            _msgText.value = Event(response.message)
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

    fun apiCallOrderDetails(orderId: Int) {
        viewModelScope.launch {

            val resource = posRepository.orderDetailsById(orderId)


            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { logInResponse ->
                        if (logInResponse?.status == 200) {

                            resource.data?.let { createTaxResponse ->
                                _Basedata.value = Event(createTaxResponse.data)
                                //_data.value = Event(createTaxResponse)
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