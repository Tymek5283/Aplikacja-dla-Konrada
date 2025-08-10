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
    val sortMode: SongSortMode = SongSortMode.Alfabetycznie
)

class SearchViewModel(private val repository: FileSystemRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    private val _queryFlow = MutableStateFlow("")
    private var searchJob: Job? = null

    private val json = Json { ignoreUnknownKeys = true }

    init {
        _queryFlow
            .debounce(500)
            .distinctUntilChanged()
            .onEach { query ->
                if (query.isNotBlank()) {
                    performSearch(query)
                } else {
                    _uiState.update { it.copy(results = emptyList(), searchPerformed = false, isLoading = false) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(newQuery: String) {
        _queryFlow.value = newQuery
        _uiState.update { it.copy(query = newQuery) }
        if (newQuery.isBlank()) {
            searchJob?.cancel()
            _uiState.update { it.copy(results = emptyList(), searchPerformed = false, isLoading = false) }
        }
    }

    fun onSearchModeChange(newMode: SearchMode) {
        _uiState.update { it.copy(searchMode = newMode, results = emptyList(), searchPerformed = false) }
        triggerSearch()
    }

    fun onSearchInTitleChange(isChecked: Boolean) {
        _uiState.update { it.copy(searchInTitle = isChecked) }
        triggerSearch()
    }

    fun onSearchInContentChange(isChecked: Boolean) {
        _uiState.update { it.copy(searchInContent = isChecked) }
        triggerSearch()
    }

    fun onSortModeChange(newSortMode: SongSortMode) {
        _uiState.update { it.copy(sortMode = newSortMode) }
        // Re-sort existing results without a new search
        val sortedResults = sortSongs(_uiState.value.results.filterIsInstance<SearchResult.SongResult>())
        _uiState.update { it.copy(results = sortedResults) }
    }

    private fun triggerSearch() {
        if (_uiState.value.query.isNotBlank()) {
            performSearch(_uiState.value.query)
        }
    }

    private fun normalize(text: String?): String {
        if (text == null) return ""
        val withoutSpecialChars = text.replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
        return withoutSpecialChars.lowercase(Locale.getDefault())
    }

    private fun performSearch(query: String) {
        searchJob?.cancel()
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