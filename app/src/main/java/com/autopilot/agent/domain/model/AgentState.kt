package com.autopilot.agent.domain.model

/**
 * Represents the current state of the autonomous agent during execution.
 */
sealed class AgentState {
    /** Agent is idle and ready for input. */
    data object Idle : AgentState()

    /** Agent is planning the task decomposition. */
    data object Planning : AgentState()

    /** Agent is thinking / reasoning about the next step. */
    data class Thinking(val thought: String = "") : AgentState()

    /** Agent is executing a tool. */
    data class ExecutingTool(
        val toolName: String,
        val input: String = ""
    ) : AgentState()

    /** Agent has produced an intermediate response. */
    data class Responding(val message: String) : AgentState()

    /** Agent is waiting for user confirmation. */
    data class WaitingForConfirmation(
        val action: AgentAction,
        val reason: String
    ) : AgentState()

    /** Agent has completed the task. */
    data class Complete(
        val summary: String,
        val details: String = ""
    ) : AgentState()

    /** Agent encountered an error. */
    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : AgentState()

    /** Agent is paused by the user. */
    data object Paused : AgentState()

    /** Agent is cancelled. */
    data object Cancelled : AgentState()
}

/**
 * Status labels for display in the UI.
 */
fun AgentState.displayLabel(): String = when (this) {
    is AgentState.Idle -> "Ready"
    is AgentState.Planning -> "Planning..."
    is AgentState.Thinking -> "Thinking..."
    is AgentState.ExecutingTool -> "Using $toolName..."
    is AgentState.Responding -> "Responding..."
    is AgentState.WaitingForConfirmation -> "Waiting for confirmation..."
    is AgentState.Complete -> "Complete"
    is AgentState.Error -> "Error"
    is AgentState.Paused -> "Paused"
    is AgentState.Cancelled -> "Cancelled"
}
