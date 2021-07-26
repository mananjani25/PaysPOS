package com.android.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.android.pos.data.entities.CategoryWithInventory
import com.android.pos.data.entities.TbCategory


/**
 * Created by vishal patel on 2/3/2018.
 */
@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(categoryModel: TbCategory?): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAll(categoryModel: List<TbCategory>)

    @Query("select * from TbCategory where TbCategory.active = 1 ORDER BY TbCategory.sort ASC")
    fun all(): LiveData<List<TbCategory>>

    @get:Query("select * from TbCategory where TbCategory.active = 1 ORDER BY TbCategory.sort ASC")
    val allHideCategory: LiveData<List<TbCategory?>>?

    @Query("SELECT * from TbCategory where TbCategory.id  = :id LIMIT 1")
    fun categoryById(id: Int?): TbCategory?

    @Query("SELECT * from TbCategory LIMIT 1")
    fun categoryOne(): TbCategory?

    @Query("SELECT * from TbCategory where TbCategory.id  = :restId LIMIT 1")
    fun categoryByRestId(restId: Int?): TbCategory?

    @Query("SELECT * from TbCategory where TbCategory.id  = :id and TbCategory.id = :id1 LIMIT 1")
    fun categoryById(id: Int?, id1: Int?): TbCategory?

    @Query("DELETE FROM TbCategory where TbCategory.id  = :id")
    suspend fun deleteCategoryById(id: Int?)

    @Query("DELETE FROM TbCategory")
    fun delete()

    @Query("UPDATE TbCategory SET sort = :sort WHERE  TbCategory.id = :id")
    fun updateSorting(id: Int, sort: Int?): Int

    @Query("UPDATE TbCategory SET active = :active WHERE  TbCategory.id = :id")
    suspend fun hideCategory(id: Int, active: Boolean?): Int

    @Transaction
    @Query("SELECT * FROM TbCategory")
    fun categoryWithInventory(): LiveData<List<CategoryWithInventory?>>?

    @Query("SELECT * FROM TbCategory WHERE TbCategory.id IN (:userIds)")
    fun categoryByIds(userIds: IntArray): List<TbCategory?>?
}