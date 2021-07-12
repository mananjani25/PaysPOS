package com.android.pos.data.remote

import javax.inject.Inject

class ApiHelper @Inject constructor(private val apiService: ApiService) : BaseDataSource() {

    suspend fun userLogIn(data: HashMap<String, String>) = getResult { apiService.userLogIn(data) }
    suspend fun getDefaultTerminal(uniq_id: String) =
        getResult { apiService.getDefaultTerminal(uniq_id) }

    suspend fun employeeClockIn(data: HashMap<String, String>) =
        getResult { apiService.employeeClockIn(data) }

    suspend fun employeeLogIn(data: HashMap<String, String>) =
        getResult { apiService.employeeLogIn(data) }

    suspend fun employeeClockOut(data: HashMap<String, String>) =
        getResult { apiService.employeeClockOut(data) }
}