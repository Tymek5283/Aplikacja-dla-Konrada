package com.qjproject.liturgicalcalendar.ui.screens.daydetails

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
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
import org.burnoutcrew.reorderable.ItemPosition

sealed class DialogState {
    object None : DialogState()
    data class ConfirmDelete(val item: Any, val description: String) : DialogState()
    data class AddEditSong(
        val moment: String,
        val existingSong: SuggestedSong? = null,
        val error: String? = null
    ) : DialogState()
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

sealed class ReorderableListItem {
    data class SongItem(val suggestedSong: SuggestedSong) : ReorderableListItem()
    data class HeaderItem(val momentKey: String, val momentName: String) : ReorderableListItem()
}

class DayDetailsViewModel(
    private val dayId: String,
    private val repository: FileSystemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DayDetailsUiState())
    val uiState: StateFlow<DayDetailsUiState> = _uiState.asStateFlow()

    private val _songTitleSearchQuery = MutableStateFlow("")
    val songTitleSearchResults = MutableStateFlow<List<Song>>(emptyList())

    private val _siedlSearchQuery = MutableStateFlow("")
    val siedlSearchResults = MutableStateFlow<List<Song>>(emptyList())
    private val _sakSearchQuery = MutableStateFlow("")
    val sakSearchResults = MutableStateFlow<List<Song>>(emptyList())
    private val _dnSearchQuery = MutableStateFlow("")
    val dnSearchResults = MutableStateFlow<List<Song>>(emptyList())

    var editableDayData: MutableState<DayData?> = mutableStateOf(null)
        private set

    val groupedSongs: StateFlow<Map<String, List<SuggestedSong>>> = _uiState.map { state ->
        state.dayData?.piesniSugerowane
            .orEmpty()
            .filterNotNull()
            .groupBy { it.moment }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val reorderableSongList: StateFlow<List<ReorderableListItem>> =
        snapshotFlow { editableDayData.value }
            .map { dayData: DayData? ->
                val items = mutableListOf<ReorderableListItem>()
                val songsByMoment =
                    dayData?.piesniSugerowane?.filterNotNull()?.groupBy { it.moment } ?: emptyMap()
                songMomentOrderMap.forEach { (momentKey, momentName) ->
                    items.add(ReorderableListItem.HeaderItem(momentKey, momentName))
                    songsByMoment[momentKey]?.forEach { song ->
                        items.add(ReorderableListItem.SongItem(song))
                    }
                }
                items
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


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
                        it.tytul.contains(query, ignoreCase = true) &&
                                (it.numerSiedl.isNotBlank() || it.numerSAK.isNotBlank() || it.numerDN.isNotBlank())
                    }.take(10)
                }
            }
        }
        viewModelScope.launch {
            _siedlSearchQuery.debounce(300).distinctUntilChanged().collectLatest { query ->
                if (query.isBlank()) {
                    siedlSearchResults.value = emptyList()
                } else {
                    siedlSearchResults.value = repository.getSongList().filter {
                        it.numerSiedl.startsWith(query, ignoreCase = true)
                    }.take(10)
                }
            }
        }
        viewModelScope.launch {
            _sakSearchQuery.debounce(300).distinctUntilChanged().collectLatest { query ->
                if (query.isBlank()) {
                    sakSearchResults.value = emptyList()
                } else {
                    sakSearchResults.value = repository.getSongList().filter {
                        it.numerSAK.startsWith(query, ignoreCase = true)
                    }.take(10)
                }
            }
        }
        viewModelScope.launch {
            _dnSearchQuery.debounce(300).distinctUntilChanged().collectLatest { query ->
                if (query.isBlank()) {
                    dnSearchResults.value = emptyList()
                } else {
                    dnSearchResults.value = repository.getSongList().filter {
                        it.numerDN.startsWith(query, ignoreCase = true)
                    }.take(10)
                }
            }
        }
    }

    fun loadDayData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.invalidateSongCache()
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

    fun getFullSong(suggestedSong: SuggestedSong, onResult: (Song?) -> Unit) {
        viewModelScope.launch {
            val song = repository.getSong(suggestedSong.piesn, suggestedSong.numer, null, null)
            onResult(song)
        }
    }

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

    fun reorderSongs(from: ItemPosition, to: ItemPosition) {
        updateEditableData { currentData ->
            val currentFlatList = reorderableSongList.value.toMutableList()

            if (currentFlatList.getOrNull(from.index) !is ReorderableListItem.SongItem) {
                return@updateEditableData currentData
            }

            val movedItem = currentFlatList.removeAt(from.index)
            currentFlatList.add(to.index, movedItem)

            val newPiesniSugerowane = mutableListOf<SuggestedSong>()
            var currentMoment: String? = null
            for (item in currentFlatList) {
                when (item) {
                    is ReorderableListItem.HeaderItem -> {
                        currentMoment = item.momentKey
                    }
                    is ReorderableListItem.SongItem -> {
                        currentMoment?.let { moment ->
                            newPiesniSugerowane.add(item.suggestedSong.copy(moment = moment))
                        }
                    }
                }
            }

            currentData.copy(piesniSugerowane = newPiesniSugerowane)
        }
    }

    fun addOrUpdateSong(
        title: String,
        siedl: String,
        sak: String,
        dn: String,
        opis: String,
        moment: String,
        originalSong: SuggestedSong?
    ) {
        viewModelScope.launch {
            val trimmedTitle = title.trim()
            val trimmedSiedl = siedl.trim()
            val trimmedSak = sak.trim()
            val trimmedDn = dn.trim()

            val songMatches = repository.getSongList().filter { it.tytul.equals(trimmedTitle, ignoreCase = true) }

            val perfectMatch = songMatches.find {
                it.numerSiedl.equals(trimmedSiedl, ignoreCase = true) &&
                        it.numerSAK.equals(trimmedSak, ignoreCase = true) &&
                        it.numerDN.equals(trimmedDn, ignoreCase = true)
            }

            if (perfectMatch == null) {
                val error = when {
                    songMatches.isEmpty() -> "Nie znaleziono pieśni o podanym tytule."
                    else -> "Numery nie pasują do pieśni o tym tytule. Wybierz pieśń z sugestii, aby automatycznie uzupełnić pola."
                }
                _uiState.update {
                    val currentDialog = it.activeDialog as? DialogState.AddEditSong
                    it.copy(activeDialog = currentDialog?.copy(error = error) ?: DialogState.None)
                }
                return@launch
            }

            val newSuggestedSong = SuggestedSong(
                numer = trimmedSiedl,
                piesn = trimmedTitle,
                opis = opis.trim(),
                moment = moment
            )

            updateEditableData { currentData ->
                val songs = currentData.piesniSugerowane.orEmpty().filterNotNull().toMutableList()
                if (originalSong != null) {
                    val index = songs.indexOf(originalSong)
                    if (index != -1) {
                        songs[index] = newSuggestedSong
                    } else {
                        songs.add(newSuggestedSong)
                    }
                } else {
                    songs.add(newSuggestedSong)
                }
                currentData.copy(piesniSugerowane = songs)
            }
            dismissDialog()
        }
    }

    fun searchSongsByTitle(query: String) {
        _songTitleSearchQuery.value = query
        if (query.length < 2) songTitleSearchResults.value = emptyList()
    }

    fun searchSongsBySiedl(query: String) {
        _siedlSearchQuery.value = query
        if (query.isBlank()) siedlSearchResults.value = emptyList()
    }

    fun searchSongsBySak(query: String) {
        _sakSearchQuery.value = query
        if (query.isBlank()) sakSearchResults.value = emptyList()
    }

    fun searchSongsByDn(query: String) {
        _dnSearchQuery.value = query
        if (query.isBlank()) dnSearchResults.value = emptyList()
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
        siedlSearchResults.value = emptyList()
        sakSearchResults.value = emptyList()
        dnSearchResults.value = emptyList()
        _songTitleSearchQuery.value = ""
        _siedlSearchQuery.value = ""
        _sakSearchQuery.value = ""
        _dnSearchQuery.value = ""
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