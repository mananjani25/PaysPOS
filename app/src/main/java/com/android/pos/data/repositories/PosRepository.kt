package com.android.pos.data.repositories


import com.android.pos.data.db.AppDatabase
import com.android.pos.data.db.IDataManager
import com.android.pos.data.model.requestModel.CreateTaxRequestModel
import com.android.pos.data.model.requestModel.CreateTipRequestModel
import com.android.pos.data.remote.ApiHelper
import com.android.pos.utils.performGetOperationNew
import javax.inject.Inject


class PosRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val apiHelperNew: ApiHelper
) : IDataManager {

    suspend fun userLogIn(data: HashMap<String, String>) = apiHelperNew.userLogIn(data)

    suspend fun getDefaultTerminal(uniq_id: String) =
        apiHelperNew.getDefaultTerminal(uniq_id)

    suspend fun employeeClockIn(data: HashMap<String, String>) = apiHelperNew.employeeClockIn(data)

    suspend fun employeeLogIn(data: HashMap<String, String>) = apiHelperNew.employeeLogIn(data)

    suspend fun employeeClockOut(data: HashMap<String, String>) =
        apiHelperNew.employeeClockOut(data)


    fun syncVenueData() =
        performGetOperationNew(networkCall = { apiHelperNew.syncVenueData() })

    fun employeesList() =
        performGetOperationNew(networkCall = { apiHelperNew.employeesList() })

    fun getTaxList() =
        performGetOperationNew(networkCall = { apiHelperNew.getTaxList() })

    suspend fun createTax(data: CreateTaxRequestModel) = apiHelperNew.createTax(data)

    suspend fun updateTax(taxId: Int, data: CreateTaxRequestModel) = apiHelperNew.updateTax(taxId,data)

    suspend fun deleteTax(data: Int) = apiHelperNew.deleteTax(data)

    fun getTipList() =
        performGetOperationNew(networkCall = { apiHelperNew.getTipsList() })

    suspend fun createTips(data: CreateTipRequestModel) = apiHelperNew.createTips(data)

    suspend fun updateTip(taxId: Int, data: CreateTipRequestModel) = apiHelperNew.updateTip(taxId,data)

    suspend fun deleteTip(data: Int) = apiHelperNew.deleteTip(data)


//    fun getCharacters() = performGetOperation(
//        databaseQuery = { appDatabase.characterDao().getAllCharacters() },
//        networkCall = { apiHelperNew.syncVenueData() },
//        saveCallResult = {
//            val mCategory = it.data.categories
//            appDatabase.characterDao().insertAll(it.results)
//        }
//    )

    override suspend fun abs() {

        appDatabase.characterDao().getCharacter(0)
    }
}

