package com.autopilot.agent.agent.tools

import com.autopilot.agent.data.remote.OpenRouterApi
import com.autopilot.agent.data.remote.dto.ChatCompletionRequest
import com.autopilot.agent.data.remote.dto.ChatMessage
import com.autopilot.agent.data.repository.ModelRepository
import com.autopilot.agent.domain.model.ToolResult
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import javax.inject.Inject

/**
 * Text processing tool that uses the LLM for summarization, translation,
 * key extraction, and format conversion.
 */
class TextProcessorTool @Inject constructor(
    private val openRouterApi: OpenRouterApi,
    private val modelRepository: ModelRepository
) : Tool {

    override val name = "TEXT_PROCESSOR"
    override val description = "Process text: summarize, translate, extract keys, format"
    override val requiresConfirmation = false

    override suspend fun execute(input: Map<String, Any?>): ToolResult {
        val startTime = System.currentTimeMillis()
        val operation = input["operation"]?.toString()?.lowercase()
        val text = input["text"]?.toString()

        if (text.isNullOrBlank()) {
            return ToolResult(name, false, "", "Text is required",
                System.currentTimeMillis() - startTime)
        }

        return when (operation) {
            "summarize" -> processWithLlm("Summarize the following text concisely:\n\n$text", startTime)
            "translate" -> {
                val targetLang = input["target_language"]?.toString() ?: "English"
                processWithLlm("Translate the following text to $targetLang:\n\n$text", startTime)
            }
            "extract_keys" -> processWithLlm(
                "Extract the key points, named entities, and main ideas from this text. List them clearly:\n\n$text",
                startTime
            )
            "sentiment" -> processWithLlm(
                "Analyze the sentiment of this text. Identify the overall tone and key emotional indicators:\n\n$text",
                startTime
            )
            "format" -> formatText(text, input, startTime)
            else -> ToolResult(name, false, "",
                "Unknown operation: $operation. Use: summarize, translate, extract_keys, sentiment, format",
                System.currentTimeMillis() - startTime)
        }
    }

    private suspend fun processWithLlm(prompt: String, startTime: Long): ToolResult {
        val authHeader = modelRepository.getAuthHeader()
            ?: return ToolResult(name, false, "", "No API key configured",
                System.currentTimeMillis() - startTime)

        return try {
            val models = modelRepository.getCachedFreeModels()
            val model = models.firstOrNull()?.id ?: "deepseek/deepseek-chat"

            val request = ChatCompletionRequest(
                model = model,
                messages = listOf(
                    ChatMessage(role = "user", content = prompt)
                ),
                temperature = 0.3,
                maxTokens = 2048
            )

            val response = openRouterApi.createChatCompletion(authHeader, request = request)
            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content ?: "No response"
                ToolResult(name, true, content, executionTimeMs = System.currentTimeMillis() - startTime)
            } else {
                ToolResult(name, false, "", "LLM request failed: ${response.code()}",
                    System.currentTimeMillis() - startTime)
            }
        } catch (e: Exception) {
            ToolResult(name, false, "", "Processing error: ${e.message}",
                System.currentTimeMillis() - startTime)
        }
    }

    private fun formatText(text: String, input: Map<String, Any?>, startTime: Long): ToolResult {
        val format = input["format"]?.toString()?.lowercase() ?: "plain"

        return try {
            val result = when (format) {
                "plain" -> {
                    text.replace(Regex("<[^>]*>"), "")
                        .replace(Regex("[#*_~`]"), "")
                        .replace(Regex("\\s+"), " ")
                        .trim()
                }
                "json_pretty" -> {
                    val json = JsonParser.parseString(text)
                    GsonBuilder().setPrettyPrinting().create().toJson(json)
                }
                "uppercase" -> text.uppercase()
                "lowercase" -> text.lowercase()
                "trim" -> text.trim()
                "lines" -> text.split(Regex("[.!?]\\s+"))
                    .filter { it.isNotBlank() }
                    .joinToString("\n") { "• ${it.trim()}" }
                else -> text
            }
            ToolResult(name, true, result, executionTimeMs = System.currentTimeMillis() - startTime)
        } catch (e: Exception) {
            ToolResult(name, false, "", "Format error: ${e.message}",
                System.currentTimeMillis() - startTime)
        }
    }
}
