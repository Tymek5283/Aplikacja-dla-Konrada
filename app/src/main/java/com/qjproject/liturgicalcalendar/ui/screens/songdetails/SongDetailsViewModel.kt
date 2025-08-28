package com.qjproject.liturgicalcalendar.ui.screens.songdetails

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qjproject.liturgicalcalendar.data.repository.FileSystemRepository.FileSystemRepository
import com.qjproject.liturgicalcalendar.data.Song
import com.qjproject.liturgicalcalendar.data.NeumyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder

data class SongDetailsUiState(
    val isLoading: Boolean = true,
    val song: Song? = null,
    val error: String? = null,
    val hasPdf: Boolean = false,
    val pdfPath: String? = null,
    val showPdfViewer: Boolean = false,
    val pdfOperationInProgress: Boolean = false,
    val pdfOperationMessage: String? = null
)

class SongDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: FileSystemRepository,
    private val context: Context
) : ViewModel() {

    private val songTitle: String? = savedStateHandle.get<String>("songTitle")?.let { URLDecoder.decode(it, "UTF-8") }
    private val siedlNum: String? = savedStateHandle.get<String>("siedlNum")?.let { URLDecoder.decode(it, "UTF-8") }
    private val sakNum: String? = savedStateHandle.get<String>("sakNum")?.let { URLDecoder.decode(it, "UTF-8") }
    private val dnNum: String? = savedStateHandle.get<String>("dnNum")?.let { URLDecoder.decode(it, "UTF-8") }

    private val _uiState = MutableStateFlow(SongDetailsUiState())
    val uiState = _uiState.asStateFlow()
    
    private val neumyManager = NeumyManager(context)

    init {
        // Wstępne ładowanie jest teraz obsługiwane przez obserwatora cyklu życia w ekranie,
        // ale zostawiamy je tutaj dla pierwszej kompozycji.
        if (_uiState.value.song == null) {
            reloadData()
        }
    }

    fun reloadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Unieważnij pamięć podręczną przed pobraniem danych, aby mieć pewność, że są świeże
            repository.invalidateSongCache()

            if (songTitle.isNullOrBlank()) {
                _uiState.update { it.copy(isLoading = false, error = "Nieprawidłowy tytuł pieśni.") }
                return@launch
            }
            val foundSong = repository.getSong(songTitle, siedlNum, sakNum, dnNum)
            if (foundSong != null) {
                // Sprawdź czy istnieje PDF dla tej pieśni
                val hasPdf = neumyManager.hasPdfForSong(foundSong.tytul)
                val pdfPath = if (hasPdf) neumyManager.getPdfPathForSong(foundSong.tytul) else null
                
                _uiState.update { 
                    it.copy(
                        isLoading = false, 
                        song = foundSong,
                        hasPdf = hasPdf,
                        pdfPath = pdfPath
                    ) 
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Nie znaleziono pieśni o tytule: $songTitle") }
            }
        }
    }

    fun getSongTextPreview(text: String?): String {
        if (text.isNullOrBlank()) return "Brak tekstu"
        return text.lines().take(6).joinToString(separator = "\n")
    }
    
    fun showPdfViewer() {
        _uiState.update { it.copy(showPdfViewer = true) }
    }
    
    fun hidePdfViewer() {
        _uiState.update { it.copy(showPdfViewer = false) }
    }
    
    fun addPdfForSong(uri: Uri) {
        val currentSong = _uiState.value.song ?: return
        
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    pdfOperationInProgress = true,
                    pdfOperationMessage = "Zapisywanie pliku PDF..."
                )
            }
            
            try {
                val result = neumyManager.savePdfForSong(currentSong.tytul, uri)
                
                if (result.isSuccess) {
                    val pdfPath = result.getOrNull()
                    _uiState.update { 
                        it.copy(
                            hasPdf = true,
                            pdfPath = pdfPath,
                            pdfOperationInProgress = false,
                            pdfOperationMessage = "PDF został pomyślnie dodany!"
                        )
                    }
                    
                    // Wyczyść komunikat po 3 sekundach
                    kotlinx.coroutines.delay(3000)
                    _uiState.update { it.copy(pdfOperationMessage = null) }
                } else {
                    _uiState.update { 
                        it.copy(
                            pdfOperationInProgress = false,
                            pdfOperationMessage = "Błąd podczas zapisywania PDF: ${result.exceptionOrNull()?.message}"
                        )
                    }
                    
                    // Wyczyść komunikat po 5 sekundach
                    kotlinx.coroutines.delay(5000)
                    _uiState.update { it.copy(pdfOperationMessage = null) }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        pdfOperationInProgress = false,
                        pdfOperationMessage = "Nieoczekiwany błąd: ${e.message}"
                    )
                }
                
                // Wyczyść komunikat po 5 sekundach
                kotlinx.coroutines.delay(5000)
                _uiState.update { it.copy(pdfOperationMessage = null) }
            }
        }
    }
    
    fun deletePdfForSong() {
        val currentSong = _uiState.value.song ?: return
        
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    pdfOperationInProgress = true,
                    pdfOperationMessage = "Usuwanie pliku PDF..."
                )
            }
            
            try {
                val success = neumyManager.deletePdfForSong(currentSong.tytul)
                
                if (success) {
                    _uiState.update { 
                        it.copy(
                            hasPdf = false,
                            pdfPath = null,
                            pdfOperationInProgress = false,
                            pdfOperationMessage = "PDF został usunięty!"
                        )
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            pdfOperationInProgress = false,
                            pdfOperationMessage = "Nie udało się usunąć pliku PDF"
                        )
                    }
                }
                
                // Wyczyść komunikat po 3 sekundach
                kotlinx.coroutines.delay(3000)
                _uiState.update { it.copy(pdfOperationMessage = null) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        pdfOperationInProgress = false,
                        pdfOperationMessage = "Błąd podczas usuwania: ${e.message}"
                    )
                }
                
                // Wyczyść komunikat po 5 sekundach
                kotlinx.coroutines.delay(5000)
                _uiState.update { it.copy(pdfOperationMessage = null) }
            }
        }
    }
}

class SongDetailsViewModelFactory(
    private val context: Context,
    private val savedStateHandle: SavedStateHandle
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SongDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SongDetailsViewModel(savedStateHandle, FileSystemRepository(context.applicationContext), context.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}