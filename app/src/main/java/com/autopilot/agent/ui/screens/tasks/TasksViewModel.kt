package com.autopilot.agent.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autopilot.agent.data.repository.ConversationRepository
import com.autopilot.agent.domain.model.Task
import com.autopilot.agent.domain.model.TaskStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TasksUiState(
    val tasks: List<Task> = emptyList(),
    val filter: String = "all",
    val isLoading: Boolean = true
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val conversationRepo: ConversationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    private val _filter = MutableStateFlow("all")

    init {
        viewModelScope.launch {
            _filter.flatMapLatest { filter ->
                when (filter) {
                    "running" -> conversationRepo.getTasksByStatus(TaskStatus.RUNNING)
                    "completed" -> conversationRepo.getTasksByStatus(TaskStatus.COMPLETED)
                    "failed" -> conversationRepo.getTasksByStatus(TaskStatus.FAILED)
                    else -> conversationRepo.getAllRootTasks()
                }
            }.collect { tasks ->
                _uiState.update {
                    it.copy(tasks = tasks, isLoading = false)
                }
            }
        }
    }

    fun setFilter(filter: String) {
        _filter.value = filter
        _uiState.update { it.copy(filter = filter) }
    }
}
