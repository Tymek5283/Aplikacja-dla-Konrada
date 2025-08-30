package com.qjproject.liturgicalcalendar.ui.screens.search

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qjproject.liturgicalcalendar.data.Category
import com.qjproject.liturgicalcalendar.data.repository.FileSystemRepository.FileSystemRepository
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
    val songResults: List<Song> = emptyList(),
    val categoryResults: List<Category> = emptyList(),
    val tagResults: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val searchInTitle: Boolean = true,
    val searchInContent: Boolean = false,
    val sortMode: SongSortMode = SongSortMode.Alfabetycznie,
    val showAddSongDialog: Boolean = false,
    val addSongError: String? = null,
    val deleteDialogState: DeleteDialogState = DeleteDialogState.None,
    val allCategories: List<Category> = emptyList(),
    val allTags: List<String> = emptyList(),
    val selectedCategory: Category? = null,
    val selectedTag: String? = null
) {
    val isBackButtonVisible: Boolean get() = selectedCategory != null || selectedTag != null
}

class SearchViewModel(private val repository: FileSystemRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    private val _queryFlow = MutableStateFlow("")
    private var searchJob: Job? = null
    private var allSongsCache: List<Song>? = null

    private val noCategoryFilter = Category("Brak kategorii", "")

    init {
        loadInitialData()
        _queryFlow
            .debounce(300)
            .distinctUntilChanged()
            .onEach { performSearch() }
            .launchIn(viewModelScope)
    }

    fun reloadData() {
        allSongsCache = null
        repository.invalidateAllCaches()
        loadInitialData()
        performSearch()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val categories = repository.getCategoryList().sortedBy { it.nazwa }
            val tags = repository.getTagList().sorted()
            allSongsCache = repository.getSongList()
            _uiState.update { 
                it.copy(
                    allCategories = categories, 
                    allTags = tags,
                    isLoading = false, 
                    categoryResults = categories,
                    tagResults = tags
                ) 
            }
        }
    }

    private fun performSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val allSongs = allSongsCache ?: repository.getSongList().also { allSongsCache = it }
            val allCategories = _uiState.value.allCategories
            val allTags = _uiState.value.allTags
            val query = _uiState.value.query.trim()
            val selectedCategory = _uiState.value.selectedCategory
            val selectedTag = _uiState.value.selectedTag

            val newSongResults: List<Song>
            val newCategoryResults: List<Category>
            val newTagResults: List<String>

            when {
                selectedCategory != null -> {
                    // Search within a category or the "no category" filter
                    newCategoryResults = emptyList()
                    newTagResults = emptyList()
                    val songsToFilter = when (selectedCategory) {
                        noCategoryFilter -> allSongs.filter { it.kategoria.isBlank() }
                        else -> allSongs.filter { it.kategoria.equals(selectedCategory.nazwa, ignoreCase = true) }
                    }

                    newSongResults = if (query.isBlank()) {
                        songsToFilter
                    } else {
                        val normalizedQuery = normalize(query)
                        filterSongsWithNumberPriority(songsToFilter, query, normalizedQuery)
                    }
                }
                selectedTag != null -> {
                    // Search within a tag
                    newCategoryResults = emptyList()
                    newTagResults = emptyList()
                    val songsToFilter = allSongs.filter { song ->
                        song.tagi.any { it.equals(selectedTag, ignoreCase = true) }
                    }

                    newSongResults = if (query.isBlank()) {
                        songsToFilter
                    } else {
                        val normalizedQuery = normalize(query)
                        filterSongsWithNumberPriority(songsToFilter, query, normalizedQuery)
                    }
                }
                else -> {
                    // Global search
                    if (query.isBlank()) {
                        newCategoryResults = allCategories
                        newTagResults = allTags
                        newSongResults = emptyList()
                    } else {
                        val normalizedQuery = normalize(query)
                        
                        // Filtrowanie kategorii - wykluczamy "Brak kategorii" chyba że dokładnie pasuje
                        newCategoryResults = allCategories.filter { 
                            normalize(it.nazwa).contains(normalizedQuery)
                        }
                        
                        // Filtrowanie tagów
                        newTagResults = allTags.filter { normalize(it).contains(normalizedQuery) }
                        
                        // Filtrowanie pieśni z obsługą wyszukiwania numerycznego
                        newSongResults = filterSongsWithNumberPriority(allSongs, query, normalizedQuery)
                    }
                }
            }

            _uiState.update { it.copy(
                songResults = sortSongsWithNumericPriority(newSongResults, query),
                categoryResults = newCategoryResults.sortedBy { it.nazwa },
                tagResults = newTagResults.sorted(),
                isLoading = false
            )}
        }
    }


    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        _queryFlow.value = newQuery
    }

    fun onCategorySelected(category: Category) {
        _uiState.update { it.copy(selectedCategory = category, query = "") }
        performSearch()
    }

    fun onNoCategorySelected() {
        _uiState.update { it.copy(selectedCategory = noCategoryFilter, query = "") }
        performSearch()
    }

    fun onTagSelected(tag: String) {
        _uiState.update { it.copy(selectedTag = tag, query = "") }
        performSearch()
    }

    fun onNavigateBack() {
        _uiState.update { it.copy(selectedCategory = null, selectedTag = null, query = "") }
        performSearch()
    }

    fun onResetToRoot() {
        onNavigateBack()
    }

    private fun sortSongs(songs: List<Song>): List<Song> {
        return when (_uiState.value.sortMode) {
            SongSortMode.Alfabetycznie -> songs.sortedBy { it.tytul }
            SongSortMode.Kategoria -> songs.sortedWith(
                compareBy<Song> { it.kategoria }.thenBy { it.tytul }
            )
        }
    }

    /**
     * Sortuje pieśni z zachowaniem priorytetu numerycznego dla zapytań składających się z cyfr
     */
    private fun sortSongsWithNumericPriority(songs: List<Song>, query: String): List<Song> {
        val trimmedQuery = query.trim()
        val isNumericQuery = trimmedQuery.isNotEmpty() && trimmedQuery.all { it.isDigit() }
        
        return if (isNumericQuery) {
            // Dla zapytań numerycznych NIE stosujemy standardowego sortowania
            // Funkcja filterSongsWithNumberPriority już zwróciła wyniki w odpowiedniej kolejności:
            // 1. Dokładne dopasowania (posortowane alfabetycznie)
            // 2. Częściowe dopasowania (posortowane alfabetycznie)
            songs
        } else {
            // Dla zapytań nienumerycznych stosujemy standardowe sortowanie
            sortSongs(songs)
        }
    }

    private fun normalize(text: String?): String {
        if (text == null) return ""
        val withoutSpecialChars = text.replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
        return withoutSpecialChars.lowercase(Locale.getDefault())
    }

    fun onSearchInTitleChange(isChecked: Boolean) {
        if (_uiState.value.searchInTitle == isChecked || (!isChecked && !_uiState.value.searchInContent)) return
        _uiState.update { it.copy(searchInTitle = isChecked) }
        performSearch()
    }

    fun onSearchInContentChange(isChecked: Boolean) {
        if (_uiState.value.searchInContent == isChecked || (!isChecked && !_uiState.value.searchInTitle)) return
        _uiState.update { it.copy(searchInContent = isChecked) }
        performSearch()
    }

    fun onSortModeChange(newSortMode: SongSortMode) {
        if (_uiState.value.sortMode == newSortMode) return
        _uiState.update { it.copy(sortMode = newSortMode) }
        _uiState.update { it.copy(songResults = sortSongs(it.songResults)) }
    }

    fun onAddSongClicked() {
        viewModelScope.launch {
            repository.invalidateSongCache()
            allSongsCache = repository.getSongList()
            _uiState.update { it.copy(showAddSongDialog = true, addSongError = null) }
        }
    }

    fun onDismissAddSongDialog() {
        _uiState.update { it.copy(showAddSongDialog = false, addSongError = null) }
        allSongsCache = null
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

    fun saveNewSong(title: String, siedl: String, sak: String, dn: String, text: String, categoryName: String, preselectedTag: String? = null) {
        val trimmedTitle = title.trim()
        val trimmedSiedl = siedl.trim()
        val trimmedSak = sak.trim()
        val trimmedDn = dn.trim()
        val trimmedText = text.trim()

        if (trimmedTitle.isBlank()) {
            _uiState.update { it.copy(addSongError = "Tytuł jest wymagany.") }
            return
        }
        validateSongInput(trimmedTitle, trimmedSiedl, trimmedSak, trimmedDn)
        if (_uiState.value.addSongError != null) return

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
                kategoriaSkr = selectedCategory?.skrot ?: "",
                tagi = if (preselectedTag != null) listOf(preselectedTag) else emptyList()
            )
            songs.add(newSong)

            repository.saveSongList(songs).fold(
                onSuccess = {
                    onDismissAddSongDialog()
                    allSongsCache = null // Invalidate cache
                    performSearch()
                },
                onFailure = { error ->
                    _uiState.update { it.copy(addSongError = "Błąd zapisu: ${error.localizedMessage}") }
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
                    allSongsCache = null
                    performSearch()
                }
                onDismissDeleteDialog()
            }
        }
    }

    /**
     * Filtruje pieśni z priorytetem dla dokładnych dopasowań numerycznych
     * Implementuje specjalną logikę dla zapytań składających się wyłącznie z cyfr
     */
    private fun filterSongsWithNumberPriority(
        songs: List<Song>, 
        originalQuery: String, 
        normalizedQuery: String
    ): List<Song> {
        val trimmedQuery = originalQuery.trim()
        val isNumericQuery = trimmedQuery.isNotEmpty() && trimmedQuery.all { it.isDigit() }
        
        if (isNumericQuery) {
            // Tryb wyszukiwania numerycznego - przeszukujemy TYLKO pola z przedrostkiem "numer"
            // Ignorujemy tytuł i tekst pieśni
            
            // Grupa 1: Dokładne dopasowania numerów (najwyższy priorytet)
            val exactMatches = songs.filter { song ->
                song.numerSiedl == trimmedQuery || 
                song.numerSAK == trimmedQuery || 
                song.numerDN == trimmedQuery
            }.sortedBy { it.tytul } // Sortowanie alfabetyczne w grupie
            
            // Grupa 2: Częściowe dopasowania numerów (niższy priorytet)
            val partialMatches = songs.filter { song ->
                // Sprawdzamy czy liczba jest częścią składową, ale nie jest identyczna
                (song.numerSiedl.contains(trimmedQuery) && song.numerSiedl != trimmedQuery) ||
                (song.numerSAK.contains(trimmedQuery) && song.numerSAK != trimmedQuery) ||
                (song.numerDN.contains(trimmedQuery) && song.numerDN != trimmedQuery)
            }.sortedBy { it.tytul } // Sortowanie alfabetyczne w grupie
            
            // Zwracamy tylko wyniki numeryczne - NIE uwzględniamy tytułu ani tekstu
            return exactMatches + partialMatches
            
        } else {
            // Standardowe wyszukiwanie dla zapytań nienumerycznych
            return songs.filter { song ->
                val matchesTitle = _uiState.value.searchInTitle && normalize(song.tytul).contains(normalizedQuery)
                val matchesContent = _uiState.value.searchInContent && normalize(song.tekst ?: "").contains(normalizedQuery)
                val matchesTag = song.tagi.any { normalize(it).contains(normalizedQuery) }
                val matchesNumbers = song.numerSiedl.contains(originalQuery, ignoreCase = true) ||
                                  song.numerSAK.contains(originalQuery, ignoreCase = true) ||
                                  song.numerDN.contains(originalQuery, ignoreCase = true)
                matchesTitle || matchesContent || matchesTag || matchesNumbers
            }
        }
    }
}