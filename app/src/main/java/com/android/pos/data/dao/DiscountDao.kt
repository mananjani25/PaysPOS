package com.android.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.android.pos.data.model.responseModel.GetDiscountResponse


@Dao
interface DiscountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addDiscount(discountModel: GetDiscountResponse.Data): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAllDiscount(discountList: List<GetDiscountResponse.Data>)

    @get:Query("select * from TbDiscount")
    val allDiscount: LiveData<List<GetDiscountResponse.Data>>

    @Query("select * from TbDiscount")
    fun allDiscountList(): List<GetDiscountResponse.Data>

    @Query("SELECT * from TbDiscount where TbDiscount.id  = :id LIMIT 1")
    fun discountById(id: Int?): GetDiscountResponse.Data

    @Query("DELETE FROM TbDiscount")
    fun delete()

    @Query("DELETE FROM TbDiscount where TbDiscount.id  = :id")
    suspend fun deleteDiscountById(id: Int)

    @Query("SELECT * FROM TbDiscount WHERE TbDiscount.id IN (:userIds)")
    fun discountByIds(userIds: IntArray): List<GetDiscountResponse.Data>

    @Query("UPDATE TbDiscount SET isActive = :active WHERE  TbDiscount.id = :id")
    suspend fun activeDiscount(id: Int, active: Boolean?): Int
}