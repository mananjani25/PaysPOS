package com.android.pos.ui.fragments.dinein

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.pos.data.model.DineInOrderDetailAttributes
import com.android.pos.data.model.MergeTableModel
import com.android.pos.data.model.requestModel.*
import com.android.pos.data.model.responseModel.*
import com.android.pos.data.remote.Constants.EMPLOYEE_ID
import com.android.pos.data.remote.Constants.LOCATION_ID
import com.android.pos.data.remote.Constants.TERMINAL_ID
import com.android.pos.data.repositories.PosRepository
import com.android.pos.di.PrefProvider
import com.android.pos.utils.Event
import com.android.pos.utils.statusUtils.Resource
import com.android.pos.utils.statusUtils.Status
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DineInViewModel @Inject constructor(
    private val posRepository: PosRepository,
    private val prefProvider: PrefProvider
) : ViewModel() {

    private val TAG = "DineInViewModel"

    private val _snackbarText = MutableLiveData<Event<String?>>()
    val snackbarText: LiveData<Event<String?>> = _snackbarText

    private val _data = MutableLiveData<Event<CreateNoteResponse?>>()
    val data: LiveData<Event<CreateNoteResponse?>> = _data

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    val _tableStatus = MutableLiveData<Event<String>>()
    val tableCheck: LiveData<Event<String>> = _tableStatus

    val _tableStatusSuccess = MutableLiveData<Event<Int>>()
    val tableCheckSuccess: LiveData<Event<Int>> = _tableStatusSuccess

    val _mergeStatus = MutableLiveData<Event<String>>()
    val mergeStatusChange: LiveData<Event<String>> = _mergeStatus

    val _unMergeStatus = MutableLiveData<Event<String>>()
    val unMergeStatusUpdate: LiveData<Event<String>> = _unMergeStatus

    fun getFloorPlan(): LiveData<Resource<GetFloorPlanResponse>> {
        return posRepository.getFloorPlan(prefProvider.getValueInt(LOCATION_ID, 0))
    }

    // val getFloorPlan = posRepository.getFloorPlan(prefProvider.getValueInt(LOCATION_ID, 0))

    val getFloorPlanDetails =
        posRepository.getFloorPlanTableDetails()

    fun mergeTable(
        parentTableId: Int,
        childIds: String,
        mergedChildOrderIds: String? = null,
        orderModel: OrderAttributeRequestModel? = null,
        orderId: Int? = null
    ) {
        _showProgress.value = Event(true)
        var mergeORder = MergeTableRequest(orderModel)
        viewModelScope.launch {
            val resource =
                posRepository.mergeFloorTable(
                    parentTableId,
                    childIds,
                    mergeORder,
                    mergedChildOrderIds,
                    orderId
                )

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    (resource.data?.message?.let {
                        _mergeStatus.value = Event(it)
                    })
                }
                Status.LOADING -> {
                    _showProgress.value = Event(true)

                }
                Status.ERROR -> {
                    _snackbarText.value = Event(resource.message.toString())
                    _showProgress.value = Event(false)

                }

            }

        }
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

    fun getTableStatus(tableId: Int, status: String) {
        _showProgress.value = Event(true)
        viewModelScope.launch {
            val resource = posRepository.getTableStatus(
                tableId, prefProvider.getValueInt(
                    EMPLOYEE_ID, 0
                ), prefProvider.getValueInt(TERMINAL_ID, 0), status
            )

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { response ->
                        Log.e(TAG, "getTableStatusResponse:  ${Gson().toJson(response)}")
                        if (response?.status == 200) {
                            _tableStatusSuccess.value = Event(response.status)

                        } else {
                            _tableStatus.value = response?.let { Event(it.message) }
                        }

                    }
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

    fun mergeTwoOrders(
        primaryOrder: GetFloorPlanDetailResponse.OrderDetails,
        secondaryOrder: GetFloorPlanDetailResponse.OrderDetails
    ): OrderAttributeRequestModel {
        var model = OrderAttributeRequestModel()

        model.date = primaryOrder.date
//        model.deliveryType = orderDetails.delivery_type
        model.employeeId = primaryOrder.employee_id
        //model.mergedTableNumbers = totalGuestCount
/*
        model.futureDeliveryDate = orderDetails.future_delivery_date
        model.futureDeliveryTime = orderDetails.future_delivery_time
*/
        //   model.id = primaryOrder.id
        model.locationId = primaryOrder.location_id
        model.note = primaryOrder.note
        model.offlineId = primaryOrder.offline_id
        //model.openOrderType = orderDetails.open_order_type

        //Order Items Attributes
        var orderItemsAttr: ArrayList<OrderItemsAttribute> = arrayListOf()
        primaryOrder.order_items.forEach {
            var model = OrderItemsAttribute()
            model.timestamp = it.timestamp
            model.category_id = it.categoryId
            model.discountAmount = it.discountAmount
            model.discountId = it.discountId
            model.discountType = it.discountType
            model.editTimestamp = it.timestamp
            model.employeeId = it.employeeId
            //   model.id = it.id
            model.isPaid = it.isPaid
            model.itemId = it.itemId
            model.itemName = it.itemName
            model.quantity = it.quantity
            model.price = it.price
            model.totalPrice = it.totalPrice
            model.note = it.note
            var modifierList: ArrayList<OrderItemModifierAttribute> = arrayListOf()
            it.orderItemModifiers.forEach { modifier ->
                var orderModifier = OrderItemModifierAttribute()
                //    orderModifier.id = modifier.id
                orderModifier.price = modifier.price
                orderModifier.quantity = modifier.quantity
                orderModifier.name = modifier.name
                orderModifier.totalPrice = modifier.price
                var itemTaxes: ArrayList<OrderModifierTaxesAttribute> = arrayListOf()
                modifier.orderItemTaxes.forEach { tax ->
                    var modifierTax = OrderModifierTaxesAttribute()
                    modifierTax.name = tax.name
                    modifierTax.tax_id = tax.taxId
                    modifierTax.amount = tax.amount
                    //      modifierTax.id = tax.id
                    modifierTax.isDefault = tax.isDefault
                    // modifierTax.order_id = tax.orderId
                    modifierTax.order_item_id = tax.orderItemId
                    modifierTax.order_item_modifier_id = tax.orderItemModifierId
                    modifierTax.taxType = tax.taxType
                    modifierTax.is_tax_removed = tax.isTaxRemoved
                    modifierTax.taxTotalAmount = tax.taxTotalAmount
                    itemTaxes.add(modifierTax)


                }
                orderModifier.order_item_taxes_attributes = itemTaxes
                //  orderModifier.orderId = modifier.orderId
                orderModifier.order_item_id = modifier.orderItemId



                modifierList.add(orderModifier)

                model.orderItemModifiersAttributes = modifierList
            }



            orderItemsAttr.add(model)
        }
        secondaryOrder.order_items.forEach {
            var model = OrderItemsAttribute()
            model.timestamp = it.timestamp
            model.category_id = it.categoryId
            model.discountAmount = it.discountAmount
            model.discountId = it.discountId
            model.discountType = it.discountType
            model.editTimestamp = it.timestamp
            model.employeeId = it.employeeId
            //model.id = it.id
            model.isPaid = it.isPaid
            model.itemId = it.itemId
            model.itemName = it.itemName
            model.price = it.price
            model.totalPrice = it.totalPrice
            model.quantity = it.quantity
            model.note = it.note
            var modifierList: ArrayList<OrderItemModifierAttribute> = arrayListOf()
            it.orderItemModifiers.forEach { modifier ->
                var orderModifier = OrderItemModifierAttribute()
                //orderModifier.id = modifier.id
                orderModifier.price = modifier.price
                orderModifier.quantity = modifier.quantity
                orderModifier.name = modifier.name
                orderModifier.totalPrice = modifier.price
                var itemTaxes: ArrayList<OrderModifierTaxesAttribute> = arrayListOf()
                modifier.orderItemTaxes.forEach { tax ->
                    var modifierTax = OrderModifierTaxesAttribute()
                    modifierTax.name = tax.name
                    modifierTax.tax_id = tax.taxId
                    modifierTax.amount = tax.amount
                    // modifierTax.id = tax.id
                    modifierTax.isDefault = tax.isDefault
                    // modifierTax.order_id = tax.orderId
                    modifierTax.order_item_id = tax.orderItemId
                    modifierTax.order_item_modifier_id = tax.orderItemModifierId
                    modifierTax.taxType = tax.taxType
                    modifierTax.is_tax_removed = tax.isTaxRemoved
                    modifierTax.taxTotalAmount = tax.taxTotalAmount
                    itemTaxes.add(modifierTax)


                }
                orderModifier.order_item_taxes_attributes = itemTaxes
                //orderModifier.orderId = modifier.orderId
                //orderModifier.order_item_id = modifier.orderItemId


                modifierList.add(orderModifier)

                model.orderItemModifiersAttributes = modifierList
            }


            orderItemsAttr.add(model)
        }

        model.orderItemsAttributes = orderItemsAttr

        //Guest Attributes
        var listGuestAttr: ArrayList<GuestsAttributes> = arrayListOf()
        primaryOrder.guest_attributes.forEach {
            var guestModel = GuestsAttributes()
            guestModel.customerAttributes?.id = it.customerId
            guestModel.customerId = it.id
            guestModel.name = it.name
            guestModel.cashDiscount = it.cashDiscount
            guestModel.orderId = it.orderId
            guestModel.isPaid = it.isPaid
            // guestModel.id = it.id
            guestModel.totalAmount = it.totalAmount
            guestModel.totalDiscount = it.totalDiscount
            guestModel.totalServiceCharge = it.totalServiceCharge
            guestModel.totalTax = it.totalTax
            guestModel.subTotal = it.subTotal
            guestModel.totalTips = it.totalTips

            var guestItemList: ArrayList<GuestItemsAttributes> = arrayListOf()
            it.guestItemAttributes.forEach {
                var guestItemAttr = GuestItemsAttributes()
                //   guestItemAttr.id = it.id
                guestItemAttr.amount = it.amount
                guestItemAttr.isPaid = it.isPaid
                guestItemAttr.orderItemId = it.orderItemId
                guestItemAttr.orderId = it.orderId
                guestItemAttr.guestId = it.guestId
                guestItemAttr.itemId = it.itemId
                guestItemAttr.timestamp = it.timestamp
                guestItemList.add(guestItemAttr)

            }
            guestModel.guestItemsAttributes = guestItemList
            listGuestAttr.add(guestModel)

        }
        secondaryOrder.guest_attributes.forEach {
            var guestModel = GuestsAttributes()
            guestModel.customerAttributes?.id = it.customerId
            guestModel.customerId = it.id
            guestModel.name = it.name
            guestModel.cashDiscount = it.cashDiscount
            guestModel.orderId = it.orderId
            guestModel.isPaid = it.isPaid
            //  guestModel.id = it.id
            guestModel.totalAmount = it.totalAmount
            guestModel.totalDiscount = it.totalDiscount
            guestModel.totalServiceCharge = it.totalServiceCharge
            guestModel.totalTax = it.totalTax
            guestModel.subTotal = it.subTotal
            guestModel.totalTips = it.totalTips

            var guestItemList: ArrayList<GuestItemsAttributes> = arrayListOf()
            it.guestItemAttributes.forEach {
                var guestItemAttr = GuestItemsAttributes()
                //   guestItemAttr.id = it.id
                guestItemAttr.amount = it.amount
                guestItemAttr.isPaid = it.isPaid
                guestItemAttr.orderItemId = it.orderItemId
                guestItemAttr.orderId = it.orderId
                guestItemAttr.guestId = it.guestId
                guestItemAttr.itemId = it.itemId
                guestItemAttr.timestamp = it.timestamp
                guestItemList.add(guestItemAttr)

            }
            guestModel.guestItemsAttributes = guestItemList
            listGuestAttr.add(guestModel)

        }

        /*  for (i in 0 until mergeTableLists.size) {

              var guestModel = GuestsAttributes()
              var guestCount = listGuestAttr.size

              guestModel.name = "Guest ${guestCount}"

              guestModel.orderId = listGuestAttr.get(0).orderId
              guestModel.isChildGuest = true
              guestModel.childMergeId = mergeTableLists[i].id

              *//*var guestItemList: ArrayList<GuestItemsAttributes> = arrayListOf()
            guestModel.guestItemsAttributes = guestItemList
*//*            listGuestAttr.add(guestModel)


        }*/
        model.guestsAttributes = listGuestAttr
        var dineInOrderDetails = DineInOrderDetailAttributes()
        dineInOrderDetails.chairCount =
            primaryOrder.floor_plan_table.chair_count + secondaryOrder.floor_plan_table.chair_count
        //dineInOrderDetails.id = orderDetails.floor_plan_table.id
        //dineInOrderDetails.totalGuestCount = totalGuestCount
        // dineInOrderDetails.orderId = primaryOrder.floor_plan_table.order_details?.order_type_id
        dineInOrderDetails.floorPlanId = primaryOrder.floor_plan_table.floor_plan_id
        dineInOrderDetails.floorPlanTableId = primaryOrder.floor_plan_table.id
        dineInOrderDetails.tableType = primaryOrder.floor_plan_table.table_type
        dineInOrderDetails.tableNumber = primaryOrder.floor_plan_table.table_number
        dineInOrderDetails.tableName = primaryOrder.floor_plan_table.table_name
        // dineInOrderDetails.floorPlanName = orderDetails.floor_plan_table.order_details.floor_plan_table.na
        model.dineInOrderDetailsAttr = dineInOrderDetails

        var listServiceCharge: ArrayList<OrderServiceChargesAttribute> = arrayListOf()

        primaryOrder.order_service_charges.forEach {
            var serviceModel = OrderServiceChargesAttribute()
            serviceModel.amount = it.amount
            //serviceModel.id = it.id
            serviceModel.name = it.name
            //  serviceModel.orderId = it.orderId
            serviceModel.rate = it.rate
            serviceModel.serviceChargeId = it.serviceChargeId

            listServiceCharge.add(serviceModel)
        }
        secondaryOrder.order_service_charges.forEach {
            var serviceModel = OrderServiceChargesAttribute()
            serviceModel.amount = it.amount
            //serviceModel.id = it.id
            serviceModel.name = it.name
            // serviceModel.orderId = it.orderId
            serviceModel.rate = it.rate
            serviceModel.serviceChargeId = it.serviceChargeId

            listServiceCharge.add(serviceModel)
        }

        model.orderServiceChargesAttributes = listServiceCharge
        model.orderTypeId = primaryOrder.order_type_id

        //--------------------NEED TO ADD PAYMENT ATTRIBUTE-----------------------//

        //  model.paymentStatus = orderDetails.payment_status
        model.serviceChargeEnabled = primaryOrder.service_charge_enabled
        model.subTotal = primaryOrder.sub_total + secondaryOrder.sub_total
        model.taxEnabled = primaryOrder.tax_enabled
        model.terminalId = primaryOrder.terminal_id
        model.totalAmount = primaryOrder.total_amount + secondaryOrder.total_amount
        model.totalDiscount = primaryOrder.total_discount + secondaryOrder.total_discount
        model.totalServiceCharges =
            primaryOrder.total_service_charges + secondaryOrder.total_service_charges
        model.totalTaxAmount = primaryOrder.total_tax_amount + secondaryOrder.total_tax_amount
        model.totalTips = primaryOrder.total_tips + secondaryOrder.total_tips
        //model.customer_id = primaryOrder.customer_id
        model.discount_id = primaryOrder.discount_id
        var mergedOrderIds: ArrayList<Int> = arrayListOf(primaryOrder.id, secondaryOrder.id)
        //model.mergedOrderIds = mergedOrderIds


        return model
    }

    fun createMergeOrderRequest(
        orderDetails: GetFloorPlanDetailResponse.OrderDetails,
        mergeTableLists: ArrayList<MergeTableModel>
    ): OrderAttributeRequestModel {
        var model = OrderAttributeRequestModel()
        model.date = orderDetails.date
//        model.deliveryType = orderDetails.delivery_type
        model.employeeId = orderDetails.employee_id
        //model.mergedTableNumbers = totalGuestCount
/*
        model.futureDeliveryDate = orderDetails.future_delivery_date
        model.futureDeliveryTime = orderDetails.future_delivery_time
*/
        model.id = orderDetails.id
        model.locationId = orderDetails.location_id
        model.note = orderDetails.note
        model.offlineId = orderDetails.offline_id
        //model.openOrderType = orderDetails.open_order_type

        //Order Items Attributes
        var orderItemsAttr: ArrayList<OrderItemsAttribute> = arrayListOf()
        orderDetails.order_items.forEach {
            var model = OrderItemsAttribute()
            model.timestamp = it.timestamp
            model.category_id = it.categoryId
            model.discountAmount = it.discountAmount
            model.discountId = it.discountId
            model.discountType = it.discountType
            model.editTimestamp = it.timestamp
            model.employeeId = it.employeeId
            model.id = it.id
            model.isPaid = it.isPaid
            model.itemId = it.itemId
            model.itemName = it.itemName
            model.note = it.note
            model.quantity = it.quantity
            model.totalPrice = it.totalPrice
            model.price = it.price

            var modifierList: ArrayList<OrderItemModifierAttribute> = arrayListOf()
            it.orderItemModifiers.forEach { modifier ->
                var orderModifier = OrderItemModifierAttribute()
                orderModifier.id = modifier.id
                orderModifier.price = modifier.price
                orderModifier.quantity = modifier.quantity
                orderModifier.name = modifier.name
                orderModifier.totalPrice = modifier.price
                var itemTaxes: ArrayList<OrderModifierTaxesAttribute> = arrayListOf()
                modifier.orderItemTaxes.forEach { tax ->
                    var modifierTax = OrderModifierTaxesAttribute()
                    modifierTax.name = tax.name
                    modifierTax.tax_id = tax.taxId
                    modifierTax.amount = tax.amount
                    modifierTax.id = tax.id
                    modifierTax.isDefault = tax.isDefault
                    modifierTax.order_id = tax.orderId
                    modifierTax.order_item_id = tax.orderItemId
                    modifierTax.order_item_modifier_id = tax.orderItemModifierId
                    modifierTax.taxType = tax.taxType
                    modifierTax.is_tax_removed = tax.isTaxRemoved
                    modifierTax.taxTotalAmount = tax.taxTotalAmount
                    itemTaxes.add(modifierTax)


                }
                orderModifier.order_item_taxes_attributes = itemTaxes
                orderModifier.orderId = modifier.orderId
                orderModifier.order_item_id = modifier.orderItemId



                modifierList.add(orderModifier)

                model.orderItemModifiersAttributes = modifierList
            }


            orderItemsAttr.add(model)
        }

        model.orderItemsAttributes = orderItemsAttr

        //Guest Attributes
        var listGuestAttr: ArrayList<GuestsAttributes> = arrayListOf()
        orderDetails.guest_attributes.forEach {
            var guestModel = GuestsAttributes()
            guestModel.customerAttributes?.id = it.customerId
            guestModel.customerId = it.id
            guestModel.name = it.name
            guestModel.cashDiscount = it.cashDiscount
            guestModel.orderId = it.orderId
            guestModel.isPaid = it.isPaid
            guestModel.id = it.id
            guestModel.totalAmount = it.totalAmount
            guestModel.totalDiscount = it.totalDiscount
            guestModel.totalServiceCharge = it.totalServiceCharge
            guestModel.totalTax = it.totalTax
            guestModel.subTotal = it.subTotal
            guestModel.totalTips = it.totalTips

            var guestItemList: ArrayList<GuestItemsAttributes> = arrayListOf()
            it.guestItemAttributes.forEach {
                var guestItemAttr = GuestItemsAttributes()
                guestItemAttr.id = it.id
                guestItemAttr.amount = it.amount
                guestItemAttr.isPaid = it.isPaid
                guestItemAttr.orderItemId = it.orderItemId
                guestItemAttr.orderId = it.orderId
                guestItemAttr.guestId = it.guestId
                guestItemAttr.itemId = it.itemId
                guestItemAttr.quantity = it.quantity
                guestItemAttr.timestamp = it.timestamp
                guestItemList.add(guestItemAttr)


            }
            guestModel.guestItemsAttributes = guestItemList
            listGuestAttr.add(guestModel)

        }

        for (i in 0 until mergeTableLists.size) {

            var guestModel = GuestsAttributes()
            var guestCount = listGuestAttr.size

            guestModel.name = "Guest ${guestCount}"

            guestModel.orderId = listGuestAttr.get(0).orderId
            guestModel.isChildGuest = true
            guestModel.childMergeId = mergeTableLists[i].id

            /*var guestItemList: ArrayList<GuestItemsAttributes> = arrayListOf()
            guestModel.guestItemsAttributes = guestItemList
*/            listGuestAttr.add(guestModel)


        }
        model.guestsAttributes = listGuestAttr
        var dineInOrderDetails = DineInOrderDetailAttributes()
        dineInOrderDetails.chairCount = orderDetails.floor_plan_table.chair_count
        //dineInOrderDetails.id = orderDetails.floor_plan_table.id
        //dineInOrderDetails.totalGuestCount = totalGuestCount
        dineInOrderDetails.orderId = orderDetails.floor_plan_table.order_details?.order_type_id
        dineInOrderDetails.floorPlanId = orderDetails.floor_plan_table.floor_plan_id
        dineInOrderDetails.floorPlanTableId = orderDetails.floor_plan_table.id
        dineInOrderDetails.tableType = orderDetails.floor_plan_table.table_type
        dineInOrderDetails.tableNumber = orderDetails.floor_plan_table.table_number
        dineInOrderDetails.tableName = orderDetails.floor_plan_table.table_name
        // dineInOrderDetails.floorPlanName = orderDetails.floor_plan_table.order_details.floor_plan_table.na
        model.dineInOrderDetailsAttr = dineInOrderDetails

        var listServiceCharge: ArrayList<OrderServiceChargesAttribute> = arrayListOf()

        orderDetails.order_service_charges.forEach {
            var serviceModel = OrderServiceChargesAttribute()
            serviceModel.amount = it.amount
            serviceModel.id = it.id
            serviceModel.name = it.name
            serviceModel.orderId = it.orderId
            serviceModel.rate = it.rate
            serviceModel.serviceChargeId = it.serviceChargeId

            listServiceCharge.add(serviceModel)
        }

        model.orderServiceChargesAttributes = listServiceCharge
        model.orderTypeId = orderDetails.order_type_id

        //--------------------NEED TO ADD PAYMENT ATTRIBUTE-----------------------//

        //  model.paymentStatus = orderDetails.payment_status
        model.serviceChargeEnabled = orderDetails.service_charge_enabled
        model.subTotal = orderDetails.sub_total
        model.taxEnabled = orderDetails.tax_enabled
        model.terminalId = orderDetails.terminal_id
        model.totalAmount = orderDetails.total_amount
        model.totalDiscount = orderDetails.total_discount
        model.totalServiceCharges = orderDetails.total_service_charges
        model.totalTaxAmount = orderDetails.total_tax_amount
        model.totalTips = orderDetails.total_tips
        model.customer_id = orderDetails.customer_id
        model.discount_id = orderDetails.discount_id
/*
        model.loyalty_program_id = orderDetails.loyalty_program_id
        model.loyalty_amount = orderDetails.loyalty_amount
        model.used_reward_points = orderDetails.used_reward_points
        model.is_loyalty_applied = orderDetails.is_loyalty_applied
*/




        return model
    }

}