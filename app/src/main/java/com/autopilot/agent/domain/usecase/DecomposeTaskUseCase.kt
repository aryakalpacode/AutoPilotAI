package com.autopilot.agent.domain.usecase

import com.autopilot.agent.data.repository.ConversationRepository
import com.autopilot.agent.domain.model.Task
import com.autopilot.agent.domain.model.TaskStatus
import javax.inject.Inject

/**
 * Use case for decomposing a high-level goal into sub-tasks.
 */
class DecomposeTaskUseCase @Inject constructor(
    private val conversationRepo: ConversationRepository
) {
    /**
     * Create a root task with sub-tasks for a conversation.
     */
    suspend operator fun invoke(
        conversationId: Long,
        title: String,
        subTaskTitles: List<String>
    ): Long {
        val rootTask = Task(
            conversationId = conversationId,
            title = title,
            description = "Main task: $title",
            status = TaskStatus.RUNNING,
            orderIndex = 0
        )
        val rootId = conversationRepo.createTask(rootTask)

        subTaskTitles.forEachIndexed { index, subTitle ->
            conversationRepo.createTask(
                Task(
                    conversationId = conversationId,
                    parentTaskId = rootId,
                    title = subTitle,
                    description = subTitle,
                    status = TaskStatus.PENDING,
                    orderIndex = index + 1
                )
            )
        }

        return rootId
    }
}
