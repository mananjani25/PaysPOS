package com.app.iPos.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query


/**
 * Created by vishal patel on 2/3/2018.
 */
@Dao
interface LevelAddonDao {

//    @Insert(onConflict = REPLACE)
//    fun add(levelAddonModel: SyncCategoriesResponse.Data.LevelAddon?): Long
//
//    @Insert(onConflict = REPLACE)
//    fun addAll(levelAddonModel: List<SyncCategoriesResponse.Data.LevelAddon>)
//
//    @get:Query("select * from LevelAddon")
//    val all: LiveData<List<SyncCategoriesResponse.Data.LevelAddon?>>?
//
//    @get:Query("select * from LevelAddon")
//    val allAddonList: List<SyncCategoriesResponse.Data.LevelAddon?>?
//
//    // masterableType = "Inventory" && masterableId == inventoryId
//    @Query("select * from LevelAddon where LevelAddon.masterableType = :masterableType  AND LevelAddon.masterableId = :masterableId ORDER BY LevelAddon.sort ASC")
//    fun addonByMasterableId(
//        masterableType: String?,
//        masterableId: Int?
//    ): List<SyncCategoriesResponse.Data.LevelAddon?>?
//
//
//    @Query("SELECT * from LevelAddon where LevelAddon.id  = :id LIMIT 1")
//    fun levelAddonById(id: Int?): SyncCategoriesResponse.Data.LevelAddon?
//
//    @Query("SELECT * from LevelAddon LIMIT 1")
//    fun levelAddonOne(): SyncCategoriesResponse.Data.LevelAddon?
//
//    @Query("SELECT * from LevelAddon where LevelAddon.id  = :restId LIMIT 1")
//    fun levelAddonByRestId(restId: Int?): SyncCategoriesResponse.Data.LevelAddon?
//
//    @Query("SELECT * from LevelAddon where LevelAddon.id  = :id and LevelAddon.id = :id1 LIMIT 1")
//    fun levelAddonById(id: Int?, id1: Int?): SyncCategoriesResponse.Data.LevelAddon?
//
//    @Query("DELETE FROM LevelAddon where LevelAddon.id  = :id")
//    fun delete(id: Int?)
//
//    @Query("DELETE FROM LevelAddon")
//    fun delete()

}