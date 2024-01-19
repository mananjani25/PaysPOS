package com.pays.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pays.pos.data.model.PrinterQueueModel

@Dao
interface PrinterQueueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addPrinterQueueData(model: PrinterQueueModel)


    @Query("select * from printerqueue where printerqueue.id =:id LIMIT 1")
      fun checkQueueDataExist(id: Int) : LiveData<PrinterQueueModel>

    @Query("UPDATE printerqueue SET printSuccessData =:list WHERE printerqueue.id =:id")
    suspend fun updatePrinterQueue(list: List<Int>, id: Int)


    @Query("DELETE FROM PRINTERQUEUE")
    suspend fun deletePrinterQueue()


    @Query("DELETE FROM printerqueue where printerqueue.id = :id")
    suspend fun deletePrinterQueueOrder(id: Int)

    @get:Query("select * from printerqueue")
    val printerQueueList: LiveData<List<PrinterQueueModel>>

    @Query("select * from printerqueue where printerqueue.id =:id")
    suspend fun getQueueData(id: Int): PrinterQueueModel


}