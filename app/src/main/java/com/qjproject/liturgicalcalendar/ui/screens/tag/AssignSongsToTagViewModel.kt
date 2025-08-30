package com.qjproject.liturgicalcalendar.ui.screens.tag

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

data class AssignSongsToTagUiState(
    val tag: String = "",
    val allSongs: List<Song> = emptyList(),
    val filteredSongs: List<Song> = emptyList(),
    val selectedSongs: Set<String> = emptySet(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null
)

class AssignSongsToTagViewModel(
    private val repository: FileSystemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssignSongsToTagUiState())
    val uiState = _uiState.asStateFlow()

    fun initializeForTag(tag: String) {
        _uiState.update { 
            it.copy(
                tag = tag, 
                isLoading = true,
                searchQuery = "", // Wyczyść pole wyszukiwarki przy otwarciu
                error = null
            ) 
        }
        loadSongs()
    }

    private fun loadSongs() {
        viewModelScope.launch {
            try {
                val songs = repository.getSongList()
                val currentTag = _uiState.value.tag
                
                // Znajdź pieśni, które już mają ten tag
                val songsWithTag = songs.filter { song ->
                    song.tagi.any { it.equals(currentTag, ignoreCase = true) }
                }.map { it.tytul }.toSet()
                
                val sortedSongs = songs.sortedBy { it.tytul }
                
                _uiState.update { 
                    it.copy(
                        allSongs = sortedSongs,
                        filteredSongs = sortedSongs,
                        selectedSongs = songsWithTag,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Błąd podczas ładowania pieśni: ${e.message}"
                    )
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { currentState ->
            val filteredSongs = if (query.isBlank()) {
                currentState.allSongs
            } else {
                currentState.allSongs.filter { song ->
                    song.tytul.contains(query, ignoreCase = true)
                }.sortedBy { it.tytul }
            }
            
            currentState.copy(
                searchQuery = query,
                filteredSongs = filteredSongs
            )
        }
    }

    fun toggleSongSelection(songTitle: String) {
        _uiState.update { currentState ->
            val newSelectedSongs = if (currentState.selectedSongs.contains(songTitle)) {
                currentState.selectedSongs - songTitle
            } else {
                currentState.selectedSongs + songTitle
            }
            
            currentState.copy(selectedSongs = newSelectedSongs)
        }
    }

    fun saveChanges() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            
            try {
                val currentState = _uiState.value
                val currentTag = currentState.tag
                val selectedSongTitles = currentState.selectedSongs
                
                // Pobierz aktualną listę pieśni
                val allSongs = repository.getSongList()
                
                // Zaktualizuj pieśni - dodaj lub usuń tag
                val updatedSongs = allSongs.map { song ->
                    val shouldHaveTag = selectedSongTitles.contains(song.tytul)
                    val currentlyHasTag = song.tagi.any { it.equals(currentTag, ignoreCase = true) }
                    
                    when {
                        shouldHaveTag && !currentlyHasTag -> {
                            // Dodaj tag
                            song.copy(tagi = song.tagi + currentTag)
                        }
                        !shouldHaveTag && currentlyHasTag -> {
                            // Usuń tag
                            song.copy(tagi = song.tagi.filter { !it.equals(currentTag, ignoreCase = true) })
                        }
                        else -> song // Bez zmian
                    }
                }
                
                // Zapisz zaktualizowaną listę pieśni
                repository.saveSongList(updatedSongs).getOrThrow()
                
                _uiState.update { it.copy(isSaving = false) }
                
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

class AssignSongsToTagViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AssignSongsToTagViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AssignSongsToTagViewModel(
                FileSystemRepository(context.applicationContext)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
