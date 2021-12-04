package com.android.pos.ui.fragments.dinein

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.db.AppDatabase
import com.android.pos.data.entities.TbCustomer
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.requestModel.DineInOrderPayment
import com.android.pos.data.model.requestModel.GuestPaymentRequest
import com.android.pos.data.model.requestModel.OrderRequestModel
import com.android.pos.data.model.responseModel.BaseResponse
import com.android.pos.data.model.responseModel.GetOrderDetailsResponse
import com.android.pos.data.model.responseModel.PrinterResponse
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
    private val TAG = "DineInOrderTableViewM"

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    val _Basedata = MutableLiveData<Event<GetOrderDetailsResponse.Data?>>()


    val _guestPayment = MutableLiveData<Event<String>>()
    val onPayment: LiveData<Event<String>> = _guestPayment

    val _unMergeStatus = MutableLiveData<Event<String>>()
    val unMergeStatusUpdate: LiveData<Event<String>> = _unMergeStatus


    val getServiceChargeList = posRepository.serviceChargeList()

    fun getCustomerReceiptSettings() = posRepository.getCustomerReceiptSettings()

    fun getKitchenReceiptSettings() = posRepository.getKitchenReceiptSettings()

    private val _msgText = MutableLiveData<Event<String>>()
    val msgText: LiveData<Event<String>> = _msgText

    var totalTaxAmount = 0.0
    var totalDiscountAmount = 0.0
    var totalAmount = 0.0
    var subTotalAmount = 0.0
    var totalServiceChargeAmount = 0.0


    fun getTipsList() = posRepository.getTipsList()
    fun getCustomerPrinterList(): LiveData<Resource<List<PrinterResponse.Data.CustomerReceiptPrinters>>> {
        return posRepository.getCustomerPrinters()
    }

    fun getKitchenPrinterList(): LiveData<Resource<List<PrinterResponse.Data.KitchenReceiptPrinters>>> {
        return posRepository.getKitchenPrinters()
    }

    fun payByGuest(
        id: Int,
        model: GuestPaymentRequest,
        isAllPaymentComplete: Boolean,
        orderReq: DineInOrderPayment
    ) {
        _showProgress.value = Event(true)
        viewModelScope.launch {
            val resource: Resource<BaseResponse> =
                posRepository.payByGuest(id, isAllPaymentComplete, model)

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { response ->
                        _guestPayment.value = Event(response?.message.toString())
                    }
                }

                Status.ERROR -> {
                    _guestPayment.value = Event(resource.message.toString())
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
        _showProgress.value = Event(true)
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

    fun updateOrder(orderId: Int, orderRequestModel: OrderRequestModel) {
        _showProgress.value = Event(true)

        viewModelScope.launch {
            val resource = posRepository.updateOrder(orderId, orderRequestModel)

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)

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

    public fun discountCalculation(item: TbItem) {
        //totalDiscountAmount += item.discountPrice

    }

    public fun taxCalculation(item: TbItem) {
        item.taxes?.forEach { tax ->
            if (tax.isActive) {
                totalTaxAmount += if (tax.taxType == "Percentage") {

                    var modifierPrice = 0.0
                    val price =
                        (item.price * item.itemQuantity) - item.discountPrice

                    item.modifiers.forEach {
                        modifierPrice += (it.price * it.itemQuantity)
                    }

                    val totalPrice = price + modifierPrice

                    val itemTaxPrice =
                        (tax.rate * totalPrice) / 100
                    Log.e("itemTaxPrice", "" + itemTaxPrice)
                    String.format("%.2f", itemTaxPrice)
                        .toDouble()
                } else {

                    String.format("%.2f", tax.rate * item.itemQuantity)
                        .toDouble()
                }
            }
        }
    }


    fun customer(): LiveData<List<TbCustomer>> {
        return appDatabase.customerDao().allCustomer
    }

    fun unMergeTable(id: Int) {
        _showProgress.value = Event(true)
        viewModelScope.launch {
            val resource = posRepository.unMergeTable(id)
            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    _unMergeStatus.value = Event(resource.data?.message.toString())

                }
                Status.ERROR -> {
                    _snackbarText.value = Event(resource.message.toString())
                    _showProgress.value = Event(false)


                }
                Status.LOADING -> {
                    _showProgress.value = Event(true)

                }

            }

        }


    }


}