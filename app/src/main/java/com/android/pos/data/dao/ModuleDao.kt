package com.android.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.android.pos.data.entities.ModulePermission
import com.android.pos.data.model.responseModel.GetTeamRoleModule
import com.android.pos.data.model.responseModel.GetTipReponse


@Dao
interface ModuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addModule(tipModel: ModulePermission): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAllModules(tipList: List<ModulePermission>)

    @get:Query("select * from TbModule")
    val allModules: LiveData<List<ModulePermission>>

    @Query("select * from TbModule")
    fun allModulesList(): List<ModulePermission>

    @Query("SELECT * from TbModule where TbModule.id  = :id LIMIT 1")
    fun tipsById(id: Int?): ModulePermission

    @Query("DELETE FROM TbModule")
    suspend fun delete()

    @Query("DELETE FROM TbModule where TbModule.id  = :id")
    suspend fun deleteTipById(id: Int)

    @Query("SELECT * FROM TbModule WHERE TbModule.id IN (:userIds)")
    fun tipsByIds(userIds: IntArray): List<ModulePermission>

}