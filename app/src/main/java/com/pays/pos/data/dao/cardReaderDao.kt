package com.pays.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.pays.pos.data.entities.TbCardReader


@Dao
interface cardReaderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(tbCardReader: TbCardReader)

    @Update
    suspend fun update(tbCardReader: TbCardReader)

    @Query("UPDATE TbCardReader SET status = :status WHERE  TbCardReader.mcAddress = :id")
    suspend fun updateById(status: Int, id: String): Int

    @Query("select * from TbCardReader")
    fun allList(): LiveData<TbCardReader>

    @Query("select * from TbCardReader where TbCardReader.status = 1 LIMIT 1")
    fun cardReaderActiveList(): LiveData<TbCardReader>

    @Query("SELECT * from TbCardReader where TbCardReader.mcAddress  = :mcAddress LIMIT 1")
    fun cardReaderById(mcAddress: String): LiveData<TbCardReader>


    @Query("DELETE FROM TbCardReader")
    suspend fun delete()
}