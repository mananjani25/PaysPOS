package com.android.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.pos.data.entities.TbItem

/**
 * Created by vishal patel on 2/3/2018.
 */
@Dao
interface DBItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(inventory: TbItem?): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAllItem(elementsBeanList: List<TbItem>)

    @get:Query("select * from TbItem where TbItem.isHide = 0 GROUP by TbItem.itemId ORDER BY TbItem.sort ASC ")
    val allItem: LiveData<List<TbItem?>>?

    @Query("select * from TbItem where TbItem.categoryId  = :id")
    fun getItemList(id: Int?): LiveData<List<TbItem?>>?

    @Query("SELECT * from TbItem where TbItem.itemId  = :id LIMIT 1")
    fun itemById(id: Int?): TbItem?

    @Query("SELECT * from TbItem LIMIT 1")
    fun itemOne(): TbItem?

    @Query("SELECT * from TbItem where TbItem.itemId  = :restId LIMIT 1")
    fun itemByInventoryId(restId: Int?): TbItem?

    @Query("SELECT * from TbItem where TbItem.itemId  = :id")
    fun itemId(id: Int?): List<TbItem?>?

    @Query("DELETE FROM TbItem where TbItem.itemId  = :id")
    suspend fun deleteItem(id: Int?)

    @Query("DELETE FROM TbItem")
    fun deleteItemTbl()

    @Query("UPDATE TbItem SET isHide = 1 WHERE  TbItem.itemId = :id")
    fun update(id: Int): Int

//    @Transaction
//    @Query("SELECT * FROM TbItem")
//    fun cartWithModifierList(): LiveData<List<ItemWithModifier?>>?
//
//    @Transaction
//    @Query("SELECT * FROM TbItem")
//    fun cartWithModifier(): List<ItemWithModifier?>?

    @Query("UPDATE TbItem SET itemQuantity = :qty WHERE  TbItem.itemId = :id")
    fun updateItemQty(id: Int?, qty: Int?)
}