package com.qjproject.liturgicalcalendar.ui.screens.daydetails

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qjproject.liturgicalcalendar.data.DayData
import com.qjproject.liturgicalcalendar.data.FileSystemRepository
import com.qjproject.liturgicalcalendar.data.SuggestedSong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DayDetailsUiState(
    val isLoading: Boolean = true,
    val dayData: DayData? = null,
    val error: String? = null,
    val isReadingsSectionExpanded: Boolean = false,
    val isSongsSectionExpanded: Boolean = true,
    val expandedReadings: Set<Int> = emptySet(),
    val expandedSongMoments: Set<String> = setOf("wejscie", "ofiarowanie", "komunia", "uwielbienie", "rozeslanie", "ogolne")
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

    val groupedSongs = MutableStateFlow<Map<String, List<SuggestedSong>>>(emptyMap())

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            Log.d("DayDetailsViewModel", "Rozpoczynanie ładowania danych dla dayId: $dayId")
            val data = repository.getDayData(dayId)
            if (data != null) {
                Log.d("DayDetailsViewModel", "Dane załadowane pomyślnie dla: ${data.tytulDnia}")
                _uiState.update { it.copy(isLoading = false, dayData = data) }
                groupSongs(data.piesniSugerowane)
            } else {
                val errorMessage = "Nie udało się odnaleźć lub odczytać pliku. Próbowano załadować: '$dayId.json'"
                Log.e("DayDetailsViewModel", errorMessage)
                _uiState.update { it.copy(isLoading = false, error = errorMessage) }
            }
        }
    }

    private fun groupSongs(songs: List<SuggestedSong?>?) {
        val grouped = songs.orEmpty().filterNotNull().groupBy { it.moment }
        groupedSongs.value = grouped
    }

    fun toggleReadingsSection() {
        _uiState.update { it.copy(isReadingsSectionExpanded = !it.isReadingsSectionExpanded) }
    }

    fun toggleSongsSection() {
        _uiState.update { it.copy(isSongsSectionExpanded = !it.isSongsSectionExpanded) }
    }

    fun toggleReading(index: Int) {
        _uiState.update {
            val newSet = it.expandedReadings.toMutableSet()
            if (index in newSet) newSet.remove(index) else newSet.add(index)
            it.copy(expandedReadings = newSet)
        }
    }

    fun collapseReading(index: Int) {
        _uiState.update {
            val newSet = it.expandedReadings.toMutableSet()
            newSet.remove(index)
            it.copy(expandedReadings = newSet)
        }
    }

    fun toggleSongMoment(moment: String) {
        _uiState.update {
            val newSet = it.expandedSongMoments.toMutableSet()
            if (moment in newSet) newSet.remove(moment) else newSet.add(moment)
            it.copy(expandedSongMoments = newSet)
        }
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