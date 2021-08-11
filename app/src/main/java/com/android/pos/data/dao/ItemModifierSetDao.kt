package com.android.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Update
import com.android.pos.data.entities.ItemModifierSet
import com.android.pos.data.entities.ModifierSet


/**
 * Created by vishal patel on 2/3/2018.
 */
@Dao
interface ItemModifierSetDao {

    @Insert(onConflict = REPLACE)
    suspend fun add(modifierModel: ItemModifierSet?): Long

    @Insert
    suspend fun insert(modifierModel: ItemModifierSet)

    @Update
    fun update(modifierModel: ItemModifierSet)

    @Insert(onConflict = REPLACE)
    suspend fun addAll(modifierModel: List<ItemModifierSet>)

    @get:Query("select * from ItemModifierSet ORDER BY ItemModifierSet.sort ASC")
    val all: LiveData<List<ItemModifierSet>>


    @Query("select * from ItemModifierSet where  ItemModifierSet.itemIds = :id")
    fun all(id: Int?): LiveData<List<ItemModifierSet?>>?

    @Query("select * from ItemModifierSet")
    fun allModifier(): List<ItemModifierSet?>?

    @Query("SELECT * FROM ItemModifierSet WHERE id IN (:itemIds)")
    fun modifierSetByItem(itemIds: IntArray): LiveData<List<ItemModifierSet>>


    @Query("SELECT * from ItemModifierSet where ItemModifierSet.id  = :id LIMIT 1")
    fun modifierById(id: Int?): ItemModifierSet?

    @Query("SELECT * from ItemModifierSet LIMIT 1")
    fun modifierOne(): ItemModifierSet?

    @Query("SELECT * from ItemModifierSet where ItemModifierSet.id  = :restId LIMIT 1")
    fun modifierByRestId(restId: Int?): ItemModifierSet?

    @Query("SELECT * from ItemModifierSet where ItemModifierSet.id  = :id and ItemModifierSet.id = :id1 LIMIT 1")
    fun modifierById(id: Int?, id1: Int?): ItemModifierSet?

    @Query("DELETE FROM ItemModifierSet where ItemModifierSet.id  = :id")
    suspend fun delete(id: Int?)

    @Query("DELETE FROM ItemModifierSet")
    fun delete()

}