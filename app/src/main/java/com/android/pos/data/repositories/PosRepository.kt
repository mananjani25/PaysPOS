package com.android.pos.data.repositories


import androidx.lifecycle.LiveData
import com.android.pos.data.db.AppDatabase
import com.android.pos.data.db.IDataManager
import com.android.pos.data.entities.*
import com.android.pos.data.entities.ModifierSet
import com.android.pos.data.model.CustomerListResponse
import com.android.pos.data.model.requestModel.*
import com.android.pos.data.model.responseModel.NoteResponse
import com.android.pos.data.remote.ApiHelper
import com.android.pos.utils.performGetOperation
import com.android.pos.utils.performGetOperationDatabase
import com.android.pos.utils.performGetOperationNew
import javax.inject.Inject


class PosRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val apiHelperNew: ApiHelper
) : IDataManager {


    fun syncVenueData() =
        performGetOperationNew(networkCall = { apiHelperNew.syncVenueData() })

    /*fun syncVenueDetails() =
        performGetOperationNew(networkCall = { apiHelperNew.syncVenueDetails() })*/

    suspend fun syncVenueDetails() = apiHelperNew.syncVenueDetails()


    fun venueDataLocal() = performGetOperation(
        databaseQuery = { appDatabase.categoryDao().categoryWithInventory()!! },
        networkCall = { apiHelperNew.syncVenueData() },
        saveCallResult = { response ->
            val mData = response.data
            val mCategory = mData.categories
            val categoryModelList = ArrayList<TbCategory>()
            val inventoryModelList = ArrayList<TbItem>()
            val modifierSetList = ArrayList<ItemModifierSet>()
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
                }
                categoryModelList.add(model)

                category.items.forEach {

                    val items = TbItem().apply {
                        itemId = it.id
                        name = it.name
                        cost = it.cost
                        price = it.price
                        priceType = it.priceType
                        quantity = it.quantity
                        kitchenName = it.kitchenName
                        productCode = it.productCode
                        sku = it.sku
                        isHide = it.active
                        sort = it.sort
                        imageUrl = it.imgUrl
                        thumbImageUrl = it.thumpImgUrl
                        categoryId = category.id
                        categoryName = category.name
                        taxes = it.taxes
                        modifier_set_ids = it.modifierIds
                    }

                    modifierSetList.addAll(it.modifierSets)

                    inventoryModelList.add(items)
                }
            }

            appDatabase.categoryDao().addAll(categoryModelList)
            appDatabase.itemDao().addAllItem(inventoryModelList)
            appDatabase.itemModifierSetDao().addAll(modifierSetList)
        }
    )

    fun getCategoryList() =
        performGetOperation(databaseQuery = { appDatabase.categoryDao().all() },
            networkCall = { apiHelperNew.getCategories() },
            saveCallResult = {
                val categoryModelList = ArrayList<TbCategory>()
                it.data.forEach { category ->
                    val model = TbCategory().apply {
                        createdAt = category.createdAt
                        id = category.id
                        active = category.active
                        name = category.name
                        sort = category.sort
                        updatedAt = category.updatedAt
                        locationId = category.locationId
                        item_ids = category.itemIds
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

                    val items = TbItem().apply {
                        itemId = it.id
                        name = it.name
                        cost = it.cost
                        price = it.price
                        priceType = it.priceType
                        quantity = it.quantity
                        kitchenName = it.kitchenName
                        productCode = it.productCode
                        sku = it.sku
                        isHide = it.active
                        sort = it.sort
                        imageUrl = it.imgUrl
                        thumbImageUrl = it.thumpImgUrl
                        categoryId = it.categoryId
                        taxes = it.taxes
//                        categoryName =
                    }
                    inventoryModelList.add(items)
                }
                appDatabase.itemDao().addAllItem(inventoryModelList)
            }
        )


    fun getNoteList() = performGetOperation(
        databaseQuery = { appDatabase.notesDao().alllNotes },
        networkCall = { apiHelperNew.getNoteList() },
        saveCallResult = { appDatabase.notesDao().addAllNotes(it.data) })

    suspend fun addAllNotesDatabase(data: List<NoteResponse.Data>) =
        appDatabase.notesDao().addAllNotesSuspend(data)

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

    fun employeesTimeSheet(startDate: String, endDate: String, teamRoleId: Int) =
        performGetOperationNew(networkCall = { apiHelperNew.employeesTimeSheet(startDate, endDate, teamRoleId) })

    fun employeesTimeSheetDetails(startDate: String, endDate: String, teamId: Int) =
        performGetOperationNew(networkCall = { apiHelperNew.employeesTimeSheetDetails(startDate, endDate, teamId) })

    suspend fun clearCustomerTb() = appDatabase.customerDao().deleteCustomerTb()

    fun customerList() = performGetOperation(
        databaseQuery = { appDatabase.customerDao().allCustomer },
        networkCall = { apiHelperNew.customerList() },
        saveCallResult = { appDatabase.customerDao().addAllCustomer(it.data) })

    suspend fun createEmployee(data: CreateEmployeeRequestModel) = apiHelperNew.createEmployee(data)

    suspend fun createEmployeeDatabase(data: Employee) =
        appDatabase.employeeDao().addEmployee(data)

    suspend fun addCustomer(data: CustomerListResponse.Data) =
        appDatabase.customerDao().addCustomer(data)


    suspend fun deleteCustomerDataBase(id: Int) = appDatabase.customerDao().deleteCustomerByID(id)

    suspend fun createCustomer(data: CreateCustomerRequestModel) = apiHelperNew.createCustomer(data)

    suspend fun updateCustomer(id: Int, data: CreateCustomerRequestModel) =
        apiHelperNew.updateCustomer(id, data)

    suspend fun deleteCustomer(id: Int) = apiHelperNew.deleteCustomer(id)

    suspend fun updateEmployee(taxId: Int, data: CreateEmployeeRequestModel) =
        apiHelperNew.updateEmployee(taxId, data)

    suspend fun deleteEmployee(data: Int) = apiHelperNew.deleteEmployee(data)

    suspend fun deleteEmployeeDatabase(employeeId: Int) =
        appDatabase.employeeDao().deleteEmployeeById(employeeId)


    suspend fun logout(data: HashMap<String, String>) = apiHelperNew.logOut(data)


    override suspend fun abs() {
        appDatabase.characterDao().getCharacter(0)
    }


    suspend fun deleteItem(itemId: Int) = apiHelperNew.deleteItem(itemId)

    suspend fun itemHide(itemId: Int, active: Boolean) =
        apiHelperNew.hideItem(itemId, active)

    fun unhideItemList() =
        performGetOperationDatabase(databaseQuery = { appDatabase.itemDao().unhideItem!! })

    suspend fun createItem(data: CreateItemRequestModel) = apiHelperNew.createItem(data)
    suspend fun updateItem(id: Int, data: CreateItemRequestModel) =
        apiHelperNew.updateItem(id, data)

    suspend fun deleteCategoryCall(data: Int) = apiHelperNew.deleteCategoryCall(data)
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

    suspend fun updateItemCategory(catId: Int, catName: String, itemId: Int?) {

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


    fun getCartList(): LiveData<List<CartModel>> {
        return appDatabase.cartDao().allItem
    }

    fun getManualSaleList(): LiveData<List<CartModel>> {
        return appDatabase.cartDao().manualItem
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

    fun modifierSetList(ids: IntArray) =
        performGetOperationDatabase(databaseQuery = {
            appDatabase.itemModifierSetDao().modifierSetByItem(ids)
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

    suspend fun createOrder(data: OrderRequestModel) = apiHelperNew.createOrder(data)
}

