package com.pays.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pays.pos.data.entities.*


@Dao
interface TimeZonesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(address: TbTimeZones): Long

    @Query("select * from TbTimeZones")
    fun allList(): List<TbTimeZones>



    @get:Query("select * from TbTimeZones ")
    val allItem: LiveData<List<TbTimeZones>>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAll(cashDiscountModel: List<TbTimeZones>)

    @Query("DELETE FROM TbTimeZones")
    suspend fun delete()
}