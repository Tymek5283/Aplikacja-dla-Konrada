package com.qjproject.liturgicalcalendar.ui.screens.daydetails

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qjproject.liturgicalcalendar.data.DayData
import com.qjproject.liturgicalcalendar.data.FileSystemRepository
import com.qjproject.liturgicalcalendar.data.Reading
import com.qjproject.liturgicalcalendar.data.Song
import com.qjproject.liturgicalcalendar.data.SuggestedSong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class DialogState {
    object None : DialogState()
    data class ConfirmDelete(val item: Any, val description: String) : DialogState()
    data class AddEditSong(val moment: String, val existingSong: SuggestedSong? = null) : DialogState()
}

data class DayDetailsUiState(
    val isLoading: Boolean = true,
    val dayData: DayData? = null,
    val error: String? = null,
    val isEditMode: Boolean = false,
    val hasChanges: Boolean = false,
    val showConfirmExitDialog: Boolean = false,
    val activeDialog: DialogState = DialogState.None,
    val isReadingsSectionExpanded: Boolean = true,
    val isSongsSectionExpanded: Boolean = true,
    val expandedReadings: Set<Int> = emptySet(),
    val expandedSongMoments: Set<String> = songMomentOrderMap.keys
)

val songMomentOrderMap: LinkedHashMap<String, String> = linkedMapOf(
    "wejscie" to "Wejście",
    "ofiarowanie" to "Ofiarowanie",
    "komunia" to "Komunia",
    "uwielbienie" to "Uwielbienie",
    "rozeslanie" to "Rozesłanie",
    "ogolne" to "Ogólne"
)

class DayDetailsViewModel(
    private val dayId: String,
    private val repository: FileSystemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DayDetailsUiState())
    val uiState: StateFlow<DayDetailsUiState> = _uiState.asStateFlow()

    private val _songTitleSearchQuery = MutableStateFlow("")
    val songTitleSearchResults = MutableStateFlow<List<Song>>(emptyList())

    private val _songNumberSearchQuery = MutableStateFlow("")
    val songNumberSearchResults = MutableStateFlow<List<Song>>(emptyList())

    var editableDayData: MutableState<DayData?> = mutableStateOf(null)
        private set

    val groupedSongs: StateFlow<Map<String, List<SuggestedSong>>> = _uiState.map { state ->
        state.dayData?.piesniSugerowane
            .orEmpty()
            .filterNotNull()
            .groupBy { it.moment }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        loadDayData()
        observeSearchQueries()
    }

    private fun observeSearchQueries() {
        viewModelScope.launch {
            _songTitleSearchQuery.debounce(300).distinctUntilChanged().collectLatest { query ->
                if (query.length < 2) {
                    songTitleSearchResults.value = emptyList()
                } else {
                    songTitleSearchResults.value = repository.getSongList().filter {
                        it.tytul.contains(query, ignoreCase = true)
                    }.take(10)
                }
            }
        }
        viewModelScope.launch {
            _songNumberSearchQuery.debounce(300).distinctUntilChanged().collectLatest { query ->
                if (query.isBlank()) {
                    songNumberSearchResults.value = emptyList()
                } else {
                    songNumberSearchResults.value = repository.getSongList().filter {
                        it.numerSiedl.startsWith(query, ignoreCase = true)
                    }.take(10)
                }
            }
        }
    }

    private fun loadDayData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val data = repository.getDayData(dayId)
            if (data != null) {
                _uiState.update { it.copy(isLoading = false, dayData = data) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Nie udało się wczytać danych dla tego dnia.") }
            }
        }
    }

    fun onEnterEditMode() {
        editableDayData.value = _uiState.value.dayData?.copy()
        _uiState.update { it.copy(isEditMode = true, hasChanges = false) }
    }

    fun onTryExitEditMode() {
        if (_uiState.value.hasChanges) {
            _uiState.update { it.copy(showConfirmExitDialog = true) }
        } else {
            onExitEditMode(save = false)
        }
    }

    fun onExitEditMode(save: Boolean) {
        viewModelScope.launch {
            if (save && _uiState.value.hasChanges) {
                val dataToSave = editableDayData.value
                if (dataToSave != null) {
                    repository.saveDayData(dayId, dataToSave).onSuccess {
                        _uiState.update { currentState -> currentState.copy(dayData = dataToSave) }
                    }
                }
            }
            _uiState.update { it.copy(isEditMode = false, hasChanges = false, showConfirmExitDialog = false) }
            editableDayData.value = null
            loadDayData()
        }
    }

    fun dismissConfirmExitDialog() {
        _uiState.update { it.copy(showConfirmExitDialog = false) }
    }

    fun showDialog(dialogState: DialogState) { _uiState.update { it.copy(activeDialog = dialogState) } }
    fun dismissDialog() { _uiState.update { it.copy(activeDialog = DialogState.None) } }

    fun toggleReadingsSection() { _uiState.update { it.copy(isReadingsSectionExpanded = !it.isReadingsSectionExpanded) } }
    fun toggleSongsSection() { _uiState.update { it.copy(isSongsSectionExpanded = !it.isSongsSectionExpanded) } }

    fun toggleReading(index: Int) {
        _uiState.update {
            val updatedSet = it.expandedReadings.toMutableSet()
            if (updatedSet.contains(index)) updatedSet.remove(index) else updatedSet.add(index)
            it.copy(expandedReadings = updatedSet)
        }
    }

    fun collapseReading(index: Int) {
        _uiState.update {
            val updatedSet = it.expandedReadings.toMutableSet()
            updatedSet.remove(index)
            it.copy(expandedReadings = updatedSet)
        }
    }

    fun toggleSongMoment(moment: String) {
        _uiState.update {
            val updatedSet = it.expandedSongMoments.toMutableSet()
            if (updatedSet.contains(moment)) updatedSet.remove(moment) else updatedSet.add(moment)
            it.copy(expandedSongMoments = updatedSet)
        }
    }

    // --- POCZĄTEK ZMIANY ---
    fun getFullSong(suggestedSong: SuggestedSong, onResult: (Song?) -> Unit) {
        viewModelScope.launch {
            val song = repository.getSong(suggestedSong.piesn, suggestedSong.numer, null, null)
            onResult(song)
        }
    }
    // --- KONIEC ZMIANY ---

    private fun updateEditableData(transform: (DayData) -> DayData) {
        editableDayData.value?.let {
            editableDayData.value = transform(it)
            _uiState.update { state -> state.copy(hasChanges = true) }
        }
    }

    fun addOrUpdateReading(reading: Reading, index: Int?) {
        updateEditableData { currentData ->
            val readings = currentData.czytania.toMutableList()
            if (index != null && index in readings.indices) {
                readings[index] = reading
            } else {
                readings.add(reading)
            }
            currentData.copy(czytania = readings)
        }
    }

    fun deleteItem(item: Any) {
        when (item) {
            is Reading -> {
                updateEditableData { currentData ->
                    currentData.copy(czytania = currentData.czytania - item)
                }
            }
            is SuggestedSong -> {
                updateEditableData { currentData ->
                    currentData.copy(piesniSugerowane = currentData.piesniSugerowane.orEmpty() - item)
                }
            }
        }
        dismissDialog()
    }

    fun reorderReadings(from: Int, to: Int) {
        updateEditableData { currentData ->
            val reorderedList = currentData.czytania.toMutableList().apply {
                add(to, removeAt(from))
            }
            currentData.copy(czytania = reorderedList)
        }
    }

    fun reorderSongs(moment: String, from: Int, to: Int) {
        updateEditableData { currentData ->
            val allSongs = currentData.piesniSugerowane.orEmpty().filterNotNull().toMutableList()
            val songsInMoment = allSongs.filter { it.moment == moment }.toMutableList()

            if (from in songsInMoment.indices && to in songsInMoment.indices) {
                val movedItem = songsInMoment.removeAt(from)
                songsInMoment.add(to, movedItem)

                val otherSongs = allSongs.filter { it.moment != moment }
                val newFullList = (otherSongs + songsInMoment).sortedWith(
                    compareBy { songMomentOrderMap.keys.indexOf(it.moment) }
                )
                currentData.copy(piesniSugerowane = newFullList)
            } else {
                currentData
            }
        }
    }

    fun addOrUpdateSong(song: SuggestedSong, moment: String, originalSong: SuggestedSong?) {
        updateEditableData { currentData ->
            val songs = currentData.piesniSugerowane.orEmpty().filterNotNull().toMutableList()
            if (originalSong != null) {
                val index = songs.indexOf(originalSong)
                if (index != -1) {
                    songs[index] = song
                }
            } else {
                songs.add(song)
            }
            currentData.copy(piesniSugerowane = songs)
        }
        dismissDialog()
    }

    fun searchSongsByTitle(query: String) {
        _songTitleSearchQuery.value = query
        if (query.length < 2) songTitleSearchResults.value = emptyList()
    }

    fun searchSongsByNumber(query: String) {
        _songNumberSearchQuery.value = query
        if (query.isBlank()) songNumberSearchResults.value = emptyList()
    }

    fun formatSongSuggestion(song: Song): String {
        val numberInfo = buildList {
            if (song.numerSiedl.isNotBlank()) add("Siedl: ${song.numerSiedl}")
            if (song.numerSAK.isNotBlank()) add("ŚAK: ${song.numerSAK}")
            if (song.numerDN.isNotBlank()) add("DN: ${song.numerDN}")
        }.joinToString(", ")

        return if (numberInfo.isNotEmpty()) {
            "${song.tytul} ($numberInfo)"
        } else {
            song.tytul
        }
    }

    fun clearAllSearchResults() {
        songTitleSearchResults.value = emptyList()
        songNumberSearchResults.value = emptyList()
        _songTitleSearchQuery.value = ""
        _songNumberSearchQuery.value = ""
    }
}

class DayDetailsViewModelFactory(
    private val context: Context,
    private val dayId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DayDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DayDetailsViewModel(dayId, FileSystemRepository(context.applicationContext)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}