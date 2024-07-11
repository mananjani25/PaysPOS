package com.pays.pos.ui.fragments.dineInNew

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pays.pos.data.db.AppDatabase
import com.pays.pos.data.entities.CartModel
import com.pays.pos.data.entities.CashDiscountModel
import com.pays.pos.data.entities.TbCartItem
import com.pays.pos.data.entities.TbCustomer
import com.pays.pos.data.entities.TbItem
import com.pays.pos.data.model.requestModel.*
import com.pays.pos.data.model.responseModel.BaseResponse
import com.pays.pos.data.model.responseModel.CreateOrderResponse
import com.pays.pos.data.model.responseModel.GetOrderDetailsResponse
import com.pays.pos.data.model.responseModel.PrinterResponse
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.DINE_IN
import com.pays.pos.data.remote.Constants.ORDER_TYPE_ID
import com.pays.pos.data.remote.Constants.ORDER_TYPE_NAME
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.data.repositories.TaxServiceChargeRepository
import com.pays.pos.data.repositories.TipDiscountRepository
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.Event
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.MethodUtils
import com.pays.pos.utils.TimeFormatUtils
import com.pays.pos.utils.statusUtils.Resource
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DineInOrderTableViewModelPays @Inject constructor(
    private val posRepository: PosRepository,
    private val appDatabase: AppDatabase,
    private val prefProvider: PrefProvider,
    private val taxServiceChargeRepository: TaxServiceChargeRepository,
    private val tipDiscountRepository: TipDiscountRepository
) : ViewModel() {
    private val TAG = "DineInOrderTableViewM"

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress
    private val _updateOrder = MutableLiveData<Event<String>>()
    val updateOrder: LiveData<Event<String>> = _updateOrder

    private val _showProgressCash = MutableLiveData<Event<Boolean>>()
    val showProgressCash: LiveData<Event<Boolean>> = _showProgressCash

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _fireAllStatus = MutableLiveData<Event<Boolean>>()
    val fireAllStatus: LiveData<Event<Boolean>> = _fireAllStatus

    private val _fireSingleStatus = MutableLiveData<Event<TbCartItem?>>()
    val fireSingleStatus: LiveData<Event<TbCartItem?>> = _fireSingleStatus

    val _Basedata = MutableLiveData<Event<GetOrderDetailsResponse.Data?>>()
    val Basedata: LiveData<Event<GetOrderDetailsResponse.Data?>> = _Basedata

    private val _queueCreateSuccess = MutableLiveData<Event<Boolean>>()
    val queueCreateSuccess: LiveData<Event<Boolean>> = _queueCreateSuccess

    private val _reorderItemsSuccess = MutableLiveData<Event<CreateOrderResponse.Data?>>()
    val reorderItemsSuccess: LiveData<Event<CreateOrderResponse.Data?>> = _reorderItemsSuccess

    val _removeGuestSuccess = MutableLiveData<Event<String>>()
    var removeGuestSuccess: LiveData<Event<String>> = _removeGuestSuccess

    private val _wastageItemsSuccess = MutableLiveData<Event<String>>()
    val wastageItemsSuccess: LiveData<Event<String>> = _wastageItemsSuccess

    val _guestPayment = MutableLiveData<Event<String>>()
    val onPayment: LiveData<Event<String>> = _guestPayment

    val _unMergeStatus = MutableLiveData<Event<String>>()
    val unMergeStatusUpdate: LiveData<Event<String>> = _unMergeStatus


    val getServiceChargeList = posRepository.serviceChargeList()
    val getAllWastageReasonsList = posRepository.getWastageReasonsListFromDb()

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
        if (cashPaymentType(model)) {
            _showProgressCash.value = Event(true)
        } else
            _showProgress.value = Event(true)

//        _showProgress.value = Event(true)
        viewModelScope.launch {
            val resource = posRepository.payByGuest(id, isAllPaymentComplete, model)

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { response ->

                        if (response != null && response.data.order.payments.get(response.data.order.payments.size - 1).paymentType.equals(
                                "Cash",
                                true
                            )
                        ) {
                            cashLogApi(response, "in")
                        } else {
                            _guestPayment.value =
                                Event(response?.message.toString())
                        }

                    }
                }

                Status.ERROR -> {
                    _guestPayment.value = Event(resource.message.toString())
                    if (cashPaymentType(model)) {
                        _showProgressCash.value = Event(false)
                    } else
                        _showProgress.value = Event(false)

//                    _showProgress.value = Event(false)
                }

                Status.LOADING -> {
                    if (cashPaymentType(model)) {
                        _showProgressCash.value = Event(true)
                    } else
                        _showProgress.value = Event(true)

//                    _showProgress.value = Event(true)
                }
            }

        }

    }

    private fun cashPaymentType(model: GuestPaymentRequest): Boolean {

        return model.paymentAttributes.paymentType.equals(
            "Cash",
            ignoreCase = true
        )
    }

    fun fireItemToKitchen(
        id: Int,
        status: Boolean,
        itemIds: String,
        isAllFired: Boolean,
        item: TbCartItem? = null
    ) {
        Log.d("###17MAR23", "fireItemToKitchen: Called - Start")
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
                    //_showProgress.value = Event(false)
                }

                Status.LOADING -> {
                    _showProgress.value = Event(true)
                }
            }
        }
    }

    fun apiCallOrderDetails(orderId: Int) {
        Log.d("###17MAR23", "apiCallOrderDetails: Called - Start")
        _showProgress.value = Event(true)
        viewModelScope.launch {
            val resource = posRepository.orderDetailsById(orderId)


            when (resource.status) {
                Status.SUCCESS -> {
                    Log.d("###17MAR23", "apiCallOrderDetails: Called - End")
                    _showProgress.value = Event(false)
                    resource.data.let { logInResponse ->
                        if (logInResponse?.status == 200) {

                            resource.data?.let { createTaxResponse ->

                                createTaxResponse.data.guestAttributes.forEachIndexed { index, guestAttributes ->

                                    guestAttributes.guestItemAttributes.forEach { singleGuestItem ->

                                        singleGuestItem.guest_index_for_dine_in = index

                                    }
                                }
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

    fun updateOrder(orderId: Int, orderRequestModel: OrderRequestModel, fromReorder: Boolean = false, message: String = "added") {
        _showProgress.value = Event(true)

        viewModelScope.launch {
            val resource = posRepository.updateOrder(orderId, orderRequestModel)

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    if (!fromReorder) {
                        _updateOrder.value = Event("Guest $message successfully.")
                    } else {
                        _reorderItemsSuccess.value = Event(resource.data?.data)
                    }

                }
                Status.ERROR -> {
                    _snackbarText.value = Event(resource.message)
                    _showProgress.value = Event(false)
                    prefProvider.setValueboolean(Constants.DINE_IN_UPDATE, false)
                }
                Status.LOADING -> {
                    _showProgress.value = Event(true)

                }

            }


        }

    }

    fun unableToRemoveGuest(message: String = "") {
        _removeGuestSuccess.value = Event(message)
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
                    LogUtil.logE("itemTaxPrice", "" + itemTaxPrice)
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
        Log.d("###17MAR23", "createQueuePrinter: Called - Start")
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
    fun randomOfflineId(): String {

        val locationId = prefProvider.getValueInt(Constants.LOCATION_ID, -1).toString()
        val timestamp = System.currentTimeMillis().toString()
        val ss = locationId + timestamp.takeLast(4)
        val reqLent = 12 - ss.length
        val Alphabet = getSaltString(reqLent)
        val timeStampFinal = Alphabet + ss
        Log.e("timeStampFinal", timeStampFinal)

        return timeStampFinal
    }
    protected open fun getSaltString(reqLent: Int): String? {
        val SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
        val salt = StringBuilder()
        val rnd = Random()
        while (salt.length < reqLent) { // length of the random string.
            val index = (rnd.nextFloat() * SALTCHARS.length).toInt()
            salt.append(SALTCHARS[index])
        }
        return salt.toString()
    }
    fun updateOrderRequest(cartModel: CartModel): OrderRequestModel {
        val orderModel: OrderAttributeRequestModel = OrderAttributeRequestModel()
        orderModel.apply {
            date = TimeFormatUtils.getCurrentDate()
            employeeId = prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
            locationId = prefProvider.getValueInt(Constants.LOCATION_ID, 1)
            terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, 0)
            note = ""
            openOrderType = "DineIn"
            orderTypeId = prefProvider.getValueInt(ORDER_TYPE_ID, 2)
            orderTypeName = prefProvider.getValue(ORDER_TYPE_NAME, DINE_IN)
            paymentStatus = 0
            subTotal = 0.0
            totalAmount = 0.0
            totalDiscount = 0.0
            totalServiceCharges = 0.0
            totalTaxAmount = 0.0
            totalTips = 0.0
            cash_discount_or_surcharge = 0.0
            cash_discount_type = ""
            offlineId = randomOfflineId()
            dineInOrderDetailsAttr
        }

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


                            LogUtil.logE(
                                "INOUT : Total Amount",
                                order.payments[order.payments.size - 1].amount.toString()
                            )
                            LogUtil.logE("INOUT : Total PayAmount", totalPayAmounts.toString())

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

    fun wastageItemApiCall(wastageItemRequest: WastageItemRequest) {
        viewModelScope.launch {
            _showProgress.value = Event(true)
            val resource = posRepository.addItemToWastage(wastageItemRequest)

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { response ->
                        if (response?.status == 200) {
                            response.let {
                                _wastageItemsSuccess.value = Event(response.message)
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