package com.android.pos.data.remote

import javax.inject.Inject

class ApiHelperNew @Inject constructor(private val apiService: ApiServieNew): BaseDataSource() {

    suspend fun getCharacters() = getResult{apiService.getAllCharacters()}

    suspend fun sendOtp(data: HashMap<String, String>) = getResult{apiService.sendOTP(data)}
}