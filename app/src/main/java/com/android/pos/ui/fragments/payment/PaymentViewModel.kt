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
import com.android.pos.data.repositories.PosRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.TimeFormatUtils
import com.android.pos.utils.statusUtils.Resource
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

    private val TAG = "PaymentViewModel"
    private var isUpdateOrder: Boolean = false
    private var onlySave: Boolean = false
    private var totalPayAmounts: Double = 0.0
    private var orderId: Int? = null
    private var paymentId: Int? = null
    private var paymentOfflineId: String? = null
    private var orderOfflineId: String? = null
    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _msgText = MutableLiveData<Event<String>>()
    val msgText: LiveData<Event<String>> = _msgText

    private val _data = MutableLiveData<Event<CreateOrderResponse?>>()
    val data: LiveData<Event<CreateOrderResponse?>> = _data

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private val _data1 = MutableLiveData<Event<BaseResponse?>>()
    val data1: LiveData<Event<BaseResponse?>> = _data1

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

                                if (orderRequestModel.order.openOrderType == Constants.OPEN_ORDER) {
                                    prefProvider.setValue(Constants.ORDER_TYPE, "")
                                    prefProvider.setValue(Constants.CUSTOMER_NAME, "")
                                    prefProvider.setValueInt(Constants.CUSTOMER_ID, -1)
                                    posRepository.deleteCart()
                                }

                                if (onlySave) {
                                    _data.value = Event(createOrderResponse)
                                } else {
                                    if (createOrderResponse.data.order.orderType != "Dine In") {
                                        cashLogApi(createOrderResponse, "in")
                                    }
                                }

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

    fun dineInWholePayment(orderRequestModel: OrderRequestModel, orderId: Int) {
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

                            prefProvider.setValue(Constants.ORDER_TYPE, "")
                            prefProvider.setValue(Constants.CUSTOMER_NAME, "")
                            prefProvider.setValueInt(Constants.CUSTOMER_ID, -1)
                            posRepository.deleteCart()
                            resource.data?.let { createOrderResponse ->

                                if (onlySave) {
                                    _data.value = Event(createOrderResponse)
                                } else {
                                    if (createOrderResponse.data.order.orderType != "Dine In") {
                                        cashLogApi(createOrderResponse, "in")
                                    }
                                }

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
        totalDiscount: Double,
        tipAmount: Double,
        splitValue: Int
    ): OrderRequestModel {

        val orderAttributeRequestModel = OrderAttributeRequestModel()


        if (isUpdateOrder)
            orderAttributeRequestModel.id = orderId

        orderAttributeRequestModel.date = TimeFormatUtils.getCurrentDate()
        if (future_delivery_date.isNotEmpty())
            orderAttributeRequestModel.futureDeliveryDate = future_delivery_date
        orderAttributeRequestModel.deliveryType = "Pickup"
        orderAttributeRequestModel.employeeId = cartModel.employeeID
        orderAttributeRequestModel.locationId = cartModel.locationId
        orderAttributeRequestModel.terminalId = cartModel.terminalId
        orderAttributeRequestModel.note = cartModel.note
        orderAttributeRequestModel.offlineId =
            if (isUpdateOrder) orderOfflineId.toString() else randomOfflineId()
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

//        if (cartModel.customer != null)
//            orderAttributeRequestModel.customer_id = cartModel.customer?.id

        val customerId = prefProvider.getValueInt(Constants.CUSTOMER_ID, -1)
        if (customerId != -1) {
            orderAttributeRequestModel.customer_id = customerId
        }


        orderAttributeRequestModel.paymentAttributes =
            paymentAttributes(
                cartModel,
                totalPrice,
                subTotalPrice,
                totalServiceCharge,
                totalTax,
                totalDiscount, tipAmount, splitValue
            )
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

                val orderItemsAttribute = OrderItemsAttribute()

                if (isUpdateOrder && item.orderItemId != null)
                    orderItemsAttribute.id = item.orderItemId


                orderItemsAttribute.category_id = item.categoryId

                orderItemsAttribute.discountAmount = item.discountPrice
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
                orderItemsAttribute.timestamp = System.currentTimeMillis().toString()
                orderItemsAttribute.totalPrice =
                    MethodUtils.roundOffAmountDouble(item.price * item.itemQuantity)
                orderItemsAttribute.orderItemTaxesAttributes = orderItemTaxesAttributes(item)
                orderItemsAttribute.orderItemModifiersAttributes =
                    orderItemModifierAttributes(item, cartModel.terminalId)

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

        cartModel.items?.forEach { item ->

            val orderItemsAttribute = OrderItemsAttribute()

            if (isUpdateOrder && item.orderItemId != null)
                orderItemsAttribute.id = item.orderItemId


            orderItemsAttribute.category_id = item.categoryId

            orderItemsAttribute.discountAmount = item.discountPrice
            orderItemsAttribute.discountType = item.discountType
            if (item.discountId != -1)
                orderItemsAttribute.discountId = item.discountId
            orderItemsAttribute.employeeId = cartModel.employeeID
            orderItemsAttribute.isCount = 0
            orderItemsAttribute.isEdited = item.isEdited
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
            orderItemsAttribute.timestamp = randomOfflineId()
            orderItemsAttribute.totalPrice =
                MethodUtils.roundOffAmountDouble(item.price * item.itemQuantity)
            orderItemsAttribute.orderItemTaxesAttributes = orderItemTaxesAttributes(item)
            orderItemsAttribute.orderItemModifiersAttributes =
                orderItemModifierAttributes(item, cartModel.terminalId)

            orderItemsAttribute.orderItemVariationAttributes =
                orderItemVariationAttributes(item)

            if (item.variationsAttributes.isNotEmpty()) {
                orderItemsAttribute.variationId = item.variationsAttributes[0].id
            }

            orderItemsAttributeList.add(orderItemsAttribute)
        }
        return orderItemsAttributeList
    }

    private fun orderItemVariationAttributes(
        item: TbItem
    ): OrderItemVariationAttribute? {

        if (item.variationsAttributes.isNotEmpty()) {

            item.variationsAttributes.forEach {

                val orderItemVariationAttribute = OrderItemVariationAttribute()
                orderItemVariationAttribute.name = it.name
                orderItemVariationAttribute.price = it.price!!
                orderItemVariationAttribute.totalPrice = it.price!! * item.itemQuantity
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

        item.modifiers.forEach {

            val orderItemModifierAttribute = OrderItemModifierAttribute().apply {

                if (isUpdateOrder && it.orderModifierId != null)
                    id = it.orderModifierId

                name = it.name
                price = it.price
                order_item_id = item.orderItemId
                totalPrice = MethodUtils.roundOffAmountDouble(it.price * it.itemQuantity)
                modifier_set_id = it.modifierSetId!!
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

        cartModel.serviceCharge?.forEach {
            if (it.isEnabled) {
                val orderServiceChargesAttribute = OrderServiceChargesAttribute()
                orderServiceChargesAttribute.amount =
                    MethodUtils.roundOffAmountDouble((subTotalPrice * it.percentage) / 100)
                orderServiceChargesAttribute.name = it.name
                orderServiceChargesAttribute.rate = it.percentage
                orderServiceChargesAttribute.serviceChargeId = it.id

                if (isUpdateOrder && it.order_service_charge_id != null)
                    orderServiceChargesAttribute.id = it.order_service_charge_id

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
        totalDis: Double,
        tipAmount: Double,
        splitValue: Int
    ): PaymentAttributes {
        return PaymentAttributes().apply {
//            if (isUpdateOrder)
//                id = paymentId
            amount =
                MethodUtils.roundOffAmountDouble(totalPrice) - MethodUtils.roundOffAmountDouble(
                    tipAmount
                )
//            cardName = ""
//            cardNumber = ""
//            cardType = 0
            cashDiscount = 0.0
            cashDiscountFee = 0.0
            employeeId = cartModel.employeeID
            offlineId = if (isUpdateOrder) paymentOfflineId.toString() else randomOfflineId()
            payableType = "Order"
            paymentType = "Cash"
            serviceChargeAmount =
                if (splitValue == -1) MethodUtils.roundOffAmountDouble(totalServiceCharge) else MethodUtils.roundOffAmountDouble(
                    totalServiceCharge
                ) / splitValue
            subTotal = MethodUtils.roundOffAmountDouble(subTotalPrice)
            taxAmount =
                if (splitValue == -1) MethodUtils.roundOffAmountDouble(totalTax) else MethodUtils.roundOffAmountDouble(
                    totalTax
                ) / splitValue
            terminalId = cartModel.terminalId
            tips = MethodUtils.roundOffAmountDouble(tipAmount)
            tipsAdjusted = false
            totalDiscount =
                if (splitValue == -1) MethodUtils.roundOffAmountDouble(totalDis) else MethodUtils.roundOffAmountDouble(
                    totalDis
                ) / splitValue

            if (isUpdateOrder && orderId != null) {
                order_id = orderId
            }
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

    fun splitByOrder(myRequest: SpitByOrderRequestModel) {

        _showProgress.value = Event(true)

        viewModelScope.launch {


            val resource = posRepository.splitByOrder(myRequest)

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { response ->
                        if (response?.status == 200) {

                            resource.data?.let { createOrderResponse ->

                                if (onlySave) {
                                    _data.value = Event(createOrderResponse)
                                } else {
                                    cashLogApi(createOrderResponse, "in")
                                }


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
}