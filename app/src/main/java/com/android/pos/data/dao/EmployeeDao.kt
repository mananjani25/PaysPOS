package com.android.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.android.pos.data.model.responseModel.EmployeeListResponse


@Dao
interface EmployeeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addEmployee(employeeModel: EmployeeListResponse.Data.Employee): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAllEmployee(employeeList: List<EmployeeListResponse.Data.Employee>)

    @get:Query("select * from TbEmployee")
    val allEmployee: LiveData<List<EmployeeListResponse.Data.Employee>>

    @Query("select * from TbEmployee")
    fun allEmployeeList(): List<EmployeeListResponse.Data.Employee>

    @Query("SELECT * from TbEmployee where TbEmployee.id  = :id LIMIT 1")
    fun employeeById(id: Int?): EmployeeListResponse.Data.Employee

    @Query("DELETE FROM TbEmployee")
    fun delete()

    @Query("DELETE FROM TbEmployee where TbEmployee.id  = :id")
    suspend fun deleteEmployeeById(id: Int)

    @Query("SELECT * FROM TbEmployee WHERE TbEmployee.id IN (:userIds)")
    fun employeeByIds(userIds: IntArray): List<EmployeeListResponse.Data.Employee>

    @Query("UPDATE TbEmployee SET isActive = :active WHERE  TbEmployee.id = :id")
    suspend fun activeEmployee(id: Int, active: Boolean?): Int
}