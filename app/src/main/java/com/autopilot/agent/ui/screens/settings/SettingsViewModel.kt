package com.autopilot.agent.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autopilot.agent.data.remote.dto.ModelInfo
import com.autopilot.agent.data.repository.AgentRepository
import com.autopilot.agent.data.repository.ConversationRepository
import com.autopilot.agent.data.repository.ModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val hasApiKey: Boolean = false,
    val maskedApiKey: String = "",
    val defaultModel: String = "",
    val freeModels: List<ModelInfo> = emptyList(),
    val maxIterations: Int = 20,
    val temperature: Float = 0.7f,
    val personality: String = "professional",
    val customPrompt: String = "",
    val autoConfirm: Boolean = false,
    val theme: String = "system",
    val fontSize: String = "medium",
    val isLoadingModels: Boolean = false,
    val showModelSelector: Boolean = false,
    val testResult: String? = null,
    val showApiKeyDialog: Boolean = false,
    val apiKeyInput: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val agentRepository: AgentRepository,
    private val modelRepository: ModelRepository,
    private val conversationRepo: ConversationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                agentRepository.defaultModel,
                agentRepository.maxIterations,
                agentRepository.temperature,
                agentRepository.personality,
                agentRepository.autoConfirm,
                agentRepository.theme,
                agentRepository.fontSize
            ) { values ->
                val model = values[0] as String
                val maxIter = values[1] as Int
                val temp = values[2] as Float
                val personality = values[3] as String
                val autoConfirm = values[4] as Boolean
                val theme = values[5] as String
                val fontSize = values[6] as String
                SettingsUiState(
                    defaultModel = model,
                    maxIterations = maxIter,
                    temperature = temp,
                    personality = personality,
                    autoConfirm = autoConfirm,
                    theme = theme,
                    fontSize = fontSize
                )
            }.collect { state ->
                val hasKey = modelRepository.hasApiKey()
                val maskedKey = if (hasKey) {
                    val key = modelRepository.getApiKey()
                    if (key != null && key.length > 8) "••••••••${key.takeLast(8)}" else "••••••••"
                } else ""

                _uiState.value = state.copy(
                    hasApiKey = hasKey,
                    maskedApiKey = maskedKey,
                    freeModels = modelRepository.getCachedFreeModels()
                )
            }
        }
    }

    fun refreshModels() {
        _uiState.update { it.copy(isLoadingModels = true) }
        viewModelScope.launch {
            val result = modelRepository.fetchFreeModels()
            result.fold(
                onSuccess = { models ->
                    _uiState.update { it.copy(freeModels = models, isLoadingModels = false) }
                },
                onFailure = {
                    _uiState.update { it.copy(isLoadingModels = false) }
                }
            )
        }
    }

    fun setDefaultModel(modelId: String) {
        viewModelScope.launch {
            agentRepository.setDefaultModel(modelId)
            _uiState.update { it.copy(defaultModel = modelId, showModelSelector = false) }
        }
    }

    fun setMaxIterations(value: Int) {
        viewModelScope.launch { agentRepository.setMaxIterations(value) }
    }

    fun setTemperature(value: Float) {
        viewModelScope.launch { agentRepository.setTemperature(value) }
    }

    fun setPersonality(value: String) {
        viewModelScope.launch { agentRepository.setPersonality(value) }
    }

    fun setAutoConfirm(value: Boolean) {
        viewModelScope.launch { agentRepository.setAutoConfirm(value) }
    }

    fun setTheme(value: String) {
        viewModelScope.launch { agentRepository.setTheme(value) }
    }

    fun setFontSize(value: String) {
        viewModelScope.launch { agentRepository.setFontSize(value) }
    }

    fun showModelSelector() {
        _uiState.update { it.copy(showModelSelector = true) }
    }

    fun hideModelSelector() {
        _uiState.update { it.copy(showModelSelector = false) }
    }

    fun showApiKeyDialog() {
        _uiState.update { it.copy(showApiKeyDialog = true, apiKeyInput = "") }
    }

    fun hideApiKeyDialog() {
        _uiState.update { it.copy(showApiKeyDialog = false, apiKeyInput = "", testResult = null) }
    }

    fun updateApiKeyInput(key: String) {
        _uiState.update { it.copy(apiKeyInput = key) }
    }

    fun testApiKey() {
        val key = _uiState.value.apiKeyInput.trim()
        if (key.isBlank()) return

        _uiState.update { it.copy(testResult = "Testing...") }
        viewModelScope.launch {
            val result = modelRepository.testConnection(key)
            result.fold(
                onSuccess = { count ->
                    _uiState.update { it.copy(testResult = "✓ Success! Found $count free models.") }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(testResult = "✗ Failed: ${error.message}") }
                }
            )
        }
    }

    fun saveApiKey() {
        val key = _uiState.value.apiKeyInput.trim()
        if (key.isBlank()) return

        viewModelScope.launch {
            modelRepository.saveApiKey(key)
            _uiState.update {
                it.copy(
                    showApiKeyDialog = false,
                    hasApiKey = true,
                    maskedApiKey = "••••••••${key.takeLast(8)}"
                )
            }
        }
    }

    fun deleteApiKey() {
        viewModelScope.launch {
            modelRepository.deleteApiKey()
            _uiState.update { it.copy(hasApiKey = false, maskedApiKey = "") }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            conversationRepo.deleteAllConversations()
        }
    }
}
