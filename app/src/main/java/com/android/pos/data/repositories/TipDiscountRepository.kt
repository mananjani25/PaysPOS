package com.android.pos.data.repositories

import androidx.lifecycle.LiveData
import com.android.pos.data.db.AppDatabase
import com.android.pos.data.db.IDataManager
import com.android.pos.data.entities.TbCategory
import com.android.pos.data.model.requestModel.CreateDiscountRequestModel
import com.android.pos.data.model.requestModel.CreateTipRequestModel
import com.android.pos.data.model.responseModel.GetDiscountResponse
import com.android.pos.data.model.responseModel.GetTipReponse
import com.android.pos.data.remote.ApiHelper
import com.android.pos.utils.performGetOperation
import com.android.pos.utils.performGetOperationNew
import com.android.pos.utils.statusUtils.Resource
import javax.inject.Inject

class TipDiscountRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val apiHelperNew: ApiHelper
) {


    fun getTipList() = performGetOperation(
        databaseQuery = { appDatabase.tipDao().allTips },
        networkCall = { apiHelperNew.getTipsList() },
        saveCallResult = { appDatabase.tipDao().addAllTips(it.data) })

    suspend fun createTips(data: CreateTipRequestModel) = apiHelperNew.createTips(data)

    suspend fun createTipsDatabase(data: GetTipReponse.Data) =
        appDatabase.tipDao().addTips(data)

    suspend fun updateTip(taxId: Int, data: CreateTipRequestModel) =
        apiHelperNew.updateTip(taxId, data)

    suspend fun tipActive(id: Int, active: Boolean) =
        apiHelperNew.tipActive(id, active)

    suspend fun tipActiveDatabase(tipId: Int, active: Boolean) =
        appDatabase.tipDao().activeTip(tipId, active)

    suspend fun deleteTip(data: Int) = apiHelperNew.deleteTip(data)

    suspend fun deleteTipDatabase(tipId: Int) = appDatabase.tipDao().deleteTipById(tipId)

    fun getDiscountsList() = performGetOperation(
        databaseQuery = { appDatabase.discountDao().allDiscount },
        networkCall = { apiHelperNew.getDiscountsList() },
        saveCallResult = { appDatabase.discountDao().addAllDiscount(it.data) })

    suspend fun createDiscount(data: CreateDiscountRequestModel) = apiHelperNew.createDiscount(data)

    suspend fun createDiscountDatabase(data: GetDiscountResponse.Data) =
        appDatabase.discountDao().addDiscount(data)

    suspend fun updateDiscount(discountId: Int, data: CreateDiscountRequestModel) =
        apiHelperNew.updateDiscount(discountId, data)

    suspend fun discountActive(id: Int, active: Boolean) =
        apiHelperNew.discountActive(id, active)

    suspend fun discountActiveDatabase(discountId: Int, active: Boolean) =
        appDatabase.discountDao().activeDiscount(discountId, active)

    suspend fun deleteDiscount(data: Int) = apiHelperNew.deleteDiscount(data)

    suspend fun deleteDiscountDatabase(discountId: Int) =
        appDatabase.discountDao().deleteDiscountById(discountId)


}