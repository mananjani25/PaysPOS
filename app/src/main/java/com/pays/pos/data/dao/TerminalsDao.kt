package com.pays.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.pays.pos.data.model.responseModel.VenueDetailsResponse


@Dao
interface TerminalsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTerminals(terminalModel: VenueDetailsResponse.Data.Terminal): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAllTerminals(terminalList: List<VenueDetailsResponse.Data.Terminal>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAllTerminalSuspend(terminalList: List<VenueDetailsResponse.Data.Terminal>)

    @get:Query("select * from TbTerminals")
    val allTerminal: LiveData<List<VenueDetailsResponse.Data.Terminal>>

    @Query("select * from TbTerminals")
    fun allTerminalsList(): List<VenueDetailsResponse.Data.Terminal>

    @Query("SELECT * from TbTerminals where TbTerminals.id  = :id LIMIT 1")
    fun terminalsById(id: Int?): VenueDetailsResponse.Data.Terminal

    @Query("DELETE FROM TbTerminals")
    suspend fun delete()

    @Query("DELETE FROM TbTerminals where TbTerminals.id  = :id")
    suspend fun deleteTerminalById(id: Int)

    @Query("SELECT * FROM TbTerminals WHERE TbTerminals.id IN (:userIds)")
    fun terminalsByIds(userIds: IntArray): List<VenueDetailsResponse.Data.Terminal>

}