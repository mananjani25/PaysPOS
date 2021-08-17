package com.android.pos.data.remote


import com.android.pos.data.model.CustomerListResponse
import com.android.pos.data.model.requestModel.*
import com.android.pos.data.model.responseModel.*
import com.android.pos.data.remote.Constants.CATEGORY
import com.android.pos.data.remote.Constants.CATEGORY_UPDATE_DELETE
import com.android.pos.data.remote.Constants.CLOCK_OUT
import com.android.pos.data.remote.Constants.CUSTOMERS
import com.android.pos.data.remote.Constants.CUSTOMER_RECEIPT_SETTINGS
import com.android.pos.data.remote.Constants.CUSTOMER_UPDATE
import com.android.pos.data.remote.Constants.DISCOUNTS
import com.android.pos.data.remote.Constants.DISCOUNTS_ACTIVE
import com.android.pos.data.remote.Constants.DISCOUNTS_UPDATE_DELETE
import com.android.pos.data.remote.Constants.EMPLOYEES
import com.android.pos.data.remote.Constants.EMPLOYEES_TIMESHEET
import com.android.pos.data.remote.Constants.EMPLOYEES_TIMESHEET_DETAILS
import com.android.pos.data.remote.Constants.EMPLOYEES_UPDATE_DELETE
import com.android.pos.data.remote.Constants.EMPLOYEE_CLOCK_IN
import com.android.pos.data.remote.Constants.EMPLOYEE_LOG_IN
import com.android.pos.data.remote.Constants.FORGOT_PASSWORD
import com.android.pos.data.remote.Constants.GET_TEAM_MODULE
import com.android.pos.data.remote.Constants.HIDE_CATEGORY
import com.android.pos.data.remote.Constants.HIDE_ITEM
import com.android.pos.data.remote.Constants.ITEMS
import com.android.pos.data.remote.Constants.ITEM_UPDATE_DELETE
import com.android.pos.data.remote.Constants.KITCHEN_RECEIPT_SETTINGS
import com.android.pos.data.remote.Constants.KITCHEN_RECEIPT_UPDATE_SEETINGS
import com.android.pos.data.remote.Constants.LOGIN_TERMINAL
import com.android.pos.data.remote.Constants.LOGOUT
import com.android.pos.data.remote.Constants.MODIFIER
import com.android.pos.data.remote.Constants.MODIFIER_UPDATE_DELETE
import com.android.pos.data.remote.Constants.NOTES
import com.android.pos.data.remote.Constants.NOTES_ACTIVE
import com.android.pos.data.remote.Constants.NOTE_UPDATE_DELETE
import com.android.pos.data.remote.Constants.ORDERS
import com.android.pos.data.remote.Constants.ORDER_EMAIL_RECEIPT
import com.android.pos.data.remote.Constants.ORDER_PHONE_RECEIPT
import com.android.pos.data.remote.Constants.ORDER_TYPES
import com.android.pos.data.remote.Constants.REORDER_CATEGORY
import com.android.pos.data.remote.Constants.REORDER_ITEM
import com.android.pos.data.remote.Constants.REORDER_MODIFIER
import com.android.pos.data.remote.Constants.SERVICE_CHARGE
import com.android.pos.data.remote.Constants.SERVICE_CHARGE_ACTIVE
import com.android.pos.data.remote.Constants.SERVICE_CHARGE_UPDATE_DELETE
import com.android.pos.data.remote.Constants.SYNC_VENUE_DATA
import com.android.pos.data.remote.Constants.SYNC_VENUE_DETAILS
import com.android.pos.data.remote.Constants.TAXES
import com.android.pos.data.remote.Constants.TAX_ACTIVE
import com.android.pos.data.remote.Constants.TAX_UPDATE_DELETE
import com.android.pos.data.remote.Constants.TEAM_ROLES
import com.android.pos.data.remote.Constants.TEAM_ROLES_UPDATE_DELETE
import com.android.pos.data.remote.Constants.TIPS
import com.android.pos.data.remote.Constants.TIPS_ACTIVE
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
    suspend fun employeeClockIn(@FieldMap options: HashMap<String, String>): ClockInReponse

    @FormUrlEncoded
    @POST(EMPLOYEE_LOG_IN)
    suspend fun employeeLogIn(@FieldMap options: HashMap<String, String>): BaseResponse

    @FormUrlEncoded
    @POST(CLOCK_OUT)
    suspend fun employeeClockOut(@FieldMap options: HashMap<String, String>): BaseResponse

    @FormUrlEncoded
    @POST(FORGOT_PASSWORD)
    suspend fun forgotPassword(@FieldMap option: HashMap<String, String>): BaseResponse

    @FormUrlEncoded
    @POST(LOGOUT)
    suspend fun userLogOut(@FieldMap option: HashMap<String, String>): BaseResponse

    @GET(SYNC_VENUE_DATA)
    suspend fun syncVenueData(): VenueDataResponse

    @GET(SYNC_VENUE_DETAILS)
    suspend fun syncVenueDetails(): VenueDetailsResponse

    @GET(ORDER_TYPES)
    suspend fun orderTypes(): OrderTypeResponse

    /* @GET(EMPLOYEES)
     suspend fun employeesList(@Query("location_id") location_id: Int): EmployeeListResponse
 */
    @GET(CUSTOMERS)
    suspend fun customerList(): CustomerListResponse


    @GET(TAXES)
    suspend fun getTaxList(): GetTaxResponse

    @POST(TAXES)
    suspend fun createTax(@Body createTax: CreateTaxRequestModel): CreateTaxResponse

    @PUT(TAX_UPDATE_DELETE)
    suspend fun updateTax(
        @Path("id") taxId: Int,
        @Body createTax: CreateTaxRequestModel
    ): CreateTaxResponse

    @PUT(TAX_ACTIVE)
    suspend fun taxActive(
        @Path("id") taxId: Int,
        @Query("is_active") is_active: Boolean
    ): CreateTaxResponse

    @DELETE(TAX_UPDATE_DELETE)
    suspend fun deleteTax(
        @Path("id") taxId: Int,
    ): CreateTaxResponse

    @GET(TIPS)
    suspend fun getTipsList(): GetTipReponse

    @POST(TIPS)
    suspend fun createTips(@Body createTip: CreateTipRequestModel): CreateTipResponse

    @PUT(TIPS_UPDATE_DELETE)
    suspend fun updateTip(
        @Path("id") tipId: Int,
        @Body createTip: CreateTipRequestModel
    ): CreateTipResponse

    @PUT(TIPS_ACTIVE)
    suspend fun tipActive(
        @Path("id") taxId: Int,
        @Query("is_active") is_active: Boolean
    ): CreateTipResponse

    @DELETE(TIPS_UPDATE_DELETE)
    suspend fun deleteTip(
        @Path("id") tipId: Int
    ): CreateTipResponse

    @GET(DISCOUNTS)
    suspend fun getDiscountsList(): GetDiscountResponse

    @POST(DISCOUNTS)
    suspend fun createDiscount(@Body createDiscount: CreateDiscountRequestModel): CreateDiscountResponse

    @PUT(DISCOUNTS_UPDATE_DELETE)
    suspend fun updateDiscount(
        @Path("id") discountId: Int,
        @Body createDiscount: CreateDiscountRequestModel
    ): CreateDiscountResponse

    @PUT(DISCOUNTS_ACTIVE)
    suspend fun discountActive(
        @Path("id") taxId: Int,
        @Query("is_active") is_active: Boolean
    ): CreateDiscountResponse

    @DELETE(DISCOUNTS_UPDATE_DELETE)
    suspend fun deleteDiscount(
        @Path("id") discountId: Int
    ): CreateDiscountResponse

    @DELETE(CUSTOMER_UPDATE)
    suspend fun deleteCustomer(@Path("id") customerId: Int): BaseResponse

    @GET(SERVICE_CHARGE)
    suspend fun getServiceChargeList(): GetServiceChargeResponse

    @POST(SERVICE_CHARGE)
    suspend fun createServiceCharge(@Body createDiscount: CreateServiceChargeRequestModel): CreateServiceChargeResponse

    @PUT(SERVICE_CHARGE_UPDATE_DELETE)
    suspend fun updateServiceCharge(
        @Path("id") discountId: Int,
        @Body createDiscount: CreateServiceChargeRequestModel
    ): CreateServiceChargeResponse

    @PUT(SERVICE_CHARGE_ACTIVE)
    suspend fun serviceChargeActive(
        @Path("id") serviceChargeId: Int,
        @Query("is_enabled") is_active: Boolean
    ): CreateServiceChargeResponse


    @DELETE(SERVICE_CHARGE_UPDATE_DELETE)
    suspend fun deleteServiceCharge(
        @Path("id") discountId: Int
    ): CreateServiceChargeResponse


    @GET(NOTES)
    suspend fun getNoteList(): NoteResponse

    @DELETE(NOTE_UPDATE_DELETE)
    suspend fun deleteNote(
        @Path("id") noteId: Int,
    ): CreateNoteResponse

    @POST(NOTES)
    suspend fun createNote(@Body createTax: CreateNoteRequest): CreateNoteResponse

    @PUT(NOTE_UPDATE_DELETE)
    suspend fun updateNote(
        @Path("id") taxId: Int,
        @Body createTax: CreateNoteRequest
    ): CreateNoteResponse

    @PUT(NOTES_ACTIVE)
    suspend fun noteActive(
        @Path("id") noteId: Int,
        @Query("is_active") is_active: Boolean
    ): CreateNoteResponse

    @GET(TEAM_ROLES)
    suspend fun getTeamRoles(): GetUserPermissionListResponse

    @GET(GET_TEAM_MODULE)
    suspend fun getTeamModules(): GetTeamRoleModule

    @POST(TEAM_ROLES)
    suspend fun createTeamRoles(@Body createDiscount: CreateTeamRoleRequestModel): GetUserPermissionListResponse

    @PUT(TEAM_ROLES_UPDATE_DELETE)
    suspend fun updateTeamRoles(
        @Path("id") discountId: Int,
        @Body createDiscount: CreateTeamRoleRequestModel
    ): GetUserPermissionListResponse


    @DELETE(TEAM_ROLES_UPDATE_DELETE)
    suspend fun deleteTeamRole(
        @Path("id") discountId: Int
    ): BaseResponse


    @GET(EMPLOYEES)
    suspend fun employeesList(@Query("location_id") location_id: Int): EmployeeListResponse

    @GET(EMPLOYEES_TIMESHEET)
    suspend fun employeesTimeSheet(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("team_role_id") teamRoleId: String,

        ): GetEmployeesTimeSheetResponse

    @GET(EMPLOYEES_TIMESHEET_DETAILS)
    suspend fun employeesTimeSheetDetails(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("team_id") teamId: String,
    ): GetEmployeeTimeSheetDetailsResponse

    @POST(EMPLOYEES)
    suspend fun createEmployee(@Body createEmployeeRequestModel: CreateEmployeeRequestModel): CreateEmployeeResponse

    @POST(CUSTOMERS)
    suspend fun createCustomer(@Body createCustomerRequestModel: CreateCustomerRequestModel): CreateCustomerReponse

    @PUT(EMPLOYEES_UPDATE_DELETE)
    suspend fun updateEmployee(
        @Path("id") taxId: Int,
        @Body createEmployeeRequestModel: CreateEmployeeRequestModel
    ): CreateEmployeeResponse

    @DELETE(EMPLOYEES_UPDATE_DELETE)
    suspend fun deleteEmployee(
        @Path("id") noteId: Int,
    ): BaseResponse

    @DELETE(ITEM_UPDATE_DELETE)
    suspend fun deleteItem(
        @Path("id") noteId: Int,
    ): BaseResponse

    @FormUrlEncoded
    @POST(CLOCK_OUT)
    suspend fun hideItem(@FieldMap options: HashMap<String, String>): BaseResponse

    @POST(EMPLOYEES)
    suspend fun createItem(@Body createItemRequestModel: CreateItemRequestModel): BaseResponse

    @PUT(NOTE_UPDATE_DELETE)
    suspend fun updateItem(
        @Path("id") id: Int,
        @Body updateItem: CreateItemRequestModel
    ): BaseResponse

    @PUT(CUSTOMER_UPDATE)
    suspend fun updateCustomer(
        @Path("id") id: Int,
        @Body createCustomerRequestModel: CreateCustomerRequestModel
    ): CreateCustomerReponse

    @DELETE(CATEGORY_UPDATE_DELETE)
    suspend fun deleteCategoryCall(
        @Path("id") noteId: Int,
    ): BaseResponse

    @PUT(HIDE_CATEGORY)
    suspend fun hideCategory(
        @Path("id") id: Int,
        @Query("is_active") is_active: Boolean,
    ): BaseResponse

    @POST(CATEGORY)
    suspend fun createCategory(@Body createItemRequestModel: CreateCategoryRequestModel): CreateCategoryResponse

    @PUT(CATEGORY_UPDATE_DELETE)
    suspend fun updateCategory(
        @Path("id") id: Int,
        @Body updateItem: CreateCategoryRequestModel
    ): CreateCategoryResponse

    @GET(CATEGORY)
    suspend fun getCategories(): CategoriesResponse

    @PUT(REORDER_CATEGORY)
    suspend fun reOrderCategory(
        @Path("id") id: Int,
        @Query("old_position") old_position: Int,
        @Query("new_position") new_position: Int,
    ): BaseResponse

    @PUT(HIDE_ITEM)
    suspend fun hideItem(
        @Path("id") id: Int,
        @Query("is_active") is_active: Boolean,
    ): BaseResponse

    @GET(ITEMS)
    suspend fun getItems(): ItemsResponse


    @PUT(REORDER_ITEM)
    suspend fun reOrderItem(
        @Path("id") id: Int,
        @Query("old_position") old_position: Int,
        @Query("new_position") new_position: Int,
    ): BaseResponse

    @DELETE(MODIFIER_UPDATE_DELETE)
    suspend fun deleteModifierSetCall(
        @Path("id") noteId: Int,
    ): BaseResponse

    @GET(MODIFIER)
    suspend fun getModifierSet(): ModifierSetResponse


    @POST(MODIFIER)
    suspend fun createModifierSet(@Body createModifierRequest: CreateModifierRequest): CreateModifierSetResponse

    @PUT(MODIFIER_UPDATE_DELETE)
    suspend fun updateModifierSets(
        @Path("id") mId: Int,
        @Body createModifierRequest: CreateModifierRequest
    ): CreateModifierSetResponse

    @PUT(REORDER_MODIFIER)
    suspend fun reOrderModifier(
        @Path("id") id: Int,
        @Query("old_position") old_position: Int,
        @Query("new_position") new_position: Int,
    ): BaseResponse

    @POST(ORDERS)
    suspend fun createOrder(@Body orderRequestModel: OrderRequestModel): CreateOrderResponse

    @GET(KITCHEN_RECEIPT_SETTINGS)
    suspend fun getKitchenReceiptSettings(): GetKitchenReceiptSettingsResponse

    @PUT(KITCHEN_RECEIPT_UPDATE_SEETINGS)
    suspend fun updateKitchenReceiptSettings(
        @Path("id") id: Int,
        @Body requestModel: UpdateKitchenReceiptRequestModel
    ): GetKitchenReceiptSettingsResponse

    @GET(CUSTOMER_RECEIPT_SETTINGS)
    suspend fun getCustomerReceiptSettings(): GetCustomerReceiptSettingsResponse


    @POST(ORDER_EMAIL_RECEIPT)
    suspend fun emailReceipt(@QueryMap options: HashMap<String, String>): BaseResponse


    @POST(ORDER_PHONE_RECEIPT)
    suspend fun phoneReceipt(@QueryMap options: HashMap<String, String>): BaseResponse


}