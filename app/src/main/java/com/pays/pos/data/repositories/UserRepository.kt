package com.pays.pos.data.repositories

import com.pays.pos.data.db.AppDatabase
import com.pays.pos.data.remote.ApiHelper
import com.pays.pos.data.remote.Constants
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val apiHelperNew: ApiHelper
) {

    suspend fun userLogIn(data: HashMap<String, String>) = apiHelperNew.userLogIn(data)

    suspend fun getDefaultTerminal(uniq_id: String,device_token:String) =
        apiHelperNew.getDefaultTerminal(uniq_id,device_token)

    suspend fun employeeClockIn(data: HashMap<String, String>) = apiHelperNew.employeeClockIn(data)

    suspend fun employeeLogIn(data: HashMap<String, String>) = apiHelperNew.employeeLogIn(data)

    suspend fun employeeClockOut(data: HashMap<String, String>) =
        apiHelperNew.employeeClockOut(data)

    suspend fun forgotPassword(data: HashMap<String, String>) =
        apiHelperNew.forgotPassword(data)



}