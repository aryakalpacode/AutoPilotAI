package com.autopilot.agent.agent

import com.autopilot.agent.data.remote.OpenRouterApi
import com.autopilot.agent.data.remote.dto.ChatCompletionRequest
import com.autopilot.agent.data.remote.dto.ChatMessage
import com.autopilot.agent.data.repository.ModelRepository
import com.autopilot.agent.domain.model.Message
import com.autopilot.agent.domain.model.MessageRole
import com.autopilot.agent.util.Constants
import com.autopilot.agent.util.TokenCounter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the context window for LLM conversations.
 * Handles token counting, message pruning, and automatic summarization
 * to keep conversations within model context limits.
 */
@Singleton
class ContextManager @Inject constructor(
    private val openRouterApi: OpenRouterApi,
    private val modelRepository: ModelRepository
) {

    /**
     * Trim messages to fit within the context window.
     * If messages exceed 75% of the context window, older messages
     * are summarized and replaced with a summary message.
     *
     * @param messages Full list of conversation messages.
     * @param systemPrompt The system prompt (always preserved).
     * @param model The current model ID.
     * @return Trimmed list of messages.
     */
    suspend fun trimToFit(
        messages: List<Message>,
        systemPrompt: String,
        model: String
    ): List<Message> {
        val contextWindow = modelRepository.getContextWindow(model)
        val systemTokens = TokenCounter.estimate(systemPrompt)
        val maxContentTokens = (contextWindow * Constants.CONTEXT_WINDOW_THRESHOLD).toInt() - systemTokens

        // Calculate current total
        val totalTokens = messages.sumOf { TokenCounter.estimate(it.content) }

        if (totalTokens <= maxContentTokens) {
            return messages // Fits fine
        }

        // Need to trim. Keep system messages, last N messages, and summarize the rest
        val systemMessages = messages.filter { it.role == MessageRole.SYSTEM }
        val nonSystemMessages = messages.filter { it.role != MessageRole.SYSTEM }

        if (nonSystemMessages.size <= Constants.PRESERVE_LAST_MESSAGES) {
            return messages // Too few to trim
        }

        val keepMessages = nonSystemMessages.takeLast(Constants.PRESERVE_LAST_MESSAGES)
        val toSummarize = nonSystemMessages.dropLast(Constants.PRESERVE_LAST_MESSAGES)

        // Create summary
        val summary = summarizeMessages(toSummarize, model)

        val result = mutableListOf<Message>()
        result.addAll(systemMessages)
        if (summary != null) {
            result.add(
                Message(
                    role = MessageRole.SYSTEM,
                    content = "[Summary of earlier conversation]\n$summary"
                )
            )
        }
        result.addAll(keepMessages)

        return result
    }

    /**
     * Estimate the total token count for a list of messages.
     */
    fun estimateTotalTokens(messages: List<Message>, systemPrompt: String): Int {
        val systemTokens = TokenCounter.estimate(systemPrompt)
        val messageTokens = messages.sumOf { msg ->
            TokenCounter.estimate(msg.content) + 4 // overhead per message
        }
        return systemTokens + messageTokens
    }

    /**
     * Summarize a list of messages using the LLM.
     */
    private suspend fun summarizeMessages(messages: List<Message>, model: String): String? {
        if (messages.isEmpty()) return null

        val authHeader = modelRepository.getAuthHeader() ?: return fallbackSummary(messages)

        val conversationText = messages.joinToString("\n") { msg ->
            "${msg.role.value}: ${msg.content.take(500)}"
        }

        val request = ChatCompletionRequest(
            model = model,
            messages = listOf(
                ChatMessage(
                    role = "user",
                    content = "Summarize the following conversation concisely, keeping all key facts and results:\n\n$conversationText"
                )
            ),
            temperature = 0.3,
            maxTokens = 1024
        )

        return try {
            val response = openRouterApi.createChatCompletion(authHeader, request = request)
            if (response.isSuccessful) {
                response.body()?.choices?.firstOrNull()?.message?.content
            } else {
                fallbackSummary(messages)
            }
        } catch (e: Exception) {
            fallbackSummary(messages)
        }
    }

    /**
     * Create a simple concatenation summary when LLM is unavailable.
     */
    private fun fallbackSummary(messages: List<Message>): String {
        return messages.joinToString("\n") { msg ->
            "[${msg.role.value}] ${msg.content.take(200)}"
        }.take(2000)
    }
}
