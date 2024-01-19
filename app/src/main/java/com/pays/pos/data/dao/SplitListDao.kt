package com.pays.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pays.pos.data.entities.TaxData
import com.pays.pos.data.model.SplitDetailListModel

@Dao
interface SplitListDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSplit(model: SplitDetailListModel): Long

    @get:Query("select * from TbSplit")
    val allSplitList: LiveData<List<SplitDetailListModel>>

    @Query("DELETE FROM TbSplit")
    suspend fun delete()

    @Query("select * from TbSplit")
    suspend fun allSplit(): List<SplitDetailListModel>


}