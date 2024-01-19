package com.pays.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.pays.pos.data.entities.CartModel
import com.pays.pos.data.entities.DineInCartModel
import com.pays.pos.data.entities.TaxData
import com.pays.pos.data.entities.TbCartItem
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

    @Query("select * from TbCartItem where orderType = :orderType AND isManualSaleItem = 0 AND employeeID=:employee_Id ORDER BY timeStamp")
    fun getCartItems(orderType: String, employee_Id: Int): Flow<List<TbCartItem>>

    @Query("delete from TbCartItem where itemId = :itemId AND guestIndexForDineIn = :guestIndexForDineIn")
    suspend fun removeCartItem(itemId:Int,guestIndexForDineIn: Int)

    @Query("select Max(cartItemId) FROM TbCartItem")
    suspend fun getLatestPrimaryKey(): Int

    @Query("select * from TbCartItem ORDER BY timeStamp")
    fun getCartItems(): Flow<List<TbCartItem>>

    @Transaction
    @Query("DELETE FROM TbCartItem")
    suspend fun deleteCartItems()

    @Delete
    fun deleteCartModel(cartModel: CartModel)

    @Delete
    suspend fun deleteItemFromCartItems(cartItem: TbCartItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addCartItemsList(cartItems: List<TbCartItem>)

    @Query("select * from TbCartItem WHERE guestIndexForDineIn = :guestIndexForDineIn ORDER BY timeStamp")
    fun getDineInCartItems(guestIndexForDineIn:Int): List<TbCartItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
     fun addSuspended(cartModel: CartModel): Long?

     @Update
     fun updateCartModel(cartModel: CartModel)

     @Query("UPDATE CartModel SET taxlistDynamic = :list")
     fun updateTaxBif(list:ArrayList<TaxData>)

     @Query("select * from CartModel LIMIT 1")
     suspend fun getCurrentCartModel(): List<CartModel>

    @Query("select * from CartModel")
    fun observeCartModel(): LiveData<List<CartModel>>

    @Query("select * from CartModel")
    fun getCartModels(): List<CartModel>

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

    @Query("DELETE FROM TbCartItem where isManualSaleItem = 1 AND employeeID=:employee_Id")
    suspend fun deleteManualSaleItemsFromCartItem(employee_Id: Int)

    @Query("select * from CartModel where CartModel.orderType = :orderType AND CartModel.isMaual = 1 AND CartModel.employeeID=:employee_Id")
    fun getManualSaleItems(orderType: String, employee_Id: Int): LiveData<List<CartModel>>

    @Query("select * from TbCartItem where orderType = :orderType AND isManualSaleItem = 1 AND employeeID=:employee_Id")
    fun getManualSaleCartItems(orderType: String, employee_Id: Int): LiveData<List<TbCartItem>>

    //For Dine in Local Database
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addDineInCartDao(cartModel: DineInCartModel): Long?

    @Query("select * from DineInCartModel where DineInCartModel.orderType = :orderType AND DineInCartModel.isMaual = 0 AND DineInCartModel.employeeID=:employee_Id")
    fun allItemDineIn(orderType: String, employee_Id: Int): LiveData<List<DineInCartModel>>

    @Query("DELETE FROM DineInCartModel")
    suspend fun deleteDineInCart()


    @Query("UPDATE CartModel SET customer = null WHERE cartId =:id")
    fun removeCustomer(id: Int)




}