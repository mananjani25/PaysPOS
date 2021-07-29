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
    fun add(cartModel: CartModel): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAllItem(elementsBeanList: List<CartModel>)

    @get:Query("select * from CartModel where CartModel.isOpenOrder = 0 ")
    val allItem: LiveData<List<CartModel?>>?


    @get:Query("select * from CartModel where CartModel.isOpenOrder = 0 ")
    val cartList: List<CartModel>
}