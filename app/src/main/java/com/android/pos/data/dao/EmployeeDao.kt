package com.android.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.android.pos.data.entities.Employee


@Dao
interface EmployeeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addEmployee(employeeModel: Employee): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAllEmployee(employeeList: List<Employee>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAllEmployeeSuspend(employeeList: List<Employee>)

    @get:Query("select * from TbEmployee where TbEmployee.isDeleted = 0")
    val allEmployee: LiveData<List<Employee>>

    @Query("select * from TbEmployee where TbEmployee.locationId  = :id and TbEmployee.isDeleted = 0")
    fun allEmployeeLocationWise(id: Int?): LiveData<List<Employee>>

    @Query("select * from TbEmployee")
    fun allEmployeeList(): List<Employee>

    @Query("SELECT * from TbEmployee where TbEmployee.id  = :id LIMIT 1")
    fun employeeById(id: Int?): LiveData<Employee>

    @Query("DELETE FROM TbEmployee")
    suspend fun delete()

    @Query("DELETE FROM TbEmployee where TbEmployee.id  = :id")
    suspend fun deleteEmployeeById(id: Int)

    @Query("SELECT * FROM TbEmployee WHERE TbEmployee.id IN (:userIds)")
    fun employeeByIds(userIds: IntArray): List<Employee>

    @Query("UPDATE TbEmployee SET isActive = :active WHERE  TbEmployee.id = :id")
    suspend fun activeEmployee(id: Int, active: Boolean?): Int

    @Query("SELECT * FROM TbEmployee WHERE TbEmployee.isDeleted = 0 and (TbEmployee.email LIKE '%' || :query || '%' OR TbEmployee.name LIKE '%' || :query || '%' OR TbEmployee.phoneNumber LIKE '%' || :query || '%')")
    fun getEmployeeSearchResults(query: String): LiveData<List<Employee>>
}