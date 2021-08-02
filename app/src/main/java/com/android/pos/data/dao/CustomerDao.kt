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
    suspend fun deleteEmployeeByID(id: Int)

}
