package com.android.pos.data.repositories

import androidx.lifecycle.LiveData
import com.android.pos.data.db.AppDatabase
import com.android.pos.data.db.IDataManager
import com.android.pos.data.entities.TbCategory
import com.android.pos.data.model.requestModel.CreateDiscountRequestModel
import com.android.pos.data.model.requestModel.CreateTipRequestModel
import com.android.pos.data.remote.ApiHelper
import com.android.pos.utils.performGetOperationNew
import com.android.pos.utils.statusUtils.Resource
import javax.inject.Inject

class TipDiscountRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val apiHelperNew: ApiHelper
) {


    fun getTipList() =
        performGetOperationNew(networkCall = { apiHelperNew.getTipsList() })

    suspend fun createTips(data: CreateTipRequestModel) = apiHelperNew.createTips(data)

    suspend fun updateTip(taxId: Int, data: CreateTipRequestModel) =
        apiHelperNew.updateTip(taxId, data)

    suspend fun deleteTip(data: Int) = apiHelperNew.deleteTip(data)


    fun getDiscountsList() =
        performGetOperationNew(networkCall = { apiHelperNew.getDiscountsList() })

    suspend fun createDiscount(data: CreateDiscountRequestModel) = apiHelperNew.createDiscount(data)

    suspend fun updateDiscount(discountId: Int, data: CreateDiscountRequestModel) =
        apiHelperNew.updateDiscount(discountId, data)

    suspend fun deleteDiscount(data: Int) = apiHelperNew.deleteDiscount(data)


}