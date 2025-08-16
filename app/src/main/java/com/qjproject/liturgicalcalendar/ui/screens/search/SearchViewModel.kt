package com.qjproject.liturgicalcalendar.ui.screens.search

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qjproject.liturgicalcalendar.data.Category
import com.qjproject.liturgicalcalendar.data.FileSystemRepository
import com.qjproject.liturgicalcalendar.data.Song
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

enum class SongSortMode { Alfabetycznie, Kategoria }

sealed class DeleteDialogState {
    object None : DeleteDialogState()
    data class ConfirmInitial(val song: Song) : DeleteDialogState()
    data class ConfirmOccurrences(val song: Song) : DeleteDialogState()
}

data class SearchUiState(
    val query: String = "",
    val results: List<Song> = emptyList(),
    val searchPerformed: Boolean = false,
    val isLoading: Boolean = false,
    // Song-specific options
    val searchInTitle: Boolean = true,
    val searchInContent: Boolean = false,
    val sortMode: SongSortMode = SongSortMode.Alfabetycznie,
    val showAddSongDialog: Boolean = false,
    val addSongError: String? = null,
    val deleteDialogState: DeleteDialogState = DeleteDialogState.None,
    val allCategories: List<Category> = emptyList()
)

class SearchViewModel(private val repository: FileSystemRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    private val _queryFlow = MutableStateFlow("")
    private var searchJob: Job? = null

    // Cache for song list to avoid reading from disk on every validation
    private var allSongsCache: List<Song>? = null

    init {
        _queryFlow
            .debounce(500)
            .distinctUntilChanged()
            .onEach { triggerSearch() }
            .launchIn(viewModelScope)
    }

    // --- POCZĄTEK ZMIANY ---
    fun triggerSearch() {
        searchJob?.cancel()
        repository.invalidateSongCache() // Unieważnij pamięć podręczną, aby pobrać świeże dane
        performSearch(_uiState.value.query)
    }
    // --- KONIEC ZMIANY ---

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        _queryFlow.value = newQuery
    }

    fun onSearchInTitleChange(isChecked: Boolean) {
        if (_uiState.value.searchInTitle == isChecked || (!isChecked && !_uiState.value.searchInContent)) return
        _uiState.update { it.copy(searchInTitle = isChecked) }
        triggerSearch()
    }

    fun onSearchInContentChange(isChecked: Boolean) {
        if (_uiState.value.searchInContent == isChecked || (!isChecked && !_uiState.value.searchInTitle)) return
        _uiState.update { it.copy(searchInContent = isChecked) }
        triggerSearch()
    }

    fun onSortModeChange(newSortMode: SongSortMode) {
        if (_uiState.value.sortMode == newSortMode) return
        _uiState.update { it.copy(sortMode = newSortMode) }
        val sortedResults = sortSongs(_uiState.value.results)
        _uiState.update { it.copy(results = sortedResults) }
    }


    private fun normalize(text: String?): String {
        if (text == null) return ""
        val withoutSpecialChars = text.replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
        return withoutSpecialChars.lowercase(Locale.getDefault())
    }

    private fun performSearch(query: String) {
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val results = searchSongs(query)
            _uiState.update { it.copy(results = results, searchPerformed = true, isLoading = false) }
        }
    }

    private fun searchSongs(query: String): List<Song> {
        val allSongs = repository.getSongList()
        val normalizedQuery = normalize(query)
        val trimmedQuery = query.trim()
        val state = _uiState.value

        if (trimmedQuery.isBlank()) {
            return sortSongs(allSongs)
        }

        val bySiedlecki = allSongs.filter { it.numerSiedl.equals(trimmedQuery, ignoreCase = true) }
        if (bySiedlecki.isNotEmpty()) return sortSongs(bySiedlecki)

        val bySak = allSongs.filter { it.numerSAK.equals(trimmedQuery, ignoreCase = true) }
        if (bySak.isNotEmpty()) return sortSongs(bySak)

        val byDn = allSongs.filter { it.numerDN.equals(trimmedQuery, ignoreCase = true) }
        if (byDn.isNotEmpty()) return sortSongs(byDn)

        // Filter by other criteria
        val filteredSongs = allSongs.filter { song ->
            val matchesTitle = state.searchInTitle && normalize(song.tytul).contains(normalizedQuery)
            val matchesContent = state.searchInContent && normalize(song.tekst ?: "").contains(normalizedQuery)
            matchesTitle || matchesContent
        }

        return sortSongs(filteredSongs)
    }

    private fun sortSongs(songs: List<Song>): List<Song> {
        return when (_uiState.value.sortMode) {
            SongSortMode.Alfabetycznie -> songs.sortedBy { it.tytul }
            SongSortMode.Kategoria -> songs.sortedWith(
                compareBy<Song> { it.kategoria }.thenBy { it.tytul }
            )
        }
    }

    fun onAddSongClicked() {
        viewModelScope.launch {
            allSongsCache = repository.getSongList() // Load song list when dialog is about to be shown
            val allCategories = repository.getCategoryList()
            _uiState.update { it.copy(showAddSongDialog = true, addSongError = null, allCategories = allCategories) }
        }
    }

    fun onDismissAddSongDialog() {
        _uiState.update { it.copy(showAddSongDialog = false, addSongError = null) }
        allSongsCache = null // Clear cache
    }

    fun validateSongInput(title: String, siedl: String, sak: String, dn: String) {
        val songs = allSongsCache ?: return
        val trimmedTitle = title.trim()
        val trimmedSiedl = siedl.trim()
        val trimmedSak = sak.trim()
        val trimmedDn = dn.trim()

        if (trimmedTitle.isNotBlank() && songs.any { it.tytul.equals(trimmedTitle, ignoreCase = true) }) {
            _uiState.update { it.copy(addSongError = "Pieśń o tym tytule już istnieje.") }
            return
        }

        if (trimmedSiedl.isNotBlank() && songs.any { it.numerSiedl.equals(trimmedSiedl, ignoreCase = true) }) {
            _uiState.update { it.copy(addSongError = "Pieśń o tym numerze (Siedlecki) już istnieje.") }
            return
        }

        if (trimmedSak.isNotBlank() && songs.any { it.numerSAK.equals(trimmedSak, ignoreCase = true) }) {
            _uiState.update { it.copy(addSongError = "Pieśń o tym numerze (ŚAK) już istnieje.") }
            return
        }

        if (trimmedDn.isNotBlank() && songs.any { it.numerDN.equals(trimmedDn, ignoreCase = true) }) {
            _uiState.update { it.copy(addSongError = "Pieśń o tym numerze (DN) już istnieje.") }
            return
        }

        _uiState.update { it.copy(addSongError = null) }
    }

    fun saveNewSong(title: String, siedl: String, sak: String, dn: String, text: String, categoryName: String) {
        val trimmedTitle = title.trim()
        val trimmedSiedl = siedl.trim()
        val trimmedSak = sak.trim()
        val trimmedDn = dn.trim()
        val trimmedText = text.trim()

        if (trimmedTitle.isBlank()) {
            _uiState.update { it.copy(addSongError = "Tytuł jest wymagany.") }
            return
        }

        if (trimmedSiedl.isBlank() && trimmedSak.isBlank() && trimmedDn.isBlank()) {
            _uiState.update { it.copy(addSongError = "Przynajmniej jeden numer jest wymagany.") }
            return
        }

        validateSongInput(trimmedTitle, trimmedSiedl, trimmedSak, trimmedDn)
        if (_uiState.value.addSongError != null) {
            return
        }

        viewModelScope.launch {
            val songs = (allSongsCache ?: repository.getSongList()).toMutableList()
            val selectedCategory = _uiState.value.allCategories.find { it.nazwa == categoryName }

            val newSong = Song(
                tytul = trimmedTitle,
                tekst = trimmedText.ifBlank { null },
                numerSiedl = trimmedSiedl,
                numerSAK = trimmedSak,
                numerDN = trimmedDn,
                kategoria = selectedCategory?.nazwa ?: "",
                kategoriaSkr = selectedCategory?.skrot ?: ""
            )
            songs.add(newSong)

            repository.saveSongList(songs).fold(
                onSuccess = {
                    onDismissAddSongDialog()
                    triggerSearch() // Refresh the list
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            addSongError = "Błąd zapisu: ${error.localizedMessage}"
                        )
                    }
                }
            )
        }
    }

    fun onSongLongPress(song: Song) {
        _uiState.update { it.copy(deleteDialogState = DeleteDialogState.ConfirmInitial(song)) }
    }

    fun onDismissDeleteDialog() {
        _uiState.update { it.copy(deleteDialogState = DeleteDialogState.None) }
    }

    fun onConfirmInitialDelete() {
        val currentState = _uiState.value.deleteDialogState
        if (currentState is DeleteDialogState.ConfirmInitial) {
            _uiState.update { it.copy(deleteDialogState = DeleteDialogState.ConfirmOccurrences(currentState.song)) }
        }
    }

    fun onFinalDelete(deleteOccurrences: Boolean) {
        val currentState = _uiState.value.deleteDialogState
        if (currentState is DeleteDialogState.ConfirmOccurrences) {
            viewModelScope.launch {
                repository.deleteSong(currentState.song, deleteOccurrences).onSuccess {
                    allSongsCache = null // Invalidate cache after deletion
                    triggerSearch() // Refresh list on success
                }
                // Dismiss dialog regardless of success or failure
                onDismissDeleteDialog()
            }
        }
    }
}

class SearchViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(FileSystemRepository(context.applicationContext)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}