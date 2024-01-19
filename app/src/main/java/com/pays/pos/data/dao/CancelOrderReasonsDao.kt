package com.pays.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pays.pos.data.model.responseModel.VenueDetailsResponse


@Dao
interface CancelOrderReasonsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addCancelOrderReason(cancelOrderReasonsModel: VenueDetailsResponse.Data.CancelOrderReason): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAllCancelOrderReasons(cancelOrderReasonsList: List<VenueDetailsResponse.Data.CancelOrderReason>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAllCancelOrderReasonsSuspend(cancelOrderReasonsList: List<VenueDetailsResponse.Data.CancelOrderReason>)

    @get:Query("select * from TbCancelOrderReason where TbCancelOrderReason.isActive = 1 and TbCancelOrderReason.isDeleted = 0")
    val allCancelOrderReasons: LiveData<List<VenueDetailsResponse.Data.CancelOrderReason>>

    @Query("select * from TbCancelOrderReason")
    fun allCancelOrderReasonList(): List<VenueDetailsResponse.Data.CancelOrderReason>

    @Query("SELECT * from TbCancelOrderReason where TbCancelOrderReason.id  = :id LIMIT 1")
    fun cancelOrderReasonById(id: Int?): LiveData<VenueDetailsResponse.Data.CancelOrderReason>

    @Query("DELETE FROM TbCancelOrderReason")
    suspend fun delete()

    @Query("DELETE FROM TbCancelOrderReason where TbCancelOrderReason.id  = :id")
    suspend fun deleteRoleById(id: Int)

    @Query("SELECT * FROM TbCancelOrderReason WHERE TbCancelOrderReason.id IN (:userIds)")
    fun cancelOrderReasonsByIds(userIds: IntArray): List<VenueDetailsResponse.Data.CancelOrderReason>

    /* @Query("UPDATE TbVenueDetailsResponse.Data.CancelOrderReason SET isActive = :active WHERE  TbVenueDetailsResponse.Data.CancelOrderReason.id = :id")
     suspend fun activeRole(id: Int, active: Boolean?): Int*/
}