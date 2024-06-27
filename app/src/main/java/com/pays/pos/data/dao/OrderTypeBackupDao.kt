package com.pays.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.pays.pos.data.entities.*
import kotlinx.coroutines.flow.Flow

/**
 * Created by Rahul Sharma on 20/06/2024.
 */
@Dao
interface OrderTypeBackupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(orderTypeBackup: OrderTypeBackup): Long?

    @Query("select * from OrderTypeBackup WHERE employeeId=:employeeId")
    fun getOrderTypeBackupList(employeeId: Int): List<OrderTypeBackup>

    @Query("select * from OrderTypeBackup WHERE orderTypeName=:orderTypeName AND orderType=:orderType AND employeeId=:employeeId")
    fun findOrderTypeBackup(
        orderType: Int,
        orderTypeName: String,
        employeeId: Int
    ): List<OrderTypeBackup>

    @Query("DELETE FROM OrderTypeBackup WHERE orderType = :orderType AND employeeId =:employeeId")
    suspend fun deleteOrderTypeBackup(orderType: Int, employeeId: Int)

    @Query("DELETE FROM OrderTypeBackup WHERE employeeId =:employeeId")
    suspend fun deleteOrderTypeBackupByName(employeeId: Int)

    @Query("UPDATE OrderTypeBackup SET orderType = :orderType , orderTypeName = :orderTypeName WHERE employeeId =:employeeId")
    suspend fun updateOrderTypeBackup(orderType: Int, orderTypeName: String, employeeId: Int)

}