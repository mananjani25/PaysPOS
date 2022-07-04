package com.android.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.pos.data.model.PrinterQueueModel

@Dao
interface PrinterQueueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addPrinterQueueData(model: List<PrinterQueueModel>)


    @Query("DELETE FROM PRINTERQUEUE")
    suspend fun deletePrinterQueue()


    @Query("DELETE FROM printerqueue where printerqueue.id = :id")
    suspend fun deletePrinterQueueOrder(id: Int)

    @get:Query("select * from printerqueue")
    val printerQueueList: LiveData<List<PrinterQueueModel>>


}