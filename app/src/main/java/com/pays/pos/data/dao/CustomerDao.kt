package com.pays.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pays.pos.data.entities.TbCustomer

@Dao
interface CustomerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAllCustomer(customerList: List<TbCustomer>)

    @get:Query("select * from TbCustomer ORDER BY TbCustomer.id DESC")
    val allCustomer: LiveData<List<TbCustomer>>

    @Query("select * from TbCustomer")
    fun allCustomerList(): List<TbCustomer>

    @Query("DELETE FROM TbCustomer where TbCustomer.id = :id")
    suspend fun deleteCustomerByID(id: Int?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addCustomer(customerModel: TbCustomer): Long

    @Query("UPDATE TbCustomer SET final_reward = :rewards WHERE id =:customer_id")
    suspend fun updateLoyaltyRewards(rewards: Int, customer_id: Int)

    @Query("DELETE FROM TbCustomer")
    suspend fun deleteCustomerTb()

    @Query("SELECT * from TbCustomer where TbCustomer.id = :id")
    fun getCustomerDetailsByID(id: Int?): LiveData<TbCustomer>

    @Query("SELECT COUNT(id) FROM TbCustomer")
    fun getTotalCustomersCount(): Int


    /*--------------------Customer Loyalty--------------------*/
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun addAllCustomers(customerList: List<TbCustomer>):List<Long>

    @Query("SELECT * FROM TbCustomer WHERE phones LIKE '%' || :phoneNumber || '%'")
    fun fetchCustomerFromPhoneNumber(phoneNumber:String): List<TbCustomer?>?

    @Query("SELECT EXISTS(SELECT * FROM TbCustomer)")
    fun hasItem(): Boolean
    /*--------------------Customer Loyalty--------------------*/


}
