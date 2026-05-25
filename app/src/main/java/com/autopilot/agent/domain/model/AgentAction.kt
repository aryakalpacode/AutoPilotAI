package com.autopilot.agent.domain.model

/**
 * Represents a parsed action from the LLM's response.
 * The agent decides what to do next by outputting one of these actions.
 */
data class AgentAction(
    val thought: String = "",
    val action: String = "", // TOOL_NAME, "RESPOND", or "COMPLETE"
    val actionInput: Map<String, Any?> = emptyMap(),
    val confidence: Double = 0.5
) {
    /** Whether this action uses a tool. */
    val isTool: Boolean
        get() = action !in listOf("RESPOND", "COMPLETE", "")

    /** Whether this is the final action. */
    val isComplete: Boolean
        get() = action == "COMPLETE"

    /** Whether this is an intermediate response to the user. */
    val isRespond: Boolean
        get() = action == "RESPOND"

    /** Get a string input parameter by key. */
    fun getStringInput(key: String): String {
        return actionInput[key]?.toString() ?: ""
    }

    /** Get a map input parameter by key. */
    @Suppress("UNCHECKED_CAST")
    fun getMapInput(key: String): Map<String, Any?> {
        return actionInput[key] as? Map<String, Any?> ?: emptyMap()
    }
}
