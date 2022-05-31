package com.android.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.pos.data.model.ShiftRportConfiguration

@Dao
interface EODReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addEODReportSettings(model: ShiftRportConfiguration): Long

    @get:Query("select * from EODSHIFTREPORT")
    val eodSettingsData: LiveData<ShiftRportConfiguration>

    @Query("DELETE FROM EODSHIFTREPORT")
    suspend fun deleteEODReportSettings()
}