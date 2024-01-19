package com.pays.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pays.pos.data.entities.TbOrderType


@Dao
interface OrderTypeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(orderType: TbOrderType): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAll(tipList: List<TbOrderType>)

    @get:Query("select * from TbOrderType where TbOrderType.isActive = 1 and TbOrderType.isDeleted = 0 ORDER BY TbOrderType.sort ASC")
    val orderTypes: LiveData<List<TbOrderType>>

    @Query("select * from TbOrderType where TbOrderType.isActive = 1 and TbOrderType.isDeleted = 0 ORDER BY TbOrderType.sort ASC")
    fun allModulesList(): List<TbOrderType>

    @Query("SELECT * from TbOrderType where TbOrderType.id  = :id LIMIT 1")
    fun tipsById(id: Int?): TbOrderType


    @Query("SELECT id from TbOrderType where TbOrderType.orderType  = :orderTypeName LIMIT 1")
    fun orderTypeByName(orderTypeName: String?): Int

    @Query("DELETE FROM TbOrderType")
    suspend fun delete()

    @Query("DELETE FROM TbOrderType where TbOrderType.id  = :id")
    suspend fun deleteTipById(id: Int)

    @Query("SELECT * FROM TbOrderType WHERE TbOrderType.id IN (:userIds)")
    fun tipsByIds(userIds: IntArray): List<TbOrderType>

}