package com.android.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.pos.data.entities.TbCategory
import com.android.pos.data.entities.TbItem
import com.google.android.material.tabs.TabItem

/**
 * Created by vishal patel on 2/3/2018.
 */
@Dao
interface DBItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(inventory: TbItem?): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAllItem(elementsBeanList: List<TbItem>)

    @get:Query("select * from TbItem where TbItem.isHide = 1 and TbItem.name != 'Manual Item' GROUP by TbItem.itemId ORDER BY TbItem.sort DESC ")
    val allItem: LiveData<List<TbItem?>>?

    /*@get:Query("select * from TbItem whe  re TbItem.isManualSales = 1")
    val manualItems : LiveData<List<TabItem?>>?
*/

    @get:Query("select * from TbItem where TbItem.isHide = 0 and TbItem.name != 'Manual Item' ORDER BY TbItem.sort DESC")
    val unhideItem: LiveData<List<TbItem>>

    @Query("select * from TbItem where TbItem.categoryId  = :id and TbItem.name != 'Manual Item'")
    fun getItemList(id: Int): LiveData<List<TbItem>>

    @Query("SELECT * from TbItem where TbItem.itemId  = :id and TbItem.name != 'Manual Item' LIMIT 1")
    fun itemById(id: Int?): LiveData<TbItem>?

    @Query("SELECT * from TbItem where TbItem.sku  = :productCode and TbItem.name != 'Manual Item' LIMIT 1")
    fun itemByProductCode(productCode: String): LiveData<TbItem>?

    @Query("SELECT * from TbItem LIMIT 1")
    fun itemOne(): TbItem?

    @Query("SELECT * from TbItem where TbItem.itemId  = :restId LIMIT 1")
    fun itemByInventoryId(restId: Int?): TbItem?

    @Query("SELECT * from TbItem where TbItem.itemId  = :id")
    fun itemId(id: Int?): List<TbItem?>?

    @Query("DELETE FROM TbItem where TbItem.itemId  = :id")
    suspend fun deleteItem(id: Int?)

    @Query("DELETE FROM TbItem")
    suspend fun delete()

    @Query("UPDATE TbItem SET isHide = 0 WHERE  TbItem.itemId = :id")
    suspend fun update(id: Int): Int

    @Query("UPDATE TbItem SET isHide = 1 WHERE  TbItem.itemId = :id")
    suspend fun updateShowItem(id: Int): Int


    @Query("UPDATE TbItem SET categoryId = :catId,categoryName = :catName  WHERE  TbItem.itemId = :itemId")
    suspend fun updateItem(catId: Int, catName: String, itemId: Int?): Int

//    @Transaction
//    @Query("SELECT * FROM TbItem")
//    fun cartWithModifierList(): LiveData<List<ItemWithModifier?>>?
//
//    @Transaction
//    @Query("SELECT * FROM TbItem")
//    fun cartWithModifier(): List<ItemWithModifier?>?

    @Query("SELECT itemId from TbItem where TbItem.categoryId  = :catId")
    suspend fun getListByCategory(catId: Int?): List<Int?>?

    @Query("UPDATE TbItem SET itemQuantity = :qty WHERE  TbItem.itemId = :id")
    fun updateItemQty(id: Int?, qty: Int?)

}