package com.autopilot.agent.domain.model

/**
 * Domain model representing a task or sub-task.
 */
data class Task(
    val id: Long = 0,
    val conversationId: Long = 0,
    val parentTaskId: Long? = null,
    val title: String,
    val description: String = "",
    val status: TaskStatus = TaskStatus.PENDING,
    val toolToUse: String? = null,
    val result: String? = null,
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val errorMessage: String? = null,
    val iterationCount: Int = 0,
    val subTasks: List<Task> = emptyList()
)

/**
 * Enumeration of possible task statuses.
 */
enum class TaskStatus(val value: String) {
    PENDING("pending"),
    RUNNING("running"),
    COMPLETED("completed"),
    FAILED("failed"),
    CANCELLED("cancelled");

    companion object {
        fun fromString(value: String): TaskStatus {
            return entries.find { it.value == value } ?: PENDING
        }
    }
}
