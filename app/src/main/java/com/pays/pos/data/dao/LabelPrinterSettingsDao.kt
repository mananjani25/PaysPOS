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

    @Query("UPDATE TbLabelPrinterSettings SET printOrderId=:printOrderId")
    suspend fun updateOrderId(printOrderId:Boolean):Int

    @Query("DELETE FROM TbLabelPrinterSettings")
    suspend fun delete()
}