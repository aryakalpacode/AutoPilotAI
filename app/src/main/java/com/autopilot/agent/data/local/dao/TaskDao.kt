package com.autopilot.agent.data.local.dao

import androidx.room.*
import com.autopilot.agent.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for task entities.
 */
@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE conversation_id = :conversationId ORDER BY order_index ASC")
    fun getByConversationId(conversationId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE conversation_id = :conversationId ORDER BY order_index ASC")
    suspend fun getByConversationIdSync(conversationId: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE parent_task_id = :parentId ORDER BY order_index ASC")
    suspend fun getSubTasks(parentId: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE parent_task_id IS NULL ORDER BY created_at DESC")
    fun getAllRootTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY created_at DESC")
    fun getByStatus(status: String): Flow<List<TaskEntity>>

    @Query("UPDATE tasks SET status = :status, completed_at = :completedAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, completedAt: Long? = null)

    @Query("UPDATE tasks SET status = :status, result = :result, completed_at = :completedAt WHERE id = :id")
    suspend fun updateResult(id: Long, status: String, result: String?, completedAt: Long? = System.currentTimeMillis())

    @Query("UPDATE tasks SET status = :status, error_message = :errorMessage WHERE id = :id")
    suspend fun updateError(id: Long, status: String = "failed", errorMessage: String?)

    @Query("UPDATE tasks SET iteration_count = iteration_count + 1 WHERE id = :id")
    suspend fun incrementIteration(id: Long)

    @Query("DELETE FROM tasks WHERE conversation_id = :conversationId")
    suspend fun deleteByConversationId(conversationId: Long)

    @Query("SELECT COUNT(*) FROM tasks WHERE conversation_id = :conversationId AND status = 'completed'")
    suspend fun getCompletedCount(conversationId: Long): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE conversation_id = :conversationId")
    suspend fun getTotalCount(conversationId: Long): Int

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()
}
