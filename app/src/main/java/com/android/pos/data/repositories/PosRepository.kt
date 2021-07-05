package com.android.pos.data.repositories


import com.android.pos.data.db.IDataManager
import com.android.pos.data.remote.ApiHelper


import javax.inject.Inject


class PosRepository @Inject constructor(
    /*private val appDatabase: AppDatabase,*/
    private val apiHelperNew: ApiHelper
) : IDataManager {

    suspend fun sendOtp(data: HashMap<String, String>) = apiHelperNew.sendOtp(data)
}