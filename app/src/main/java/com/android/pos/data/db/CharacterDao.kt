package com.android.pos.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.pos.data.model.CharacterModel

@Dao
interface CharacterDao {

    @Query("SELECT * FROM characters")
    fun getAllCharacters(): LiveData<List<CharacterModel>>

    @Query("SELECT * FROM characters WHERE id = :id")
    fun getCharacter(id: Int): LiveData<CharacterModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(characters: List<CharacterModel>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(character: CharacterModel)

    @Query("DELETE FROM characters")
    suspend fun delete()

}