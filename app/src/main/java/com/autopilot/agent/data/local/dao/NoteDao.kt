package com.autopilot.agent.data.local.dao

import androidx.room.*
import com.autopilot.agent.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for note entities.
 */
@Dao
interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): NoteEntity?

    @Query("SELECT * FROM notes ORDER BY updated_at DESC")
    fun getAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' ORDER BY updated_at DESC")
    fun search(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE tags LIKE '%' || :tag || '%' ORDER BY updated_at DESC")
    fun getByTag(tag: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE source_conversation_id = :conversationId ORDER BY created_at DESC")
    fun getByConversation(conversationId: Long): Flow<List<NoteEntity>>

    @Query("DELETE FROM notes")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun getCount(): Int
}
