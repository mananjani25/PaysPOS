package com.android.pos.data.remote


import com.android.pos.data.model.responseModel.BaseResponse
import retrofit2.Call
import retrofit2.http.*


interface ApiServie {


    @FormUrlEncoded
    @POST("users/session/otp")
    suspend fun sendOTP(@FieldMap options: MutableMap<String, String>): BaseResponse

}