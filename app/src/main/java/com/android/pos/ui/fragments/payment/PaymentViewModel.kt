package com.android.pos.ui.fragments.payment

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.db.AppDatabase
import com.android.pos.data.entities.*
import com.android.pos.data.model.requestModel.*
import com.android.pos.data.model.responseModel.BaseResponse
import com.android.pos.data.model.responseModel.CreateOrderResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.DINE_IN
import com.android.pos.data.remote.Constants.IS_PRINTER_QUEUE_ENABLE
import com.android.pos.data.remote.Constants.PAYMENT_ID
import com.android.pos.data.remote.Constants.TAKEOUT
import com.android.pos.data.repositories.PosRepository
import com.android.pos.di.PrefProvider
import com.android.pos.ui.fragments.magtek.PaymentResponse
import com.android.pos.utils.Event
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.TimeFormatUtils
import com.android.pos.utils.statusUtils.Resource
import com.android.pos.utils.statusUtils.Status
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
open class PaymentViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val appDatabase: AppDatabase,
    private val prefProvider: PrefProvider
) : ViewModel() {

    private var cardNumberLast4: String = ""
    private val TAG = "PaymentViewModel"
    var isUpdateOrder: Boolean = false
    private var onlySave: Boolean = false
    private var totalPayAmounts: Double = 0.0
    private var orderId: Int? = null
    private var paymentId: Int? = null
    private var paymentOfflineId: String? = null
    public var order_type_id = -1
    private var orderOfflineId: String? = null
    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private var _queuePrinter = MutableLiveData<Event<String>>()
    val queuePrinter: LiveData<Event<String>> = _queuePrinter

    private var _queueCreateSaveOrder = MutableLiveData<Event<Boolean?>>()
    val QueueCreateSaveOrder: LiveData<Event<Boolean?>> = _queueCreateSaveOrder

    private val _queueStartSaveOrder = MutableLiveData<Event<CreateOrderResponse?>>()
    val queueStartSaveOrder: LiveData<Event<CreateOrderResponse?>> = _queueStartSaveOrder

    private val _msgText = MutableLiveData<Event<String>>()
    val msgText: LiveData<Event<String>> = _msgText

    private val _queueStart = MutableLiveData<Event<CreateOrderResponse?>>()
    val QueueStart: LiveData<Event<CreateOrderResponse?>> = _queueStart

    private val _data = MutableLiveData<Event<CreateOrderResponse?>>()
    val data: LiveData<Event<CreateOrderResponse?>> = _data

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private val _data1 = MutableLiveData<Event<BaseResponse?>>()
    val data1: LiveData<Event<BaseResponse?>> = _data1

    private val _orderCreate = MutableLiveData<Event<Boolean>>()
    val orderCreate: LiveData<Event<Boolean>> = _orderCreate

    public var actual_Total: Double = 0.0
    public var actual_SubTotal: Double = 0.0
    public var actual_TotalTax: Double = 0.0
    public var actual_TotalServiceCharge: Double = 0.0
    public var actual_TotalTips: Double = 0.0
    public var actual_TotalDiscount: Double = 0.0
    public var actual_CashDiscountSurCharge: Double = 0.0
    public var actual_CardAmount: Double = 0.0

    private var magensaResponse: String? = null

    fun cardReaderList() = posRepository.cardReaderActiveList()

    fun setOrderTypeId(order_typeId: Int) {
        this.order_type_id = order_typeId
    }

    fun submit(orderRequestModel: OrderRequestModel) {

        _showProgress.value = Event(true)

        viewModelScope.launch {

            val resource: Resource<CreateOrderResponse> = if (isUpdateOrder) {
                posRepository.updateOrder(
                    orderId,
                    orderRequestModel
                ) as Resource<CreateOrderResponse>
            } else {

                posRepository.createOrder(orderRequestModel)
            }

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { response ->
                        if (response?.status == 200) {

                            resource.data?.let { createOrderResponse ->
                                if (createOrderResponse.data.order.customer != null) {
                                    posRepository.updateFinalRewards(
                                        createOrderResponse.data.order.customer.final_reward.toInt(),
                                        createOrderResponse.data.order.customer.id
                                    )
                                }

                                if (createOrderResponse.data.order.payments.isNotEmpty()) {
                                    prefProvider.setValueInt(
                                        PAYMENT_ID,
                                        createOrderResponse.data.order.payments[createOrderResponse.data.order.payments.size - 1].id
                                    )
                                }

                                if (onlySave || orderRequestModel.completed_all_payments) {
                                    posRepository.deleteCart(
                                        prefProvider.getValueInt(
                                            Constants.EMPLOYEE_ID,
                                            0
                                        )
                                    )
                                }

                                Log.e(TAG, "isOnlySave:  ${onlySave}")
                                Log.e(
                                    TAG,
                                    "IS_PRINTER_QUEUE_ENABLE  ${
                                        prefProvider.getValueboolean(
                                            IS_PRINTER_QUEUE_ENABLE,
                                            false
                                        )
                                    }"
                                )
                                if (prefProvider.getValueboolean(IS_PRINTER_QUEUE_ENABLE, false)) {
                                    Log.e(TAG, "QueueStart")
                                    _queueStartSaveOrder.value = Event(createOrderResponse)
                                }
                                if (onlySave) {
                                    Log.e("QueueCheck", "OnlySave")

                                    _queueStart.value = Event(createOrderResponse)

                                } else {
                                    if (createOrderResponse.data.order.orderType != "Dine In" && response.data.order.payments[response.data.order.payments.size - 1].paymentType != "Card") {
                                        cashLogApi(createOrderResponse, "in")
                                        Log.e("QueueCheck", "CashLogAPI")
                                    } else {
                                        _data.value = Event(createOrderResponse)
                                        Log.e("QueueCheck", "CreateOrderData")
                                    }

                                    if (createOrderResponse.data.order.orderType != "Dine In") {
                                        _queueStart.value = Event(createOrderResponse)
                                        Log.e("QueueCheck", "QueueStart")
                                    }
                                }

                                _msgText.value = Event(response.message)

                                _orderCreate.value = Event(true)

//
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

    fun saveActualValue(
        total: Double,
        subtotal: Double,
        totaltax: Double,
        serviceCharge: Double,
        totaltips: Double,
        totalDisc: Double,
        cashDiscountSur: Double,
        cardActualAmount: Double
    ) {
        actual_Total = MethodUtils.roundOffAmountDouble(total)
        actual_SubTotal = MethodUtils.roundOffAmountDouble(subtotal)
        actual_CashDiscountSurCharge = MethodUtils.roundOffAmountDouble(cashDiscountSur)
        actual_TotalDiscount = MethodUtils.roundOffAmountDouble(totalDisc)
        actual_TotalServiceCharge = MethodUtils.roundOffAmountDouble(serviceCharge)
        actual_TotalTax = MethodUtils.roundOffAmountDouble(totaltax)
        actual_TotalTips = MethodUtils.roundOffAmountDouble(totaltips)
        actual_CardAmount = MethodUtils.roundOffAmountDouble(cardActualAmount)
    }

    fun dineInWholePayment(orderRequestModel: OrderRequestModel, orderId: Int, splitValue: Int) {
        _showProgress.value = Event(true)

        viewModelScope.launch {

            val resource: Resource<CreateOrderResponse> =
                posRepository.updateOrder(
                    orderId,
                    orderRequestModel
                ) as Resource<CreateOrderResponse>


            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { response ->
                        if (response?.status == 200) {

                            if (splitValue != -1) {
                                posRepository.deleteCart(
                                    prefProvider.getValueInt(
                                        Constants.EMPLOYEE_ID,
                                        0
                                    )
                                )
                            }
                            resource.data?.let { createOrderResponse ->
                                if (createOrderResponse.data.order.payments.isNotEmpty()) {
                                    prefProvider.setValueInt(
                                        PAYMENT_ID,
                                        createOrderResponse.data.order.payments[createOrderResponse.data.order.payments.size - 1].id
                                    )
                                }

                                if (onlySave) {
                                    _data.value = Event(createOrderResponse)
                                }
                                cashLogApi(createOrderResponse, "in")


                                _msgText.value = Event(response.message)


//
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
            null
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
                                (order.payments[order.payments.size - 1].amount + order.payments[order.payments.size - 1].tips).toString()
                            )
                            Log.e("INOUT : Total PayAmount", totalPayAmounts.toString())


                            if (order.payments.isNotEmpty()) {
                                if (order.payments[order.payments.size - 1].amount + order.payments[order.payments.size - 1].tips == totalPayAmounts) {
                                    _data.value = Event(createOrderResponse)
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

    fun getCashDiscountDetails(active: Int): LiveData<CashDiscountModel>? {
        return posRepository.getCashDisDetail(active)
    }


    private suspend fun cashOutApi(createOrderResponse: CreateOrderResponse, event: String) {

        val order = createOrderResponse.data.order

        val amount =
            MethodUtils.roundOffAmountDouble(totalPayAmounts) - (order.payments[order.payments.size - 1].amount + order.payments[order.payments.size - 1].tips)

        if (amount > 0 && (amount != 0.01 || amount != 0.1)) {

            val cashLogRequest = CashLogRequest(
                amount,
                order.employeeId,
                event,
                order.id,
                order.payments[order.payments.size - 1].id,
                "Change returned after order's payment",
                order.terminalId,
                null,
                null
            )

            val resource = posRepository.cashInOut(cashLogRequest)

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { response ->
                        if (response?.status == 200) {

                            resource.data?.let {
                                _data.value = Event(createOrderResponse)
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
        } else {
            _data.value = Event(createOrderResponse)
        }
    }


    fun createOrderRequest(
        cartModel: CartModel,
        subTotalPrice: Double,
        totalPrice: Double,
        totalServiceCharge: Double,
        totalTax: Double,
        ORDER_TYPE: String,
        future_delivery_date: String,
        future_delivery_time: String,
        isPaid: Boolean,
        totalDiscount: Double,
        tipAmount: Double,
        splitValue: Int,
        redeemLoyaltyInfo: RedeemLoyaltyInfo?,
        finaldiscount: Double,
        needToAddPaymentAttributes: Boolean?,
        paymentType: String,
        cashdiscountType: String,
        tipID: Int? = null,
        isPrinterQueue: Boolean = false,
        offlineId: String = ""
    ): OrderRequestModel {

        val orderAttributeRequestModel = OrderAttributeRequestModel()


        if (isUpdateOrder)
            orderAttributeRequestModel.id = orderId



        orderAttributeRequestModel.openOrderType =
            prefProvider.getValue(Constants.ORDER_TYPE, TAKEOUT)

        orderAttributeRequestModel.orderTypeId = order_type_id
        orderAttributeRequestModel.date = TimeFormatUtils.getCurrentDate()
        if (future_delivery_date.isNotEmpty())
            orderAttributeRequestModel.futureDeliveryDate = future_delivery_date

        if (future_delivery_time.isNotEmpty())
            orderAttributeRequestModel.futureDeliveryTime = future_delivery_time

        if (cartModel.openOrderType.isNotEmpty() && cartModel.openOrderType != null) {
            orderAttributeRequestModel.deliveryType = cartModel.openOrderType
        } else {
            orderAttributeRequestModel.deliveryType = cartModel.deliveryType
        }
        orderAttributeRequestModel.employeeId = prefProvider.getValueInt(Constants.EMPLOYEE_ID, 0)
        orderAttributeRequestModel.locationId = prefProvider.getValueInt(Constants.LOCATION_ID, 1)
        orderAttributeRequestModel.terminalId = prefProvider.getValueInt(Constants.TERMINAL_ID, 0)
        orderAttributeRequestModel.note = cartModel.note
        if (paymentType == "Cash") {
            if (cashdiscountType == "SurCharge") {
                orderAttributeRequestModel.cash_discount_type = ""
                orderAttributeRequestModel.cash_discount_or_surcharge = 0.0
                orderAttributeRequestModel.totalAmount = actual_Total
            } else if (cashdiscountType == "CashDiscount") {
                orderAttributeRequestModel.cash_discount_or_surcharge = actual_CashDiscountSurCharge
                orderAttributeRequestModel.cash_discount_type = cashdiscountType

                orderAttributeRequestModel.totalAmount = actual_Total - actual_CashDiscountSurCharge
            } else {
                orderAttributeRequestModel.cash_discount_or_surcharge = 0.0
                orderAttributeRequestModel.cash_discount_type = ""
                orderAttributeRequestModel.totalAmount = actual_Total
            }
        } /*else if (paymentType == "Card") {
            if (cashdiscountType == "SurCharge") {
                orderAttributeRequestModel.cash_discount_or_surcharge = actual_CashDiscountSurCharge
                orderAttributeRequestModel.cash_discount_type = cashdiscountType
            } else if (cashdiscountType == "CashDiscount") {
                orderAttributeRequestModel.cash_discount_type = ""
                orderAttributeRequestModel.cash_discount_or_surcharge = 0.0
            }
        }*/
        orderAttributeRequestModel.offlineId =
            if (isUpdateOrder) orderOfflineId.toString() else MethodUtils.randomOfflineId(
                prefProvider.getValueInt(Constants.LOCATION_ID, -1).toString()
            )

        if (isPrinterQueue) {
            orderAttributeRequestModel.offlineId = offlineId
        }
        Log.e(TAG, "openOrderType: " + cartModel.orderType)
        orderAttributeRequestModel.paymentStatus = if (isPaid) 1 else 0
        orderAttributeRequestModel.serviceChargeEnabled = true
        orderAttributeRequestModel.taxEnabled = true
        orderAttributeRequestModel.subTotal = actual_SubTotal


        if (cartModel.discountId != null && cartModel.discountId != -1)
            orderAttributeRequestModel.discount_id = cartModel.discountId
        orderAttributeRequestModel.totalDiscount = actual_TotalDiscount
        orderAttributeRequestModel.totalServiceCharges = actual_TotalServiceCharge
        orderAttributeRequestModel.totalTaxAmount = actual_TotalTax
        orderAttributeRequestModel.totalTips = tipAmount
        orderAttributeRequestModel.is_loyalty_applied = redeemLoyaltyInfo?.needToApplyLoyalty
        if (orderAttributeRequestModel.is_loyalty_applied == true) {
            orderAttributeRequestModel.loyalty_program_id =
                "${redeemLoyaltyInfo?.loyaltyProgramsModel?.id}"
            orderAttributeRequestModel.loyalty_amount = redeemLoyaltyInfo?.usedLoyaltyAmount
            orderAttributeRequestModel.used_reward_points = redeemLoyaltyInfo?.usedLoyaltyPoints
        }

//        if (cartModel.customer != null)
//            orderAttributeRequestModel.customer_id = cartModel.customer?.id

        val customerId = prefProvider.getValueInt(Constants.CUSTOMER_ID, -1)
        if (customerId != -1) {
            orderAttributeRequestModel.customer_id = customerId
        }


        orderAttributeRequestModel.paymentAttributes = if (needToAddPaymentAttributes == true) {
            paymentAttributes(
                cartModel,
                totalPrice,
                subTotalPrice,
                totalServiceCharge,
                totalTax,
                totalDiscount,
                tipAmount,
                splitValue,
                finaldiscount,
                paymentType,
                orderAttributeRequestModel.cash_discount_type,
                redeemLoyaltyInfo = redeemLoyaltyInfo,
                tipID
            )
        } else {
            null
        }

        orderAttributeRequestModel.orderServiceChargesAttributes =
            orderServiceChargesAttributes(cartModel, subTotalPrice)
        if (cartModel.orderType == DINE_IN) {
            orderAttributeRequestModel.guestsAttributes = getGuestsAttributes(cartModel)
            orderAttributeRequestModel.orderItemsAttributes = dineInOrderItemAttributed(cartModel)
        } else {

            orderAttributeRequestModel.orderItemsAttributes = orderItemsAttributes(cartModel)
        }

//        if (cartModel.customer != null)
//            orderAttributeRequestModel.customerAttributes = customerAttributes(cartModel)


        val orderRequestModel = OrderRequestModel(isPaid, orderAttributeRequestModel)

        Log.e("orderRequestModel", ":  ${Gson().toJson(orderRequestModel)}")

        return orderRequestModel
    }


    fun createOpenOrderRequest(
        cartModel: CartModel,
        subTotalPrice: Double,
        totalPrice: Double,
        totalServiceCharge: Double,
        totalTax: Double,
        ORDER_TYPE: String,
        future_delivery_date: String,
        future_delivery_time: String,
        isPaid: Boolean,
        totalDiscount: Double,
        tipAmount: Double,
        splitValue: Int,
        redeemLoyaltyInfo: RedeemLoyaltyInfo?,
        finaldiscount: Double,
        needToAddPaymentAttributes: Boolean?,
        paymentType: String,
        cashdiscountType: String
    ): OrderRequestModel {

        val orderAttributeRequestModel = OrderAttributeRequestModel()


        if (isUpdateOrder)
            orderAttributeRequestModel.id = orderId

        orderAttributeRequestModel.date = TimeFormatUtils.getCurrentDate()
        if (future_delivery_date.isNotEmpty())
            orderAttributeRequestModel.futureDeliveryDate = future_delivery_date

        if (future_delivery_time.isNotEmpty())
            orderAttributeRequestModel.futureDeliveryTime = future_delivery_time


        if (cartModel.openOrderType.isNotEmpty() && cartModel.openOrderType != null) {
            orderAttributeRequestModel.deliveryType = cartModel.openOrderType
        } else {
            orderAttributeRequestModel.deliveryType = cartModel.deliveryType
        }
        orderAttributeRequestModel.employeeId = cartModel.employeeID
        orderAttributeRequestModel.locationId = cartModel.locationId
        orderAttributeRequestModel.terminalId = cartModel.terminalId
        orderAttributeRequestModel.note = cartModel.note
        orderAttributeRequestModel.offlineId =
            if (isUpdateOrder) orderOfflineId.toString() else MethodUtils.randomOfflineId(
                prefProvider.getValueInt(Constants.LOCATION_ID, -1).toString()
            )
        Log.e(TAG, "openOrderType: " + cartModel.orderType)
        orderAttributeRequestModel.openOrderType = cartModel.orderType
        orderAttributeRequestModel.orderTypeId = cartModel.orderTypeId
        orderAttributeRequestModel.paymentStatus = if (isPaid) 1 else 0
        orderAttributeRequestModel.serviceChargeEnabled = true
        orderAttributeRequestModel.taxEnabled = true
        orderAttributeRequestModel.subTotal = MethodUtils.roundOffAmountDouble(subTotalPrice)
        orderAttributeRequestModel.totalAmount =
            MethodUtils.roundOffAmountDouble(totalPrice) - MethodUtils.roundOffAmountDouble(
                tipAmount
            )
        if (cartModel.discountId != null && cartModel.discountId != -1)
            orderAttributeRequestModel.discount_id = cartModel.discountId
        orderAttributeRequestModel.totalDiscount = totalDiscount
        orderAttributeRequestModel.totalServiceCharges =
            MethodUtils.roundOffAmountDouble(totalServiceCharge)
        orderAttributeRequestModel.totalTaxAmount = MethodUtils.roundOffAmountDouble(totalTax)
        orderAttributeRequestModel.totalTips = MethodUtils.roundOffAmountDouble(tipAmount)

        orderAttributeRequestModel.is_loyalty_applied = redeemLoyaltyInfo?.needToApplyLoyalty
        if (orderAttributeRequestModel.is_loyalty_applied == true) {
            orderAttributeRequestModel.loyalty_program_id =
                "${redeemLoyaltyInfo?.loyaltyProgramsModel?.id}"
            orderAttributeRequestModel.loyalty_amount = redeemLoyaltyInfo?.usedLoyaltyAmount
            orderAttributeRequestModel.used_reward_points = redeemLoyaltyInfo?.usedLoyaltyPoints
        }
//        if (cartModel.customer != null)
//            orderAttributeRequestModel.customer_id = cartModel.customer?.id

        val customerId = prefProvider.getValueInt(Constants.CUSTOMER_ID, -1)
        if (customerId != -1) {
            orderAttributeRequestModel.customer_id = customerId
        }


        orderAttributeRequestModel.paymentAttributes = if (needToAddPaymentAttributes == true) {
            paymentAttributes(
                cartModel,
                totalPrice,
                subTotalPrice,
                totalServiceCharge,
                totalTax,
                totalDiscount,
                tipAmount,
                splitValue,
                finaldiscount,
                paymentType,
                orderAttributeRequestModel.cash_discount_type,
                redeemLoyaltyInfo = redeemLoyaltyInfo
            )
        } else {
            null
        }
        orderAttributeRequestModel.orderServiceChargesAttributes =
            orderServiceChargesAttributes(cartModel, subTotalPrice)
        if (cartModel.orderType == DINE_IN) {
            orderAttributeRequestModel.guestsAttributes = getGuestsAttributes(cartModel)

            orderAttributeRequestModel.orderItemsAttributes = dineInOrderItemAttributed(cartModel)

        } else {

            orderAttributeRequestModel.orderItemsAttributes = orderItemsAttributes(cartModel)
        }

//        if (cartModel.customer != null)
//            orderAttributeRequestModel.customerAttributes = customerAttributes(cartModel)


        val orderRequestModel = OrderRequestModel(isPaid, orderAttributeRequestModel)


        return orderRequestModel
    }

    fun createOrderRequestForCard(
        cartModel: CartModel,
        subTotalPrice: Double,
        totalPrice: Double,
        totalServiceCharge: Double,
        totalTax: Double,
        ORDER_TYPE: String,
        future_delivery_date: String,
        future_delivery_time: String,
        isPaid: Boolean,
        totalDiscount: Double,
        tipAmount: Double,
        splitValue: Int,
        redeemLoyaltyInfo: RedeemLoyaltyInfo?,
        finaldiscount: Double,
        needToAddPaymentAttributes: Boolean?,
        paymentType: String,
        cashdiscountType: String,
        tipID: Int? = null
    ): OrderRequestModel {

        val orderAttributeRequestModel = OrderAttributeRequestModel()


        if (isUpdateOrder)
            orderAttributeRequestModel.id = orderId



        orderAttributeRequestModel.openOrderType =
            prefProvider.getValue(Constants.ORDER_TYPE, TAKEOUT)

        orderAttributeRequestModel.orderTypeId = order_type_id
        orderAttributeRequestModel.date = TimeFormatUtils.getCurrentDate()
        if (future_delivery_date.isNotEmpty())
            orderAttributeRequestModel.futureDeliveryDate = future_delivery_date

        if (future_delivery_time.isNotEmpty())
            orderAttributeRequestModel.futureDeliveryTime = future_delivery_time

        if (cartModel.openOrderType.isNotEmpty() && cartModel.openOrderType != null) {
            orderAttributeRequestModel.deliveryType = cartModel.openOrderType
        } else {
            orderAttributeRequestModel.deliveryType = cartModel.deliveryType
        }
        orderAttributeRequestModel.employeeId = cartModel.employeeID
        orderAttributeRequestModel.locationId = cartModel.locationId
        orderAttributeRequestModel.terminalId = cartModel.terminalId
        orderAttributeRequestModel.note = cartModel.note
        if (paymentType == "Card") {
            if (cashdiscountType == "SurCharge") {
                orderAttributeRequestModel.cash_discount_or_surcharge = actual_CashDiscountSurCharge
                orderAttributeRequestModel.cash_discount_type = cashdiscountType
                orderAttributeRequestModel.totalAmount =
                    actual_CardAmount + actual_CashDiscountSurCharge
            } else if (cashdiscountType == "CashDiscount") {
                orderAttributeRequestModel.cash_discount_type = ""
                orderAttributeRequestModel.cash_discount_or_surcharge = 0.0
                orderAttributeRequestModel.totalAmount = actual_CardAmount
            } else {
                orderAttributeRequestModel.cash_discount_or_surcharge = 0.0
                orderAttributeRequestModel.cash_discount_type = ""
                orderAttributeRequestModel.totalAmount = actual_CardAmount
            }
        }

        orderAttributeRequestModel.magensaResponse = magensaResponse.toString()

        orderAttributeRequestModel.offlineId =
            if (isUpdateOrder) orderOfflineId.toString() else MethodUtils.randomOfflineId(
                prefProvider.getValueInt(Constants.LOCATION_ID, -1).toString()
            )

        orderAttributeRequestModel.paymentStatus = if (isPaid) 1 else 0
        orderAttributeRequestModel.serviceChargeEnabled = true
        orderAttributeRequestModel.taxEnabled = true
        orderAttributeRequestModel.subTotal = actual_SubTotal

        if (cartModel.discountId != null && cartModel.discountId != -1)
            orderAttributeRequestModel.discount_id = cartModel.discountId
        orderAttributeRequestModel.totalDiscount = actual_TotalDiscount
        orderAttributeRequestModel.totalServiceCharges = actual_TotalServiceCharge
        orderAttributeRequestModel.totalTaxAmount = actual_TotalTax
        orderAttributeRequestModel.totalTips = tipAmount

        orderAttributeRequestModel.is_loyalty_applied = redeemLoyaltyInfo?.needToApplyLoyalty
        if (orderAttributeRequestModel.is_loyalty_applied == true) {
            orderAttributeRequestModel.loyalty_program_id =
                "${redeemLoyaltyInfo?.loyaltyProgramsModel?.id}"
            orderAttributeRequestModel.loyalty_amount = redeemLoyaltyInfo?.usedLoyaltyAmount
            orderAttributeRequestModel.used_reward_points = redeemLoyaltyInfo?.usedLoyaltyPoints
        }

//        if (cartModel.customer != null)
//            orderAttributeRequestModel.customer_id = cartModel.customer?.id

        val customerId = prefProvider.getValueInt(Constants.CUSTOMER_ID, -1)
        if (customerId != -1) {
            orderAttributeRequestModel.customer_id = customerId
        }


        orderAttributeRequestModel.paymentAttributes = if (needToAddPaymentAttributes == true) {
            paymentAttributesForCard(
                cartModel,
                totalPrice,
                subTotalPrice,
                totalServiceCharge,
                totalTax,
                totalDiscount,
                tipAmount,
                splitValue,
                finaldiscount,
                paymentType,
                orderAttributeRequestModel.cash_discount_type,
                redeemLoyaltyInfo = redeemLoyaltyInfo
            )
        } else {
            null
        }

        orderAttributeRequestModel.orderServiceChargesAttributes =
            orderServiceChargesAttributes(cartModel, subTotalPrice)
        if (cartModel.orderType == DINE_IN) {
            orderAttributeRequestModel.guestsAttributes = getGuestsAttributes(cartModel)
            Log.e(
                TAG,
                "guestsAttributesData:  ${Gson().toJson(orderAttributeRequestModel.guestsAttributes)}"
            )

            orderAttributeRequestModel.orderItemsAttributes = dineInOrderItemAttributed(cartModel)
            Log.e(
                TAG,
                "dineInOrderItemData:  ${Gson().toJson(orderAttributeRequestModel.orderItemsAttributes)}"
            )
        } else {

            orderAttributeRequestModel.orderItemsAttributes = orderItemsAttributes(cartModel)
        }

//        if (cartModel.customer != null)
//            orderAttributeRequestModel.customerAttributes = customerAttributes(cartModel)


        val orderRequestModel = OrderRequestModel(isPaid, orderAttributeRequestModel)

        Log.e("orderRequestModel", ":  ${Gson().toJson(orderRequestModel)}")

        return orderRequestModel
    }

    private fun getGuestsAttributes(cartModel: CartModel): List<GuestsAttributes> {
        val orderItemsAttributeList: ArrayList<GuestsAttributes> = arrayListOf()
        cartModel.dineInList?.forEach { it ->
            val model = GuestsAttributes()
            model.name = it.title.toString()
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
                            orderItemId = tb.orderItemId,
                            quantity = tb.itemQuantity,
                            itemId = tb.itemId,
                            amount = tb.price
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


                model.guestItemsAttributes = listItems
            }
            if (it.customer != null) {
                model.customerId = it.customer?.id
                var addressList: ArrayList<CustomerAttributes.AddressesAttribute> = arrayListOf()
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
                    phoneModel.phoneNumber = it.customer?.phones?.get(i)?.phone_number.toString()
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

            }
            orderItemsAttributeList.add(model)


        }

        return orderItemsAttributeList

    }

    private fun customerAttributes(cartModel: CartModel): CustomerAttributes {

        val customerAttributes = CustomerAttributes().apply {
            // id = cartModel.customer?.id ?: 0
            birthDate = cartModel.customer?.birth_date.toString()
            firstName = cartModel.customer?.first_name.toString()
            lastName = cartModel.customer?.last_name.toString()
            companyName = cartModel.customer?.company.toString()
            locationId = cartModel.locationId
            phonesAttributes = phonesAttributes(cartModel.customer?.id, cartModel.customer?.phones)
            emailsAttributes = emailsAttributes(cartModel.customer?.id, cartModel.customer?.email)
            addressesAttributes = addressesAttributes(cartModel.customer?.id, cartModel.customer)
        }

        return customerAttributes

    }

    private fun addressesAttributes(
        id: Int?,
        customer: TbCustomer?
    ): List<CustomerAttributes.AddressesAttribute> {

        val addressesAttributeList: ArrayList<CustomerAttributes.AddressesAttribute> =
            arrayListOf()
        customer?.addresses?.forEach {

            val addressesAttribute = CustomerAttributes.AddressesAttribute().apply {
                address1 = it.address1
                address2 = it.address2
                address3 = ""
                addressableId = it.id
                //  addressableType = it.address_type
                city = it.city
                country = it.country
                destroy = false
                latitude = 0.0
                longitude = 0.0
                postcode = it.postcode
                state = it.state
                typeOfAddress = it.type_of_address.toString()
            }
            addressesAttributeList.add(addressesAttribute)
        }

        return addressesAttributeList
    }

    private fun emailsAttributes(
        custId: Int?,
        emailId: String?
    ): List<CustomerAttributes.EmailsAttribute> {

        val phonesAttributeList: ArrayList<CustomerAttributes.EmailsAttribute> =
            arrayListOf()
        val email = CustomerAttributes.EmailsAttribute().apply {
            destroy = false
            // customerId = custId!!
            emailAddress = emailId.toString()
        }
        phonesAttributeList.add(email)
        return phonesAttributeList

    }

    private fun phonesAttributes(
        custId: Int?,
        phones: List<TbPhones>?
    ): List<CustomerAttributes.PhonesAttribute> {

        val phonesAttributeList: ArrayList<CustomerAttributes.PhonesAttribute> =
            arrayListOf()
        phones?.forEach {

            val phone = CustomerAttributes.PhonesAttribute().apply {
                destroy = false
                //   customerId = custId!!
                phoneNumber = it.phone_number
            }
            phonesAttributeList.add(phone)
        }

        return phonesAttributeList
    }

    private fun dineInOrderItemAttributed(cartModel: CartModel): List<OrderItemsAttribute> {
        val orderItemsAttributeList: ArrayList<OrderItemsAttribute> =
            arrayListOf()

        for (i in 0 until cartModel.dineInList?.size!!) {
            cartModel.dineInList?.get(i)?.items?.forEach { item ->

                Log.e(TAG, "getItemDinefas  ${Gson().toJson(item)}")
                val orderItemsAttribute = OrderItemsAttribute()

                if (isUpdateOrder && item.orderItemId != null)
                    orderItemsAttribute.id = item.orderItemId


                orderItemsAttribute.category_id = item.categoryId

                orderItemsAttribute.discountAmount = (item.discountPrice * item.itemQuantity)
                orderItemsAttribute.discountType = item.discountType
                if (item.discountId != -1)
                    orderItemsAttribute.discountId = item.discountId
                orderItemsAttribute.employeeId = cartModel.employeeID
                orderItemsAttribute.isCount = 0
                orderItemsAttribute.isEdited = item.isEdited
                orderItemsAttribute.isPaid = false
                orderItemsAttribute.isPrinted = true
                orderItemsAttribute.isTaxRemoved = false
                orderItemsAttribute.itemId = if (item.isManualSales) 30 else item.itemId
                orderItemsAttribute.is_manual_sales = item.isManualSales
                orderItemsAttribute.itemName = item.name
                orderItemsAttribute.note = item.note
                orderItemsAttribute.price = item.price
                orderItemsAttribute.quantity = item.itemQuantity
                orderItemsAttribute.terminalId = cartModel.terminalId
                orderItemsAttribute.isFired = item.isFired
                orderItemsAttribute.timestamp = System.currentTimeMillis().toString()
                orderItemsAttribute.totalPrice =
                    MethodUtils.roundOffAmountDouble(item.price * item.itemQuantity)
                orderItemsAttribute.orderItemTaxesAttributes =
                    orderItemTaxesAttributesForDineIn(item)
                orderItemsAttribute.orderItemModifiersAttributes =
                    orderItemModifierAttributesDineIn(item, cartModel.terminalId)

                orderItemsAttribute.orderItemVariationAttributes =
                    orderItemVariationAttributes(item)

                if (item.variationsAttributes.isNotEmpty()) {
                    orderItemsAttribute.variationId = item.variationsAttributes[0].id
                }

                orderItemsAttributeList.add(orderItemsAttribute)
            }
        }
        return orderItemsAttributeList


    }

    private fun orderItemsAttributes(cartModel: CartModel): List<OrderItemsAttribute> {

        val orderItemsAttributeList: ArrayList<OrderItemsAttribute> =
            arrayListOf()
        Log.e(TAG, "insideSize  ${cartModel.items?.size}")

        cartModel.items?.forEach { item ->

            val orderItemsAttribute = OrderItemsAttribute()

            if (isUpdateOrder && item.orderItemId != null)
                orderItemsAttribute.id = item.orderItemId


            orderItemsAttribute.category_id = item.categoryId


            if (cartModel.reorder) {
                orderItemsAttribute.discountAmount = (item.discountPrice)
            } else {
                orderItemsAttribute.discountAmount = (item.discountPrice * item.itemQuantity)
            }


            orderItemsAttribute.discountType = item.discountType
            if (item.discountId != -1)
                orderItemsAttribute.discountId = item.discountId
            orderItemsAttribute.employeeId = cartModel.employeeID
            orderItemsAttribute.isCount = 0
            orderItemsAttribute.isEdited = item.isEdited
            orderItemsAttribute.isDestroy = item.isDestroy
            orderItemsAttribute.isPaid = false
            orderItemsAttribute.isPrinted = true
            orderItemsAttribute.isTaxRemoved = false
            orderItemsAttribute.itemId = item.itemId
            orderItemsAttribute.is_manual_sales = item.isManualSales
            orderItemsAttribute.itemName = item.name
            orderItemsAttribute.note = item.note
            orderItemsAttribute.price = item.price
            orderItemsAttribute.quantity = item.itemQuantity
            orderItemsAttribute.terminalId = cartModel.terminalId
            orderItemsAttribute.timestamp = MethodUtils.randomOfflineId(
                prefProvider.getValueInt(Constants.LOCATION_ID, -1).toString()
            )
            orderItemsAttribute.totalPrice =
                MethodUtils.roundOffAmountDouble(item.price * item.itemQuantity)
            orderItemsAttribute.orderItemTaxesAttributes = orderItemTaxesAttributes(item)
            orderItemsAttribute.orderItemModifiersAttributes =
                orderItemModifierAttributes(item, cartModel.terminalId)
            orderItemsAttribute.isFired = item.isFired

            orderItemsAttribute.orderItemVariationAttributes =
                orderItemVariationAttributes(item)

            if (item.variationsAttributes.isNotEmpty()) {
                orderItemsAttribute.variationId = item.variationsAttributes[0].id
            }

            orderItemsAttributeList.add(orderItemsAttribute)
        }
        Log.e(TAG, "orderItemsAttributeList:  ${Gson().toJson(orderItemsAttributeList)}")
        return orderItemsAttributeList
    }

    private fun orderItemVariationAttributes(
        item: TbItem
    ): OrderItemVariationAttribute? {

        if (item.variationsAttributes.isNotEmpty()) {

            item.variationsAttributes.forEach {

                val orderItemVariationAttribute = OrderItemVariationAttribute()
                orderItemVariationAttribute.name = it.name
                orderItemVariationAttribute.price = it.price ?: 0.0
                orderItemVariationAttribute.totalPrice = it.price ?: 0.0 * item.itemQuantity
                orderItemVariationAttribute.variationId = it.id!!
                orderItemVariationAttribute.quantity = item.itemQuantity

                if (isUpdateOrder) {
                    orderItemVariationAttribute.orderId = orderId
                    orderItemVariationAttribute.order_item_id = item.orderItemId
                    orderItemVariationAttribute.id = it.orderVariationId
                }

                return orderItemVariationAttribute

            }

        }

        return null
    }

    private fun orderItemModifierAttributes(
        item: TbItem,
        terminalId: Int
    ): List<OrderItemModifierAttribute> {

        val orderItemModifierAttributeList: ArrayList<OrderItemModifierAttribute> =
            arrayListOf()

        Log.e(TAG, "getmodifiers:  ${Gson().toJson(item.modifiers)}")
        item.modifiers.forEach {

            val orderItemModifierAttribute = OrderItemModifierAttribute().apply {

                if (isUpdateOrder && it.orderModifierId != null)
                    id = it.orderModifierId

                name = it.name
                price = it.price
                modifier_id = it.id
                order_item_id = item.orderItemId
                totalPrice = MethodUtils.roundOffAmountDouble(it.price * it.itemQuantity)
                modifier_set_id = it.modifierSetId ?: 0
                quantity = it.itemQuantity
                order_item_taxes_attributes = arrayListOf()
            }
            orderItemModifierAttributeList.add(orderItemModifierAttribute)
        }

        return orderItemModifierAttributeList
    }

    private fun orderItemModifierAttributesDineIn(
        item: TbItem,
        terminalId: Int
    ): List<OrderItemModifierAttribute> {

        val orderItemModifierAttributeList: ArrayList<OrderItemModifierAttribute> =
            arrayListOf()

        item.modifiers.forEach {

            val orderItemModifierAttribute = OrderItemModifierAttribute().apply {

                if (isUpdateOrder && it.id != null)
                    id = it.id

                name = it.name
                price = it.price
                order_item_id = item.orderItemId
                totalPrice = MethodUtils.roundOffAmountDouble(it.price * it.itemQuantity)
                modifier_set_id = it.modifierSetId ?: 0
                quantity = it.itemQuantity
                order_item_taxes_attributes = arrayListOf()
            }
            orderItemModifierAttributeList.add(orderItemModifierAttribute)
        }

        return orderItemModifierAttributeList
    }

    private fun orderModifierTaxesAttributes(
        items: TbItem,
        modifier: Modifier,
        terminalId: Int
    ): List<OrderModifierTaxesAttribute> {

        val orderItemTaxesAttributeList: ArrayList<OrderModifierTaxesAttribute> =
            arrayListOf()

//        items.taxes?.forEach { tax ->
//            if (tax.isActive) {
//                val orderModifierTaxesAttribute = OrderModifierTaxesAttribute()
//                if (isUpdateOrder && tax.orderTaxId != null)
//                    orderModifierTaxesAttribute.id = tax.orderTaxId
//
//
//                orderModifierTaxesAttribute.order_item_modifier_id = modifier.id
//                orderModifierTaxesAttribute.tax_id = tax.id
//                orderModifierTaxesAttribute.isDefault = tax.isDefault
//                orderModifierTaxesAttribute.is_tax_removed = false
//                orderModifierTaxesAttribute.is_modifier = true
//                orderModifierTaxesAttribute.category_id = items.categoryId
//                orderModifierTaxesAttribute.terminal_id = terminalId
//                orderModifierTaxesAttribute.modifier_id = modifier.id!!
//                orderModifierTaxesAttribute.timestamp = System.currentTimeMillis().toString()
//                orderModifierTaxesAttribute.name = tax.name.toString()
//                orderModifierTaxesAttribute.amount = tax.rate
//
//                if (isUpdateOrder) {
//                    orderModifierTaxesAttribute.order_id = orderId
//                    orderModifierTaxesAttribute.order_item_id = items.orderItemId
//                }
//
//                if (tax.taxType == "Percentage") {
//                    val itemTaxPrice =
//                        (tax.rate * (modifier.price * modifier.itemQuantity)) / 100
//                    orderModifierTaxesAttribute.taxTotalAmount =
//                        MethodUtils.roundOffAmountDouble(itemTaxPrice)
//                } else {
//
//                    val ss = tax.rate * modifier.itemQuantity
//
//                    orderModifierTaxesAttribute.taxTotalAmount =
//                        MethodUtils.roundOffAmountDouble((ss))
//                }
//
//
//                orderItemTaxesAttributeList.add(orderModifierTaxesAttribute)
//            }
//        }


        return orderItemTaxesAttributeList

    }

    private fun orderItemTaxesAttributes(items: TbItem): List<OrderItemTaxesAttribute> {

        val orderItemTaxesAttributeList: ArrayList<OrderItemTaxesAttribute> =
            arrayListOf()

        items.taxes?.forEach { tax ->

            if (tax.isActive) {

                val orderItemTaxesAttribute = OrderItemTaxesAttribute()

                if (isUpdateOrder && tax.orderTaxId != null)
                    orderItemTaxesAttribute.id = tax.orderTaxId

                orderItemTaxesAttribute.isDefault = tax.isDefault
                orderItemTaxesAttribute.isTaxRemoved = true
                orderItemTaxesAttribute.name = tax.name.toString()
                orderItemTaxesAttribute.rate = tax.rate
                orderItemTaxesAttribute.taxId = tax.id

                orderItemTaxesAttribute.orderItemId = items.orderItemId
                orderItemTaxesAttribute.orderId = orderId
                if (isUpdateOrder) {
                }

                if (tax.taxType == "Percentage") {


                    var modifierPrice = 0.0

                    var price = 0.0
                    price =
                        (items.price * items.itemQuantity) - (items.discountPrice * items.itemQuantity)



                    items.modifiers.forEach {
                        modifierPrice += (it.price * it.itemQuantity)
                    }

                    val totalPrice = price + modifierPrice

                    val itemTaxPrice =
                        (tax.rate * totalPrice) / 100

                    Log.e("Tax Amount 1", itemTaxPrice.toString())

                    orderItemTaxesAttribute.taxTotalAmount =
                        MethodUtils.roundOffAmountDouble(itemTaxPrice)
                } else {

                    val ss = tax.rate * items.itemQuantity

                    Log.e("Tax Amount", ss.toString())

                    orderItemTaxesAttribute.taxTotalAmount =
                        MethodUtils.roundOffAmountDouble((ss))
                }




                orderItemTaxesAttribute.taxType = tax.taxType.toString()
                orderItemTaxesAttributeList.add(orderItemTaxesAttribute)
            }
        }


        return orderItemTaxesAttributeList
    }

    private fun orderItemTaxesAttributesForDineIn(items: TbItem): List<OrderItemTaxesAttribute> {
        Log.e(TAG, "getDineitems:  ${Gson().toJson(items)}")

        val orderItemTaxesAttributeList: ArrayList<OrderItemTaxesAttribute> =
            arrayListOf()

        items.taxes?.forEach { tax ->

            if (tax.isActive) {

                val orderItemTaxesAttribute = OrderItemTaxesAttribute()

                if (isUpdateOrder && tax.id != null)
                    orderItemTaxesAttribute.id = tax.id

                orderItemTaxesAttribute.isDefault = tax.isDefault
                orderItemTaxesAttribute.isTaxRemoved = true
                orderItemTaxesAttribute.name = tax.name.toString()
                orderItemTaxesAttribute.rate = tax.rate

                if (tax.orderTaxId != null) {
                    tax.orderTaxId?.let { orderItemTaxesAttribute.taxId = it }
                }

                orderItemTaxesAttribute.orderItemId = items.orderItemId
                orderItemTaxesAttribute.orderId = orderId
                if (isUpdateOrder) {
                }

                if (tax.taxType == "Percentage") {


                    var modifierPrice = 0.0

                    val price =
                        (items.price * items.itemQuantity) - items.discountPrice

                    items.modifiers.forEach {
                        modifierPrice += (it.price * it.itemQuantity)
                    }

                    val totalPrice = price + modifierPrice

                    val itemTaxPrice =
                        (tax.rate * totalPrice) / 100

                    orderItemTaxesAttribute.taxTotalAmount =
                        MethodUtils.roundOffAmountDouble(itemTaxPrice)
                } else {

                    val ss = tax.rate * items.itemQuantity

                    orderItemTaxesAttribute.taxTotalAmount =
                        MethodUtils.roundOffAmountDouble((ss))
                }




                orderItemTaxesAttribute.taxType = tax.taxType.toString()
                orderItemTaxesAttributeList.add(orderItemTaxesAttribute)
            }
        }


        return orderItemTaxesAttributeList
    }


    private fun orderServiceChargesAttributes(
        cartModel: CartModel,
        subTotalPrice: Double
    ): List<OrderServiceChargesAttribute> {

        val orderServiceChargesAttributeList: ArrayList<OrderServiceChargesAttribute> =
            arrayListOf()
        val serviceChargesList = cartModel.serviceCharge
        if (serviceChargesList != null && serviceChargesList.isNotEmpty()) {
            if (prefProvider.getValueboolean(Constants.SERVICECHARGE_TAKEOUT_OPENORDER, false)) {
                serviceChargesList.forEach {
                    val orderServiceChargesAttribute = OrderServiceChargesAttribute()
                    orderServiceChargesAttribute.amount =
                        MethodUtils.roundOffAmountDouble((subTotalPrice * it.percentage) / 100)
                    orderServiceChargesAttribute.name = it.name
                    orderServiceChargesAttribute.rate = it.percentage
                    orderServiceChargesAttribute.serviceChargeId = it.id
                    orderServiceChargesAttribute.order_type = it.order_type
                    if (isUpdateOrder && it.order_service_charge_id != null)
                        orderServiceChargesAttribute.id = it.order_service_charge_id
                    orderServiceChargesAttributeList.add(orderServiceChargesAttribute)
                }
            } else if (prefProvider.getValueboolean(Constants.SERVICECHARGE_DINEIN_ORDER, false)) {
                serviceChargesList.forEach {
                    val orderServiceChargesAttribute = OrderServiceChargesAttribute()
                    orderServiceChargesAttribute.amount =
                        MethodUtils.roundOffAmountDouble((subTotalPrice * it.percentage) / 100)
                    orderServiceChargesAttribute.name = it.name
                    orderServiceChargesAttribute.rate = it.percentage
                    orderServiceChargesAttribute.serviceChargeId = it.id
                    orderServiceChargesAttribute.order_type = it.order_type
                    orderServiceChargesAttribute.max_guest_count = it.max_guest_count
                    orderServiceChargesAttribute.min_guest_count = it.min_guest_count
                    if (isUpdateOrder && it.order_service_charge_id != null)
                        orderServiceChargesAttribute.id = it.order_service_charge_id
                    orderServiceChargesAttributeList.add(orderServiceChargesAttribute)
                }
            }

        }
        return orderServiceChargesAttributeList
    }

    private fun paymentAttributes(
        cartModel: CartModel,
        totalPrice: Double,
        subTotalPrice: Double,
        totalServiceCharge: Double,
        totalTax: Double,
        totalDis: Double,
        tipAmount: Double,
        splitValue: Int,
        finalcashdiscount: Double,
        paymentTypeStatus: String,
        cashdiscountType: String,
        redeemLoyaltyInfo: RedeemLoyaltyInfo?,
        tipID: Int? = null
    ): PaymentAttributes {
        return PaymentAttributes().apply {
//            if (isUpdateOrder)
//                id = paymentId
            val totalPP = MethodUtils.roundOffAmountDouble(totalPrice)
            val totalDC = MethodUtils.roundOffAmountDouble(tipAmount)
            val totalAM = totalPP /*- totalDC*/
            amount = totalAM
            if (paymentTypeStatus == "Cash") {
                if (cashdiscountType == "SurCharge") {
                    cash_discount_or_surcharge = 0.0
                    total_cash_discount = 0.0
                    cash_discount_type = ""
                } else if (cashdiscountType == "CashDiscount") {
                    cash_discount_or_surcharge = finalcashdiscount
                    total_cash_discount = finalcashdiscount
                    cash_discount_type = cashdiscountType
                }
            } else if (paymentTypeStatus == "Card") {
                if (cashdiscountType == "SurCharge") {
                    cash_discount_or_surcharge = finalcashdiscount
                    total_cash_discount = finalcashdiscount
                    cash_discount_type = cashdiscountType
                } else if (cashdiscountType == "CashDiscount") {
                    cash_discount_or_surcharge = 0.0
                    total_cash_discount = 0.0
                    cash_discount_type = ""
                }
            }
            cashDiscountFee = 0.0

            employeeId = cartModel.employeeID
            offlineId =
                if (isUpdateOrder) paymentOfflineId.toString() else MethodUtils.randomOfflineId(
                    prefProvider.getValueInt(Constants.LOCATION_ID, -1).toString()
                )
            payableType = "Order"
            paymentType = paymentTypeStatus
            serviceChargeAmount = MethodUtils.roundOffAmountDouble(totalServiceCharge)
            subTotal = MethodUtils.roundOffAmountDouble(subTotalPrice)
            taxAmount = MethodUtils.roundOffAmountDouble(totalTax)
            terminalId = cartModel.terminalId
            tips = MethodUtils.roundOffAmountDouble(tipAmount)
            tipsAdjusted = false
            totalDiscount = MethodUtils.roundOffAmountDouble(totalDis)
            tipID?.let { tipId = it }



            if (isUpdateOrder && orderId != null) {
                order_id = orderId
            }
            //           transactionId = ""
            is_loyalty_applied = redeemLoyaltyInfo?.needToApplyLoyalty
            if (is_loyalty_applied == true) {
                loyalty_program_id = "${redeemLoyaltyInfo?.loyaltyProgramsModel?.id}"
                loyalty_amount =
                    if (splitValue == -1) redeemLoyaltyInfo?.usedLoyaltyAmount else redeemLoyaltyInfo?.usedLoyaltyAmount?.div(
                        splitValue
                    )
                used_reward_points =
                    if (splitValue == -1) redeemLoyaltyInfo?.usedLoyaltyPoints else redeemLoyaltyInfo?.usedLoyaltyPoints?.div(
                        splitValue
                    )
                is_loyalty_applied = redeemLoyaltyInfo?.needToApplyLoyalty
            }

        }
    }

    private fun paymentAttributesForCard(
        cartModel: CartModel,
        totalPrice: Double,
        subTotalPrice: Double,
        totalServiceCharge: Double,
        totalTax: Double,
        totalDis: Double,
        tipAmount: Double,
        splitValue: Int,
        finalcashdiscount: Double,
        paymentTypeStatus: String,
        cashdiscountType: String,
        redeemLoyaltyInfo: RedeemLoyaltyInfo?
    ): PaymentAttributes {
        return PaymentAttributes().apply {
//            if (isUpdateOrder)
//                id = paymentId
            val totalPP = MethodUtils.roundOffAmountDouble(totalPrice)
            val totalDC = MethodUtils.roundOffAmountDouble(tipAmount)
            val totalAM = totalPP /*- totalDC*/
            amount = totalAM
            if (magensaResponse != null) {
                val model = Gson().fromJson(
                    magensaResponse,
                    PaymentResponse.PaymentResponseItem::class.java
                )
                Log.e("magensaResponse", Gson().toJson(model))


                if (model.dataOutput != null) {
                    Log.e("dataOutput", Gson().toJson(model))
                    cardNumber = model.dataOutput.PANLast4
                    var cardN = ""
                    model.dataOutput.additionalOutputData?.forEach {
                        Log.e("additionalOutputData", it.key)
                        if (it.key == "CardType") {
                            cardN = it.value
                        }
                    }
                    cardName = cardN


                }

                if (model.cardSwipeOutput != null) {
                    Log.e("cardSwipeOutput", Gson().toJson(model))
                    cardNumber = model.cardSwipeOutput.pANLast4
                    var cardN = ""
                    model.cardSwipeOutput.additionalOutputData?.forEach {
                        if (it.key == "CardType") {
                            cardN = it.value
                        }
                    }

                    cardName = cardN
                }


                if (model.transactionOutput?.transactionOutputDetails?.isNotEmpty() == true) {
                    var CardType = ""
                    model.transactionOutput.transactionOutputDetails.forEach {
                        if (it.key == "CardType") {
                            CardType = it.value
                        }
                    }

                    cardName = CardType
                    cardNumber =
                        if (cardNumberLast4.isNotEmpty()) cardNumberLast4.takeLast(4) else ""
                }


                transactionId = model.transactionOutput?.transactionID.toString()
                cardType = 0
            }
//            cardName = ""
//            cardNumber = ""
//            cardType = 0
            if (cashdiscountType.isNotEmpty()) {
                cash_discount_or_surcharge = finalcashdiscount
                total_cash_discount = finalcashdiscount
            } else {
                cash_discount_or_surcharge = 0.0
                total_cash_discount = 0.0
            }


            magensa_response = magensaResponse.toString()
            cashDiscountFee = 0.0
            cash_discount_type = cashdiscountType
            employeeId = cartModel.employeeID
            offlineId =
                if (isUpdateOrder) paymentOfflineId.toString() else MethodUtils.randomOfflineId(
                    prefProvider.getValueInt(Constants.LOCATION_ID, -1).toString()
                )
            payableType = "Order"
            paymentType = paymentTypeStatus
            serviceChargeAmount = totalServiceCharge
            subTotal = subTotalPrice
            taxAmount = totalTax
            terminalId = cartModel.terminalId
            tips = MethodUtils.roundOffAmountDouble(tipAmount)

            tipsAdjusted = false
            totalDiscount = totalDis


            if (isUpdateOrder && orderId != null) {
                order_id = orderId
            }
            //           transactionId = ""
            is_loyalty_applied = redeemLoyaltyInfo?.needToApplyLoyalty
            if (is_loyalty_applied == true) {
                loyalty_program_id = "${redeemLoyaltyInfo?.loyaltyProgramsModel?.id}"
                loyalty_amount =
                    if (splitValue == -1) redeemLoyaltyInfo?.usedLoyaltyAmount else redeemLoyaltyInfo?.usedLoyaltyAmount?.div(
                        splitValue
                    )
                used_reward_points =
                    if (splitValue == -1) redeemLoyaltyInfo?.usedLoyaltyPoints else redeemLoyaltyInfo?.usedLoyaltyPoints?.div(
                        splitValue
                    )
                is_loyalty_applied = redeemLoyaltyInfo?.needToApplyLoyalty
            }

        }
    }


    fun totalPayAmount(paymentAmount: Double) {

        totalPayAmounts = MethodUtils.roundOffAmountDouble(paymentAmount)

        Log.e("totalPayAmounts::", totalPayAmounts.toString())
    }

    fun saveOrder(isSave: Boolean) {
        onlySave = isSave
    }

    fun updateOrder(
        updateOrder: Boolean,
        orderId: Int?,
        paymentId: Int?,
        paymentOfflineId: String,
        orderOfflineId: String
    ) {

        isUpdateOrder = updateOrder
        this.orderId = orderId
        this.paymentId = paymentId
        this.paymentOfflineId = paymentOfflineId
        this.orderOfflineId = orderOfflineId

    }


    fun splitByOrder(myRequest: SpitByOrderRequestModel, isDineIn: Boolean) {

        _showProgress.value = Event(true)

        viewModelScope.launch {


            val resource = posRepository.splitByOrder(myRequest)

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { response ->
                        if (response?.status == 200) {

                            resource.data?.let { createOrderResponse ->
                                if (createOrderResponse.data.order.payments.isNotEmpty()) {
                                    prefProvider.setValueInt(
                                        PAYMENT_ID,
                                        createOrderResponse.data.order.payments[createOrderResponse.data.order.payments.size - 1].id
                                    )
                                }

                                if (myRequest.completed_all_payments) {
                                    posRepository.deleteCart(
                                        prefProvider.getValueInt(
                                            Constants.EMPLOYEE_ID,
                                            0
                                        )
                                    )
                                }

                                if (onlySave) {
                                    _data.value = Event(createOrderResponse)
                                } else {
                                    if (response.data.order.payments[response.data.order.payments.size - 1].paymentType != "Card") {
                                        cashLogApi(createOrderResponse, "in")
                                    } else {
                                        _data.value = Event(createOrderResponse)
                                    }
                                }



                                if (isDineIn)
                                    _msgText.value = Event(response.message)


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

    fun createQueuePrinter(
        createQueuePrinterModel: CreateQueuePrinterRequestModel,
        createOrder: CreateOrderResponse
    ) {
        _showProgress.value = Event(true)
        Log.e(
            "CreateOrderRequest",
            "createQueuePrinterModel  ${Gson().toJson(createQueuePrinterModel)}"
        )

        viewModelScope.launch {
            val resource = posRepository.createQueuePrinter(createQueuePrinterModel)

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    _data.value = Event(createOrder)
                    _queueCreateSaveOrder.value = Event(true)

                    // _queuePrinter.value = Event(resource?.data?.message.toString())

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

    fun setMagensaResponse(response: String?, cardNumber1: String) {

        magensaResponse = response
        cardNumberLast4 = cardNumber1

    }

    fun updateActiveOrderFlagClear() {


        prefProvider.setValueboolean(Constants.IS_UPDATE_ORDER, false)
        prefProvider.setValueInt(Constants.IS_UPDATE_ORDER_ID, -1)
        prefProvider.setValueInt(Constants.IS_UPDATE_ORDER_PAYMENT_ID, -1)
        prefProvider.setValue(Constants.IS_UPDATE_ORDER_PAY_OFFLINE_ID, "")
        prefProvider.setValue(Constants.IS_UPDATE_ORDER_OFFLINE_ID, "")
        prefProvider.setValueboolean(Constants.IS_UPDATE_ORDER_FROM_ACTIVE_ORDER, false)
        prefProvider.setValueboolean(Constants.IS_UPDATE_ORDER_LOYALTY_APPLIED, false)


    }
}