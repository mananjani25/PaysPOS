package com.android.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import com.android.pos.data.entities.TbModifier


/**
 * Created by vishal patel on 13/7/2021.
 */
@Dao
interface DBModifierDao {

    @Insert(onConflict = REPLACE)
    fun add(modifierModel: TbModifier?): Long

    @Insert(onConflict = REPLACE)
    fun addAll(modifierModel: List<TbModifier?>?)

    @get:Query("select * from TbModifier")
    val all: LiveData<List<TbModifier?>>?


    @Query("select * from TbModifier where  TbModifier.inventoryId = :id")
    fun all(id: Int?): LiveData<List<TbModifier?>>?

    @Query("select * from TbModifier")
    fun allModifier(): List<TbModifier?>?

    @Query("SELECT * FROM TbModifier WHERE id IN (:userIds)")
    fun loadAllByIds(userIds: IntArray): List<TbModifier?>?


    @Query("SELECT * from TbModifier where TbModifier.id  = :id LIMIT 1")
    fun modifierById(id: Int?): TbModifier?

    @Query("SELECT * from TbModifier LIMIT 1")
    fun modifierOne(): TbModifier?

    @Query("SELECT * from TbModifier where TbModifier.id  = :restId and TbModifier.inventoryId  = :invId LIMIT 1")
    fun modifierByRestId(restId: Int?, invId: Int?): TbModifier?

    @Query("SELECT * from TbModifier where TbModifier.modifierId  = :id and TbModifier.modifierGroupId =:mgId and TbModifier.inventoryParentId =:invId LIMIT 1")
    fun modifierByIds(id: Int?, mgId: Int?, invId: Int?): TbModifier?

    @Query("SELECT * from TbModifier where TbModifier.modifierId  = :modifierId and TbModifier.modifierGroupId =:modifierGroupId and TbModifier.inventoryId =:inventoryId  and TbModifier.inventoryParentId =:inventoryParentId LIMIT 1")
    fun dbModifierByParent(
        modifierId: Int?,
        modifierGroupId: Int?,
        inventoryId: Int?,
        inventoryParentId: Int?
    ): TbModifier?

    @Query("DELETE FROM TbModifier where TbModifier.modifierId  = :id and TbModifier.modifierGroupId =:mgId and TbModifier.inventoryId =:invId and TbModifier.inventoryParentId =:inventoryParentId")
    fun delete(id: Int?, mgId: Int?, invId: Int?, inventoryParentId: Int?)

    @Query("DELETE FROM TbModifier")
    fun deleteModifierTbl()


    @Query("UPDATE TbModifier SET mItemQuantity = :qty WHERE  TbModifier.modifierId = :id and TbModifier.modifierGroupId =:mgId and TbModifier.inventoryId =:invId and TbModifier.inventoryParentId =:inventoryParentId")
    fun updateQty(id: Int?, mgId: Int?, invId: Int?, qty: Int?, inventoryParentId: Int?)

}