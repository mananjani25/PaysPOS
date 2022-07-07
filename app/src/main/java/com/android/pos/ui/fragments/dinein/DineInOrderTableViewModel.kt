package com.android.pos.ui.fragments.dinein

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.db.AppDatabase
import com.android.pos.data.entities.CartModel
import com.android.pos.data.entities.CashDiscountModel
import com.android.pos.data.entities.TbCustomer
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.requestModel.*
import com.android.pos.data.model.responseModel.BaseResponse
import com.android.pos.data.model.responseModel.CreateOrderResponse
import com.android.pos.data.model.responseModel.GetOrderDetailsResponse
import com.android.pos.data.model.responseModel.PrinterResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.repositories.PosRepository
import com.android.pos.data.repositories.TaxServiceChargeRepository
import com.android.pos.data.repositories.TipDiscountRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.TimeFormatUtils
import com.android.pos.utils.statusUtils.Resource
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.ArrayList
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
    private val _updateOrder = MutableLiveData<Event<Any?>>()
    val updateOrder: LiveData<Event<Any?>> = _updateOrder


    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _fireAllStatus = MutableLiveData<Event<Boolean>>()
    val fireAllStatus: LiveData<Event<Boolean>> = _fireAllStatus

    private val _fireSingleStatus = MutableLiveData<Event<TbItem?>>()
    val fireSingleStatus: LiveData<Event<TbItem?>> = _fireSingleStatus

    val _Basedata = MutableLiveData<Event<GetOrderDetailsResponse.Data?>>()
    val Basedata: LiveData<Event<GetOrderDetailsResponse.Data?>> = _Basedata

    private val _queueCreateSuccess = MutableLiveData<Event<Boolean>>()
    val queueCreateSuccess: LiveData<Event<Boolean>> = _queueCreateSuccess


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
    private var totalPayAmounts: Double = 0.0

    fun getTipsList() = posRepository.getTipsList()
    fun getCustomerPrinterList(): LiveData<Resource<List<PrinterResponse.Data.CustomerReceiptPrinters>>> {
        return posRepository.getCustomerPrinters()
    }

    fun getCashDiscountDetails(active: Int): LiveData<CashDiscountModel>? {
        return posRepository.getCashDisDetail(active)
    }

    fun getKitchenPrinterList(): LiveData<Resource<List<PrinterResponse.Data.KitchenReceiptPrinters>>> {
        return posRepository.getKitchenPrinters()
    }

    fun totalPayAmount(paymentAmount: Double) {

        totalPayAmounts = MethodUtils.roundOffAmountDouble(paymentAmount)
    }

    fun payByGuest(
        id: Int,
        model: GuestPaymentRequest,
        isAllPaymentComplete: Boolean,
        orderReq: DineInOrderPayment
    ) {
        _showProgress.value = Event(true)
        viewModelScope.launch {
            val resource = posRepository.payByGuest(id, isAllPaymentComplete, model)

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { response ->

                        if (response != null) {
                            cashLogApi(response, "in")
                        }

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

    fun fireItemToKitchen(
        id: Int,
        status: Boolean,
        itemIds: String,
        isAllFired: Boolean,
        item: TbItem? = null
    ) {

        _showProgress.value = Event(true)

        viewModelScope.launch {
            val resource: Resource<BaseResponse> =
                posRepository.updateKitchenFireStatus(id, status, itemIds)
            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { response ->
                        if (response?.status == 200) {

                            if (isAllFired) {
                                _fireAllStatus.value = Event(true)
                            } else {
                                _fireSingleStatus.value = Event(item)
                            }
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
                    _updateOrder.value = Event("Guest Added.")

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

    fun createQueuePrinter(
        createQueuePrinterModel: CreateQueuePrinterRequestModel
    ) {
        _showProgress.value = Event(true)

        viewModelScope.launch {
            val resource = posRepository.createQueuePrinter(createQueuePrinterModel)

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    _queueCreateSuccess.value = Event(true)


                }
                Status.LOADING -> {
                    _showProgress.value = Event(true)

                }
                Status.ERROR -> {
                    _snackbarText.value = Event(resource.message)
                    _showProgress.value = Event(false)
                }
            }


        }


    }
    fun updateOrderRequest(cartModel: CartModel): OrderRequestModel {
        val orderModel: OrderAttributeRequestModel = OrderAttributeRequestModel()
        Log.e(TAG, "getCartmodelId  ${cartModel.orderId}")
        orderModel.apply {
            guestsAttributes = getGuestsAttributes(cartModel)
        }
        return OrderRequestModel(false, orderModel)


    }
    private fun getGuestsAttributes(cartModel: CartModel): List<GuestsAttributes> {
        val orderItemsAttributeList: ArrayList<GuestsAttributes> = arrayListOf()
        cartModel.dineInList?.forEach { it ->
            val model = GuestsAttributes()
            model.name = it.title.toString()
            if (it.id != 0) {
                model.id = it.id
            }
            if (it.items.isNotEmpty()) {
                var listItems: ArrayList<GuestItemsAttributes> = arrayListOf()
                var subTotal = 0.0
                var totalTax = 0.0
                var totalTips = 0.0
                var totalDiscount = 0.0
                var totalAmount = 0.0
                it.items.forEach { tb ->


                    listItems.add(
                        GuestItemsAttributes(
                            id = tb.guestItemId,
                            orderItemId = tb.orderItemId,
                            quantity = tb.itemQuantity,
                            itemId = tb.itemId,
                            amount = tb.price,
                            timestamp = tb.timeStamp,
                            guestId = it.id?.let { it }

                        )

                    )



                    subTotal += tb.price
                    tb.taxes?.forEach {
                        totalTax += it.rate
                    }
                    totalDiscount += tb.discountPrice

                }
                totalAmount = (subTotal + totalTax) - totalDiscount
                model.totalAmount = totalAmount
                model.totalTax = totalTax
                model.totalTips = totalTips
                if (it.id != null && it.id != 0) {
                    model.id = it.id
                }



                model.guestItemsAttributes = listItems
            }


            if (it.customer != null) {
                model.customerId = it.customer?.id
                var addressList: ArrayList<CustomerAttributes.AddressesAttribute> =
                    arrayListOf()
                var phoneList: ArrayList<CustomerAttributes.PhonesAttribute> = arrayListOf()
                for (i in 0.until(it.customer?.addresses?.size!!)) {

                    var address = CustomerAttributes.AddressesAttribute()
                    address.address1 = it.customer?.addresses?.get(i)?.address1.toString()
                    address.address2 = it.customer?.addresses?.get(i)?.address2.toString()
                    address.addressableId = it.customer?.addresses?.get(i)?.id
                    address.city = it.customer?.addresses?.get(i)?.city.toString()
                    address.country = it.customer?.addresses?.get(i)?.country.toString()
                    /*address.latitude = it.customer?.addresses?.get(i)?.latitude!!.toDouble()
                    address.longitude = it.customer?.addresses?.get(i)?.longitude!!.toDouble()*/
                    address.latitude = 0.0
                    address.longitude = 0.0
                    address.state = it.customer?.addresses?.get(i)?.state.toString()
                    addressList.add(address)
                }
                for (i in 0 until it.customer?.phones?.size!!) {
                    val phoneModel = CustomerAttributes.PhonesAttribute()
                    phoneModel.id = it.customer?.phones?.get(i)?.id
                    phoneModel.customerId = it.customer?.id
                    phoneModel.phoneNumber =
                        it.customer?.phones?.get(i)?.phone_number.toString()
                    phoneList.add(phoneModel)
                }
                val customerModel = CustomerAttributes()
                /*  customerModel.addressesAttributes = addressList
                  customerModel.birthDate = it.customer?.birth_date.toString()
                  customerModel.firstName = it.customer?.first_name.toString()
                  customerModel.lastName = it.customer?.last_name.toString()*/
                customerModel.id = it.customer?.id
                /* customerModel.companyName = it.customer?.company.toString()
                 customerModel.phonesAttributes = phoneList
                 customerModel.locationId = prefProvider.getValueInt(LOCATION_ID, 1)
    */
                //  model.customerAttributes = customerModel

            } else {
                model.customerId = 0
            }
            orderItemsAttributeList.add(model)


        }

        return orderItemsAttributeList

    }
    private suspend fun cashLogApi(createOrderResponse: CreateOrderResponse, event: String) {


        val order = createOrderResponse.data.order
        val cashLogRequest = CashLogRequest(
            totalPayAmounts,
            order.employeeId,
            event,
            order.id,
            order.payments[order.payments.size - 1].id,
            "Payment received for order",
            order.terminalId,
            null,
            order.payments[order.payments.size - 1].tips
        )


        val resource = posRepository.cashInOut(cashLogRequest)

        when (resource.status) {
            Status.SUCCESS -> {
                _showProgress.value = Event(false)
                resource.data.let { response ->
                    if (response?.status == 200) {

                        resource.data?.let {


                            Log.e(
                                "INOUT : Total Amount",
                                order.payments[order.payments.size - 1].amount.toString()
                            )
                            Log.e("INOUT : Total PayAmount", totalPayAmounts.toString())

                            if (order.payments.isNotEmpty()) {
                                if (order.payments[order.payments.size - 1].amount == totalPayAmounts) {

                                    _guestPayment.value =
                                        Event(createOrderResponse.message.toString())

                                } else {
                                    cashOutApi(createOrderResponse, "out")
                                }
                            }


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

    private suspend fun cashOutApi(createOrderResponse: CreateOrderResponse, event: String) {

        val order = createOrderResponse.data.order

        val cashLogRequest = CashLogRequest(
            MethodUtils.roundOffAmountDouble(totalPayAmounts) - order.payments[order.payments.size - 1].amount,
            order.employeeId,
            event,
            order.id,
            order.payments[order.payments.size - 1].id,
            "Change returned after order's payment",
            order.terminalId,
            null,
            order.payments[order.payments.size - 1].tips
        )

        val resource = posRepository.cashInOut(cashLogRequest)

        when (resource.status) {
            Status.SUCCESS -> {
                _showProgress.value = Event(false)
                resource.data.let { response ->
                    if (response?.status == 200) {

                        resource.data?.let {

                            _guestPayment.value =
                                Event(createOrderResponse.message.toString())

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