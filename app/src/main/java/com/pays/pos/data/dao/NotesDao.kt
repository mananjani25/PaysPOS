package com.pays.pos.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.pays.pos.data.model.responseModel.NoteResponse


@Dao
interface NotesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addNotes(noteModel: NoteResponse.Data): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAllNotes(noteList: List<NoteResponse.Data>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAllNotesSuspend(noteList: List<NoteResponse.Data>)

    @get:Query("select * from TbNotes where TbNotes.isDeleted = 0 ORDER BY TbNotes.sort DESC")
    val alllNotes: LiveData<List<NoteResponse.Data>>

    @get:Query("select * from TbNotes where TbNotes.isDeleted = 0 and TbNotes.isActive = 1 ORDER BY TbNotes.sort DESC")
    val taxListActive: LiveData<List<NoteResponse.Data>>

    @Query("select * from TbNotes where TbNotes.isDeleted = 0")
    fun allNotesList(): List<NoteResponse.Data>

    @Query("SELECT * from TbNotes where TbNotes.id  = :id LIMIT 1")
    fun notessById(id: Int?): NoteResponse.Data

    @Query("DELETE FROM TbNotes")
    suspend fun delete()

    @Query("DELETE FROM TbNotes where TbNotes.id  = :id")
    suspend fun deleteNotesById(id: Int)

    @Query("SELECT * FROM TbNotes WHERE TbNotes.id IN (:userIds)")
    fun notesByIds(userIds: IntArray): List<NoteResponse.Data>

    @Query("UPDATE TbNotes SET isActive = :active WHERE  TbNotes.id = :id")
    suspend fun activeNote(id: Int, active: Boolean?): Int
}