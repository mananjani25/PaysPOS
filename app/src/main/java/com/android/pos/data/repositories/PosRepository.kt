package com.android.pos.data.repositories


import com.android.pos.data.db.AppDatabase
import com.android.pos.data.db.IDataManager
import com.android.pos.data.entities.TbCategory
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.requestModel.CreateEmployeeRequestModel
import com.android.pos.data.model.requestModel.CreateNoteRequest
import com.android.pos.data.remote.ApiHelper
import com.android.pos.utils.performGetOperation
import com.android.pos.utils.performGetOperationNew
import javax.inject.Inject


class PosRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val apiHelperNew: ApiHelper
) : IDataManager {


    fun syncVenueData() =
        performGetOperationNew(networkCall = { apiHelperNew.syncVenueData() })

    fun employeesList(locationId: Int) =
        performGetOperationNew(networkCall = { apiHelperNew.employeesList(locationId) })


    suspend fun logout(data: HashMap<String, String>) = apiHelperNew.logOut(data)


    fun getCharacters() = performGetOperation(
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
                        name = it.name
                        imageUrl = it.imgUrl.toString()
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

    fun getNoteList() =
        performGetOperationNew(networkCall = { apiHelperNew.getNoteList() })

    suspend fun deleteNote(data: Int) = apiHelperNew.deleteNote(data)

    suspend fun createNote(data: CreateNoteRequest) = apiHelperNew.createNote(data)

    suspend fun updateNote(taxId: Int, data: CreateNoteRequest) =
        apiHelperNew.updateNote(taxId, data)

    suspend fun createEmployee(data: CreateEmployeeRequestModel) = apiHelperNew.createEmployee(data)
    suspend fun updateEmployee(taxId: Int, data: CreateEmployeeRequestModel) =
        apiHelperNew.updateEmployee(taxId, data)

    suspend fun deleteEmployee(data: Int) = apiHelperNew.deleteEmployee(data)

    override suspend fun abs() {

        appDatabase.characterDao().getCharacter(0)
    }
}

