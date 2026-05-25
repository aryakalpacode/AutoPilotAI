package com.autopilot.agent.domain.model

/**
 * Represents the result of a tool execution.
 */
data class ToolResult(
    val toolName: String,
    val success: Boolean,
    val output: String,
    val error: String? = null,
    val executionTimeMs: Long = 0
) {
    /** Get a formatted display string for the result. */
    fun toDisplayString(): String {
        return if (success) {
            "✓ $toolName: ${output.take(500)}"
        } else {
            "✗ $toolName failed: ${error ?: output}"
        }
    }

    /** Get content suitable for adding to conversation context. */
    fun toContextString(): String {
        return if (success) {
            "Tool $toolName result:\n$output"
        } else {
            "Tool $toolName failed with error: ${error ?: output}"
        }
    }
}
