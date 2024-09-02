package com.pays.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.pays.pos.data.entities.*
import kotlinx.coroutines.flow.Flow


/**
 * Created by Rahul Sharma on 14/08/2024.
 */
@Dao
interface DynamicPaymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(dynamicPaymentsRecords: TbDynamicPaymentRecords?): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAll(dynamicPaymentsRecords: List<TbDynamicPaymentRecords?>?)

    @Query("select * from TbDynamicPaymentRecords")
    fun getDynamicPaymentRecords(): List<TbDynamicPaymentRecords>

    @Query("Select * from TbDynamicPaymentRecords WHERE id=:id AND isActive=1")
    fun getDynamicPaymentFromID(id:Int):TbDynamicPaymentRecords

    @Query("select * from TbDynamicPaymentRecords where isActive = :isActive AND locationId = :locationId")
    fun getDynamicPaymentRecords(isActive: Boolean, locationId: Int): Flow<List<TbDynamicPaymentRecords>>

    @get:Query("select * from TbDynamicPaymentRecords")
    val getAllDynamicPayments: List<TbDynamicPaymentRecords>

    @Query("DELETE FROM TbDynamicPaymentRecords")
    suspend fun delete()

    @Query("DELETE from TbDynamicPaymentRecords where TbDynamicPaymentRecords.id = :idList")
    suspend fun deleteDynamicPaymentById(idList: Int)
}