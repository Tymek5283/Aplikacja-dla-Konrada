// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/ui/screens/notes/NotesViewModel.kt
// Opis: ViewModel zarządzający stanem ekranu notatek i logiką biznesową
package com.qjproject.liturgicalcalendar.ui.screens.notes

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qjproject.liturgicalcalendar.data.Note
import com.qjproject.liturgicalcalendar.data.NotesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showAddDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val noteToDelete: Note? = null
)

class NotesViewModel(
    private val notesRepository: NotesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    init {
        loadNotes()
    }

    fun loadNotes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val notes = notesRepository.getAllNotes()
                _uiState.value = _uiState.value.copy(
                    notes = notes,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Błąd podczas wczytywania notatek: ${e.message}"
                )
            }
        }
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true)
    }

    fun hideAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false)
    }

    fun addNote(title: String, description: String) {
        viewModelScope.launch {
            try {
                val result = notesRepository.addNote(title, description)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(showAddDialog = false)
                        loadNotes() // Odśwież listę notatek
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Błąd podczas dodawania notatki: ${exception.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Błąd podczas dodawania notatki: ${e.message}"
                )
            }
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            try {
                val result = notesRepository.deleteNote(noteId)
                result.fold(
                    onSuccess = {
                        loadNotes() // Odśwież listę notatek
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Błąd podczas usuwania notatki: ${exception.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Błąd podczas usuwania notatki: ${e.message}"
                )
            }
        }
    }

    fun showDeleteDialog(note: Note) {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = true,
            noteToDelete = note
        )
    }

    fun hideDeleteDialog() {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = false,
            noteToDelete = null
        )
    }

    fun confirmDeleteNote() {
        val noteToDelete = _uiState.value.noteToDelete
        if (noteToDelete != null) {
            deleteNote(noteToDelete.id)
            hideDeleteDialog()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun refreshNotes() {
        loadNotes()
    }
}

class NotesViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotesViewModel::class.java)) {
            return NotesViewModel(NotesRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
