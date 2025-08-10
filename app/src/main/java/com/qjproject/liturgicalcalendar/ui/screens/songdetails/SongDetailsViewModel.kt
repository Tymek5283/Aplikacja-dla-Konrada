package com.qjproject.liturgicalcalendar.ui.screens.songdetails

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qjproject.liturgicalcalendar.data.FileSystemRepository
import com.qjproject.liturgicalcalendar.data.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SongDetailsUiState(
    val isLoading: Boolean = true,
    val song: Song? = null,
    val error: String? = null
)

class SongDetailsViewModel(
    private val songNumber: String?,
    private val repository: FileSystemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SongDetailsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadSongDetails()
    }

    private fun loadSongDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            if (songNumber.isNullOrBlank()) {
                _uiState.update { it.copy(isLoading = false, error = "Nieprawidłowy numer pieśni.") }
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

    fun getSongTextPreview(text: String?): String {
        if (text.isNullOrBlank()) return "Brak tekstu dla tej pieśni."
        return text.lines().take(6).joinToString(separator = "\n")
    }
}

class SongDetailsViewModelFactory(
    private val context: Context,
    private val songNumber: String?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SongDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SongDetailsViewModel(songNumber, FileSystemRepository(context.applicationContext)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}