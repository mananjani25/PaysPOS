package com.android.pos.data.repositories

import androidx.lifecycle.LiveData
import com.android.pos.data.db.AppDatabase
import com.android.pos.data.db.IDataManager
import com.android.pos.data.entities.TbCategory
import com.android.pos.data.remote.ApiHelper
import com.android.pos.utils.statusUtils.Resource
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val apiHelperNew: ApiHelper
) {

    suspend fun userLogIn(data: HashMap<String, String>) = apiHelperNew.userLogIn(data)

    suspend fun getDefaultTerminal(uniq_id: String) =
        apiHelperNew.getDefaultTerminal(uniq_id)

    suspend fun employeeClockIn(data: HashMap<String, String>) = apiHelperNew.employeeClockIn(data)

    suspend fun employeeLogIn(data: HashMap<String, String>) = apiHelperNew.employeeLogIn(data)

    suspend fun employeeClockOut(data: HashMap<String, String>) =
        apiHelperNew.employeeClockOut(data)

    suspend fun forgotPassword(data: HashMap<String, String>) =
        apiHelperNew.forgotPassword(data)

    suspend fun logout(data: HashMap<String, String>) = apiHelperNew.logOut(data)


}