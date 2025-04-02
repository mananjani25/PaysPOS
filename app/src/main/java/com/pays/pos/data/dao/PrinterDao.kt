package com.pays.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pays.pos.data.model.responseModel.PrinterResponse

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

    @Query("select * from TbCustomerPrint")
    suspend fun getCustomerPrinterList() : List<PrinterResponse.Data.CustomerReceiptPrinters>

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

    @get:Query("select * from TbCustomerPrint")
    val getCustomerPrintList: List<PrinterResponse.Data.CustomerReceiptPrinters>

    @Query("select * from TbKitchenPrint")
    suspend fun getKitchenPrinterForPrinting() : List<PrinterResponse.Data.KitchenReceiptPrinters>

    @Update
    suspend fun updateKitchenPrinter(kitchenPrinter:PrinterResponse.Data.KitchenReceiptPrinters)

    @Update
    suspend fun updateCustomerPrinter(customerPrinter:PrinterResponse.Data.CustomerReceiptPrinters)

    //Printer room database update

    @Query("SELECT id FROM TbCustomerPrint")
    suspend fun getCustomerPrinterIds(): List<Int>

    @Query("SELECT id FROM TbKitchenPrint")
    suspend fun getKitchenPrinterIds(): List<Int>

    @Query("DELETE FROM TbCustomerPrint WHERE id IN (:ids)")
    suspend fun deleteCustomerPrintersByIds(ids: List<Int>)

    @Query("DELETE FROM TbKitchenPrint WHERE id IN (:ids)")
    suspend fun deleteKitchenPrintersByIds(ids: List<Int>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCustomerPrinters(printers: List<PrinterResponse.Data.CustomerReceiptPrinters>)

    @Update
    suspend fun updateCustomerPrinters(printers: List<PrinterResponse.Data.CustomerReceiptPrinters>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertKitchenPrinters(printers: List<PrinterResponse.Data.KitchenReceiptPrinters>)

    @Update
    suspend fun updateKitchenPrinters(printers: List<PrinterResponse.Data.KitchenReceiptPrinters>)


}