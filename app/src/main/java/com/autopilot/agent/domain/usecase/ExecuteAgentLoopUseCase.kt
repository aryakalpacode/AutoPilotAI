package com.autopilot.agent.domain.usecase

import com.autopilot.agent.agent.AgentOrchestrator
import com.autopilot.agent.domain.model.AgentState
import javax.inject.Inject

/**
 * Use case for executing the autonomous agent loop.
 * Delegates to the AgentOrchestrator for the actual implementation.
 */
class ExecuteAgentLoopUseCase @Inject constructor(
    private val agentOrchestrator: AgentOrchestrator
) {
    /**
     * Execute the agent loop for a given user prompt.
     */
    suspend operator fun invoke(
        userPrompt: String,
        conversationId: Long,
        model: String,
        maxIterations: Int = 20,
        temperature: Double = 0.7,
        personality: String = "professional",
        autoConfirm: Boolean = false,
        onUpdate: (AgentState) -> Unit = {}
    ): Result<String> {
        return agentOrchestrator.executeTask(
            userPrompt = userPrompt,
            conversationId = conversationId,
            model = model,
            maxIterations = maxIterations,
            temperature = temperature,
            personality = personality,
            autoConfirm = autoConfirm,
            onUpdate = onUpdate
        )
    }
}
