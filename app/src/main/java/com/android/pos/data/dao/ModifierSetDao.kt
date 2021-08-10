package com.android.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import com.android.pos.data.entities.ModifierSet


/**
 * Created by vishal patel on 2/3/2018.
 */
@Dao
interface ModifierSetDao {

    @Insert(onConflict = REPLACE)
    suspend fun add(modifierModel: ModifierSet?): Long

    @Insert(onConflict = REPLACE)
    suspend fun addAll(modifierModel: List<ModifierSet>)

    @get:Query("select * from ModifierSet")
    val all: LiveData<List<ModifierSet>>


    @Query("select * from ModifierSet where  ModifierSet.itemIds = :id")
    fun all(id: Int?): LiveData<List<ModifierSet?>>?

    @Query("select * from ModifierSet")
    fun allModifier(): List<ModifierSet?>?

    @Query("SELECT * FROM ModifierSet WHERE id IN (:itemIds)")
    fun modifierSetByItem(itemIds: IntArray): LiveData<List<ModifierSet>>


    @Query("SELECT * from ModifierSet where ModifierSet.id  = :id LIMIT 1")
    fun modifierById(id: Int?): ModifierSet?

    @Query("SELECT * from ModifierSet LIMIT 1")
    fun modifierOne(): ModifierSet?

    @Query("SELECT * from ModifierSet where ModifierSet.id  = :restId LIMIT 1")
    fun modifierByRestId(restId: Int?): ModifierSet?

    @Query("SELECT * from ModifierSet where ModifierSet.id  = :id and ModifierSet.id = :id1 LIMIT 1")
    fun modifierById(id: Int?, id1: Int?): ModifierSet?

    @Query("DELETE FROM ModifierSet where ModifierSet.id  = :id")
    suspend fun delete(id: Int?)

    @Query("DELETE FROM ModifierSet")
    fun delete()

}