package com.autopilot.agent.ui.state

import com.autopilot.agent.domain.model.*

/**
 * UI state for the chat screen.
 */
data class ChatUiState(
    val conversationId: Long = 0,
    val conversationTitle: String = "New Chat",
    val messages: List<Message> = emptyList(),
    val tasks: List<Task> = emptyList(),
    val agentState: AgentState = AgentState.Idle,
    val currentModel: String = "",
    val totalTokens: Int = 0,
    val currentIteration: Int = 0,
    val maxIterations: Int = 20,
    val showDebugView: Boolean = false,
    val showTaskProgress: Boolean = false,
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val showConfirmDialog: Boolean = false,
    val confirmAction: AgentAction? = null,
    val confirmReason: String = ""
)
