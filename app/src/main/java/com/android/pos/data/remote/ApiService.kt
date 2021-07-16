package com.android.pos.data.remote


import com.android.pos.data.model.requestModel.CreateTaxRequestModel
import com.android.pos.data.model.requestModel.CreateTipRequestModel
import com.android.pos.data.model.responseModel.*
import com.android.pos.data.remote.Constants.CLOCK_OUT
import com.android.pos.data.remote.Constants.EMPLOYEES
import com.android.pos.data.remote.Constants.EMPLOYEE_CLOCK_IN
import com.android.pos.data.remote.Constants.EMPLOYEE_LOG_IN
import com.android.pos.data.remote.Constants.LOGIN_TERMINAL
import com.android.pos.data.remote.Constants.SYNC_VENUE_DATA
import com.android.pos.data.remote.Constants.TAXES
import com.android.pos.data.remote.Constants.TAX_UPDATE_DELETE
import com.android.pos.data.remote.Constants.TIPS
import com.android.pos.data.remote.Constants.TIPS_UPDATE_DELETE
import com.android.pos.data.remote.Constants.USERS_LOG_IN
import retrofit2.http.*


interface ApiService {

    @FormUrlEncoded
    @POST(USERS_LOG_IN)
    suspend fun userLogIn(@FieldMap options: HashMap<String, String>): LogInResponse

    @GET(LOGIN_TERMINAL)
    suspend fun getDefaultTerminal(@Query("uniq_id") uniq_id: String?): TerminalResponse


    @FormUrlEncoded
    @POST(EMPLOYEE_CLOCK_IN)
    suspend fun employeeClockIn(@FieldMap options: HashMap<String, String>): BaseResponse

    @FormUrlEncoded
    @POST(EMPLOYEE_LOG_IN)
    suspend fun employeeLogIn(@FieldMap options: HashMap<String, String>): BaseResponse

    @FormUrlEncoded
    @POST(CLOCK_OUT)
    suspend fun employeeClockOut(@FieldMap options: HashMap<String, String>): BaseResponse

    @GET(SYNC_VENUE_DATA)
    suspend fun syncVenueData(): VenueDataResponse

    @GET(EMPLOYEES)
    suspend fun employeesList(): EmployeeResponse

    @GET(TAXES)
    suspend fun getTaxList(): GetTaxResponse

    @POST(TAXES)
    suspend fun createTax(@Body createTax: CreateTaxRequestModel): CreateTaxResponse

    @PUT(TAX_UPDATE_DELETE)
    suspend  fun updateTax(
        /* @Path("id") taxId: String?,*/
        @Body createTax: CreateTaxRequestModel
    ): CreateTaxResponse

    @DELETE(TAX_UPDATE_DELETE)
    suspend  fun deleteTax(
        @Path("id") taxId: Int,
    ): CreateTaxResponse

    @GET(TIPS)
    suspend fun getTipsList(): GetTipReponse

    @POST(TIPS)
    suspend fun createTips(@Body createTip: CreateTipRequestModel): CreateTipResponse

    @PUT(TIPS_UPDATE_DELETE)
    suspend fun updateTip(
        @Path("id") tipId: String?,
        @Body createTax: CreateTipRequestModel
    ): CreateTipResponse

    @DELETE(TIPS_UPDATE_DELETE)
    suspend fun deleteTip(
        @Path("id") tipId: String?
    ): BaseResponse
}