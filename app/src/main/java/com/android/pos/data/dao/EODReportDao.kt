package com.android.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.pos.data.model.responseModel.EODShiftReport
import com.android.pos.data.model.responseModel.EodReportResponse

@Dao
interface EODReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addEODReportSettings(model: EODShiftReport): Long

    @get:Query("select * from EODSHIFTREPORT")
    val eodSettingsData: LiveData<EodReportResponse>

    @Query("DELETE FROM EODSHIFTREPORT")
    suspend fun deleteEODReportSettings()
}