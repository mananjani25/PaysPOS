package com.pays.pos.data.remote

import com.pays.pos.data.model.responseModel.PosLinkResult
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService2 {

    @GET(Constants.PAX_DETAILS)
    fun getPAXDetails(
        @Query("TerminalId") terminal_id: String?,
        @Query("SerialNo") serial_number: String,
        @Query("Token") token: String
    ): Call<PosLinkResult>

}