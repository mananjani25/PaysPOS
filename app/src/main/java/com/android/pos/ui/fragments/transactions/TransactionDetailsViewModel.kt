package com.android.pos.ui.fragments.transactions

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.entities.CashDiscountModel
import com.android.pos.data.model.GetPaymentOrderDetailsResponse
import com.android.pos.data.model.requestModel.CashInOutModel
import com.android.pos.data.model.requestModel.CashInOutPaymentModel
import com.android.pos.data.model.requestModel.CashLogRequest
import com.android.pos.data.model.requestModel.RefundRequestModel
import com.android.pos.data.model.responseModel.BaseResponse
import com.android.pos.data.model.responseModel.GetOrderDetailsResponse
import com.android.pos.data.model.responseModel.OnlineOrderStatusUpdateResponse
import com.android.pos.data.model.responseModel.PrinterResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.repositories.PosRepository
import com.android.pos.data.repositories.TaxServiceChargeRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Resource
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionDetailsViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val taxServiceChargeRepository: TaxServiceChargeRepository,
    private val prefProvider: PrefProvider
) : ViewModel() {

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<GetOrderDetailsResponse?>>()
    val data: LiveData<Event<GetOrderDetailsResponse?>> = _data


    private val _data1 = MutableLiveData<Event<BaseResponse?>>()
    val data1: LiveData<Event<BaseResponse?>> = _data1

    private val _data2 = MutableLiveData<Event<Double>>()
    val data2: LiveData<Event<Double>> = _data2

    private val _datapayment = MutableLiveData<Event<GetPaymentOrderDetailsResponse?>>()
    val dataPayment: LiveData<Event<GetPaymentOrderDetailsResponse?>> = _datapayment

    private val _dataRefundDone = MutableLiveData<Event<BaseResponse?>>()
    val dataRefundDone: LiveData<Event<BaseResponse?>> = _dataRefundDone

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    val startDate = MutableLiveData<String>()

    fun getTipsList() = posRepository.getTipsList()

    fun getCustomerReceiptSettings() = posRepository.getCustomerReceiptSettings()

    fun getKitchenReceiptSettings() = posRepository.getKitchenReceiptSettings()

    fun getCustomerPrinterList(): LiveData<Resource<List<PrinterResponse.Data.CustomerReceiptPrinters>>> {
        return posRepository.getCustomerPrinters()
    }

    fun getKitchenPrinterList(): LiveData<Resource<List<PrinterResponse.Data.KitchenReceiptPrinters>>> {
        return posRepository.getKitchenPrinters()
    }

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

    fun acceptedAndDeclineOrder(
        time: Int,
        order_id: Int,
        isaccepted: Boolean,
        employee_id: Int,
        terminalid: Int
    ): LiveData<Resource<OnlineOrderStatusUpdateResponse>> =
        posRepository.acceptedAndDeclineOrders(time, order_id, isaccepted, employee_id, terminalid)


    fun getCashDiscountDetails(active: Int): LiveData<CashDiscountModel>? {
        return posRepository.getCashDisDetail(active)
    }

    fun showProgressDialog(isShow: Boolean) {
        _showProgress.value = Event(isShow)
    }

    val serviceCharges = posRepository.serviceChargeList()
    fun refundPaymentApiCall(
        refundAmount: Double,
        refundData: RefundRequestModel,
        refundReason: String,
        paymentType: String
    ) {

        refundData.paymentRefund?.reasonForRefund = refundReason
        refundData.paymentRefund?.amount = refundAmount

        if (refundData.paymentRefund?.employeeId != prefProvider.employeeId()) {
            refundData.paymentRefund?.employeeId = prefProvider.employeeId()
        }

        // If payment type is card then progress bar is already enabled from networkCall method
        if (paymentType != "Card") {
            _showProgress.value = Event(true)
        }

        viewModelScope.launch {

            val resource = taxServiceChargeRepository.refundPayment(refundData)
            when (resource.status) {
                Status.SUCCESS -> {
//                    _showProgress.value = Event(false)
                    resource.data.let { logInResponse ->
                        if (logInResponse?.status == 200) {

                            resource.data?.let { createTaxResponse ->


                                if (paymentType == "Card" || paymentType == Constants.EXTERNAL_PAYMENT) {
                                    _showProgress.value = Event(false)
                                    _dataRefundDone.value = Event(createTaxResponse)

                                } else {
                                    cashOutApi(
                                        refundData,
                                        refundAmount,
                                        createTaxResponse,
                                        refundReason
                                    )
                                }


                            }


                        } else {
                            _snackbarText.value = Event(resource.message)
                            _showProgress.value = Event(false)
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

    fun apiCallPaymentDetails(paymentId: Int) {

        _showProgress.value = Event(true)
        viewModelScope.launch {

            val resource = posRepository.paymentDetailsById(paymentId)


            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { logInResponse ->
                        if (logInResponse?.status == 200) {

                            resource.data?.let { createTaxResponse ->
                                _datapayment.value = Event(createTaxResponse)
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

    fun orderUpdateTip(orderID: Int, tipAmount: Double, is_captured: Boolean) {

        val paymentModel = CashInOutPaymentModel()
        paymentModel.id = orderID
        paymentModel.isCaptured = is_captured
        val data = CashInOutModel()
        data.paymentAttributes = paymentModel

        viewModelScope.launch {

            val resource = posRepository.orderUpdateTip(orderID, tipAmount, is_captured, data)

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { baseResponse ->
                        if (baseResponse?.status == 200) {
                            resource.data?.let { response ->
                                _data1.value = Event(response)
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

    private suspend fun cashOutApi(
        refundRequestModel: RefundRequestModel,
        amount: Double,
        createTaxResponse: BaseResponse,
        refundReason: String
    ) {

//        _showProgress.value = Event(true)

        val order = refundRequestModel.paymentRefund

        val cashLogRequest = order?.employeeId?.let {
            order.orderId?.let { it1 ->
                order.paymentId?.let { it2 ->
                    order.terminalId?.let { it3 ->
                        CashLogRequest(
                            amount,
                            it,
                            "out",
                            it1,
                            it2,
                            refundReason,
                            it3,
                            null,
                            order.tipsRefunded
                        )
                    }
                }
            }
        }

        val resource = cashLogRequest?.let { posRepository.cashInOut(it) }

        if (resource != null) {
            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { response ->
                        if (response?.status == 200) {

                            resource.data?.let {

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

