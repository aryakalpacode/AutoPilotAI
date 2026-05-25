package com.autopilot.agent.ui.screens.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autopilot.agent.data.local.dao.NoteDao
import com.autopilot.agent.data.local.entity.NoteEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotesUiState(
    val notes: List<NoteEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isGridView: Boolean = true,
    val editingNote: NoteEntity? = null,
    val showEditDialog: Boolean = false
)

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val noteDao: NoteDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _searchQuery.flatMapLatest { query ->
                if (query.isBlank()) noteDao.getAll()
                else noteDao.search(query)
            }.collect { notes ->
                _uiState.update { it.copy(notes = notes, isLoading = false) }
            }
        }
    }

    fun updateSearch(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleViewMode() {
        _uiState.update { it.copy(isGridView = !it.isGridView) }
    }

    fun showEditDialog(note: NoteEntity? = null) {
        _uiState.update { it.copy(showEditDialog = true, editingNote = note) }
    }

    fun hideEditDialog() {
        _uiState.update { it.copy(showEditDialog = false, editingNote = null) }
    }

    fun saveNote(title: String, content: String, tags: String) {
        viewModelScope.launch {
            val existing = _uiState.value.editingNote
            if (existing != null) {
                noteDao.update(
                    existing.copy(
                        title = title,
                        content = content,
                        tags = tags,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } else {
                noteDao.insert(
                    NoteEntity(
                        title = title,
                        content = content,
                        tags = tags
                    )
                )
            }
            hideEditDialog()
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            noteDao.delete(note)
        }
    }
}
