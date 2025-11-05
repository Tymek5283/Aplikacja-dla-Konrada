package com.qjproject.liturgicalcalendar.ui.screens.search

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qjproject.liturgicalcalendar.data.Category
import com.qjproject.liturgicalcalendar.data.repository.FileSystemRepository.FileSystemRepository
import com.qjproject.liturgicalcalendar.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    val selectedTag: String? = null,
    val resetToTopEventId: Int = 0,
    val rootQueryBeforeEnter: String? = null,
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false
) {
    val isBackButtonVisible: Boolean get() = selectedCategory != null || selectedTag != null
}

class SearchViewModel(private val repository: FileSystemRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    private val _queryFlow = MutableStateFlow("")
    private var searchJob: Job? = null
    private var allSongsCache: List<Song>? = null
    private var lastResults: List<Song> = emptyList()
    private var visibleCount: Int = 0
    private val pageSize: Int = 25
    private val nonWordRegex = Regex("[^\\p{L}\\p{N}\\s]")
    private var lastSearchKey: String? = null

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
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val categories = withContext(Dispatchers.IO) { repository.getCategoryList().sortedBy { it.nazwa } }
            val tags = withContext(Dispatchers.IO) { repository.getTagList().sorted() }
            val songs = withContext(Dispatchers.IO) { repository.getSongList() }
            allSongsCache = songs
            _uiState.update {
                it.copy(
                    allCategories = categories,
                    allTags = tags
                )
            }
            performSearch()
        }
    }

    private fun performSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val hadCache = allSongsCache != null
            _uiState.update { it.copy(isLoading = if (!hadCache) true else it.isLoading, isLoadingMore = false) }
            val allSongs = allSongsCache ?: withContext(Dispatchers.IO) { repository.getSongList() }.also { allSongsCache = it }
            val allCategories = _uiState.value.allCategories
            val allTags = _uiState.value.allTags
            val query = _uiState.value.query.trim()
            val selectedCategory = _uiState.value.selectedCategory
            val selectedTag = _uiState.value.selectedTag
            val searchKey = buildString {
                append(selectedCategory?.nazwa ?: "")
                append('|')
                append(selectedTag ?: "")
                append('|')
                append(normalize(query))
                append('|')
                append(_uiState.value.searchInTitle)
                append('|')
                append(_uiState.value.searchInContent)
                append('|')
                append(_uiState.value.sortMode.name)
            }

            val computeResult = withContext(Dispatchers.Default) {
                val newCategoryResults: List<Category>
                val newTagResults: List<String>
                val songsToFilter: List<Song>

                when {
                    selectedCategory != null -> {
                        newCategoryResults = emptyList()
                        newTagResults = emptyList()
                        songsToFilter = when (selectedCategory) {
                            noCategoryFilter -> allSongs.filter { it.kategoria.isBlank() }
                            else -> allSongs.filter { it.kategoria.equals(selectedCategory.nazwa, ignoreCase = true) }
                        }
                    }
                    selectedTag != null -> {
                        newCategoryResults = emptyList()
                        newTagResults = emptyList()
                        songsToFilter = allSongs.filter { song ->
                            song.tagi.any { it.equals(selectedTag, ignoreCase = true) }
                        }
                    }
                    else -> {
                        if (query.isBlank()) {
                            return@withContext Triple(emptyList<Song>(), allCategories, allTags)
                        }
                        val normalizedQuery = normalize(query)
                        newCategoryResults = allCategories.filter { normalize(it.nazwa).contains(normalizedQuery) }
                        newTagResults = allTags.filter { normalize(it).contains(normalizedQuery) }
                        songsToFilter = allSongs
                    }
                }

                val normalizedQuery = normalize(query)
                val filtered = if (query.isBlank()) songsToFilter else filterSongsWithNumberPriority(songsToFilter, query, normalizedQuery)
                val sorted = sortSongsWithNumericPriority(filtered, query)
                Triple(sorted, newCategoryResults, newTagResults)
            }

            lastResults = computeResult.first
            val shouldKeepVisible = (lastSearchKey != null && lastSearchKey == searchKey)
            val newVisible = if (lastResults.isEmpty()) 0 else if (shouldKeepVisible) minOf(visibleCount, lastResults.size) else minOf(pageSize, lastResults.size)
            visibleCount = newVisible
            _uiState.update { it.copy(
                songResults = if (visibleCount == 0) emptyList() else lastResults.take(visibleCount),
                categoryResults = computeResult.second.sortedBy { it.nazwa },
                tagResults = computeResult.third.sorted(),
                hasMore = visibleCount < lastResults.size,
                isLoading = false,
                isLoadingMore = false
            ) }
            lastSearchKey = searchKey
        }
    }


    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        _queryFlow.value = newQuery
    }

    fun onCategorySelected(category: Category) {
        _uiState.update { current ->
            current.copy(
                rootQueryBeforeEnter = if (current.selectedCategory == null && current.selectedTag == null && current.rootQueryBeforeEnter == null) current.query else current.rootQueryBeforeEnter,
                selectedCategory = category,
                query = ""
            )
        }
        performSearch()
    }

    fun onNoCategorySelected() {
        _uiState.update { it.copy(selectedCategory = noCategoryFilter, query = "") }
        performSearch()
    }

    fun onTagSelected(tag: String) {
        _uiState.update { current ->
            current.copy(
                rootQueryBeforeEnter = if (current.selectedCategory == null && current.selectedTag == null && current.rootQueryBeforeEnter == null) current.query else current.rootQueryBeforeEnter,
                selectedTag = tag,
                query = ""
            )
        }
        performSearch()
    }

    fun onNavigateBack() {
        val rootQuery = _uiState.value.rootQueryBeforeEnter
        _uiState.update { it.copy(selectedCategory = null, selectedTag = null, query = rootQuery ?: it.query, rootQueryBeforeEnter = null) }
        performSearch()
    }

    fun onResetToRoot() {
        _uiState.update { it.copy(selectedCategory = null, selectedTag = null, query = "", resetToTopEventId = it.resetToTopEventId + 1) }
        performSearch()
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
     * lub priorytetyzacją według pozycji frazy dla zapytań tekstowych
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
        } else if (trimmedQuery.isNotEmpty()) {
            val normalizedQuery = normalize(trimmedQuery)

            songs
                .map { song ->
                    var minIndex = Int.MAX_VALUE
                    if (_uiState.value.searchInTitle) {
                        val normalizedTitle = normalize(song.tytul)
                        val titleIndex = normalizedTitle.indexOf(normalizedQuery)
                        if (titleIndex >= 0 && titleIndex < minIndex) minIndex = titleIndex
                    }
                    if (_uiState.value.searchInContent && song.tekst != null) {
                        val normalizedContent = normalize(song.tekst)
                        val contentIndex = normalizedContent.indexOf(normalizedQuery)
                        if (contentIndex >= 0 && contentIndex < minIndex) minIndex = contentIndex
                    }
                    Pair(song, minIndex)
                }
                .sortedWith(compareBy<Pair<Song, Int>> { it.second }.thenBy { it.first.tytul })
                .map { it.first }
        } else {
            // Dla pustego zapytania stosujemy standardowe sortowanie
            sortSongs(songs)
        }
    }

    private fun normalize(text: String?): String {
        if (text == null) return ""
        val withoutSpecialChars = text.replace(nonWordRegex, "")
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

    fun getAllNumberSuffixes(): List<String> {
        val core = listOf("Siedl", "SAK", "DN", "SAK2020")
        val songs = allSongsCache ?: repository.getSongList().also { allSongsCache = it }
        val extras = songs.flatMap { it.numery.keys }.toSet().minus(core.toSet()).toList().sorted()
        return core + extras
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

    fun validateSongInput(title: String, siedl: String, sak: String, dn: String, sak2020: String, extras: Map<String, String> = emptyMap()) {
        val songs = allSongsCache ?: return
        val trimmedTitle = title.trim()
        val trimmedSiedl = siedl.trim()
        val trimmedSak = sak.trim()
        val trimmedDn = dn.trim()
        val trimmedSak2020 = sak2020.trim()

        if (trimmedTitle.isNotBlank() && songs.any { it.tytul.equals(trimmedTitle, ignoreCase = true) }) {
            _uiState.update { it.copy(addSongError = "Pieśń o tym tytule już istnieje.") }
            return
        }
        if (trimmedSiedl.isNotBlank() && songs.any { it.numerSiedl.equals(trimmedSiedl, ignoreCase = true) }) {
            _uiState.update { it.copy(addSongError = "Pieśń o tym numerze (Siedl) już istnieje.") }
            return
        }
        if (trimmedSak.isNotBlank() && songs.any { it.numerSAK.equals(trimmedSak, ignoreCase = true) }) {
            _uiState.update { it.copy(addSongError = "Pieśń o tym numerze (SAK) już istnieje.") }
            return
        }
        if (trimmedDn.isNotBlank() && songs.any { it.numerDN.equals(trimmedDn, ignoreCase = true) }) {
            _uiState.update { it.copy(addSongError = "Pieśń o tym numerze (DN) już istnieje.") }
            return
        }
        if (trimmedSak2020.isNotBlank() && songs.any { it.numerSAK2020.equals(trimmedSak2020, ignoreCase = true) }) {
            _uiState.update { it.copy(addSongError = "Pieśń o tym numerze (SAK2020) już istnieje.") }
            return
        }
        
        // Walidacja wszystkich dodatkowych numerów
        extras.forEach { (suffix, value) ->
            val trimmedValue = value.trim()
            if (trimmedValue.isNotBlank()) {
                val existingSong = songs.find { song ->
                    val songNumber = song.numery[suffix] ?: ""
                    songNumber.equals(trimmedValue, ignoreCase = true)
                }
                if (existingSong != null) {
                    _uiState.update { it.copy(addSongError = "Pieśń o tym numerze ($suffix) już istnieje.") }
                    return
                }
            }
        }
        
        _uiState.update { it.copy(addSongError = null) }
    }

    fun saveNewSong(title: String, siedl: String, sak: String, dn: String, sak2020: String, extras: Map<String, String>, text: String, categoryName: String, preselectedTag: String? = null) {
        val trimmedTitle = title.trim()
        val trimmedSiedl = siedl.trim()
        val trimmedSak = sak.trim()
        val trimmedDn = dn.trim()
        val trimmedSak2020 = sak2020.trim()
        val trimmedText = text.trim()

        if (trimmedTitle.isBlank()) {
            _uiState.update { it.copy(addSongError = "Tytuł jest wymagany.") }
            return
        }
        validateSongInput(trimmedTitle, trimmedSiedl, trimmedSak, trimmedDn, trimmedSak2020, extras)
        if (_uiState.value.addSongError != null) return

        viewModelScope.launch {
            val songs = (allSongsCache ?: repository.getSongList()).toMutableList()
            val selectedCategory = _uiState.value.allCategories.find { it.nazwa == categoryName }

            val coreSuffixes = setOf("Siedl", "SAK", "DN", "SAK2020")
            val extraMap = extras
                .filterKeys { it !in coreSuffixes }
                .mapValues { it.value.trim() }
                .filterValues { true }

            val newSong = Song(
                tytul = trimmedTitle,
                tekst = trimmedText.ifBlank { null },
                numerSiedl = trimmedSiedl,
                numerSAK = trimmedSak,
                numerDN = trimmedDn,
                numerSAK2020 = trimmedSak2020,
                kategoria = selectedCategory?.nazwa ?: "",
                kategoriaSkr = selectedCategory?.skrot ?: "",
                tagi = if (preselectedTag != null) listOf(preselectedTag) else emptyList(),
                numery = extraMap
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

    fun loadMoreResults() {
        val current = _uiState.value
        if (!current.hasMore || current.isLoading || current.isLoadingMore) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val newVisible = minOf(visibleCount + pageSize, lastResults.size)
            if (newVisible != visibleCount) {
                visibleCount = newVisible
                _uiState.update {
                    it.copy(
                        songResults = lastResults.take(visibleCount),
                        hasMore = visibleCount < lastResults.size,
                        isLoadingMore = false
                    )
                }
            } else {
                _uiState.update { it.copy(hasMore = false, isLoadingMore = false) }
            }
        }
    }

    fun onSongOpened(song: Song) {
        val index = lastResults.indexOfFirst { it == song }
        if (index >= 0) {
            val requiredVisible = ((index / pageSize) + 1) * pageSize
            val newVisible = minOf(maxOf(visibleCount, requiredVisible), lastResults.size)
            if (newVisible != visibleCount) {
                visibleCount = newVisible
                _uiState.update {
                    it.copy(
                        songResults = lastResults.take(visibleCount),
                        hasMore = visibleCount < lastResults.size
                    )
                }
            }
        }
    }

    private fun filterSongsWithNumberPriority(
        songs: List<Song>,
        originalQuery: String,
        normalizedQuery: String
    ): List<Song> {
        val trimmedQuery = originalQuery.trim()
        val isNumericQuery = trimmedQuery.isNotEmpty() && trimmedQuery.all { it.isDigit() }

        if (isNumericQuery) {
            fun allNumbersOf(song: Song): List<String> = buildList {
                add(song.numerSAK2020)
                add(song.numerDN)
                add(song.numerSiedl)
                add(song.numerSAK)
                addAll(song.numery.values)
            }

            val exactMatches = songs
                .filter { song -> allNumbersOf(song).any { it == trimmedQuery } }
                .sortedBy { it.tytul }

            val partialMatches = songs
                .filter { song ->
                    val numbers = allNumbersOf(song)
                    numbers.any { it.contains(trimmedQuery) && it != trimmedQuery }
                }
                .sortedBy { it.tytul }

            return exactMatches + partialMatches
        } else {
            return songs.filter { song ->
                val matchesTitle = _uiState.value.searchInTitle && normalize(song.tytul).contains(normalizedQuery)
                val matchesContent = _uiState.value.searchInContent && normalize(song.tekst ?: "").contains(normalizedQuery)
                val matchesTag = song.tagi.any { normalize(it).contains(normalizedQuery) }
                val matchesNumbers = buildList {
                    add(song.numerSAK2020)
                    add(song.numerDN)
                    add(song.numerSiedl)
                    add(song.numerSAK)
                    addAll(song.numery.values)
                }.any { it.contains(originalQuery, ignoreCase = true) }
                matchesTitle || matchesContent || matchesTag || matchesNumbers
            }
        }
    }
}