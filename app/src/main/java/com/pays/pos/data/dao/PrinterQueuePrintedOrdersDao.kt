package com.pays.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pays.pos.data.entities.TbPrinterQueuePrintedOrders
import com.pays.pos.data.model.responseModel.VenueDetailsResponse


@Dao
interface PrinterQueuePrintedOrdersDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun addPrintedOrders(data: TbPrinterQueuePrintedOrders)

}