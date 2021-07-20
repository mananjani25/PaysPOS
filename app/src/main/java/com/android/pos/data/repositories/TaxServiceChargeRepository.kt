package com.android.pos.data.repositories

import com.android.pos.data.db.AppDatabase
import com.android.pos.data.db.IDataManager
import com.android.pos.data.model.requestModel.CreateServiceChargeRequestModel
import com.android.pos.data.model.requestModel.CreateTaxRequestModel
import com.android.pos.data.remote.ApiHelper
import com.android.pos.utils.performGetOperationNew
import javax.inject.Inject

class TaxServiceChargeRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val apiHelperNew: ApiHelper
) : IDataManager {

    fun getTaxList() =
        performGetOperationNew(networkCall = { apiHelperNew.getTaxList() })

    suspend fun createTax(data: CreateTaxRequestModel) = apiHelperNew.createTax(data)

    suspend fun updateTax(taxId: Int, data: CreateTaxRequestModel) =
        apiHelperNew.updateTax(taxId, data)

    suspend fun deleteTax(data: Int) = apiHelperNew.deleteTax(data)

    fun getServiceChargeList() =
        performGetOperationNew(networkCall = { apiHelperNew.getServiceChargeList() })

    suspend fun createServiceCharge(data: CreateServiceChargeRequestModel) =
        apiHelperNew.createServiceCharge(data)

    suspend fun updateServiceCharge(discountId: Int, data: CreateServiceChargeRequestModel) =
        apiHelperNew.updateServiceCharge(discountId, data)

    suspend fun deleteServiceCharge(data: Int) = apiHelperNew.deleteServiceCharge(data)

    override suspend fun abs() {
        TODO("Not yet implemented")
    }

}