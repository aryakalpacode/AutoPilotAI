package com.autopilot.agent.domain.model

/**
 * Domain model representing a conversation thread.
 */
data class Conversation(
    val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val modelUsed: String,
    val isArchived: Boolean = false,
    val messages: List<Message> = emptyList(),
    val tasks: List<Task> = emptyList()
)
