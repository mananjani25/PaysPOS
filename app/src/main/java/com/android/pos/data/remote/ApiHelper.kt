package com.android.pos.data.remote

import javax.inject.Inject

class ApiHelper @Inject constructor(private val apiService: ApiService): BaseDataSource() {

    suspend fun userLogIn(data: HashMap<String, String>) = getResult { apiService.userLogIn(data) }
    suspend fun getDefaultTerminal(data: HashMap<String, String>) =
        getResult { apiService.getDefaultTerminal(data) }

    suspend fun employeeClockIn(data: HashMap<String, String>) =
        getResult { apiService.employeeClockIn(data) }
}