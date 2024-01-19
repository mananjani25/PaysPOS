package com.pays.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.pays.pos.data.entities.ItemModifierSets


@Dao
interface ItemModifierSetsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(noteModel: ItemModifierSets): Long

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAll(modifierSets: List<ItemModifierSets>)

    @Query("DELETE FROM ItemModifierSets")
    suspend fun delete()

    @Query("SELECT * from ItemModifierSets where ItemModifierSets.itemId  = :itemId and ItemModifierSets.modifierSetId  = :modifierSetId LIMIT 1")
    fun minMaxByItemModifier(itemId: Int, modifierSetId: Int): LiveData<ItemModifierSets?>
}