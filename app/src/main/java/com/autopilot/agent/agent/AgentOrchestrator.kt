package com.autopilot.agent.agent

import android.util.Log
import com.autopilot.agent.agent.tools.*
import com.autopilot.agent.data.remote.OpenRouterApi
import com.autopilot.agent.data.remote.dto.ChatCompletionRequest
import com.autopilot.agent.data.remote.dto.ChatMessage
import com.autopilot.agent.data.repository.ConversationRepository
import com.autopilot.agent.data.repository.ModelRepository
import com.autopilot.agent.domain.model.*
import com.autopilot.agent.util.Constants
import com.autopilot.agent.util.estimateTokens
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentOrchestrator @Inject constructor(
    private val openRouterApi: OpenRouterApi,
    private val promptBuilder: PromptBuilder,
    private val responseParser: ResponseParser,
    private val contextManager: ContextManager,
    private val conversationRepo: ConversationRepository,
    private val modelRepository: ModelRepository,
    private val webSearchTool: WebSearchTool,
    private val webScrapeTool: WebScrapeTool,
    private val fileManagerTool: FileManagerTool,
    private val codeExecutorTool: CodeExecutorTool,
    private val calculatorTool: CalculatorTool,
    private val clipboardTool: ClipboardTool,
    private val deviceInfoTool: DeviceInfoTool,
    private val notesDatabaseTool: NotesDatabaseTool,
    private val reminderTool: ReminderTool,
    private val textProcessorTool: TextProcessorTool
) {

    companion object {
        private const val TAG = "AgentOrchestrator"

        /** Free models ordered by quality. On 429/failure, we try the next one. */
        private val FREE_MODELS = listOf(
            "deepseek/deepseek-v4-flash:free",
            "meta-llama/llama-3.3-70b-instruct:free",
            "google/gemma-4-31b-it:free",
            "nvidia/nemotron-3-super-120b-a12b:free",
            "openai/gpt-oss-120b:free",
            "qwen/qwen3-coder:free",
            "minimax/minimax-m2.5:free",
            "nvidia/nemotron-nano-9b-v2:free",
            "nousresearch/hermes-3-llama-3.1-405b:free"
        )

        private const val RATE_LIMIT_WAIT_MS = 5000L // 5 seconds between model switches on 429
    }

    private val tools: Map<String, Tool> by lazy {
        mapOf(
            "WEB_SEARCH" to webSearchTool,
            "WEB_SCRAPE" to webScrapeTool,
            "FILE_MANAGER" to fileManagerTool,
            "CODE_EXECUTOR" to codeExecutorTool,
            "CALCULATOR" to calculatorTool,
            "CLIPBOARD" to clipboardTool,
            "DEVICE_INFO" to deviceInfoTool,
            "NOTES_DATABASE" to notesDatabaseTool,
            "REMINDER" to reminderTool,
            "TEXT_PROCESSOR" to textProcessorTool
        )
    }

    private val _agentState = MutableStateFlow<AgentState>(AgentState.Idle)
    val agentState: StateFlow<AgentState> = _agentState.asStateFlow()

    private var currentJob: Job? = null
    @Volatile private var isPaused = false
    @Volatile private var isCancelled = false
    var onConfirmationNeeded: (suspend (AgentAction, String) -> Boolean)? = null
    private val gson = Gson()

    suspend fun executeTask(
        userPrompt: String,
        conversationId: Long,
        model: String,
        maxIterations: Int = Constants.DEFAULT_MAX_ITERATIONS,
        temperature: Double = Constants.DEFAULT_TEMPERATURE,
        personality: String = "professional",
        autoConfirm: Boolean = false,
        onUpdate: (AgentState) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        isCancelled = false
        isPaused = false
        var iteration = 0
        var consecutiveErrors = 0
        val currentModel = model

        try {
            conversationRepo.addMessage(
                Message(conversationId = conversationId, role = MessageRole.USER, content = userPrompt, model = currentModel)
            )

            val systemPrompt = promptBuilder.buildSystemPrompt(personality)

            while (iteration < maxIterations && !isCancelled) {
                while (isPaused && !isCancelled) { delay(500) }
                if (isCancelled) break

                iteration++
                Log.d(TAG, "=== Iteration $iteration of $maxIterations ===")

                val history = conversationRepo.getMessagesSync(conversationId)
                val trimmedHistory = contextManager.trimToFit(history, systemPrompt, currentModel)
                val messages = promptBuilder.buildMessages(systemPrompt, trimmedHistory)

                _agentState.value = AgentState.Thinking()
                onUpdate(AgentState.Thinking())

                val (action, errorDetail) = callLlmWithFallback(messages, currentModel, temperature)
                if (action == null) {
                    consecutiveErrors++
                    val errorMsg = errorDetail ?: "Unknown LLM error"
                    if (consecutiveErrors >= Constants.MAX_CONSECUTIVE_ERRORS) {
                        val finalMsg = "Failed after $consecutiveErrors attempts.\n$errorMsg"
                        _agentState.value = AgentState.Error(finalMsg)
                        onUpdate(AgentState.Error(finalMsg))
                        conversationRepo.addMessage(
                            Message(conversationId = conversationId, role = MessageRole.ERROR, content = finalMsg)
                        )
                        return@withContext Result.failure(Exception(finalMsg))
                    }
                    conversationRepo.addMessage(
                        Message(
                            conversationId = conversationId, role = MessageRole.ERROR,
                            content = "Attempt $consecutiveErrors failed: $errorMsg"
                        )
                    )
                    delay(Constants.INITIAL_BACKOFF_MS * consecutiveErrors)
                    continue
                }

                consecutiveErrors = 0

                val actionJson = gson.toJson(action)
                conversationRepo.addMessage(
                    Message(conversationId = conversationId, role = MessageRole.ASSISTANT,
                        content = actionJson, model = currentModel, tokenCount = actionJson.estimateTokens())
                )

                when {
                    action.isComplete -> {
                        val summary = action.getStringInput("summary")
                        val details = action.getStringInput("details")
                        val finalMessage = if (details.isNotBlank()) "$summary\n\n$details" else summary
                        conversationRepo.addMessage(
                            Message(conversationId = conversationId, role = MessageRole.ASSISTANT,
                                content = finalMessage, model = currentModel)
                        )
                        _agentState.value = AgentState.Complete(summary, details)
                        onUpdate(AgentState.Complete(summary, details))
                        return@withContext Result.success(finalMessage)
                    }

                    action.isRespond -> {
                        val message = action.getStringInput("message")
                        conversationRepo.addMessage(
                            Message(conversationId = conversationId, role = MessageRole.ASSISTANT,
                                content = message, model = currentModel)
                        )
                        _agentState.value = AgentState.Responding(message)
                        onUpdate(AgentState.Responding(message))
                        continue
                    }

                    action.isTool -> {
                        val tool = tools[action.action]
                        if (tool == null) {
                            conversationRepo.addMessage(
                                Message(conversationId = conversationId, role = MessageRole.ERROR,
                                    content = "Unknown tool: ${action.action}")
                            )
                            continue
                        }

                        val needsConfirmation = !autoConfirm && (
                            tool.requiresConfirmation || iteration > 10 ||
                            action.confidence < Constants.CONFIDENCE_THRESHOLD
                        )
                        if (needsConfirmation) {
                            val reason = when {
                                tool.requiresConfirmation -> "This action modifies data (${tool.name})"
                                iteration > 10 -> "Over 10 iterations executed"
                                else -> "Low confidence (${action.confidence})"
                            }
                            _agentState.value = AgentState.WaitingForConfirmation(action, reason)
                            onUpdate(AgentState.WaitingForConfirmation(action, reason))
                            val confirmed = onConfirmationNeeded?.invoke(action, reason) ?: true
                            if (!confirmed) {
                                conversationRepo.addMessage(
                                    Message(conversationId = conversationId, role = MessageRole.SYSTEM,
                                        content = "User declined ${action.action}. Skipping.")
                                )
                                continue
                            }
                        }

                        _agentState.value = AgentState.ExecutingTool(tool.name)
                        onUpdate(AgentState.ExecutingTool(tool.name))

                        conversationRepo.addMessage(
                            Message(conversationId = conversationId, role = MessageRole.TOOL_CALL,
                                content = "Calling ${tool.name}", toolName = tool.name,
                                toolInput = gson.toJson(action.actionInput))
                        )

                        val result = try {
                            tool.execute(action.actionInput)
                        } catch (e: Exception) {
                            ToolResult(tool.name, false, "", "Tool error: ${e.message}")
                        }

                        conversationRepo.addMessage(
                            Message(conversationId = conversationId, role = MessageRole.TOOL_RESULT,
                                content = result.toContextString(), toolName = tool.name,
                                toolOutput = result.output.take(4000))
                        )

                        if (!result.success) {
                            consecutiveErrors++
                            if (consecutiveErrors >= Constants.MAX_CONSECUTIVE_ERRORS) {
                                val errorMsg = "Tool failed $consecutiveErrors times. Stopping."
                                _agentState.value = AgentState.Error(errorMsg)
                                onUpdate(AgentState.Error(errorMsg))
                                return@withContext Result.failure(Exception(errorMsg))
                            }
                        } else { consecutiveErrors = 0 }
                    }

                    else -> {
                        conversationRepo.addMessage(
                            Message(conversationId = conversationId, role = MessageRole.ERROR,
                                content = "Unknown action: ${action.action}")
                        )
                    }
                }
            }

            if (isCancelled) {
                _agentState.value = AgentState.Cancelled
                onUpdate(AgentState.Cancelled)
                return@withContext Result.failure(Exception("Cancelled"))
            }
            val msg = "Reached max iterations ($maxIterations)."
            _agentState.value = AgentState.Error(msg, true)
            onUpdate(AgentState.Error(msg, true))
            return@withContext Result.failure(Exception(msg))

        } catch (e: CancellationException) {
            _agentState.value = AgentState.Cancelled
            return@withContext Result.failure(e)
        } catch (e: Exception) {
            val errorMsg = "Error: ${e.message}"
            _agentState.value = AgentState.Error(errorMsg)
            try { conversationRepo.addMessage(
                Message(conversationId = conversationId, role = MessageRole.ERROR, content = errorMsg)
            ) } catch (_: Exception) {}
            return@withContext Result.failure(e)
        }
    }

    /**
     * KEY FIX: On 429 rate limit, immediately try the NEXT free model
     * instead of retrying the same one. This way we cycle through all
     * available models before giving up.
     */
    private suspend fun callLlmWithFallback(
        messages: List<ChatMessage>,
        primaryModel: String,
        temperature: Double
    ): Pair<AgentAction?, String?> {
        val authHeader = modelRepository.getAuthHeader()
            ?: return Pair(null, "No API key configured. Go to Settings → API Key.")

        // Build the model try-order: primary first, then all fallbacks
        val modelsToTry = mutableListOf(primaryModel)
        for (m in FREE_MODELS) {
            if (m != primaryModel && m !in modelsToTry) modelsToTry.add(m)
        }

        var lastError: String? = null

        for (model in modelsToTry) {
            Log.d(TAG, "Trying model: $model")

            val result = callLlmOnce(authHeader, messages, model, temperature)

            when (result) {
                is LlmResult.Success -> {
                    Log.i(TAG, "✓ Success with model: $model")
                    return Pair(result.action, null)
                }
                is LlmResult.RateLimited -> {
                    Log.w(TAG, "⚡ Rate limited on $model, trying next model...")
                    lastError = "Rate limited on $model. Trying alternatives..."
                    delay(RATE_LIMIT_WAIT_MS) // Brief pause before trying next
                    continue // Try next model immediately
                }
                is LlmResult.ModelNotFound -> {
                    Log.w(TAG, "✗ Model not found: $model")
                    lastError = "Model not found: $model"
                    continue // Try next model immediately
                }
                is LlmResult.AuthError -> {
                    return Pair(null, result.message) // Don't retry auth errors
                }
                is LlmResult.ServerError -> {
                    Log.w(TAG, "✗ Server error on $model: ${result.message}")
                    lastError = result.message
                    delay(3000) // Wait a bit on server errors
                    continue
                }
                is LlmResult.NetworkError -> {
                    return Pair(null, result.message) // Don't retry network errors
                }
                is LlmResult.OtherError -> {
                    lastError = result.message
                    continue
                }
            }
        }

        // All models exhausted
        return Pair(null,
            "All free models are rate-limited right now.\n" +
            "Free tier: 20 requests/min, 200/day per model.\n" +
            "Please wait 1-2 minutes and try again.\n\n" +
            "Last error: $lastError")
    }

    /** Result type for a single LLM call attempt. */
    private sealed class LlmResult {
        data class Success(val action: AgentAction) : LlmResult()
        data class RateLimited(val message: String) : LlmResult()
        data class ModelNotFound(val message: String) : LlmResult()
        data class AuthError(val message: String) : LlmResult()
        data class ServerError(val message: String) : LlmResult()
        data class NetworkError(val message: String) : LlmResult()
        data class OtherError(val message: String) : LlmResult()
    }

    /** Make exactly ONE API call to one model. No retries here - the caller handles fallback. */
    private suspend fun callLlmOnce(
        authHeader: String,
        messages: List<ChatMessage>,
        model: String,
        temperature: Double
    ): LlmResult {
        return try {
            val request = ChatCompletionRequest(
                model = model, messages = messages,
                temperature = temperature, maxTokens = Constants.DEFAULT_MAX_TOKENS
            )

            val response = openRouterApi.createChatCompletion(authHeader, request = request)

            if (response.isSuccessful) {
                val body = response.body()
                val apiError = body?.error
                if (apiError != null) {
                    val msg = apiError.message ?: "Unknown API error"
                    return when {
                        msg.contains("rate", ignoreCase = true) || msg.contains("limit", ignoreCase = true) ->
                            LlmResult.RateLimited(msg)
                        msg.contains("not found", ignoreCase = true) || msg.contains("No endpoints", ignoreCase = true) ->
                            LlmResult.ModelNotFound(msg)
                        msg.contains("credit", ignoreCase = true) || msg.contains("payment", ignoreCase = true) ->
                            LlmResult.AuthError("Credits required: $msg")
                        else -> LlmResult.OtherError("API error: $msg")
                    }
                }

                val content = body?.choices?.firstOrNull()?.message?.content
                if (content.isNullOrBlank()) {
                    return LlmResult.OtherError("Empty response from $model")
                }

                val action = responseParser.parse(content)
                LlmResult.Success(action)
            } else {
                val code = response.code()
                val errorBody = try { response.errorBody()?.string()?.take(500) ?: "" } catch (_: Exception) { "" }

                when (code) {
                    401 -> LlmResult.AuthError("Invalid API key (401). Check Settings.")
                    402 -> LlmResult.AuthError("Credits required (402). Free models should not need credits - try a different model ending in :free")
                    403 -> LlmResult.AuthError("Access denied (403) for $model.")
                    404 -> LlmResult.ModelNotFound("Model $model not found (404)")
                    429 -> LlmResult.RateLimited("Rate limited (429) on $model")
                    in 500..599 -> LlmResult.ServerError("Server error ($code) on $model")
                    else -> LlmResult.OtherError("HTTP $code on $model: ${errorBody.take(200)}")
                }
            }
        } catch (e: java.net.UnknownHostException) {
            LlmResult.NetworkError("No internet connection.")
        } catch (e: java.net.SocketTimeoutException) {
            LlmResult.ServerError("Timeout connecting to $model")
        } catch (e: javax.net.ssl.SSLException) {
            LlmResult.NetworkError("SSL error: ${e.message}")
        } catch (e: Exception) {
            LlmResult.OtherError("${e.javaClass.simpleName}: ${e.message}")
        }
    }

    fun pause() { isPaused = true; _agentState.value = AgentState.Paused }
    fun resume() { isPaused = false }
    fun cancel() { isCancelled = true; currentJob?.cancel(); _agentState.value = AgentState.Cancelled }
    fun reset() { isCancelled = false; isPaused = false; _agentState.value = AgentState.Idle }
    fun isRunning(): Boolean {
        val s = _agentState.value
        return s is AgentState.Thinking || s is AgentState.ExecutingTool || s is AgentState.Planning || s is AgentState.Responding
    }
}
