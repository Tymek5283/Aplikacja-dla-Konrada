// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/ui/screens/notes/NoteDetailsViewModel.kt
// Opis: ViewModel zarządzający stanem ekranu szczegółów notatki z automatycznym zapisem
package com.qjproject.liturgicalcalendar.ui.screens.notes

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import com.qjproject.liturgicalcalendar.data.Note
import com.qjproject.liturgicalcalendar.data.NotesRepository
import androidx.compose.runtime.mutableStateOf
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

data class NoteDetailsUiState(
    val note: Note? = null,
    val htmlContent: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val richTextState: RichTextState? = null,
    val currentSelection: TextRange = TextRange.Zero,
    val hasUnsavedChanges: Boolean = false,
    val showEditTitleDescriptionDialog: Boolean = false,
    val showColorPickerDialog: Boolean = false,
    val selectedColor: Color? = null,
    val activeTextColor: Color? = null
)

class NoteDetailsViewModel(
    private val noteId: String,
    private val notesRepository: NotesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteDetailsUiState())
    val uiState: StateFlow<NoteDetailsUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null
    private var lastSaveTime = 0L
    private val autoSaveDelayMs = 3000L // 3 sekundy
    
    val richTextState = mutableStateOf<RichTextState?>(null)

    init {
        loadNote()
    }

    private fun loadNote() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val note = notesRepository.getNoteById(noteId)
                if (note != null) {
                    _uiState.value = _uiState.value.copy(
                        note = note,
                        htmlContent = note.content,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Nie znaleziono notatki"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Błąd podczas wczytywania notatki: ${e.message}"
                )
            }
        }
    }

    fun updateHtmlContent(newHtmlContent: String) {
        val currentState = _uiState.value
        if (currentState.htmlContent != newHtmlContent) {
            _uiState.value = currentState.copy(
                htmlContent = newHtmlContent,
                hasUnsavedChanges = true
            )
            scheduleAutoSave()
        }
    }

    fun onSelectionChange(selection: TextRange) {
        _uiState.value = _uiState.value.copy(currentSelection = selection)
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(autoSaveDelayMs)
            saveChanges()
        }
    }

    private fun saveChanges() {
        val currentState = _uiState.value
        val note = currentState.note
        
        if (note != null && currentState.hasUnsavedChanges) {
            viewModelScope.launch {
                try {
                    val updatedNote = note.copy(
                        content = currentState.htmlContent,
                        modifiedAt = System.currentTimeMillis()
                    )
                    
                    val result = notesRepository.updateNote(updatedNote)
                    result.fold(
                        onSuccess = {
                            _uiState.value = _uiState.value.copy(
                                note = updatedNote,
                                hasUnsavedChanges = false
                            )
                            lastSaveTime = System.currentTimeMillis()
                        },
                        onFailure = { exception ->
                            _uiState.value = _uiState.value.copy(
                                errorMessage = "Błąd podczas zapisywania: ${exception.message}"
                            )
                        }
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Błąd podczas zapisywania: ${e.message}"
                    )
                }
            }
        }
    }

    fun saveImmediately() {
        autoSaveJob?.cancel()
        // Synchroniczny zapis - czekaj na zakończenie
        runBlocking {
            saveChangesSync()
        }
    }
    
    private suspend fun saveChangesSync() {
        val currentState = _uiState.value
        val note = currentState.note
        
        if (note != null && currentState.hasUnsavedChanges) {
            try {
                val updatedNote = note.copy(
                    content = currentState.htmlContent,
                    modifiedAt = System.currentTimeMillis()
                )
                
                val result = notesRepository.updateNote(updatedNote)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            note = updatedNote,
                            hasUnsavedChanges = false
                        )
                        lastSaveTime = System.currentTimeMillis()
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Błąd podczas zapisywania: ${exception.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Błąd podczas zapisywania: ${e.message}"
                )
            }
        }
    }

    fun toggleBold() {
        richTextState.value?.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
    }

    fun toggleItalic() {
        richTextState.value?.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
    }

    fun toggleUnderline() {
        richTextState.value?.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline))
    }

    fun toggleColor() {
        _uiState.value = _uiState.value.copy(showColorPickerDialog = true)
    }

    fun applyColor(color: Color) {
        richTextState.value?.let { state ->
            // Jeśli jest zaznaczenie, zastosuj kolor do zaznaczonego tekstu
            if (state.selection.length > 0) {
                state.addSpanStyle(
                    androidx.compose.ui.text.SpanStyle(color = color),
                    TextRange(state.selection.start, state.selection.end)
                )
            } else {
                // Jeśli nie ma zaznaczenia, ustaw kolor dla nowego tekstu
                state.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(color = color))
            }
        }
        _uiState.value = _uiState.value.copy(
            selectedColor = color,
            activeTextColor = color,
            showColorPickerDialog = false
        )
    }

    fun toggleList() {
        richTextState.value?.toggleUnorderedList()
    }

    fun showColorPickerDialog() {
        _uiState.value = _uiState.value.copy(showColorPickerDialog = true)
    }

    fun hideColorPickerDialog() {
        _uiState.value = _uiState.value.copy(showColorPickerDialog = false)
    }
    
    fun removeColor() {
        richTextState.value?.let { state ->
            // Usuń kolor z zaznaczonego tekstu lub z nowego tekstu
            if (state.selection.length > 0) {
                // Dla zaznaczonego tekstu - zastąp aktualny kolor domyślnym kolorem białym
                state.addSpanStyle(
                    androidx.compose.ui.text.SpanStyle(color = Color.White),
                    TextRange(state.selection.start, state.selection.end)
                )
            } else {
                // Reset stylu dla nowego tekstu - ustaw domyślny kolor biały
                state.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(color = Color.White))
            }
        }
        _uiState.value = _uiState.value.copy(
            selectedColor = Color.White,
            activeTextColor = null,
            showColorPickerDialog = false
        )
    }

    fun showEditTitleDescriptionDialog() {
        _uiState.value = _uiState.value.copy(showEditTitleDescriptionDialog = true)
    }

    fun hideEditTitleDescriptionDialog() {
        _uiState.value = _uiState.value.copy(showEditTitleDescriptionDialog = false)
    }

    fun updateTitleAndDescription(newTitle: String, newDescription: String) {
        val currentState = _uiState.value
        val note = currentState.note
        
        if (note != null) {
            viewModelScope.launch {
                try {
                    val updatedNote = note.copy(
                        title = newTitle,
                        description = newDescription,
                        modifiedAt = System.currentTimeMillis()
                    )
                    
                    val result = notesRepository.updateNote(updatedNote)
                    result.fold(
                        onSuccess = {
                            _uiState.value = _uiState.value.copy(
                                note = updatedNote,
                                showEditTitleDescriptionDialog = false
                            )
                        },
                        onFailure = { exception ->
                            _uiState.value = _uiState.value.copy(
                                errorMessage = "Błąd podczas zapisywania: ${exception.message}"
                            )
                        }
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Błąd podczas zapisywania: ${e.message}"
                    )
                }
            }
        }
    }

    // Stara funkcja - zastąpiona przez nową logikę formatowania w RichTextEditor
    @Deprecated("Zastąpiona przez nową logikę formatowania")
    private fun applyFormatting(formatType: String) {
        // Funkcja została zastąpiona przez nową implementację w RichTextEditor
        // i odpowiednie funkcje toggle* w tym ViewModelu
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        // Zapisz zmiany przed zniszczeniem ViewModelu
        if (_uiState.value.hasUnsavedChanges) {
            saveImmediately()
        }
    }
}

class NoteDetailsViewModelFactory(
    private val noteId: String,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteDetailsViewModel::class.java)) {
            return NoteDetailsViewModel(noteId, NotesRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
