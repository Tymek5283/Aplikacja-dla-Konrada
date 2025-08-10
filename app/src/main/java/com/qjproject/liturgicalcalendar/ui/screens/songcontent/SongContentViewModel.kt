package com.qjproject.liturgicalcalendar.ui.screens.songcontent

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qjproject.liturgicalcalendar.data.FileSystemRepository
import com.qjproject.liturgicalcalendar.data.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SongContentUiState(
    val isLoading: Boolean = true,
    val song: Song? = null,
    val error: String? = null,
    val isEditMode: Boolean = false,
    val hasChanges: Boolean = false,
    val showConfirmExitDialog: Boolean = false
)

class SongContentViewModel(
    private val songNumber: String?,
    private val repository: FileSystemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SongContentUiState())
    val uiState = _uiState.asStateFlow()

    var editableText = mutableStateOf("")
        private set

    init {
        loadSong()
    }

    private fun loadSong() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            if (songNumber.isNullOrBlank()) {
                _uiState.update { it.copy(isLoading = false, error = "Brak numeru pieśni.") }
                return@launch
            }
            val foundSong = repository.getSongByNumber(songNumber)
            if (foundSong != null) {
                _uiState.update { it.copy(isLoading = false, song = foundSong) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Nie znaleziono pieśni o numerze: $songNumber") }
            }
        }
    }

    fun onEnterEditMode() {
        val currentText = _uiState.value.song?.tekst ?: ""
        editableText.value = currentText
        _uiState.update { it.copy(isEditMode = true, hasChanges = false) }
    }

    fun onTryExitEditMode() {
        if (_uiState.value.hasChanges) {
            _uiState.update { it.copy(showConfirmExitDialog = true) }
        } else {
            onDiscardChanges()
        }
    }

    fun onDiscardChanges() {
        _uiState.update { it.copy(isEditMode = false, hasChanges = false, showConfirmExitDialog = false) }
    }

    fun dismissConfirmExitDialog() {
        _uiState.update { it.copy(showConfirmExitDialog = false) }
    }

    fun onTextChange(newText: String) {
        editableText.value = newText
        _uiState.update { it.copy(hasChanges = newText != it.song?.tekst) }
    }

    fun onSaveChanges() {
        val songToUpdate = _uiState.value.song ?: return
        viewModelScope.launch {
            val allSongs = repository.getSongList().toMutableList()
            val songIndex = allSongs.indexOfFirst { it.numer == songToUpdate.numer }

            if (songIndex != -1) {
                allSongs[songIndex] = songToUpdate.copy(tekst = editableText.value)
                repository.saveSongList(allSongs).onSuccess {
                    _uiState.update { it.copy(isEditMode = false, hasChanges = false, song = allSongs[songIndex]) }
                }.onFailure { error ->
                    _uiState.update { it.copy(error = "Błąd zapisu: ${error.localizedMessage}") }
                }
            } else {
                _uiState.update { it.copy(error = "Nie można znaleźć pieśni do zaktualizowania.") }
            }
        }
    }
}

class SongContentViewModelFactory(
    private val context: Context,
    private val songNumber: String?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SongContentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SongContentViewModel(songNumber, FileSystemRepository(context.applicationContext)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}