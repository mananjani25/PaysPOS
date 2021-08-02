package com.android.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.android.pos.data.model.responseModel.GetServiceChargeResponse
import com.android.pos.data.model.responseModel.GetTipReponse


@Dao
interface ServiceChargeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addServiceCharge(serviceChargeModel: GetServiceChargeResponse.Data): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAllServiceCharge(serviceChargeList: List<GetServiceChargeResponse.Data>)

    @get:Query("select * from TbServiceCharge")
    val allServiceCharge: LiveData<List<GetServiceChargeResponse.Data>>

    @Query("select * from TbServiceCharge")
    fun allServiceChargeList(): List<GetServiceChargeResponse.Data>

    @Query("SELECT * from TbServiceCharge where TbServiceCharge.id  = :id LIMIT 1")
    fun serviceChargeById(id: Int?): GetServiceChargeResponse.Data

    @Query("DELETE FROM TbServiceCharge")
    fun delete()

    @Query("DELETE FROM TbServiceCharge where TbServiceCharge.id  = :id")
    suspend fun deleteServiceChargeById(id: Int)

    @Query("SELECT * FROM TbServiceCharge WHERE TbServiceCharge.id IN (:userIds)")
    fun serviceChargesByIds(userIds: IntArray): List<GetServiceChargeResponse.Data>

    @Query("UPDATE TbServiceCharge SET isEnabled = :active WHERE  TbServiceCharge.id = :id")
    suspend fun activeServiceCharge(id: Int, active: Boolean?): Int
}