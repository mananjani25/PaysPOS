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
    suspend fun add(categoryModel: TbCategory?): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAll(categoryModel: List<TbCategory>)

    @Query("select * from TbCategory where TbCategory.active = 1 and TbCategory.name != 'Manual Sales' ORDER BY TbCategory.sort ASC")
    fun all(): LiveData<List<TbCategory>>

    @get:Query("select * from TbCategory where TbCategory.active = 0 and TbCategory.name != 'Manual Sales' ORDER BY TbCategory.sort DESC")
    val unhideCategory: LiveData<List<TbCategory>>

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
    suspend fun delete()

    @Query("UPDATE TbCategory SET sort = :sort WHERE  TbCategory.id = :id")
    fun updateSorting(id: Int, sort: Int?): Int

    @get:Query("SELECT * from TbCategory WHERE TbCategory.name == 'Manual Sales' LIMIT 1")
    val manualCategoryId: LiveData<TbCategory>

    @Query("UPDATE TbCategory SET active = :active WHERE  TbCategory.id = :id")
    suspend fun hideCategory(id: Int, active: Boolean?): Int

    @Transaction
    @Query("SELECT * FROM TbCategory where TbCategory.active = 1 and TbCategory.name != 'Manual Sales' ORDER BY TbCategory.sort DESC")
    fun categoryWithInventory(): LiveData<List<CategoryWithInventory?>>?

    @Query("SELECT * FROM TbCategory WHERE TbCategory.id IN (:userIds)")
    fun categoryByIds(userIds: IntArray): List<TbCategory?>?


}