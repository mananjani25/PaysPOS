package com.android.pos.data.remote

import android.util.Log
import com.android.pos.data.entities.VariationsAttribute
import com.android.pos.data.model.requestModel.*
import com.android.pos.utils.FileUtils.getContentType
import com.android.pos.utils.MethodUtils
import javax.inject.Inject

class ApiHelper @Inject constructor(private val apiService: ApiService) : BaseDataSource() {

    suspend fun userLogIn(data: HashMap<String, String>) = getResult { apiService.userLogIn(data) }
    suspend fun getDefaultTerminal(uniq_id: String) =
        getResult { apiService.getDefaultTerminal(uniq_id) }

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

    suspend fun getPrinterData() =
        getResult { apiService.getPrinterList() }

    suspend fun createPrinter(data: CreatePrinterRequestModel) = getResult {
        apiService.createPrinter(data)
    }

    suspend fun deletePrinter(id: Int) = getResult { apiService.deletePrinter(id) }

    suspend fun updatePrinter(id: Int, model: CreatePrinterRequestModel) =
        getResult { apiService.updatePrinter(id, model) }

    suspend fun updatePrinterStatus(id: Int, terminal_id: Int, status: Boolean) =
        getResult { apiService.updatePrinterStatus(id, terminal_id, status) }

    suspend fun syncVenueDetails() =
        getResult { apiService.syncVenueDetails() }

    suspend fun employeesList(locationId: Int) =
        getResult { apiService.employeesList(locationId) }

    suspend fun employeesTimeSheet(startDate: String, endDate: String, teamRoleId: String) =
        getResult { apiService.employeesTimeSheet(startDate, endDate, teamRoleId) }

    suspend fun employeesTimeSheetDetails(startDate: String, endDate: String, teamId: String) =
        getResult { apiService.employeesTimeSheetDetails(startDate, endDate, teamId) }

    suspend fun customerList() =
        getResult { apiService.customerList() }

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

    suspend fun createServiceCharge(data: CreateServiceChargeRequestModel) =
        getResult { apiService.createServiceCharge(data) }

    suspend fun updateServiceCharge(serviceChargeId: Int, data: CreateServiceChargeRequestModel) =
        getResult { apiService.updateServiceCharge(serviceChargeId, data) }

    suspend fun serChargeActive(serChargeId: Int, active: Boolean) =
        getResult { apiService.serviceChargeActive(serChargeId, active) }

    suspend fun deleteServiceCharge(serviceChargeId: Int) =
        getResult { apiService.deleteServiceCharge(serviceChargeId) }

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

    suspend fun updateEmployee(taxId: Int, data: CreateEmployeeRequestModel) =
        getResult { apiService.updateEmployee(taxId, data) }

    suspend fun deleteEmployee(data: Int) =
        getResult { apiService.deleteEmployee(data) }

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
        getResult { apiService.reOrderCategory(id, oldPos, newPos) }

    suspend fun getItemsCall() =
        getResult { apiService.getItems() }

    suspend fun reOrderItemCall(id: Int, oldPos: Int, newPos: Int) =
        getResult { apiService.reOrderItem(id, oldPos, newPos) }

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

    suspend fun orderTypes() =
        getResult { apiService.orderTypes() }

    suspend fun emailReceipt(data: HashMap<String, String>) =
        getResult { apiService.emailReceipt(data) }

    suspend fun phoneReceipt(data: HashMap<String, String>) =
        getResult { apiService.phoneReceipt(data) }

    suspend fun assignCustomerOrder(orderId: Int, customerId: Int, newPos: Int) =
        getResult { apiService.assignCustomerOrder(orderId, customerId, newPos) }

    suspend fun getOpenOrders(param1: String) =
        getResult { apiService.getOpenOrders(param1) }

    suspend fun getUpcomingOpenOrders() =
        getResult { apiService.getUpcomingOpenOrders(true) }

    suspend fun cashInOut(data: CashLogRequest) =
        getResult { apiService.cashInOut(data) }

    suspend fun getCashInOut(startDate: String, endDate: String, terminalId: String) =
        getResult { apiService.getCashInOut(startDate, endDate, terminalId) }

    suspend fun orderUpdateTip(orderId: Int, customerId: Double) =
        getResult { apiService.orderUpdateTip(orderId, customerId) }

    suspend fun updateKitchenFireStatus(id: Int, isFired: Boolean, items: String) =
        getResult {
            apiService.updateKitchenFireStatus(id, isFired, items)
        }

    suspend fun getTableStatus(tableId: Int, empId: Int, terminalId: Int, status: String) =
        getResult {
            apiService.getTableStatus(tableId, empId, terminalId, status)
        }


    suspend fun orderCancel(id: Int, data: OrderCancelRequest) =
        getResult { apiService.cancelOrder(id, data) }

    suspend fun getFloorPlan(locationId: Int) =
        getResult { apiService.getFloorPlan(locationId) }

    suspend fun payByGuest(id: Int, payAll: Boolean, model: GuestPaymentRequest) = getResult {
        apiService.payByGuest(id, payAll, model)
    }

    suspend fun getReportSummary(startDate: String, endDate: String, terminalId: String) =
        getResult {
            apiService.getReportSummary(startDate, endDate, terminalId)
        }

    suspend fun getOrderHistory(id: String) = getResult {
        apiService.getCustomerOrderHistory(id)
    }
}