package com.qjproject.liturgicalcalendar.ui.screens.search

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
enum class SearchViewState { CATEGORY_SELECTION, SONG_LIST }

sealed class DeleteDialogState {
    object None : DeleteDialogState()
    data class ConfirmInitial(val song: Song) : DeleteDialogState()
    data class ConfirmOccurrences(val song: Song) : DeleteDialogState()
}

data class SearchUiState(
    val query: String = "",
    val results: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val searchInTitle: Boolean = true,
    val searchInContent: Boolean = false,
    val sortMode: SongSortMode = SongSortMode.Alfabetycznie,
    val showAddSongDialog: Boolean = false,
    val addSongError: String? = null,
    val deleteDialogState: DeleteDialogState = DeleteDialogState.None,
    val allCategories: List<Category> = emptyList(),
    val selectedCategory: Category? = null,
    val currentView: SearchViewState = SearchViewState.CATEGORY_SELECTION
) {
    val isBackButtonVisible: Boolean get() = currentView == SearchViewState.SONG_LIST
}

class SearchViewModel(private val repository: FileSystemRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()

    private val _queryFlow = MutableStateFlow("")
    private var searchJob: Job? = null
    private var allSongsCache: List<Song>? = null

    init {
        loadCategories()
        _queryFlow
            .debounce(300)
            .distinctUntilChanged()
            .onEach { performSearch() }
            .launchIn(viewModelScope)
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val categories = repository.getCategoryList().sortedBy { it.nazwa }
            _uiState.update { it.copy(allCategories = categories, isLoading = false) }
        }
    }

    private fun performSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (_uiState.value.currentView != SearchViewState.SONG_LIST) return@launch

            _uiState.update { it.copy(isLoading = true) }
            val allSongs = allSongsCache ?: repository.getSongList().also { allSongsCache = it }
            val selectedCategory = _uiState.value.selectedCategory

            val categoryFilteredSongs = if (selectedCategory != null) {
                allSongs.filter { it.kategoria.equals(selectedCategory.nazwa, ignoreCase = true) }
            } else {
                allSongs
            }

            val query = _uiState.value.query.trim()
            val finalResults = if (query.isBlank()) {
                categoryFilteredSongs
            } else {
                val normalizedQuery = normalize(query)
                categoryFilteredSongs.filter { song ->
                    val matchesTitle = _uiState.value.searchInTitle && normalize(song.tytul).contains(normalizedQuery)
                    val matchesContent = _uiState.value.searchInContent && normalize(song.tekst ?: "").contains(normalizedQuery)
                    matchesTitle || matchesContent
                }
            }
            _uiState.update { it.copy(results = sortSongs(finalResults), isLoading = false) }
        }
    }

    fun onQueryChange(newQuery: String) {
        _uiState.update { it.copy(query = newQuery) }
        _queryFlow.value = newQuery
    }

    fun onCategorySelected(category: Category) {
        repository.invalidateSongCache()
        allSongsCache = null
        _uiState.update { it.copy(selectedCategory = category, currentView = SearchViewState.SONG_LIST, query = "") }
        performSearch()
    }

    fun onNavigateBack() {
        _uiState.update {
            it.copy(
                currentView = SearchViewState.CATEGORY_SELECTION,
                selectedCategory = null,
                results = emptyList(),
                query = ""
            )
        }
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
        _uiState.update { it.copy(results = sortSongs(it.results)) }
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
                kategoriaSkr = selectedCategory?.skrot ?: ""
            )
            songs.add(newSong)

            repository.saveSongList(songs).fold(
                onSuccess = {
                    onDismissAddSongDialog()
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