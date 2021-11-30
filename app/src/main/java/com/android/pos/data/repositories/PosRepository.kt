package com.android.pos.data.repositories


import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.android.pos.data.db.AppDatabase
import com.android.pos.data.db.IDataManager
import com.android.pos.data.entities.*
import com.android.pos.data.entities.ModifierSet
import com.android.pos.data.model.SplitDetailListModel
import com.android.pos.data.model.requestModel.*
import com.android.pos.data.model.responseModel.*
import com.android.pos.data.remote.ApiHelper
import com.android.pos.utils.performGetOperation
import com.android.pos.utils.performGetOperationDatabase
import com.android.pos.utils.performGetOperationNew
import com.android.pos.utils.statusUtils.Resource
import javax.inject.Inject


class PosRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val apiHelperNew: ApiHelper
) : IDataManager {

    fun getCustomerReceiptSettings() = appDatabase.customerSettingsDao().getCustomerSettings

    fun getKitchenReceiptSettings() = appDatabase.kitchenSettingsDao().getKitchenSettings

    fun getTipsList() = appDatabase.tipDao().allTips

    fun syncVenueData() =
        performGetOperationNew(networkCall = { apiHelperNew.syncVenueData() })

    suspend fun deleteKitchenPrinter(id: Int) =
        appDatabase.printerDao().deleteKitchenPrinterById(id)

    suspend fun deleteCustomerPrinter(id: Int) =
        appDatabase.printerDao().deleteCustomerPrinterById(id)

    suspend fun updateKitchenPrinterStatus(status: Boolean, id: Int) =
        appDatabase.printerDao().updateKitchenStatus(status, id)

    suspend fun updateCustomerPrinterStatus(status: Boolean, id: Int) =
        appDatabase.printerDao().updateCustomerStatus(status, id)

    fun getPrinters() = performGetOperation(
        databaseQuery = {
            appDatabase.printerDao().customerPrintList
        },
        networkCall = { apiHelperNew.getPrinterData() },
        saveCallResult = {
            appDatabase.printerDao().addCustomerPrinterList(it.data.customerReceiptPrinters)
            it.data.kitchenReceiptPrinters?.let { it1 ->
                appDatabase.printerDao().addKitchenPrinterList(
                    it1
                )
            }
        }

    )

    fun getKitchenPrinters() =
        performGetOperationDatabase { appDatabase.printerDao().kitchenPrintList }

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

    suspend fun createPrinter(data: CreatePrinterRequestModel) =
        apiHelperNew.createPrinter(data)

    suspend fun deletePrinter(id: Int) = apiHelperNew.deletePrinter(id)

    suspend fun updatePrinter(id: Int, model: CreatePrinterRequestModel) =
        apiHelperNew.updatePrinter(id, model)

    suspend fun updatePrinterStatus(id: Int, terminal_id: Int, status: Boolean) =
        apiHelperNew.updatePrinterStatus(id, terminal_id, status)
    /*fun syncVenueDetails() =
        performGetOperationNew(networkCall = { apiHelperNew.syncVenueDetails() })*/

    suspend fun syncVenueDetails() = apiHelperNew.syncVenueDetails()


    suspend fun syncInventory() = apiHelperNew.syncVenueData()


    fun venueDataLocal() = performGetOperationDatabase(
        databaseQuery = { appDatabase.categoryDao().categoryWithInventory()!! },
    )

    suspend fun saveDatabase(response: VenueDataResponse) {
        appDatabase.categoryDao().delete()
        appDatabase.itemDao().delete()
        appDatabase.modifierSetDao().delete()
        appDatabase.itemModifierSetsDao().delete()
        appDatabase.optionSetDao().delete()

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

    suspend fun hideCategory(catId: Int, active: Boolean) =
        appDatabase.categoryDao().hideCategory(catId, active)


    fun getItemsList() =
        performGetOperationDatabase(databaseQuery = { appDatabase.itemDao().allItem!! })

    fun getItemsbyId(itemId: Int) =
        performGetOperationDatabase(databaseQuery = { appDatabase.itemDao().itemById(itemId)!! })

    fun modifierSetsList() =
        performGetOperationDatabase(databaseQuery = { appDatabase.modifierSetDao().all })

    fun modifierSets() =
        performGetOperation(databaseQuery = { appDatabase.modifierSetDao().all },
            networkCall = { apiHelperNew.getModifierSetCall() },
            saveCallResult = {
                appDatabase.modifierSetDao().addAll(it.data)
            })


    fun getInventory() =
        performGetOperation(
            databaseQuery = { appDatabase.itemDao().allItem!! },
            networkCall = { apiHelperNew.getItemsCall() },
            saveCallResult = {

                val inventoryModelList = ArrayList<TbItem>()

                it.data.forEach {

                    val items = TbItem().convertToItem(it, null)
                    inventoryModelList.add(items)
                }
                appDatabase.itemDao().addAllItem(inventoryModelList)
            }
        )


    fun getNoteList() = performGetOperation(
        databaseQuery = { appDatabase.notesDao().alllNotes },
        networkCall = { apiHelperNew.getNoteList() },
        saveCallResult = { appDatabase.notesDao().addAllNotes(it.data) })

    suspend fun deleteNotesFromDb() =
        appDatabase.notesDao().delete()

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

    fun getActiveLoyaltyProgramFromDb() =  performGetOperationDatabase(databaseQuery = { appDatabase.loyaltyProgramsDao().findActiveLoyalty(active = true) })


    suspend fun addCashDiscountsFromDb(data: List<CashDiscountModel>) {
        appDatabase.cashDiscountDao().addAll(data)
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

    fun getCustomerDetailsByID(id: Int?): LiveData<TbCustomer> {

        return appDatabase.customerDao().getCustomerDetailsByID(id)
    }


    suspend fun deleteCustomerDataBase(id: Int?) =
        appDatabase.customerDao().deleteCustomerByID(id)


    suspend fun createCustomer(data: CreateCustomerRequestModel) =
        apiHelperNew.createCustomer(data)

    suspend fun updateCustomer(id: Int, data: CreateCustomerRequestModel) =
        apiHelperNew.updateCustomer(id, data)

    suspend fun deleteCustomer(id: Int?) = apiHelperNew.deleteCustomer(id)

    suspend fun updateEmployee(
        taxId: Int,
        data: CreateEmployeeRequestModel
    ) =
        apiHelperNew.updateEmployee(taxId, data)

    suspend fun deleteEmployee(data: Int) =
        apiHelperNew.deleteEmployee(data)

    suspend fun deleteEmployeeDatabase(employeeId: Int) =
        appDatabase.employeeDao().deleteEmployeeById(employeeId)


    suspend fun logout(data: HashMap<String, String>) =
        apiHelperNew.logOut(data)


    override suspend fun abs() {
        appDatabase.characterDao().getCharacter(0)
    }


    suspend fun deleteItem(itemId: Int) = apiHelperNew.deleteItem(itemId)

    suspend fun itemHide(itemId: Int, active: Boolean) =
        apiHelperNew.hideItem(itemId, active)

    fun unhideItemList() =
        performGetOperationDatabase(databaseQuery = { appDatabase.itemDao().unhideItem!! })

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


    fun getCartList(orderType: String): LiveData<List<CartModel>> {
        return appDatabase.cartDao().allItem(orderType)
    }

    fun getManualSaleList(): LiveData<List<CartModel>> {
        return appDatabase.cartDao().manualItem
    }

    fun getManualCategoryId(): LiveData<TbCategory> {
        return appDatabase.categoryDao().manualCategoryId
    }

    suspend fun addItemCart(cartModel: CartModel) {

        appDatabase.cartDao().add(cartModel)
    }

    suspend fun deleteCart() {

        appDatabase.cartDao().delete()
    }

    suspend fun deleteManualSaleCart() {
        appDatabase.cartDao().deleteManualSale()
    }

    suspend fun updateModifierSort(allCategories: ArrayList<ModifierSet>) {
        appDatabase.modifierSetDao().addAll(allCategories)
    }

    suspend fun deleteModifierSetCall(id: Int) = apiHelperNew.deleteModifierSetCall(id)
    suspend fun deleteModifierSet(id: Int) = appDatabase.modifierSetDao().delete(id)

    fun serviceChargeList() =
        performGetOperationDatabase(databaseQuery = { appDatabase.serviceChargeDao().allServiceCharge })

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

    suspend fun phoneReceipt(data: HashMap<String, String>) =
        apiHelperNew.phoneReceipt(data)

    suspend fun assignCustomerOrder(
        orderId: Int,
        customerId: Int,
        newPos: Int
    ) =
        apiHelperNew.assignCustomerOrder(
            orderId,
            customerId,
            newPos
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


    fun getOpenOrders(param1: String): LiveData<Resource<OpenOrderResponse>> =
        performGetOperationNew(networkCall = {
            if (param1 == "Upcoming") apiHelperNew.getUpcomingOpenOrders() else apiHelperNew.getOpenOrders(
                param1
            )
        })


    suspend fun cashInOut(data: CashLogRequest) = apiHelperNew.cashInOut(data)


    suspend fun getCashLog(startDate: String, endDate: String, terminalId: String) =
        apiHelperNew.getCashInOut(startDate, endDate, terminalId)

    suspend fun orderUpdateTip(orderId: Int, customerId: Double) =
        apiHelperNew.orderUpdateTip(orderId, customerId)

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
        status: String
    ) =
        apiHelperNew.getTableStatus(tableId, empId, terminalId, status)

    suspend fun mergeFloorTable(
        parentTableId: Int,
        childIds: String,
        orderModel: OrderAttributeRequestModel,
        orderId:Int
    ) =
        apiHelperNew.mergeFloorTable(parentTableId, childIds,orderModel,orderId)

    suspend fun unMergeTable(id: Int) = apiHelperNew.unMergeTable(id)
    suspend fun payByGuest(
        id: Int,
        isAllComplete: Boolean,
        model: GuestPaymentRequest
    ) =
        apiHelperNew.payByGuest(id, isAllComplete, model)

    suspend fun orderCancel(id: Int, data: OrderCancelRequest) =
        apiHelperNew.orderCancel(id, data)

    fun getFloorPlan(locationId: Int) =
        performGetOperationNew(networkCall = { apiHelperNew.getFloorPlan(locationId) })

    fun getFloorPlanTableDetails() =
        performGetOperationNew(networkCall = { apiHelperNew.getFloorPlanTableDetails() })

    suspend fun employeeClockOut(data: HashMap<String, String>) =
        apiHelperNew.employeeClockOut(data)

    suspend fun getReportSummary(
        startDate: String,
        endDate: String,
        terminalId: String
    ) =
        apiHelperNew.getReportSummary(startDate, endDate, terminalId)

    suspend fun getOrderHistory(id: String) =
        apiHelperNew.getOrderHistory(id)

    suspend fun clearTable() {

        Log.e("clear Db Table", "-------")
        appDatabase.characterDao().delete()
        appDatabase.categoryDao().delete()
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
        appDatabase.printerDao().delete()
        appDatabase.kitchenSettingsDao().delete()
        appDatabase.customerSettingsDao().delete()
    }
}

