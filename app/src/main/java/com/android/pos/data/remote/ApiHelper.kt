package com.android.pos.data.remote

import javax.inject.Inject

class ApiHelper @Inject constructor(private val apiService: ApiServie): BaseDataSource() {

    suspend fun sendOtp(data: HashMap<String, String>) = getResult{apiService.sendOTP(data)}
}