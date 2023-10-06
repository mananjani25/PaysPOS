package com.android.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.pos.data.entities.PAXData

@Dao
interface PAXDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(paxData: PAXData)

    @Query("select * from PAXData")
    fun getPAXDetails(): LiveData<PAXData>

    @Query("DELETE FROM PAXData")
    suspend fun delete()

}