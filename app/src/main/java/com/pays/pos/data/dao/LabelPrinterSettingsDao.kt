package com.pays.pos.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pays.pos.data.entities.TbLabelPrinterSettings

@Dao
interface LabelPrinterSettingsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(entity: TbLabelPrinterSettings)

    @Query("SELECT * FROM TbLabelPrinterSettings LIMIT 1")
    suspend fun getLabelPrinterSettingsData(): TbLabelPrinterSettings

    @Query("DELETE FROM TbLabelPrinterSettings")
    suspend fun delete()
}