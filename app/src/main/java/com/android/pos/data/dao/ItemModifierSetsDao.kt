package com.android.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.pos.data.entities.ItemModifierSets


@Dao
interface ItemModifierSetsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(noteModel: ItemModifierSets): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAll(modifierSets: List<ItemModifierSets>)

    @Query("DELETE FROM ItemModifierSets")
    fun delete()

    @Query("SELECT * from ItemModifierSets where ItemModifierSets.itemId  = :itemId and ItemModifierSets.modifierSetId  = :modifierSetId LIMIT 1")
    fun minMaxByItemModifier(itemId: Int, modifierSetId: Int): LiveData<ItemModifierSets?>
}