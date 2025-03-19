package com.pays.pos.data.repositories

import com.pays.pos.data.db.AppDatabase
import com.pays.pos.data.entities.TbDiscount
import com.pays.pos.data.model.requestModel.CreateDiscountRequestModel
import com.pays.pos.data.model.requestModel.CreateTipRequestModel
import com.pays.pos.data.model.responseModel.GetTipReponse
import com.pays.pos.data.remote.ApiHelper
import com.pays.pos.utils.performGetOperation
import com.pays.pos.utils.performGetOperationDatabase
import javax.inject.Inject

class TipDiscountRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val apiHelperNew: ApiHelper
) {


    fun getTipList() = performGetOperation(
        databaseQuery = { appDatabase.tipDao().allTips },
        networkCall = { apiHelperNew.getTipsList() },
        saveCallResult = { appDatabase.tipDao().addAllTips(it.data) })

    fun getTipList1() = performGetOperationDatabase(
        databaseQuery = { appDatabase.tipDao().allTips },
    )

    fun getTipActiveList() = performGetOperationDatabase(
        databaseQuery = { appDatabase.tipDao().allTipsActive },
    )

    suspend fun deleteTipsFromDb() {
        appDatabase.tipDao().delete()
    }

    suspend fun allTipsList(): List<GetTipReponse.Data> {
        return appDatabase.tipDao().allTipsList()
    }

    suspend fun addTips(tipSettings: List<GetTipReponse.Data>) {
        appDatabase.tipDao().addAllTips(tipSettings)
    }

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

    fun allDiscountList() = performGetOperationDatabase {
        appDatabase.discountDao().allDiscount
    }

    fun discountList() = performGetOperationDatabase {
        appDatabase.discountDao().allActiveDiscount
    }

    suspend fun deleteDiscountsFromDb() {
        appDatabase.discountDao().delete()
    }

    suspend fun addDiscount(list: List<TbDiscount>) {
        appDatabase.discountDao().addDiscounts(list)
    }

    suspend fun createDiscount(data: CreateDiscountRequestModel) = apiHelperNew.createDiscount(data)

    suspend fun createDiscountDatabase(data: TbDiscount) =
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

    suspend fun reOrderTip(id: Int, oldPos: Int, newPos: Int) =
        apiHelperNew.reOrderTip(id, oldPos, newPos)


}