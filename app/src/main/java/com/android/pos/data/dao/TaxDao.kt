package com.android.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.pos.data.entities.TaxData


@Dao
interface TaxDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTax(taxModel: TaxData): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAllTaxes(taxList: List<TaxData>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAllTaxesSuspend(taxList: List<TaxData>)

    @get:Query("select * from TbTax")
    val allTax: LiveData<List<TaxData>>

    @get:Query("select * from TbTax where TbTax.isActive = 1")
    val enableTax: LiveData<List<TaxData>>

    @Query("select * from TbTax")
    fun allTaxList(): List<TaxData>

    @Query("SELECT * from TbTax where TbTax.id  = :id LIMIT 1")
    fun taxById(id: Int?): TaxData

    @Query("DELETE FROM TbTax")
    suspend fun delete()

    @Query("DELETE FROM TbTax where TbTax.id  = :id")
    suspend fun deleteTaxById(id: Int)

    @Query("SELECT * FROM TbTax WHERE TbTax.id IN (:userIds)")
    fun taxByIds(userIds: IntArray): List<TaxData>

    @Query("UPDATE TbTax SET isActive = :active WHERE  TbTax.id = :id")
    suspend fun activeTax(id: Int, active: Boolean?): Int
}