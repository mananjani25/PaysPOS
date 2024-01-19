package com.pays.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pays.pos.data.entities.TbDiscount


@Dao
interface DiscountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addDiscount(discountModel: TbDiscount): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAllDiscount(discountList: List<TbDiscount>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addDiscounts(discountList: List<TbDiscount>)

    @get:Query("select * from TbDiscount where TbDiscount.isDeleted  = 0")
    val allDiscount: LiveData<List<TbDiscount>>

    @get:Query("select * from TbDiscount where TbDiscount.isDeleted  = 0 and TbDiscount.isActive = 1")
    val allActiveDiscount: LiveData<List<TbDiscount>>

    @Query("select * from TbDiscount where TbDiscount.isDeleted  = 0")
    fun allDiscountList(): List<TbDiscount>

    @Query("SELECT * from TbDiscount where TbDiscount.id  = :id LIMIT 1")
    fun discountById(id: Int?): TbDiscount

    @Query("DELETE FROM TbDiscount")
    suspend fun delete()

    @Query("DELETE FROM TbDiscount where TbDiscount.id  = :id")
    suspend fun deleteDiscountById(id: Int)

    @Query("SELECT * FROM TbDiscount WHERE TbDiscount.id IN (:userIds)")
    fun discountByIds(userIds: IntArray): List<TbDiscount>

    @Query("UPDATE TbDiscount SET isActive = :active WHERE  TbDiscount.id = :id")
    suspend fun activeDiscount(id: Int, active: Boolean?): Int
}