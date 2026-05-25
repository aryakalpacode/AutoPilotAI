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

/**
 * The brain of AutoPilot AI. Implements the autonomous ReAct (Reason + Act) loop.
 */
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

        /** Fallback free models to try if the selected model fails */
        private val FALLBACK_MODELS = listOf(
            "deepseek/deepseek-chat:free",
            "deepseek/deepseek-r1-0528:free",
            "meta-llama/llama-3.1-8b-instruct:free",
            "google/gemma-2-9b-it:free",
            "mistralai/mistral-7b-instruct:free",
            "qwen/qwen-2-7b-instruct:free"
        )
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

    @Volatile
    private var isPaused = false

    @Volatile
    private var isCancelled = false

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
        var currentModel = model

        try {
            conversationRepo.addMessage(
                Message(
                    conversationId = conversationId,
                    role = MessageRole.USER,
                    content = userPrompt,
                    model = currentModel
                )
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
                        val finalMsg = "Failed after $consecutiveErrors attempts. Last error: $errorMsg"
                        _agentState.value = AgentState.Error(finalMsg)
                        onUpdate(AgentState.Error(finalMsg))

                        // Also save the detailed error as a message so user sees it
                        conversationRepo.addMessage(
                            Message(
                                conversationId = conversationId,
                                role = MessageRole.ERROR,
                                content = finalMsg
                            )
                        )
                        return@withContext Result.failure(Exception(finalMsg))
                    }
                    conversationRepo.addMessage(
                        Message(
                            conversationId = conversationId,
                            role = MessageRole.ERROR,
                            content = "LLM call failed. Retrying... (attempt $consecutiveErrors)\nError: $errorMsg"
                        )
                    )
                    delay(Constants.INITIAL_BACKOFF_MS * consecutiveErrors)
                    continue
                }

                consecutiveErrors = 0
                Log.d(TAG, "Action: ${action.action}, Confidence: ${action.confidence}")

                val actionJson = gson.toJson(action)
                conversationRepo.addMessage(
                    Message(
                        conversationId = conversationId,
                        role = MessageRole.ASSISTANT,
                        content = actionJson,
                        model = currentModel,
                        tokenCount = actionJson.estimateTokens()
                    )
                )

                when {
                    action.isComplete -> {
                        val summary = action.getStringInput("summary")
                        val details = action.getStringInput("details")
                        val finalMessage = if (details.isNotBlank()) "$summary\n\n$details" else summary

                        conversationRepo.addMessage(
                            Message(
                                conversationId = conversationId,
                                role = MessageRole.ASSISTANT,
                                content = finalMessage,
                                model = currentModel
                            )
                        )

                        _agentState.value = AgentState.Complete(summary, details)
                        onUpdate(AgentState.Complete(summary, details))
                        return@withContext Result.success(finalMessage)
                    }

                    action.isRespond -> {
                        val message = action.getStringInput("message")
                        conversationRepo.addMessage(
                            Message(
                                conversationId = conversationId,
                                role = MessageRole.ASSISTANT,
                                content = message,
                                model = currentModel
                            )
                        )
                        _agentState.value = AgentState.Responding(message)
                        onUpdate(AgentState.Responding(message))
                        continue
                    }

                    action.isTool -> {
                        val tool = tools[action.action]
                        if (tool == null) {
                            conversationRepo.addMessage(
                                Message(
                                    conversationId = conversationId,
                                    role = MessageRole.ERROR,
                                    content = "Unknown tool: ${action.action}"
                                )
                            )
                            continue
                        }

                        val needsConfirmation = !autoConfirm && (
                            tool.requiresConfirmation ||
                            iteration > 10 ||
                            action.confidence < Constants.CONFIDENCE_THRESHOLD
                        )

                        if (needsConfirmation) {
                            val reason = when {
                                tool.requiresConfirmation -> "This action modifies data (${tool.name})"
                                iteration > 10 -> "Over 10 iterations have been executed"
                                action.confidence < Constants.CONFIDENCE_THRESHOLD -> "Low confidence (${action.confidence})"
                                else -> "Action requires confirmation"
                            }
                            _agentState.value = AgentState.WaitingForConfirmation(action, reason)
                            onUpdate(AgentState.WaitingForConfirmation(action, reason))
                            val confirmed = onConfirmationNeeded?.invoke(action, reason) ?: true
                            if (!confirmed) {
                                conversationRepo.addMessage(
                                    Message(
                                        conversationId = conversationId,
                                        role = MessageRole.SYSTEM,
                                        content = "User declined to execute ${action.action}. Skipping."
                                    )
                                )
                                continue
                            }
                        }

                        _agentState.value = AgentState.ExecutingTool(tool.name)
                        onUpdate(AgentState.ExecutingTool(tool.name))

                        conversationRepo.addMessage(
                            Message(
                                conversationId = conversationId,
                                role = MessageRole.TOOL_CALL,
                                content = "Calling ${tool.name} with: ${gson.toJson(action.actionInput)}",
                                toolName = tool.name,
                                toolInput = gson.toJson(action.actionInput)
                            )
                        )

                        val result = try {
                            tool.execute(action.actionInput)
                        } catch (e: Exception) {
                            ToolResult(tool.name, false, "", "Tool execution error: ${e.message}")
                        }

                        conversationRepo.addMessage(
                            Message(
                                conversationId = conversationId,
                                role = MessageRole.TOOL_RESULT,
                                content = result.toContextString(),
                                toolName = tool.name,
                                toolOutput = result.output.take(4000)
                            )
                        )

                        if (!result.success) {
                            consecutiveErrors++
                            if (consecutiveErrors >= Constants.MAX_CONSECUTIVE_ERRORS) {
                                val errorMsg = "Tool failed $consecutiveErrors consecutive times. Stopping."
                                _agentState.value = AgentState.Error(errorMsg)
                                onUpdate(AgentState.Error(errorMsg))
                                return@withContext Result.failure(Exception(errorMsg))
                            }
                        } else {
                            consecutiveErrors = 0
                        }
                    }

                    else -> {
                        conversationRepo.addMessage(
                            Message(
                                conversationId = conversationId,
                                role = MessageRole.ERROR,
                                content = "Unknown action: ${action.action}"
                            )
                        )
                    }
                }
            }

            if (isCancelled) {
                _agentState.value = AgentState.Cancelled
                onUpdate(AgentState.Cancelled)
                return@withContext Result.failure(Exception("Task cancelled by user"))
            }

            val maxIterMsg = "Reached maximum iterations ($maxIterations). Task may be incomplete."
            _agentState.value = AgentState.Error(maxIterMsg, canRetry = true)
            onUpdate(AgentState.Error(maxIterMsg, canRetry = true))
            return@withContext Result.failure(Exception(maxIterMsg))

        } catch (e: CancellationException) {
            _agentState.value = AgentState.Cancelled
            onUpdate(AgentState.Cancelled)
            return@withContext Result.failure(e)
        } catch (e: Exception) {
            val errorMsg = "Error: ${e.message}"
            Log.e(TAG, errorMsg, e)
            _agentState.value = AgentState.Error(errorMsg)
            onUpdate(AgentState.Error(errorMsg))
            // Save error so user can see it in chat
            try {
                conversationRepo.addMessage(
                    Message(conversationId = conversationId, role = MessageRole.ERROR, content = errorMsg)
                )
            } catch (_: Exception) {}
            return@withContext Result.failure(e)
        }
    }

    /**
     * Try the selected model first, then fallback to other free models if it fails.
     * Returns (AgentAction?, errorDetail)
     */
    private suspend fun callLlmWithFallback(
        messages: List<ChatMessage>,
        primaryModel: String,
        temperature: Double
    ): Pair<AgentAction?, String?> {
        // Try primary model
        val (action, error) = callLlmSingle(messages, primaryModel, temperature)
        if (action != null) return Pair(action, null)

        // If primary model returned 404/model-not-found, try fallbacks
        if (error != null && (error.contains("404") || error.contains("not found", ignoreCase = true)
                    || error.contains("not available", ignoreCase = true)
                    || error.contains("No endpoints", ignoreCase = true))) {
            Log.w(TAG, "Primary model $primaryModel not available, trying fallbacks...")

            for (fallbackModel in FALLBACK_MODELS) {
                if (fallbackModel == primaryModel) continue
                Log.d(TAG, "Trying fallback model: $fallbackModel")
                val (fbAction, fbError) = callLlmSingle(messages, fallbackModel, temperature)
                if (fbAction != null) {
                    Log.i(TAG, "Fallback model $fallbackModel succeeded!")
                    return Pair(fbAction, null)
                }
                Log.w(TAG, "Fallback $fallbackModel also failed: $fbError")
            }
        }

        return Pair(null, error ?: "All models failed")
    }

    /**
     * Single attempt to call one model with retries.
     * Returns (AgentAction?, errorDetailString)
     */
    private suspend fun callLlmSingle(
        messages: List<ChatMessage>,
        model: String,
        temperature: Double
    ): Pair<AgentAction?, String?> {
        val authHeader = modelRepository.getAuthHeader()
            ?: return Pair(null, "No API key configured. Go to Settings to add your OpenRouter API key.")

        var backoffMs = Constants.INITIAL_BACKOFF_MS
        var lastError: String? = null

        repeat(Constants.MAX_RETRIES) { attempt ->
            try {
                val request = ChatCompletionRequest(
                    model = model,
                    messages = messages,
                    temperature = temperature,
                    maxTokens = Constants.DEFAULT_MAX_TOKENS
                )

                Log.d(TAG, "API call attempt ${attempt+1} to model: $model")
                val response = openRouterApi.createChatCompletion(authHeader, request = request)

                if (response.isSuccessful) {
                    val responseBody = response.body()

                    // Check for API-level error in response body
                    val apiError = responseBody?.error
                    if (apiError != null) {
                        lastError = "API error: ${apiError.message ?: "Unknown"} (code: ${apiError.code})"
                        Log.w(TAG, "API returned error in body: $lastError")
                        return Pair(null, lastError)
                    }

                    val content = responseBody?.choices?.firstOrNull()?.message?.content

                    if (content.isNullOrBlank()) {
                        Log.w(TAG, "Empty LLM response on attempt ${attempt + 1}")
                        lastError = "Empty response from model $model"
                        delay(backoffMs)
                        backoffMs = (backoffMs * 2).coerceAtMost(Constants.MAX_BACKOFF_MS)
                        return@repeat // continue to next attempt
                    }

                    Log.d(TAG, "Got response (${content.length} chars) from $model")
                    val action = responseParser.parse(content)
                    return Pair(action, null)

                } else {
                    val code = response.code()
                    val errorBody = try { response.errorBody()?.string() ?: "" } catch (_: Exception) { "" }
                    Log.w(TAG, "HTTP $code on attempt ${attempt + 1}: $errorBody")

                    lastError = "HTTP $code: ${errorBody.take(300)}"

                    when (code) {
                        401 -> return Pair(null, "Invalid API key (401). Check your OpenRouter key in Settings.")
                        402 -> return Pair(null, "Payment required (402). Your OpenRouter account may need credits.")
                        403 -> return Pair(null, "Forbidden (403). This model may require special access.")
                        404 -> return Pair(null, "Model not found (404): $model")
                        429 -> {
                            lastError = "Rate limited (429). Waiting before retry..."
                            delay(backoffMs)
                            backoffMs = (backoffMs * 2).coerceAtMost(Constants.MAX_BACKOFF_MS)
                        }
                        in 500..599 -> {
                            lastError = "OpenRouter server error ($code). Retrying..."
                            delay(backoffMs)
                            backoffMs = (backoffMs * 2).coerceAtMost(Constants.MAX_BACKOFF_MS)
                        }
                        else -> {
                            lastError = "API error $code: ${errorBody.take(200)}"
                            delay(backoffMs)
                            backoffMs = (backoffMs * 2).coerceAtMost(Constants.MAX_BACKOFF_MS)
                        }
                    }
                }
            } catch (e: java.net.UnknownHostException) {
                lastError = "No internet connection. Check your network."
                return Pair(null, lastError)
            } catch (e: java.net.SocketTimeoutException) {
                lastError = "Connection timed out. The server may be overloaded."
                delay(backoffMs)
                backoffMs = (backoffMs * 2).coerceAtMost(Constants.MAX_BACKOFF_MS)
            } catch (e: javax.net.ssl.SSLException) {
                lastError = "SSL/TLS error: ${e.message}"
                return Pair(null, lastError)
            } catch (e: Exception) {
                Log.e(TAG, "Exception on attempt ${attempt + 1}", e)
                lastError = "${e.javaClass.simpleName}: ${e.message}"
                delay(backoffMs)
                backoffMs = (backoffMs * 2).coerceAtMost(Constants.MAX_BACKOFF_MS)
            }
        }

        return Pair(null, lastError ?: "Failed after ${Constants.MAX_RETRIES} attempts")
    }

    fun pause() {
        isPaused = true
        _agentState.value = AgentState.Paused
    }

    fun resume() {
        isPaused = false
    }

    fun cancel() {
        isCancelled = true
        currentJob?.cancel()
        _agentState.value = AgentState.Cancelled
    }

    fun reset() {
        isCancelled = false
        isPaused = false
        _agentState.value = AgentState.Idle
    }

    fun isRunning(): Boolean {
        val state = _agentState.value
        return state is AgentState.Thinking ||
                state is AgentState.ExecutingTool ||
                state is AgentState.Planning ||
                state is AgentState.Responding
    }
}
