package com.android.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.android.pos.data.model.responseModel.CreateTaxResponse
import com.android.pos.data.model.responseModel.GetTaxResponse


@Dao
interface TaxDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addTax(taxModel: GetTaxResponse.TaxData): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAllTaxes(taxList: List<GetTaxResponse.TaxData>)

    @get:Query("select * from TbTax")
    val allTax: LiveData<List<GetTaxResponse.TaxData>>

    @Query("select * from TbTax")
    fun allTaxList(): List<GetTaxResponse.TaxData>

    @Query("SELECT * from TbTax where TbTax.id  = :id LIMIT 1")
    fun taxById(id: Int?): GetTaxResponse.TaxData

    @Query("DELETE FROM TbTax")
    fun delete()

    @Query("DELETE FROM TbTax where TbTax.id  = :id")
    suspend fun deleteTaxById(id: Int)

    @Query("SELECT * FROM TbTax WHERE TbTax.id IN (:userIds)")
    fun taxByIds(userIds: IntArray): List<GetTaxResponse.TaxData>
}