package com.android.pos.data.remote

import android.util.Log
import com.android.pos.data.entities.TbBusinessDetails
import com.android.pos.data.entities.VariationsAttribute
import com.android.pos.data.model.requestModel.*
import com.android.pos.utils.FileUtils.getContentType
import com.android.pos.utils.MethodUtils
import javax.inject.Inject

class ApiHelper @Inject constructor(private val apiService: ApiService) : BaseDataSource() {

    suspend fun userLogIn(data: HashMap<String, String>) = getResult { apiService.userLogIn(data) }
    suspend fun getDefaultTerminal(uniq_id: String, device_token: String) =
        getResult { apiService.getDefaultTerminal(uniq_id, device_token) }

    suspend fun employeeClockIn(data: HashMap<String, String>) =
        getResult { apiService.employeeClockIn(data) }

    suspend fun employeeLogIn(data: HashMap<String, String>) =
        getResult { apiService.employeeLogIn(data) }

    suspend fun employeeClockOut(data: HashMap<String, String>) =
        getResult { apiService.employeeClockOut(data) }

    suspend fun forgotPassword(data: HashMap<String, String>) =
        getResult { apiService.forgotPassword(data) }

    suspend fun syncVenueData() =
        getResult { apiService.syncVenueData() }

    suspend fun getPrinterData(terminalId:Int) =
        getResult { apiService.getPrinterList(terminalId) }

    suspend fun createPrinter(data: CreatePrinterRequestModel) = getResult {
        apiService.createPrinter(data)
    }

    suspend fun deletePrinter(id: Int, type: String? = null) =
        getResult { apiService.deletePrinter(id, type) }

    suspend fun deleteQueuePrinter(id: Int) = getResult { apiService.deletePrinterQueue(id) }

    suspend fun deleteAllQueuePrinter(id: Array<Int>) =
        getResult { apiService.deleteAllPrinterQueue(id) }

    suspend fun updatePrinter(id: Int, model: CreatePrinterRequestModel) =
        getResult { apiService.updatePrinter(id, model) }

    suspend fun updatePrinterStatus(id: Int, terminal_id: Int, status: Boolean) =
        getResult { apiService.updatePrinterStatus(id, terminal_id, status) }

    suspend fun updateServiceChargeEnable(id: Int, enable_service_charge: Boolean) =
        getResult { apiService.updateServiceChargeTakeoutEnable(id, enable_service_charge) }


    suspend fun updateServiceChargeDininEnable(id: Int, enable_service_charge: Boolean) =
        getResult { apiService.updateServiceChargeDineinEnable(id, enable_service_charge) }

    suspend fun syncVenueDetails(terminalId:Int) =
        getResult { apiService.syncVenueDetails(terminalId) }

    suspend fun getOnlineOrderCountNoti() =
        getResult { apiService.getCountOnlineOrdering() }

    suspend fun employeesList(locationId: Int) =
        getResult { apiService.employeesList(locationId) }

    suspend fun employeesTimeSheet(startDate: String, endDate: String, teamRoleId: String) =
        getResult { apiService.employeesTimeSheet(startDate, endDate, teamRoleId) }

    suspend fun employeesTimeSheetDetails(startDate: String, endDate: String, teamId: String) =
        getResult { apiService.employeesTimeSheetDetails(startDate, endDate, teamId) }

    suspend fun customerList() =
        getResult { apiService.customerList() }


    suspend fun customerListPagination(data: HashMap<String, String>) =
        getResult { apiService.customerListPagination(data) }

    suspend fun getTaxList() =
        getResult { apiService.getTaxList() }

    suspend fun createTax(data: CreateTaxRequestModel) =
        getResult { apiService.createTax(data) }

    suspend fun updateTax(taxId: Int, data: CreateTaxRequestModel) =
        getResult { apiService.updateTax(taxId, data) }

    suspend fun taxActive(taxId: Int, active: Boolean) =
        getResult { apiService.taxActive(taxId, active) }

    suspend fun deleteTax(data: Int) =
        getResult { apiService.deleteTax(data) }

    suspend fun getTipsList() =
        getResult { apiService.getTipsList() }

    suspend fun createTips(data: CreateTipRequestModel) =
        getResult { apiService.createTips(data) }

    suspend fun updateTip(tipId: Int, data: CreateTipRequestModel) =
        getResult { apiService.updateTip(tipId, data) }

    suspend fun tipActive(tipId: Int, active: Boolean) =
        getResult { apiService.tipActive(tipId, active) }

    suspend fun deleteTip(tipId: Int) =
        getResult { apiService.deleteTip(tipId) }

    suspend fun getDiscountsList() =
        getResult { apiService.getDiscountsList() }

    suspend fun createDiscount(data: CreateDiscountRequestModel) =
        getResult { apiService.createDiscount(data) }

    suspend fun updateDiscount(discountId: Int, data: CreateDiscountRequestModel) =
        getResult { apiService.updateDiscount(discountId, data) }

    suspend fun discountActive(discountId: Int, active: Boolean) =
        getResult { apiService.discountActive(discountId, active) }

    suspend fun deleteDiscount(discountId: Int) =
        getResult { apiService.deleteDiscount(discountId) }

    suspend fun deleteCustomer(customerId: Int?) =
        getResult { apiService.deleteCustomer(customerId) }

    suspend fun getServiceChargeList() =
        getResult { apiService.getServiceChargeList() }

    suspend fun getServiceChargeWholeList() =
        getResult { apiService.getServiceChargeWholeList() }

    suspend fun loyaltyPointList() =
        getResult { apiService.loyaltyPointList() }

    suspend fun createServiceCharge(data: CreateServiceChargeRequestModel) =
        getResult { apiService.createServiceCharge(data) }

    suspend fun updateServiceCharge(serviceChargeId: Int, data: CreateServiceChargeRequestModel) =
        getResult { apiService.updateServiceCharge(serviceChargeId, data) }

    suspend fun serChargeActive(serChargeId: Int, active: Boolean) =
        getResult { apiService.serviceChargeActive(serChargeId, active) }

    suspend fun loyaltyPointActive(serChargeId: Int, active: Boolean) =
        getResult { apiService.loyaltyPointActive(serChargeId, active) }

    suspend fun deleteServiceCharge(serviceChargeId: Int) =
        getResult { apiService.deleteServiceCharge(serviceChargeId) }

    suspend fun deleteLoyaltyPoint(serviceChargeId: Int) =
        getResult { apiService.deleteLoyaltyPoint(serviceChargeId) }

    suspend fun getTeamRoleList() =
        getResult { apiService.getTeamRoles() }

    suspend fun getTeamModules() =
        getResult { apiService.getTeamModules() }


    suspend fun createTeamRole(data: CreateTeamRoleRequestModel) =
        getResult { apiService.createTeamRoles(data) }

    suspend fun updateTeamRole(teamRoleId: Int, data: CreateTeamRoleRequestModel) =
        getResult { apiService.updateTeamRoles(teamRoleId, data) }

    suspend fun deleteTeamRole(teamRoleId: Int) =
        getResult { apiService.deleteTeamRole(teamRoleId) }

    suspend fun getTeamMemberTimeSheet(data: CreateTeamRoleRequestModel) =
        getResult { apiService.createTeamRoles(data) }

    suspend fun getNoteList() =
        getResult { apiService.getNoteList() }

    suspend fun deleteNote(data: Int) =
        getResult { apiService.deleteNote(data) }

    suspend fun createNote(data: CreateNoteRequest) =
        getResult { apiService.createNote(data) }

    suspend fun updateNote(taxId: Int, data: CreateNoteRequest) =
        getResult { apiService.updateNote(taxId, data) }

    suspend fun noteActive(tipId: Int, active: Boolean) =
        getResult { apiService.noteActive(tipId, active) }

    suspend fun logOut(data: HashMap<String, String>) = getResult {
        apiService.userLogOut(data)
    }

    suspend fun createEmployee(data: CreateEmployeeRequestModel) =
        getResult { apiService.createEmployee(data) }

    suspend fun createCustomer(data: CreateCustomerRequestModel) =
        getResult { apiService.createCustomer(data) }

    suspend fun updateCustomer(id: Int, data: CreateCustomerRequestModel) =
        getResult { apiService.updateCustomer(id, data) }

    suspend fun updateBusiness(id: Int, data: TbBusinessDetails) =
        getResult { apiService.updateBusiness(id, data) }

    suspend fun updateEmployee(taxId: Int, data: CreateEmployeeRequestModel) =
        getResult { apiService.updateEmployee(taxId, data) }

    suspend fun deleteEmployee(data: Int) =
        getResult { apiService.deleteEmployee(data) }

    suspend fun searchCustomers(query: String) =
        getResult { apiService.customerSearch(query) }

    suspend fun deleteItem(itemId: Int) =
        getResult { apiService.deleteItem(itemId) }

    suspend fun hideItem(itemId: Int, active: Boolean) =
        getResult { apiService.hideItem(itemId, active) }

    suspend fun createItem(data: CreateItemRequestModel) =
        getResult {
            val createItemRequestMap = MethodUtils.generateItemRequest(data)

            val taxIds = HashMap<String, List<String>>()
            data.taxIds?.let {
                taxIds["tax_ids"] = it
            }
            val modifierSetIds = HashMap<String, List<Int>>()
            data.modifierSetIds?.let {
                modifierSetIds["modifier_set_ids"] = it
            }

            val variationAttributes = HashMap<String, List<VariationsAttribute>>()
            data.variationsAttributes?.let {
                variationAttributes["variations_attributes"] = it
            }

            //file multipart
            Log.e("!_@_", "data.image:  ${data.image}")
            val filePart =
                MethodUtils.makeMultiPartBody(
                    fileUrl = data.image,
                    contentType = getContentType(data.image),
                    fileKeyName = "image"
                )

            apiService.createItem(
                file = filePart,
                request = createItemRequestMap,
                taxIds = taxIds,
                modifierIds = modifierSetIds,
                variationAttributes = variationAttributes
            )
        }

    suspend fun updateItem(id: Int, data: CreateItemRequestModel) =
        getResult {

            val createItemRequestMap = MethodUtils.generateItemRequest(data)

            val taxIds = HashMap<String, List<String>>()
            data.taxIds?.let {
                taxIds["tax_ids"] = it
            }
            val modifierSetIds = HashMap<String, List<Int>>()
            data.modifierSetIds?.let {
                modifierSetIds["modifier_set_ids"] = it
            }

            val variationAttributes = HashMap<String, List<VariationsAttribute>>()
            data.variationsAttributes?.let {
                variationAttributes["variations_attributes"] = it
            }

            //file multipart
            Log.e("!_@_", "data.image:  ${data.image}")
            val filePart =
                MethodUtils.makeMultiPartBody(
                    fileUrl = data.image,
                    contentType = getContentType(data.image),
                    fileKeyName = "image"
                )

            apiService.updateItem(
                id = id,
                file = filePart,
                request = createItemRequestMap,
                taxIds = taxIds,
                modifierIds = modifierSetIds,
                variationAttributes = variationAttributes
            )
        }

    suspend fun deleteCategoryCall(data: Int) =
        getResult { apiService.deleteCategoryCall(data) }

    suspend fun hideCategoryCall(id: Int, active: Boolean) =
        getResult { apiService.hideCategory(id, active) }

    suspend fun createCategoryCall(data: CreateCategoryRequestModel) =
        getResult {

            val createCategoryRequestMap = MethodUtils.generateCategoryRequest(data)

            val itemIds = HashMap<String, List<Int>>()
            data.item_ids?.let {
                itemIds["item_ids"] = it
            }

            //file multipart
            Log.e("!_@_", "data.image:  ${data.image}")
            val filePart =
                MethodUtils.makeMultiPartBody(
                    fileUrl = data.image,
                    contentType = getContentType(data.image),
                    fileKeyName = "image"
                )

            apiService.createCategory(
                file = filePart,
                request = createCategoryRequestMap,
                itemIds = itemIds
            )
        }

    suspend fun updateCategoryCall(id: Int, data: CreateCategoryRequestModel) =
        getResult {

            val createCategoryRequestMap = MethodUtils.generateCategoryRequest(data)

            val itemIds = HashMap<String, List<Int>>()
            data.item_ids?.let {
                itemIds["item_ids"] = it
            }

            //file multipart
            Log.e("!_@_", "data.image:  ${data.image}")
            val filePart =
                MethodUtils.makeMultiPartBody(
                    fileUrl = data.image,
                    contentType = getContentType(data.image),
                    fileKeyName = "image"
                )

            apiService.updateCategory(
                id = id,
                file = filePart,
                request = createCategoryRequestMap,
                itemIds = itemIds
            )
        }

    suspend fun getCategories() =
        getResult { apiService.getCategories() }

    suspend fun reOrderCategoryCall(id: Int, oldPos: Int, newPos: Int) =
        getResult { apiService.reOrderCategory(id, newPos, oldPos) }

    suspend fun getItemsCall() =
        getResult { apiService.getItems() }

    suspend fun reOrderItemCall(id: Int, oldPos: Int, newPos: Int) =
        getResult { apiService.reOrderItem(id, oldPos, newPos) }

    suspend fun reOrderNote(id: Int, oldPos: Int, newPos: Int) =
        getResult { apiService.reOrderNote(id, oldPos, newPos) }

    suspend fun reOrderTip(id: Int, oldPos: Int, newPos: Int) =
        getResult { apiService.reOrderTip(id, oldPos, newPos) }


    suspend fun deleteModifierSetCall(id: Int) =
        getResult { apiService.deleteModifierSetCall(id) }

    suspend fun getModifierSetCall() =
        getResult { apiService.getModifierSet() }

    suspend fun createModifierSet(data: CreateModifierRequest) =
        getResult { apiService.createModifierSet(data) }

    suspend fun updateModifierSets(mId: Int, data: CreateModifierRequest) =
        getResult { apiService.updateModifierSets(mId, data) }

    suspend fun reOrderModifierCall(id: Int, oldPos: Int, newPos: Int) =
        getResult { apiService.reOrderModifier(id, oldPos, newPos) }

    suspend fun getOptionSet() =
        getResult { apiService.getOptionSet() }

    suspend fun createOptionSet(data: CreateOptionRequestModel) =
        getResult { apiService.createOptionSet(data) }

    suspend fun updateOptionSet(mId: Int, data: CreateOptionRequestModel) =
        getResult { apiService.updateOptionSets(mId, data) }

    suspend fun deleteOptionSet(id: Int) =
        getResult { apiService.deleteOptionSet(id) }

    suspend fun reOrderOptionSet(id: Int, oldPos: Int, newPos: Int) =
        getResult { apiService.reOrderOptionSet(id, oldPos, newPos) }


    suspend fun createOrder(data: OrderRequestModel) =
        getResult { apiService.createOrder(data) }

    suspend fun splitByOrder(data: SpitByOrderRequestModel) =
        getResult { apiService.splitByOrder(data) }

    suspend fun updateOrder(orderId: Int?, data: OrderRequestModel) =
        getResult { orderId?.let { apiService.updateOrder(it, data) } }

    suspend fun orderDetailsById(orderId: Int) =
        getResult { apiService.orderDetailsById(orderId) }


    suspend fun paymentDetailsById(paymentId: Int) =
        getResult { apiService.orderPaymentDetailsById(paymentId) }

    suspend fun orderDetailsId(orderId: Int) =
        getResult { apiService.orderDetailsId(orderId) }

    suspend fun getKitchenReceiptSettings() =
        getResult { apiService.getKitchenReceiptSettings() }

    suspend fun getCustomerReceiptSettings() =
        getResult { apiService.getCustomerReceiptSettings() }

    suspend fun updateCustomerSettings(id: Int, model: UpdateCustomerReceiptRequestModel) =
        getResult { apiService.updateCustomerReceiptSettings(id, model) }

    suspend fun updateKitchenSettings(id: Int, model: UpdateKitchenReceiptRequestModel) =
        getResult { apiService.updateKitchenReceiptSettings(id, model) }

    suspend fun getTransactionList(data: HashMap<String, String>) =
        getResult { apiService.getTransactionList(data) }

    suspend fun refundPayment(data: RefundRequestModel) =
        getResult { apiService.refundPayment(data) }

    suspend fun refundPaymentOnline(data: RefundRequestModelOnlineOrder) =
        getResult { apiService.refundPaymentOnlineOrder(data) }

    suspend fun orderTypes() =
        getResult { apiService.orderTypes() }

    suspend fun emailReceipt(data: HashMap<String, String>) =
        getResult { apiService.emailReceipt(data) }

    suspend fun phoneReceipt(data: HashMap<String, String>) =
        getResult { apiService.phoneReceipt(data) }

    suspend fun assignCustomerOrder(
        orderId: Int,
        customerId: Int,
        newPos: Int,
        paymentId: Int,
        finalrewards: Int
    ) =
        getResult {
            apiService.assignCustomerOrder(
                orderId,
                customerId,
                newPos,
                paymentId,
                finalrewards
            )
        }

    suspend fun getOpenOrders(paymentStatus: String, startDate: String, endDate: String) =
        getResult { apiService.getOpenOrders(paymentStatus, startDate, endDate) }

    suspend fun getOnlineOrders(startDate: String, endDate: String, order_status: String) =
        getResult { apiService.getOnlineOrders(startDate, endDate, order_status) }

    suspend fun setAcceptedAndDeclineorder(
        time: Int,
        order_id: Int,
        isaccepted: Boolean,
        employee_id: Int,
        terminalid: Int
    ) =
        getResult {
            apiService.setAcceptedAndDeclineOrders(
                order_id,
                isaccepted,
                time,
                employee_id,
                terminalid
            )
        }

    suspend fun updateOnlineOrder(
        order_id: Int,
        order_status: String
    ) =
        getResult { apiService.updateOnlineOrders(order_id, order_status) }

    suspend fun getUpcomingOpenOrders() =
        getResult { apiService.getUpcomingOpenOrders(true) }

    suspend fun cashInOut(data: CashLogRequest) =
        getResult { apiService.cashInOut(data) }

    suspend fun getCashInOut(
        startDate: String,
        endDate: String,
        terminalId: String,
        s: String,
        s1: String
    ) =
        getResult { apiService.getCashInOut(startDate, endDate, terminalId, s, s1) }

    suspend fun orderUpdateTip(orderId: Int, customerId: Double) =
        getResult { apiService.orderUpdateTip(orderId, customerId) }

    suspend fun updateKitchenFireStatus(id: Int, isFired: Boolean, items: String) =
        getResult {
            apiService.updateKitchenFireStatus(id, isFired, items)
        }

    suspend fun getTableStatus(
        tableId: Int,
        empId: Int,
        terminalId: Int,
        status: String,
        clearTable: Boolean
    ) =
        getResult {
            apiService.getTableStatus(tableId, empId, terminalId, status, clearTable)
        }

    suspend fun mergeFloorTable(
        parentTableId: Int,
        childIds: String,
        orderModel: MergeTableRequest?,
        mergedOrderIds: String?,
        orderId: Int? = null
    ) =
        getResult {
            apiService.mergeFloorTable(
                parentTableId, childIds, orderId,
                mergedOrderIds, orderModel
            )
        }

    suspend fun unMergeTable(id: Int) = getResult { apiService.unMergeTable(id) }

    suspend fun orderCancel(id: Int, data: OrderCancelRequest) =
        getResult { apiService.cancelOrder(id, data) }

    suspend fun getFloorPlan(locationId: Int) =
        getResult { apiService.getFloorPlan(locationId) }

    suspend fun getFloorPlanTableDetails() = getResult { apiService.getFloorPlanTableDetails() }

    suspend fun payByGuest(id: Int, payAll: Boolean, model: GuestPaymentRequest) = getResult {
        apiService.payByGuest(id, payAll, model)
    }

    suspend fun getReportSummary(startDate: String, endDate: String, terminalId: String) =
        getResult {
            apiService.getReportSummary(startDate, endDate, terminalId)
        }

    suspend fun getReportEOD(
        startDate: String,
        endDate: String,
        terminalId: String,
        employee_id: String,
        email: String
    ) =
        getResult {
            apiService.getReportEOD(startDate, endDate, terminalId, employee_id, email)
        }

    suspend fun sendEmailTimeSheet(
        startDate: String,
        endDate: String,
        email: String,
        employee_id: String
    ) =
        getResult {
            apiService.sendEmailReportSummary(startDate, endDate, email, employee_id)
        }

    suspend fun getOrderHistory(id: String) = getResult {
        apiService.getCustomerOrderHistory(id)
    }

    suspend fun createLoyaltyPoint(loyaltyPointRequest: LoyaltyPointRequest) = getResult {
        apiService.createLoyaltyPoint(loyaltyPointRequest)
    }

    suspend fun editLoyaltyPoint(loyaltyPointRequest: LoyaltyPointRequest) = getResult {
        apiService.editLoyaltyPoint("${loyaltyPointRequest.id}", loyaltyPointRequest)
    }

    suspend fun createQueuePrinter(createQueuePrinterModel: CreateQueuePrinterRequestModel) =
        getResult {
            apiService.createQueuePrinter(createQueuePrinterModel)
        }

    suspend fun orderCounts(startDate: String?, endDate: String?) =
        getResult { apiService.orderCounts(startDate, endDate) }

    suspend fun inventoryCounts() =
        getResult { apiService.inventoryCounts() }

    suspend fun onlineOrderCounts(startDate: String?, endDate: String?) =
        getResult { apiService.onlineOrderCounts(startDate, endDate) }


    suspend fun timeDetails() =
        getResult { apiService.getTimeDetails() }

}