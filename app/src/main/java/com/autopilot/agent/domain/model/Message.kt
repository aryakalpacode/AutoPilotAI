package com.autopilot.agent.domain.model

/**
 * Domain model representing a message in a conversation.
 */
data class Message(
    val id: Long = 0,
    val conversationId: Long = 0,
    val role: MessageRole = MessageRole.USER,
    val content: String,
    val toolName: String? = null,
    val toolInput: String? = null,
    val toolOutput: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val tokenCount: Int = 0,
    val model: String? = null
)

/**
 * Enumeration of possible message roles.
 */
enum class MessageRole(val value: String) {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
    TOOL_CALL("tool_call"),
    TOOL_RESULT("tool_result"),
    ERROR("error");

    companion object {
        fun fromString(value: String): MessageRole {
            return entries.find { it.value == value } ?: USER
        }
    }
}
