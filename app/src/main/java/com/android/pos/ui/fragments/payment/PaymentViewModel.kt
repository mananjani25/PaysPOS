package com.android.pos.ui.fragments.payment

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.db.AppDatabase
import com.android.pos.data.entities.*
import com.android.pos.data.model.requestModel.*
import com.android.pos.data.model.responseModel.CreateOrderResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.repositories.PosRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.TimeFormatUtils
import com.android.pos.utils.statusUtils.Status
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val appDatabase: AppDatabase,
    private val prefProvider: PrefProvider
) : ViewModel() {

    private var totalPayAmounts: Double = 0.0
    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<CreateOrderResponse?>>()
    val data: LiveData<Event<CreateOrderResponse?>> = _data

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress


    fun submit(orderRequestModel: OrderRequestModel) {

        _showProgress.value = Event(true)

        viewModelScope.launch {

            val resource = posRepository.createOrder(orderRequestModel)

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { response ->
                        if (response?.status == 200) {

                            prefProvider.setValue(Constants.ORDER_TYPE, "")
                            prefProvider.setValue(Constants.CUSTOMER_NAME, "")
                            posRepository.deleteCart()
                            resource.data?.let { createOrderResponse ->

                                cashLogApi(createOrderResponse, "in")
//                                _data.value = Event(createOrderResponse)
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
            order.payments[0].id,
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

                            Log.e("INOUT : Total Amount", order.totalAmount.toString())
                            Log.e("INOUT : Total PayAmount", totalPayAmounts.toString())

                            if (order.totalAmount == totalPayAmounts) {
                                _data.value = Event(createOrderResponse)
                            } else {
                                cashOutApi(createOrderResponse, "out")
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
            MethodUtils.roundOffAmountDouble(totalPayAmounts - createOrderResponse.data.order.totalAmount),
            order.employeeId,
            event,
            order.id,
            order.payments[0].id,
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
        totalDiscount: Double
    ): OrderRequestModel {

        val orderAttributeRequestModel = OrderAttributeRequestModel()

        orderAttributeRequestModel.date = TimeFormatUtils.getCurrentDate()
        if (future_delivery_date.isNotEmpty())
            orderAttributeRequestModel.futureDeliveryDate = future_delivery_date
        orderAttributeRequestModel.deliveryType = "Pickup"
        orderAttributeRequestModel.employeeId = cartModel.employeeID
        orderAttributeRequestModel.locationId = cartModel.locationId
        orderAttributeRequestModel.terminalId = cartModel.terminalId
        orderAttributeRequestModel.note = cartModel.note
        orderAttributeRequestModel.offlineId = randomOfflineId()
        orderAttributeRequestModel.openOrderType = cartModel.orderType
        orderAttributeRequestModel.orderTypeId = cartModel.orderTypeId
        orderAttributeRequestModel.paymentStatus = if (isPaid) 1 else 0
        orderAttributeRequestModel.serviceChargeEnabled = true
        orderAttributeRequestModel.taxEnabled = true
        orderAttributeRequestModel.subTotal = MethodUtils.roundOffAmountDouble(subTotalPrice)
        orderAttributeRequestModel.totalAmount = MethodUtils.roundOffAmountDouble(totalPrice)
        // orderAttributeRequestModel.totalCashDiscount = 0.0
        orderAttributeRequestModel.totalDiscount = totalDiscount
        orderAttributeRequestModel.totalServiceCharges =
            MethodUtils.roundOffAmountDouble(totalServiceCharge)
        orderAttributeRequestModel.totalTaxAmount = MethodUtils.roundOffAmountDouble(totalTax)
        orderAttributeRequestModel.totalTips = 0.0
        if (cartModel.customer != null)
            orderAttributeRequestModel.customer_id = cartModel.customer?.id


        orderAttributeRequestModel.paymentAttributes =
            paymentAttributes(
                cartModel,
                totalPrice,
                subTotalPrice,
                totalServiceCharge,
                totalTax,
                totalDiscount
            )
        orderAttributeRequestModel.orderServiceChargesAttributes =
            orderServiceChargesAttributes(cartModel, subTotalPrice)
        orderAttributeRequestModel.orderItemsAttributes = orderItemsAttributes(cartModel)

//        if (cartModel.customer != null)
//            orderAttributeRequestModel.customerAttributes = customerAttributes(cartModel)


        val orderRequestModel = OrderRequestModel(isPaid, orderAttributeRequestModel)

        Log.e("orderRequestModel", ":  ${Gson().toJson(orderRequestModel)}")

        return orderRequestModel
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

    private fun orderItemsAttributes(cartModel: CartModel): List<OrderItemsAttribute> {

        val orderItemsAttributeList: ArrayList<OrderItemsAttribute> =
            arrayListOf()

        cartModel.items?.forEach { item ->

            val orderItemsAttribute = OrderItemsAttribute()
            orderItemsAttribute.categoryId = if (item.isManualSales) 25 else item.categoryId
            orderItemsAttribute.discountAmount = 0.0
            orderItemsAttribute.discountTotalAmount = 0.0
            orderItemsAttribute.discountType = ""
            orderItemsAttribute.employeeId = cartModel.employeeID
            orderItemsAttribute.isCount = 0
            orderItemsAttribute.isEdited = false
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
            orderItemsAttribute.timestamp = System.currentTimeMillis().toString()
            orderItemsAttribute.totalPrice =
                MethodUtils.roundOffAmountDouble(item.price * item.itemQuantity)
            orderItemsAttribute.orderItemTaxesAttributes = orderItemTaxesAttributes(item)
            orderItemsAttribute.orderItemModifiersAttributes =
                orderItemModifierAttributes(item, cartModel.terminalId)
            orderItemsAttributeList.add(orderItemsAttribute)
        }
        return orderItemsAttributeList
    }

    private fun orderItemModifierAttributes(
        item: TbItem,
        terminalId: Int
    ): List<OrderItemModifierAttribute> {

        val orderItemModifierAttributeList: ArrayList<OrderItemModifierAttribute> =
            arrayListOf()

        item.modifiers.forEach {

            val orderItemModifierAttribute = OrderItemModifierAttribute().apply {
                name = it.name
                price = it.price
                order_item_id = item.itemId
                totalPrice = MethodUtils.roundOffAmountDouble(it.price * it.itemQuantity)
                modifier_set_id = it.modifierSetId!!
                quantity = it.itemQuantity
                order_item_taxes_attributes = orderModifierTaxesAttributes(item, it, terminalId)
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

        items.taxes?.forEach { tax ->
            val orderModifierTaxesAttribute = OrderModifierTaxesAttribute()
            orderModifierTaxesAttribute.order_item_id = items.itemId
            orderModifierTaxesAttribute.order_item_modifier_id = modifier.id
            orderModifierTaxesAttribute.tax_id = tax.id
            orderModifierTaxesAttribute.isDefault = tax.isDefault
            orderModifierTaxesAttribute.is_tax_removed = false
            orderModifierTaxesAttribute.is_modifier = true
            orderModifierTaxesAttribute.category_id = items.categoryId
            orderModifierTaxesAttribute.terminal_id = terminalId
            orderModifierTaxesAttribute.modifier_id = modifier.id!!
            orderModifierTaxesAttribute.timestamp = System.currentTimeMillis().toString()
            orderModifierTaxesAttribute.name = tax.name.toString()
            orderModifierTaxesAttribute.amount = tax.rate
            orderItemTaxesAttributeList.add(orderModifierTaxesAttribute)
        }


        return orderItemTaxesAttributeList

    }

    private fun orderItemTaxesAttributes(items: TbItem): List<OrderItemTaxesAttribute> {

        val orderItemTaxesAttributeList: ArrayList<OrderItemTaxesAttribute> =
            arrayListOf()

        items.taxes?.forEach { tax ->
            val orderItemTaxesAttribute = OrderItemTaxesAttribute()
            orderItemTaxesAttribute.isDefault = tax.isDefault
            orderItemTaxesAttribute.isTaxRemoved = true
            orderItemTaxesAttribute.name = tax.name.toString()
            orderItemTaxesAttribute.rate = tax.rate
            orderItemTaxesAttribute.taxId = tax.id

            val itemTaxPrice =
                (tax.rate * ((items.price - items.discountPrice) * items.itemQuantity)) / 100
            orderItemTaxesAttribute.taxTotalAmount =
                MethodUtils.roundOffAmountDouble(itemTaxPrice)
            orderItemTaxesAttribute.taxType = tax.taxType.toString()
            orderItemTaxesAttributeList.add(orderItemTaxesAttribute)
        }


        return orderItemTaxesAttributeList
    }

    private fun orderServiceChargesAttributes(
        cartModel: CartModel,
        subTotalPrice: Double
    ): List<OrderServiceChargesAttribute> {

        val orderServiceChargesAttributeList: ArrayList<OrderServiceChargesAttribute> =
            arrayListOf()

        cartModel.serviceCharge?.forEach {
            if (it.isEnabled) {
                val orderServiceChargesAttribute = OrderServiceChargesAttribute()
                orderServiceChargesAttribute.amount =
                    MethodUtils.roundOffAmountDouble((subTotalPrice * it.percentage) / 100)
                orderServiceChargesAttribute.name = it.name
                orderServiceChargesAttribute.rate = it.percentage
                orderServiceChargesAttribute.serviceChargeId = it.id
                orderServiceChargesAttributeList.add(orderServiceChargesAttribute)
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
        totalDis: Double
    ): PaymentAttributes {
        return PaymentAttributes().apply {
            amount = MethodUtils.roundOffAmountDouble(totalPrice)
//            cardName = ""
//            cardNumber = ""
//            cardType = 0
            cashDiscount = 0.0
            cashDiscountFee = 0.0
            employeeId = cartModel.employeeID
            offlineId = randomOfflineId()
            payableType = "Order"
            paymentType = "Cash"
            serviceChargeAmount = MethodUtils.roundOffAmountDouble(totalServiceCharge)
            subTotal = MethodUtils.roundOffAmountDouble(subTotalPrice)
            taxAmount = MethodUtils.roundOffAmountDouble(totalTax)
            terminalId = cartModel.terminalId
            tips = 0.0
            tipsAdjusted = false
            totalDiscount = MethodUtils.roundOffAmountDouble(totalDis)
            //           transactionId = ""
        }
    }

    private fun randomOfflineId(): String {

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

    fun totalPayAmount(paymentAmount: Double) {

        totalPayAmounts = MethodUtils.roundOffAmountDouble(paymentAmount)
    }
}