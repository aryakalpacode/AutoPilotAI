package com.autopilot.agent.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autopilot.agent.data.repository.AgentRepository
import com.autopilot.agent.data.repository.ConversationRepository
import com.autopilot.agent.data.repository.ModelRepository
import com.autopilot.agent.domain.model.Conversation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val recentConversations: List<Conversation> = emptyList(),
    val defaultModel: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val conversationRepo: ConversationRepository,
    private val agentRepository: AgentRepository,
    private val modelRepository: ModelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                conversationRepo.getRecentConversations(10),
                agentRepository.defaultModel
            ) { conversations, model ->
                HomeUiState(
                    recentConversations = conversations,
                    defaultModel = model,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }

        // Pre-fetch models in background
        viewModelScope.launch {
            modelRepository.fetchFreeModels()
        }
    }

    fun createNewConversation(
        title: String = "New Chat",
        templatePrompt: String? = null,
        onCreated: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val model = _uiState.value.defaultModel.ifBlank { "deepseek/deepseek-chat" }
            val id = conversationRepo.createConversation(title, model)
            onCreated(id)
        }
    }

    fun deleteConversation(id: Long) {
        viewModelScope.launch {
            conversationRepo.deleteConversation(id)
        }
    }

    fun getTemplatePrompt(templateName: String): String {
        return when (templateName) {
            "research" -> "Research the following topic thoroughly. Search the web for information, read relevant sources, summarize key findings, and save a structured report to notes. Topic: "
            "news" -> "Get me a daily news briefing. Search for the latest news on the following topics, summarize each story, and compile them into a briefing. Topics: "
            "code" -> "Generate code for the following requirement. Understand the requirements, generate the code, test it if possible, fix any errors, and save the final code to a file. Requirement: "
            "write" -> "Write a comprehensive article/content about the following. Research the topic, create an outline, write a draft, review and improve it, then save the final version. Topic: "
            "analyze" -> "Analyze the following data or topic. Read the data, understand its structure, compute relevant statistics, generate insights, and create a report. Data/Topic: "
            "study" -> "Create study materials for the following topic. Search for explanations, create flashcard-style notes, generate quiz questions, and save everything to notes. Topic: "
            "travel" -> "Plan a trip to the following destination. Search for activities and attractions, create a day-by-day itinerary, estimate costs, and save the plan. Destination: "
            "custom" -> "Execute the following custom workflow: "
            else -> ""
        }
    }
}
