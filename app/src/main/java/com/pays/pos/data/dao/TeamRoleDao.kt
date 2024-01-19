package com.pays.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.pays.pos.data.entities.TeamRole
import com.pays.pos.data.model.responseModel.GetUserPermissionListResponse


@Dao
interface TeamRoleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addRole(roleModel: TeamRole): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAllRoles(roleList: List<TeamRole>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAllRolesSuspend(roleList: List<TeamRole>)

    @get:Query("select * from TbTeamRole where TbTeamRole.isDeleted = 0")
    val allRoles: LiveData<List<TeamRole>>

    @Query("select * from TbTeamRole where TbTeamRole.isDeleted = 0")
    fun allRoleList(): List<TeamRole>

    @Query("SELECT * from TbTeamRole where TbTeamRole.id  = :id LIMIT 1")
    fun roleById(id: Int?): LiveData<TeamRole>

    @Query("DELETE FROM TbTeamRole")
    suspend fun delete()

    @Query("DELETE FROM TbTeamRole where TbTeamRole.id  = :id")
    suspend fun deleteRoleById(id: Int)

    @Query("SELECT * FROM TbTeamRole WHERE TbTeamRole.id IN (:userIds) and  TbTeamRole.isDeleted = 0")
    fun rolesByIds(userIds: IntArray): List<TeamRole>

    /* @Query("UPDATE TbTeamRole SET isActive = :active WHERE  TbTeamRole.id = :id")
     suspend fun activeRole(id: Int, active: Boolean?): Int*/
}