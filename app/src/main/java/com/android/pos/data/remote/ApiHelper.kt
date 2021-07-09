package com.android.pos.data.remote

import javax.inject.Inject

class ApiHelper @Inject constructor(private val apiService: ApiServie): BaseDataSource() {

    suspend fun userLogIn(data: HashMap<String, String>) = getResult { apiService.userLogIn(data) }
}