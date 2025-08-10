package com.qjproject.liturgicalcalendar.ui.screens.daydetails

import android.content.Context
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
import kotlinx.coroutines.flow.asStateFlow
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
    val isReadingsSectionExpanded: Boolean = false,
    val isSongsSectionExpanded: Boolean = true,
    val expandedReadings: Set<Int> = emptySet(),
    val expandedSongMoments: Set<String> = songMomentOrderMap.keys.toSet(),
    val isEditMode: Boolean = false,
    val showConfirmExitDialog: Boolean = false,
    val hasChanges: Boolean = false,
    val activeDialog: DialogState = DialogState.None
)

val songMomentOrderMap = mapOf(
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
    val uiState = _uiState.asStateFlow()

    private val _groupedSongs = MutableStateFlow<Map<String, List<SuggestedSong>>>(emptyMap())
    val groupedSongs = _groupedSongs.asStateFlow()

    var editableDayData = mutableStateOf<DayData?>(null)
        private set

    private val allSongs: List<Song> by lazy { repository.getSongList() }
    val songSearchResults = MutableStateFlow<List<Song>>(emptyList())

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val data = repository.getDayData(dayId)
            if (data != null) {
                _uiState.update { it.copy(isLoading = false, dayData = data) }
                groupSongs(data.piesniSugerowane)
            } else {
                val errorMessage = "Nie udało się odnaleźć lub odczytać pliku: '$dayId.json'"
                _uiState.update { it.copy(isLoading = false, error = errorMessage) }
            }
        }
    }

    private fun groupSongs(songs: List<SuggestedSong?>?) {
        val songsByMoment = songs.orEmpty().filterNotNull().groupBy { it.moment }
        val fullGroupedMap = songMomentOrderMap.keys.associateWith { momentKey ->
            songsByMoment[momentKey] ?: emptyList()
        }
        _groupedSongs.value = fullGroupedMap
    }

    fun onEnterEditMode() {
        _uiState.value.dayData?.let {
            editableDayData.value = it.copy(
                czytania = it.czytania.map { c -> c.copy() },
                piesniSugerowane = it.piesniSugerowane?.mapNotNull { s -> s?.copy() }
            )
            _uiState.update { state -> state.copy(isEditMode = true, hasChanges = false) }
        }
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
            _uiState.update { it.copy(showConfirmExitDialog = false) }
            if (save && _uiState.value.hasChanges) {
                editableDayData.value?.let { dataToSave ->
                    repository.saveDayData(dayId, dataToSave).onSuccess {
                        _uiState.update { it.copy(isEditMode = false, hasChanges = false) }
                        loadData()
                    }.onFailure { error ->
                        _uiState.update { it.copy(error = "Błąd zapisu: ${error.localizedMessage}") }
                    }
                }
            } else {
                _uiState.update { it.copy(isEditMode = false, hasChanges = false) }
                editableDayData.value = null
            }
        }
    }

    fun dismissConfirmExitDialog() {
        _uiState.update { it.copy(showConfirmExitDialog = false) }
    }

    private fun markAsChanged() {
        if (!_uiState.value.hasChanges) {
            _uiState.update { it.copy(hasChanges = true) }
        }
    }

    fun showDialog(dialog: DialogState) { _uiState.update { it.copy(activeDialog = dialog) } }
    fun dismissDialog() { _uiState.update { it.copy(activeDialog = DialogState.None) } }

    fun addOrUpdateReading(reading: Reading, index: Int?) {
        editableDayData.value?.let {
            val newList = it.czytania.toMutableList()
            if (index != null && index in newList.indices) {
                newList[index] = reading
            } else {
                newList.add(reading)
            }
            editableDayData.value = it.copy(czytania = newList)
            markAsChanged()
        }
    }

    fun deleteItem(item: Any) {
        when (item) {
            is Reading -> deleteReading(item)
            is SuggestedSong -> deleteSong(item)
        }
        dismissDialog()
    }

    private fun deleteReading(readingToDelete: Reading) {
        editableDayData.value?.let {
            val newList = it.czytania.toMutableList().apply { remove(readingToDelete) }
            editableDayData.value = it.copy(czytania = newList)
            markAsChanged()
        }
    }

    fun reorderReadings(from: Int, to: Int) {
        editableDayData.value?.let {
            val currentList = it.czytania.toMutableList()
            if (from in currentList.indices && to in currentList.indices) {
                val item = currentList.removeAt(from)
                currentList.add(to, item)
                editableDayData.value = it.copy(czytania = currentList)
                markAsChanged()
            }
        }
    }

    fun addOrUpdateSong(song: SuggestedSong, moment: String, originalSong: SuggestedSong?) {
        editableDayData.value?.let { data ->
            val currentSongs = data.piesniSugerowane.orEmpty().filterNotNull().toMutableList()
            if (originalSong != null) {
                val index = currentSongs.indexOf(originalSong)
                if (index != -1) currentSongs[index] = song
            } else {
                currentSongs.add(song)
            }
            editableDayData.value = data.copy(piesniSugerowane = currentSongs)
            markAsChanged()
        }
    }

    private fun deleteSong(songToDelete: SuggestedSong) {
        editableDayData.value?.let { data ->
            val currentSongs = data.piesniSugerowane.orEmpty().filterNotNull().toMutableList()
            currentSongs.remove(songToDelete)
            editableDayData.value = data.copy(piesniSugerowane = currentSongs)
            markAsChanged()
        }
    }

    fun reorderSongs(moment: String, fromIndexInMoment: Int, toIndexInMoment: Int) {
        editableDayData.value?.let { data ->
            val allSongs = data.piesniSugerowane.orEmpty().filterNotNull()
            val songsInMoment = allSongs.filter { it.moment == moment }.toMutableList()
            val otherSongs = allSongs.filter { it.moment != moment }

            if (fromIndexInMoment in songsInMoment.indices && toIndexInMoment in songsInMoment.indices) {
                val movedSong = songsInMoment.removeAt(fromIndexInMoment)
                songsInMoment.add(toIndexInMoment, movedSong)
                editableDayData.value = data.copy(piesniSugerowane = otherSongs + songsInMoment)
                markAsChanged()
            }
        }
    }

    fun searchSongs(query: String) {
        if (query.isBlank()) {
            songSearchResults.value = emptyList()
            return
        }
        val cleanedQuery = query.lowercase().replace(",", "")
        viewModelScope.launch {
            songSearchResults.value = allSongs.filter {
                val titleMatch = it.tytul.lowercase().replace(",", "").contains(cleanedQuery)
                val numberMatch = it.numer == cleanedQuery
                titleMatch || numberMatch
            }
        }
    }

    fun toggleReadingsSection() { _uiState.update { it.copy(isReadingsSectionExpanded = !it.isReadingsSectionExpanded) } }
    fun toggleSongsSection() { _uiState.update { it.copy(isSongsSectionExpanded = !it.isSongsSectionExpanded) } }
    fun toggleReading(index: Int) { _uiState.update { val s = it.expandedReadings.toMutableSet(); if (index in s) s.remove(index) else s.add(index); it.copy(expandedReadings = s) } }
    fun collapseReading(index: Int) { _uiState.update { val s = it.expandedReadings.toMutableSet(); s.remove(index); it.copy(expandedReadings = s) } }
    fun toggleSongMoment(moment: String) { _uiState.update { val s = it.expandedSongMoments.toMutableSet(); if (moment in s) s.remove(moment) else s.add(moment); it.copy(expandedSongMoments = s) } }
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