package com.android.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.android.pos.data.entities.CategoryWithInventory
import com.android.pos.data.entities.TbCategory
import com.android.pos.utils.swipereveallayout.SwipeRevealLayout


/**
 * Created by vishal patel on 2/3/2018.
 */
@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(categoryModel: TbCategory?): Long

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAll(categoryModel: List<TbCategory>)

    @Query("select * from TbCategory where TbCategory.active = 1 and TbCategory.isDeleted = 0 and TbCategory.name != 'Manual Sales' ORDER BY TbCategory.sort ASC")
    fun all(): LiveData<List<TbCategory>>

    @Query("select * from TbCategory where TbCategory.active = 1 and TbCategory.isDeleted = 0 and TbCategory.name != 'Manual Sales' and TbCategory.name != 'GIFT CARD' ORDER BY TbCategory.sort ASC")
    fun allWithoutGiftCard(): LiveData<List<TbCategory>>

    @Query("select * from TbCategory where TbCategory.active = 1 and TbCategory.isDeleted = 0 and TbCategory.name != 'Manual Sales' and TbCategory.name != 'Items Without Category' ORDER BY TbCategory.sort ASC")
    fun allCatWithoutItem(): LiveData<List<TbCategory>>

    @get:Query("select * from TbCategory where TbCategory.active = 0 and TbCategory.isDeleted = 0 and TbCategory.name != 'Manual Sales' ORDER BY TbCategory.sort DESC")
    val unhideCategory: LiveData<List<TbCategory>>

    @Query("select * from TbCategory where TbCategory.active = 1 and TbCategory.isDeleted = 0 and TbCategory.id = :id LIMIT 1")
    fun getCategory(id: Int): LiveData<TbCategory>

    @Query("SELECT * from TbCategory where TbCategory.id  = :id  and TbCategory.isDeleted = 0 LIMIT 1")
    fun categoryById(id: Int?): TbCategory?

    @Query("SELECT sort from TbCategory")
    fun getAllSortNumbers(): List<Int>

    @Query("SELECT * from TbCategory  LIMIT 1")
    fun categoryOne(): TbCategory?

    @Query("SELECT * from TbCategory where TbCategory.id  = :restId and TbCategory.isDeleted = 0 LIMIT 1")
    fun categoryByRestId(restId: Int?): TbCategory?

    @Query("SELECT * from TbCategory where TbCategory.id  = :id and TbCategory.id = :id1 and TbCategory.isDeleted = 0 LIMIT 1")
    fun categoryById(id: Int?, id1: Int?): TbCategory?

    @Query("DELETE FROM TbCategory where TbCategory.id  = :id")
    suspend fun deleteCategoryById(id: Int?)

    @Query("UPDATE TbCategory SET item_ids = :itemsIdList WHERE  TbCategory.id = :catId ")
    suspend fun updateCategoryList(catId: Int, itemsIdList : List<Int>)

    @Query("DELETE FROM TbCategory")
    suspend fun delete()

    @Query("DELETE FROM TbItem")
    suspend fun deleteItem()

    @Query("DELETE FROM TbTax")
    suspend fun deleteTax()

    @Query("DELETE FROM TbTips")
    suspend fun deleteTip()

    @Query("DELETE FROM TbDiscount")
    suspend fun deleteDiscount()

    @Query("DELETE FROM TbNotes")
    suspend fun deleteNote()

    @Query("DELETE FROM TbServiceCharge")
    suspend fun deleteSc()

    @Query("DELETE FROM CartModel")
    suspend fun deleteCart()

    @Query("DELETE FROM TbEmployee")
    suspend fun deleteEmp()

    @Query("DELETE FROM TbCustomer")
    suspend fun deleteCustomerTb()

    @Query("DELETE FROM TbTeamRole")
    suspend fun deleteTeamRole()

    @Query("DELETE FROM TbModule")
    suspend fun deleteModule()

    @Query("DELETE FROM ModifierSet")
    suspend fun deleteModifierSet()

    @Query("DELETE FROM OptionSet")
    suspend fun deleteOptionSet()

    @Query("DELETE FROM TbOrderType")
    suspend fun deleteOT()

    @Query("DELETE FROM TbTerminals")
    suspend fun deleteTerminal()

    @Query("DELETE FROM ItemModifierSets")
    suspend fun deleteModifierSets()

    @Query("DELETE FROM TbKitchenPrint")
    suspend fun deleteKitchenPrinters()

    @Query("DELETE FROM TbCustomerPrint")
    suspend fun deleteCustomerPrinters()

    @Query("DELETE FROM TbKitchenSettings")
    suspend fun deleteKS()

    @Query("DELETE FROM TbCustomerSettings")
    suspend fun deleteCS()

    @Query("DELETE FROM TbCancelOrderReason")
    suspend fun deleteCOR()

    @Query("DELETE FROM CashDiscount")
    suspend fun deleteCD()

    @Query("DELETE FROM TbCountryList")
    suspend fun deleteCL()

    @Query("DELETE FROM TbTimeZones")
    suspend fun deleteTZ()

    @Query("DELETE FROM TbBusinessDetails")
    suspend fun deleteBD()

    @Query("DELETE FROM PRINTERQUEUE")
    suspend fun deletePrinterQueue()

    @Transaction
    suspend fun delete1() {
        delete()
        deleteItem()
        deleteTax()
        deleteTip()
        deleteDiscount()
        deleteNote()
        deleteSc()
        deleteCart()
        deleteEmp()
        deleteCustomerTb()
        deleteTeamRole()
        deleteModule()
        deleteModifierSet()
        deleteOptionSet()

        deleteOT()
        deleteTerminal()
        deleteModifierSets()
        deleteKitchenPrinters()
        deleteCustomerPrinters()
        deleteKS()
        deleteCS()
        deleteCOR()
        deleteCD()

        deleteCL()
        deleteTZ()
        deleteBD()
        deletePrinterQueue()
    }

    @Query("UPDATE TbCategory SET sort = :sort WHERE  TbCategory.name = :name ")
    suspend fun updateSorting(name: String, sort: Int?): Int

    @get:Query("SELECT * from TbCategory WHERE TbCategory.name == 'Manual Sales' and TbCategory.isDeleted = 0 LIMIT 1")
    val manualCategoryId: LiveData<TbCategory>

    @Query("UPDATE TbCategory SET active = :active WHERE  TbCategory.id = :id ")
    suspend fun hideCategory(id: Int, active: Boolean?): Int

    @Transaction
    @Query("SELECT * FROM TbCategory where TbCategory.active = 1 and TbCategory.isDeleted = 0 and TbCategory.name != 'Manual Sales' ORDER BY TbCategory.sort ASC")
    fun categoryWithInventory(): LiveData<List<CategoryWithInventory?>>?

    @Query("SELECT * FROM TbCategory WHERE TbCategory.id IN (:userIds) and TbCategory.isDeleted = 0")
    fun categoryByIds(userIds: IntArray): List<TbCategory?>?


}