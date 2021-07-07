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
    suspend fun sendOtp(data: HashMap<String, String>) = apiHelperNew.sendOtp(data)
    override suspend fun abs() {

        appDatabase.characterDao().getCharacter(0)
    }

}