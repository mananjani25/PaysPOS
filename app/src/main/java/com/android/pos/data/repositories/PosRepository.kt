package com.android.pos.data.repositories


import com.android.pos.data.db.AppDatabase
import com.android.pos.data.db.IDataManager
import com.android.pos.data.entities.TbCategory
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.requestModel.CreateEmployeeRequestModel
import com.android.pos.data.model.requestModel.CreateItemRequestModel
import com.android.pos.data.model.requestModel.CreateNoteRequest
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

    fun venueDataLocal() = performGetOperation(
        databaseQuery = { appDatabase.categoryDao().categoryWithInventory()!! },
        networkCall = { apiHelperNew.syncVenueData() },
        saveCallResult = { response ->
            val mCategory = response.data.categories
            val categoryModelList = ArrayList<TbCategory>()
            val inventoryModelList = ArrayList<TbItem>()
            mCategory.forEach { category ->
                val model = TbCategory().apply {
                    createdAt = ""
                    id = category.id
                    isHide = false
                    name = category.name
                    sort = category.sort
                    updatedAt = ""
                }
                categoryModelList.add(model)

                category.items.forEach {

                    val items = TbItem().apply {
                        itemId = it.id
                        categoryId = category.id
                        categoryName = category.name
                        name = it.name
                        imageUrl = it.imgUrl
                        kitchenName = it.kitchenName
                        price = it.price
                        productCode = it.productCode
                    }

                    inventoryModelList.add(items)
                }
            }

            appDatabase.categoryDao().addAll(categoryModelList)
            appDatabase.itemDao().addAllItem(inventoryModelList)
        }
    )

    fun getCategoryList() =
        performGetOperationDatabase(databaseQuery = { appDatabase.categoryDao().all() })

    suspend fun deleteCategory(catId: Int) = appDatabase.categoryDao().deleteCategoryById(catId)


    fun getItemsList() =
        performGetOperationDatabase(databaseQuery = { appDatabase.itemDao().allItem!! })

    fun getInventory(catId: Int) =
        performGetOperationDatabase(databaseQuery = { appDatabase.itemDao().getItemList(catId)!! })




    fun getNoteList() =
        performGetOperationNew(networkCall = { apiHelperNew.getNoteList() })

    suspend fun deleteNote(data: Int) = apiHelperNew.deleteNote(data)

    suspend fun createNote(data: CreateNoteRequest) = apiHelperNew.createNote(data)

    suspend fun updateNote(taxId: Int, data: CreateNoteRequest) =
        apiHelperNew.updateNote(taxId, data)


    fun employeesList(locationId: Int) =
        performGetOperationNew(networkCall = { apiHelperNew.employeesList(locationId) })

    suspend fun createEmployee(data: CreateEmployeeRequestModel) = apiHelperNew.createEmployee(data)

    suspend fun updateEmployee(taxId: Int, data: CreateEmployeeRequestModel) =
        apiHelperNew.updateEmployee(taxId, data)

    suspend fun deleteEmployee(data: Int) = apiHelperNew.deleteEmployee(data)


    suspend fun logout(data: HashMap<String, String>) = apiHelperNew.logOut(data)



    override suspend fun abs() {

        appDatabase.characterDao().getCharacter(0)
    }


    suspend fun deleteItem(itemId: Int) = apiHelperNew.deleteItem(itemId)

    suspend fun itemHide(itemId: Int, data: HashMap<String, String>) =
        apiHelperNew.hideItem(itemId, data)

    suspend fun createItem(data: CreateItemRequestModel) = apiHelperNew.createItem(data)
    suspend fun updateItem(id: Int, data: CreateItemRequestModel) =
        apiHelperNew.updateItem(id, data)
}

