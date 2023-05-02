package com.android.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.android.pos.data.entities.LoyaltyProgramsModel
import com.android.pos.data.model.responseModel.GetTipReponse


@Dao
interface LoyaltyProgramsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(tipModel: LoyaltyProgramsModel): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAll(tipList: List<LoyaltyProgramsModel>)

    @get:Query("select * from LoyaltyPrograms where LoyaltyPrograms.isDeleted = 0")
    val all: LiveData<List<LoyaltyProgramsModel>>

    @Query("select * from LoyaltyPrograms")
    fun allList(): List<LoyaltyProgramsModel>

    @Query("SELECT * from LoyaltyPrograms where LoyaltyPrograms.id  = :id LIMIT 1")
    fun byId(id: Int?): LoyaltyProgramsModel

    @Query("SELECT * from LoyaltyPrograms where LoyaltyPrograms.isEnable  = :active and LoyaltyPrograms.isDeleted = 0 LIMIT 1")
    fun findActiveLoyalty(active: Boolean?): LiveData<LoyaltyProgramsModel>

    @Query("DELETE FROM LoyaltyPrograms")
    suspend fun delete()

    @Query("DELETE FROM LoyaltyPrograms where LoyaltyPrograms.id  = :id")
    suspend fun deleteTipById(id: Int)

    @Query("UPDATE LoyaltyPrograms SET isEnable = :active WHERE  LoyaltyPrograms.id = :id")
    suspend fun activeLoyaltyProgram(id: Int, active: Boolean?): Int


}