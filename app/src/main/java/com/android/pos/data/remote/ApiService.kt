package com.android.pos.data.remote


import com.android.pos.data.entities.VariationsAttribute
import com.android.pos.data.model.CustomerListResponse
import com.android.pos.data.model.requestModel.*
import com.android.pos.data.model.responseModel.*
import com.android.pos.data.model.responseModel.category.CategoriesResponse
import com.android.pos.data.model.responseModel.category.CreateCategoryResponse
import com.android.pos.data.model.responseModel.item.ItemResponseNew
import com.android.pos.data.model.responseModel.item.ItemsResponse
import com.android.pos.data.model.responseModel.report.ReportSummaryResponse
import com.android.pos.data.remote.Constants.CASH_EVENTS
import com.android.pos.data.remote.Constants.CATEGORY
import com.android.pos.data.remote.Constants.CATEGORY_UPDATE_DELETE
import com.android.pos.data.remote.Constants.CLOCK_OUT
import com.android.pos.data.remote.Constants.CUSTOMERS
import com.android.pos.data.remote.Constants.CUSTOMER_RECEIPTS_UPDATE_SETTINGS
import com.android.pos.data.remote.Constants.CUSTOMER_RECEIPT_SETTINGS
import com.android.pos.data.remote.Constants.CUSTOMER_UPDATE
import com.android.pos.data.remote.Constants.DELETE_UPDATE_PRINTER
import com.android.pos.data.remote.Constants.DISCOUNTS
import com.android.pos.data.remote.Constants.DISCOUNTS_ACTIVE
import com.android.pos.data.remote.Constants.DISCOUNTS_UPDATE_DELETE
import com.android.pos.data.remote.Constants.EMPLOYEES
import com.android.pos.data.remote.Constants.EMPLOYEES_TIMESHEET
import com.android.pos.data.remote.Constants.EMPLOYEES_TIMESHEET_DETAILS
import com.android.pos.data.remote.Constants.EMPLOYEES_UPDATE_DELETE
import com.android.pos.data.remote.Constants.EMPLOYEE_CLOCK_IN
import com.android.pos.data.remote.Constants.EMPLOYEE_LOG_IN
import com.android.pos.data.remote.Constants.FIRE_ITEM_TO_KITCHEN
import com.android.pos.data.remote.Constants.FLOOR_PLAN_STATUS
import com.android.pos.data.remote.Constants.FORGOT_PASSWORD
import com.android.pos.data.remote.Constants.GET_FLOOR_PLAN
import com.android.pos.data.remote.Constants.GET_PRINTERS
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
import com.android.pos.data.remote.Constants.OPEN_ORDERS
import com.android.pos.data.remote.Constants.OPTION_SETS
import com.android.pos.data.remote.Constants.OPTION_UPDATE_DELETE
import com.android.pos.data.remote.Constants.ORDERS
import com.android.pos.data.remote.Constants.ORDER_ASSIGN_CUSTOMER
import com.android.pos.data.remote.Constants.ORDER_DETAILS
import com.android.pos.data.remote.Constants.ORDER_EMAIL_RECEIPT
import com.android.pos.data.remote.Constants.ORDER_PHONE_RECEIPT
import com.android.pos.data.remote.Constants.ORDER_TYPES
import com.android.pos.data.remote.Constants.PAY_BY_GUEST
import com.android.pos.data.remote.Constants.REFUND_PAYMENT
import com.android.pos.data.remote.Constants.REORDER_CATEGORY
import com.android.pos.data.remote.Constants.REORDER_ITEM
import com.android.pos.data.remote.Constants.REORDER_MODIFIER
import com.android.pos.data.remote.Constants.REORDER_OPTION_SET
import com.android.pos.data.remote.Constants.REPORT_SUMMARY
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
import com.android.pos.data.remote.Constants.TRANSACTION_LIST
import com.android.pos.data.remote.Constants.UPDATE_PRINTER_STATUS
import com.android.pos.data.remote.Constants.UPDATE_TIP
import com.android.pos.data.remote.Constants.USERS_LOG_IN
import okhttp3.MultipartBody
import okhttp3.RequestBody
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

    @GET(GET_PRINTERS)
    suspend fun getPrinterList(): PrinterResponse

    @POST(GET_PRINTERS)
    suspend fun createPrinter(@Body createPrinter: CreatePrinterRequestModel): PrinterResponse

    @DELETE(DELETE_UPDATE_PRINTER)
    suspend fun deletePrinter(
        @Path("id") Id: Int,
    ): DeletePrinterResponseModel

    @PUT(DELETE_UPDATE_PRINTER)
    suspend fun updatePrinter(
        @Path("id") Id: Int,
        @Body model: CreatePrinterRequestModel
    ): DeletePrinterResponseModel


    @PUT(UPDATE_PRINTER_STATUS)
    suspend fun updatePrinterStatus(
        @Path("id") Id: Int,
        @Query("terminal_id") terminal_id: Int,
        @Query("status") status: Boolean
    ): DeletePrinterResponseModel

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
    suspend fun deleteCustomer(@Path("id") customerId: Int?): BaseResponse

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

    @Multipart
    @POST(ITEMS)
    suspend fun createItem(
        @Part file: MultipartBody.Part?,
        @PartMap() request: @JvmSuppressWildcards Map<String, RequestBody>,
        @PartMap() taxIds: @JvmSuppressWildcards Map<String, List<Int>>,
        @PartMap() modifierIds: @JvmSuppressWildcards Map<String, List<Int>>,
        @PartMap() variationAttributes: @JvmSuppressWildcards Map<String, List<VariationsAttribute>>
    ): ItemResponseNew

    @Multipart
    @PUT(ITEM_UPDATE_DELETE)
    suspend fun updateItem(
        @Path("id") id: Int,
        @Part file: MultipartBody.Part?,
        @PartMap() request: @JvmSuppressWildcards Map<String, RequestBody>,
        @PartMap() taxIds: @JvmSuppressWildcards Map<String, List<Int>>,
        @PartMap() modifierIds: @JvmSuppressWildcards Map<String, List<Int>>,
        @PartMap() variationAttributes: @JvmSuppressWildcards Map<String, List<VariationsAttribute>>
    ): ItemResponseNew

    /*@PUT(ITEM_UPDATE_DELETE)
    suspend fun updateItem(
        @Path("id") id: Int,
        @Body updateItem: CreateItemRequestModel
    ): ItemResponseNew*/

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

    @Multipart
    @POST(CATEGORY)
    suspend fun createCategory(
        @Part file: MultipartBody.Part?,
        @PartMap() request: @JvmSuppressWildcards Map<String, RequestBody>,
        @PartMap() itemIds: @JvmSuppressWildcards Map<String, List<Int>>,
    ): CreateCategoryResponse

    /* @POST(CATEGORY)
     suspend fun createCategory(@Body createItemRequestModel: CreateCategoryRequestModel): CreateCategoryResponse*/

    @Multipart
    @PUT(CATEGORY_UPDATE_DELETE)
    suspend fun updateCategory(
        @Path("id") id: Int,
        @Part file: MultipartBody.Part?,
        @PartMap() request: @JvmSuppressWildcards Map<String, RequestBody>,
        @PartMap() itemIds: @JvmSuppressWildcards Map<String, List<Int>>,
    ): CreateCategoryResponse

    /*@PUT(CATEGORY_UPDATE_DELETE)
    suspend fun updateCategory(
        @Path("id") id: Int,
        @Body updateItem: CreateCategoryRequestModel
    ): CreateCategoryResponse*/

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

    @GET(OPTION_SETS)
    suspend fun getOptionSet(): GetOptionSetResponse


    @POST(OPTION_SETS)
    suspend fun createOptionSet(@Body createModifierRequest: CreateOptionRequestModel): GetOptionSetResponse

    @PUT(OPTION_UPDATE_DELETE)
    suspend fun updateOptionSets(
        @Path("id") mId: Int,
        @Body createModifierRequest: CreateOptionRequestModel
    ): GetOptionSetResponse

    @DELETE(OPTION_UPDATE_DELETE)
    suspend fun deleteOptionSet(
        @Path("id") noteId: Int,
    ): BaseResponse

    @PUT(REORDER_OPTION_SET)
    suspend fun reOrderOptionSet(
        @Path("id") id: Int,
        @Query("old_position") old_position: Int,
        @Query("new_position") new_position: Int,
    ): BaseResponse

    @POST(ORDERS)
    suspend fun createOrder(@Body orderRequestModel: OrderRequestModel): CreateOrderResponse


    @PUT(ORDER_DETAILS)
    suspend fun updateOrder(
        @Path("id") orderId: Int,
        @Body orderRequestModel: OrderRequestModel
    ): CreateOrderResponse

    @GET(ORDER_DETAILS)
    suspend fun orderDetailsById(@Path("id") orderId: Int): GetOrderDetailsResponse

    @GET(ORDER_DETAILS)
    suspend fun orderDetailsId(@Path("id") orderId: Int): CreateOrderResponse


    @GET(KITCHEN_RECEIPT_SETTINGS)
    suspend fun getKitchenReceiptSettings(): GetKitchenReceiptSettingsResponse

    @PUT(KITCHEN_RECEIPT_UPDATE_SEETINGS)
    suspend fun updateKitchenReceiptSettings(
        @Path("id") id: Int,
        @Body requestModel: UpdateKitchenReceiptRequestModel
    ): GetKitchenReceiptSettingsResponse

    @GET(CUSTOMER_RECEIPT_SETTINGS)
    suspend fun getCustomerReceiptSettings(): GetCustomerReceiptSettingsResponse

    @GET(TRANSACTION_LIST)
    suspend fun getTransactionList(@QueryMap options: HashMap<String, String>): GetTransactionListResponse

    @POST(REFUND_PAYMENT)
    suspend fun refundPayment(@Body refundRequestModel: RefundRequestModel): BaseResponse

    @PUT(CUSTOMER_RECEIPTS_UPDATE_SETTINGS)
    suspend fun updateCustomerReceiptSettings(
        @Path("id") id: Int,
        @Body requestModel: UpdateCustomerReceiptRequestModel
    ): GetCustomerReceiptSettingsResponse


    @POST(ORDER_EMAIL_RECEIPT)
    suspend fun emailReceipt(@QueryMap options: HashMap<String, String>): BaseResponse


    @POST(ORDER_PHONE_RECEIPT)
    suspend fun phoneReceipt(@QueryMap options: HashMap<String, String>): BaseResponse


    @PUT(ORDER_ASSIGN_CUSTOMER)
    suspend fun assignCustomerOrder(
        @Path("id") orderId: Int,
        @Query("customer_id") customer_id: Int,
        @Query("customer_address_id") customer_address_id: Int,
    ): BaseResponse

    @GET(OPEN_ORDERS)
    suspend fun getOpenOrders(@Query("payment_status") payment_status: String): OpenOrderResponse

    @GET(OPEN_ORDERS)
    suspend fun getUpcomingOpenOrders(
        @Query("upcoming_orders") upcoming_orders: Boolean?,
    ): OpenOrderResponse


    @POST(CASH_EVENTS)
    suspend fun cashInOut(@Body cashLogRequest: CashLogRequest): BaseResponse

    @GET(CASH_EVENTS)
    suspend fun getCashInOut(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("terminal_id") terminalId: String,

        ): CashLogResponse

    @PUT(UPDATE_TIP)
    suspend fun orderUpdateTip(
        @Path("id") id: Int,
        @Query("tips") old_position: Double
    ): BaseResponse


    @PUT(FIRE_ITEM_TO_KITCHEN)
    suspend fun updateKitchenFireStatus(
        @Path("id") id: Int,
        @Query("is_fired") is_fired: Boolean,
        @Query("order_item_ids") order_item_ids: String
    ): BaseResponse


    @PUT(FLOOR_PLAN_STATUS)
    suspend fun getTableStatus(
        @Path("id") tableId: Int,
        @Query("employee_id") employee_id: Int,
        @Query("terminal_id") terminal_id: Int,
        @Query("status") status: String
    ):BaseResponse

    @POST(PAY_BY_GUEST)
    suspend fun payByGuest(
        @Query("id") id: Int,
        @Query("completed_all_payments") completed_all_payments: Boolean,
        @Body guestPaymentRequest: GuestPaymentRequest
    ): BaseResponse


    @PUT(ORDER_DETAILS)
    suspend fun cancelOrder(
        @Path("id") id: Int,
        @Body updateItem: OrderCancelRequest
    ): BaseResponse

    @GET(GET_FLOOR_PLAN)
    suspend fun getFloorPlan(@Query("location_id") location_id: Int): GetFloorPlanResponse

    @GET(REPORT_SUMMARY)
    suspend fun getReportSummary(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String
    ): ReportSummaryResponse
}