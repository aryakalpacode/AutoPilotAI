package com.autopilot.agent.data.repository

import com.autopilot.agent.data.local.dao.ConversationDao
import com.autopilot.agent.data.local.dao.MessageDao
import com.autopilot.agent.data.local.dao.TaskDao
import com.autopilot.agent.data.local.entity.ConversationEntity
import com.autopilot.agent.data.local.entity.MessageEntity
import com.autopilot.agent.data.local.entity.TaskEntity
import com.autopilot.agent.domain.model.*
import com.autopilot.agent.util.estimateTokens
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing conversations, messages, and tasks.
 * Acts as the single source of truth for conversation data.
 */
@Singleton
class ConversationRepository @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val taskDao: TaskDao
) {

    /** Create a new conversation and return its ID. */
    suspend fun createConversation(title: String, model: String): Long {
        val entity = ConversationEntity(
            title = title,
            modelUsed = model
        )
        return conversationDao.insert(entity)
    }

    /** Get a conversation by ID. */
    suspend fun getConversation(id: Long): Conversation? {
        val entity = conversationDao.getById(id) ?: return null
        return entity.toDomain()
    }

    /** Get all active conversations as Flow. */
    fun getAllConversations(): Flow<List<Conversation>> {
        return conversationDao.getAllActive().map { list ->
            list.map { it.toDomain() }
        }
    }

    /** Get recent conversations. */
    fun getRecentConversations(limit: Int = 10): Flow<List<Conversation>> {
        return conversationDao.getRecent(limit).map { list ->
            list.map { it.toDomain() }
        }
    }

    /** Update conversation title. */
    suspend fun updateTitle(id: Long, title: String) {
        conversationDao.updateTitle(id, title)
    }

    /** Update conversation timestamp (e.g., when new message added). */
    suspend fun touchConversation(id: Long) {
        conversationDao.updateTimestamp(id)
    }

    /** Delete a conversation. */
    suspend fun deleteConversation(id: Long) {
        conversationDao.deleteById(id)
    }

    /** Delete all conversations. */
    suspend fun deleteAllConversations() {
        conversationDao.deleteAll()
    }

    // ─── Messages ────────────────────────────────────────────────

    /** Add a message to a conversation. */
    suspend fun addMessage(message: Message): Long {
        val entity = message.toEntity()
        val id = messageDao.insert(entity)
        conversationDao.updateTimestamp(message.conversationId)
        return id
    }

    /** Get all messages for a conversation as Flow. */
    fun getMessages(conversationId: Long): Flow<List<Message>> {
        return messageDao.getByConversationId(conversationId).map { list ->
            list.map { it.toDomain() }
        }
    }

    /** Get all messages synchronously (for agent loop). */
    suspend fun getMessagesSync(conversationId: Long): List<Message> {
        return messageDao.getByConversationIdSync(conversationId).map { it.toDomain() }
    }

    /** Get the last N messages for a conversation. */
    suspend fun getLastMessages(conversationId: Long, limit: Int): List<Message> {
        return messageDao.getLastMessages(conversationId, limit).map { it.toDomain() }.reversed()
    }

    /** Get estimated total token count for a conversation. */
    suspend fun getTotalTokens(conversationId: Long): Int {
        return messageDao.getTotalTokens(conversationId) ?: 0
    }

    // ─── Tasks ───────────────────────────────────────────────────

    /** Create a new task. */
    suspend fun createTask(task: Task): Long {
        return taskDao.insert(task.toEntity())
    }

    /** Create multiple tasks. */
    suspend fun createTasks(tasks: List<Task>) {
        taskDao.insertAll(tasks.map { it.toEntity() })
    }

    /** Get tasks for a conversation as Flow. */
    fun getTasks(conversationId: Long): Flow<List<Task>> {
        return taskDao.getByConversationId(conversationId).map { list ->
            list.map { it.toDomain() }
        }
    }

    /** Get tasks synchronously. */
    suspend fun getTasksSync(conversationId: Long): List<Task> {
        return taskDao.getByConversationIdSync(conversationId).map { it.toDomain() }
    }

    /** Get all root-level tasks as Flow. */
    fun getAllRootTasks(): Flow<List<Task>> {
        return taskDao.getAllRootTasks().map { list ->
            list.map { it.toDomain() }
        }
    }

    /** Get tasks filtered by status. */
    fun getTasksByStatus(status: TaskStatus): Flow<List<Task>> {
        return taskDao.getByStatus(status.value).map { list ->
            list.map { it.toDomain() }
        }
    }

    /** Update task status. */
    suspend fun updateTaskStatus(taskId: Long, status: TaskStatus) {
        val completedAt = if (status == TaskStatus.COMPLETED) System.currentTimeMillis() else null
        taskDao.updateStatus(taskId, status.value, completedAt)
    }

    /** Update task with result. */
    suspend fun updateTaskResult(taskId: Long, result: String) {
        taskDao.updateResult(taskId, TaskStatus.COMPLETED.value, result)
    }

    /** Update task with error. */
    suspend fun updateTaskError(taskId: Long, error: String) {
        taskDao.updateError(taskId, TaskStatus.FAILED.value, error)
    }

    /** Increment task iteration count. */
    suspend fun incrementTaskIteration(taskId: Long) {
        taskDao.incrementIteration(taskId)
    }

    /** Get task completion progress. */
    suspend fun getTaskProgress(conversationId: Long): Pair<Int, Int> {
        val completed = taskDao.getCompletedCount(conversationId)
        val total = taskDao.getTotalCount(conversationId)
        return Pair(completed, total)
    }

    // ─── Mappers ─────────────────────────────────────────────────

    private fun ConversationEntity.toDomain() = Conversation(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        modelUsed = modelUsed,
        isArchived = isArchived
    )

    private fun MessageEntity.toDomain() = Message(
        id = id,
        conversationId = conversationId,
        role = MessageRole.fromString(role),
        content = content,
        toolName = toolName,
        toolInput = toolInput,
        toolOutput = toolOutput,
        timestamp = timestamp,
        tokenCount = tokenCount,
        model = model
    )

    private fun Message.toEntity() = MessageEntity(
        id = if (id == 0L) 0 else id,
        conversationId = conversationId,
        role = role.value,
        content = content,
        toolName = toolName,
        toolInput = toolInput,
        toolOutput = toolOutput,
        timestamp = timestamp,
        tokenCount = if (tokenCount == 0) content.estimateTokens() else tokenCount,
        model = model
    )

    private fun TaskEntity.toDomain() = Task(
        id = id,
        conversationId = conversationId,
        parentTaskId = parentTaskId,
        title = title,
        description = description,
        status = TaskStatus.fromString(status),
        toolToUse = toolToUse,
        result = result,
        orderIndex = orderIndex,
        createdAt = createdAt,
        completedAt = completedAt,
        errorMessage = errorMessage,
        iterationCount = iterationCount
    )

    private fun Task.toEntity() = TaskEntity(
        id = if (id == 0L) 0 else id,
        conversationId = conversationId,
        parentTaskId = parentTaskId,
        title = title,
        description = description,
        status = status.value,
        toolToUse = toolToUse,
        result = result,
        orderIndex = orderIndex,
        createdAt = createdAt,
        completedAt = completedAt,
        errorMessage = errorMessage,
        iterationCount = iterationCount
    )
}
