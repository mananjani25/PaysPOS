package com.android.pos.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.pos.data.entities.CashDiscountModel
import com.android.pos.data.entities.LoyaltyProgramsModel


@Dao
interface CashDiscountsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(cashDiscountModel: CashDiscountModel): Long

    @Query("select * from CashDiscount")
    fun allList(): List<CashDiscountModel>


    @Query("select * from CashDiscount WHERE CashDiscount.is_active= :active LIMIT 1")
    suspend fun getActiveCashDiscount(active: Int): CashDiscountModel

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAll(cashDiscountModel: List<CashDiscountModel>)
}