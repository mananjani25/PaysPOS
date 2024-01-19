package com.pays.pos.data.remote


import com.pays.pos.ui.fragments.magtek.PaymentResponse
import com.google.gson.JsonArray
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST


interface ApiService1 {

    @POST("ProcessCardSwipe")
    fun processCardSwipe(@Body data: JsonArray): Call<PaymentResponse>

    @POST("processData")
    fun processData(@Body data: JsonArray): Call<PaymentResponse>

    @POST("ProcessToken")
    fun processToken(@Body data: JsonArray): Call<PaymentResponse>

    @POST("ProcessReferenceID")
    fun processReferenceID(@Body data: JsonArray): Call<PaymentResponse>

    @POST("ProcessManualEntry")
    fun processManualEntry(@Body data: JsonArray): Call<PaymentResponse>
}