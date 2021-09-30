package com.android.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.android.pos.data.entities.TbServiceCharge
import com.android.pos.data.model.responseModel.GetServiceChargeResponse
import com.android.pos.data.model.responseModel.GetTipReponse


@Dao
interface ServiceChargeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addServiceCharge(serviceChargeModel: TbServiceCharge): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAllServiceCharge(serviceChargeList: List<TbServiceCharge>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addServiceCharges(serviceChargeList: List<TbServiceCharge>)

    @get:Query("select * from TbServiceCharge")
    val allServiceCharge: LiveData<List<TbServiceCharge>>

    @Query("select * from TbServiceCharge")
    fun allServiceChargeList(): List<TbServiceCharge>

    @Query("SELECT * from TbServiceCharge where TbServiceCharge.id  = :id LIMIT 1")
    fun serviceChargeById(id: Int?): TbServiceCharge

    @Query("DELETE FROM TbServiceCharge")
    suspend fun delete()

    @Query("DELETE FROM TbServiceCharge where TbServiceCharge.id  = :id")
    suspend fun deleteServiceChargeById(id: Int)

    @Query("SELECT * FROM TbServiceCharge WHERE TbServiceCharge.id IN (:userIds)")
    fun serviceChargesByIds(userIds: IntArray): List<TbServiceCharge>

    @Query("UPDATE TbServiceCharge SET isEnabled = :active WHERE  TbServiceCharge.id = :id")
    suspend fun activeServiceCharge(id: Int, active: Boolean?): Int
}