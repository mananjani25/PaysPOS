package com.android.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.pos.data.model.CustomerListResponse

@Dao
interface CustomerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAllCustomer(customerList: List<CustomerListResponse.Data>)

    @get:Query("select * from TbCustomer")
    val allCustomer: LiveData<List<CustomerListResponse.Data>>

    @Query("select * from TbCustomer")
    fun allCustomerList(): List<CustomerListResponse.Data>

    @Query("DELETE FROM TbCustomer where TbCustomer.id = :id")
    suspend fun deleteCustomerByID(id: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addCustomer(customerModel: CustomerListResponse.Data): Long

    @Query("UPDATE TbCustomer SET first_name = :fname,last_name = :lname,birth_date = :bDate,email = :email,phones = :phones,addresses = :address WHERE TbCustomer.id =:id")
    suspend fun updateCustomer(
        id: Int,
        fname: String,
        lname: String,
        bDate: String,
        email: String,
        phones: List<CustomerListResponse.Data.Phones>,
        address: List<CustomerListResponse.Data.Addresses>
    )


}
