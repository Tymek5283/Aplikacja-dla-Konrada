package com.qjproject.liturgicalcalendar.ui.screens.songdetails

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qjproject.liturgicalcalendar.data.FileSystemRepository
import com.qjproject.liturgicalcalendar.data.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder

data class SongDetailsUiState(
    val isLoading: Boolean = true,
    val song: Song? = null,
    val error: String? = null
)

class SongDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: FileSystemRepository
) : ViewModel() {

    private val songTitle: String? = savedStateHandle.get<String>("songTitle")?.let { URLDecoder.decode(it, "UTF-8") }
    private val siedlNum: String? = savedStateHandle.get<String>("siedlNum")?.let { URLDecoder.decode(it, "UTF-8") }
    private val sakNum: String? = savedStateHandle.get<String>("sakNum")?.let { URLDecoder.decode(it, "UTF-8") }
    private val dnNum: String? = savedStateHandle.get<String>("dnNum")?.let { URLDecoder.decode(it, "UTF-8") }

    private val _uiState = MutableStateFlow(SongDetailsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        reloadData()
    }

    fun reloadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            if (songTitle.isNullOrBlank()) {
                _uiState.update { it.copy(isLoading = false, error = "Nieprawidłowy tytuł pieśni.") }
                return@launch
            }
            // --- POCZĄTEK ZMIANY ---
            val foundSong = repository.getSong(songTitle, siedlNum, sakNum, dnNum)
            // --- KONIEC ZMIANY ---
            if (foundSong != null) {
                _uiState.update { it.copy(isLoading = false, song = foundSong) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Nie znaleziono pieśni o tytule: $songTitle") }
            }
        }
    }

    fun getSongTextPreview(text: String?): String {
        if (text.isNullOrBlank()) return "Brak tekstu"
        return text.lines().take(6).joinToString(separator = "\n")
    }
}

class SongDetailsViewModelFactory(
    private val context: Context,
    private val savedStateHandle: SavedStateHandle
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SongDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SongDetailsViewModel(savedStateHandle, FileSystemRepository(context.applicationContext)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}