package com.android.pos.data.repositories


import com.android.pos.data.db.AppDatabase
import com.android.pos.data.db.CharacterDao
import com.android.pos.data.db.IDataManager
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


    override suspend fun abs() {

        appDatabase.characterDao().getCharacter(0)
    }

}