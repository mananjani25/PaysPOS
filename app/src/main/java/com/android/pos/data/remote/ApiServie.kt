package com.android.pos.data.remote


import com.android.pos.data.model.responseModel.BaseResponse
import com.android.pos.data.model.responseModel.LogInResponse
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST


interface ApiServie {

    @FormUrlEncoded
    @POST("users/log_in")
    suspend fun userLogIn(@FieldMap options: HashMap<String, String>): LogInResponse


    @FormUrlEncoded
    @POST("employee_activities/clock_in")
    suspend fun employeeClockIn(@FieldMap options: HashMap<String, String>): BaseResponse
}