package com.qjproject.liturgicalcalendar.ui.screens.songdetails

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qjproject.liturgicalcalendar.data.Song
import com.qjproject.liturgicalcalendar.data.repository.FileSystemRepository.FileSystemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AssignTagsToSongUiState(
    val song: Song? = null,
    val allTags: List<String> = emptyList(),
    val filteredTags: List<String> = emptyList(),
    val selectedTags: Set<String> = emptySet(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null
)

class AssignTagsToSongViewModel(
    private val repository: FileSystemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssignTagsToSongUiState())
    val uiState = _uiState.asStateFlow()

    fun initializeForSong(song: Song) {
        _uiState.update { 
            it.copy(
                song = song, 
                isLoading = true,
                searchQuery = "",
                error = null
            ) 
        }
        loadTags()
    }

    private fun loadTags() {
        viewModelScope.launch {
            try {
                val tags = repository.getTagList()
                val currentSong = _uiState.value.song
                
                // Znajdź tagi, które już są przypisane do tej pieśni
                val currentSongTags = currentSong?.tagi?.toSet() ?: emptySet()
                
                val sortedTags = tags.sorted()
                
                _uiState.update { 
                    it.copy(
                        allTags = sortedTags,
                        filteredTags = sortedTags,
                        selectedTags = currentSongTags,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Błąd podczas ładowania tagów: ${e.message}"
                    )
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { currentState ->
            val filteredTags = if (query.isBlank()) {
                currentState.allTags
            } else {
                currentState.allTags.filter { tag ->
                    tag.contains(query, ignoreCase = true)
                }
            }.sorted()
            
            currentState.copy(
                searchQuery = query,
                filteredTags = filteredTags
                // selectedTags pozostają niezmienione - to jest kluczowe dla zachowania zaznaczenia
            )
        }
    }

    fun toggleTagSelection(tag: String) {
        _uiState.update { currentState ->
            val newSelectedTags = if (currentState.selectedTags.contains(tag)) {
                currentState.selectedTags - tag
            } else {
                currentState.selectedTags + tag
            }
            
            currentState.copy(selectedTags = newSelectedTags)
        }
    }

    fun saveChanges(onSuccess: (Song) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            
            try {
                val currentState = _uiState.value
                val currentSong = currentState.song ?: return@launch
                val selectedTags = currentState.selectedTags.toList()
                
                // Stwórz zaktualizowaną pieśń z nowymi tagami
                val updatedSong = currentSong.copy(tagi = selectedTags)
                
                // Pobierz aktualną listę pieśni
                val allSongs = repository.getSongList()
                
                // Zaktualizuj pieśń w liście
                val updatedSongs = allSongs.map { song ->
                    if (song.tytul == currentSong.tytul && 
                        song.numerSiedl == currentSong.numerSiedl) {
                        updatedSong
                    } else {
                        song
                    }
                }
                
                // Zapisz zaktualizowaną listę pieśni
                repository.saveSongList(updatedSongs).getOrThrow()
                
                _uiState.update { it.copy(isSaving = false) }
                
                // Wywołaj callback z zaktualizowaną pieśnią
                onSuccess(updatedSong)
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isSaving = false,
                        error = "Błąd podczas zapisywania: ${e.message}"
                    )
                }
            }
        }
    }
}

class AssignTagsToSongViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AssignTagsToSongViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AssignTagsToSongViewModel(
                FileSystemRepository(context.applicationContext)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
