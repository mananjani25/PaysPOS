package com.android.pos.data.remote

import com.android.pos.data.entities.VariationsAttribute
import com.android.pos.data.model.requestModel.*
import com.android.pos.utils.ProgressRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
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
            //apiService.createItem(data)
            val active: RequestBody =
                data.active.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val categoryId: RequestBody =
                data.categoryId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val cost: RequestBody =
                data.cost.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val desc: RequestBody =
                data.desc.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val id: RequestBody = data.id.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val kitchenName: RequestBody =
                data.kitchenName.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val locationId: RequestBody =
                data.locationId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val name: RequestBody =
                data.name.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val price: RequestBody =
                data.price.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val priceType: RequestBody =
                data.priceType.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val productCode: RequestBody =
                data.productCode.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val quantity: RequestBody =
                data.quantity.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val sku: RequestBody =
                data.sku.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val taxIds = HashMap<String, List<Int>>()
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
            var filePart: MultipartBody.Part? = null
            if (data.image?.isNotEmpty() == true) {
                val file = File(data.image!!)
                val fileBody = ProgressRequestBody(File(data.image!!), "*/*", null)
                filePart = MultipartBody.Part.createFormData("image", file.name, fileBody)
            }


            apiService.createItemMultiPart(
                filePart,
                active = active,
                category_id = categoryId,
                cost = cost,
                desc = desc,
                id = id,
                kitchen_name = kitchenName,
                location_id = locationId,
                name = name,
                price = price,
                priceType = priceType,
                productCode = productCode,
                quantity = quantity,
                sku = sku,
                taxIds = taxIds,
                modifierIds = modifierSetIds,
                variationAttributes = variationAttributes
            )
        }

    suspend fun updateItem(id: Int, data: CreateItemRequestModel) =
        getResult { apiService.updateItem(id, data) }

    suspend fun deleteCategoryCall(data: Int) =
        getResult { apiService.deleteCategoryCall(data) }

    suspend fun hideCategoryCall(id: Int, active: Boolean) =
        getResult { apiService.hideCategory(id, active) }

    suspend fun createCategoryCall(data: CreateCategoryRequestModel) =
        getResult { apiService.createCategory(data) }

    suspend fun updateCategoryCall(id: Int, data: CreateCategoryRequestModel) =
        getResult { apiService.updateCategory(id, data) }

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

    suspend fun updateOrder(orderId: Int?, data: OrderRequestModel) =
        getResult { orderId?.let { apiService.updateOrder(it, data) } }

    suspend fun orderDetailsById(orderId: Int) =
        getResult { apiService.orderDetailsById(orderId) }

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

    suspend fun updateKitchenFireStatus(id: Int, isFired: Boolean, items: ArrayList<Int>) =
        getResult {
            apiService.updateKitchenFireStatus(id, isFired, items)
        }

    suspend fun orderCancel(id: Int, data: OrderCancelRequest) =
        getResult { apiService.cancelOrder(id, data) }

    suspend fun getFloorPlan(locationId: Int) =
        getResult { apiService.getFloorPlan(locationId) }
}