package com.app.iPos.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pays.pos.data.entities.TbLevelAddon


/**
 * Created by vishal patel on 2/3/2018.
 */
@Dao
interface DBLevelAddonDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addLevelAddon(serviceCharge: TbLevelAddon?): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAllLevelAddon(serviceChargeList: List<TbLevelAddon>)

    @get:Query("select * from TbLevelAddon")
    val allLevelAddon: LiveData<List<TbLevelAddon?>>?

    @Query("select * from TbLevelAddon")
    fun allLevelAddonList(): List<TbLevelAddon?>?

    @Query("SELECT * from TbLevelAddon where TbLevelAddon.inventoryParentId  = :inventoryParentId and TbLevelAddon.levelAddonId  = :levelAddonId LIMIT 1")
    fun levelAddonById(inventoryParentId: Int?, levelAddonId: Int?): TbLevelAddon?

    @Query("DELETE FROM TbLevelAddon where TbLevelAddon.inventoryId  = :id")
    fun deleteAddonLevelById(id: Int?)


    @Query("DELETE FROM TbLevelAddon")
    fun delete()

    @Query("UPDATE TbLevelAddon SET itemQuantity = :qty WHERE  TbLevelAddon.inventoryId = :inventoryId and TbLevelAddon.categoryId =:categoryId and TbLevelAddon.inventoryParentId =:inventoryParentId")
    fun updateAddonLevelQty(inventoryId: Int?, categoryId: Int?, inventoryParentId: Int?, qty: Int?)

}