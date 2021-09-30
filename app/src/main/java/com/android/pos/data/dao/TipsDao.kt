package com.android.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.android.pos.data.model.responseModel.GetTipReponse


@Dao
interface TipsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTips(tipModel: GetTipReponse.Data): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAllTips(tipList: List<GetTipReponse.Data>)

    @get:Query("select * from TbTips")
    val allTips: LiveData<List<GetTipReponse.Data>>

    @Query("select * from TbTips")
    fun allTipsList(): List<GetTipReponse.Data>

    @Query("SELECT * from TbTips where TbTips.id  = :id LIMIT 1")
    fun tipsById(id: Int?): GetTipReponse.Data

    @Query("DELETE FROM TbTips")
    suspend fun delete()

    @Query("DELETE FROM TbTips where TbTips.id  = :id")
    suspend fun deleteTipById(id: Int)

    @Query("SELECT * FROM TbTips WHERE TbTips.id IN (:userIds)")
    fun tipsByIds(userIds: IntArray): List<GetTipReponse.Data>

    @Query("UPDATE TbTips SET isActive = :active WHERE  TbTips.id = :id")
    suspend fun activeTip(id: Int, active: Boolean?): Int
}