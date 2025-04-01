package com.pays.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pays.pos.data.model.responseModel.VenueDetailsResponse


@Dao
interface WastageReasonsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAllWastageReasons(wastageReasonsList: List<VenueDetailsResponse.Data.WastageReason>)

    @get:Query("select * from TbWastageReason where TbWastageReason.isActive = 1 AND TbWastageReason.deletedAt IS NULL")
    val allWastageReasons: LiveData<List<VenueDetailsResponse.Data.WastageReason>>

    @Query("DELETE FROM TbWastageReason")
    suspend fun delete()


    @Query("DELETE FROM TbWastageReason where TbWastageReason.id  = :id")
    suspend fun deleteWastageReasonById(id: Int)


    @Query("select * from TbWastageReason")
    fun allWastageReasonList(): List<VenueDetailsResponse.Data.WastageReason>


}