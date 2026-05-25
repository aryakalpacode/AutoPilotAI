package com.autopilot.agent.domain.usecase

import com.autopilot.agent.data.repository.ToolRepository
import com.autopilot.agent.domain.model.ToolResult
import javax.inject.Inject

/**
 * Use case for executing a specific tool by name.
 */
class ExecuteToolUseCase @Inject constructor(
    private val toolRepository: ToolRepository
) {
    /**
     * Execute the specified tool with the given input.
     */
    suspend operator fun invoke(toolName: String, input: Map<String, Any?>): ToolResult {
        return toolRepository.executeTool(toolName, input)
    }
}
