package com.autopilot.agent.ui.screens.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autopilot.agent.data.remote.dto.ModelInfo
import com.autopilot.agent.data.repository.AgentRepository
import com.autopilot.agent.data.repository.ModelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUiState(
    val apiKey: String = "",
    val isTestingConnection: Boolean = false,
    val connectionResult: String? = null,
    val isConnectionSuccess: Boolean = false,
    val freeModels: List<ModelInfo> = emptyList(),
    val selectedModel: String = "",
    val canProceed: Boolean = false
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val modelRepository: ModelRepository,
    private val agentRepository: AgentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    fun updateApiKey(key: String) {
        _uiState.update { it.copy(apiKey = key, connectionResult = null) }
    }

    fun testConnection() {
        val key = _uiState.value.apiKey.trim()
        if (key.isBlank()) {
            _uiState.update { it.copy(connectionResult = "Please enter an API key") }
            return
        }

        _uiState.update { it.copy(isTestingConnection = true, connectionResult = null) }

        viewModelScope.launch {
            val result = modelRepository.testConnection(key)
            result.fold(
                onSuccess = { count ->
                    val models = modelRepository.getCachedFreeModels()
                    val defaultModel = models.firstOrNull()?.id ?: ""
                    _uiState.update {
                        it.copy(
                            isTestingConnection = false,
                            connectionResult = "Success! Found $count free models.",
                            isConnectionSuccess = true,
                            freeModels = models,
                            selectedModel = defaultModel,
                            canProceed = true
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isTestingConnection = false,
                            connectionResult = "Failed: ${error.message}",
                            isConnectionSuccess = false,
                            canProceed = false
                        )
                    }
                }
            )
        }
    }

    fun selectModel(modelId: String) {
        _uiState.update { it.copy(selectedModel = modelId) }
    }

    fun completeSetup(onComplete: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.apiKey.isNotBlank()) {
                modelRepository.saveApiKey(state.apiKey.trim())
            }
            if (state.selectedModel.isNotBlank()) {
                agentRepository.setDefaultModel(state.selectedModel)
            }
            agentRepository.setSetupComplete(true)
            onComplete()
        }
    }
}
