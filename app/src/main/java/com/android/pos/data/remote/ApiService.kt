package com.android.pos.data.remote


import com.android.pos.data.model.responseModel.LogInResponse
import com.android.pos.data.remote.Constants.USERS_LOG_IN
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST


interface ApiService {

    @FormUrlEncoded
    @POST(USERS_LOG_IN)
    suspend fun userLogIn(@FieldMap options: HashMap<String, String>): LogInResponse

}