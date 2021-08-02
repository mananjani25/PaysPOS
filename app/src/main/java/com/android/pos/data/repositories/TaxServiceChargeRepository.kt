package com.android.pos.data.repositories

import androidx.lifecycle.LiveData
import com.android.pos.data.db.AppDatabase
import com.android.pos.data.db.IDataManager
import com.android.pos.data.entities.TbCategory
import com.android.pos.data.model.requestModel.CreateServiceChargeRequestModel
import com.android.pos.data.model.requestModel.CreateTaxRequestModel
import com.android.pos.data.model.responseModel.GetServiceChargeResponse
import com.android.pos.data.model.responseModel.GetTaxResponse
import com.android.pos.data.remote.ApiHelper
import com.android.pos.utils.performGetOperation
import com.android.pos.utils.performGetOperationNew
import com.android.pos.utils.statusUtils.Resource
import javax.inject.Inject

class TaxServiceChargeRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val apiHelperNew: ApiHelper
) {

    fun getTaxList() =
        performGetOperation(
            databaseQuery = { appDatabase.taxDao().allTax },
            networkCall = { apiHelperNew.getTaxList() },
            saveCallResult = { appDatabase.taxDao().addAllTaxes(it.data) })

    suspend fun createTax(data: CreateTaxRequestModel) = apiHelperNew.createTax(data)

    suspend fun createTaxDatabase(data: GetTaxResponse.TaxData) =
        appDatabase.taxDao().addTax(data)

    suspend fun updateTax(taxId: Int, data: CreateTaxRequestModel) =
        apiHelperNew.updateTax(taxId, data)

    suspend fun taxActive(id: Int, active: Boolean) =
        apiHelperNew.taxActive(id, active)

    suspend fun taxActiveDatabase(taxId: Int, active: Boolean) =
        appDatabase.taxDao().activeTax(taxId, active)

    suspend fun deleteTax(data: Int) = apiHelperNew.deleteTax(data)

    suspend fun deleteTaxDatabase(taxId: Int) = appDatabase.taxDao().deleteTaxById(taxId)


    fun getServiceChargeList() =
        performGetOperation(
            databaseQuery = { appDatabase.serviceChargeDao().allServiceCharge },
            networkCall = { apiHelperNew.getServiceChargeList() },
            saveCallResult = { appDatabase.serviceChargeDao().addAllServiceCharge(it.data) })

    suspend fun createServiceCharge(data: CreateServiceChargeRequestModel) =
        apiHelperNew.createServiceCharge(data)

    suspend fun createServiceChargeDatabase(data: GetServiceChargeResponse.Data) =
        appDatabase.serviceChargeDao().addServiceCharge(data)

    suspend fun updateServiceCharge(discountId: Int, data: CreateServiceChargeRequestModel) =
        apiHelperNew.updateServiceCharge(discountId, data)

    suspend fun serChargeActive(id: Int, active: Boolean) =
        apiHelperNew.serChargeActive(id, active)

    suspend fun serChargeActiveDatabase(serChargeId: Int, active: Boolean) =
        appDatabase.serviceChargeDao().activeServiceCharge(serChargeId, active)

    suspend fun deleteServiceCharge(data: Int) = apiHelperNew.deleteServiceCharge(data)

    suspend fun deleteSerChargeDatabase(serChargeId: Int) =
        appDatabase.serviceChargeDao().deleteServiceChargeById(serChargeId)

}