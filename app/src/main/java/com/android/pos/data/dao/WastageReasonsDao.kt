package com.android.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.pos.data.model.responseModel.VenueDetailsResponse


@Dao
interface WastageReasonsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAllWastageReasons(wastageReasonsList: List<VenueDetailsResponse.Data.WastageReason>)

    @get:Query("select * from TbWastageReason where TbWastageReason.isActive = 1 AND TbWastageReason.deletedAt IS NULL")
    val allWastageReasons: LiveData<List<VenueDetailsResponse.Data.WastageReason>>

}