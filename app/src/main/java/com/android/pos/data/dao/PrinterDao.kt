package com.android.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.pos.data.model.responseModel.PrinterResponse

@Dao
interface PrinterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addCustomerPrinterList(elementsBeanList: List<PrinterResponse.Data.CustomerReceiptPrinters>?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addKitchenPrinterList(customerPrint: List<PrinterResponse.Data.KitchenReceiptPrinters>)

    @get:Query("select * from TbCustomerPrint")
    val customerPrintList: LiveData<List<PrinterResponse.Data.CustomerReceiptPrinters>>

    @get:Query("select * from TbKitchenPrint")
    val kitchenPrintList: LiveData<List<PrinterResponse.Data.KitchenReceiptPrinters>>

    @Query("select * from TbKitchenPrint")
    suspend fun getKitchenPrinterList() : List<PrinterResponse.Data.KitchenReceiptPrinters>

    @Query("DELETE FROM TbCustomerPrint where TbCustomerPrint.id  = :id")
    suspend fun deleteCustomerPrinterById(id: Int)

    @Query("DELETE FROM TbKitchenPrint where TbKitchenPrint.id  = :id")
    suspend fun deleteKitchenPrinterById(id: Int)

    @Query("UPDATE TbCustomerPrint set status = :status WHERE TbCustomerPrint.id = :id")
    suspend fun updateCustomerStatus(status: Boolean, id: Int)

    @Query("UPDATE TbKitchenPrint set status = :status WHERE TbKitchenPrint.id = :id")
    suspend fun updateKitchenStatus(status: Boolean, id: Int)

    @Query("DELETE FROM TbKitchenPrint")
    suspend fun deleteKitchenPrinters()

    @Query("DELETE FROM TbCustomerPrint")
    suspend fun deleteCustomerPrinters()


}