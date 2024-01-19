package com.pays.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pays.pos.data.entities.TbBusinessDetails


@Dao
interface BusinessDetailsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(address: TbBusinessDetails): Long

    @Query("select * from TbBusinessDetails")
    fun allList(): List<TbBusinessDetails>


    @get:Query("select * from TbBusinessDetails")
    val allData: LiveData<TbBusinessDetails>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAll(cashDiscountModel: List<TbBusinessDetails>)

    @Query("DELETE FROM TbBusinessDetails")
    suspend fun delete()
}