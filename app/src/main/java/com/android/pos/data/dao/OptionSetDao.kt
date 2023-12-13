package com.android.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import com.android.pos.data.entities.ModifierSet
import com.android.pos.data.entities.OptionSet


/**
 * Created by vishal patel on 2/3/2018.
 */
@Dao
interface OptionSetDao {

    @Insert(onConflict = REPLACE)
    suspend fun add(modifierModel: OptionSet?): Long

    @Insert
    suspend fun insert(modifierModel: OptionSet)

    @Update
    fun update(modifierModel: OptionSet)

    @Transaction
    @Insert(onConflict = REPLACE)
    suspend fun addAll(modifierModel: List<OptionSet>)

    @get:Query("select * from OptionSet where OptionSet.isDeleted = 0 ORDER BY OptionSet.sort ASC")
    val all: LiveData<List<OptionSet>>

    @Query("select * from OptionSet where OptionSet.isDeleted = 0")
    fun allOptionsSet(): List<OptionSet?>?

    @Query("SELECT * FROM OptionSet WHERE id IN (:itemIds) and OptionSet.isDeleted = 0")
    fun optionSetByItem(itemIds: IntArray): LiveData<List<OptionSet>>


    @Query("SELECT * from OptionSet where OptionSet.id  = :id LIMIT 1")
    fun optionSetById(id: Int?): OptionSet?

    @Query("SELECT * from OptionSet LIMIT 1")
    fun optionOne(): OptionSet?

    @Query("SELECT * from OptionSet where OptionSet.id  = :restId LIMIT 1")
    fun optionByRestId(restId: Int?): OptionSet?

    @Query("SELECT * from OptionSet where OptionSet.id  = :id and OptionSet.id = :id1 LIMIT 1")
    fun optionById(id: Int?, id1: Int?): OptionSet?

    @Query("DELETE FROM OptionSet where OptionSet.id  = :id")
    suspend fun delete(id: Int?)

    @Query("DELETE FROM OptionSet")
    suspend fun delete()


    @Query("UPDATE OptionSet SET options = :optionsJSON WHERE OptionSet.id = :optId")
    suspend fun updateOptionsJSON(optId: Int, optionsJSON: String)

}