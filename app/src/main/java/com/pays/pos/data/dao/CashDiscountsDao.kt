package com.pays.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pays.pos.data.entities.CashDiscountModel
import com.pays.pos.data.entities.LoyaltyProgramsModel


@Dao
interface CashDiscountsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(cashDiscountModel: CashDiscountModel): Long

    @Query("select * from CashDiscount")
    fun allList(): List<CashDiscountModel>


    @Query("select * from CashDiscount WHERE CashDiscount.is_active= :active LIMIT 1")
    fun getActiveCashDiscount(active: Int): LiveData<CashDiscountModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAll(cashDiscountModel: List<CashDiscountModel>)

    @Query("DELETE FROM CashDiscount")
    suspend fun delete()
}