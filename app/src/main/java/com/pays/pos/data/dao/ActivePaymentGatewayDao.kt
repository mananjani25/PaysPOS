package com.pays.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pays.pos.data.entities.ActivePaymentGateway
import com.pays.pos.data.entities.TbDiscount


@Dao
interface ActivePaymentGatewayDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivePaymentGateway(activePaymentGateway: ActivePaymentGateway): Long

    @Query("select * from active_payment_gateway")
    fun getActivePaymentGateway(): List<ActivePaymentGateway>

    @Query("DELETE FROM active_payment_gateway")
    suspend fun delete()

//    @Query("UPDATE active_payment_gateway SET type = :type WHERE id = :id")
    @Update
    suspend fun updateActivePayment(activePaymentGateway: ActivePaymentGateway): Int
}