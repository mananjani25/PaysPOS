package com.android.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.android.pos.data.entities.ModifierSet


/**
 * Created by vishal patel on 2/3/2018.
 */
@Dao
interface ModifierSetDao {

    @Insert(onConflict = REPLACE)
    suspend fun add(modifierModel: ModifierSet?): Long

    @Insert
    suspend fun insert(modifierModel: ModifierSet)

    @Update
    fun update(modifierModel: ModifierSet)

    @Transaction
    @Insert(onConflict = REPLACE)
    suspend fun addAll(modifierModel: List<ModifierSet>)

    @get:Query("select * from ModifierSet where ModifierSet.isDeleted = 0 ORDER BY ModifierSet.sort ASC")
    val all: LiveData<List<ModifierSet>>


    @Query("select * from ModifierSet where  ModifierSet.itemIds = :id and ModifierSet.isDeleted = 0")
    fun all(id: Int?): LiveData<List<ModifierSet?>>?

    @Query("select * from ModifierSet where ModifierSet.isDeleted = 0")
    fun allModifier(): List<ModifierSet?>?

    @Query("SELECT * FROM ModifierSet WHERE id IN (:itemIds) and ModifierSet.isDeleted = 0")
    fun modifierSetByItem(itemIds: IntArray): LiveData<List<ModifierSet>>


    @Query("SELECT * from ModifierSet where ModifierSet.id  = :id  and ModifierSet.isDeleted = 0 LIMIT 1")
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
    suspend fun delete()

    @Query("SELECT * from ModifierSet  where ModifierSet.id  = :id LIMIT 1")
    fun itemOne(id: Int): ModifierSet?

    // To update associated item ids array for a modifier
    @Query("UPDATE ModifierSet SET itemIds = :itemIdsList WHERE ModifierSet.id = :modId")
    suspend fun updateModifiersItem(modId: Int, itemIdsList: List<Int>)

    @Query("UPDATE ModifierSet SET modifiers = :modifiersJSON WHERE ModifierSet.id = :modId")
    suspend fun updateModifierJSON(modId: Int, modifiersJSON: String)
}