package com.android.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.android.pos.data.entities.CartModel
import com.android.pos.data.entities.DineInCartModel
import com.android.pos.data.entities.TbCartItem
import kotlinx.coroutines.flow.Flow

/**
 * Created by vishal patel on 2/3/2018.
 */
@Dao
interface CartDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(cartModel: CartModel): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addCartItem(cartItem: TbCartItem): Long?

    @Query("select * from TbCartItem ORDER BY timeStamp")
    fun getCartItems(): Flow<List<TbCartItem>>

    @Transaction
    @Query("DELETE FROM TbCartItem")
    suspend fun deleteCartItems()

    @Delete
    suspend fun deleteItemFromCartItems(cartItem: TbCartItem)

    @Insert
    suspend fun addCartItemsList(cartItems: List<TbCartItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
     fun addSuspended(cartModel: CartModel): Long?

     @Update
     fun updateCartModel(cartModel: CartModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAllItem(elementsBeanList: List<CartModel>)

    @Query("select * from CartModel where CartModel.orderType = :orderType AND CartModel.isMaual = 0 AND CartModel.employeeID=:employee_Id")
    fun allItem(orderType: String, employee_Id: Int): LiveData<List<CartModel>>

    @Query("select * from CartModel where CartModel.orderType = :orderType AND CartModel.isMaual = 0 AND CartModel.employeeID=:employee_Id")
    fun allItemFlow(orderType: String, employee_Id: Int): Flow<List<CartModel>>

    @Query("select * from CartModel where CartModel.isMaual = 0 AND CartModel.employeeID=:employee_Id")
    fun allItemMod(employee_Id: Int): List<CartModel>

    @Query("DELETE FROM CartModel where CartModel.employeeID=:employee_Id")
    suspend fun delete(employee_Id: Int)

    @Transaction
    @Query("DELETE FROM CartModel")
    suspend fun delete()

    @Query("select * from CartModel where CartModel.isMaual = 1 AND CartModel.employeeID=:employee_Id")
    fun manualItem(employee_Id: Int): LiveData<List<CartModel>>

    @Query("DELETE FROM CartModel where CartModel.isMaual = 1 AND CartModel.employeeID=:employee_Id")
    suspend fun deleteManualSale(employee_Id: Int)

    @Query("select * from CartModel where CartModel.orderType = :orderType AND CartModel.isMaual = 1 AND CartModel.employeeID=:employee_Id")
    fun getManualSaleItems(orderType: String, employee_Id: Int): LiveData<List<CartModel>>

    //For Dine in Local Database
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addDineInCartDao(cartModel: DineInCartModel): Long?

    @Query("select * from DineInCartModel where DineInCartModel.orderType = :orderType AND DineInCartModel.isMaual = 0 AND DineInCartModel.employeeID=:employee_Id")
    fun allItemDineIn(orderType: String, employee_Id: Int): LiveData<List<DineInCartModel>>

    @Query("DELETE FROM DineInCartModel")
    suspend fun deleteDineInCart()





}