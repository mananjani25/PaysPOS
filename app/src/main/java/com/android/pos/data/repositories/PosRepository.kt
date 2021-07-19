package com.android.pos.data.repositories


import com.android.pos.data.db.AppDatabase
import com.android.pos.data.db.IDataManager
import com.android.pos.data.model.requestModel.*
import com.android.pos.data.remote.ApiHelper
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








//    fun getCharacters() = performGetOperation(
//        databaseQuery = { appDatabase.characterDao().getAllCharacters() },
//        networkCall = { apiHelperNew.syncVenueData() },
//        saveCallResult = {
//            val mCategory = it.data.categories
//            appDatabase.characterDao().insertAll(it.results)
//        }
//    )

    fun getNoteList() =
        performGetOperationNew(networkCall = { apiHelperNew.getNoteList() })

    suspend fun deleteNote(data: Int) = apiHelperNew.deleteNote(data)

    suspend fun createNote(data: CreateNoteRequest) = apiHelperNew.createNote(data)

    suspend fun updateNote(taxId: Int, data: CreateNoteRequest) =
        apiHelperNew.updateNote(taxId, data)

    override suspend fun abs() {

        appDatabase.characterDao().getCharacter(0)
    }
}

