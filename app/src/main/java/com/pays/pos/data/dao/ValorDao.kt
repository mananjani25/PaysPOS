package com.pays.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pays.pos.data.entities.TbBusinessDetails
import com.pays.pos.data.model.ValorModel


@Dao
interface ValorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(valor: ValorModel): Long

    @Query("select * from valor")
    fun allList(): List<ValorModel>
}