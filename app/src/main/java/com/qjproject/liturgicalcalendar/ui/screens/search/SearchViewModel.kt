package com.qjproject.liturgicalcalendar.ui.screens.search

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qjproject.liturgicalcalendar.data.FileSystemRepository
import com.qjproject.liturgicalcalendar.data.Song
import com.qjproject.liturgicalcalendar.data.models.SearchResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

enum class SearchMode { Dni, Pieśni }
enum class SongSortMode { Alfabetycznie, Numerycznie }

data class SearchUiState(
    val query: String = "",
    val searchMode: SearchMode = SearchMode.Dni,
    val results: List<SearchResult> = emptyList(),
    val searchPerformed: Boolean = false,
    val isLoading: Boolean = false,
    // Song-specific options
    val searchInTitle: Boolean = true,
    val searchInContent: Boolean = false,
    val sortMode: SongSortMode = SongSortMode.Alfabetycznie,
    val showAddSongDialog: Boolean = false,
    val addSongError: String? = null
)

class SearchViewModel(private val repository: FileSystemRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    private val _queryFlow = MutableStateFlow("")
    private var searchJob: Job? = null

    private val json = Json { ignoreUnknownKeys = true }

    // Cache for song list to avoid reading from disk on every validation
    private var allSongsCache: List<Song>? = null

    init {
        _queryFlow
            .debounce(500)
            .distinctUntilChanged()
            .onEach { triggerSearch() }
            .launchIn(viewModelScope)
    }

    private fun triggerSearch() {
        val query = _uiState.value.query
        val mode = _uiState.value.searchMode

        searchJob?.cancel()
        if (query.isNotBlank()) {
            performSearch(query)
        } else {
            if (mode == SearchMode.Pieśni) {
                loadAllSongs()
            } else {
                _uiState.update { it.copy(results = emptyList(), searchPerformed = false, isLoading = false) }
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        _queryFlow.value = newQuery
    }

    fun onSearchModeChange(newMode: SearchMode) {
        _uiState.update { it.copy(searchMode = newMode, results = emptyList(), searchPerformed = false) }
        triggerSearch()
    }

    fun onSearchInTitleChange(isChecked: Boolean) {
        if (_uiState.value.searchInTitle == isChecked) return
        _uiState.update { it.copy(searchInTitle = isChecked) }
        triggerSearch()
    }

    fun onSearchInContentChange(isChecked: Boolean) {
        if (_uiState.value.searchInContent == isChecked) return
        _uiState.update { it.copy(searchInContent = isChecked) }
        triggerSearch()
    }

    fun onSortModeChange(newSortMode: SongSortMode) {
        if (_uiState.value.sortMode == newSortMode) return
        _uiState.update { it.copy(sortMode = newSortMode) }
        val sortedResults = sortSongs(_uiState.value.results.filterIsInstance<SearchResult.SongResult>())
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
            val results = when (_uiState.value.searchMode) {
                SearchMode.Dni -> searchDays(query)
                SearchMode.Pieśni -> searchSongs(query)
            }
            _uiState.update { it.copy(results = results, searchPerformed = true, isLoading = false) }
        }
    }

    private suspend fun searchDays(query: String): List<SearchResult> {
        val normalizedQuery = normalize(query)
        val dayPaths = repository.getAllDayFilePaths()
        val results = mutableListOf<SearchResult.DayResult>()

        dayPaths.forEach { path ->
            try {
                val dayData = repository.getDayData(path)
                dayData?.let {
                    if (normalize(it.tytulDnia).contains(normalizedQuery)) {
                        results.add(SearchResult.DayResult(it.tytulDnia, path))
                    }
                }
            } catch (e: Exception) {
                // Log error or ignore corrupted file
            }
        }
        return results
    }

    private fun searchSongs(query: String): List<SearchResult> {
        val allSongs = repository.getSongList()
        val normalizedQuery = normalize(query)
        val state = _uiState.value

        // Exact number match has priority
        val byNumber = allSongs.filter { it.numer.equals(query.trim(), ignoreCase = true) }
        if (byNumber.isNotEmpty()) {
            return sortSongs(byNumber.map { SearchResult.SongResult(it) })
        }

        // Filter by other criteria
        val filteredSongs = allSongs.filter { song ->
            val matchesTitle = state.searchInTitle && normalize(song.tytul).contains(normalizedQuery)
            val matchesContent = state.searchInContent && normalize(song.tekst).contains(normalizedQuery)
            matchesTitle || matchesContent
        }.map { SearchResult.SongResult(it) }

        return sortSongs(filteredSongs)
    }

    private fun loadAllSongs() {
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val allSongs = repository.getSongList()
            val results = allSongs.map { SearchResult.SongResult(it) }
            val sortedResults = sortSongs(results)
            _uiState.update { it.copy(results = sortedResults, searchPerformed = true, isLoading = false) }
        }
    }

    private fun sortSongs(songs: List<SearchResult.SongResult>): List<SearchResult> {
        return when (_uiState.value.sortMode) {
            SongSortMode.Alfabetycznie -> songs.sortedBy { it.song.tytul }
            SongSortMode.Numerycznie -> songs.sortedWith(songNumberComparator)
        }
    }

    private val songNumberComparator = Comparator<SearchResult.SongResult> { a, b ->
        val numA = a.song.numer
        val numB = b.song.numer

        val isNumAInt = numA.toIntOrNull() != null
        val isNumBInt = numB.toIntOrNull() != null

        when {
            isNumAInt && isNumBInt -> numA.toInt().compareTo(numB.toInt())
            isNumAInt -> -1 // Numbers first
            isNumBInt -> 1  // Then strings
            else -> numA.compareTo(numB) // Both are strings
        }
    }

    fun onAddSongClicked() {
        viewModelScope.launch {
            allSongsCache = repository.getSongList() // Load song list when dialog is about to be shown
            _uiState.update { it.copy(showAddSongDialog = true, addSongError = null) }
        }
    }

    fun onDismissAddSongDialog() {
        _uiState.update { it.copy(showAddSongDialog = false, addSongError = null) }
        allSongsCache = null // Clear cache
    }

    fun validateSongInput(title: String, number: String) {
        val songs = allSongsCache ?: return

        if (title.isNotBlank() && songs.any { it.tytul.equals(title.trim(), ignoreCase = true) }) {
            _uiState.update { it.copy(addSongError = "Pieśń o tym tytule już istnieje.") }
            return
        }

        if (number.isNotBlank() && songs.any { it.numer.equals(number.trim(), ignoreCase = true) }) {
            _uiState.update { it.copy(addSongError = "Pieśń o tym numerze już istnieje.") }
            return
        }

        _uiState.update { it.copy(addSongError = null) }
    }

    fun saveNewSong(title: String, number: String, text: String) {
        val trimmedTitle = title.trim()
        val trimmedNumber = number.trim()
        val trimmedText = text.trim()

        if (trimmedTitle.isBlank()) {
            _uiState.update { it.copy(addSongError = "Tytuł jest wymagany.") }
            return
        }

        // Final validation before save
        validateSongInput(trimmedTitle, trimmedNumber)
        if (_uiState.value.addSongError != null) {
            return
        }

        viewModelScope.launch {
            val songs = (allSongsCache ?: repository.getSongList()).toMutableList()
            val newSong = Song(
                tytul = trimmedTitle,
                numer = trimmedNumber,
                tekst = trimmedText.ifBlank { null },
                opis = null // New songs don't have descriptions from liturgical days
            )
            songs.add(newSong)

            repository.saveSongList(songs).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(showAddSongDialog = false)
                    }
                    allSongsCache = null // Invalidate cache
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