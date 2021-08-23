package com.android.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.pos.data.entities.CartModel

/**
 * Created by vishal patel on 2/3/2018.
 */
@Dao
interface CartDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(cartModel: CartModel): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAllItem(elementsBeanList: List<CartModel>)

    @Query("select * from CartModel where CartModel.orderType = :orderType AND CartModel.isMaual = 0")
    fun allItem(orderType: String): LiveData<List<CartModel>>


    @Query("select * from CartModel where CartModel.isOpenOrder = 0 ")
    suspend fun cartList(): List<CartModel>

    @Query("DELETE FROM CartModel where CartModel.isOpenOrder = 0 AND CartModel.isMaual = 0")
    suspend fun delete()

    @get:Query("select * from CartModel where CartModel.isMaual = 1")
    val manualItem: LiveData<List<CartModel>>

    @Query("DELETE FROM CartModel where CartModel.isMaual = 1")
    suspend fun deleteManualSale()


}