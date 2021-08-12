package com.android.pos.ui.fragments.payment

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.db.AppDatabase
import com.android.pos.data.entities.CartModel
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.requestModel.*
import com.android.pos.data.model.responseModel.BaseResponse
import com.android.pos.data.remote.Constants
import com.android.pos.data.repositories.PosRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.MethodUtils
import com.android.pos.utils.statusUtils.Status
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
open class PaymentViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val appDatabase: AppDatabase,
    private val prefProvider: PrefProvider
) : ViewModel() {

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<BaseResponse?>>()
    val data: LiveData<Event<BaseResponse?>> = _data

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress


    fun deleteCart() {
        viewModelScope.launch {
            posRepository.deleteCart()
        }

    }

    fun submit(orderRequestModel: OrderRequestModel) {

        viewModelScope.launch {

            val resource = posRepository.createOrder(orderRequestModel)

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


    fun createOrderRequest(
        cartModel: CartModel,
        subTotalPrice: Double,
        totalPrice: Double,
        totalServiceCharge: Double,
        totalTax: Double
    ): OrderRequestModel {

        val orderAttributeRequestModel = OrderAttributeRequestModel()

        orderAttributeRequestModel.date = "2021-08-12"
        orderAttributeRequestModel.deliveryType = "Pickup"
        orderAttributeRequestModel.employeeId = cartModel.employeeID
        orderAttributeRequestModel.indexOfDate = ""
        orderAttributeRequestModel.locationId = cartModel.locationId
        orderAttributeRequestModel.terminalId = cartModel.terminalId
        orderAttributeRequestModel.note = cartModel.note
        orderAttributeRequestModel.offlineId = randomOfflineId()
        orderAttributeRequestModel.openOrderType = ""
        orderAttributeRequestModel.orderTypeId = 0
        orderAttributeRequestModel.paymentStatus = ""
        orderAttributeRequestModel.serviceChargeEnabled = true
        orderAttributeRequestModel.taxEnabled = true
        orderAttributeRequestModel.subTotal = MethodUtils.roundOffAmountDouble(subTotalPrice)
        orderAttributeRequestModel.totalAmount = MethodUtils.roundOffAmountDouble(totalPrice)
        orderAttributeRequestModel.totalCashDiscount = 0.0
        orderAttributeRequestModel.totalDiscount = 0.0
        orderAttributeRequestModel.totalServiceCharges =
            MethodUtils.roundOffAmountDouble(totalServiceCharge)
        orderAttributeRequestModel.totalTaxAmount = MethodUtils.roundOffAmountDouble(totalTax)
        orderAttributeRequestModel.totalTips = 0.0


        orderAttributeRequestModel.paymentAttributes =
            paymentAttributes(cartModel, totalPrice, subTotalPrice, totalServiceCharge, totalTax)
        orderAttributeRequestModel.orderServiceChargesAttributes =
            orderServiceChargesAttributes(cartModel, subTotalPrice)
        orderAttributeRequestModel.orderItemsAttributes = orderItemsAttributes(cartModel)


        val orderRequestModel = OrderRequestModel(true, orderAttributeRequestModel)

        Log.e("orderRequestModel", ":  ${Gson().toJson(orderRequestModel)}")

        return orderRequestModel
    }

    private fun orderItemsAttributes(cartModel: CartModel): List<OrderItemsAttribute> {

        val orderItemsAttributeList: ArrayList<OrderItemsAttribute> =
            arrayListOf()

        cartModel.items?.forEach { item ->

            val orderItemsAttribute = OrderItemsAttribute()
            orderItemsAttribute.categoryId = item.categoryId
            orderItemsAttribute.discountAmount = 0.0
            orderItemsAttribute.discountTotalAmount = 0.0
            orderItemsAttribute.discountType = ""
            orderItemsAttribute.employeeId = cartModel.employeeID
            orderItemsAttribute.isCount = 0
            orderItemsAttribute.isEdited = false
            orderItemsAttribute.isPaid = false
            orderItemsAttribute.isPrinted = true
            orderItemsAttribute.isTaxRemoved = false
            orderItemsAttribute.itemId = item.itemId
            orderItemsAttribute.itemName = item.name
            orderItemsAttribute.note = item.note
            orderItemsAttribute.price = item.price
            orderItemsAttribute.quantity = item.itemQuantity
            orderItemsAttribute.terminalId = cartModel.terminalId
            orderItemsAttribute.timestamp = System.currentTimeMillis().toString()
            orderItemsAttribute.totalPrice =
                MethodUtils.roundOffAmountDouble(item.price * item.itemQuantity)
            orderItemsAttribute.orderItemTaxesAttributes = orderItemTaxesAttributes(item)
            orderItemsAttributeList.add(orderItemsAttribute)
        }
        return orderItemsAttributeList
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
            orderItemTaxesAttribute.taxTotalAmount =
                MethodUtils.roundOffAmountDouble((items.price * items.itemQuantity) / 100)
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
        totalTax: Double
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
            totalDiscount = 0.0
            //           transactionId = ""
        }
    }

    private fun randomOfflineId(): String {

        val locationId = prefProvider.getValue(Constants.LOCATION_ID, "")
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
}