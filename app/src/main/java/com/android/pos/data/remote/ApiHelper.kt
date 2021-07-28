package com.android.pos.data.remote

import com.android.pos.data.model.requestModel.*
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

    suspend fun employeesList(locationId: Int) =
        getResult { apiService.employeesList(locationId) }

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

    suspend fun getServiceChargeList() =
        getResult { apiService.getServiceChargeList() }

    suspend fun createServiceCharge(data: CreateServiceChargeRequestModel) =
        getResult { apiService.createServiceCharge(data) }

    suspend fun updateServiceCharge(serviceChargeId: Int, data: CreateServiceChargeRequestModel) =
        getResult { apiService.updateServiceCharge(serviceChargeId, data) }

    suspend fun deleteServiceCharge(serviceChargeId: Int) =
        getResult { apiService.deleteServiceCharge(serviceChargeId) }

    suspend fun getNoteList() =
        getResult { apiService.getNoteList() }

    suspend fun deleteNote(data: Int) =
        getResult { apiService.deleteNote(data) }

    suspend fun createNote(data: CreateNoteRequest) =
        getResult { apiService.createNote(data) }

    suspend fun updateNote(taxId: Int, data: CreateNoteRequest) =
        getResult { apiService.updateNote(taxId, data) }

    suspend fun logOut(data: HashMap<String, String>) = getResult {
        apiService.userLogOut(data)
    }

    suspend fun createEmployee(data: CreateEmployeeRequestModel) =
        getResult { apiService.createEmployee(data) }

    suspend fun updateEmployee(taxId: Int, data: CreateEmployeeRequestModel) =
        getResult { apiService.updateEmployee(taxId, data) }

    suspend fun deleteEmployee(data: Int) =
        getResult { apiService.deleteEmployee(data) }

    suspend fun deleteItem(itemId: Int) =
        getResult { apiService.deleteItem(itemId) }

    suspend fun hideItem(itemId: Int, data: HashMap<String, String>) =
        getResult { apiService.hideItem(data) }

    suspend fun createItem(data: CreateItemRequestModel) =
        getResult { apiService.createItem(data) }

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
}