package com.android.pos.data.repositories


import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.pos.data.db.AppDatabase
import com.android.pos.data.db.IDataManager
import com.android.pos.data.entities.*
import com.android.pos.data.model.PrinterQueueModel
import com.android.pos.data.model.ShiftRportConfiguration
import com.android.pos.data.model.SplitDetailListModel
import com.android.pos.data.model.requestModel.CashInOutModel
import com.android.pos.data.model.requestModel.CashLogRequest
import com.android.pos.data.model.requestModel.CreateCategoryRequestModel
import com.android.pos.data.model.requestModel.CreateCustomerRequestModel
import com.android.pos.data.model.requestModel.CreateEmployeeRequestModel
import com.android.pos.data.model.requestModel.CreateItemRequestModel
import com.android.pos.data.model.requestModel.CreateModifierRequest
import com.android.pos.data.model.requestModel.CreateNoteRequest
import com.android.pos.data.model.requestModel.CreateOptionRequestModel
import com.android.pos.data.model.requestModel.CreatePrinterRequestModel
import com.android.pos.data.model.requestModel.CreateQueuePrinterRequestModel
import com.android.pos.data.model.requestModel.GuestPaymentRequest
import com.android.pos.data.model.requestModel.MergeTableRequest
import com.android.pos.data.model.requestModel.OrderCancelRequest
import com.android.pos.data.model.requestModel.OrderRequestModel
import com.android.pos.data.model.requestModel.RefundRequestModelOnlineOrder
import com.android.pos.data.model.requestModel.SpitByOrderRequestModel
import com.android.pos.data.model.requestModel.WastageItemRequest
import com.android.pos.data.model.requestModel.giftCard.request.GiftCardAddValueRequest
import com.android.pos.data.model.requestModel.giftCard.request.GiftCardCheckBalanceRequest
import com.android.pos.data.model.requestModel.giftCard.request.SellGiftCardRequestModel
import com.android.pos.data.model.responseModel.BaseResponse
import com.android.pos.data.model.responseModel.GetCustomerReceiptSettingsResponse
import com.android.pos.data.model.responseModel.GetKitchenReceiptSettingsResponse
import com.android.pos.data.model.responseModel.NoteResponse
import com.android.pos.data.model.responseModel.OnlineOrderResponseModel
import com.android.pos.data.model.responseModel.OnlineOrderStatusUpdateResponse
import com.android.pos.data.model.responseModel.OpenOrderResponse
import com.android.pos.data.model.responseModel.PrinterResponse
import com.android.pos.data.model.responseModel.VenueDataResponse
import com.android.pos.data.model.responseModel.VenueDetailsResponse
import com.android.pos.data.remote.ApiHelper
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.DINE_IN
import com.android.pos.data.remote.Constants.EMPLOYEE_ID
import com.android.pos.data.remote.Constants.LOCATION_ID
import com.android.pos.data.remote.Constants.SYNC_SETTING_TIME_STAMP
import com.android.pos.data.remote.Constants.TERMINAL_ID
import com.android.pos.di.PrefProvider
import com.android.pos.utils.LogUtil
import com.android.pos.utils.performGetOperation
import com.android.pos.utils.performGetOperationDatabase
import com.android.pos.utils.performGetOperationNew
import com.android.pos.utils.statusUtils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject


class PosRepository @Inject constructor(
    private val prefProvider: PrefProvider,
    private val appDatabase: AppDatabase,
    private val apiHelperNew: ApiHelper
) : IDataManager {

    fun getPrinterQueueData() = appDatabase.printerQueueDao().printerQueueList

    suspend fun addPrinterQueueData(list: PrinterQueueModel) =
        appDatabase.printerQueueDao().addPrinterQueueData(list)

    fun checkQueueExist(id: Int) =
        performGetOperationDatabase { appDatabase.printerQueueDao().checkQueueDataExist(id) }


    suspend fun getPrinterQueueQueryData(id: Int) = appDatabase.printerQueueDao().getQueueData(id)

    fun getCustomerReceiptSettings() = appDatabase.customerSettingsDao().getCustomerSettings

    fun getKitchenReceiptSettings() = appDatabase.kitchenSettingsDao().getKitchenSettings
    fun getTipsList() = appDatabase.tipDao().allTips

//    fun syncVenueData() =
//        performGetOperationNew(networkCall = { apiHelperNew.syncVenueData() })

    suspend fun deleteKitchenPrinter(id: Int) =
        appDatabase.printerDao().deleteKitchenPrinterById(id)

    suspend fun deleteCustomerPrinter(id: Int) =
        appDatabase.printerDao().deleteCustomerPrinterById(id)

    suspend fun updateKitchenPrinterStatus(status: Boolean, id: Int) =
        appDatabase.printerDao().updateKitchenStatus(status, id)

    suspend fun updateCustomerPrinterStatus(status: Boolean, id: Int) =
        appDatabase.printerDao().updateCustomerStatus(status, id)

    fun getPrinters() = performGetOperation(databaseQuery = {
        appDatabase.printerDao().customerPrintList
    },
        networkCall = { apiHelperNew.getPrinterData(prefProvider.getValueInt(TERMINAL_ID, 0)) },
        saveCallResult = {
            if (it.data.customerReceiptPrinters?.isEmpty() == true || it.data.customerReceiptPrinters?.size == 0) {
                appDatabase.printerDao().deleteCustomerPrinters()
            } else {
                appDatabase.printerDao().addCustomerPrinterList(it.data.customerReceiptPrinters)
            }
            if (it.data.kitchenReceiptPrinters?.isEmpty() == true || it.data.kitchenReceiptPrinters?.size == 0) {
                appDatabase.printerDao().deleteKitchenPrinters()

            } else {
                it.data.kitchenReceiptPrinters?.let { it1 ->
                    appDatabase.printerDao().addKitchenPrinterList(
                        it1
                    )
                }
            }
        }

    )

    fun getCancelOrderListDatabse() =
        performGetOperationDatabase(databaseQuery = { appDatabase.cancelOrderReasonDao().allCancelOrderReasons })

    suspend fun addCancelOrderReasonFromDb(cancelOrderReason: List<VenueDetailsResponse.Data.CancelOrderReason>) {
        appDatabase.cancelOrderReasonDao().addAllCancelOrderReasonsSuspend(cancelOrderReason)
    }

    fun getKitchenPrinters() = performGetOperationDatabase { appDatabase.printerDao().kitchenPrintList }

    suspend fun getKitchenPrintersList() = appDatabase.printerDao().getKitchenPrinterList()

    suspend fun addWastageReasonInDb(wastageReasonsList: List<VenueDetailsResponse.Data.WastageReason>) {
        appDatabase.wastageReasonsDao().addAllWastageReasons(wastageReasonsList)
    }

    fun getWastageReasonsListFromDb() =
        performGetOperationDatabase(databaseQuery = { appDatabase.wastageReasonsDao().allWastageReasons })

    fun getCustomerPrinters() =
        performGetOperationDatabase { appDatabase.printerDao().customerPrintList }

    fun getPrinterDataMerge(): MutableLiveData<PrinterResponse.Data> {
        var data = PrinterResponse.Data()
        //var list:LiveData<PrinterResponse.Data> = data
        data.customerReceiptPrinters = appDatabase.printerDao().customerPrintList.value
        data.kitchenReceiptPrinters = appDatabase.printerDao().kitchenPrintList.value


        val liveData = MutableLiveData<PrinterResponse.Data>()
        liveData.postValue(data)

        return liveData
    }

    suspend fun createPrinter(data: CreatePrinterRequestModel) = apiHelperNew.createPrinter(data)

    suspend fun checkPermissionRole(passcode: String) = apiHelperNew.checkPermissionRole(passcode)

    suspend fun createQueuePrinter(createQueuePrinterRequest: CreateQueuePrinterRequestModel) =
        apiHelperNew.createQueuePrinter(createQueuePrinterRequest)

    suspend fun deletePrinter(id: Int, status: String? = null) =
        apiHelperNew.deletePrinter(id, status)

    suspend fun textPaySplit(id: Int) =
        apiHelperNew.textPaySplit(id)

    suspend fun deleteQueuePrinter(id: Int) = apiHelperNew.deleteQueuePrinter(id)

    suspend fun deleteAllQueuePrinter(id: Array<Int>) = apiHelperNew.deleteAllQueuePrinter(id)

    suspend fun updatePrinter(id: Int, model: CreatePrinterRequestModel) =
        apiHelperNew.updatePrinter(id, model)

    suspend fun updatePrinterStatus(id: Int, terminal_id: Int, status: Boolean) =
        apiHelperNew.updatePrinterStatus(id, terminal_id, status)

    /*  suspend fun updatePrinterStatusKitchen(id: Int, terminal_id: Int, status: Boolean) =
          apiHelperNew.updatePrinterStatusKitchen(id, terminal_id, status)

      suspend fun updatePrinterStatusCustomer(id: Int, terminal_id: Int, status: Boolean) =
          apiHelperNew.updatePrinterStatusCustomer(id, terminal_id, status)*//*fun syncVenueDetails() =
        performGetOperationNew(networkCall = { apiHelperNew.syncVenueDetails() })*/

    suspend fun syncVenueDetails() = apiHelperNew.syncVenueDetails(
        prefProvider.getValueInt(
            TERMINAL_ID, 0
        ),
        prefProvider.getValue(SYNC_SETTING_TIME_STAMP, "")
    )

    suspend fun getOnlineOrderNotificationCount() = apiHelperNew.getOnlineOrderCountNoti()

    suspend fun syncInventory(terminalId: Int, timeStamp: String) =
        apiHelperNew.syncVenueData(terminalId, timeStamp)

    suspend fun clearPrinterQueue() = apiHelperNew.clearPrinterQueue(
        prefProvider.getValueInt(
            LOCATION_ID, 0
        )
    )


    suspend fun updateTransactionLockScreen(lock_screen_after_each_transaction: Boolean) =
        apiHelperNew.updateTransactionLockScreen(lock_screen_after_each_transaction)

    fun venueDataLocal() = performGetOperationDatabase(
        databaseQuery = { appDatabase.categoryDao().categoryWithInventory()!! },
    )

    suspend fun saveDatabase(response: VenueDataResponse) {
        appDatabase.customerDao().deleteCustomerTb()
//        appDatabase.categoryDao().delete()
//        appDatabase.itemDao().delete()
//        appDatabase.modifierSetDao().delete()
//        appDatabase.itemModifierSetsDao().delete()
//        appDatabase.optionSetDao().delete()

        val mData = response.data
        val mCategory = mData.categories
        val categoryModelList = ArrayList<TbCategory>()
        val inventoryModelList = ArrayList<TbItem>()
        val modifierSetList = ArrayList<ModifierSet>()
        val itemModifierSetList = ArrayList<ItemModifierSets>()
        mCategory.forEach { category ->
            val model = TbCategory().apply {
                createdAt = ""
                id = category.id
                active = category.active
                name = category.name
                sort = category.sort
                updatedAt = ""
                locationId = category.locationId
                item_ids = category.itemIds
                thumbImgUrl = category.thumbImgUrl
                originalImgUrl = category.originalImgUrl
                isDeleted = category.isDeleted
            }
            categoryModelList.add(model)

            category.items.forEach {

                val items = TbItem().convertToItem(it, category)

                it.modifierSets.forEach { modifierSets ->

                    val itemModifierSets = ItemModifierSets().apply {
                        itemId = it.id
                        modifierSetId = modifierSets.id!!
                        minRequired = modifierSets.min_required
                        maxAllowed = modifierSets.max_allowed
                        isDeleted = modifierSets.isDeleted
                    }

                    itemModifierSetList.add(itemModifierSets)
                }

                modifierSetList.addAll(it.modifierSets)

                inventoryModelList.add(items)
            }
        }

        appDatabase.categoryDao().addAll(categoryModelList)
        appDatabase.itemDao().addAllItem(inventoryModelList)
        appDatabase.modifierSetDao().addAll(modifierSetList)
        appDatabase.itemModifierSetsDao().addAll(itemModifierSetList)
        appDatabase.optionSetDao().addAll(mData.optionSets)
    }

    fun getCategoryListAll() =
        performGetOperationDatabase(databaseQuery = {
            appDatabase.categoryDao().allWithoutGiftCard()
        })

    fun getCategoryListIWCAll() = performGetOperationDatabase(databaseQuery = {
        appDatabase.categoryDao().allCatWithoutItem()
    })

    fun getCategoryList() =
        performGetOperation(databaseQuery = { appDatabase.categoryDao().all() },
            networkCall = { apiHelperNew.getCategories() },
            saveCallResult = {
                val categoryModelList = ArrayList<TbCategory>()
                it.data.forEach { category ->
                    val model = TbCategory().apply {
                        createdAt = category.createdAt.toString()
                        id = category.id
                        active = category.active
                        name = category.name ?: ""
                        sort = category.sort
                        updatedAt = category.updatedAt.toString()
                        locationId = category.locationId
                        item_ids = category.itemIds
                        thumbImgUrl = category.thumbImgUrl
                        originalImgUrl = category.originalImgUrl
                    }
                    categoryModelList.add(model)
                }
                appDatabase.categoryDao().addAll(categoryModelList)
            }
        )

    suspend fun deleteCategory(catId: Int) = appDatabase.categoryDao().deleteCategoryById(catId)

    suspend fun deleteAllCategories() = appDatabase.categoryDao().delete()

    suspend fun hideCategory(catId: Int, active: Boolean) =
        appDatabase.categoryDao().hideCategory(catId, active)


    fun getItemsList() =
        performGetOperationDatabase(databaseQuery = { appDatabase.itemDao().allItem!! })

    fun getWholeItemFromPos() =
        performGetOperationDatabase(databaseQuery = { appDatabase.itemDao().allItemFromPos!! })

    fun getWholeItemsWithManualFromPos() =
        performGetOperationDatabase(databaseQuery = { appDatabase.itemDao().allItemsWithManualFromPos!! })

    fun getTimeZones() =
        performGetOperationDatabase(databaseQuery = { appDatabase.timeZonesDao().allItem })

    fun getBusinessData() =
        performGetOperationDatabase(databaseQuery = { appDatabase.businessDetailsDao().allData })

    fun getItemsbyId(itemId: Int) =
        performGetOperationDatabase(databaseQuery = { appDatabase.itemDao().itemById(itemId)!! })


    suspend fun updateTaxDataForItem(taxes: List<TaxData>, itemId: Int?) {
        appDatabase.itemDao().updateItemTaxes(itemId!!, taxes)
    }

    fun getItemByProductCode(productCode: String) =
        performGetOperationDatabase(databaseQuery = {
            appDatabase.itemDao().itemByProductCode(productCode)!!
        })

    fun checkCategoryHideOrNot(id: Int) = performGetOperationDatabase(databaseQuery = {
        appDatabase.categoryDao().getCategory(id)
    })

    fun getItemByCategoryId(id: Int) =
        performGetOperationDatabase(databaseQuery = {
            appDatabase.itemDao().getItemList(id)
        })

    fun getSingleItem(id: Int) = appDatabase.itemDao().itemOne(id)

    fun getItemList() = appDatabase.cartDao().allItemMod(prefProvider.getValueInt(EMPLOYEE_ID, 0))

    fun getSingleModifier(id: Int) = appDatabase.modifierSetDao().itemOne(id)

    fun updateModifier(mod: ModifierSet) = appDatabase.modifierSetDao().update(mod)


    fun modifierSetsList() =
        performGetOperationDatabase(databaseQuery = { appDatabase.modifierSetDao().all })

    fun updateModSet(mod: ModifierSet) = appDatabase.modifierSetDao().update(mod)

    fun getAllCountryList() = appDatabase.countryListDao().all

    fun modifierSets() =
        performGetOperation(databaseQuery = { appDatabase.modifierSetDao().all },
            networkCall = { apiHelperNew.getModifierSetCall() },
            saveCallResult = {
                appDatabase.modifierSetDao().addAll(it.data)
            })


    fun getInventory() =
        performGetOperationDatabase(
            databaseQuery = { appDatabase.itemDao().allItem!! }
        )


    fun getNoteList() = performGetOperationDatabase(
        databaseQuery = { appDatabase.notesDao().alllNotes },
    )

    fun taxListActive() = performGetOperationDatabase(
        databaseQuery = { appDatabase.notesDao().taxListActive },
    )


    suspend fun deleteNotesFromDb() =
        appDatabase.notesDao().delete()

    suspend fun deleteEODReportSettings() =
        appDatabase.eodReportSettings().deleteEODReportSettings()

    fun getEodReportSettings() =
        performGetOperationDatabase(databaseQuery = { appDatabase.eodReportSettings().eodSettingsData })

    suspend fun addAllNotesDatabase(data: List<NoteResponse.Data>) =
        appDatabase.notesDao().addAllNotesSuspend(data)

    suspend fun deleteKitchenReceiptSettingsFromDb() {
        appDatabase.kitchenSettingsDao().delete()
    }

    suspend fun addKitchenReceiptSettings(data: GetKitchenReceiptSettingsResponse.Data) {
        appDatabase.kitchenSettingsDao().add(data)
    }

    suspend fun deleteLoyaltyProgramFromDb() {
        appDatabase.loyaltyProgramsDao().delete()
    }

    suspend fun addLoyaltyProgramFromDb(data: List<LoyaltyProgramsModel>) {
        appDatabase.loyaltyProgramsDao().addAll(data)
    }

    fun getActiveLoyaltyProgramFromDb() = performGetOperationDatabase(databaseQuery = {
        appDatabase.loyaltyProgramsDao().findActiveLoyalty(active = true)
    })

    suspend fun deleteTeamRoleFromDb() {
        appDatabase.teamRoleDao().delete()
    }

    suspend fun deleteOrderTypeFromDb() {
        appDatabase.orderTypeDao().delete()
    }

    suspend fun deleteSurcharge() {
        appDatabase.cashDiscountDao().delete()
    }

    suspend fun addTeamRoleFromDb(teamRoleList: List<TeamRole>) {
        appDatabase.teamRoleDao().addAllRolesSuspend(teamRoleList)
    }

    suspend fun employeeListAddAllFromSeeting(employeelist: List<Employee>) {
        appDatabase.employeeDao().addAllEmployeeSuspend(employeelist)
    }

    suspend fun addOrderType(OrderTypeList: List<TbOrderType>) {
        appDatabase.orderTypeDao().addAll(OrderTypeList)
    }

    suspend fun addAllCountryList(countryList: List<TbCountryList>) {
        appDatabase.countryListDao().addAll(countryList)
    }

    suspend fun addTimeZones(countryList: List<TbTimeZones>) {
        appDatabase.timeZonesDao().addAll(countryList)
    }

    suspend fun addBusinessDetails(countryList: TbBusinessDetails) {
        appDatabase.businessDetailsDao().add(countryList)
    }


    fun getCurrentUserTeamRoleFromDb() = performGetOperationDatabase(databaseQuery = {
        appDatabase.teamRoleDao().roleById(id = prefProvider.getEmployeeRoleId())
    })

    suspend fun addCashDiscountsFromDb(data: List<CashDiscountModel>) {
        appDatabase.cashDiscountDao().addAll(data)
    }

    suspend fun addEODReportSettings(data: ShiftRportConfiguration) {
        appDatabase.eodReportSettings().addEODReportSettings(data)
    }

    suspend fun deleteCustomerReceiptSettingsFromDb() {
        appDatabase.customerSettingsDao().delete()
    }

    suspend fun deleteSplitDb() {
        appDatabase.splitDao().delete()
    }

    suspend fun addCustomerReceiptSettings(data: GetCustomerReceiptSettingsResponse.Data) {
        appDatabase.customerSettingsDao().add(data)
    }

    suspend fun addSplitAmount(model: SplitDetailListModel) {
        appDatabase.splitDao().addSplit(model)
    }

    suspend fun createNote(data: CreateNoteRequest) = apiHelperNew.createNote(data)

    suspend fun createNoteDatabase(data: NoteResponse.Data) =
        appDatabase.notesDao().addNotes(data)

    suspend fun updateNote(taxId: Int, data: CreateNoteRequest) =
        apiHelperNew.updateNote(taxId, data)

    suspend fun noteActive(id: Int, active: Boolean) =
        apiHelperNew.noteActive(id, active)

    suspend fun noteActiveDatabase(noteId: Int, active: Boolean) =
        appDatabase.notesDao().activeNote(noteId, active)

    suspend fun deleteNote(data: Int) = apiHelperNew.deleteNote(data)

    suspend fun deleteNoteDatabase(noteId: Int) = appDatabase.notesDao().deleteNotesById(noteId)

    /*fun employeesList(locationId: Int) =
        performGetOperationNew(networkCall = { apiHelperNew.employeesList(locationId) })*/

    fun employeesList(locationId: Int) = performGetOperation(
        databaseQuery = { appDatabase.employeeDao().allEmployee },
        networkCall = { apiHelperNew.employeesList(locationId) },
        saveCallResult = { appDatabase.employeeDao().addAllEmployee(it.data.employees) })

    fun getEmployeeListDatabse() =
        performGetOperationDatabase(databaseQuery = { appDatabase.employeeDao().allEmployee })

    fun getEmployeeListLocationWiseDatabse(locationId: Int) =
        performGetOperationDatabase(databaseQuery = {
            appDatabase.employeeDao().allEmployeeLocationWise(locationId)
        })

    fun getORderTypesListDatabase() =
        performGetOperationDatabase(databaseQuery = { appDatabase.orderTypeDao().orderTypes })


    /*fun employeesTimeSheet(startDate: String, endDate: String, teamRoleId: String) =
        performGetOperationNew(networkCall = { apiHelperNew.employeesTimeSheet(startDate, endDate, teamRoleId) })*/

    suspend fun employeesTimeSheet(startDate: String, endDate: String, teamRoleId: String) =
        apiHelperNew.employeesTimeSheet(startDate, endDate, teamRoleId)

    suspend fun employeesTimeSheetDetails(startDate: String, endDate: String, teamRoleId: String) =
        apiHelperNew.employeesTimeSheetDetails(startDate, endDate, teamRoleId)

    /*fun employeesTimeSheetDetails(startDate: String, endDate: String, teamId: String) =
        performGetOperationNew(networkCall = { apiHelperNew.employeesTimeSheetDetails(startDate, endDate, teamId) })*/


    fun customerList() = performGetOperation(
        databaseQuery = { appDatabase.customerDao().allCustomer },
        networkCall = { apiHelperNew.customerList() },
        saveCallResult = { appDatabase.customerDao().addAllCustomer(it.data) })

    fun customerListPagination(data: HashMap<String, String>) = performGetOperation(
        databaseQuery = { appDatabase.customerDao().allCustomer },
        networkCall = { apiHelperNew.customerListPagination(data) },
        saveCallResult = { appDatabase.customerDao().addAllCustomer(it.data) })


    fun orderTypes() = performGetOperation(
        databaseQuery = {
            appDatabase.orderTypeDao().orderTypes
        },
        networkCall = { apiHelperNew.orderTypes() },
        saveCallResult = { appDatabase.orderTypeDao().addAll(it.data) })

    fun orderTypesDb() = performGetOperationDatabase(
        databaseQuery = {
            appDatabase.orderTypeDao().orderTypes
        })

    fun orderTypesfromDatabase() = performGetOperationDatabase(
        databaseQuery = {
            appDatabase.orderTypeDao().orderTypes
        }
    )

    suspend fun createEmployee(data: CreateEmployeeRequestModel) = apiHelperNew.createEmployee(data)

    suspend fun createEmployeeDatabase(data: Employee) =
        appDatabase.employeeDao().addEmployee(data)

    suspend fun addCustomer(data: TbCustomer) =
        appDatabase.customerDao().addCustomer(data)

    suspend fun updateFinalRewards(finalrewards: Int, customerId: Int) =
        appDatabase.customerDao().updateLoyaltyRewards(finalrewards, customerId)

    fun getCustomerDetailsByID(id: Int?): LiveData<TbCustomer> {

        return appDatabase.customerDao().getCustomerDetailsByID(id)
    }


    suspend fun deleteCustomerDataBase(id: Int?) =
        appDatabase.customerDao().deleteCustomerByID(id)


    suspend fun createCustomer(data: CreateCustomerRequestModel) =
        apiHelperNew.createCustomer(data)

    suspend fun updateCustomer(id: Int, data: CreateCustomerRequestModel) =
        apiHelperNew.updateCustomer(id, data)

    suspend fun updateBusiness(id: Int, data: TbBusinessDetails) =
        apiHelperNew.updateBusiness(id, data)

    suspend fun deleteCustomer(id: Int?) = apiHelperNew.deleteCustomer(id)

    suspend fun updateEmployee(
        taxId: Int,
        data: CreateEmployeeRequestModel
    ) =
        apiHelperNew.updateEmployee(taxId, data)

    suspend fun deleteEmployee(data: Int) =
        apiHelperNew.deleteEmployee(data)


    suspend fun deleteAllEmployee() =
        appDatabase.employeeDao().delete()

    suspend fun searchCustomer(query: String) =
        apiHelperNew.searchCustomers(query)

    fun searchEmployeesDatabase(query: String) =
        performGetOperationDatabase(databaseQuery = {
            appDatabase.employeeDao().getEmployeeSearchResults(query)
        })

    suspend fun deleteEmployeeDatabase(employeeId: Int) =
        appDatabase.employeeDao().deleteEmployeeById(employeeId)


    suspend fun logout(data: HashMap<String, String>) =
        apiHelperNew.logOut(data, prefProvider.getValueInt(Constants.TERMINAL_ID, -1).toString())


    override suspend fun abs() {
        appDatabase.characterDao().getCharacter(0)
    }


    suspend fun deleteItem(itemId: Int) = apiHelperNew.deleteItem(itemId)

    suspend fun increaseOnGoingOrderCounter() = apiHelperNew.increaseOnGoingOrderCounter()
    suspend fun decreaseOnGoingOrderCounter() = apiHelperNew.decreaseOnGoingOrderCounter()

    suspend fun itemHide(itemId: Int, hide_status: String) =
        apiHelperNew.hideItem(itemId, hide_status)

    suspend fun hideItemWebsite(itemId: Int, hide_status: String) =
        apiHelperNew.hideItemWebsite(itemId, hide_status)

    fun unhideItemListPOS() =
        performGetOperationDatabase(databaseQuery = { appDatabase.itemDao().unhideItemPos!! })

    fun unhideItemListWebsite() =
        performGetOperationDatabase(databaseQuery = { appDatabase.itemDao().unhideItemWebsite!! })

    suspend fun createItem(item: TbItem) =
        appDatabase.itemDao().add(item)

    suspend fun createItemApiCall(data: CreateItemRequestModel) =
        apiHelperNew.createItem(data)

    suspend fun updateItemApiCall(id: Int, data: CreateItemRequestModel) =
        apiHelperNew.updateItem(id, data)

    suspend fun deleteCategoryCall(data: Int) =
        apiHelperNew.deleteCategoryCall(data)

    suspend fun hideCategoryCall(id: Int, active: Boolean) =
        apiHelperNew.hideCategoryCall(id, active)

    fun unhideCategoryList() =
        performGetOperationDatabase(databaseQuery = { appDatabase.categoryDao().unhideCategory })


    suspend fun createCategoryCall(data: CreateCategoryRequestModel) =
        apiHelperNew.createCategoryCall(data)

    suspend fun updateCategoryCall(id: Int, data: CreateCategoryRequestModel) =
        apiHelperNew.updateCategoryCall(id, data)

    suspend fun updateCategoryItems(id: Int, itemIdsList: List<Int>) =
        appDatabase.categoryDao().updateCategoryList(id, itemIdsList)

    suspend fun createCategory(category: TbCategory) =
        appDatabase.categoryDao().add(category)

    suspend fun updateItemCategory(
        catId: Int,
        catName: String,
        itemId: Int?
    ) {

        appDatabase.itemDao().updateItem(catId, catName, itemId)
    }

    suspend fun getItemsByCategory(id: Int): List<Int?>? {
        return appDatabase.itemDao().getListByCategory(id)
    }

    suspend fun reOrderCategoryCall(id: Int, oldPos: Int, newPos: Int) =
        apiHelperNew.reOrderCategoryCall(id, oldPos, newPos)

    suspend fun updateCategorySort(allCategories: ArrayList<TbCategory>) {
        appDatabase.categoryDao().addAll(allCategories)
    }

    suspend fun reOrderItemCall(id: Int, oldPos: Int, newPos: Int) =
        apiHelperNew.reOrderItemCall(id, oldPos, newPos)

    suspend fun reOrderNote(id: Int, oldPos: Int, newPos: Int) =
        apiHelperNew.reOrderNote(id, oldPos, newPos)


    suspend fun reOrderTip(id: Int, oldPos: Int, newPos: Int) =
        apiHelperNew.reOrderTip(id, oldPos, newPos)

    fun getCartList(orderType: String, employee_Id: Int): LiveData<List<CartModel>> {
        return appDatabase.cartDao().allItem(orderType, employee_Id)
    }

    fun getCartListFlow(orderType: String, employee_Id: Int): Flow<List<CartModel>> {
        return appDatabase.cartDao().allItemFlow(orderType, employee_Id)
    }

    fun getAllCartItems() = appDatabase.cartDao().getCartItems()
    fun getDineInCartItems(guestIndexForDineIn:Int) = appDatabase.cartDao().getDineInCartItems(guestIndexForDineIn)

    fun getCartDineInList(employee_Id: Int): LiveData<List<DineInCartModel>> {
        return appDatabase.cartDao().allItemDineIn(DINE_IN, employee_Id)
    }


    fun getManualSaleList(employee_id: Int): LiveData<List<CartModel>> {
        return appDatabase.cartDao().manualItem(employee_id)
    }

    fun getManualSaleItems(orderType: String, employee_Id: Int): LiveData<List<CartModel>> {
        return appDatabase.cartDao().getManualSaleItems(orderType, employee_Id)
    }

    fun getManualCategoryId(): LiveData<TbCategory> {
        return appDatabase.categoryDao().manualCategoryId
    }

     fun addItemCart(cartModel: CartModel) {
        synchronized(this) {
          //  appDatabase.beginTransaction()
            appDatabase.cartDao().addSuspended(cartModel)
           // appDatabase.endTransaction()
        }

    }

    suspend fun addItemToCart(tbCartItem: TbCartItem) {
        val startTime = System.currentTimeMillis()
        appDatabase.cartDao().addCartItem(tbCartItem)
        // Calculate the time taken
        val endTime = System.currentTimeMillis()
        val timeTaken = endTime - startTime
        Log.d("InsertTime", "Time taken to insert: $timeTaken ms")
    }

    suspend fun addItemCartDineIn(cartModel: DineInCartModel) {

        appDatabase.cartDao().addDineInCartDao(cartModel)
    }


    suspend fun createEmptyCart(cartModel: CartModel) {
        appDatabase.cartDao().add(cartModel)
    }


    suspend fun deleteCart(employee_id: Int) {
        appDatabase.cartDao().delete(employee_id)
        appDatabase.cartDao().deleteCartItems()
    }

    suspend fun deleteAllCart() {
        appDatabase.cartDao().delete()
    }

    suspend fun deleteManualSaleCart(employee_id: Int) {
        appDatabase.cartDao().deleteManualSale(employee_id)
    }

    suspend fun updateModifierSort(allCategories: ArrayList<ModifierSet>) {
        appDatabase.modifierSetDao().addAll(allCategories)
    }

    suspend fun deleteModifierSetCall(id: Int) = apiHelperNew.deleteModifierSetCall(id)
    suspend fun deleteModifierSet(id: Int) = appDatabase.modifierSetDao().delete(id)

    fun serviceChargeList() =
        performGetOperationDatabase(databaseQuery = { appDatabase.serviceChargeDao().allServiceCharge })

    fun getOrderTypes() =
        performGetOperationDatabase(databaseQuery = { appDatabase.orderTypeDao().orderTypes })

    fun disocuntList() =
        performGetOperationDatabase(databaseQuery = { appDatabase.discountDao().allDiscount })

    fun taxList() = performGetOperationDatabase(databaseQuery = { appDatabase.taxDao().allTax })

    fun modifierSetList(ids: IntArray) =
        performGetOperationDatabase(databaseQuery = {
            appDatabase.modifierSetDao().modifierSetByItem(ids)
        })

    suspend fun createModifierSet(data: CreateModifierRequest) =
        apiHelperNew.createModifierSet(data)

    suspend fun addModifierSets(modifierSet: ModifierSet) {
        appDatabase.modifierSetDao().add(modifierSet)
    }

    suspend fun updateModifierSets(mId: Int, data: CreateModifierRequest) =
        apiHelperNew.updateModifierSets(mId, data)

    suspend fun reOrderModifierCall(id: Int, oldPos: Int, newPos: Int) =
        apiHelperNew.reOrderModifierCall(id, oldPos, newPos)


    fun getOptionSet() =
        performGetOperation(databaseQuery = { appDatabase.optionSetDao().all },
            networkCall = { apiHelperNew.getOptionSet() },
            saveCallResult = {
                appDatabase.optionSetDao().addAll(it.data)
            })

    fun getOptionListData() =
        performGetOperationDatabase(databaseQuery = { appDatabase.optionSetDao().all })

    suspend fun createOptionSet(data: CreateOptionRequestModel) =
        apiHelperNew.createOptionSet(data)


    suspend fun addOptionSetsDatabase(data: List<OptionSet>) =
        appDatabase.optionSetDao().addAll(data)

    suspend fun updateOptionSet(mId: Int, data: CreateOptionRequestModel) =
        apiHelperNew.updateOptionSet(mId, data)

    suspend fun deleteOptionSet(id: Int) =
        apiHelperNew.deleteOptionSet(id)

    suspend fun deleteOptionSetDatabase(id: Int) =
        appDatabase.optionSetDao().delete(id)

    suspend fun reOrderOptionSet(
        id: Int,
        oldPos: Int,
        newPos: Int
    ) =
        apiHelperNew.reOrderOptionSet(id, oldPos, newPos)

    suspend fun updateOptionSort(allCategories: ArrayList<OptionSet>) {
        appDatabase.optionSetDao().addAll(allCategories)
    }

    suspend fun createOrder(data: OrderRequestModel) =
        apiHelperNew.createOrder(data)

    suspend fun sellGiftCard(data: SellGiftCardRequestModel) =
        apiHelperNew.sellGiftCard(data)

    suspend fun addValueInGiftCard(data: GiftCardAddValueRequest) =
        apiHelperNew.addValueInGiftCard(data)

    suspend fun giftCardCheckBalance(giftCardCheckBalanceRequest: GiftCardCheckBalanceRequest) =
        apiHelperNew.giftCardCheckBalance(giftCardCheckBalanceRequest)

    suspend fun splitByOrder(data: SpitByOrderRequestModel) =
        apiHelperNew.splitByOrder(data)

    suspend fun updateOrder(
        orderId: Int?,
        data: OrderRequestModel
    ) =
        apiHelperNew.updateOrder(orderId, data)

    suspend fun orderDetailsById(orderId: Int) =
        apiHelperNew.orderDetailsById(orderId)


    suspend fun paymentDetailsById(paymentId: Int) =
        apiHelperNew.paymentDetailsById(paymentId)


    suspend fun orderDetailsId(orderId: Int) =
        apiHelperNew.orderDetailsId(orderId)

    suspend fun emailReceipt(data: HashMap<String, String>) =
        apiHelperNew.emailReceipt(data)

    suspend fun emailReceiptForETS(data: HashMap<String, String>) =
        apiHelperNew.emailReceiptForETS(data)

    suspend fun phoneReceipt(data: HashMap<String, String>) =
        apiHelperNew.phoneReceipt(data)

    suspend fun giftCardEmailReceipt(data: HashMap<String, String>) =
        apiHelperNew.giftCardEmailReceipt(data)

    suspend fun giftCardPhoneReceipt(data: HashMap<String, String>) =
        apiHelperNew.giftCardPhoneReceipt(data)

    suspend fun assignCustomerOrder(
        orderId: Int,
        customerId: Int,
        newPos: Int,
        paymentId: Int,
        finalrewards: Int
    ) =
        apiHelperNew.assignCustomerOrder(
            orderId,
            customerId,
            newPos,
            paymentId,
            finalrewards
        )

    suspend fun deleteTerminalsFromDb() =
        appDatabase.terminalDao().delete()

    suspend fun addTerminalsDatabase(
        data: List<VenueDetailsResponse.Data.Terminal>
    ) =
        appDatabase.terminalDao()
            .addAllTerminalSuspend(
                data
            )


    fun getEmployeeEmail(selectedTerminalId: Int) =
        performGetOperationDatabase(databaseQuery = {
            appDatabase.employeeDao()
                .employeeById(selectedTerminalId)
        })

    fun getTerminalListDatabse() =
        performGetOperationDatabase(databaseQuery = { appDatabase.terminalDao().allTerminal })

    fun getMinMax(_itemId: Int, modifierSetId: Int?): LiveData<ItemModifierSets?>? {
        if (modifierSetId != null) {
            return appDatabase.itemModifierSetsDao()
                .minMaxByItemModifier(_itemId, modifierSetId)
        }
        return null
    }

    fun getCashDisDetail(active: Int): LiveData<CashDiscountModel>? {
        return appDatabase.cashDiscountDao().getActiveCashDiscount(active)
    }


    fun getOpenOrders(
        paymentStatus: String,
        startDate: String,
        endDate: String
    ): LiveData<Resource<OpenOrderResponse>> =
        performGetOperationNew(networkCall = {
            if (paymentStatus == "Upcoming") apiHelperNew.getUpcomingOpenOrders() else apiHelperNew.getOpenOrders(
                paymentStatus,
                startDate,
                endDate
            )
        })

    fun getPhoneOrders(
        paymentStatus: String,
        startDate: String,
        endDate: String
    ): LiveData<Resource<OpenOrderResponse>> =
        performGetOperationNew(networkCall = {
            if (paymentStatus == "Upcoming") apiHelperNew.getUpcomingOpenOrders() else apiHelperNew.getPhoneOrders(
                paymentStatus,
                startDate,
                endDate
            )
        })


    suspend fun refundPaymentOnline(data: RefundRequestModelOnlineOrder) =
        apiHelperNew.refundPaymentOnline(data)

    fun getOnlineOrders(
        startDate: String,
        endDate: String,
        order_status: String
    ): LiveData<Resource<OnlineOrderResponseModel>> =
        performGetOperationNew(networkCall = {
            apiHelperNew.getOnlineOrders(
                startDate,
                endDate,
                order_status
            )
        })

    fun getAllOrders(
        startDate: String,
        endDate: String,
        order_status: String,
        payment_status: String,
        order_type_id: String
    ): LiveData<Resource<OnlineOrderResponseModel>> =
        performGetOperationNew(networkCall = {
            apiHelperNew.getAllOrders(
                startDate,
                endDate,
                order_status,
                payment_status,
                order_type_id
            )
        })

    fun acceptedAndDeclineOrders(
        time: Int,
        order_id: Int,
        isaccepted: Boolean,
        employee_id: Int,
        terminalid: Int
    ): LiveData<Resource<OnlineOrderStatusUpdateResponse>> =
        performGetOperationNew(networkCall = {
            apiHelperNew.setAcceptedAndDeclineorder(
                time,
                order_id,
                isaccepted,
                employee_id,
                terminalid
            )
        })

    fun updateOnlineOrders(
        order_id: Int,
        order_status: String
    ): LiveData<Resource<BaseResponse>> =
        performGetOperationNew(networkCall = {
            apiHelperNew.updateOnlineOrder(
                order_id,
                order_status
            )
        })


    suspend fun cashInOut(data: CashLogRequest) = apiHelperNew.cashInOut(data)

    suspend fun addItemToWastage(data: WastageItemRequest) = apiHelperNew.addItemToWastage(data)

    suspend fun getCashLog(
        startDate: String,
        endDate: String,
        terminalId: String,
        s: String,
        s1: String
    ) =
        apiHelperNew.getCashInOut(startDate, endDate, terminalId, s, s1)

    suspend fun orderUpdateTip(
        orderId: Int,
        customerId: Double,
        is_captured: Boolean,
        data: CashInOutModel
    ) =
        apiHelperNew.orderUpdateTip(orderId, customerId, is_captured, data)

    suspend fun updateTipWithSignature(orderId: Int, signatureInBase64: String, tip: Double) =
        apiHelperNew.updateTipWithSignature(orderId, signatureInBase64, tip)

    suspend fun updateTipWithSignatureFM(option: HashMap<String, Any>) =
        apiHelperNew.updateTipWithSignatureFM(option)

    suspend fun updateKitchenFireStatus(
        id: Int,
        isFired: Boolean,
        items: String
    ) =
        apiHelperNew.updateKitchenFireStatus(id, isFired, items)

    suspend fun getTableStatus(
        tableId: Int,
        empId: Int,
        terminalId: Int,
        status: String,
        clearTable: Boolean
    ) =
        apiHelperNew.getTableStatus(tableId, empId, terminalId, status, clearTable)

    suspend fun mergeFloorTable(
        parentTableId: Int,
        childIds: String,
        orderModel: MergeTableRequest?,
        childOrderIds: String?,
        orderId: Int? = null
    ) =
        apiHelperNew.mergeFloorTable(parentTableId, childIds, orderModel, childOrderIds, orderId)

    suspend fun transferTable(
        orderId: Int,
        floorPlanId: Int,
        floorPlanTableId: Int,
        oldFloorPlanTableId: Int
    ) =
        apiHelperNew.transferTable(orderId, floorPlanId, floorPlanTableId, oldFloorPlanTableId)

    suspend fun unMergeTable(id: Int) = apiHelperNew.unMergeTable(id)
    suspend fun payByGuest(
        id: Int,
        isAllComplete: Boolean,
        model: GuestPaymentRequest
    ) = apiHelperNew.payByGuest(id, isAllComplete, model)

    suspend fun orderCancel(id: Int, data: OrderCancelRequest) =
        apiHelperNew.orderCancel(id, data)

    fun getFloorPlan(locationId: Int) =
        performGetOperationNew(networkCall = { apiHelperNew.getFloorPlan(locationId) })

    fun getFloorPlanTableDetails() =
        performGetOperationNew(networkCall = { apiHelperNew.getFloorPlanTableDetails() })

    fun getAvailableTransferTableList() =
        performGetOperationNew(networkCall = {
            apiHelperNew.getAvailableTransferTableList(
                prefProvider.getValueInt(
                    EMPLOYEE_ID, 0
                )
            )
        })

    suspend fun employeeClockOut(data: HashMap<String, String>) =
        apiHelperNew.employeeClockOut(data)

    suspend fun getReportSummary(
        startDate: String,
        endDate: String,
        terminalId: String
    ) = apiHelperNew.getReportSummary(startDate, endDate, terminalId)

    suspend fun getReportEOD(
        startDate: String,
        endDate: String,
        terminalId: String,
        employee_id: String,
        email: String
    ) =
        apiHelperNew.getReportEOD(startDate, endDate, terminalId, employee_id, email)

    suspend fun getEmployeeTip(
        startDate: String,
        endDate: String
    ) =
        apiHelperNew.getEmployeeTip(startDate, endDate)

    suspend fun sendEmailReportSummary(
        startDate: String,
        endDate: String,
        email: String,
        employee_id: String
    ) =
        apiHelperNew.sendEmailTimeSheet(startDate, endDate, email, employee_id)

    suspend fun getOrderHistory(id: String) =
        apiHelperNew.getOrderHistory(id)

    suspend fun clearTableManually() {
        appDatabase.clearAllTables()
    }

    suspend fun clearTable() {

        LogUtil.logE("clear Db Table", "-------")
        appDatabase.categoryDao().delete1()
        appDatabase.itemDao().delete()
        appDatabase.taxDao().delete()
        appDatabase.tipDao().delete()
        appDatabase.discountDao().delete()
        appDatabase.notesDao().delete()
        appDatabase.serviceChargeDao().delete()
        appDatabase.cartDao().delete()
        appDatabase.employeeDao().delete()
        appDatabase.customerDao().deleteCustomerTb()
        appDatabase.teamRoleDao().delete()
        appDatabase.moduleDao().delete()
        appDatabase.modifierSetDao().delete()
        appDatabase.optionSetDao().delete()
        appDatabase.orderTypeDao().delete()
        appDatabase.terminalDao().delete()
        appDatabase.itemModifierSetsDao().delete()
        appDatabase.printerDao().deleteKitchenPrinters()
        appDatabase.printerDao().deleteCustomerPrinters()
        appDatabase.kitchenSettingsDao().delete()
        appDatabase.customerSettingsDao().delete()
        appDatabase.cancelOrderReasonDao().delete()
        appDatabase.cashDiscountDao().delete()

    }

    fun orderCounts(startDate: String?, endDate: String?, isOpenOrder: Boolean) =
        performGetOperationNew(networkCall = {
            apiHelperNew.orderCounts(
                startDate,
                endDate,
                isOpenOrder
            )
        })

    fun onlineOrderCounts(startDate: String?, endDate: String?) =
        performGetOperationNew(networkCall = { apiHelperNew.onlineOrderCounts(startDate, endDate) })

    fun allOrderCounts(startDate: String?, endDate: String?) =
        performGetOperationNew(networkCall = { apiHelperNew.allOrderCounts(startDate, endDate) })

    fun inventoryCounts() =
        performGetOperationNew(networkCall = { apiHelperNew.inventoryCounts() })

    suspend fun addPAXData(paxData: PAXData) {
        appDatabase.PAXDao().add(paxData)
    }

    suspend fun getPAXDetails() = appDatabase.PAXDao().getPAXDetails()

    suspend fun deletePAXTable() {
        appDatabase.PAXDao().delete()
    }

    suspend fun addCardReader(tbCardReader: TbCardReader) {
        appDatabase.cardReaderDao().add(tbCardReader)
    }

    suspend fun deleteTable() {
        appDatabase.cardReaderDao().delete()
    }

    suspend fun updateCardReader(tbCardReader: TbCardReader) {
        appDatabase.cardReaderDao().updateById(tbCardReader.status, tbCardReader.mcAddress)
    }

    fun getCardReaderList(id: String) =
        performGetOperationDatabase { appDatabase.cardReaderDao().cardReaderById(id) }

    fun getCardReaderList() =
        performGetOperationDatabase { appDatabase.cardReaderDao().allList() }

    fun cardReaderActiveList() =
        performGetOperationDatabase { appDatabase.cardReaderDao().cardReaderActiveList() }

    suspend fun deleteDineInCart() = appDatabase.cartDao().deleteDineInCart()

    suspend fun timeDetails(terminalId: Int) = apiHelperNew.timeDetails(terminalId)

    suspend fun deleteCustomerPrinters() {
        appDatabase.printerDao().deleteCustomerPrinters()
    }

    suspend fun deleteKitchenPrinters() {
        appDatabase.printerDao().deleteKitchenPrinters()
    }

    suspend fun addKitchenPrinter(kitchenPrinterList: List<PrinterResponse.Data.KitchenReceiptPrinters>) {
        appDatabase.printerDao().addKitchenPrinterList(kitchenPrinterList)
    }

    suspend fun addCustomerPrinter(customerPrinterList: List<PrinterResponse.Data.CustomerReceiptPrinters>) {
        appDatabase.printerDao().addCustomerPrinterList(customerPrinterList)

    }

    suspend fun updateModifiersForItem(modifierSetIds: List<Int>, itemId: Int?) {
        appDatabase.itemDao().updateItemModifiers(itemId!!, modifierSetIds)
    }

    suspend fun fetchAllItemsList(): List<TbItem?>? {
        return appDatabase.itemDao().allItemsList()
    }

    // To update Items ids array for single modifier
    suspend fun updateItemIdsForModifier(modId: Int?, itemIdsList: List<Int>) {
        appDatabase.modifierSetDao().updateModifiersItem(modId!!, itemIdsList)
    }
}

