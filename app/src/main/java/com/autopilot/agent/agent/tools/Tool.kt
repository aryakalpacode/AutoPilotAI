package com.autopilot.agent.agent.tools

import com.autopilot.agent.domain.model.ToolResult

/**
 * Interface that all agent tools must implement.
 * Each tool provides a specific capability to the autonomous agent.
 */
interface Tool {
    /** Unique name identifier for this tool (matches the agent's action string). */
    val name: String

    /** Human-readable description of what this tool does. */
    val description: String

    /** Whether this tool modifies state (files, clipboard, etc.) and needs confirmation. */
    val requiresConfirmation: Boolean

    /**
     * Execute the tool with the given input parameters.
     * @param input Map of input parameters from the agent's action_input.
     * @return ToolResult containing the output or error.
     */
    suspend fun execute(input: Map<String, Any?>): ToolResult
}
