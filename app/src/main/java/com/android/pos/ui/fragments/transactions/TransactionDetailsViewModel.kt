package com.android.pos.ui.fragments.transactions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.model.requestModel.RefundRequestModel
import com.android.pos.data.model.responseModel.BaseResponse
import com.android.pos.data.model.responseModel.GetOrderDetailsResponse
import com.android.pos.data.repositories.PosRepository
import com.android.pos.data.repositories.TaxServiceChargeRepository
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionDetailsViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val taxServiceChargeRepository: TaxServiceChargeRepository
) : ViewModel() {

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<GetOrderDetailsResponse?>>()
    val data: LiveData<Event<GetOrderDetailsResponse?>> = _data

    private val _dataRefundDone = MutableLiveData<Event<BaseResponse?>>()
    val dataRefundDone: LiveData<Event<BaseResponse?>> = _dataRefundDone

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    val startDate = MutableLiveData<String>()

    val endDate = MutableLiveData<String>()

        fun apiCallOrderDetails(orderId: Int) {
            viewModelScope.launch {

                val resource = posRepository.orderDetailsById(orderId)


                when (resource.status) {
                    Status.SUCCESS -> {
                        _showProgress.value = Event(false)
                        resource.data.let { logInResponse ->
                            if (logInResponse?.status == 200) {

                                resource.data?.let { createTaxResponse ->
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


    fun refundPaymentApiCall(
        refundAmount: Double,
        refundData: RefundRequestModel,
        refundReason: String
    ) {

        refundData.paymentRefund?.reasonForRefund = refundReason
        refundData.paymentRefund?.amount = refundAmount
        viewModelScope.launch {

            val resource = taxServiceChargeRepository.refundPayment(refundData)
            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { logInResponse ->
                        if (logInResponse?.status == 200) {

                            resource.data?.let { createTaxResponse ->
                                _dataRefundDone.value = Event(createTaxResponse)
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

