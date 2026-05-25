package com.autopilot.agent.ui.screens.chat

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autopilot.agent.agent.AgentOrchestrator
import com.autopilot.agent.data.repository.AgentRepository
import com.autopilot.agent.data.repository.ConversationRepository
import com.autopilot.agent.domain.model.*
import com.autopilot.agent.service.AgentForegroundService
import com.autopilot.agent.ui.state.ChatUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.resume

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val agentOrchestrator: AgentOrchestrator,
    private val conversationRepo: ConversationRepository,
    private val agentRepository: AgentRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var agentJob: Job? = null
    private var confirmationContinuation: ((Boolean) -> Unit)? = null
    private var hasAutoSent = false

    val conversationId: Long
        get() = _uiState.value.conversationId

    fun initialize(conversationId: Long, initialPrompt: String? = null) {
        if (_uiState.value.conversationId == conversationId) return // Already initialized

        _uiState.update { it.copy(conversationId = conversationId) }

        viewModelScope.launch {
            conversationRepo.getMessages(conversationId).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }

        viewModelScope.launch {
            conversationRepo.getTasks(conversationId).collect { tasks ->
                _uiState.update { it.copy(tasks = tasks) }
            }
        }

        viewModelScope.launch {
            val conv = conversationRepo.getConversation(conversationId)
            if (conv != null) {
                _uiState.update {
                    it.copy(conversationTitle = conv.title, currentModel = conv.modelUsed)
                }
            }
        }

        viewModelScope.launch {
            combine(
                agentRepository.maxIterations,
                agentRepository.defaultModel,
                agentRepository.autoConfirm
            ) { maxIter, model, _ -> Triple(maxIter, model, false) }
                .collect { (maxIter, model, _) ->
                    _uiState.update {
                        it.copy(
                            maxIterations = maxIter,
                            currentModel = it.currentModel.ifBlank { model }
                        )
                    }
                }
        }

        viewModelScope.launch {
            agentOrchestrator.agentState.collect { state ->
                _uiState.update { it.copy(agentState = state) }
            }
        }

        agentOrchestrator.onConfirmationNeeded = { action, reason ->
            suspendCancellableCoroutine { continuation ->
                _uiState.update {
                    it.copy(showConfirmDialog = true, confirmAction = action, confirmReason = reason)
                }
                confirmationContinuation = { confirmed -> continuation.resume(confirmed) }
            }
        }

        // If there's an initial prompt, set it up for auto-send
        if (initialPrompt != null && initialPrompt.isNotBlank() && !hasAutoSent) {
            _uiState.update { it.copy(inputText = initialPrompt) }
        }
    }

    fun updateInput(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return

        hasAutoSent = true
        _uiState.update { it.copy(inputText = "", isLoading = true, error = null) }

        agentJob = viewModelScope.launch {
            val state = _uiState.value
            val model = state.currentModel.ifBlank { "deepseek/deepseek-v4-flash:free" }
            val autoConfirm = agentRepository.autoConfirm.first()
            val personality = agentRepository.personality.first()
            val temperature = agentRepository.temperature.first().toDouble()
            val maxIterations = state.maxIterations

            // Start foreground service
            try {
                val serviceIntent = Intent(appContext, AgentForegroundService::class.java).apply {
                    action = AgentForegroundService.ACTION_START
                    putExtra(AgentForegroundService.EXTRA_TASK_NAME, text.take(50))
                }
                appContext.startForegroundService(serviceIntent)
            } catch (_: Exception) {}

            var iterCount = 0
            val result = agentOrchestrator.executeTask(
                userPrompt = text,
                conversationId = _uiState.value.conversationId,
                model = model,
                maxIterations = maxIterations,
                temperature = temperature,
                personality = personality,
                autoConfirm = autoConfirm
            ) { agentState ->
                iterCount++
                _uiState.update {
                    it.copy(agentState = agentState, currentIteration = iterCount)
                }
            }

            // Stop foreground service
            try {
                val stopIntent = Intent(appContext, AgentForegroundService::class.java).apply {
                    action = AgentForegroundService.ACTION_STOP
                }
                appContext.startService(stopIntent)
            } catch (_: Exception) {}

            _uiState.update {
                it.copy(
                    isLoading = false,
                    currentIteration = 0,
                    error = result.exceptionOrNull()?.message
                )
            }

            // Update conversation title on first message
            val messages = conversationRepo.getMessagesSync(_uiState.value.conversationId)
            val userMessages = messages.filter { it.role == MessageRole.USER }
            if (userMessages.size == 1) {
                val title = text.take(50).let { t -> if (text.length > 50) "$t..." else t }
                conversationRepo.updateTitle(_uiState.value.conversationId, title)
                _uiState.update { it.copy(conversationTitle = title) }
            }
        }
    }

    fun confirmAction(confirmed: Boolean) {
        _uiState.update { it.copy(showConfirmDialog = false, confirmAction = null, confirmReason = "") }
        confirmationContinuation?.invoke(confirmed)
        confirmationContinuation = null
    }

    fun toggleDebugView() {
        _uiState.update { it.copy(showDebugView = !it.showDebugView) }
    }

    fun toggleTaskProgress() {
        _uiState.update { it.copy(showTaskProgress = !it.showTaskProgress) }
    }

    fun stopAgent() {
        agentOrchestrator.cancel()
        agentJob?.cancel()
        _uiState.update { it.copy(isLoading = false, agentState = AgentState.Cancelled) }
    }

    fun pauseAgent() {
        agentOrchestrator.pause()
    }

    fun resumeAgent() {
        agentOrchestrator.resume()
    }

    fun exportConversation(): File? {
        val state = _uiState.value
        return try {
            val dir = File(appContext.getExternalFilesDir(null), "exports")
            dir.mkdirs()
            val file = File(dir, "${state.conversationTitle.replace("[^a-zA-Z0-9]".toRegex(), "_")}.md")
            file.writeText(buildString {
                appendLine("# ${state.conversationTitle}")
                appendLine("Model: ${state.currentModel}")
                appendLine("---\n")
                state.messages.forEach { msg ->
                    when (msg.role) {
                        MessageRole.USER -> appendLine("**You:** ${msg.content}\n")
                        MessageRole.ASSISTANT -> appendLine("**AI:** ${msg.content}\n")
                        MessageRole.TOOL_CALL -> appendLine("*🔧 Tool: ${msg.toolName} - ${msg.content}*\n")
                        MessageRole.TOOL_RESULT -> appendLine("*📋 Result: ${msg.content.take(300)}*\n")
                        MessageRole.ERROR -> appendLine("*⚠️ Error: ${msg.content}*\n")
                        MessageRole.SYSTEM -> appendLine("*System: ${msg.content}*\n")
                    }
                }
            })
            file
        } catch (_: Exception) { null }
    }

    fun deleteConversation(onDeleted: () -> Unit) {
        viewModelScope.launch {
            conversationRepo.deleteConversation(_uiState.value.conversationId)
            onDeleted()
        }
    }

    fun renameConversation(newTitle: String) {
        viewModelScope.launch {
            conversationRepo.updateTitle(_uiState.value.conversationId, newTitle)
            _uiState.update { it.copy(conversationTitle = newTitle) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        agentOrchestrator.reset()
    }
}
